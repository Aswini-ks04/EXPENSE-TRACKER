package com.expensetracker.ui.gui;

import com.expensetracker.model.*;
import com.expensetracker.service.ExpenseService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public class ExpenseTrackerGUI extends JFrame {
    private final ExpenseService service;

    // Components
    private JTable expenseTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private CategoryChartPanel chartPanel;

    // Metric Cards Labels
    private JLabel lblTotalSpent;
    private JLabel lblMonthSpent;
    private JLabel lblBudgetRemaining;
    private JLabel lblTxCount;

    // Filters
    private JTextField txtSearch;
    private JComboBox<String> comboCategoryFilter;
    private JComboBox<String> comboTimeFilter;

    public ExpenseTrackerGUI(ExpenseService service) {
        this.service = service;
        setTitle("Personal Expense Tracker & Budget Manager");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        initUI();
        refreshAll();
    }

    public static void launch(ExpenseService service) {
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored2) {}
            }

            ExpenseTrackerGUI gui = new ExpenseTrackerGUI(service);
            gui.setVisible(true);
        });
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(12, 16, 12, 16));
        root.setBackground(new Color(245, 247, 250));

        // 1. Top Header with Actions
        root.add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Metrics Bar & Content Panel
        JPanel mainContent = new JPanel(new BorderLayout(0, 12));
        mainContent.setOpaque(false);

        mainContent.add(createMetricsPanel(), BorderLayout.NORTH);

        // Center split: Left is Table & Filter, Right is Category Breakdown
        JPanel centerSplit = new JPanel(new BorderLayout(14, 0));
        centerSplit.setOpaque(false);

        JPanel tableSection = createTableSection();
        centerSplit.add(tableSection, BorderLayout.CENTER);

        JPanel rightSection = createRightAnalyticsSection();
        centerSplit.add(rightSection, BorderLayout.EAST);

        mainContent.add(centerSplit, BorderLayout.CENTER);
        root.add(mainContent, BorderLayout.CENTER);

        setContentPane(root);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        // Title & subtitle
        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);

        JLabel lblTitle = new JLabel("💰 Personal Expense Tracker");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(24, 43, 73));

        JLabel lblSub = new JLabel("Track, analyze, and master your financial health");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(110, 120, 135));

        titleBox.add(lblTitle);
        titleBox.add(lblSub);
        header.add(titleBox, BorderLayout.WEST);

        // Action Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        toolbar.setOpaque(false);

        JButton btnAdd = createStyledButton("➕ Add Expense", new Color(41, 128, 185), Color.WHITE);
        btnAdd.addActionListener(e -> onAddExpense());

        JButton btnEdit = createStyledButton("✏️ Edit", new Color(236, 240, 241), new Color(44, 62, 80));
        btnEdit.addActionListener(e -> onEditExpense());

        JButton btnDelete = createStyledButton("🗑️ Delete", new Color(254, 237, 236), new Color(192, 57, 43));
        btnDelete.addActionListener(e -> onDeleteExpense());

        JButton btnBudget = createStyledButton("🎯 Budget", new Color(236, 240, 241), new Color(44, 62, 80));
        btnBudget.addActionListener(e -> onSetBudget());

        JButton btnExport = createStyledButton("💾 Export", new Color(236, 240, 241), new Color(44, 62, 80));
        btnExport.addActionListener(e -> onExportCsv());

        toolbar.add(btnAdd);
        toolbar.add(btnEdit);
        toolbar.add(btnDelete);
        toolbar.add(btnBudget);
        toolbar.add(btnExport);

        header.add(toolbar, BorderLayout.EAST);
        return header;
    }

    private JPanel createMetricsPanel() {
        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.setOpaque(false);

        lblTotalSpent = new JLabel("$0.00");
        lblMonthSpent = new JLabel("$0.00");
        lblBudgetRemaining = new JLabel("$0.00");
        lblTxCount = new JLabel("0");

        metrics.add(createCard("Total Spending", lblTotalSpent, new Color(41, 128, 185)));
        metrics.add(createCard("Spent This Month", lblMonthSpent, new Color(142, 68, 173)));
        metrics.add(createCard("Budget Remaining", lblBudgetRemaining, new Color(39, 174, 96)));
        metrics.add(createCard("Transactions", lblTxCount, new Color(230, 126, 34)));

        return metrics;
    }

    private JPanel createCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTitle.setForeground(new Color(110, 120, 135));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 19));
        valueLabel.setForeground(new Color(30, 40, 60));

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createTableSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        // Filter / Search Toolbar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterBar.setBackground(Color.WHITE);
        filterBar.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 240), 1));

        filterBar.add(new JLabel("🔍"));
        txtSearch = new JTextField(12);
        txtSearch.putClientProperty("JTextField.placeholderText", "Search expenses...");
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilters(); }
            public void removeUpdate(DocumentEvent e) { applyFilters(); }
            public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });
        filterBar.add(txtSearch);

        filterBar.add(new JLabel("Category:"));
        comboCategoryFilter = new JComboBox<>();
        comboCategoryFilter.addItem("All Categories");
        for (Category c : Category.values()) {
            comboCategoryFilter.addItem(c.getDisplayName());
        }
        comboCategoryFilter.addActionListener(e -> applyFilters());
        filterBar.add(comboCategoryFilter);

        filterBar.add(new JLabel("Time Period:"));
        comboTimeFilter = new JComboBox<>(new String[]{"All Time", "This Month", "Last 30 Days", "This Year"});
        comboTimeFilter.addActionListener(e -> applyFilters());
        filterBar.add(comboTimeFilter);

        JButton btnReset = new JButton("Reset");
        btnReset.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            comboCategoryFilter.setSelectedIndex(0);
            comboTimeFilter.setSelectedIndex(0);
            applyFilters();
        });
        filterBar.add(btnReset);

        panel.add(filterBar, BorderLayout.NORTH);

        // Expenses Table
        String[] columnNames = {"ID", "Date", "Description", "Category", "Amount", "Method", "Notes"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        expenseTable = new JTable(tableModel);
        expenseTable.setRowHeight(28);
        expenseTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        expenseTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        expenseTable.getTableHeader().setBackground(new Color(240, 244, 250));
        expenseTable.setShowGrid(true);
        expenseTable.setGridColor(new Color(235, 240, 248));
        expenseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Column widths
        expenseTable.getColumnModel().getColumn(0).setPreferredWidth(75);
        expenseTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        expenseTable.getColumnModel().getColumn(2).setPreferredWidth(210);
        expenseTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        expenseTable.getColumnModel().getColumn(4).setPreferredWidth(90);
        expenseTable.getColumnModel().getColumn(5).setPreferredWidth(110);
        expenseTable.getColumnModel().getColumn(6).setPreferredWidth(180);

        // Align amounts to the right
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        expenseTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        // Double click to edit
        expenseTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2 && expenseTable.getSelectedRow() != -1) {
                    onEditExpense();
                }
            }
        });

        rowSorter = new TableRowSorter<>(tableModel);
        expenseTable.setRowSorter(rowSorter);

        JScrollPane scrollPane = new JScrollPane(expenseTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 240), 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRightAnalyticsSection() {
        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setPreferredSize(new Dimension(380, 0));
        right.setOpaque(false);

        chartPanel = new CategoryChartPanel();
        chartPanel.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 240), 1));

        JScrollPane chartScroll = new JScrollPane(chartPanel);
        chartScroll.setBorder(null);
        chartScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        right.add(chartScroll, BorderLayout.CENTER);
        return right;
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 220), 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
        return btn;
    }

    private void refreshAll() {
        // Refresh Table
        applyFilters();

        // Refresh Cards
        List<Expense> all = service.getAllExpenses();
        double totalAllTime = service.calculateTotal(all);
        lblTotalSpent.setText(String.format("$%.2f", totalAllTime));
        lblTxCount.setText(String.valueOf(all.size()));

        YearMonth thisMonth = YearMonth.now();
        Budget budget = service.getBudget(thisMonth);
        lblMonthSpent.setText(budget.getFormattedSpent());

        double remaining = budget.getRemaining();
        if (remaining >= 0) {
            lblBudgetRemaining.setText(String.format("$%.2f left", remaining));
            lblBudgetRemaining.setForeground(budget.getStatus() == Budget.Status.WARNING 
                    ? new Color(211, 84, 0) : new Color(39, 174, 96));
        } else {
            lblBudgetRemaining.setText(String.format("-$%.2f OVER", Math.abs(remaining)));
            lblBudgetRemaining.setForeground(new Color(192, 57, 43));
        }

        // Refresh Chart
        chartPanel.setSummaries(service.getCategorySummaries(all));
    }

    private void applyFilters() {
        List<Expense> list = service.getAllExpenses();

        // Time period filter
        String timeSelection = (String) comboTimeFilter.getSelectedItem();
        LocalDate now = LocalDate.now();
        if ("This Month".equals(timeSelection)) {
            LocalDate start = now.withDayOfMonth(1);
            list = service.filterByDateRange(start, now.plusDays(31));
        } else if ("Last 30 Days".equals(timeSelection)) {
            list = service.filterByDateRange(now.minusDays(30), now);
        } else if ("This Year".equals(timeSelection)) {
            LocalDate start = now.withDayOfYear(1);
            list = service.filterByDateRange(start, now.plusDays(365));
        }

        // Category filter
        String catSelection = (String) comboCategoryFilter.getSelectedItem();
        if (catSelection != null && !catSelection.equals("All Categories")) {
            Category cat = Category.fromString(catSelection);
            list = list.stream().filter(e -> e.getCategory() == cat).toList();
        }

        // Search text filter
        String query = txtSearch.getText().trim().toLowerCase();
        if (!query.isEmpty()) {
            list = list.stream().filter(e ->
                    e.getTitle().toLowerCase().contains(query) ||
                    e.getId().toLowerCase().contains(query) ||
                    e.getCategory().getDisplayName().toLowerCase().contains(query) ||
                    e.getPaymentMethod().getDisplayName().toLowerCase().contains(query) ||
                    e.getNotes().toLowerCase().contains(query)
            ).toList();
        }

        // Populate table
        tableModel.setRowCount(0);
        for (Expense e : list) {
            tableModel.addRow(new Object[]{
                    e.getId(),
                    e.getFormattedDate(),
                    e.getTitle(),
                    e.getCategory().getFormattedName(),
                    e.getFormattedAmount(),
                    e.getPaymentMethod().getFormattedName(),
                    e.getNotes()
            });
        }
    }

    private void onAddExpense() {
        AddExpenseDialog dialog = new AddExpenseDialog(this, null);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            service.addExpense(
                    dialog.getDateInput(),
                    dialog.getTitleInput(),
                    dialog.getCategoryInput(),
                    dialog.getAmountInput(),
                    dialog.getPaymentMethodInput(),
                    dialog.getNotesInput()
            );
            refreshAll();

            // Check budget alert
            Budget b = service.getBudget(YearMonth.from(dialog.getDateInput()));
            if (b.getStatus() == Budget.Status.EXCEEDED) {
                JOptionPane.showMessageDialog(this, 
                        "Alert: This expense puts you over your monthly budget of " + b.getFormattedLimit() + "!",
                        "Budget Alert", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void onEditExpense() {
        int selectedRow = expenseTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an expense row to edit.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int modelRow = expenseTable.convertRowIndexToModel(selectedRow);
        String id = (String) tableModel.getValueAt(modelRow, 0);
        Optional<Expense> opt = service.getExpenseById(id);

        if (opt.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Could not find expense: " + id, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Expense expense = opt.get();
        AddExpenseDialog dialog = new AddExpenseDialog(this, expense);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            expense.setTitle(dialog.getTitleInput());
            expense.setAmount(dialog.getAmountInput());
            expense.setCategory(dialog.getCategoryInput());
            expense.setDate(dialog.getDateInput());
            expense.setPaymentMethod(dialog.getPaymentMethodInput());
            expense.setNotes(dialog.getNotesInput());

            service.updateExpense(expense);
            refreshAll();
        }
    }

    private void onDeleteExpense() {
        int selectedRow = expenseTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an expense row to delete.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int modelRow = expenseTable.convertRowIndexToModel(selectedRow);
        String id = (String) tableModel.getValueAt(modelRow, 0);
        String title = (String) tableModel.getValueAt(modelRow, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete expense [" + id + "]: " + title + "?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            service.deleteExpense(id);
            refreshAll();
        }
    }

    private void onSetBudget() {
        BudgetDialog dialog = new BudgetDialog(this, service, YearMonth.now());
        dialog.setVisible(true);
        if (dialog.isUpdated()) {
            refreshAll();
        }
    }

    private void onExportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("expenses_export.csv"));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            try {
                service.exportToCsv(chooser.getSelectedFile().toPath());
                JOptionPane.showMessageDialog(this, "Export successful!\nSaved to: " + chooser.getSelectedFile().getAbsolutePath(),
                        "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
