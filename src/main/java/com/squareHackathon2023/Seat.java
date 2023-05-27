package com.squareHackathon2023;

import java.util.UUID;

public class Seat {
    private long price;
    private String auth;
    private boolean sold;
    private boolean arrived;

    public Seat() {
        price = 100L;
        auth = UUID.randomUUID().toString();
        sold = false;
    }

    public Seat(long price) {
        this.price = price;
        auth = UUID.randomUUID().toString();
        sold = false;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public String getAuth() {
        return auth;
    }

    public boolean isSold() {
        return sold;
    }

    public void sell() {
        sold = true;
    }

    public boolean isArrived() {
        return arrived;
    }

    public void arrive() {
        arrived = true;
    }
}
