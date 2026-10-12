package com.expensetracker.ui.cli;

import com.expensetracker.model.*;
import com.expensetracker.service.CategorySummary;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.ui.gui.ExpenseTrackerGUI;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import static com.expensetracker.ui.cli.AnsiColor.*;

public class ExpenseTrackerCLI {
    private final ExpenseService service;
    private final Scanner scanner;

    public ExpenseTrackerCLI(ExpenseService service) {
        this.service = service;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        boolean running = true;
        while (running) {
            printHeader();
            printMenu();
            String choice = prompt("Select an option: ");

            switch (choice.trim()) {
                case "1" -> handleAddExpense();
                case "2" -> handleViewAll();
                case "3" -> handleFilterAndSearch();
                case "4" -> handleAnalytics();
                case "5" -> handleBudget();
                case "6" -> handleEditExpense();
                case "7" -> handleDeleteExpense();
                case "8" -> handleExport();
                case "9" -> handleLaunchGui();
                case "0" -> {
                    running = false;
                    System.out.println("\n" + success("Thank you for using Expense Tracker! Goodbye. 👋"));
                }
                default -> System.out.println(error("Invalid option. Please choose a number between 0 and 9."));
            }

            if (running) {
                System.out.println();
                pause();
            }
        }
    }

    private void printHeader() {
        System.out.println("\n" + bold(CYAN + "============================================================" + RESET));
        System.out.println(bold(BRIGHT_CYAN + "         💰 PERSONAL EXPENSE TRACKER & BUDGET APP         " + RESET));
        System.out.println(bold(CYAN + "============================================================" + RESET));
        YearMonth now = YearMonth.now();
        Budget budget = service.getBudget(now);
        double spent = budget.getCurrentSpent();
        double limit = budget.getMonthlyLimit();

        String budgetInfo = String.format("Current Month (%s): Spent %s / Budget %s [%s]",
                now, budget.getFormattedSpent(), budget.getFormattedLimit(),
                budget.getStatus().getIcon() + " " + budget.getStatus().getLabel());
        System.out.println(DIM + budgetInfo + RESET);
        System.out.println(CYAN + "------------------------------------------------------------" + RESET);
    }

    private void printMenu() {
        System.out.println(bold("Main Menu:"));
        System.out.println("  " + BRIGHT_YELLOW + "1." + RESET + " ➕ Add New Expense");
        System.out.println("  " + BRIGHT_YELLOW + "2." + RESET + " 📋 View All Expenses");
        System.out.println("  " + BRIGHT_YELLOW + "3." + RESET + " 🔍 Filter & Search Expenses");
        System.out.println("  " + BRIGHT_YELLOW + "4." + RESET + " 📊 Spending Analytics & Category Breakdown");
        System.out.println("  " + BRIGHT_YELLOW + "5." + RESET + " 🎯 Monthly Budget & Alerts");
        System.out.println("  " + BRIGHT_YELLOW + "6." + RESET + " ✏️  Edit an Expense");
        System.out.println("  " + BRIGHT_YELLOW + "7." + RESET + " 🗑️  Delete an Expense");
        System.out.println("  " + BRIGHT_YELLOW + "8." + RESET + " 💾 Export Expenses to CSV");
        System.out.println("  " + BRIGHT_YELLOW + "9." + RESET + " 🖥️  Launch Graphical Desktop GUI");
        System.out.println("  " + BRIGHT_RED + "0." + RESET + " 🚪 Exit");
        System.out.println();
    }

    private void handleAddExpense() {
        System.out.println("\n" + bold(GREEN + "=== Add New Expense ===" + RESET));

        String title = "";
        while (title.isEmpty()) {
            title = prompt("Expense Description / Title: ").trim();
            if (title.isEmpty()) {
                System.out.println(error("Title cannot be empty. Please enter a valid description."));
            }
        }

        double amount = -1;
        while (amount < 0) {
            String amtStr = prompt("Amount ($): ").trim();
            try {
                amount = Double.parseDouble(amtStr);
                if (amount < 0) {
                    System.out.println(error("Amount cannot be negative."));
                }
            } catch (NumberFormatException e) {
                System.out.println(error("Invalid number. Please enter a valid amount (e.g. 24.50)."));
            }
        }

        Category category = selectCategory();
        LocalDate date = selectDate();
        PaymentMethod paymentMethod = selectPaymentMethod();
        String notes = prompt("Notes (optional, press Enter to skip): ").trim();

        try {
            Expense created = service.addExpense(date, title, category, amount, paymentMethod, notes);
            System.out.println("\n" + success("✔ Expense added successfully! Assigned ID: " + created.getId()));
            
            // Check budget status
            Budget budget = service.getBudget(YearMonth.from(date));
            if (budget.getStatus() == Budget.Status.EXCEEDED) {
                System.out.println(error("🚨 Alert: You have exceeded your monthly budget of " + budget.getFormattedLimit() + "!"));
            } else if (budget.getStatus() == Budget.Status.WARNING) {
                System.out.println(warning("⚠️ Warning: You have reached " + String.format("%.1f%%", budget.getPercentageUsed()) + " of your monthly budget."));
            }
        } catch (Exception e) {
            System.out.println(error("Failed to add expense: " + e.getMessage()));
        }
    }

