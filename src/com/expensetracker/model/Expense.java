package com.expensetracker.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Expense {
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String id;
    private LocalDate date;
    private String title;
    private Category category;
    private double amount;
    private PaymentMethod paymentMethod;
    private String notes;

    public Expense() {
        this.date = LocalDate.now();
        this.category = Category.OTHER;
        this.paymentMethod = PaymentMethod.CASH;
        this.notes = "";
    }

    public Expense(String id, LocalDate date, String title, Category category, double amount, 
                   PaymentMethod paymentMethod, String notes) {
        this.id = id;
        this.date = date != null ? date : LocalDate.now();
        this.title = title != null ? title.trim() : "Untitled Expense";
        this.category = category != null ? category : Category.OTHER;
        this.amount = Math.max(0, amount);
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.CASH;
        this.notes = notes != null ? notes.trim() : "";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = Math.max(0, amount);
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getFormattedDate() {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    public String getFormattedAmount() {
        return String.format(java.util.Locale.US, "₹%.2f", amount);
    }

    /**
     * Converts expense to a CSV record line.
     */
    public String toCsvLine() {
        return String.format("%s,%s,%s,%s,%.2f,%s,%s",
                escapeCsv(id),
                escapeCsv(getFormattedDate()),
                escapeCsv(title),
                escapeCsv(category.name()),
                amount,
                escapeCsv(paymentMethod.name()),
                escapeCsv(notes));
    }

    /**
     * Helper to escape fields for CSV.
     */
    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Expense expense)) return false;
        return Objects.equals(id, expense.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %-20s | %-16s | %10s | %-12s | %s",
                id, getFormattedDate(), title, category.getDisplayName(),
                getFormattedAmount(), paymentMethod.getDisplayName(), notes);
    }
}
