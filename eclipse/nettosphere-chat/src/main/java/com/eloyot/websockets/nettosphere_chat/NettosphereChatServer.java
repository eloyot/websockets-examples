package com.eloyot.websockets.nettosphere_chat;

import org.atmosphere.nettosphere.Config;
import org.atmosphere.nettosphere.Nettosphere;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Start the Nettosphere server and load the Atmosphere chat handler
 */
public class NettosphereChatServer {
    private static final Logger logger = LoggerFactory.getLogger(Nettosphere.class);

    public static void main(String[] args) throws IOException {
        Nettosphere server = new Nettosphere.Builder().config(
                new Config.Builder()
                   .host("127.0.0.1")
                   .port(8080)
                   .resource("/chat", AtmosphereChatHandler.class)
                   .build())
                .build();
        server.start();
        logger.info("Nettosphere server started on port 8080!");
    }
}
