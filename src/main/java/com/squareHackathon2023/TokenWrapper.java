package com.squareHackathon2023;

/**
 * TokenWrapper is a model object representing the token received from the front end.
 */
public class TokenWrapper {

    private String token;
    private String name;
    private String email;
    private String venueId;
    private int seatNum;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
    
    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getVenueId() {
      return venueId;
    }

    public void setVenueId(String venueId) { 
      this.venueId = venueId;
    }

    public int getSeatNum() {
      return seatNum;
    }

    public void setSeatNum(int seatNum) {
      this.seatNum = seatNum;
    }
}
