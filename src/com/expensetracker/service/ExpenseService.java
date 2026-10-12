package com.expensetracker.service;

import com.expensetracker.model.*;
import com.expensetracker.repository.ExpenseRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class ExpenseService {
    private final ExpenseRepository repository;

    public ExpenseService(ExpenseRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repository cannot be null");
    }

    public List<Expense> getAllExpenses() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(Expense::getDate).reversed())
                .collect(Collectors.toList());
    }

    public Optional<Expense> getExpenseById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        return repository.findById(id.trim());
    }

    public Expense addExpense(LocalDate date, String title, Category category, double amount,
                              PaymentMethod paymentMethod, String notes) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }

        String id = repository.generateNextId();
        Expense expense = new Expense(id, date, title.trim(), category, amount, paymentMethod, notes);
        return repository.save(expense);
    }

    public boolean updateExpense(Expense expense) {
        if (expense == null || expense.getId() == null) {
            return false;
        }
        return repository.update(expense);
    }

    public boolean deleteExpense(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        return repository.deleteById(id.trim());
    }

    public List<Expense> filterByDateRange(LocalDate from, LocalDate to) {
        return getAllExpenses().stream()
                .filter(e -> {
                    LocalDate d = e.getDate();
                    if (from != null && d.isBefore(from)) return false;
                    if (to != null && d.isAfter(to)) return false;
                    return true;
                })
                .collect(Collectors.toList());
    }

    public List<Expense> filterByCategory(Category category) {
        if (category == null) return getAllExpenses();
        return getAllExpenses().stream()
                .filter(e -> e.getCategory() == category)
                .collect(Collectors.toList());
    }

    public List<Expense> filterByPaymentMethod(PaymentMethod method) {
        if (method == null) return getAllExpenses();
        return getAllExpenses().stream()
                .filter(e -> e.getPaymentMethod() == method)
                .collect(Collectors.toList());
    }

    public List<Expense> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllExpenses();
        }
        String q = query.trim().toLowerCase();
        return getAllExpenses().stream()
                .filter(e -> e.getTitle().toLowerCase().contains(q) ||
                             e.getCategory().getDisplayName().toLowerCase().contains(q) ||
                             e.getPaymentMethod().getDisplayName().toLowerCase().contains(q) ||
                             e.getNotes().toLowerCase().contains(q) ||
                             e.getId().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    public double getTotalSpent() {
        return calculateTotal(getAllExpenses());
    }

    public double calculateTotal(List<Expense> expenses) {
        return expenses.stream()
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    public double getTotalSpentForMonth(YearMonth month) {
        return getAllExpenses().stream()
                .filter(e -> YearMonth.from(e.getDate()).equals(month))
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    public Budget getBudget(YearMonth month) {
        double limit = repository.getMonthlyBudget(month);
        double spent = getTotalSpentForMonth(month);
        return new Budget(month, limit, spent);
    }

    public void setBudget(YearMonth month, double limit) {
        repository.setMonthlyBudget(month, limit);
    }

    public List<CategorySummary> getCategorySummaries(List<Expense> expensesList) {
        double total = calculateTotal(expensesList);
        Map<Category, List<Expense>> grouped = expensesList.stream()
                .collect(Collectors.groupingBy(Expense::getCategory));

        List<CategorySummary> summaries = new ArrayList<>();
        for (Category cat : Category.values()) {
            List<Expense> list = grouped.getOrDefault(cat, Collections.emptyList());
            if (!list.isEmpty()) {
                double catTotal = list.stream().mapToDouble(Expense::getAmount).sum();
                double pct = total > 0 ? (catTotal / total) * 100.0 : 0.0;
                summaries.add(new CategorySummary(cat, catTotal, list.size(), pct));
            }
        }

        summaries.sort(Comparator.comparingDouble(CategorySummary::getTotalAmount).reversed());
        return summaries;
    }

    public List<MonthlySummary> getMonthlySummaries(int year) {
        Map<YearMonth, List<Expense>> grouped = getAllExpenses().stream()
                .filter(e -> e.getDate().getYear() == year)
                .collect(Collectors.groupingBy(e -> YearMonth.from(e.getDate())));

        List<MonthlySummary> list = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(year, m);
            List<Expense> expList = grouped.getOrDefault(ym, Collections.emptyList());
            double sum = expList.stream().mapToDouble(Expense::getAmount).sum();
            list.add(new MonthlySummary(ym, sum, expList.size()));
        }
        return list;
    }

    public Optional<Expense> getHighestExpense() {
        return getAllExpenses().stream()
                .max(Comparator.comparingDouble(Expense::getAmount));
    }

    public double getAverageDailyExpense(YearMonth month) {
        double spent = getTotalSpentForMonth(month);
        int days = month.lengthOfMonth();
        return spent / days;
    }

    public void exportToCsv(Path destinationPath) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("ID,Date,Title,Category,Amount,PaymentMethod,Notes");
        for (Expense exp : getAllExpenses()) {
            lines.add(exp.toCsvLine());
        }
        Files.write(destinationPath, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void clearAll() {
        repository.clearAll();
    }
}
