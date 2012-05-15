// jWebSocket Chat
var socket;
var username;
var jWebSocketClient;

//Connect to the server and set up the handlers
function connect() {
	var url = 'ws://localhost:8787/jWebSocket/jWebSocket';
	var password = '';
	
	// Connect to the server
	jWebSocketClient.logon(url, username, password, {

		// Connect Handler
		OnOpen: function( event ) {
			$('#consoleDiv').append('<br/>[Me] Connected to jWebSocketServer using web socket protocol');
			jWebSocketClient.startKeepAlive({interval: 5000});
		},

		// Message Handler
		OnMessage: function(event, token) {
			if (token) {
				if (token.type == "response") {
					// This is a response from a request this client made to the server
					if (token.reqType == "login") {
						if (token.code == 0) {
							$('#consoleDiv').append('<br/>[Server] Welcome ' + token.username);
						} else {
							$('#consoleDiv').append('<br/>[Server] Error logging in ' + username + "': " + token.msg);
						}
					}
				} else if (token.type == "goodBye") {
					// Server sending response to socket close
					$('#consoleDiv').append('<br/>[Server] Goodbye ' + username);
				} else if (token.type == "broadcast") {
					// Receiving chat message
					if (token.data) {
						$('#consoleDiv').append('<br/>[' + token.sender + '] ' + token.data);
					}
				}
			}
		},

		// Close Handler
		OnClose: function( event ) {
			jWebSocketClient.stopKeepAlive();
			$('#consoleDiv').append('<br/>[Me] Disconnected');
		}		
	});
}

//Disconnect from the server
function disconnect() {
	jWebSocketClient.stopKeepAlive();
	jWebSocketClient.close({timeout: 3000});
}

//Send the message
function send() {
	var message = $('#messageInput').val();
	if (message.length > 0) {
		var result = jWebSocketClient.broadcastText("",	message);
		if (result.code == 0 ) {
			$('#consoleDiv').append('<br/>[Me] ' + message);
		} else {
			$('#consoleDiv').append('<br/>Error ' + result.msg);	
		}
	}
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
	setUsername();

	// Check for Web Socket support
	if( jws.browserSupportsWebSockets() ) {
		jWebSocketClient = new jws.jWebSocketJSONClient();
	} else {
		$('#consoleDiv').append('<br/>' + jws.MSG_WS_NOT_SUPPORTED);
	}
});
