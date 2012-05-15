// Atmosphere chat
var socket;
var username;
var request;

// Connect to the server and set up the handlers
function connect() {
	// Set up the atmosphere request
	request = {
		url : 'http://localhost:8080/' + 'chat',
		contentType : "application/json",
		logLevel : 'debug',
		transport : 'websocket',
		fallbackTransport : 'long-polling'
	};

	// Connection handler
	request.onOpen = function(response) {
		$('#consoleDiv').append('<br/>Atmosphere connected using ' + response.transport);
		socket.push('register:' + username);
	};

	request.onClose = function(response) {
		$('#consoleDiv').append('<br/>[Me] Disconnected');
	};

	request.onReconnect = function(request, response) {
		$.atmosphere.info('Reconnecting')
	};

	// Message handler
	request.onMessage = function(response) {
		$('#consoleDiv').append('<br/>' + response.responseBody);
	};

	// Error handler
	request.onError = function(response) {
		$('#consoleDiv').append('<br/>Error communicating with server!');
	};

	// Subscribe to the communication channel
	socket = $.atmosphere.subscribe(request);
}

// Send the message
function send() {
	socket.push('message:' + $('#messageInput').val());
}

// Disconnect from the server
function disconnect() {
	$.atmosphere.unsubscribe(request);
	socket.close();
}

// Set the username based on the browser
function setUsername() {
	if ($.browser.mozilla) {
		username = "Firefox";
	} else if ($.browser.webkit) {
		username = "Chrome";
	} else {
		username = "Somebody";
	}
}

// Clear the message area
function clearMessages() {
	$('#consoleDiv').html('');
}

$(document).ready(function() {
	// Set the name based on the browser
	setUsername();
});
