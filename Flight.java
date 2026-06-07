package com.fleurairlines.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Flight {

    private static final DateTimeFormatter FORMATTER     = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int               COLS_PER_ROW  = 6;
    private static final int               FIRST_ROWS    = 4; // First 4 rows are First Class
    private static final int               BUSINESS_ROWS = 4; // Rows 5 to 8 are Business Class

    private int              id;
    private String           flightNumber;
    private Airport          origin;
    private Airport          destination;
    private LocalDateTime    departureTime;
    private LocalDateTime    arrivalTime;
    private Aircraft         aircraft;
    private String           status;
    private final List<Seat> seats;

    public Flight() {
        this.id = 0;
        this.seats = new ArrayList<>();
    }

    public Flight(String flightNumber, Airport origin, Airport destination,
                  LocalDateTime departureTime, LocalDateTime arrivalTime,
                  Aircraft aircraft, String status) {
        this.flightNumber  = flightNumber;
        this.origin        = origin;
        this.destination   = destination;
        this.departureTime = departureTime;
        this.arrivalTime   = arrivalTime;
        this.aircraft      = aircraft;
        this.status        = (status != null) ? status : "SCHEDULED";
        this.seats         = new ArrayList<>();
        generateSeats();
    }

    private void generateSeats() {
        int totalRows = aircraft.getCapacity() / COLS_PER_ROW;
        for (int row = 1; row <= totalRows; row++) {
            Seat.SeatClass seatClass = classForRow(row);
            double         price     = priceForClass(seatClass, row);
            
            // Generates standardized structural seat names: 1A, 1B, 1C, 1D, 1E, 1F
            for (char col = 'A'; col <= 'F'; col++) {
                seats.add(new Seat(row + String.valueOf(col), seatClass, price));
            }
        }
    }

    private Seat.SeatClass classForRow(int row) {
        if (row <= FIRST_ROWS) return Seat.SeatClass.FIRST;
        if (row <= (FIRST_ROWS + BUSINESS_ROWS)) return Seat.SeatClass.BUSINESS;
        return Seat.SeatClass.ECONOMY;
    }

    private double priceForClass(Seat.SeatClass seatClass, int row) {
        return switch (seatClass) {
            case FIRST -> 500.00 + (row * 10);
            case BUSINESS -> 250.00 + (row * 5);
            default -> 100.00 + (row * 2);
        };
    }

    public List<Seat> findAvailableSeats() {
        return seats.stream().filter(Seat::isAvailable).collect(Collectors.toList());
    }

    public List<Seat> findAvailableSeats(Seat.SeatClass filter) {
        return seats.stream().filter(s -> s.isAvailable() && s.getSeatClass() == filter).collect(Collectors.toList());
    }

    public Seat findSeat(String seatNumber) {
        if (seatNumber == null) return null;
        return seats.stream().filter(s -> s.getSeatNumber().equals(seatNumber)).findFirst().orElse(null);
    }

    public String getDurationFormatted() {
        Duration duration = Duration.between(departureTime, arrivalTime);
        return String.format("%dh %02dm", duration.toHours(), duration.toMinutesPart());
    }

    public String  getFlightNumber()  { return flightNumber; }
    public int     getId()           { return id; }
    public Airport getOrigin()        { return origin; }
    public Airport getDestination()   { return destination; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public LocalDateTime getArrivalTime()   { return arrivalTime; }
    public Aircraft getAircraft()     { return aircraft; }
    public String  getStatus()        { return status; }
    public List<Seat> getSeats()      { return Collections.unmodifiableList(seats); }

    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
    public void setId(int id) { this.id = id; }
    public void setOrigin(Airport origin) { this.origin = origin; }
    public void setDestination(Airport destination) { this.destination = destination; }
    public void setDepartureTime(LocalDateTime t) { this.departureTime = t; }
    public void setArrivalTime(LocalDateTime t) { this.arrivalTime = t; }
    public void setAircraft(Aircraft aircraft) { this.aircraft = aircraft; }
    public void setStatus(String status) { this.status = status; }
    public void setSeats(List<Seat> newSeats) { this.seats.clear(); this.seats.addAll(newSeats); }
}