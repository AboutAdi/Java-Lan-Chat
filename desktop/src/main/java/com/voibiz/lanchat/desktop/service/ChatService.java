package com.voibiz.lanchat.desktop.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.voibiz.lanchat.core.model.ChatMessage;
import com.voibiz.lanchat.core.model.Peer;
import com.voibiz.lanchat.core.model.User;
import com.voibiz.lanchat.core.network.DiscoveryListener;
import com.voibiz.lanchat.core.network.PeerRegistry;
import com.voibiz.lanchat.core.network.TCPConnectionManager;
import com.voibiz.lanchat.core.network.TCPMessageHandler;
import com.voibiz.lanchat.core.network.UDPDiscoveryService;
import com.voibiz.lanchat.core.protocol.MessageType;
import com.voibiz.lanchat.core.protocol.ProtocolMessage;

import javafx.application.Platform;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Service to bridge core network components with the UI.
 */
public class ChatService {

    private final User localUser;
    private PeerRegistry registry;
    private UDPDiscoveryService udpService;
    private TCPConnectionManager tcpManager;
    private final HistoryService historyService;
    private final Gson gson;

    /**
     * Constructs a new ChatService.
     *
     * @param localUser the local user
     */
    public ChatService(User localUser) {
        this.localUser = localUser;
        this.historyService = new HistoryService();
        this.gson = new Gson();
    }

    /**
     * Starts the network services.
     *
     * @param onMessageReceived callback for when a message is received
     * @param onPeerDiscovered  callback for when a new peer is discovered
     * @param onPeerUpdated     callback for when a peer is updated
     * @param onPeerLost        callback for when a peer is lost
     * @throws IOException if network services fail to start
     */
    public void start(Consumer<ProtocolMessage> onMessageReceived,
                      Consumer<Peer> onPeerDiscovered,
                      Consumer<Peer> onPeerUpdated,
                      Consumer<Peer> onPeerLost) throws IOException {

        registry = new PeerRegistry();
        registry.setListener(new DiscoveryListener() {
            @Override
            public void onPeerDiscovered(Peer peer) {
                if (onPeerDiscovered != null) {
                    Platform.runLater(() -> onPeerDiscovered.accept(peer));
                }
            }

            @Override
            public void onPeerUpdated(Peer peer) {
                if (onPeerUpdated != null) {
                    Platform.runLater(() -> onPeerUpdated.accept(peer));
                }
            }

            @Override
            public void onPeerLost(Peer peer) {
                if (onPeerLost != null) {
                    Platform.runLater(() -> onPeerLost.accept(peer));
                }
            }
        });

        tcpManager = new TCPConnectionManager(localUser, new TCPMessageHandler() {
            @Override
            public void onMessageReceived(ProtocolMessage message, String peerId) {
                if (message.getType() == MessageType.CHAT_MESSAGE && message.getPayload() != null) {
                    try {
                        ChatMessage chatMsg = gson.fromJson(message.getPayload(), ChatMessage.class);
                        if (chatMsg != null) {
                            historyService.saveMessage(chatMsg);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to parse chat message payload: " + e.getMessage());
                    }
                }
                if (onMessageReceived != null) {
                    Platform.runLater(() -> onMessageReceived.accept(message));
                }
            }

            @Override
            public void onPeerConnected(String peerId) {
                // Handled internally by registry if needed, no explicit requirement.
            }

            @Override
            public void onPeerDisconnected(String peerId) {
                // Handled internally by registry if needed, no explicit requirement.
            }
        }, 50506);

        udpService = new UDPDiscoveryService(localUser, registry);

        tcpManager.start();
        udpService.start();
    }

    /**
     * Sends a chat message to a peer or room.
     *
     * @param peerId the destination peer ID
     * @param roomId the destination room ID, or null for direct message
     * @param text   the message text
     */
    public void sendMessage(String peerId, String roomId, String text) {
        String recipientId = (roomId != null) ? null : peerId;
        ChatMessage chatMsg = new ChatMessage(
                localUser.getUserId(),
                localUser.getDisplayName(),
                recipientId,
                roomId,
                text
        );
        historyService.saveMessage(chatMsg);

        JsonObject payload = gson.toJsonTree(chatMsg).getAsJsonObject();
        ProtocolMessage protoMsg = ProtocolMessage.create(MessageType.CHAT_MESSAGE, localUser, payload);

        try {
            Peer peer = registry.getPeer(peerId);
            if (peer != null) {
                tcpManager.connectToPeer(peerId, peer.getIpAddress(), peer.getPort());
            }
            tcpManager.sendMessage(peerId, protoMsg);
        } catch (IOException e) {
            System.err.println("Failed to send message to peer " + peerId + ": " + e.getMessage());
        }
    }

    /**
     * Stops the network services.
     */
    public void stop() {
        if (udpService != null) {
            udpService.sendGoodbye();
            udpService.stop();
        }
        if (tcpManager != null) {
            tcpManager.stop();
        }
    }

    /**
     * Retrieves the HistoryService instance.
     *
     * @return the history service
     */
    public HistoryService getHistoryService() {
        return historyService;
    }
}
