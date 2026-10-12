package com.expensetracker.model;

public enum PaymentMethod {
    UPI("UPI (GPay / PhonePe / Paytm)", "📱"),
    CASH("Cash", "💵"),
    DEBIT_CARD("Debit Card", "💳"),
    CREDIT_CARD("Credit Card", "💳"),
    NET_BANKING("Net Banking", "🏦"),
    OTHER("Other", "📝");

    private final String displayName;
    private final String emoji;

    PaymentMethod(String displayName, String emoji) {
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

    public static PaymentMethod fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return UPI;
        }
        String clean = text.trim();
        for (PaymentMethod pm : values()) {
            if (pm.name().equalsIgnoreCase(clean) || 
                pm.displayName.equalsIgnoreCase(clean) ||
                clean.toLowerCase().contains(pm.name().toLowerCase().replace("_", " "))) {
                return pm;
            }
        }
        return UPI;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
