/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squareHackathon2023;

import com.squareup.square.Environment;
import com.squareup.square.api.PaymentsApi;
import com.squareup.square.api.CustomersApi;
import com.squareup.square.api.DevicesApi;
import com.squareup.square.api.CardsApi;
import com.squareup.square.api.TerminalApi;
import com.squareup.square.models.*;
// import com.squareup.square.utilities.JsonObject;
import com.squareup.square.SquareClient;
import com.squareup.square.exceptions.ApiException;

// import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


@Controller
@SpringBootApplication
//@RequestMapping("/ticket-selling/") // Set the base URL
public class Main {
  protected final SquareClient squareClient;
  protected final String squareLocationId;
  protected final String squareAppId;
  protected final String squareEnvironment;

  protected final Venue venue = new Venue("Concert Central", "Ice Spice", 10);  // TODO: Would need to change if implementing multiple venues
  private final Gson gson = new Gson();
  private String deviceId = "9fa747a2-25ff-48ee-b078-04381f7c828f";

  public Main() throws ApiException {
    squareEnvironment = mustLoadEnvironmentVariable(System.getenv("ENVIRONMENT"));
    squareAppId = mustLoadEnvironmentVariable(System.getenv("SQUARE_APPLICATION_ID"));
    squareLocationId = mustLoadEnvironmentVariable(System.getenv("SQUARE_LOCATION_ID"));

    squareClient = new SquareClient.Builder()
        .environment(Environment.fromString(squareEnvironment))
        .accessToken(mustLoadEnvironmentVariable(System.getenv("SQUARE_ACCESS_TOKEN")))
        .userAgentDetail("Ticketing") // Remove or replace this detail when building your own app
        .build();
  }

  public static void main(String[] args) throws Exception {
    SpringApplication.run(Main.class, args);
  }

  private String mustLoadEnvironmentVariable(String value) {
    if (value == null || value.length() == 0) {
      throw new IllegalStateException(
          String.format("An environment variable is missing"));
    }
    return value;
  }

  @RequestMapping("/")
  String index(Map<String, Object> model) throws InterruptedException, ExecutionException {

    // Get currency and country for location
    RetrieveLocationResponse locationResponse = getLocationInformation(squareClient).get();
    model.put("paymentFormUrl", squareEnvironment.equals("sandbox") ? "https://sandbox.web.squarecdn.com/v1/square.js" : "https://web.squarecdn.com/v1/square.js");
    model.put("locationId", squareLocationId);
    model.put("appId", squareAppId);
    model.put("currency", locationResponse.getLocation().getCurrency());
    model.put("country", locationResponse.getLocation().getCountry());
    model.put("idempotencyKey", UUID.randomUUID().toString());

    return "index";
  }

  @RequestMapping("/check-in")
  String checkIn(Map<String, Object> model) throws InterruptedException, ExecutionException {

    // Get currency and country for location
    RetrieveLocationResponse locationResponse = getLocationInformation(squareClient).get();
    model.put("paymentFormUrl", squareEnvironment.equals("sandbox") ? "https://sandbox.web.squarecdn.com/v1/square.js" : "https://web.squarecdn.com/v1/square.js");
    model.put("locationId", squareLocationId);
    model.put("appId", squareAppId);
    model.put("currency", locationResponse.getLocation().getCurrency());
    model.put("country", locationResponse.getLocation().getCountry());
    model.put("idempotencyKey", UUID.randomUUID().toString());

    return "checkIn";
  }

  /**
   * Returns status of the venue. To be called repeatedly by frontend.
   */
  @GetMapping("/venue")
  @ResponseBody
  public Venue getSeats() {
    return venue;
  }

