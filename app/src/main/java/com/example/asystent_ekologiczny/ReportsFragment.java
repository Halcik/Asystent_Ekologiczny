package com.example.asystent_ekologiczny;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * Raporty: obliczanie sumy wydatków miesięcznie i wykres słupkowy wydatki vs odzyskane kaucje.
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

    private String formatPln(double value) {
        // Użyj lokalnych separatorów, ale poprzedź wartość "PLN " ze zwykłą spacją
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

        Spinner yearSpinner = root.findViewById(R.id.spinner_year);
        Spinner monthSpinner = root.findViewById(R.id.spinner_month);
        Button calcBtn = root.findViewById(R.id.btn_calculate_monthly_sum);
        TextView resultTv = root.findViewById(R.id.tv_monthly_sum);
        TextView depositResultTv = root.findViewById(R.id.tv_deposit_sum);
        barChart = root.findViewById(R.id.bar_chart_expenses_vs_deposits);
        categorySumsTv = root.findViewById(R.id.tv_category_sums);
        avgPriceTv = root.findViewById(R.id.tv_monthly_avg_price);
        expiredCountTv = root.findViewById(R.id.tv_expired_count);
        categoryPieChart = root.findViewById(R.id.pie_chart_categories);

        // Konfiguracja spinnera miesięcy
        ArrayAdapter<CharSequence> monthsAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.months_names, android.R.layout.simple_spinner_item);
        monthsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthsAdapter);

        // Konfiguracja spinnera lat (ostatnie 10 lat od bieżącego w dół)
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        List<String> years = new ArrayList<>();
        for (int i = 0; i < 10; i++) { years.add(String.valueOf(currentYear - i)); }
        ArrayAdapter<String> yearsAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, years);
        yearsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearsAdapter);
        yearSpinner.setSelection(0); // bieżący rok na górze

        int currentMonthIndex = cal.get(Calendar.MONTH); // 0-11
        monthSpinner.setSelection(currentMonthIndex);

        // Automatyczne przeliczenie dla bieżącego miesiąca
        int currentMonth = currentMonthIndex + 1; // 1-12
        double initialSum = dbHelper.getMonthlySum(currentYear, currentMonth);
        double initialDepositSum = depositDbHelper.sumReturnedValueInMonth(currentYear, currentMonth);
        double initialAvg = dbHelper.getMonthlyAveragePrice(currentYear, currentMonth);
        int initialExpired = dbHelper.getExpiredProductsCountForMonth(currentYear, currentMonth);
        resultTv.setText(getString(R.string.reports_monthly_sum_result, formatPln(initialSum)));
        depositResultTv.setText(getString(R.string.reports_deposit_sum_result, formatPln(initialDepositSum)));
        avgPriceTv.setText(getString(R.string.reports_monthly_avg_result, formatPln(initialAvg)));
        expiredCountTv.setText(getString(R.string.reports_expired_result, initialExpired));

        // Konfiguracja wykresu i początkowe dane
        setupBarChart();
        updateBarChartForYear(currentYear);
        updateCategoryReport(currentYear, currentMonth);
        updateCategoryPie(currentYear, currentMonth);

        // Zmiana roku -> odśwież wykres (raport kategorii zostaje powiązany z przyciskiem Oblicz)
        yearSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedYearStr = (String) yearSpinner.getItemAtPosition(position);
                try {
                    int year = Integer.parseInt(selectedYearStr);
                    updateBarChartForYear(year);
                } catch (NumberFormatException ignored) { }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // nic
            }
        });

        calcBtn.setOnClickListener(v -> {
            try {
                String selectedYearStr = (String) yearSpinner.getSelectedItem();
                int year = Integer.parseInt(selectedYearStr);
                int month = monthSpinner.getSelectedItemPosition() + 1; // 1-12
                double sum = dbHelper.getMonthlySum(year, month);
                double depSum = depositDbHelper.sumReturnedValueInMonth(year, month);
                double avg = dbHelper.getMonthlyAveragePrice(year, month);
                int expired = dbHelper.getExpiredProductsCountForMonth(year, month);
                resultTv.setText(getString(R.string.reports_monthly_sum_result, formatPln(sum)));
                depositResultTv.setText(getString(R.string.reports_deposit_sum_result, formatPln(depSum)));
                avgPriceTv.setText(getString(R.string.reports_monthly_avg_result, formatPln(avg)));
                expiredCountTv.setText(getString(R.string.reports_expired_result, expired));

                // Odśwież raport kategorii dla wybranego okresu
                updateCategoryReport(year, month);
                updateCategoryPie(year, month);
            } catch (Exception ex) {
                resultTv.setText(getString(R.string.reports_error_number_format));
                depositResultTv.setText(getString(R.string.reports_error_number_format));
                avgPriceTv.setText(getString(R.string.reports_monthly_avg_placeholder));
                expiredCountTv.setText(getString(R.string.reports_expired_placeholder));
                if (categoryPieChart != null) {
                    categoryPieChart.clear();
                }
            }
        });

        return root;
    }

    private void updateCategoryReport(int year, int month) {
        if (dbHelper == null || categorySumsTv == null) return;

        Map<String, Double> categorySums = dbHelper.getCategorySumsForMonth(year, month);
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

        // Oś X - miesiące
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        String[] monthsShort = getResources().getStringArray(R.array.months_short_names);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(monthsShort));

        // Oś Y - tylko lewa
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setGranularity(1f);
        barChart.getAxisRight().setEnabled(false);

        // Rozpoznanie motywu (jasny / ciemny)
        int nightModeFlags = requireContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;

        int textColor = isDarkMode
                ? ContextCompat.getColor(requireContext(), android.R.color.white)
                : ContextCompat.getColor(requireContext(), android.R.color.black);

        // Legenda
        Legend legend = barChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextColor(textColor);

        // Kolory opisów osi
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

        // Kolor wartości nad słupkami zależny od motywu (jasny/ciemny)
        int nightModeFlags = requireContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        int valueTextColor = isDarkMode
                ? ContextCompat.getColor(requireContext(), android.R.color.white)
                : ContextCompat.getColor(requireContext(), android.R.color.black);

        expensesSet.setValueTextColor(valueTextColor);
        depositsSet.setValueTextColor(valueTextColor);
        expensesSet.setValueTextSize(10f);
        depositsSet.setValueTextSize(10f);

        // Uproszczone formatowanie wartości (2 miejsca po przecinku)
        ValueFormatter valueFormatter = new ValueFormatter() {
            private final DecimalFormat df = new DecimalFormat("0.00");
            @Override
            public String getBarLabel(BarEntry barEntry) {
                return df.format(barEntry.getY());
            }
        };
        expensesSet.setValueFormatter(valueFormatter);
        depositsSet.setValueFormatter(valueFormatter);

        // Wyłączanie zmiany koloru przy zaznaczeniu (highlight)
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

        // Zakres X dla 12 miesięcy pogrupowanych
        int groupCount = 12;
        float startX = 0f;
        float groupWidth = data.getGroupWidth(groupSpace, barSpace);
        barChart.getXAxis().setAxisMinimum(startX);
        barChart.getXAxis().setAxisMaximum(startX + groupWidth * groupCount);
        barChart.groupBars(startX, groupSpace, barSpace);

        barChart.invalidate();
    }

    private void updateCategoryPie(int year, int month) {
        if (dbHelper == null || categoryPieChart == null) return;

        Map<String, Double> categorySums = dbHelper.getCategorySumsForMonth(year, month);
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

        // Rozpoznanie motywu (jasny / ciemny) dla koloru tekstu legendy i wartości
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
    }
}
