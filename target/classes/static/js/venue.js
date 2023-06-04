var chosenSeat = null;
var venueId;
var seatsContainer = document.getElementById("seats-container");

/**
 * Receives all venue information from the server
 */
(async function () {
    try {
        const response = await fetch('/venue', {
            method: 'GET'
        });

        if (!response.ok) {
            throw new Error('Failed to fetch venue info');
        }

        const venue = await response.json();
        //console.log(venue);
        venueId = venue.venueId;
        setEventName(venue.venueName, venue.eventName);
        generateSeats(venue.seats);
    } catch (error) {
        console.error('Error:', error);
    }
})();

/**
 * Generates a div that holds all of the seats in the venue. Each seat is clickable
 * @param {*} seats
 */
function generateSeats(seats) {
    // Generate seat elements dynamically
    var seatsContainer = document.getElementById("seats-container");

    for (var seatNum in seats) {
        var seat = seats[seatNum];
        var seatEl = document.createElement("div");

        seatEl.classList.add("seat");
        seatEl.textContent = seatNum;

        // Check if the seat is filled or sold
        if (seat.arrived) {
            seatEl.classList.add("arrived");
            seatEl.setAttribute("title", "Seat is filled");
        } else if (seat.sold) {
            seatEl.classList.add("sold");
            seatEl.setAttribute("title", "Seat is sold");
        }

        // Add price as a data attribute
        seatEl.setAttribute("data-price", "$" + seat.price);

        // Add click event listener to select the seat
        seatEl.addEventListener("click", function () {
            // Check if the seat is filled or sold before selecting
            if (this.classList.contains("arrived")) {
                alert("Sorry! This seat is already filled");
            } else if (this.classList.contains("sold")) {
                alert("Sorry! This seat has been sold");
            } else {
                // Deselect previously selected seats
                var selectedSeats = document.getElementsByClassName("selected");
                for (var j = 0; j < selectedSeats.length; j++) {
                    selectedSeats[j].classList.remove("selected");
                }

                // Select the clicked seat
                this.classList.add("selected");
                chosenSeat = parseInt(this.textContent);
            }
        });

        seatsContainer.appendChild(seatEl);
    }
}

/**
 * Displays the event name and venue on the page
 * @param {*} venueName 
 * @param {*} eventName 
 */
function setEventName(venueName, eventName) {
    var eventNameContainer = document.getElementById("event-name");
    eventNameContainer.textContent = eventName + " at " + venueName;
}

