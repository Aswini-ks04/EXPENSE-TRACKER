package com.expensetracker.service;

import com.expensetracker.model.Category;

public class CategorySummary {
    private final Category category;
    private final double totalAmount;
    private final int count;
    private final double percentage;

    public CategorySummary(Category category, double totalAmount, int count, double percentage) {
        this.category = category;
        this.totalAmount = totalAmount;
        this.count = count;
        this.percentage = percentage;
    }

    public Category getCategory() {
        return category;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public int getCount() {
        return count;
    }

    public double getPercentage() {
        return percentage;
    }

    public String getFormattedTotal() {
        return String.format(java.util.Locale.US, "₹%.2f", totalAmount);
    }

    public String getFormattedPercentage() {
        return String.format("%.1f%%", percentage);
    }

    public String getProgressBar(int barLength) {
        int filled = (int) Math.round((percentage / 100.0) * barLength);
        filled = Math.max(0, Math.min(barLength, filled));
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < filled; i++) {
            sb.append("#");
        }
        for (int i = filled; i < barLength; i++) {
            sb.append("-");
        }
        sb.append("]");
        return sb.toString();
    }
}
