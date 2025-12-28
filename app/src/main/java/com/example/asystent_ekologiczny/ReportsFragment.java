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
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Raporty: obliczanie sumy wydatków miesięcznie i wykres słupkowy wydatki vs odzyskane kaucje.
 */
public class ReportsFragment extends Fragment {

    public static final String TAG = "ReportsFragment";

    private ProductDbHelper dbHelper;
    private DepositDbHelper depositDbHelper;
    private BarChart barChart;

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
        resultTv.setText(getString(R.string.reports_monthly_sum_result, formatPln(initialSum)));
        depositResultTv.setText(getString(R.string.reports_deposit_sum_result, formatPln(initialDepositSum)));

        // Konfiguracja wykresu i początkowe dane
        setupBarChart();
        updateBarChartForYear(currentYear);

        // Zmiana roku -> odśwież wykres
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
                resultTv.setText(getString(R.string.reports_monthly_sum_result, formatPln(sum)));
                depositResultTv.setText(getString(R.string.reports_deposit_sum_result, formatPln(depSum)));
            } catch (Exception ex) {
                resultTv.setText(getString(R.string.reports_error_number_format));
                depositResultTv.setText(getString(R.string.reports_error_number_format));
            }
        });

        return root;
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

        int textColor;
        if (isDarkMode) {
            textColor = ContextCompat.getColor(requireContext(), android.R.color.white);
        } else {
            textColor = ContextCompat.getColor(requireContext(), android.R.color.black);
        }

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dbHelper != null) { dbHelper.close(); dbHelper = null; }
        if (depositDbHelper != null) { depositDbHelper.close(); depositDbHelper = null; }
        barChart = null;
    }
}
