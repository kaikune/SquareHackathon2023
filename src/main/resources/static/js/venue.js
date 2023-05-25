var chosenSeat = null;
var venueId;

/**
 * Receives all venue information from the server
 */
(async function() {
    try {
        const response = await fetch('/venue', {
            method: 'GET'
        });

        if (!response.ok) {
            throw new Error('Failed to fetch venue info');
        }

        const venue = await response.json();
        venueId = venue.venueId;
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
    var venue = document.getElementById("venue");

    //console.log(seats);

    for (var seatNum in seats) {
        var seat = seats[seatNum];
        var seatEl = document.createElement("div");

        seatEl.classList.add("seat");
        seatEl.textContent = seatNum;

        // Check if the seat is sold
        if (seat.sold) {
            seatEl.classList.add("sold");
            seatEl.setAttribute("title", "Seat is sold");
            seatEl.addEventListener("click", function() {
                alert("This seat is already sold.");
                return; // Prevent selecting the seat after the alert
            });
        } else {
            seatEl.addEventListener("click", function() {
                // Deselect previously selected seats
                var selectedSeats = document.getElementsByClassName("selected");
                for (var j = 0; j < selectedSeats.length; j++) {
                    selectedSeats[j].classList.remove("selected");
                }
                
                // Select the clicked seat
                this.classList.add("selected");
                chosenSeat = parseInt(this.textContent);
            });
        }

        venue.appendChild(seatEl);
    }

    // Get all seat elements
    var seatEls = document.getElementsByClassName("seat");

    // Add click event listener to each seat
    for (var i = 0; i < seatEls.length; i++) {
        seatEls[i].addEventListener("click", function() {
            // Deselect previously selected seats
            var selectedSeats = document.getElementsByClassName("selected");
            for (var j = 0; j < selectedSeats.length; j++) {
                selectedSeats[j].classList.remove("selected");
            }
            
            // Select the clicked seat
            this.classList.add("selected");
            chosenSeat = parseInt(this.textContent);
        });
    }
}