    private void handleViewAll() {
        System.out.println("\n" + bold(BLUE + "=== All Expenses ===" + RESET));
        List<Expense> expenses = service.getAllExpenses();
        if (expenses.isEmpty()) {
            System.out.println(warning("No expenses recorded yet."));
            return;
        }
        printExpenseTable(expenses);
        double total = service.calculateTotal(expenses);
        System.out.println(bold(String.format("Total Spending (%d transactions): %s", expenses.size(), String.format("$%.2f", total))));
    }

    private void handleFilterAndSearch() {
        System.out.println("\n" + bold(CYAN + "=== Filter & Search Expenses ===" + RESET));
        System.out.println("  1. Search by Keyword (title, note, category, ID)");
        System.out.println("  2. Filter by Category");
        System.out.println("  3. Filter by Date Range");
        System.out.println("  4. Filter by Payment Method");
        System.out.println("  0. Back to Main Menu");

        String sub = prompt("Select filter option: ").trim();
        List<Expense> results;

        switch (sub) {
            case "1" -> {
                String kw = prompt("Enter search keyword: ");
                results = service.search(kw);
                System.out.println("\n" + info("Search results for: \"" + kw + "\""));
                printExpenseTable(results);
            }
            case "2" -> {
                Category cat = selectCategory();
                results = service.filterByCategory(cat);
                System.out.println("\n" + info("Filtered by category: " + cat.getFormattedName()));
                printExpenseTable(results);
            }
            case "3" -> {
                System.out.println("Enter Start Date (YYYY-MM-DD) or press Enter for no start limit:");
                String sDate = scanner.nextLine().trim();
                LocalDate from = null;
                if (!sDate.isEmpty()) {
                    try { from = LocalDate.parse(sDate, Expense.DATE_FORMATTER); } catch (Exception ignored) {}
                }

                System.out.println("Enter End Date (YYYY-MM-DD) or press Enter for today:");
                String eDate = scanner.nextLine().trim();
                LocalDate to = LocalDate.now();
                if (!eDate.isEmpty()) {
                    try { to = LocalDate.parse(eDate, Expense.DATE_FORMATTER); } catch (Exception ignored) {}
                }

                results = service.filterByDateRange(from, to);
                System.out.println("\n" + info("Filtered by Date Range: " + (from == null ? "Beginning" : from) + " to " + to));
                printExpenseTable(results);
            }
            case "4" -> {
                PaymentMethod pm = selectPaymentMethod();
                results = service.filterByPaymentMethod(pm);
                System.out.println("\n" + info("Filtered by Payment Method: " + pm.getFormattedName()));
                printExpenseTable(results);
            }
            default -> {
                return;
            }
        }
    }

    private void handleAnalytics() {
        System.out.println("\n" + bold(MAGENTA + "=== Spending Analytics & Insights ===" + RESET));
        List<Expense> all = service.getAllExpenses();
        if (all.isEmpty()) {
            System.out.println(warning("No expenses available for analytics."));
            return;
        }

        double totalAllTime = service.calculateTotal(all);
        YearMonth thisMonth = YearMonth.now();
        double thisMonthTotal = service.getTotalSpentForMonth(thisMonth);
        double dailyAvg = service.getAverageDailyExpense(thisMonth);
        Optional<Expense> highest = service.getHighestExpense();

        System.out.println(bold(CYAN + "Summary Statistics:" + RESET));
        System.out.println(String.format("  • Total All-Time Spending:   %s (%d transactions)", 
                bold(String.format("$%.2f", totalAllTime)), all.size()));
        System.out.println(String.format("  • Spending This Month (%s): %s", 
                thisMonth, bold(String.format("$%.2f", thisMonthTotal))));
        System.out.println(String.format("  • Daily Average This Month:  %s", 
                bold(String.format("$%.2f / day", dailyAvg))));

        highest.ifPresent(exp -> System.out.println(String.format("  • Highest Single Expense:    %s on %s (%s)",
                bold(exp.getFormattedAmount()), exp.getTitle(), exp.getFormattedDate())));

        System.out.println("\n" + bold(CYAN + "Category Breakdown (Visual Distribution):" + RESET));
        List<CategorySummary> summaries = service.getCategorySummaries(all);
        int barLength = 24;

        for (CategorySummary cs : summaries) {
            String bar = cs.getProgressBar(barLength);
            System.out.printf("  %-22s | %s | %6s | %9s (%d tx)%n",
                    cs.getCategory().getFormattedName(),
                    GREEN + bar + RESET,
                    cs.getFormattedPercentage(),
                    bold(cs.getFormattedTotal()),
                    cs.getCount());
        }
    }