  /**
   * Function that processes a payment given a token. Called by a POST to /process-paymet
   * @param tokenObject
   * @return
   * @throws InterruptedException
   * @throws ExecutionException
   */
  @PostMapping("/process-payment")
  @ResponseBody
  SquareResult processPayment(@RequestBody TokenWrapper tokenObject) throws InterruptedException, ExecutionException {
    PaymentsApi paymentsApi = squareClient.getPaymentsApi();

    // Get currency for location
    RetrieveLocationResponse locationResponse = getLocationInformation(squareClient).get();
    String currency = locationResponse.getLocation().getCurrency();

    // TODO: Add billing address here and in frontend

    Seat seat = venue.findSeat(tokenObject.getSeatNum()); // Gets auth if seat number corresponds

    // Set price of ticket
    Money bodyAmountMoney = new Money.Builder()
        .amount(seat.getPrice())
        .currency(currency)
        .build();

    // Payment request that binds to the customer
    CreatePaymentRequest createPaymentRequest = new CreatePaymentRequest.Builder(
        tokenObject.getToken(),
        UUID.randomUUID().toString())
        .amountMoney(bodyAmountMoney)
        .locationId(squareLocationId)
        .build();
    
    //System.out.println("Source Id: " + tokenObject.getToken());

    return (
      paymentsApi.createPaymentAsync(createPaymentRequest)
        .thenApply(result -> {
          // Create customer and add card to file
          createCustomer(tokenObject, result.getPayment().getId());
          System.out.printf("Fingerprint: %s\n", result.getPayment().getCardDetails().getCard().getFingerprint());
          System.out.println("Payment Request Success!");
          return new SquareResult("SUCCESS", null);
        })
        .exceptionally(exception -> {
          ApiException e = (ApiException) exception.getCause();
          System.out.println("Failed to make the request 1");
          System.out.printf("Exception: %s%n", e.getMessage());
          return new SquareResult("FAILURE", e.getErrors());
        }).join()
    );
  }

  /**
   * Controls the connection of a Square Terminal to the application.
   * @return SquareResult with the device code
   */
  @GetMapping("/connect")
  @ResponseBody
  SquareResult connectToTerminal() {
    DevicesApi devicesApi = squareClient.getDevicesApi();
    DeviceCode deviceCode = new DeviceCode.Builder("TERMINAL_API")
      .name("Check-In")
      .locationId(squareLocationId)
      .build();

    CreateDeviceCodeRequest body = new CreateDeviceCodeRequest.Builder(UUID.randomUUID().toString(), deviceCode)
      .build();

    return devicesApi.createDeviceCodeAsync(body)
      .thenApply(result -> {
        System.out.println("Device code aquired!");
        return new SquareResult(result.getDeviceCode().getCode(), null);
      })
      .exceptionally(exception -> {
        ApiException e = (ApiException) exception.getCause();
        System.out.println("Failed to make the device request");
        System.out.printf("Exception: %s%n", e.getMessage());
        return new SquareResult("FAILURE", e.getErrors());
      }).join();
  }

  @PostMapping("/device")
  @ResponseBody
  void getDeviceId(@RequestBody String deviceJson) {
    TerminalResult result = gson.fromJson(deviceJson, TerminalResult.class);

    deviceId = result.getDeviceId();
    System.out.println("Device Id: " + deviceId);
  }

