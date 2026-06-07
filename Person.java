package com.fleurairlines.model;

public abstract class Person {

    // ── Fields ───────────────────────────────────────────────────────────────
    private String id;
    private String name;
    private String email;
    private String phone;

    // ── Constructors ─────────────────────────────────────────────────────────
    protected Person() {
        this.id    = "";
        this.name  = "";
        this.email = "";
        this.phone = "";
    }

    public Person(String id, String name, String email, String phone) {
        if (id    == null || id.isBlank())    throw new IllegalArgumentException("ID cannot be empty.");
        if (name  == null || name.isBlank())  throw new IllegalArgumentException("Name cannot be empty.");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email cannot be empty.");

        this.id    = id;
        this.name  = name;
        this.email = email;
        this.phone = phone;
    }

    // ── Abstract ─────────────────────────────────────────────────────────────
    public abstract String getRole();

    // ── Getters ──────────────────────────────────────────────────────────────
    public String getId()    { return id; }
    public String getName()  { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }

    // ── Setters ──────────────────────────────────────────────────────────────

    // FIX 1: was "setId(int id)" — int can't be null-checked and can't assign to String.
    // FIX 2: was "i.isBlank()" — undefined variable, should be "id".
    // Solution: parameter is String; DatabaseService passes String.valueOf(rs.getInt("id")).
    public void setId(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("ID cannot be empty.");
        this.id = id;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name cannot be empty.");
        this.name = name;
    }

    public void setEmail(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email cannot be empty.");
        this.email = email;
    }

    public void setPhone(String phone) { this.phone = phone; }

    // ── toString ─────────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return String.format("Person{id='%s', name='%s', email='%s', phone='%s'}",
                id, name, email, phone);
    }
}