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
import com.voibiz.lanchat.core.network.DiscoveryService;
import com.voibiz.lanchat.core.network.JmDNSDiscoveryService;
import com.voibiz.lanchat.core.protocol.MessageType;
import com.voibiz.lanchat.core.protocol.ProtocolMessage;

import javafx.application.Platform;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service to bridge core network components with the UI.
 */
public class ChatService {

    private final User localUser;
    private PeerRegistry registry;
    private DiscoveryService discoveryService;
    private DiscoveryService udpDiscoveryService;
    private TCPConnectionManager tcpManager;
    private final HistoryService historyService;
    private final Gson gson;
    private ScheduledExecutorService refreshScheduler;
    private com.voibiz.lanchat.core.network.KnownPeerCache knownPeerCache;

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
        knownPeerCache = new com.voibiz.lanchat.core.network.KnownPeerCache(new java.io.File("known_peers.json"));
        
        registry.setListener(new DiscoveryListener() {
            @Override
            public void onPeerDiscovered(Peer peer) {
                knownPeerCache.addPeer(peer);
                if (onPeerDiscovered != null) {
                    Platform.runLater(() -> onPeerDiscovered.accept(peer));
                }
                new Thread(() -> {
                    try {
                        tcpManager.connectToPeer(peer.getUserId(), peer.getIpAddress(), peer.getPort());
                    } catch (IOException e) {
                        System.err.println("Failed proactive connection to peer " + peer.getUserId() + ": " + e.getMessage());
                    }
                }).start();
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
                if (registry.getPeer(peerId) == null && message.getSender() != null) {
                    User sender = message.getSender();
                    // Fallback registration if UDP discovery missed them
                    Peer fallbackPeer = new Peer(sender.getUserId(), sender.getDisplayName(), sender.getIpAddress(), sender.getPort(), Peer.PeerStatus.ONLINE);
                    registry.addOrUpdatePeer(fallbackPeer);
                }
                
                if (message.getType() == MessageType.CHAT_MESSAGE && message.getPayload() != null) {
                    ChatMessage chatMsg = gson.fromJson(message.getPayload(), ChatMessage.class);
                    if (chatMsg != null) {
                        historyService.saveMessage(chatMsg);
                        
                        // Send delivered receipt
                        try {
                            JsonObject payload = new JsonObject();
                            payload.addProperty("messageId", chatMsg.getMessageId());
                            ProtocolMessage receipt = ProtocolMessage.create(MessageType.MESSAGE_DELIVERED, localUser, payload);
                            tcpManager.sendMessage(peerId, receipt);
                        } catch (Exception e) {}

                        if (onMessageReceived != null) {
                            Platform.runLater(() -> onMessageReceived.accept(message));
                        }
                    }
                } else if (message.getType() == MessageType.MESSAGE_DELIVERED || message.getType() == MessageType.MESSAGE_READ) {
                    if (onMessageReceived != null) {
                        Platform.runLater(() -> onMessageReceived.accept(message));
                    }
                }
            }

            @Override
            public void onPeerConnected(String peerId) {
                // Handled internally by registry if needed, no explicit requirement.
            }

            @Override
            public void onPeerDisconnected(String peerId) {
                registry.removePeer(peerId);
            }
        });

        tcpManager.start();

        discoveryService = new JmDNSDiscoveryService(localUser, registry);
        discoveryService.setPort(tcpManager.getLocalPort());
        
        // Proactively connect to known peers
        for (Peer peer : knownPeerCache.getKnownPeers()) {
            if (!peer.getUserId().equals(localUser.getUserId())) {
                new Thread(() -> {
                    try {
                        tcpManager.connectToPeer(peer.getUserId(), peer.getIpAddress(), peer.getPort());
                    } catch (IOException e) {
                        // Expected if peer is offline
                    }
                }).start();
            }
        }
        
        discoveryService.start();
        