  /**
   * Creates checkout request for the connected terminal
   * @param deviceId
   * @return
   */
  @GetMapping("/verify")
  @ResponseBody
  SquareResult sendCheckoutRequest() {
    Money amountMoney = new Money.Builder()
      .amount(1L)
      .currency("USD")
      .build();

    PaymentOptions paymentOptions = new PaymentOptions.Builder()
      .autocomplete(false)
      .delayDuration("PT1M")  // cancels payment automatically after 1 minute
      .acceptPartialAuthorization(false)
      .delayAction("CANCEL")
      .build();

    DeviceCheckoutOptions deviceOptions = new DeviceCheckoutOptions.Builder(deviceId) // DeviceId from connectToTerminal
      .skipReceiptScreen(true)
      .collectSignature(false)
      .build();

    TerminalCheckout checkout = new TerminalCheckout.Builder(amountMoney, deviceOptions)
      .note("Tap Here to check in")
      .paymentOptions(paymentOptions)
      .paymentType("CARD_PRESENT")
      .build();

    CreateTerminalCheckoutRequest body = new CreateTerminalCheckoutRequest.Builder(UUID.randomUUID().toString(), checkout)
      .build();

    TerminalApi terminalApi = squareClient.getTerminalApi();

    return (
      terminalApi.createTerminalCheckoutAsync(body)
        .thenApply(result -> {
          System.out.println("Checkout Request Success");
          return new SquareResult("SUCCESS", null);
        })
        .exceptionally(exception -> {
          ApiException e = (ApiException) exception.getCause();
          System.out.println("Failed to make the checkout request");
          System.out.printf("Exception: %s%n", e.getMessage());
          return new SquareResult("FAILURE", e.getErrors());
        }).join()
    );
  }

