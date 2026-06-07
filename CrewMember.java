package com.fleurairlines.model;

public class CrewMember {
    private String id;
    private String name;
    private String role;
    private int experience;

    public CrewMember(String id, String name, String role, int experience) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.experience = experience;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    @Override
    public String toString() {
        return "CrewMember{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", role='" + role + '\'' +
                ", experience=" + experience +
                '}';
    }
}