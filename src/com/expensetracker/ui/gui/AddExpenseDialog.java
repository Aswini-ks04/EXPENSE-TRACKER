package com.expensetracker.ui.gui;

import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class AddExpenseDialog extends JDialog {
    private final JTextField titleField = new JTextField(20);
    private final JTextField amountField = new JTextField(10);
    private final JComboBox<Category> categoryCombo = new JComboBox<>(Category.values());
    private final JTextField dateField = new JTextField(10);
    private final JComboBox<PaymentMethod> paymentCombo = new JComboBox<>(PaymentMethod.values());
    private final JTextField notesField = new JTextField(20);

    private boolean confirmed = false;
    private final Expense existingExpense;

    public AddExpenseDialog(Frame parent, Expense expenseToEdit) {
        super(parent, expenseToEdit == null ? "Add New Expense" : "Edit Expense", true);
        this.existingExpense = expenseToEdit;

        setLayout(new BorderLayout());
        setResizable(false);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addFormField(formPanel, gbc, row++, "Description / Title:*", titleField);
        addFormField(formPanel, gbc, row++, "Amount ($):*", amountField);
        addFormField(formPanel, gbc, row++, "Category:*", categoryCombo);
        addFormField(formPanel, gbc, row++, "Date (YYYY-MM-DD):*", dateField);
        addFormField(formPanel, gbc, row++, "Payment Method:", paymentCombo);
        addFormField(formPanel, gbc, row++, "Notes (Optional):", notesField);

        // Prepopulate if editing
        if (existingExpense != null) {
            titleField.setText(existingExpense.getTitle());
            amountField.setText(String.format(java.util.Locale.US, "%.2f", existingExpense.getAmount()));
            categoryCombo.setSelectedItem(existingExpense.getCategory());
            dateField.setText(existingExpense.getFormattedDate());
            paymentCombo.setSelectedItem(existingExpense.getPaymentMethod());
            notesField.setText(existingExpense.getNotes());
        } else {
            dateField.setText(LocalDate.now().format(Expense.DATE_FORMATTER));
        }

        // Action Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBackground(new Color(245, 247, 250));

        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = new JButton(existingExpense == null ? "Add Expense" : "Save Changes");
        btnSave.setBackground(new Color(41, 128, 185));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSave.addActionListener(e -> validateAndSave());

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSave);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panel.add(field, gbc);
    }

    private void validateAndSave() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            titleField.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim());
            if (amount < 0) {
                JOptionPane.showMessageDialog(this, "Amount must be a positive number.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                amountField.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid amount (e.g. 25.50).", "Validation Error", JOptionPane.ERROR_MESSAGE);
            amountField.requestFocus();
            return;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateField.getText().trim(), Expense.DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Please use YYYY-MM-DD.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            dateField.requestFocus();
            return;
        }

        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getTitleInput() {
        return titleField.getText().trim();
    }

    public double getAmountInput() {
        return Double.parseDouble(amountField.getText().trim());
    }

    public Category getCategoryInput() {
        return (Category) categoryCombo.getSelectedItem();
    }

    public LocalDate getDateInput() {
        return LocalDate.parse(dateField.getText().trim(), Expense.DATE_FORMATTER);
    }

    public PaymentMethod getPaymentMethodInput() {
        return (PaymentMethod) paymentCombo.getSelectedItem();
    }

    public String getNotesInput() {
        return notesField.getText().trim();
    }
}
