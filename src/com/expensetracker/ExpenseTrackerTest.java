package com.expensetracker;

import com.expensetracker.model.*;
import com.expensetracker.repository.CsvExpenseRepository;
import com.expensetracker.service.CategorySummary;
import com.expensetracker.service.ExpenseService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public class ExpenseTrackerTest {
    public static void main(String[] args) {
        System.out.println("Running Expense Tracker Test Suite...\n");
        int passed = 0;
        int failed = 0;

        try {
            Path tempCsv = Files.createTempFile("test_expenses", ".csv");
            Path tempBudget = Files.createTempFile("test_budget", ".properties");
            tempCsv.toFile().deleteOnExit();
            tempBudget.toFile().deleteOnExit();

            CsvExpenseRepository repo = new CsvExpenseRepository(tempCsv, tempBudget);
            ExpenseService service = new ExpenseService(repo);

            // Test 1: Fresh start initialized with 0 expenses
            List<Expense> initial = service.getAllExpenses();
            assertCondition(initial.isEmpty(), "Initial expenses should start completely clean and empty", ++passed);

            // Test 2: Add Expense
            Expense added = service.addExpense(LocalDate.now(), "Test Coffee", Category.FOOD, 4.50, PaymentMethod.UPI, "Espresso");
            assertCondition(added != null && added.getId() != null, "Expense should be created with an ID", ++passed);

            // Test 3: Retrieve Expense by ID
            Optional<Expense> fetched = service.getExpenseById(added.getId());
            assertCondition(fetched.isPresent() && fetched.get().getTitle().equals("Test Coffee"), "Expense should be retrievable by ID", ++passed);

            // Test 4: Edit Expense
            added.setTitle("Test Latte");
            added.setAmount(5.50);
            boolean updated = service.updateExpense(added);
            Optional<Expense> reFetched = service.getExpenseById(added.getId());
            assertCondition(updated && reFetched.isPresent() && reFetched.get().getAmount() == 5.50 && reFetched.get().getTitle().equals("Test Latte"), 
                    "Expense should be updated properly", ++passed);

            // Test 5: Category filter
            List<Expense> foodExpenses = service.filterByCategory(Category.FOOD);
            assertCondition(foodExpenses.stream().allMatch(e -> e.getCategory() == Category.FOOD), "Filter by category should only return category items", ++passed);

            // Test 6: Search
            List<Expense> searchResults = service.search("Latte");
            assertCondition(!searchResults.isEmpty() && searchResults.get(0).getTitle().contains("Latte"), "Search keyword should find matches", ++passed);

            // Test 7: Budget Alert logic
            YearMonth now = YearMonth.now();
            service.setBudget(now, 5.0);
            Budget budget = service.getBudget(now);
            assertCondition(budget.getMonthlyLimit() == 5.0, "Budget limit should be updated", ++passed);
            assertCondition(budget.getStatus() == Budget.Status.EXCEEDED, "Budget status should be EXCEEDED when spent > limit", ++passed);

            // Test 8: Category Summaries
            List<CategorySummary> summaries = service.getCategorySummaries(service.getAllExpenses());
            assertCondition(!summaries.isEmpty() && summaries.get(0).getPercentage() > 0, "Category summaries should calculate percentages", ++passed);

            // Test 9: Delete Expense
            boolean deleted = service.deleteExpense(added.getId());
            Optional<Expense> deletedCheck = service.getExpenseById(added.getId());
            assertCondition(deleted && deletedCheck.isEmpty(), "Deleted expense should not be present", ++passed);

            // Clean up
            Files.deleteIfExists(tempCsv);
            Files.deleteIfExists(tempBudget);

            System.out.println("\n-----------------------------------------");
            System.out.println("ALL " + passed + " TESTS PASSED SUCCESSFULLY! ✔");
            System.out.println("-----------------------------------------");

        } catch (Throwable t) {
            System.err.println("\n❌ Test Suite Failed: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void assertCondition(boolean condition, String message, int testNum) {
        if (!condition) {
            throw new AssertionError("Test " + testNum + " FAILED: " + message);
        }
        System.out.println("Test " + testNum + " PASSED: " + message);
    }
}