    private void handleBudget() {
        System.out.println("\n" + bold(YELLOW + "=== Monthly Budget Management ===" + RESET));
        YearMonth currentMonth = YearMonth.now();
        Budget budget = service.getBudget(currentMonth);

        System.out.println(String.format("Budget for:               %s", bold(currentMonth.toString())));
        System.out.println(String.format("Monthly Budget Limit:     %s", bold(budget.getFormattedLimit())));
        System.out.println(String.format("Current Spending:         %s", bold(budget.getFormattedSpent())));
        System.out.println(String.format("Remaining Balance:        %s", bold(budget.getFormattedRemaining())));
        System.out.println(String.format("Percentage Utilized:      %.1f%%", budget.getPercentageUsed()));
        System.out.println(String.format("Budget Health Status:     %s %s", 
                budget.getStatus().getIcon(), bold(budget.getStatus().getLabel())));

        System.out.println("\nWould you like to change the monthly budget limit? (y/N): ");
        String ans = scanner.nextLine().trim();
        if (ans.equalsIgnoreCase("y") || ans.equalsIgnoreCase("yes")) {
            double newLimit = -1;
            while (newLimit < 0) {
                String val = prompt("Enter new monthly budget ($): ");
                try {
                    newLimit = Double.parseDouble(val.trim());
                    if (newLimit < 0) System.out.println(error("Budget cannot be negative."));
                } catch (NumberFormatException e) {
                    System.out.println(error("Please enter a valid numeric value."));
                }
            }
            service.setBudget(currentMonth, newLimit);
            System.out.println(success("✔ Budget for " + currentMonth + " updated to $" + String.format("%.2f", newLimit)));
        }
    }

    private void handleEditExpense() {
        System.out.println("\n" + bold(YELLOW + "=== Edit an Expense ===" + RESET));
        String id = prompt("Enter the ID of the expense to edit (e.g. EXP-1001): ").trim();
        Optional<Expense> opt = service.getExpenseById(id);
        if (opt.isEmpty()) {
            System.out.println(error("Expense with ID '" + id + "' not found."));
            return;
        }

        Expense exp = opt.get();
        System.out.println("Currently editing: " + exp);
        System.out.println(DIM + "(Press Enter without typing to keep the current value)" + RESET);

        String newTitle = prompt("New Title [" + exp.getTitle() + "]: ").trim();
        if (!newTitle.isEmpty()) {
            exp.setTitle(newTitle);
        }

        String amtStr = prompt("New Amount [$" + exp.getAmount() + "]: ").trim();
        if (!amtStr.isEmpty()) {
            try {
                double newAmt = Double.parseDouble(amtStr);
                if (newAmt >= 0) exp.setAmount(newAmt);
            } catch (NumberFormatException ignored) {}
        }

        System.out.println("Change category? (Current: " + exp.getCategory().getFormattedName() + ") [y/N]:");
        String changeCat = scanner.nextLine().trim();
        if (changeCat.equalsIgnoreCase("y")) {
            exp.setCategory(selectCategory());
        }

        String dateStr = prompt("New Date (YYYY-MM-DD) [" + exp.getFormattedDate() + "]: ").trim();
        if (!dateStr.isEmpty()) {
            try {
                exp.setDate(LocalDate.parse(dateStr, Expense.DATE_FORMATTER));
            } catch (DateTimeParseException ignored) {}
        }

        System.out.println("Change payment method? (Current: " + exp.getPaymentMethod().getFormattedName() + ") [y/N]:");
        String changePm = scanner.nextLine().trim();
        if (changePm.equalsIgnoreCase("y")) {
            exp.setPaymentMethod(selectPaymentMethod());
        }

        String newNotes = prompt("New Notes [" + exp.getNotes() + "]: ").trim();
        if (!newNotes.isEmpty()) {
            exp.setNotes(newNotes);
        }

        if (service.updateExpense(exp)) {
            System.out.println(success("✔ Expense updated successfully!"));
        } else {
            System.out.println(error("Failed to update expense."));
        }
    }

