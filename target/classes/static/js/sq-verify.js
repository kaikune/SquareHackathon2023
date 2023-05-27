/**
 * TODO: Change to create terminal checkout and then refresh page on successful checkout (webhook)
 */

async function SquareVerifyFlow() {
    // Create card payment object and attach to page
    CardVerify(document.getElementById('card-container'), document.getElementById('card-button'));
}

window.payments = Square.payments(window.applicationId, window.locationId);

window.paymentFlowMessageEl = document.getElementById('payment-flow-message');

window.showSuccess = function(message) {
    window.paymentFlowMessageEl.classList.add('success');
    window.paymentFlowMessageEl.classList.remove('error');
    window.paymentFlowMessageEl.innerText = message;
}

window.showError = function(message) {
    window.paymentFlowMessageEl.classList.add('error');
    window.paymentFlowMessageEl.classList.remove('success');
    window.paymentFlowMessageEl.innerText = message;
}
  
async function CardVerify(fieldEl, buttonEl) {
    // Create a card payment object and attach to page
    const card = await window.payments.card({
      style: {
        '.input-container.is-focus': {
          borderColor: '#006AFF'
        },
        '.message-text.is-error': {
          color: '#BF0020'
        }
      }
    });
    await card.attach(fieldEl);
  
    async function eventHandler(event) {
      // Clear any existing messages
      window.paymentFlowMessageEl.innerText = '';
  
      try {
        const result = await card.tokenize();
        if (result.status === 'OK') {
          // Use global method from sq-payment-flow.js
          window.createVerification(result.token, chosenSeat);
        }
      } catch (e) {
        if (e.message) {
            window.showError(`Error: ${e.message}`);
        } else {
            window.showError('Something went wrong');
        }
      }
    }
  
    buttonEl.addEventListener('click', eventHandler);
  }  

window.createVerification = async function(token, seatNum) {
    const dataJsonString = JSON.stringify({
      token,
      name: null,
      email: null,
      venueId: venueId,
      seatNum: seatNum,
      idempotencyKey: create_UUID(),
    });
  
    try {
    const response = await fetch('/process-verification', {
        method: 'POST',
        headers: {
        'Content-Type': 'application/json'
        },
        body: dataJsonString
    });

    const data = await response.json();
    if (data.errors && data.errors.length > 0) {
        if (data.errors[0].detail) {
            window.showError(data.errors[0].detail);
        } else {
            window.showError('Verification Failed.');
        }
    } else {
        window.showSuccess('Verification Successful!');
    }
    } catch (error) {
        console.error('Error:', error);
    }
}
  