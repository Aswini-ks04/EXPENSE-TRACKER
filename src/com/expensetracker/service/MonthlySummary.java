package com.expensetracker.service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class MonthlySummary {
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final YearMonth month;
    private final double totalAmount;
    private final int count;

    public MonthlySummary(YearMonth month, double totalAmount, int count) {
        this.month = month;
        this.totalAmount = totalAmount;
        this.count = count;
    }

    public YearMonth getMonth() {
        return month;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public int getCount() {
        return count;
    }

    public String getFormattedMonth() {
        return month.format(DISPLAY_FORMATTER);
    }

    public String getFormattedTotal() {
        return String.format(java.util.Locale.US, "₹%.2f", totalAmount);
    }
}