        udpDiscoveryService = new com.voibiz.lanchat.core.network.UDPBroadcastDiscoveryService(localUser, registry);
        udpDiscoveryService.setPort(tcpManager.getLocalPort());
        udpDiscoveryService.start();

        if (refreshScheduler == null || refreshScheduler.isShutdown()) {
            refreshScheduler = Executors.newSingleThreadScheduledExecutor();
            refreshScheduler.scheduleWithFixedDelay(() -> {
                if (discoveryService != null) {
                    discoveryService.silentRefresh();
                }
                if (udpDiscoveryService != null) {
                    udpDiscoveryService.silentRefresh();
                }
            }, 10, 10, TimeUnit.SECONDS);
        }
    }

    /**
     * Refreshes the network discovery by restarting the service.
     */
    public void refreshDiscovery() {
        if (discoveryService != null) {
            new Thread(() -> {
                try {
                    discoveryService.stop();
                    if (udpDiscoveryService != null) udpDiscoveryService.stop();
                    Thread.sleep(500); // Give JmDNS time to clear sockets
                    discoveryService.start();
                    if (udpDiscoveryService != null) udpDiscoveryService.start();
                    System.out.println("[ChatService] Discovery service refreshed.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * Sends a chat message to a peer or room.
     *
     * @param peerId the destination peer ID
     * @param roomId the destination room ID, or null for direct message
     * @param text   the message text
     */
    public void sendMessage(String peerId, ChatMessage chatMsg) {
        historyService.saveMessage(chatMsg);

        JsonObject payload = gson.toJsonTree(chatMsg).getAsJsonObject();
        ProtocolMessage protoMsg = ProtocolMessage.create(MessageType.CHAT_MESSAGE, localUser, payload);

        new Thread(() -> {
            try {
                Peer peer = registry.getPeer(peerId);
                if (peer != null) {
                    tcpManager.connectToPeer(peerId, peer.getIpAddress(), peer.getPort());
                }
                tcpManager.sendMessage(peerId, protoMsg);
            } catch (IOException e) {
                System.err.println("Failed to send message to peer " + peerId + ": " + e.getMessage());
            }
        }).start();
    }

    public void sendReceipt(String peerId, String messageId, MessageType receiptType) {
        if (tcpManager == null) return;
        new Thread(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("messageId", messageId);
                ProtocolMessage receipt = ProtocolMessage.create(receiptType, localUser, payload);
                tcpManager.sendMessage(peerId, receipt);
            } catch (Exception e) {}
        }).start();
    }

    /**
     * Broadcasts a chat message to all known online peers.
     *
     * @param text the message text
     */
    public void broadcastMessage(String text) {
        ChatMessage chatMsg = new ChatMessage(
                localUser.getUserId(),
                localUser.getDisplayName(),
                null,
                null,
                text
        );
        historyService.saveMessage(chatMsg);

        JsonObject payload = gson.toJsonTree(chatMsg).getAsJsonObject();
        ProtocolMessage protoMsg = ProtocolMessage.create(MessageType.CHAT_MESSAGE, localUser, payload);

        new Thread(() -> {
            for (Peer peer : registry.getOnlinePeers()) {
                if (!peer.getUserId().equals(localUser.getUserId())) {
                    try {
                        tcpManager.connectToPeer(peer.getUserId(), peer.getIpAddress(), peer.getPort());
                    } catch (IOException e) {
                        // ignore
                    }
                    tcpManager.sendMessage(peer.getUserId(), protoMsg);
                }
            }
        }).start();
    }

    /**
     * Stops the network services.
     */
    public void stop() {
        if (discoveryService != null) {
            discoveryService.stop();
        }
        if (udpDiscoveryService != null) {
            udpDiscoveryService.stop();
        }
        if (tcpManager != null) {
            tcpManager.stop();
        }
        if (refreshScheduler != null) {
            refreshScheduler.shutdownNow();
        }
    }

    /**
     * Clears persistent network data.
     */
    public void clearData() {
        if (knownPeerCache != null) {
            knownPeerCache.clear();
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