  /**
   * Recieves JSON from Square on payment.created and checks card to see if there is a seat associated with customer
   * @param String
   * @return
   * @throws InterruptedException
   * @throws ExecutionException
   */
  @PostMapping("/process-verification")
  @ResponseBody
  void getCardInfo(@RequestBody String paymentJson) throws InterruptedException, ExecutionException{
    // TerminalResult result = gson.fromJson(paymentJson, TerminalResult.class);
    System.out.println("In getCardInfo()");

    // Parse the JSON string
    JsonObject result = gson.fromJson(paymentJson, JsonObject.class);
    result = result.getAsJsonObject("data")
        .getAsJsonObject("object")
        .getAsJsonObject("payment");

    System.out.println(result);
    String note = result.get("note").getAsString();
    System.out.println("Note: " + note);

    // Test for check-in JSON
    if (note.compareTo("Tap Here to check in") == 0) {
      // Access the nested card details
      JsonObject cardDetails = result.getAsJsonObject("payment")
          .getAsJsonObject("card_details");

    // Extract the fingerprint
    String fingerprint = cardDetails.getAsJsonObject("card")
        .get("fingerprint")
        .getAsString();

    System.out.println("Fingerprint: " + fingerprint);

    // Test for check-in JSON
    if (note.compareTo("Tap Here to check in") == 0) {
      int seatNum = validateCard(fingerprint);
      
      //Check in attendee
      if (seatNum != -1) {
        System.out.println("Seat found!");
        venue.findSeat(seatNum).arrive();
      } else {
        System.out.println("Seat not found");
      }
    }
    else {
      System.out.println("Ignoring payment");
    }
    
  }

  /**
   * Method that grabs all of the customers and compares their filed cards with a given card
   * @param fingerprint
   * @return seat number (-1 if invalid)
   */
  public int validateCard(String fingerprint) {
    CustomersApi customersApi = squareClient.getCustomersApi();
    AtomicInteger seat = new AtomicInteger(-1);
    
    System.out.println("in validateCard()");

    try {
      CompletableFuture<Void> future = customersApi.listCustomersAsync(null, null, null, null)
      .thenAccept(result -> {
        System.out.println("List Customers Success!");
  
        if (result.getCustomers() != null) {
          for (Customer customer : result.getCustomers()) {
            if (isCardOnFile(customer, fingerprint)) {
              Ticket ticket = gson.fromJson(customer.getNote(), Ticket.class);
              Seat currSeat = venue.findSeat(ticket.getSeat());

              System.out.println("Found customer");

              // Check ticket for validity
              if (ticket.getAuth() == currSeat.getAuth()) {
                System.out.printf("Ticket for seat %d Valid!\n", ticket.getSeat());
                seat.set(ticket.getSeat());
              } else {
                System.out.printf("Unable to validate ticket %d\n", ticket.getSeat());
              }
              return;
            }
          }
        }
      })
      .exceptionally(exception -> {
        System.out.println("Failed to make the request");
        System.out.println(String.format("Exception: %s", exception.getMessage()));
        return null;
      });
      future.join();
      
    } catch (Exception e) {
      System.out.println(String.format("Exception: %s", e.getMessage()));
    }
  
    return seat.get();
  }

  private boolean isCardOnFile(Customer customer, String fingerprint) {
    // Retrieve the list of cards associated with the customer
    List<Card> cards = customer.getCards();

    // Check if the received card data matches any of the cards on file
    if (cards != null) {
      for (Card card : cards) {
        if (card.getFingerprint().equals(fingerprint)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Helper function for createCustomer that creates a card with a given customerId and paymentId
   * @param tokenObject
   * @param customerId
   * @param paymentId
   */
  private void createCard(TokenWrapper tokenObject, String customerId, String paymentId) { 
    CardsApi cardsApi = squareClient.getCardsApi();

    Card card = new Card.Builder()
      .cardholderName(tokenObject.getName())
      //.billingAddress("Where to get billing address")
      .customerId(customerId)
      .build();
    
    CreateCardRequest body = new CreateCardRequest.Builder(
      UUID.randomUUID().toString(),
      paymentId,
      card)
      .build();
    
    cardsApi.createCardAsync(body)
      .thenAccept(result -> {
        System.out.println("Card Request Success!");
      })
      .exceptionally(exception -> {
        System.out.println("Failed to make the request 2");
        System.out.println(String.format("Exception: %s", exception.getMessage()));
        return null;
      });
  }

  /**
   * Requests customer creation
   * @param tokenObject
   * @return a future that holds the customerId
   */
  protected void createCustomer(TokenWrapper tokenObject, String paymentId) {  
    CustomersApi customersApi = squareClient.getCustomersApi();

    Seat seat = venue.findSeat(tokenObject.getSeatNum()); // Gets auth if seat number corresponds
    String note = "No ticket";

    // Debug prints
    // System.out.printf("Seat #: %s vs. token seat: %s\n", tokenObject.getSeatNum(), seat);
    // System.out.printf("Venue #: %s vs. venue: %s\n", tokenObject.getVenueId(), venue.getVenueId());
    // System.out.println("Sold? " + seat.isSold());
    // System.out.println("auth: " + seat.getAuth());

    // Venue matches, seat exists, and seat is still for sale
    if (venue.getVenueId().equals(tokenObject.getVenueId()) && seat != null && !seat.isSold()) {
      Ticket ticket = new Ticket(tokenObject.getSeatNum(), seat.getAuth()); // Creates ticket

      note = gson.toJson(ticket); // Converts ticket to string JSON
      System.out.println(note);

      seat.sell();
    }

    CreateCustomerRequest customer = new CreateCustomerRequest.Builder()
        .givenName(tokenObject.getName())
        .emailAddress(tokenObject.getEmail())
        .referenceId(UUID.randomUUID().toString())
        .note(note) // Holds authentication id
        .build();
    
    customersApi.createCustomerAsync(customer)
      .thenAccept(result -> {
        String customerId = result.getCustomer().getId();
        System.out.println("Customer Request Success!");
        createCard(tokenObject, customerId, paymentId);
      })
      .exceptionally(exception -> {
        System.out.println("Failed to make the request 3");
        System.out.println(String.format("Exception: %s", exception.getMessage()));
        System.out.println("Check if email is valid");
        return null;
      });
  }

  /**
   * Helper method that makes a retrieveLocation API call using the configured
   * locationId and returns the future containing the response
   *
   * @param squareClient the API client
   * @return a future that holds the retrieveLocation response
   */
  protected CompletableFuture<RetrieveLocationResponse> getLocationInformation(SquareClient squareClient) {
    return squareClient.getLocationsApi().retrieveLocationAsync(squareLocationId)
        .thenApply(result -> {
          return result;
        })
        .exceptionally(exception -> {
          System.out.println("Failed to make the request 4");
          System.out.printf("Exception: %s%n", exception.getMessage());
          return null;
        });
  }
}

