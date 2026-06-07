
package com.fleurairlines.model;

public class Reservation {

    
    public enum ReservationStatus { CONFIRMED, CANCELLED, CHECKED_IN, COMPLETED }
    private ReservationStatus status;
    private String bookingCode;
    private int passengerId;
    private int flightId;
    private int seatId;
    private Seat.SeatClass seatClass;
    private double totalPrice;
    private double extrasCost;
    private MealType mealType;
    private int extraLuggageCount;
    private String selectedDrinks; // comma-separated drink names

    public Reservation() {
        this.bookingCode = "";
        this.passengerId = 0;
        this.flightId = 0;
        this.seatId = 0;
        this.seatClass = Seat.SeatClass.ECONOMY;
        this.totalPrice = 0.0;
        this.extrasCost = 0.0;
        this.mealType = MealType.STANDARD;
        this.extraLuggageCount = 0;
        this.selectedDrinks = "";
        this.status = ReservationStatus.CONFIRMED;
    }

    public Reservation(String bookingCode, String passengerId, String flightNumber, String seatNumber, Seat.SeatClass seatClass, double totalPrice, ReservationStatus status) {
        this.bookingCode = bookingCode;
        this.passengerId = Integer.parseInt(passengerId);
        this.flightId = 0;
        this.seatId = 0;
        this.seatClass = seatClass;
        this.totalPrice = totalPrice;
        this.extrasCost = 0.0;
        this.mealType = MealType.STANDARD;
        this.extraLuggageCount = 0;
        this.selectedDrinks = "";
        this.status = status;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }

    public int getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(int passengerId) {
        this.passengerId = passengerId;
    }

    public int getFlightId() {
        return flightId;
    }

    public void setFlightId(int flightId) {
        this.flightId = flightId;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public Seat.SeatClass getSeatClass() {
        return seatClass;
    }

    public void setSeatClass(Seat.SeatClass seatClass) {
        this.seatClass = seatClass;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public double getExtrasCost() {
        return extrasCost;
    }

    public void setExtrasCost(double extrasCost) {
        this.extrasCost = extrasCost;
    }

    public MealType getMealType() {
        return mealType;
    }

    public void setMealType(MealType mealType) {
        if (mealType == null) {
            this.mealType = MealType.STANDARD;
        } else {
            this.mealType = mealType;
        }
    }

    public int getExtraLuggageCount() {
        return extraLuggageCount;
    }

    public void setExtraLuggageCount(int extraLuggageCount) {
        this.extraLuggageCount = Math.max(0, extraLuggageCount);
    }

    public String getSelectedDrinks() {
        return selectedDrinks;
    }

    public void setSelectedDrinks(String selectedDrinks) {
        this.selectedDrinks = selectedDrinks == null ? "" : selectedDrinks;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "bookingCode='" + bookingCode + '\'' +
                ", passengerId=" + passengerId +
                ", flightId=" + flightId +
                ", seatId=" + seatId +
                ", seatClass='" + seatClass + '\'' +
                ", totalPrice=" + totalPrice +
                ", status='" + status + '\'' +
                '}';
    }
}