package com.expensetracker.ui.gui;

import com.expensetracker.model.Budget;
import com.expensetracker.service.ExpenseService;

import javax.swing.*;
import java.awt.*;
import java.time.YearMonth;
import java.util.Locale;

public class BudgetDialog extends JDialog {
    private final JTextField budgetField = new JTextField(10);
    private boolean updated = false;

    public BudgetDialog(Frame parent, ExpenseService service, YearMonth month) {
        super(parent, "Monthly Budget Settings", true);
        setLayout(new BorderLayout());
        setResizable(false);

        Budget budget = service.getBudget(month);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Month:"), gbc);
        gbc.gridx = 1;
        JLabel lblMonth = new JLabel(month.toString());
        lblMonth.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(lblMonth, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Current Spent:"), gbc);
        gbc.gridx = 1;
        panel.add(new JLabel(budget.getFormattedSpent()), gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Monthly Limit ($):"), gbc);
        gbc.gridx = 1;
        budgetField.setText(String.format(Locale.US, "%.2f", budget.getMonthlyLimit()));
        panel.add(budgetField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = new JButton("Update Budget");
        btnSave.setBackground(new Color(39, 174, 96));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSave.addActionListener(e -> {
            try {
                double val = Double.parseDouble(budgetField.getText().trim());
                if (val < 0) {
                    JOptionPane.showMessageDialog(this, "Budget cannot be negative.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                service.setBudget(month, val);
                updated = true;
                dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid number.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSave);

        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    public boolean isUpdated() {
        return updated;
    }
}
