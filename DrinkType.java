package com.fleurairlines.model;

public enum DrinkType {
    WATER(0, "Water"),
    COFFEE(0, "Coffee"),
    TEA(0, "Tea"),
    PEPSI(5, "Pepsi"),
    COKE(5, "Coke"),
    SPRITE(5, "Sprite"),
    WINE(12, "Wine"),
    BEER(10, "Beer"),
    WHISKEY(15, "Whiskey");

    private final double price;
    private final String displayName;

    DrinkType(double price, String displayName) {
        this.price = price;
        this.displayName = displayName;
    }

    public double getPrice() {
        return price;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAlcoholic() {
        return this == WINE || this == BEER || this == WHISKEY;
    }
}
