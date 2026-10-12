package com.expensetracker.repository;

import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class CsvExpenseRepository implements ExpenseRepository {
    private static final String CSV_HEADER = "ID,Date,Title,Category,Amount,PaymentMethod,Notes";
    private final Path csvFilePath;
    private final Path budgetFilePath;
    private final List<Expense> expenses = new ArrayList<>();
    private final Map<YearMonth, Double> budgets = new HashMap<>();
    private int nextIdCounter = 1001;

    public CsvExpenseRepository() {
        this(Paths.get("data", "expenses.csv"), Paths.get("data", "budget.properties"));
    }

    public CsvExpenseRepository(Path csvFilePath, Path budgetFilePath) {
        this.csvFilePath = csvFilePath;
        this.budgetFilePath = budgetFilePath;
        initStorage();
        loadBudgets();
        loadExpenses();
    }

    private void initStorage() {
        try {
            if (csvFilePath.getParent() != null && !Files.exists(csvFilePath.getParent())) {
                Files.createDirectories(csvFilePath.getParent());
            }
        } catch (IOException e) {
            System.err.println("Failed to create data directory: " + e.getMessage());
        }
    }

    private synchronized void loadExpenses() {
        expenses.clear();
        if (!Files.exists(csvFilePath)) {
            persistAll();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(csvFilePath, StandardCharsets.UTF_8)) {
            String line;
            boolean isFirstLine = true;
            int maxIdNum = 1000;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip CSV header
                }
                if (line.trim().isEmpty()) {
                    continue;
                }

                Expense expense = parseCsvLine(line);
                if (expense != null) {
                    expenses.add(expense);
                    // Extract ID number if matching EXP-xxxx
                    if (expense.getId() != null && expense.getId().startsWith("EXP-")) {
                        try {
                            int num = Integer.parseInt(expense.getId().substring(4));
                            if (num > maxIdNum) {
                                maxIdNum = num;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            nextIdCounter = maxIdNum + 1;
        } catch (IOException e) {
            System.err.println("Error reading expenses CSV: " + e.getMessage());
        }
    }

    private synchronized void persistAll() {
        try {
            List<String> lines = new ArrayList<>();
            lines.add(CSV_HEADER);
            for (Expense exp : expenses) {
                lines.add(exp.toCsvLine());
            }
            Files.write(csvFilePath, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Error saving expenses to CSV: " + e.getMessage());
        }
    }

    private void loadBudgets() {
        budgets.clear();
        if (!Files.exists(budgetFilePath)) {
            return;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(budgetFilePath)) {
            props.load(in);
            for (String key : props.stringPropertyNames()) {
                try {
                    YearMonth ym = YearMonth.parse(key);
                    double limit = Double.parseDouble(props.getProperty(key));
                    budgets.put(ym, limit);
                } catch (Exception ignored) {}
            }
        } catch (IOException e) {
            System.err.println("Error reading budget properties: " + e.getMessage());
        }
    }

    private void saveBudgets() {
        Properties props = new Properties();
        for (Map.Entry<YearMonth, Double> entry : budgets.entrySet()) {
            props.setProperty(entry.getKey().toString(), String.format(Locale.US, "%.2f", entry.getValue()));
        }
        try (OutputStream out = Files.newOutputStream(budgetFilePath, 
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            props.store(out, "Personal Expense Tracker Budgets");
        } catch (IOException e) {
            System.err.println("Error saving budgets: " + e.getMessage());
        }
    }

    @Override
    public synchronized void clearAll() {
        expenses.clear();
        nextIdCounter = 1001;
        persistAll();
        budgets.clear();
        saveBudgets();
    }

    @Override
    public synchronized List<Expense> findAll() {
        return new ArrayList<>(expenses);
    }

    @Override
    public synchronized Optional<Expense> findById(String id) {
        return expenses.stream()
                .filter(e -> e.getId().equalsIgnoreCase(id))
                .findFirst();
    }

    @Override
    public synchronized Expense save(Expense expense) {
        if (expense.getId() == null || expense.getId().trim().isEmpty()) {
            expense.setId(generateNextId());
        }
        expenses.add(expense);
        persistAll();
        return expense;
    }

    @Override
    public synchronized boolean update(Expense expense) {
        for (int i = 0; i < expenses.size(); i++) {
            if (expenses.get(i).getId().equalsIgnoreCase(expense.getId())) {
                expenses.set(i, expense);
                persistAll();
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean deleteById(String id) {
        boolean removed = expenses.removeIf(e -> e.getId().equalsIgnoreCase(id));
        if (removed) {
            persistAll();
        }
        return removed;
    }

    @Override
    public synchronized void saveAll(List<Expense> newExpenses) {
        expenses.clear();
        expenses.addAll(newExpenses);
        persistAll();
    }

    @Override
    public synchronized String generateNextId() {
        return String.format("EXP-%04d", nextIdCounter++);
    }

    @Override
    public synchronized double getMonthlyBudget(YearMonth month) {
        return budgets.getOrDefault(month, 2000.0);
    }

    @Override
    public synchronized void setMonthlyBudget(YearMonth month, double amount) {
        budgets.put(month, Math.max(0, amount));
        saveBudgets();
    }

    /**
     * Parses a CSV row into an Expense object, respecting quotes.
     */
    private Expense parseCsvLine(String line) {
        List<String> tokens = parseCsvTokens(line);
        if (tokens.size() < 5) {
            return null;
        }

        try {
            String id = tokens.get(0).trim();
            LocalDate date = LocalDate.parse(tokens.get(1).trim(), Expense.DATE_FORMATTER);
            String title = tokens.get(2).trim();
            Category category = Category.fromString(tokens.get(3).trim());
            double amount = Double.parseDouble(tokens.get(4).trim());
            PaymentMethod paymentMethod = tokens.size() > 5 ? PaymentMethod.fromString(tokens.get(5).trim()) : PaymentMethod.CASH;
            String notes = tokens.size() > 6 ? tokens.get(6).trim() : "";

            return new Expense(id, date, title, category, amount, paymentMethod, notes);
        } catch (Exception e) {
            System.err.println("Skipping malformed CSV line: " + line + " (" + e.getMessage() + ")");
            return null;
        }
    }

    private List<String> parseCsvTokens(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    sb.append('\"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString());
        return result;
    }
}
