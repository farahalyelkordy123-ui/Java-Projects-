
package com.fleurairlines.model;

public class Airport {
    private String airportCode;
    private String name;
    private String location;

    public Airport() {
        this("", "", "");
    }

    public Airport(String airportCode, String name, String location) {
        this.airportCode = airportCode;
        this.name = name;
        this.location = location;
    }

    public String getAirportCode() {
        return airportCode;
    }

    public void setAirportCode(String airportCode) {
        this.airportCode = airportCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "Airport{" +
                "airportCode='" + airportCode + '\'' +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}