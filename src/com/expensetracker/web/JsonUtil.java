package com.expensetracker.web;

import com.expensetracker.model.*;
import com.expensetracker.service.CategorySummary;
import com.expensetracker.service.MonthlySummary;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonUtil {

    public static String toJson(Expense e) {
        if (e == null) return "null";
        return String.format(Locale.US,
                "{\"id\":\"%s\",\"date\":\"%s\",\"title\":\"%s\",\"category\":\"%s\",\"categoryName\":\"%s\",\"categoryEmoji\":\"%s\",\"amount\":%.2f,\"paymentMethod\":\"%s\",\"paymentMethodName\":\"%s\",\"notes\":\"%s\"}",
                escape(e.getId()),
                escape(e.getFormattedDate()),
                escape(e.getTitle()),
                escape(e.getCategory().name()),
                escape(e.getCategory().getDisplayName()),
                escape(e.getCategory().getEmoji()),
                e.getAmount(),
                escape(e.getPaymentMethod().name()),
                escape(e.getPaymentMethod().getDisplayName()),
                escape(e.getNotes())
        );
    }

    public static String toJson(List<Expense> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(toJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String summaryToJson(double totalAllTime, double monthTotal, Budget budget, 
                                      int txCount, double dailyAvg, Expense highest,
                                      List<CategorySummary> catSummaries, List<MonthlySummary> monthlySummaries) {
        StringBuilder sb = new StringBuilder("{");
        sb.append(String.format(Locale.US, "\"totalAllTime\":%.2f,", totalAllTime));
        sb.append(String.format(Locale.US, "\"monthTotal\":%.2f,", monthTotal));
        sb.append(String.format(Locale.US, "\"budgetLimit\":%.2f,", budget.getMonthlyLimit()));
        sb.append(String.format(Locale.US, "\"budgetRemaining\":%.2f,", budget.getRemaining()));
        sb.append(String.format(Locale.US, "\"budgetPercentage\":%.1f,", budget.getPercentageUsed()));
        sb.append(String.format("\"budgetStatus\":\"%s\",", budget.getStatus().name()));
        sb.append(String.format("\"budgetStatusLabel\":\"%s\",", budget.getStatus().getLabel()));
        sb.append(String.format("\"txCount\":%d,", txCount));
        sb.append(String.format(Locale.US, "\"dailyAverage\":%.2f,", dailyAvg));

        if (highest != null) {
            sb.append("\"highestExpense\":").append(toJson(highest)).append(",");
        } else {
            sb.append("\"highestExpense\":null,");
        }

        // Category breakdown
        sb.append("\"categories\":[");
        for (int i = 0; i < catSummaries.size(); i++) {
            CategorySummary cs = catSummaries.get(i);
            sb.append(String.format(Locale.US,
                    "{\"name\":\"%s\",\"displayName\":\"%s\",\"emoji\":\"%s\",\"total\":%.2f,\"percentage\":%.1f,\"count\":%d}",
                    escape(cs.getCategory().name()),
                    escape(cs.getCategory().getDisplayName()),
                    escape(cs.getCategory().getEmoji()),
                    cs.getTotalAmount(),
                    cs.getPercentage(),
                    cs.getCount()));
            if (i < catSummaries.size() - 1) sb.append(",");
        }
        sb.append("],");

        // Monthly breakdown
        sb.append("\"monthly\":[");
        for (int i = 0; i < monthlySummaries.size(); i++) {
            MonthlySummary ms = monthlySummaries.get(i);
            sb.append(String.format(Locale.US,
                    "{\"month\":\"%s\",\"label\":\"%s\",\"total\":%.2f,\"count\":%d}",
                    escape(ms.getMonth().toString()),
                    escape(ms.getFormattedMonth()),
                    ms.getTotalAmount(),
                    ms.getCount()));
            if (i < monthlySummaries.size() - 1) sb.append(",");
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    public static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;

        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([0-9.-]+|true|false|null))");
        Matcher matcher = pattern.matcher(json);
        while (matcher.find()) {
            String key = matcher.group(1);
            String val = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            map.put(key, val);
        }
        return map;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
