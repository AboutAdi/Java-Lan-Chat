package com.voibiz.lanchat.core.network;

import com.voibiz.lanchat.core.model.User;
import com.voibiz.lanchat.core.protocol.MessageType;
import com.voibiz.lanchat.core.protocol.ProtocolMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkSimulationSandboxTest {

    private TCPConnectionManager peer1Manager;
    private TCPConnectionManager peer2Manager;
    private TCPConnectionManager peer3Manager;

    private TestMessageHandler peer1Handler;
    private TestMessageHandler peer2Handler;
    private TestMessageHandler peer3Handler;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setup() throws IOException {
        user1 = new User(UUID.randomUUID().toString(), "Peer1", "127.0.0.1", 50001);
        user2 = new User(UUID.randomUUID().toString(), "Peer2", "127.0.0.1", 50002);
        user3 = new User(UUID.randomUUID().toString(), "Peer3", "127.0.0.1", 50003);

        peer1Handler = new TestMessageHandler();
        peer2Handler = new TestMessageHandler();
        peer3Handler = new TestMessageHandler();

        peer1Manager = new TCPConnectionManager(user1, peer1Handler);
        peer2Manager = new TCPConnectionManager(user2, peer2Handler);
        peer3Manager = new TCPConnectionManager(user3, peer3Handler);

        peer1Manager.start();
        peer2Manager.start();
        peer3Manager.start();
    }

    @AfterEach
    void tearDown() {
        if (peer1Manager != null) peer1Manager.stop();
        if (peer2Manager != null) peer2Manager.stop();
        if (peer3Manager != null) peer3Manager.stop();
    }

    @Test
    void testDirectMessageDelivery() throws Exception {
        // Peer 1 connects to Peer 2
        peer1Manager.connectToPeer(user2.getUserId(), "127.0.0.1", peer2Manager.getPort());
        
        // Wait for connection to establish on Peer 2
        assertTrue(peer2Handler.waitForConnection(), "Peer 2 should accept connection");
        
        // Peer 1 sends a message to Peer 2
        ProtocolMessage chatMsg = ProtocolMessage.create(MessageType.CHAT_MESSAGE, user1, new com.google.gson.JsonObject());
        chatMsg.getPayload().addProperty("text", "Hello Peer2!");
        chatMsg.getPayload().addProperty("roomId", "private");
        
        peer1Manager.sendMessage(user2.getUserId(), chatMsg);

        // Wait for Peer 2 to receive the message
        ProtocolMessage received = peer2Handler.waitForMessage();
        assertNotNull(received, "Message should be received");
        assertEquals(MessageType.CHAT_MESSAGE, received.getType());
        assertEquals("Hello Peer2!", received.getPayload().get("text").getAsString());
        assertEquals(user1.getUserId(), received.getSender().getUserId());
    }

    @Test
    void testConcurrentConnectionsAndMessaging() throws Exception {
        // Peer 1 connects to 2 and 3
        peer1Manager.connectToPeer(user2.getUserId(), "127.0.0.1", peer2Manager.getPort());
        peer1Manager.connectToPeer(user3.getUserId(), "127.0.0.1", peer3Manager.getPort());
        
        assertTrue(peer2Handler.waitForConnection());
        assertTrue(peer3Handler.waitForConnection());

        ProtocolMessage msgFor2 = ProtocolMessage.create(MessageType.CHAT_MESSAGE, user1, new com.google.gson.JsonObject());
        msgFor2.getPayload().addProperty("text", "To 2");
        peer1Manager.sendMessage(user2.getUserId(), msgFor2);

        ProtocolMessage msgFor3 = ProtocolMessage.create(MessageType.CHAT_MESSAGE, user1, new com.google.gson.JsonObject());
        msgFor3.getPayload().addProperty("text", "To 3");
        peer1Manager.sendMessage(user3.getUserId(), msgFor3);

        ProtocolMessage received2 = peer2Handler.waitForMessage();
        assertNotNull(received2);
        assertEquals("To 2", received2.getPayload().get("text").getAsString());

        ProtocolMessage received3 = peer3Handler.waitForMessage();
        assertNotNull(received3);
        assertEquals("To 3", received3.getPayload().get("text").getAsString());
    }

    @Test
    void testDisconnectionHandling() throws Exception {
        peer1Manager.connectToPeer(user2.getUserId(), "127.0.0.1", peer2Manager.getPort());
        assertTrue(peer2Handler.waitForConnection());
        
        // Peer 1 explicitly disconnects from Peer 2
        peer1Manager.disconnectPeer(user2.getUserId());
        
        // Verify Peer 2 registers the disconnection
        assertTrue(peer2Handler.waitForDisconnection(), "Peer 2 should register disconnection");
    }

    private static class TestMessageHandler implements TCPMessageHandler {
        private final CountDownLatch connectionLatch = new CountDownLatch(1);
        private final CountDownLatch disconnectionLatch = new CountDownLatch(1);
        private final List<ProtocolMessage> messages = new ArrayList<>();

        @Override
        public void onMessageReceived(ProtocolMessage message, String peerId) {
            synchronized (messages) {
                messages.add(message);
                messages.notifyAll();
            }
        }

        @Override
        public void onPeerConnected(String peerId) {
            connectionLatch.countDown();
        }

        @Override
        public void onPeerDisconnected(String peerId) {
            disconnectionLatch.countDown();
        }

        public boolean waitForConnection() throws InterruptedException {
            return connectionLatch.await(3, TimeUnit.SECONDS);
        }

        public boolean waitForDisconnection() throws InterruptedException {
            return disconnectionLatch.await(3, TimeUnit.SECONDS);
        }

        public ProtocolMessage waitForMessage() throws InterruptedException {
            synchronized (messages) {
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < 3000) {
                    for (int i = 0; i < messages.size(); i++) {
                        ProtocolMessage msg = messages.get(i);
                        if (msg.getType() != MessageType.HELLO) {
                            return messages.remove(i);
                        }
                    }
                    messages.wait(100);
                }
                return null;
            }
        }
    }
}
