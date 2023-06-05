package com.squareHackathon2023;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.UUID;
import java.util.Random;

/**
 * Venue Class that holds total seats, venueId, and a dictionary with an integer key (seat number) and String value (authentication Id to match customer's note field)
 */
public class Venue {
    private String venueName;
    private String eventName;
    private int totalSeats;
    private String venueId;
    private Dictionary<Integer, Seat> seats;

    public Venue() {
        venueName = "";
        eventName = "";
        totalSeats = 0;
        venueId = UUID.randomUUID().toString();
        seats = new Hashtable<>();
    }

    public Venue(String venueName, String eventName, int totalSeats) {
        this.venueName = venueName;
        this.eventName = eventName;
        this.totalSeats = totalSeats;
        venueId = UUID.randomUUID().toString();
        seats = new Hashtable<>();
        fill(totalSeats);
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String name) {
        this.venueName = name;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public String getVenueId() {
        return venueId;
    }

    public Seat findSeat(int seatNum) {
        System.out.println("Looking for seat: " + seatNum);
        
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
     * Creates seats
     * @param totalSeats
     */
    public void fill(int totalSeats) {
        Random random = new Random();
        this.totalSeats = totalSeats;
        boolean val;
        for(int i = 0; i < totalSeats; i++) {
            // Random prices for proof of concept
            long price = random.nextInt(100) + 70;
            val = new Random().nextInt(4)==0;

            seats.put(i, new Seat(i, price, val)); 
        }
        
        // Testing
        //seats.get(2).arrive();
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
