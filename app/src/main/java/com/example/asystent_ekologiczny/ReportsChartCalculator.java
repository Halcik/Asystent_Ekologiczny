package com.example.asystent_ekologiczny;

import java.util.ArrayList;
import java.util.List;

/**
 * Pomocnicza klasa do wyliczania miesięcznych statystyk (wydatki vs odzyskane kaucje)
 * dla potrzeb wykresu słupkowego w ReportsFragment.
 */
public class ReportsChartCalculator {

    public static class MonthlyExpenseDepositStat {
        private final int year;
        private final int month; // 1-12
        private final double expenseTotal;
        private final double depositReturnedTotal;

        public MonthlyExpenseDepositStat(int year, int month, double expenseTotal, double depositReturnedTotal) {
            this.year = year;
            this.month = month;
            this.expenseTotal = expenseTotal;
            this.depositReturnedTotal = depositReturnedTotal;
        }

        public int getYear() {
            return year;
        }

        public int getMonth() {
            return month;
        }

        public double getExpenseTotal() {
            return expenseTotal;
        }

        public double getDepositReturnedTotal() {
            return depositReturnedTotal;
        }
    }

    /**
     * Zwraca listę 12 elementów, po jednym na każdy miesiąc danego roku (1-12).
     * Jeśli w danym miesiącu brak danych, sumy będą równe 0.
     */
    public static List<MonthlyExpenseDepositStat> getMonthlyStatsForYear(ProductDbHelper productsHelper,
                                                                         DepositDbHelper depositHelper,
                                                                         int year) {
        List<MonthlyExpenseDepositStat> result = new ArrayList<>();
        if (productsHelper == null || depositHelper == null) {
            return result;
        }
        for (int month = 1; month <= 12; month++) {
            double expenses = productsHelper.getMonthlySum(year, month);
            double deposits = depositHelper.sumReturnedValueInMonth(year, month);
            result.add(new MonthlyExpenseDepositStat(year, month, expenses, deposits));
        }
        return result;
    }
}

