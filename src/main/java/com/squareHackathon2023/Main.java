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
import com.squareup.square.SquareClient;
import com.squareup.square.exceptions.ApiException;
import com.squareup.square.utilities.WebhooksHelper;

import java.util.concurrent.ExecutionException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;


@Controller
@SpringBootApplication
public class Main {
  protected final SquareClient squareClient;
  protected final String squareLocationId;
  protected final String squareAppId;
  protected final String squareEnvironment;
  protected final String squarePaymentWebhook;
  protected final String squareDeviceWebhook;

  private static CompletableFuture<String> seated = new CompletableFuture<>();
  
  // Hardcoded event for testing
  protected static final Venue venue = new Venue("Concert Central", "Billy Joel", 100);
  private final Gson gson = new Gson();

  private String deviceId = "9fa747a2-25ff-48ee-b078-04381f7c828f"; // Hardcoded deviceId for testing

  public Main() throws ApiException {
    squareEnvironment = mustLoadEnvironmentVariable(System.getenv("ENVIRONMENT"));
    squareAppId = mustLoadEnvironmentVariable(System.getenv("SQUARE_APPLICATION_ID"));
    squareLocationId = mustLoadEnvironmentVariable(System.getenv("SQUARE_LOCATION_ID"));
    squarePaymentWebhook = Main.mustLoadEnvironmentVariable(System.getenv("WEBHOOK_KEY"));
    squareDeviceWebhook = Main.mustLoadEnvironmentVariable(System.getenv("WEBHOOK_DEVICE_KEY"));

    squareClient = new SquareClient.Builder()
        .environment(Environment.fromString(squareEnvironment))
        .accessToken(mustLoadEnvironmentVariable(System.getenv("SQUARE_ACCESS_TOKEN")))
        .userAgentDetail("ConcertMaster")
        .build();
  }

  public static void main(String[] args) throws Exception {
    SpringApplication.run(Main.class, args);
  }

