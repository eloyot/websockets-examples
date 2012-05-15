package com.eloyot.websockets.nettosphere_chat;

import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.nettosphere.Nettosphere;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

/**
 * AtmosphereHandler providing a simple chat server implementation. 
 */
public class AtmosphereChatHandler implements AtmosphereHandler {
    private static final Logger logger = LoggerFactory.getLogger(Nettosphere.class);
    private Map<Integer, String> usernameMap = new Hashtable<Integer, String>();

	/**
	 * Process HTTP GET requests as connection requests. Process HTTP POST requests as
	 * broadcast requests. When a resource is suspended that means that it is connected.
	 * 
	 * @param resource Provides context and request
	 */
    @Override
    public void onRequest(AtmosphereResource resource) throws IOException {
        AtmosphereRequest request = resource.getRequest();

        if (request.getMethod().equalsIgnoreCase("GET")) {
            // Process connection request by suspending
        	resource.suspend();
        } else if (request.getMethod().equalsIgnoreCase("POST")) {
            // Broadcast incoming message to all connections
        	resource.getBroadcaster().broadcast(new Message(request.getReader().readLine().trim(), request.getRemotePort()));
        }
    }

    /**
     * This handler will be invoked on broadcast, once for each connection. The state of 
     * each connection will be suspended for the duration of the connection.
     * 
     * @param event Event, this will be suspended for the duration of the connection
     */
    @Override
    public void onStateChange(AtmosphereResourceEvent event) throws IOException {
        AtmosphereResource resource = event.getResource();
        AtmosphereResponse response = resource.getResponse();
    	int remotePort = resource.getRequest().getRemotePort();

        if (event.isSuspended()) {
            Message message = new Message((Message)event.getMessage());

            // Dispatch message according to logical message type
        	if ("register".equalsIgnoreCase(message.getType())) {
            	// Handle a registration message
        		String username = message.getBody();
        		if (remotePort == message.getSender()) {
        			// My connection is registering
            		logger.info("Register Event: " + username);
            		usernameMap.put(remotePort, username);
            		message.setBody("[Server] Welcome " + username);
        		} else {
        			// Another connection is registering
        			message.setBody("[Server] " + username + " Connected");
        		}
        	} else if ("message".equalsIgnoreCase(message.getType())) {
        		if (remotePort == message.getSender()) {
        			// I'm the sender
        			String username = getUsername(resource.getRequest().getRemotePort());
            		logger.info("Message: " + username + ": " + message.getBody());
            		message.setBody("[Me] " + message.getBody());
        		} else if (usernameMap.containsKey(message.getSender())){
            		message.setBody("[" + getUsername(message.getSender()) + "] " + message.getBody());        			
        		} else {
            		message.setBody("[Server] " + message.getBody());        			
        		}
        	} else {
        		// Unknown logical message type
        		logger.info("Unknown Message Type: " + message.getBody());
        	}    	

            response.getWriter().write(message.getBody());
            switch (resource.transport()) {
                case JSONP:
                case LONG_POLLING:
                    resource.resume();
                    break;
                case WEBSOCKET :
                case STREAMING:
                    response.getWriter().flush();
                    break;
            }
        } else if (event.isCancelled()){
        	String username = getUsername(remotePort);
            usernameMap.remove(remotePort);
            event.broadcaster().removeAtmosphereResource(resource);
            event.broadcaster().broadcast(new Message("message:" + username + " Disconnected", remotePort));
        } else if (event.isResumedOnTimeout()){
        	event.broadcaster().broadcast("Resumed on timeout");
        } else if (event.isResuming()){
        	// No need to do anything
        } else {
        	// Error, event state
        }
    }
    
    /**
     * Lookup the username in the username map. Return Unregistered User if username not found.
     * 
     * @param resource
     * @return Username or Unregistered User of username not found
     */
    private String getUsername(int sender) {
    	String username = usernameMap.get(sender);
    	if (username == null) {
    		// User not registered
    		username = "Unregistered User";
    	}
    	
    	return username;
    }
    
    @Override
    public void destroy() {
    	// Nothing to do on destroy
    }
    
    private class Message {
    	private String type;
    	private String body;
    	private int sender;
    	
    	public Message(String rawMessage, int sender) {
    		this.sender = sender;  
        	if (rawMessage.startsWith("register:")) {
        		this.type = "Register";
        		this.body = rawMessage.substring(rawMessage.indexOf(':') + 1).trim();
        	} else if (rawMessage.startsWith("message:")) {
        		this.type = "message";
        		body = rawMessage.substring(rawMessage.indexOf(':') + 1).trim();
    		} else {
    			this.type = "unknown";
    			body = rawMessage;
    		}
    	}
    	
    	public Message(Message message) {
    		this.sender = message.getSender();
    		this.type = message.getType();
    		this.body = message.getBody();
    	}

		public String getType() {
			return type;
		}

		public String getBody() {
			return body;
		}

		public void setBody(String body) {
			this.body = body;
		}

		public int getSender() {
			return sender;
		}
    }
}
