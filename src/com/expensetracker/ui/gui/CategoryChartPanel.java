package com.expensetracker.ui.gui;

import com.expensetracker.service.CategorySummary;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class CategoryChartPanel extends JPanel {
    private List<CategorySummary> summaries = Collections.emptyList();

    private static final Color[] PALETTE = {
            new Color(46, 204, 113),  // Emerald Green
            new Color(52, 152, 219),  // Peter River Blue
            new Color(155, 89, 182),  // Amethyst Purple
            new Color(241, 196, 15),  // Sunflower Yellow
            new Color(230, 126, 34),  // Carrot Orange
            new Color(231, 76, 60),   // Alizarin Red
            new Color(26, 188, 156),  // Turquoise
            new Color(52, 73, 94),    // Wet Asphalt
            new Color(243, 156, 18),  // Orange
            new Color(142, 68, 173),  // Wisteria
            new Color(39, 174, 96),   // Nephritis
            new Color(127, 140, 141)  // Asbestos Gray
    };

    public CategoryChartPanel() {
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    }

    public void setSummaries(List<CategorySummary> summaries) {
        this.summaries = summaries != null ? summaries : Collections.emptyList();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Background
        g2.setColor(new Color(250, 252, 255));
        g2.fillRect(0, 0, width, height);

        // Header Title
        g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
        g2.setColor(new Color(40, 50, 75));
        g2.drawString("Spending by Category", 16, 26);

        if (summaries.isEmpty()) {
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            g2.setColor(new Color(150, 160, 175));
            g2.drawString("No expense data available to display.", 16, 60);
            g2.dispose();
            return;
        }

        int startY = 50;
        int rowHeight = 32;
        int barHeight = 14;
        int labelWidth = 140;
        int valueWidth = 110;
        int maxBarWidth = Math.max(50, width - labelWidth - valueWidth - 50);

        for (int i = 0; i < summaries.size(); i++) {
            CategorySummary cs = summaries.get(i);
            int y = startY + (i * rowHeight);
            if (y + rowHeight > height) break;

            Color barColor = PALETTE[i % PALETTE.length];

            // Category Label
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(new Color(50, 60, 70));
            String catName = cs.getCategory().getDisplayName();
            if (catName.length() > 18) catName = catName.substring(0, 16) + "..";
            g2.drawString(catName, 16, y + 12);

            // Bar background (light grey track)
            int barX = labelWidth + 10;
            int barY = y + 2;
            g2.setColor(new Color(235, 238, 245));
            g2.fillRoundRect(barX, barY, maxBarWidth, barHeight, 6, 6);

            // Progress bar
            int currentBarWidth = (int) Math.round((cs.getPercentage() / 100.0) * maxBarWidth);
            currentBarWidth = Math.max(3, Math.min(maxBarWidth, currentBarWidth));
            g2.setColor(barColor);
            g2.fillRoundRect(barX, barY, currentBarWidth, barHeight, 6, 6);

            // Amount and Percentage
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.setColor(new Color(40, 50, 75));
            String stats = String.format("%s (%.1f%%)", cs.getFormattedTotal(), cs.getPercentage());
            g2.drawString(stats, barX + maxBarWidth + 12, y + 13);
        }

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        int rows = Math.max(4, summaries.size());
        return new Dimension(380, 65 + (rows * 34));
    }
}