  /**
   * Validates an environment variable
   * @param value
   * @return
   */
  protected static String mustLoadEnvironmentVariable(String value) {
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
   * Returns status of the venue To be called repeatedly by frontend.
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
    // Create customer and check for errors
    List<String> createCustomerResult = createCustomer(tokenObject);

    if (createCustomerResult == null) {
      return new SquareResult("FAILURE", null);
    }

    // Payment stuffs
    PaymentsApi paymentsApi = squareClient.getPaymentsApi();

    // Get currency for location
    RetrieveLocationResponse locationResponse = getLocationInformation(squareClient).get();
    String currency = locationResponse.getLocation().getCurrency();

    Seat seat = venue.findSeat(tokenObject.getSeatNum()); // Gets auth if seat number corresponds

    // Set price of ticket
    Money bodyAmountMoney = new Money.Builder()
        .amount(seat.getPrice())
        .currency(currency)
        .build();

    // Payment request that binds to the customer
    CreatePaymentRequest createPaymentRequest = new CreatePaymentRequest.Builder(
        createCustomerResult.get(1),
        UUID.randomUUID().toString())
        .amountMoney(bodyAmountMoney)
        .customerId(createCustomerResult.get(0))
        .locationId(squareLocationId)
        .build();

    SquareResult sqRes = paymentsApi.createPaymentAsync(createPaymentRequest)
        .thenApply(paymentResponse -> {
          return new SquareResult("SUCCESS", null);
        })
        .exceptionally(exception -> {
          ApiException e = (ApiException) exception.getCause();
          System.out.println("Failed to make the create payment request");
          System.out.printf("Exception: %s%n", e.getMessage());
          return new SquareResult("FAILURE", e.getErrors());
        }).join();

    System.out.println("Sending result");
    return sqRes;
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

  /**
   * Gets deviceId from device.code.paired webhook
   * @param deviceJson
   */
  @PostMapping("/device")
  ResponseEntity<String> getDeviceId(@RequestBody String deviceJson) {
    System.out.println("Recieved device webhook");
    // Get the HttpServletRequest object
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

    // Check if attributes is null
    if (attributes == null) {
      System.out.println("ServletRequestAttributes is null");
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    HttpServletRequest request = attributes.getRequest();

    // Get the X-Square-Signature header from the request
    String signatureHeader = request.getHeader("x-square-hmacsha256-signature");

    // Verify Webhook signature
    boolean isValid = WebhooksHelper.isValidWebhookEventSignature(deviceJson, signatureHeader, squareDeviceWebhook, "https://concertmaster.azurewebsites.net/device");

    if (isValid) {  // Signature is valid
      System.out.println("Signature is valid");

      JsonObject result = gson.fromJson(deviceJson, JsonObject.class);

      deviceId = result.getAsJsonObject("data")
          .getAsJsonObject("object")
          .getAsJsonObject("device_code")
          .get("device_id")
          .getAsString();
      
      // Return 200 OK.
      System.out.println("Returing Status 200");
      return new ResponseEntity<>(HttpStatus.OK);
    } else {  // Signature is invalid. Return 403
      System.out.println("Signature is invalid\nReturning Status 403");
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

  }

  /**
   * Creates checkout request for the connected terminal
   * @return
   */
  @GetMapping("/verify")
  @ResponseBody
  SquareResult sendCheckoutRequest() {
    Money amountMoney = new Money.Builder()
      .amount(1L) // Can't charge 0
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
   * Recieves JSON from Square on payment.created webhook and checks card to see if there is a seat associated with customer
   * @param String
   * @return
   * @throws InterruptedException
   * @throws ExecutionException
   */
  @PostMapping("/process-verification")
  ResponseEntity<String> getCardInfo(@RequestBody String paymentJson) throws InterruptedException, ExecutionException{
    System.out.println("Recieved payment.created webhook");

    // Get the HttpServletRequest object
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

    // Check if attributes is null
    if (attributes == null) {
      System.out.println("ServletRequestAttributes is null");
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    HttpServletRequest request = attributes.getRequest();

    // Get the X-Square-Signature header from the request
    String signatureHeader = request.getHeader("x-square-hmacsha256-signature");

    // Verify Webhook signature
    boolean isValid = WebhooksHelper.isValidWebhookEventSignature(paymentJson, signatureHeader, squarePaymentWebhook, "https://concertmaster.azurewebsites.net/process-verification");

    if (isValid) {  // Signature is valid
      System.out.println("Signature is valid");

      // Verify card against cards on file
      verifyCard(paymentJson);
      
      System.out.println("Returning Status 200");
      return new ResponseEntity<>(HttpStatus.OK);
    } else {  // Signature is invalid. Send 403
      System.out.println("Signature is invalid\nReturning Status 403");
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
  }

  /**
   * Helper function for getCardInfo()
   * @param paymentJson
   */
  private void verifyCard(String paymentJson) {
    System.out.println("In verifyCard()");

    JsonObject result;
    try {
      // Parse the JSON string
      result = gson.fromJson(paymentJson, JsonObject.class);
    } catch(JsonSyntaxException e) {
      System.out.println(e.getMessage());
      return;
    }

    result = result.getAsJsonObject("data")
          .getAsJsonObject("object")
          .getAsJsonObject("payment");
          
    String note = result.get("note").getAsString();

    // Access the nested card details
    JsonObject cardDetails = result.getAsJsonObject("card_details");

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
        seated.complete("Seat #" + seatNum + " verified");
        venue.findSeat(seatNum).arrive();
        return;
      } else {
        System.out.println("Seat not found");
        seated.complete("Seat not found!");
        return;
      }
    }
    else {
      System.out.println("Ignoring");
      seated.complete("FAILURE");
      return;
    }
  }

  @GetMapping("/check-card-info")
  @ResponseBody
  SquareResult checkCardInfo() throws InterruptedException, ExecutionException {
    // Get seated
    CompletableFuture<String> checkSeated = seated;

    // Set seated back to original state
    seated = new CompletableFuture<String>();

    if (checkSeated != null) {
      System.out.println("Waiting for card info...");
      return new SquareResult(checkSeated.get(), null); // This will block until the future is completed
    } else {
      return new SquareResult("FAILURE", null);
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
        //System.out.println("Customers: \n" + result.getCustomers());
        if (result.getCustomers() != null) {
          for (Customer customer : result.getCustomers()) {
            if (isCardOnFile(customer, fingerprint)) {
              Ticket ticket = gson.fromJson(customer.getNote(), Ticket.class);
              Seat currSeat = venue.findSeat(ticket.getSeat());

              System.out.println("Found customer");
              
              // Check ticket for validity
              if (ticket.getAuth().equals(currSeat.getAuth())) {
                System.out.printf("Ticket for seat %d Valid!\n", ticket.getSeat());
                seat.set(ticket.getSeat());
              } else {
                System.out.printf("Unable to validate ticket %d\n", ticket.getSeat());
              }
              return;
            }
          }
        } else {
          System.out.println("No customers found");
        }
      })
      .exceptionally(exception -> {
        ApiException e = (ApiException) exception.getCause();
        System.out.println("Failed to make the list customers request");
        System.out.println(String.format("Exception: %s", e.getMessage()));
        return null;
      });
      future.join();
      
    } catch (Exception e) {
      System.out.println(String.format("Exception: %s", e.getMessage()));
    }
  
    return seat.get();
  }

  private boolean isCardOnFile(Customer customer, String fingerprint) {
    CardsApi cardsApi = squareClient.getCardsApi();

    System.out.println("in isCardOnFile()");

    // Retrieve the list of cards associated with the customer
    return (
      cardsApi.listCardsAsync(null,customer.getId(),null,null,null)
        .thenApply(result -> {
          //System.out.println("Success!\n" + result.getCards());
          if (result != null) {
            for (Card card : result.getCards()) {
              if (card.getFingerprint().equals(fingerprint)) {
                System.out.println("Found Card!");
                return true;
              }
            }
          }
      
          return false;
        })
        .exceptionally(exception -> {
          System.out.println("Failed to make the list cards request");
          System.out.println(String.format("Exception: %s", exception.getMessage()));
          return false;
        }).join()
    );
  }

  /**
   * Requests customer creation
   * @param tokenObject
   * @return List of [Customer ID, Card ID]
   */
  protected List<String> createCustomer(TokenWrapper tokenObject) {  
    CustomersApi customersApi = squareClient.getCustomersApi();

    Seat seat = venue.findSeat(tokenObject.getSeatNum()); // Gets auth if seat number corresponds

    CreateCustomerRequest customer = new CreateCustomerRequest.Builder()
        .givenName(tokenObject.getName())
        .emailAddress(tokenObject.getEmail())
        .referenceId(UUID.randomUUID().toString())
        .note("No ticket") // Holds authentication id
        .build();
    
    return (
        customersApi.createCustomerAsync(customer)
        .thenApply(result -> {
          System.out.println("Customer Request Success!");

          String customerId = result.getCustomer().getId();
          String cardId = createCard(tokenObject, customerId);

          if (cardId == null) {
            return null;
          }

          // Venue matches, seat exists, and seat is still for sale
          if (venue.getVenueId().equals(tokenObject.getVenueId()) && seat != null && !seat.isSold()) {
            Ticket ticket = new Ticket(tokenObject.getSeatNum(), seat.getAuth()); // Creates ticket
            String note = gson.toJson(ticket); // Converts ticket to string JSON
            
            // Give the ticket to the customer
            UpdateCustomerRequest body = new UpdateCustomerRequest.Builder()
                .note(note) // Holds the ticket
                .build();

            customersApi.updateCustomerAsync(customerId, body)
                .thenAccept(updateResult -> {
                  System.out.println("Success!");
                })
                .exceptionally(exception -> {
                  System.out.println("Failed to make the request");
                  System.out.println(String.format("Exception: %s", exception.getMessage()));
                  return null;
                });

            seat.sell();
            System.out.println("Sold a ticket");
          }
          return Arrays.asList(customerId, cardId);
        })
        .exceptionally(exception -> {
          System.out.println("Failed to make the create customer request");
          System.out.println(String.format("Exception: %s", exception.getMessage()));
          return null;
        }).join()
    );
  }

  /**
   * Helper function for createCustomer that creates a card for the customer
   * @param tokenObject
   * @param customerId
   * @param paymentId
   */
  private String createCard(TokenWrapper tokenObject, String customerId) { 
    CardsApi cardsApi = squareClient.getCardsApi();

    Card card = new Card.Builder()
      .cardholderName(tokenObject.getName())
      .customerId(customerId)
      .build();
    
    CreateCardRequest body = new CreateCardRequest.Builder(
      UUID.randomUUID().toString(),
      tokenObject.getToken(),
      card)
      .build();
    
    return (
      cardsApi.createCardAsync(body)
      .thenApply(result -> {
        System.out.println("Card Request Success!");
        return result.getCard().getId();
      })
      .exceptionally(exception -> {
        System.out.println("Failed to make the create card request");
        System.out.println(String.format("Exception: %s", exception.getMessage()));
        return null;
      }).join()
    );
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
          System.out.println("Failed to make the retrieve locations request");
          System.out.printf("Exception: %s%n", exception.getMessage());
          return null;
        });
  }
}