package com.fleurairlines.model;
 
public class Ticket {
    private String ticketId;
    private String passengerId;
    private String flightNumber;
    private double price;

    public Ticket(String ticketId, String passengerId, String flightNumber, double price) {
        this.ticketId = ticketId;
        this.passengerId = passengerId;
        this.flightNumber = flightNumber;
        this.price = price;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(String passengerId) {
        this.passengerId = passengerId;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "Ticket{" +
                "ticketId='" + ticketId + '\'' +
                ", passengerId='" + passengerId + '\'' +
                ", flightNumber='" + flightNumber + '\'' +
                ", price=" + price +
                '}';
    }
}