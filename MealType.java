package com.fleurairlines.model;

public enum MealType {
    STANDARD(0.0, "Standard Meal"),
    KOSHER(0.0, "Kosher Meal"),
    HALAL(0.0, "Halal Meal"),
    VEGAN(0.0, "Vegan Meal"),
    SEAFOOD(50.0, "Seafood Premium");

    private final double price;
    private final String displayName;

    MealType(double price, String displayName) {
        this.price = price;
        this.displayName = displayName;
    }

    public double getPrice() {
        return price;
    }

    public String getDisplayName() {
        return displayName;
    }
}