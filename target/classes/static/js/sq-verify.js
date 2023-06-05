async function SquareVerifyFlow() {
    // Create card payment object and attach to page
    CardVerify(document.getElementById('card-container'), document.getElementById('card-button'));
}

window.payments = Square.payments(window.applicationId, window.locationId);

window.verifyFlowMessageEl = document.getElementById('verify-flow-message');
window.deviceCodeEl = document.getElementById('device-code');
var loaderContainer = document.getElementById('loader-container');

window.showSuccess = function(message) {
    window.loaderContainer.removeChild(loaderContainer.firstChild);
    window.verifyFlowMessageEl.classList.add('success');
    window.verifyFlowMessageEl.classList.remove('error');
    window.verifyFlowMessageEl.innerText = message;
}

window.showError = function(message) {
    window.loaderContainer.removeChild(loaderContainer.firstChild);
    window.verifyFlowMessageEl.classList.add('error');
    window.verifyFlowMessageEl.classList.remove('success');
    window.verifyFlowMessageEl.innerText = message;
}

window.showLoader = function() {
    var loaderEl = document.createElement('div');
    loaderEl.classList.add('loader');
    loaderContainer.appendChild(loaderEl)

    window.verifyFlowMessageEl.classList.remove('success');
    window.verifyFlowMessageEl.classList.remove('error');
}

window.showDeviceCode = function(code) {
    window.deviceCodeEl.classList.add('Device-Code');
    window.deviceCodeEl.innerText = 'Device Code: ' + code;
}

window.deviceId = "";

async function CardVerify(buttonEl) {
  
    async function eventHandler(event) {
        // Clear any existing messages
        window.verifyFlowMessageEl.innerText = '';
        window.showLoader();
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

async function createVerification() {   // Get deviceId from somewhere
    console.log("Verifying");
    window.showLoader();
    
    try {
        const response = await fetch('/verify', {
            method: 'GET'
        });

        const data = await response.json();
        if (data.errors && data.errors.length > 0) {
            if (data.errors[0].detail) {
                window.showError(data.errors[0].detail);
            } else {
                window.showError('Checkout Creation Failed.');
                console.log('Checkout failure');
            }
        } else {
            window.showSuccess('Checkout Creation Successful!');
            console.log('Checkout success');
        }
    } catch (error) {
        console.error('Error: ', error);
    }

    // Check for verifcation status
    try {
        const response = await fetch('/check-card-info', {
            method: 'GET'
        });
        const data = await response.json();

        console.log(data);
        if (data.title === 'FAILURE' || data.status === 404) {
            window.showError('Verification Failed')
        } else {
            window.showSuccess(data.title);

            // Reload window after successful verification
            if (data.title !== "Seat not found")  window.location.reload();
        }
    } catch (error) {
        console.error('Error: ', error);
    }
}

async function connectToTerminal() {
    console.log("Connecting");
    try {
        const response = await fetch('/connect', {
            method: 'GET',
        });

        const data = await response.json();
        if (data.errors && data.errors.length > 0) {
            if (data.errors[0].detail) {
                window.showError(data.errors[0].detail);
            } else {
                window.showError('Failure fetching device code');
            }
        } else {
            window.showSuccess('Device code Aquired');
        }

        // Get the device code
        let deviceCode = data.title;
        console.log(deviceCode);
        window.showDeviceCode(deviceCode);
        alert("Please Input this device code into your Square terminal: " + deviceCode);

        // TODO: Display code to user
    } catch (error) {
        console.error('Error: ', error);
    }
}
  