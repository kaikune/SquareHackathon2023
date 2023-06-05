package com.squareHackathon2023;

import java.util.UUID;

public class Seat {
    private int num;
    private long price;
    private String auth;
    private boolean sold;
    private boolean arrived;

    public Seat() {
        price = 100L;
        auth = UUID.randomUUID().toString();
        sold = false;
        arrived = false;
    }

    public Seat(int num, long price) {
        this.num = num;
        this.price = price;
        auth = UUID.randomUUID().toString();
        sold = false;
        arrived = false;
    }

    public Seat(int num, long price, boolean sold) {
        this.num = num;
        this.price = price;
        auth = UUID.randomUUID().toString();
        this.sold = sold;
        arrived = false;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
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
