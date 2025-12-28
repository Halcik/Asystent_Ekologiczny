package com.example.asystent_ekologiczny;

import android.app.DatePickerDialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

    private String formatPln(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return "PLN " + df.format(value);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_reports, container, false);
        dbHelper = new ProductDbHelper(requireContext());
        depositDbHelper = new DepositDbHelper(requireContext());

        dateFromTv = root.findViewById(R.id.tv_date_from);
        dateToTv = root.findViewById(R.id.tv_date_to);
        calcRangeBtn = root.findViewById(R.id.btn_calculate_range);
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

        return root;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dbHelper != null) { dbHelper.close(); dbHelper = null; }
        if (depositDbHelper != null) { depositDbHelper.close(); depositDbHelper = null; }
        barChart = null;
        categoryPieChart = null;
    }
}
