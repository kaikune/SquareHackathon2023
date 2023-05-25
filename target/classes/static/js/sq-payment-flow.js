
async function SquarePaymentFlow() {
  // Create card payment object and attach to page
  CardPay(document.getElementById('card-container'), document.getElementById('card-button'));
}

function create_UUID(){
  var dt = new Date().getTime();
  var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      var r = (dt + Math.random()*16)%16 | 0;
      dt = Math.floor(dt/16);
      return (c=='x' ? r :(r&0x3|0x8)).toString(16);
  });
  return uuid;
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
    idempotencyKey: create_UUID(),
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
      if (data.errors && data.errors.length > 0) {
        if (data.errors[0].detail) {
          window.showError(data.errors[0].detail);
        } else {
          window.showError('Payment Failed.');
        }
      } else {
        window.showSuccess('Payment Successful!');
      }
    } catch (error) {
      console.error('Error:', error);
    }
  }
  else {
    window.showError("Unable to process payment without permission.")
  }
}

SquarePaymentFlow();
