package com.eloyot.websockets.netty_chat;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 * An HTTP server which serves web socket requests at http://localhost:8080/websocket
 */
public class WebSocketServer {
    private final int port;
    
    public WebSocketServer(int port) {
        this.port = port;
    }
    
    public void run() {
        // Bootstrap Netty
        ServerBootstrap bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));

        // Set up the event pipeline
        bootstrap.setPipelineFactory(new WebSocketServerPipelineFactory());

        // Start processing messages
        bootstrap.bind(new InetSocketAddress(port));

        System.out.println("Netty web socket server started at port " + port + '!');
    }

    public static void main(String[] args) {
        new WebSocketServer(8080).run();
    }
}
