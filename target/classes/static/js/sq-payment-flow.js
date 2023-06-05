async function SquarePaymentFlow() {
  // Create card payment object and attach to page
  CardPay(document.getElementById('card-container'), document.getElementById('card-button'));
}

window.payments = Square.payments(window.applicationId, window.locationId);

window.paymentFlowMessageEl = document.getElementById('payment-flow-message');
var loaderContainer = document.getElementById('loader-container');

window.showSuccess = function(message) {
  if (window.showLoader.firstChild) {
    window.loaderContainer.removeChild(loaderContainer.firstChild);
  }
  window.paymentFlowMessageEl.classList.add('success');
  window.paymentFlowMessageEl.classList.remove('error');
  window.paymentFlowMessageEl.innerText = message;
}

window.showError = function(message) {
  if (window.showLoader.firstChild) {
    window.loaderContainer.removeChild(loaderContainer.firstChild);
  }
  window.paymentFlowMessageEl.classList.add('error');
  window.paymentFlowMessageEl.classList.remove('success');
  window.paymentFlowMessageEl.innerText = message;
}

window.showLoader = function() {
  if (window.loaderContainer.firstChild) {
    return;
  }
  var loaderEl = document.createElement('div');
  loaderEl.classList.add('loader');
  loaderContainer.appendChild(loaderEl)

  window.paymentFlowMessageEl.classList.remove('success');
  window.paymentFlowMessageEl.classList.remove('error');
}

window.createPayment = async function(token, seatNum) {
  const name = document.getElementById('customer-name').value;
  const email = document.getElementById('customer-email').value;

  if (!name || !email) {
    window.showError('Please fill in the required fields.');
    return;
  }
  
  if (!seatNum) {
    window.showError("Please pick a seat!");
    return;
  }

  const dataJsonString = JSON.stringify({
    token,
    name: name,
    email: email,
    venueId: venueId,
    seatNum: seatNum,
  });

  if (document.getElementById('permissions').checked) {
    try {
      const response = await fetch('process-payment', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: dataJsonString
      }); 
  
      const data = await response.json();
      if (data.title === "FAILURE") {
        if (data.errors) {
          window.showError(data.errors[0].detail);
        } else {
          window.showError('Transaction Failed.');
        }
      } else {
        window.showSuccess('Payment Successful!');
        window.location.reload();
      }
    } catch (error) {
      console.error('Error:', error);
    }
  }
  else {
    window.showError("Unable to process payment without permission.")
  }
}

async function CardPay(fieldEl, buttonEl) {
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
    window.showLoader()
    try {
      const result = await card.tokenize();
      if (result.status === 'OK') {
        // Use global method from sq-payment-flow.js
        window.createPayment(result.token, chosenSeat);
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

SquarePaymentFlow();
