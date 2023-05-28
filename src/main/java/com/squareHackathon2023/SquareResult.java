package com.squareHackathon2023;

import java.util.List;

/**
 * PaymentResult is an object representing the response back to the front end.
 */
public class SquareResult {

  private String title;

  private List<com.squareup.square.models.Error> errors;

  public SquareResult(String t, List<com.squareup.square.models.Error> errorMessages) {
    this.title = t;
    this.errors = errorMessages;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTitle() {
    return this.title;
  }

  public void setErrors(List<com.squareup.square.models.Error> errors) {
    this.errors = errors;
  }

  public List<com.squareup.square.models.Error> getErrors() {
    return this.errors;
  }
}