    private void handleDeleteExpense() {
        System.out.println("\n" + bold(RED + "=== Delete an Expense ===" + RESET));
        String id = prompt("Enter the ID of the expense to delete (e.g. EXP-1001): ").trim();
        Optional<Expense> opt = service.getExpenseById(id);
        if (opt.isEmpty()) {
            System.out.println(error("Expense with ID '" + id + "' not found."));
            return;
        }

        Expense exp = opt.get();
        System.out.println("Found: " + exp);
        String confirm = prompt("Are you sure you want to permanently delete this expense? (y/N): ").trim();
        if (confirm.equalsIgnoreCase("y") || confirm.equalsIgnoreCase("yes")) {
            if (service.deleteExpense(id)) {
                System.out.println(success("✔ Expense " + id + " has been deleted."));
            } else {
                System.out.println(error("Failed to delete expense."));
            }
        } else {
            System.out.println(info("Deletion cancelled."));
        }
    }

    private void handleExport() {
        System.out.println("\n" + bold(CYAN + "=== Export Expenses to CSV ===" + RESET));
        String filename = prompt("Enter filename/path (press Enter for 'expenses_export.csv'): ").trim();
        if (filename.isEmpty()) {
            filename = "expenses_export.csv";
        }
        Path path = Paths.get(filename);
        try {
            service.exportToCsv(path);
            System.out.println(success("✔ Expenses successfully exported to: " + path.toAbsolutePath()));
        } catch (IOException e) {
            System.out.println(error("Export failed: " + e.getMessage()));
        }
    }

    private void handleLaunchGui() {
        System.out.println(info("Launching Desktop Graphical Interface..."));
        new Thread(() -> {
            try {
                ExpenseTrackerGUI.launch(service);
            } catch (Exception e) {
                System.out.println(error("Could not launch GUI: " + e.getMessage()));
            }
        }).start();
        System.out.println(success("✔ Desktop window opened! You can continue using this CLI or the GUI window."));
    }

    private Category selectCategory() {
        Category[] cats = Category.values();
        System.out.println("Select Category:");
        for (int i = 0; i < cats.length; i++) {
            System.out.printf("  %2d. %s%n", (i + 1), cats[i].getFormattedName());
        }
        while (true) {
            String input = prompt("Enter category number (1-" + cats.length + "): ").trim();
            try {
                int index = Integer.parseInt(input) - 1;
                if (index >= 0 && index < cats.length) {
                    return cats[index];
                }
            } catch (NumberFormatException ignored) {}
            System.out.println(error("Invalid selection. Try again."));
        }
    }

    private PaymentMethod selectPaymentMethod() {
        PaymentMethod[] methods = PaymentMethod.values();
        System.out.println("Select Payment Method:");
        for (int i = 0; i < methods.length; i++) {
            System.out.printf("  %2d. %s%n", (i + 1), methods[i].getFormattedName());
        }
        while (true) {
            String input = prompt("Enter payment method number (1-" + methods.length + ") [default 1]: ").trim();
            if (input.isEmpty()) return PaymentMethod.CASH;
            try {
                int index = Integer.parseInt(input) - 1;
                if (index >= 0 && index < methods.length) {
                    return methods[index];
                }
            } catch (NumberFormatException ignored) {}
            System.out.println(error("Invalid selection. Try again."));
        }
    }

    private LocalDate selectDate() {
        while (true) {
            String input = prompt("Date (YYYY-MM-DD) [press Enter for today " + LocalDate.now() + "]: ").trim();
            if (input.isEmpty()) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(input, Expense.DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.println(error("Invalid date format. Please use YYYY-MM-DD (e.g. 2026-10-12)."));
            }
        }
    }

    private void printExpenseTable(List<Expense> list) {
        if (list == null || list.isEmpty()) {
            System.out.println(warning("No matching expenses found."));
            return;
        }

        String border = "+----------+------------+--------------------------------+-----------------------+------------+---------------+--------------------------------+";
        System.out.println(border);
        System.out.printf("| %-8s | %-10s | %-30s | %-21s | %10s | %-13s | %-30s |%n",
                "ID", "Date", "Title / Description", "Category", "Amount", "Method", "Notes");
        System.out.println(border);

        for (Expense exp : list) {
            String title = truncate(exp.getTitle(), 30);
            String notes = truncate(exp.getNotes(), 30);
            String cat = truncate(exp.getCategory().getDisplayName(), 21);
            String pm = truncate(exp.getPaymentMethod().getDisplayName(), 13);

            System.out.printf("| %-8s | %-10s | %-30s | %-21s | %10s | %-13s | %-30s |%n",
                    exp.getId(),
                    exp.getFormattedDate(),
                    title,
                    cat,
                    exp.getFormattedAmount(),
                    pm,
                    notes);
        }
        System.out.println(border);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private String prompt(String message) {
        System.out.print(message);
        return scanner.nextLine();
    }

    private void pause() {
        System.out.print(DIM + "Press [Enter] to continue..." + RESET);
        scanner.nextLine();
    }
}
