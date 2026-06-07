package com.fleurairlines.model;

public class Passenger extends Person {

    // ── Fields ───────────────────────────────────────────────────────────────
    private String passportNumber;
    private String nationality;
    private String passwordHash;
    private String passwordSalt;
    private int    loyaltyPoints;
    private String preferredCurrency;
    private String dateOfBirth;  // ISO format: yyyy-MM-dd

    // ── Constructors ─────────────────────────────────────────────────────────
    public Passenger() {
        super();
        this.passportNumber = "";
        this.nationality    = "";
        this.passwordHash   = "";
        this.passwordSalt   = "";
        this.loyaltyPoints  = 0;
        this.preferredCurrency = "USD";
        this.dateOfBirth    = "";
    }

    public Passenger(String id, String name, String email, String phone,
                     String passportNumber, String nationality,
                     String passwordHash, String passwordSalt) {
        super(id, name, email, phone);
        if (passportNumber == null || passportNumber.isBlank()) throw new IllegalArgumentException("Passport number cannot be empty.");
        if (nationality    == null || nationality.isBlank())    throw new IllegalArgumentException("Nationality cannot be empty.");
        if (passwordHash   == null || passwordHash.isBlank())   throw new IllegalArgumentException("Password hash cannot be empty.");
        if (passwordSalt   == null || passwordSalt.isBlank())   throw new IllegalArgumentException("Password salt cannot be empty.");

        this.passportNumber = passportNumber;
        this.nationality    = nationality;
        this.passwordHash   = passwordHash;
        this.passwordSalt   = passwordSalt;
        this.loyaltyPoints  = 0;
    }

    // ── Abstract implementation ───────────────────────────────────────────────
    @Override
    public String getRole() { return "PASSENGER"; }

    // ── Loyalty points ────────────────────────────────────────────────────────
    public void addLoyaltyPoints(int points) {
        if (points < 0) throw new IllegalArgumentException("Points cannot be negative.");
        this.loyaltyPoints += points;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public String getPassportNumber() { return passportNumber; }
    public String getNationality()    { return nationality; }
    public String getPasswordHash()   { return passwordHash; }
    public String getPasswordSalt()   { return passwordSalt; }
    public int    getLoyaltyPoints()  { return loyaltyPoints; }
    public String getPreferredCurrency() { return preferredCurrency; }
    public String getDateOfBirth()    { return dateOfBirth; }
    
    // Calculate age from dateOfBirth (yyyy-MM-dd format)
    public int getAge() {
        if (dateOfBirth == null || dateOfBirth.isBlank()) return 0;
        try {
            java.time.LocalDate dob = java.time.LocalDate.parse(dateOfBirth);
            return java.time.Period.between(dob, java.time.LocalDate.now()).getYears();
        } catch (Exception ex) {
            return 0;
        }
    }

    // ── Setters ──────────────────────────────────────────────────────────────
    // FIX: Removed 3 broken auto-generated stubs that were overriding Person's
    //      setId(int), setPhone(long), and setName(String) with UnsupportedOperationException.
    //      Person already provides correct implementations for all three.

    public void setPassportNumber(String passportNumber) {
        if (passportNumber == null || passportNumber.isBlank()) throw new IllegalArgumentException("Passport number cannot be empty.");
        this.passportNumber = passportNumber;
    }
    public void setNationality(String nationality)   { this.nationality  = nationality; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setPasswordSalt(String passwordSalt) { this.passwordSalt = passwordSalt; }
    public void setLoyaltyPoints(int loyaltyPoints)  { this.loyaltyPoints = loyaltyPoints; }
    public void setPreferredCurrency(String preferredCurrency) {
        if (preferredCurrency == null || preferredCurrency.isBlank()) {
            this.preferredCurrency = "USD";
            return;
        }
        this.preferredCurrency = preferredCurrency;
    }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth != null ? dateOfBirth : ""; }

    // ── toString ─────────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return String.format(
                "Passenger{id='%s', name='%s', email='%s', phone='%s', passport='%s', nationality='%s', loyaltyPoints=%d}",
                getId(), getName(), getEmail(), getPhone(),
                passportNumber, nationality, loyaltyPoints);
    }
}