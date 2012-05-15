// Basic web socket chat
var socket;
var username;

// Connect to the server and set up the handlers
function connect() {
	// Connect to the server
	if (window.WebSocket) {
		socket = new WebSocket("ws://localhost:8080/websocket");

		// Connect handler
		socket.onopen = function(event) {
			// Register username with server
			socket.send('register:' + username);
		};

		socket.onmessage = function(event) {
			// Display the message
			$('#consoleDiv').append('<br/> ' + event.data);
		};
		
		socket.onclose = function(event) {
			$('#consoleDiv').append('<br/>[Me] Disconnected');
		};
	} else {
		alert("Your browser does not support Web Sockets!");
	}
}

// Send the message
function send() {
	if (socket.readyState == WebSocket.OPEN) {
		socket.send('message: ' + $('#messageInput').val());
	} else {
		alert("The socket is not open.");
	}
}

// Disconnect from the server
function disconnect() {
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
