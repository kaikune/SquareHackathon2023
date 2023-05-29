/**
 * TODO: Change to create terminal checkout and then refresh page on successful checkout (webhook)
 */

async function SquareVerifyFlow() {
    // Create card payment object and attach to page
    CardVerify(document.getElementById('card-container'), document.getElementById('card-button'));
}

window.payments = Square.payments(window.applicationId, window.locationId);

window.verifyFlowMessageEl = document.getElementById('verify-flow-message');

window.showSuccess = function(message) {
    window.paFlowMessageEl.classList.add('success');
    window.verifyFlowMessageEl.classList.remove('error');
    window.verifyFlowMessageEl.innerText = message;
}

window.showError = function(message) {
    window.verifyFlowMessageEl.classList.add('error');
    window.verifyFlowMessageEl.classList.remove('success');
    window.verifyFlowMessageEl.innerText = message;
}
  
async function CardVerify(buttonEl) {
  
    async function eventHandler(event) {
      // Clear any existing messages
      window.verifyFlowMessageEl.innerText = '';
  
      try {
        window.createVerification(result.token, chosenSeat);
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

window.createVerification = async function(deviceId) {
    try {
    const response = await fetch('/verify', {
        method: 'POST',
        headers: {
        'Content-Type': 'application/json'
        },
        body: deviceId
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
  