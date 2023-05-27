package com.squareHackathon2023;

public class Ticket {
    private int seat;
    private String auth;

    public Ticket(int seat, String auth) {
        this.seat = seat;
        this.auth = auth;
    }

    public int getSeat() {
        return seat;
    }

    public String getAuth() {
        return auth;
    }
}