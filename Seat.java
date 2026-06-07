package com.fleurairlines.model;

public class Seat {

    public enum SeatClass { FIRST, BUSINESS, ECONOMY }

    private int       id;
    private String    seatNumber;
    private SeatClass seatClass;
    private double    price;
    private boolean   isAvailable;

    public Seat() {
        this("", SeatClass.ECONOMY, 0.0);
    }

    public Seat(String seatNumber, SeatClass seatClass, double price) {
        this.id = 0;
        this.seatNumber  = seatNumber;
        this.seatClass   = seatClass;
        this.price       = price;
        this.isAvailable = true;
    }

    public String    getSeatNumber()  { return seatNumber; }
    public int       getId()          { return id; }
    public SeatClass getSeatClass()   { return seatClass; }
    public double    getPrice()       { return price; }
    public boolean   isAvailable()    { return isAvailable; }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }
    public void setId(int id) {
        if (id < 0) throw new IllegalArgumentException("Seat id cannot be negative.");
        this.id = id;
    }

    public void setSeatClass(SeatClass seatClass) {
        this.seatClass = seatClass;
    }

    public void setAvailable(boolean available) { this.isAvailable = available; }
    public void setPrice(double price)          { this.price = price; }

    @Override
    public String toString() {
        return String.format("Seat{number='%s', class=%s, price=%.2f, available=%s}",
                seatNumber, seatClass, price, isAvailable);
    }
}