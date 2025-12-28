package com.example.asystent_ekologiczny;

import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Raporty: obliczanie sumy wydatków, średniej ceny, kaucji i udziału kategorii
 * w zadanym zakresie dat (purchase_date, returned_at) oraz wykresy.
 */
public class ReportsFragment extends Fragment {

    public static final String TAG = "ReportsFragment";

    private ProductDbHelper dbHelper;
    private DepositDbHelper depositDbHelper;
    private BarChart barChart;
    private TextView categorySumsTv;
    private TextView avgPriceTv;
    private TextView expiredCountTv;
    private PieChart categoryPieChart;
    private TextView dateFromTv;
    private TextView dateToTv;
    private Button calcRangeBtn;
    // jeden przycisk eksportu z wyborem formatu
    private ImageButton exportBtn;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

    private String formatPln(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return "PLN " + df.format(value);
    }

    // pola do trzymania ostatniego wyniku raportu
    private String lastFrom;
    private String lastTo;
    private double lastSum;
    private double lastDepositSum;
    private double lastAvg;
    private int lastExpired;
    private Map<String, Double> lastCategorySums;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_reports, container, false);
        dbHelper = new ProductDbHelper(requireContext());
        depositDbHelper = new DepositDbHelper(requireContext());

        dateFromTv = root.findViewById(R.id.tv_date_from);
        dateToTv = root.findViewById(R.id.tv_date_to);
        calcRangeBtn = root.findViewById(R.id.btn_calculate_range);
        exportBtn = root.findViewById(R.id.btn_export);
        TextView resultTv = root.findViewById(R.id.tv_monthly_sum);
        TextView depositResultTv = root.findViewById(R.id.tv_deposit_sum);
        barChart = root.findViewById(R.id.bar_chart_expenses_vs_deposits);
        categorySumsTv = root.findViewById(R.id.tv_category_sums);
        avgPriceTv = root.findViewById(R.id.tv_monthly_avg_price);
        expiredCountTv = root.findViewById(R.id.tv_expired_count);
        categoryPieChart = root.findViewById(R.id.pie_chart_categories);

        // Domyślny zakres: od początku bieżącego miesiąca do dziś
        Calendar cal = Calendar.getInstance();
        String todayStr = dateFormat.format(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, 1);
        String monthStartStr = dateFormat.format(cal.getTime());
        dateFromTv.setText(monthStartStr);
        dateToTv.setText(todayStr);

        dateFromTv.setOnClickListener(v -> showDatePicker(dateFromTv));
        dateToTv.setOnClickListener(v -> showDatePicker(dateToTv));

        // Pierwsze przeliczenie dla domyślnego zakresu
        recalculateForCurrentRange(resultTv, depositResultTv);

        // Konfiguracja wykresu słupkowego (dla całego roku bieżącego jak dotychczas)
        setupBarChart();
        updateBarChartForYear(Calendar.getInstance().get(Calendar.YEAR));

        calcRangeBtn.setOnClickListener(v -> recalculateForCurrentRange(resultTv, depositResultTv));
        exportBtn.setOnClickListener(v -> showExportFormatDialog());

        return root;
    }

    private void showExportFormatDialog() {
        if (lastFrom == null || lastTo == null) {
            Toast.makeText(requireContext(), R.string.reports_error_date_range, Toast.LENGTH_SHORT).show();
            return;
        }
        final CharSequence[] items = {
                getString(R.string.reports_export_csv_option),
                getString(R.string.reports_export_pdf_option),
                getString(R.string.reports_export_html_option)
        };

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.reports_export_all_title)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        exportReportToCsv();
                    } else if (which == 1) {
                        exportReportToPdf();
                    } else if (which == 2) {
                        exportReportToHtml();
                    }
                })
                .show();
    }

    private void showDatePicker(TextView target) {
        try {
            Date current = dateFormat.parse(target.getText().toString());
            Calendar cal = Calendar.getInstance();
            if (current != null) cal.setTime(current);
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(requireContext(), (DatePicker view, int y, int m, int d) -> {
                Calendar chosen = Calendar.getInstance();
                chosen.set(y, m, d, 0, 0, 0);
                target.setText(dateFormat.format(chosen.getTime()));
            }, year, month, day);
            dialog.show();
        } catch (ParseException e) {
            // Jeśli parsowanie się nie udało, ustaw dzisiejszą datę
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog dialog = new DatePickerDialog(requireContext(), (DatePicker view, int y, int m, int d) -> {
                Calendar chosen = Calendar.getInstance();
                chosen.set(y, m, d, 0, 0, 0);
                target.setText(dateFormat.format(chosen.getTime()));
            }, year, month, day);
            dialog.show();
        }
    }

    private boolean isValidRange(String from, String to) {
        try {
            Date dFrom = dateFormat.parse(from);
            Date dTo = dateFormat.parse(to);
            return dFrom != null && dTo != null && !dFrom.after(dTo);
        } catch (ParseException e) {
            return false;
        }
    }

    private void recalculateForCurrentRange(TextView resultTv, TextView depositResultTv) {
        String from = dateFromTv.getText().toString();
        String to = dateToTv.getText().toString();
        if (!isValidRange(from, to)) {
            resultTv.setText(getString(R.string.reports_error_date_range));
            depositResultTv.setText(getString(R.string.reports_error_date_range));
            avgPriceTv.setText(getString(R.string.reports_monthly_avg_placeholder));
            expiredCountTv.setText(getString(R.string.reports_expired_placeholder));
            categorySumsTv.setText(getString(R.string.reports_category_sum_placeholder));
            if (categoryPieChart != null) categoryPieChart.clear();
            return;
        }

        double sum = dbHelper.getSumInRange(from, to);
        double depSum = depositDbHelper.sumReturnedValueInRange(from, to);
        double avg = dbHelper.getAveragePriceInRange(from, to);
        int expired = dbHelper.getExpiredCountInRange(from, to);

        // zapamiętujemy ostatnie wartości, aby móc je wyeksportować
        lastFrom = from;
        lastTo = to;
        lastSum = sum;
        lastDepositSum = depSum;
        lastAvg = avg;
        lastExpired = expired;
        lastCategorySums = dbHelper.getCategorySumsInRange(from, to);

        resultTv.setText(getString(R.string.reports_monthly_sum_result, formatPln(sum)));
        depositResultTv.setText(getString(R.string.reports_deposit_sum_result, formatPln(depSum)));
        avgPriceTv.setText(getString(R.string.reports_monthly_avg_result, formatPln(avg)));
        expiredCountTv.setText(getString(R.string.reports_expired_result, expired));

        updateCategoryReport(from, to);
        updateCategoryPie(from, to);
    }

    private void updateCategoryReport(String from, String to) {
        if (dbHelper == null || categorySumsTv == null) return;

        Map<String, Double> categorySums = dbHelper.getCategorySumsInRange(from, to);
        if (categorySums.isEmpty()) {
            categorySumsTv.setText(getString(R.string.reports_category_sum_placeholder));
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Double> entry : categorySums.entrySet()) {
            String line = getString(
                    R.string.reports_category_sum_item,
                    entry.getKey(),
                    formatPln(entry.getValue())
            );
            sb.append("• ")
              .append(line)
              .append("\n");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        categorySumsTv.setText(sb.toString());
    }

    private void setupBarChart() {
        if (barChart == null) return;

        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setFitBars(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        String[] monthsShort = getResources().getStringArray(R.array.months_short_names);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(monthsShort));

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setGranularity(1f);
        barChart.getAxisRight().setEnabled(false);

        int nightModeFlags = requireContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;

        int textColor = isDarkMode
                ? ContextCompat.getColor(requireContext(), android.R.color.white)
                : ContextCompat.getColor(requireContext(), android.R.color.black);

        Legend legend = barChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextColor(textColor);

        xAxis.setTextColor(textColor);
        leftAxis.setTextColor(textColor);

        barChart.setNoDataText(getString(R.string.reports_bar_chart_no_data));
        barChart.setNoDataTextColor(textColor);
    }

    private void updateBarChartForYear(int year) {
        if (barChart == null || dbHelper == null || depositDbHelper == null) return;

        List<ReportsChartCalculator.MonthlyExpenseDepositStat> stats =
                ReportsChartCalculator.getMonthlyStatsForYear(dbHelper, depositDbHelper, year);

        List<BarEntry> expensesEntries = new ArrayList<>();
        List<BarEntry> depositEntries = new ArrayList<>();

        for (int i = 0; i < stats.size(); i++) {
            ReportsChartCalculator.MonthlyExpenseDepositStat s = stats.get(i);
            int xIndex = s.getMonth() - 1; // 0-11
            expensesEntries.add(new BarEntry(xIndex, (float) s.getExpenseTotal()));
            depositEntries.add(new BarEntry(xIndex, (float) s.getDepositReturnedTotal()));
        }

        BarDataSet expensesSet = new BarDataSet(expensesEntries, getString(R.string.reports_label_expenses));
        BarDataSet depositsSet = new BarDataSet(depositEntries, getString(R.string.reports_label_deposits));

        int colorExpenses = ContextCompat.getColor(requireContext(), R.color.chart_expenses);
        int colorDeposits = ContextCompat.getColor(requireContext(), R.color.chart_deposits);
        expensesSet.setColor(colorExpenses);
        depositsSet.setColor(colorDeposits);

        int nightModeFlags = requireContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        int valueTextColor = isDarkMode
                ? ContextCompat.getColor(requireContext(), android.R.color.white)
                : ContextCompat.getColor(requireContext(), android.R.color.black);

        expensesSet.setValueTextColor(valueTextColor);
        depositsSet.setValueTextColor(valueTextColor);
        expensesSet.setValueTextSize(10f);
        depositsSet.setValueTextSize(10f);

        ValueFormatter valueFormatter = new ValueFormatter() {
            private final DecimalFormat df = new DecimalFormat("0.00");
            @Override
            public String getBarLabel(BarEntry barEntry) {
                return df.format(barEntry.getY());
            }
        };
        expensesSet.setValueFormatter(valueFormatter);
        depositsSet.setValueFormatter(valueFormatter);

        expensesSet.setHighLightColor(colorExpenses);
        depositsSet.setHighLightColor(colorDeposits);
        expensesSet.setHighLightAlpha(255);
        depositsSet.setHighLightAlpha(255);

        float groupSpace = 0.15f;
        float barSpace = 0.02f;
        float barWidth = 0.4f;

        BarData data = new BarData(expensesSet, depositsSet);
        data.setBarWidth(barWidth);
        barChart.setData(data);

        int groupCount = 12;
        float startX = 0f;
        float groupWidth = data.getGroupWidth(groupSpace, barSpace);
        barChart.getXAxis().setAxisMinimum(startX);
        barChart.getXAxis().setAxisMaximum(startX + groupWidth * groupCount);
        barChart.groupBars(startX, groupSpace, barSpace);

        barChart.invalidate();
    }

    private void updateCategoryPie(String from, String to) {
        if (dbHelper == null || categoryPieChart == null) return;

        Map<String, Double> categorySums = dbHelper.getCategorySumsInRange(from, to);
        if (categorySums.isEmpty()) {
            categoryPieChart.clear();
            categoryPieChart.setNoDataText(getString(R.string.reports_category_sum_placeholder));
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categorySums.entrySet()) {
            float value = (float) entry.getValue().doubleValue();
            if (value <= 0f) continue;
            entries.add(new PieEntry(value, entry.getKey()));
        }

        if (entries.isEmpty()) {
            categoryPieChart.clear();
            categoryPieChart.setNoDataText(getString(R.string.reports_category_sum_placeholder));
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        int nightModeFlags = requireContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        int textColor = isDarkMode
                ? ContextCompat.getColor(requireContext(), android.R.color.white)
                : ContextCompat.getColor(requireContext(), android.R.color.black);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(10f);
        data.setValueTextColor(textColor);

        categoryPieChart.setUsePercentValues(true);
        categoryPieChart.getDescription().setEnabled(false);
        categoryPieChart.setDrawHoleEnabled(true);
        categoryPieChart.setHoleColor(android.R.color.transparent);
        categoryPieChart.setTransparentCircleRadius(55f);

        Legend legend = categoryPieChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextColor(textColor);

        categoryPieChart.setEntryLabelColor(textColor);
        categoryPieChart.setData(data);
        categoryPieChart.invalidate();
    }

    private void exportReportToCsv() {
        if (lastFrom == null || lastTo == null) {
            Toast.makeText(requireContext(), R.string.reports_error_date_range, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (downloads == null) throw new IllegalStateException("Brak katalogu Pobrane");
            if (!downloads.exists() && !downloads.mkdirs()) {
                throw new IllegalStateException("Nie udało się utworzyć katalogu Pobrane: " + downloads.getAbsolutePath());
            }

            String fileName = "raport_ekologiczny_" + lastFrom + "_" + lastTo + ".csv";
            fileName = fileName.replace(":", "-");
            File outFile = new File(downloads, fileName);

            Log.d(TAG, "Eksport raportu do pliku: " + outFile.getAbsolutePath());

            FileOutputStream fos = new FileOutputStream(outFile);
            OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");

            // Wszystkie teksty z resources - żadnych hardcoded stringów!
            writer.write(getString(R.string.csv_report_header) + ";" +
                        getString(R.string.csv_period_from) + ";" + lastFrom + ";" +
                        getString(R.string.csv_period_to) + ";" + lastTo + "\n");
            writer.write(getString(R.string.csv_metric_label) + ";" +
                        getString(R.string.csv_value_label) + "\n");
            writer.write(getString(R.string.csv_sum_expenses) + ";" + lastSum + "\n");
            writer.write(getString(R.string.csv_avg_price) + ";" + lastAvg + "\n");
            writer.write(getString(R.string.csv_sum_deposits) + ";" + lastDepositSum + "\n");
            writer.write(getString(R.string.csv_expired_products) + ";" + lastExpired + "\n\n");

            writer.write(getString(R.string.csv_category_expenses) + ";" +
                        getString(R.string.csv_category_sum) + "\n");
            if (lastCategorySums != null) {
                for (Map.Entry<String, Double> e : lastCategorySums.entrySet()) {
                    writer.write(e.getKey() + ";" + e.getValue() + "\n");
                }
            }
            writer.flush();
            writer.close();

            Uri uri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    outFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, getString(R.string.reports_export_csv)));

        } catch (Exception e) {
            Log.e(TAG, "CSV export failed: " + e.getMessage(), e);
            Toast.makeText(requireContext(), R.string.reports_export_csv_error, Toast.LENGTH_LONG).show();
        }
    }

    private void exportReportToPdf() {
        if (lastFrom == null || lastTo == null) {
            Toast.makeText(requireContext(), R.string.reports_error_date_range, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String fileName = "raport_ekologiczny_" + lastFrom + "_" + lastTo + ".pdf";
            fileName = fileName.replace(":" , "-");

            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Zapis przez MediaStore do Pobranych
                ContentResolver resolver = requireContext().getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    throw new IllegalStateException("Nie udało się utworzyć wpisu w MediaStore dla PDF");
                }

                PdfDocument document = createPdfDocument();
                try (java.io.OutputStream os = resolver.openOutputStream(uri)) {
                    if (os == null) throw new IllegalStateException("Brak strumienia wyjściowego dla PDF");
                    document.writeTo(os);
                }
                document.close();

                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                resolver.update(uri, values, null, null);
            } else {
                // Starsze API: zapis bezpośrednio do katalogu Pobrane
                File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (downloads == null) throw new IllegalStateException("Brak katalogu Pobrane");
                if (!downloads.exists() && !downloads.mkdirs()) {
                    throw new IllegalStateException("Nie udało się utworzyć katalogu Pobrane: " + downloads.getAbsolutePath());
                }
                File outFile = new File(downloads, fileName);

                PdfDocument document = createPdfDocument();
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    document.writeTo(fos);
                }
                document.close();

                uri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        outFile
                );
            }

            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setDataAndType(uri, "application/pdf");
            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (viewIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(viewIntent);
            } else {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("application/pdf");
                share.putExtra(Intent.EXTRA_STREAM, uri);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(share, getString(R.string.reports_share_pdf_title)));
            }

        } catch (Exception e) {
            Log.e(TAG, "PDF export failed: " + e.getMessage(), e);
            Toast.makeText(requireContext(), R.string.reports_export_pdf_error, Toast.LENGTH_LONG).show();
        }
    }

    private void exportReportToHtml() {
        if (lastFrom == null || lastTo == null) {
            Toast.makeText(requireContext(), R.string.reports_error_date_range, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (downloads == null) throw new IllegalStateException("Brak katalogu Pobrane");
            if (!downloads.exists() && !downloads.mkdirs()) {
                throw new IllegalStateException("Nie udało się utworzyć katalogu Pobrane: " + downloads.getAbsolutePath());
            }

            String fileName = "raport_ekologiczny_" + lastFrom + "_" + lastTo + ".html";
            fileName = fileName.replace(":", "-");
            File outFile = new File(downloads, fileName);

            Log.d(TAG, "Eksport HTML do pliku: " + outFile.getAbsolutePath());

            FileOutputStream fos = new FileOutputStream(outFile);
            OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");

            String html = buildHtmlReport();
            writer.write(html);
            writer.flush();
            writer.close();

            Uri uri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    outFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/html");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                    getString(R.string.reports_email_subject_html, lastFrom, lastTo));

            startActivity(Intent.createChooser(shareIntent, getString(R.string.reports_export_html_title)));

        } catch (Exception e) {
            Log.e(TAG, "HTML export failed: " + e.getMessage(), e);
            Toast.makeText(requireContext(), R.string.reports_export_html_error, Toast.LENGTH_LONG).show();
        }
    }

    private String buildHtmlReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n")
          .append("<html lang=\"pl\">\n")
          .append("<head>\n")
          .append("  <meta charset=\"UTF-8\"/>\n")
          .append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n")
          .append("  <title>").append(getString(R.string.pdf_title)).append("</title>\n")
          .append("  <style>\n")
          .append("    body { font-family: -apple-system, Roboto, Arial, sans-serif; margin: 16px; background-color: #fafafa; color: #222; }\n")
          .append("    h1 { font-size: 20px; margin-bottom: 4px; }\n")
          .append("    h2 { font-size: 16px; margin-top: 16px; margin-bottom: 4px; }\n")
          .append("    .section { background: #ffffff; border-radius: 8px; padding: 12px 16px; margin-bottom: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }\n")
          .append("    .muted { color: #666; font-size: 12px; }\n")
          .append("    ul { padding-left: 20px; }\n")
          .append("    table { width: 100%; border-collapse: collapse; margin-top: 8px; }\n")
          .append("    th, td { border-bottom: 1px solid #ddd; padding: 6px 4px; text-align: left; font-size: 12px; }\n")
          .append("    th { background-color: #f0f0f0; }\n")
          .append("  </style>\n")
          .append("</head>\n")
          .append("<body>\n");

        // Nagłówek
        sb.append("<div class=\"section\">\n")
          .append("  <h1>").append(getString(R.string.pdf_title)).append("</h1>\n")
          .append("  <div class=\"muted\">")
          .append(getString(R.string.pdf_range_label, lastFrom, lastTo))
          .append("</div>\n")
          .append("</div>\n");

        // Podsumowanie liczbowe
        sb.append("<div class=\"section\">\n")
          .append("  <h2>").append(getString(R.string.reports_monthly_sum_title)).append("</h2>\n")
          .append("  <p>")
          .append(getString(R.string.pdf_sum_expenses, formatPln(lastSum)))
          .append("<br/>")
          .append(getString(R.string.pdf_sum_deposits, formatPln(lastDepositSum)))
          .append("<br/>")
          .append(getString(R.string.pdf_avg_price, formatPln(lastAvg)))
          .append("<br/>")
          .append(getString(R.string.pdf_expired_count, lastExpired))
          .append("</p>\n")
          .append("</div>\n");

        // Wydatki wg kategorii
        sb.append("<div class=\"section\">\n")
          .append("  <h2>").append(getString(R.string.reports_category_sum_title)).append("</h2>\n");
        if (lastCategorySums != null && !lastCategorySums.isEmpty()) {
            sb.append("  <table>\n")
              .append("    <thead><tr><th>")
              .append(getString(R.string.csv_category_expenses))
              .append("</th><th style=\"text-align:right;\">")
              .append(getString(R.string.csv_category_sum))
              .append("</th></tr></thead>\n")
              .append("    <tbody>\n");
            for (Map.Entry<String, Double> entry : lastCategorySums.entrySet()) {
                sb.append("      <tr><td>")
                  .append(entry.getKey())
                  .append("</td><td style=\"text-align:right;\">")
                  .append(formatPln(entry.getValue()))
                  .append("</td></tr>\n");
            }
            sb.append("    </tbody>\n  </table>\n");
        } else {
            sb.append("  <p class=\"muted\">")
              .append(getString(R.string.reports_category_sum_placeholder))
              .append("</p>\n");
        }
        sb.append("</div>\n");

        sb.append("</body>\n</html>\n");
        return sb.toString();
    }

    private PdfDocument createPdfDocument() {
        PdfDocument document = new PdfDocument();

        int pageWidth = 595;
        int pageHeight = 842;
        int margin = 40;

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        int y = margin;

        paint.setTextSize(18f);
        paint.setFakeBoldText(true);
        canvas.drawText(getString(R.string.pdf_title), margin, y, paint);

        paint.setFakeBoldText(false);
        paint.setTextSize(12f);
        y += 25;
        canvas.drawText(getString(R.string.pdf_range_label, lastFrom, lastTo), margin, y, paint);
        y += 18;

        y += 10;
        canvas.drawText(getString(R.string.pdf_sum_expenses, formatPln(lastSum)), margin, y, paint);
        y += 16;
        canvas.drawText(getString(R.string.pdf_sum_deposits, formatPln(lastDepositSum)), margin, y, paint);
        y += 16;
        canvas.drawText(getString(R.string.pdf_avg_price, formatPln(lastAvg)), margin, y, paint);
        y += 16;
        canvas.drawText(getString(R.string.pdf_expired_count, lastExpired), margin, y, paint);
        y += 24;

        if (lastCategorySums != null && !lastCategorySums.isEmpty()) {
            canvas.drawText(getString(R.string.pdf_category_header), margin, y, paint);
            y += 18;
            for (Map.Entry<String, Double> entry : lastCategorySums.entrySet()) {
                String line = "• " + entry.getKey() + ": " + formatPln(entry.getValue());
                canvas.drawText(line, margin, y, paint);
                y += 16;
                if (y > pageHeight - 240) { // zostawiamy trochę więcej miejsca na wykresy
                    document.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = margin;
                }
            }
            y += 10;
        }

        // stały "maksymalny" rozmiar wykresu w jednostkach PDF, z zachowaniem proporcji
        int chartAreaWidth = pageWidth - 2 * margin;
        int chartAreaHeight = 220; // trochę wyższe niż wcześniej, ale nadal w granicach strony

        // Wykres słupkowy
        Bitmap barBitmap = createChartBitmap(barChart, chartAreaWidth, chartAreaHeight);
        if (barBitmap != null) {
            int scaledBarWidth = chartAreaWidth;
            int scaledBarHeight = (int) (barBitmap.getHeight() * (chartAreaWidth / (float) barBitmap.getWidth()));
            if (scaledBarHeight > chartAreaHeight) {
                float ratio = chartAreaHeight / (float) scaledBarHeight;
                scaledBarHeight = chartAreaHeight;
                scaledBarWidth = (int) (scaledBarWidth * ratio);
            }

            if (y + scaledBarHeight + 40 > pageHeight - margin) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = margin;
            }

            int barX = margin + (chartAreaWidth - scaledBarWidth) / 2; // wyśrodkowanie

            canvas.drawText(getString(R.string.pdf_bar_chart_title), margin, y, paint);
            y += 18;

            Rect srcRect = new Rect(0, 0, barBitmap.getWidth(), barBitmap.getHeight());
            Rect destRect = new Rect(barX, y, barX + scaledBarWidth, y + scaledBarHeight);
            canvas.drawBitmap(barBitmap, srcRect, destRect, null);
            y += scaledBarHeight + 24;
        }

        // Wykres kołowy
        Bitmap pieBitmap = createChartBitmap(categoryPieChart, chartAreaWidth, chartAreaHeight);
        if (pieBitmap != null) {
            int scaledPieWidth = chartAreaWidth;
            int scaledPieHeight = (int) (pieBitmap.getHeight() * (chartAreaWidth / (float) pieBitmap.getWidth()));
            if (scaledPieHeight > chartAreaHeight) {
                float ratio = chartAreaHeight / (float) scaledPieHeight;
                scaledPieHeight = chartAreaHeight;
                scaledPieWidth = (int) (scaledPieWidth * ratio);
            }

            if (y + scaledPieHeight + 40 > pageHeight - margin) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = margin;
            }

            int pieX = margin + (chartAreaWidth - scaledPieWidth) / 2;

            canvas.drawText(getString(R.string.pdf_pie_chart_title), margin, y, paint);
            y += 18;

            Rect srcRectPie = new Rect(0, 0, pieBitmap.getWidth(), pieBitmap.getHeight());
            Rect destRectPie = new Rect(pieX, y, pieX + scaledPieWidth, y + scaledPieHeight);
            canvas.drawBitmap(pieBitmap, srcRectPie, destRectPie, null);
            y += scaledPieHeight + 24;
        }

        document.finishPage(page);
        return document;
    }

    private Bitmap createChartBitmap(com.github.mikephil.charting.charts.Chart<?> chart, int width, int height) {
        if (chart == null) return null;

        int specWidth = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int specHeight = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
        chart.measure(specWidth, specHeight);
        chart.layout(0, 0, width, height);

        // Wariant do PDF: małe, ciemne napisy (jak tekst w raporcie), żeby były widoczne, ale nie dominowały
        int black = 0xFF000000;
        float tinyTextSize = 4f;      // ogólny rozmiar małych napisów
        float legendTextSize = 3f;    // jeszcze mniejsza legenda

        if (chart instanceof BarChart) {
            BarChart bc = (BarChart) chart;

            // Osie
            XAxis xAxis = bc.getXAxis();
            xAxis.setTextColor(black);
            xAxis.setTextSize(tinyTextSize);
            xAxis.setDrawGridLines(false);

            YAxis left = bc.getAxisLeft();
            left.setTextColor(black);
            left.setTextSize(tinyTextSize);
            left.setDrawGridLines(false);

            YAxis right = bc.getAxisRight();
            right.setTextColor(black);
            right.setTextSize(tinyTextSize);
            right.setDrawGridLines(false);

            // Legenda
            Legend legend = bc.getLegend();
            legend.setEnabled(true);
            legend.setTextColor(black);
            legend.setTextSize(legendTextSize);

            // Wartości nad słupkami
            if (bc.getData() != null) {
                for (com.github.mikephil.charting.interfaces.datasets.IBarDataSet set : bc.getData().getDataSets()) {
                    set.setValueTextColor(black);
                    set.setValueTextSize(tinyTextSize);
                    set.setDrawValues(true);
                }
            }
        } else if (chart instanceof PieChart) {
            PieChart pc = (PieChart) chart;

            // Legenda
            Legend legend = pc.getLegend();
            legend.setEnabled(true);
            legend.setTextColor(black);
            legend.setTextSize(legendTextSize);

            // Etykiety przy segmentach
            pc.setDrawEntryLabels(true);
            pc.setEntryLabelColor(black);
            pc.setEntryLabelTextSize(tinyTextSize);

            if (pc.getData() != null && pc.getData().getDataSet() != null) {
                pc.getData().getDataSet().setDrawValues(true);
                pc.getData().getDataSet().setValueTextColor(black);
                pc.getData().getDataSet().setValueTextSize(tinyTextSize);
            }
            // W PDF lepiej pokazać wartości surowe, nie procenty
            pc.setUsePercentValues(false);
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0xFFFFFFFF); // białe tło
        chart.draw(canvas);
        return bitmap;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dbHelper != null) { dbHelper.close(); dbHelper = null; }
        if (depositDbHelper != null) { depositDbHelper.close(); depositDbHelper = null; }
        barChart = null;
        categoryPieChart = null;
    }
}
