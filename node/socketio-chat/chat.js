// Simple chat implementation
var app = require('express').createServer()
	, io = require('socket.io').listen(app);

// Start the express web server
app.listen(80);

// Setup the express web server routes
app.get('/', function(req, res) {
	res.sendfile(__dirname + '/public/index.html');
});
app.get('/*', function(req, res) {
	res.sendfile(__dirname + '/public/' + req.params);
});

// Set the log level to info
io.set('log level', 1);

// Set socket listeners
io.sockets.on('connection', function(socket) {
	// Message handler
	socket.on('message', function(message) {
		socket.get('username', function(error, username) {
			console.log('Message:' + username + ': ' + message);
			socket.broadcast.send('[' + username + '] ' + message);
			socket.send('[Me] ' + message);
		});
	});
	
	// Registration handler
	socket.on('register', function(username) {
		socket.set('username', username, function() {
			console.log('Register Event: ' + username);
			
			// Send message to caller
			socket.send('[Server] Welcome ' + username);
			
			// Send message to all except caller
			socket.broadcast.send('[Server] ' + username + ' Connected');
		});
	});

	// Disconnect handler
	socket.on('disconnect', function() {
		socket.get('username', function(error, username) {
			console.log('Disconnect:' + username + ':');
			
			// Send to all connected users
			io.sockets.send('[' + username + '] Disconnected');
		});
	});
});
