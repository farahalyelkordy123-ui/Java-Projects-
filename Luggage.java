package com.fleurairlines.model;

public class Luggage {
    private int extraLuggageCount;
    private double extraLuggagePrice;

    public static final double EXTRA_LUGGAGE_PRICE = 35.0; // per luggage

    public Luggage() {
        this.extraLuggageCount = 0;
        this.extraLuggagePrice = 0.0;
    }

    public Luggage(int extraLuggageCount) {
        this.extraLuggageCount = extraLuggageCount;
        this.extraLuggagePrice = extraLuggageCount * EXTRA_LUGGAGE_PRICE;
    }

    public int getExtraLuggageCount() {
        return extraLuggageCount;
    }

    public void setExtraLuggageCount(int count) {
        this.extraLuggageCount = count;
        this.extraLuggagePrice = count * EXTRA_LUGGAGE_PRICE;
    }

    public double getExtraLuggagePrice() {
        return extraLuggagePrice;
    }

    @Override
    public String toString() {
        return "Luggage{" +
                "extraCount=" + extraLuggageCount +
                ", price=$" + String.format("%.2f", extraLuggagePrice) +
                '}';
    }
}
