package com.expensetracker.model;

import java.time.YearMonth;

public class Budget {
    public enum Status {
        ON_TRACK("On Track", "✅"),
        WARNING("Near Limit (>=80%)", "⚠️"),
        EXCEEDED("Budget Exceeded", "🚨");

        private final String label;
        private final String icon;

        Status(String label, String icon) {
            this.label = label;
            this.icon = icon;
        }

        public String getLabel() {
            return label;
        }

        public String getIcon() {
            return icon;
        }
    }

    private final YearMonth month;
    private double monthlyLimit;
    private double currentSpent;

    public Budget(YearMonth month, double monthlyLimit, double currentSpent) {
        this.month = month;
        this.monthlyLimit = Math.max(0, monthlyLimit);
        this.currentSpent = Math.max(0, currentSpent);
    }

    public YearMonth getMonth() {
        return month;
    }

    public double getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(double monthlyLimit) {
        this.monthlyLimit = Math.max(0, monthlyLimit);
    }

    public double getCurrentSpent() {
        return currentSpent;
    }

    public void setCurrentSpent(double currentSpent) {
        this.currentSpent = currentSpent;
    }

    public double getRemaining() {
        return monthlyLimit - currentSpent;
    }

    public double getPercentageUsed() {
        if (monthlyLimit <= 0) return 0.0;
        return (currentSpent / monthlyLimit) * 100.0;
    }

    public Status getStatus() {
        if (monthlyLimit <= 0) {
            return Status.ON_TRACK;
        }
        if (currentSpent > monthlyLimit) {
            return Status.EXCEEDED;
        } else if (currentSpent >= monthlyLimit * 0.8) {
            return Status.WARNING;
        }
        return Status.ON_TRACK;
    }

    public String getFormattedLimit() {
        return String.format(java.util.Locale.US, "₹%.2f", monthlyLimit);
    }

    public String getFormattedSpent() {
        return String.format(java.util.Locale.US, "₹%.2f", currentSpent);
    }

    public String getFormattedRemaining() {
        double rem = getRemaining();
        if (rem < 0) {
            return String.format(java.util.Locale.US, "-₹%.2f (over budget)", Math.abs(rem));
        }
        return String.format(java.util.Locale.US, "₹%.2f", rem);
    }
}
