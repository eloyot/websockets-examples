package com.eloyot.websockets.netty_chat;

import static org.jboss.netty.handler.codec.http.HttpHeaders.*;
import static org.jboss.netty.handler.codec.http.HttpMethod.*;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.*;
import static org.jboss.netty.handler.codec.http.HttpVersion.*;

import java.util.HashMap;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.util.CharsetUtil;

/**
 * Handle web socket connection requests (handshakes) and web socket messages.
 * Message are broadcast to all connected users. A connection should register its 
 * username before sending messages.
 */
public class WebSocketServerHandler extends SimpleChannelUpstreamHandler {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(WebSocketServerHandler.class);
    private static final String WEBSOCKET_PATH = "/websocket";
    private static final ChannelGroup channels = new DefaultChannelGroup();    

    private WebSocketServerHandshaker handshaker;
    private Map<Integer, String> userNameMap = new HashMap<Integer, String>();

    /**
     * Process an incoming message. This could be a connection request or a web socket message.
     * Handshakes come in as HTTP upgrade requests. Handle these as well as any stray HTTP requests.
     * Handle web socket requests as well.
     * 
     * @param context   Message context
     * @param event		Contains message
     */
    @Override
    public void messageReceived(ChannelHandlerContext context, MessageEvent event) throws Exception {
        Object message = event.getMessage();
        if (message instanceof HttpRequest) {
            handleHttpRequest(context, (HttpRequest) message);
        } else if (message instanceof WebSocketFrame) {
            handleWebSocketFrame(context, (WebSocketFrame) message);
        }
    }

    /**
     * Handle HTTP requests, these are handshake requests to connect to a web socket as well as 
     * any stray HTTP requests.
     * 
     * @param context 	Message context
     * @param request	HTTP request
     * @throws Exception
     */
    private void handleHttpRequest(ChannelHandlerContext context, HttpRequest request) throws Exception {
        // Only process HTTP GET requests, ignore everything else
        if (request.getMethod() != GET) {
            sendHttpResponse(context, request, new DefaultHttpResponse(HTTP_1_1, FORBIDDEN));
            return;
        }

        if (request.getUri().equals("/websocket")) {
            // This is a web socket handshake request
            WebSocketServerHandshakerFactory handshakerFactory = new WebSocketServerHandshakerFactory(this.getWebSocketLocation(request), null, false);
            this.handshaker = handshakerFactory.newHandshaker(request);
            if (this.handshaker == null) {
            	// The communication protocol is not supported
                handshakerFactory.sendUnsupportedWebSocketVersionResponse(context.getChannel());
            } else {
            	// Keep track of all connections
            	channels.add(context.getChannel());
            	
            	// Respond to the handshake
                this.handshaker.handshake(context.getChannel(), request).addListener(WebSocketServerHandshaker.HANDSHAKE_LISTENER);
            }
        } else {
        	// Ignore anything except web socket handshake requests
            sendHttpResponse(context, request, new DefaultHttpResponse(HTTP_1_1, FORBIDDEN));
        }
    }

    /**
     * Handle pings, close connection request, and regular text message requests.
     * 
     * @param context Message context
     * @param frame	  Web socket message frame
     */
    private void handleWebSocketFrame(ChannelHandlerContext context, WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame) {
        	// Close the connection
        	String username = getUsername(context.getChannel().getId());
        	logger.info("Disconnected: " + username);
        	this.handshaker.close(context.getChannel(), (CloseWebSocketFrame) frame);
            broadcast(context, "[Server] " + username + " Disconnected");
        } else if (frame instanceof PingWebSocketFrame) {
        	// Pings are primarily used for keep alive
            context.getChannel().write(new PongWebSocketFrame(frame.getBinaryData()));
        } else if (frame instanceof TextWebSocketFrame) {
        	// Process text based message
            handleWebSocketMessage(context, ((TextWebSocketFrame) frame).getText());
        } else {
            throw new UnsupportedOperationException(String.format("%s frame type not supported!", frame.getClass().getName()));        	
        }
    }
    
    /**
     * Dispatch the message according to its logcial type. The logical type can be register to 
     * register the username, or message to send a message.
     * 
     * @param context Message context
     * @param message Message text
     */
    private void handleWebSocketMessage(ChannelHandlerContext context, String message) {
    	// Dispatch message according to logical message type
    	if (message.startsWith("register:")) {
        	// Handle a registration message
    		String username = message.substring(message.indexOf(':') + 1).trim();
    		userNameMap.put(context.getChannel().getId(), username);
    		
    		logger.info("Register Event: " + username);
    		
    		context.getChannel().write(new TextWebSocketFrame("[Server] Welcome " + username));
    		broadcast(context, "[Server] " + username + " Connected");
    	} else if (message.startsWith("message:")) {
            // Send the message to all channels, the sending channel gets an echo
    		String username = getUsername(context.getChannel().getId());
    		String userMessage = message.substring(message.indexOf(':') + 1).trim();
    		
    		logger.info("Message: " + username + ": " + userMessage);
    		
    		broadcast(context, "[" + username + "] " + userMessage);
            context.getChannel().write(new TextWebSocketFrame("[Me] " + userMessage));
    	} else {
    		// Unknown logical message type
    		logger.info("Unknown Message Type: " + message);
            context.getChannel().write(new TextWebSocketFrame("[Server] Unknown message type!"));
    	}    	
    }
    
    /**
     * Broadcast the specified message to all registered channels except the sender.
     * 
     * @param context Message context
     * @param message Message to broadcast
     */
    private void broadcast(ChannelHandlerContext context, String message) {
        for (Channel channel: channels) {
            if (channel != context.getChannel()) {
                channel.write(new TextWebSocketFrame(message));
            }
        }
    }
    
    /**
     * Lookup the username in the username map. Return Unregistered User if username not found.
     * 
     * @param channelId
     * @return Username or Unregistered User of username not found
     */
    private String getUsername(Integer channelId) {
    	String username = userNameMap.get(channelId);
    	if (username == null) {
    		// User not registered
    		username = "Unregistered User";
    	}
    	
    	return username;
    }

    /**
     * Send HTTP response. In our case this is an error response so send it and close the communication channel.
     * 
     * @param context  Message context
     * @param request  HTTP request
     * @param response HTTP response
     */
    private void sendHttpResponse(ChannelHandlerContext context, HttpRequest request, HttpResponse response) {
        // Generate an error response if the response status code is not OK (200)
        if (response.getStatus().getCode() != 200) {
            response.setContent(ChannelBuffers.copiedBuffer(response.getStatus().toString(), CharsetUtil.UTF_8));
            setContentLength(response, response.getContent().readableBytes());
        }

        /*
         *  Send the response (channel.write) and close the connection if this is an error response.
         *  Use a future because write is an asynchronous call.
         */
        ChannelFuture future = context.getChannel().write(response);
        if (!isKeepAlive(request) || response.getStatus().getCode() != 200) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * There was a fatal exception, log it and close the connection.
     * 
     * @param context
     * @param exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext context, ExceptionEvent exception) throws Exception {
        exception.getCause().printStackTrace();
        userNameMap.remove(exception.getChannel().getId());
        exception.getChannel().close();
    }

    /**
     * Return the location string for this web socket.
     * 
     * @param request
     * @return Location string for this web socket
     */
    private String getWebSocketLocation(HttpRequest request) {
        return "ws://" + request.getHeader(HttpHeaders.Names.HOST) + WEBSOCKET_PATH;
    }
}
