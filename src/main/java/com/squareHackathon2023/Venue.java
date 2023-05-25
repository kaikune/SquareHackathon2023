package com.squareHackathon2023;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.UUID;

/**
 * Venue Class that holds total seats, venueId, and a dictionary with an integer key (seat number) and String value (authentication Id to match customer's note field)
 */
public class Venue {
    private String name;
    private int totalSeats;
    private String venueId;
    private Dictionary<Integer, Seat> seats;

    public Venue() {
        name = "";
        totalSeats = 0;
        venueId = UUID.randomUUID().toString();
        seats = new Hashtable<>();
    }

    public Venue(String name, int totalSeats) {
        this.name = name;
        this.totalSeats = totalSeats;
        venueId = UUID.randomUUID().toString();
        seats = new Hashtable<>();
        fill(totalSeats);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public int getTotalSeats() {
        return totalSeats;
    }

    public String getVenueId() {
        return venueId;
    }

    public Seat findSeat(int seatNum) {
        try {
            return seats.get(seatNum);
        }
        catch (NullPointerException e) {
            return null;
        }
    }

    public Dictionary<Integer, Seat> getSeats() {
        return seats;
    }

    /**
     * Fills seats with authentication ids
     * @param totalSeats
     */
    public void fill(int totalSeats) {
        this.totalSeats = totalSeats;

        for(int i = 0; i < totalSeats; i++) {
            seats.put(i, new Seat());   // TODO: Edit to create price for seats
        }
    }

    public String toString() {
        String str = "";
        Enumeration<Integer> k = seats.keys();
        while (k.hasMoreElements()) {
            int key = k.nextElement();
            str = str + "Seat: " + key + ", Auth: " + seats.get(key) + "\n";
        }
        return str;
    }
}
