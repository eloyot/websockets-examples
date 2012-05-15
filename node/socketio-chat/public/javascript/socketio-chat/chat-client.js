// Socket.IO web socket chat
var socket;
var username;

// Connect to the server and set up the handlers
function connect() {
	// Connect to the server
	socket = io.connect('localhost', {
		'force new connection' : true
	});
	
	// Connect handler, register once successfully connected
	socket.on('connect', function() {
		// Emit sends an event
		socket.emit('register', username);
	});
	
	// Disconnect handler
	socket.on('disconnect', function() {
		$('#consoleDiv').append('<br/>[Me] Disconnected');
	});
	
	// Message handler
	socket.on('message', function(data) {
		$('#consoleDiv').append('<br/>' + data);
	});
}

// Send the message
function send() {
	// Send sends a message
	socket.send($('#messageInput').val());
}

// Disconnect from the server
function disconnect() {
	socket.disconnect();
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
