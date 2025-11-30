package com.example.asystent_ekologiczny;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Currency;
import java.util.Locale;

/**
 * Raporty: obliczanie sumy wydatków miesięcznie.
 */
public class ReportsFragment extends Fragment {

    public static final String TAG = "ReportsFragment";

    private ProductDbHelper dbHelper;
    private DepositDbHelper depositDbHelper;

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

        // Konfiguracja spinnera miesięcy
        ArrayAdapter<CharSequence> monthsAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.months_names, android.R.layout.simple_spinner_item);
        monthsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthsAdapter);

        // Konfiguracja spinnera lat (ostatnie 10 lat od bieżącego w dół)
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int currentYear = cal.get(java.util.Calendar.YEAR);
        java.util.List<String> years = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) { years.add(String.valueOf(currentYear - i)); }
        ArrayAdapter<String> yearsAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, years);
        yearsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearsAdapter);
        yearSpinner.setSelection(0); // bieżący rok na górze

        int currentMonthIndex = cal.get(java.util.Calendar.MONTH); // 0-11
        monthSpinner.setSelection(currentMonthIndex);

        // Automatyczne przeliczenie dla bieżącego miesiąca
        int currentMonth = currentMonthIndex + 1; // 1-12
        double initialSum = dbHelper.getMonthlySum(currentYear, currentMonth);
        double initialDepositSum = depositDbHelper.sumReturnedValueInMonth(currentYear, currentMonth);
        resultTv.setText(getString(R.string.reports_monthly_sum_result, formatPln(initialSum)));
        depositResultTv.setText(getString(R.string.reports_deposit_sum_result, formatPln(initialDepositSum)));

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dbHelper != null) { dbHelper.close(); dbHelper = null; }
        if (depositDbHelper != null) { depositDbHelper.close(); depositDbHelper = null; }
    }
}
