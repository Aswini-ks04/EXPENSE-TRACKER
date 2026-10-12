package com.expensetracker.repository;

import com.expensetracker.model.Expense;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository {
    List<Expense> findAll();
    Optional<Expense> findById(String id);
    Expense save(Expense expense);
    boolean update(Expense expense);
    boolean deleteById(String id);
    void saveAll(List<Expense> expenses);
    String generateNextId();

    double getMonthlyBudget(YearMonth month);
    void setMonthlyBudget(YearMonth month, double amount);
    void clearAll();
}
