package com.fleurairlines.model;

public class Admin extends Person {

    // ── Fields ───────────────────────────────────────────────────────────────
    private String department;
    private int    permissionLevel;
    private String passwordHash;
    private String passwordSalt;

    // ── Constructor ──────────────────────────────────────────────────────────
    public Admin(String string, String super_Admin, String adminfleurcom, String string1, String operations, boolean par, String hash, String salt) {
        super();
        this.department = "";
        this.permissionLevel = 1;
        this.passwordHash = "";
        this.passwordSalt = "";
    }

    // No-arg constructor needed for ORM/DAO population and deserialization.
    public Admin() {
        super();
        this.department = "";
        this.permissionLevel = 1;
        this.passwordHash = "";
        this.passwordSalt = "";
    }

    public Admin(String id, String name, String email, String phone,
                 String passwordHash, String passwordSalt,
                 String department, int permissionLevel) {
        super(id, name, email, phone);
        if (passwordHash  == null || passwordHash.isBlank())  throw new IllegalArgumentException("Password hash cannot be empty.");
        if (passwordSalt  == null || passwordSalt.isBlank())  throw new IllegalArgumentException("Password salt cannot be empty.");
        if (department    == null || department.isBlank())    throw new IllegalArgumentException("Department cannot be empty.");
        if (permissionLevel < 1 || permissionLevel > 2)      throw new IllegalArgumentException("Permission level must be 1 (standard) or 2 (super admin).");

        this.passwordHash   = passwordHash;
        this.passwordSalt   = passwordSalt;
        this.department     = department;
        this.permissionLevel = permissionLevel;
    }

    // ── Abstract implementation ───────────────────────────────────────────────
    @Override
    public String getRole() { return "ADMIN"; }

    // ── Permission helpers ────────────────────────────────────────────────────
    public boolean isSuperAdmin()    { return permissionLevel == 2; }
    public boolean isStandardAdmin() { return permissionLevel == 1; }

    // ── Getters & Setters ────────────────────────────────────────────────────
    public String getDepartment()     { return department; }
    public int    getPermissionLevel(){ return permissionLevel; }
    public String getPasswordHash()   { return passwordHash; }
    public String getPasswordSalt()   { return passwordSalt; }

    public void setDepartment(String department) {
        if (department == null || department.isBlank()) throw new IllegalArgumentException("Department cannot be empty.");
        this.department = department;
    }
    public void setPermissionLevel(int permissionLevel) {
        if (permissionLevel < 1 || permissionLevel > 2) throw new IllegalArgumentException("Permission level must be 1 or 2.");
        this.permissionLevel = permissionLevel;
    }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setPasswordSalt(String passwordSalt) { this.passwordSalt = passwordSalt; }

    // ── toString ─────────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return String.format(
                "Admin{id='%s', name='%s', email='%s', phone='%s', department='%s', permissionLevel=%d}",
                getId(), getName(), getEmail(), getPhone(),
                department, permissionLevel);
    }
}