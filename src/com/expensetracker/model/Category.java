package com.expensetracker.model;

public enum Category {
    FOOD("Food & Dining", "🍛"),
    GROCERIES("Groceries & Essentials", "🛒"),
    TRANSPORT("Transportation & Fuel", "🚗"),
    HOUSING("Housing & Rent", "🏠"),
    UTILITIES("Bills & Utilities (Power, Wifi)", "💡"),
    SHOPPING("Shopping", "🛍️"),
    ENTERTAINMENT("Entertainment & OTT", "🎬"),
    HEALTH("Healthcare & Medicine", "💊"),
    EDUCATION("Education & Fees", "📚"),
    PERSONAL("Personal Care", "🧴"),
    INVESTMENT("Investments & SIPs", "📈"),
    TRAVEL("Travel & Vacation", "✈️"),
    OTHER("Other", "📦");

    private final String displayName;
    private final String emoji;

    Category(String displayName, String emoji) {
        this.displayName = displayName;
        this.emoji = emoji;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getFormattedName() {
        return emoji + " " + displayName;
    }

    public static Category fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return OTHER;
        }
        String clean = text.trim();
        for (Category c : values()) {
            if (c.name().equalsIgnoreCase(clean) || 
                c.displayName.equalsIgnoreCase(clean) ||
                clean.toLowerCase().contains(c.name().toLowerCase())) {
                return c;
            }
        }
        return OTHER;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
