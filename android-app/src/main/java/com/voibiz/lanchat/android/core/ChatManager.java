package com.voibiz.lanchat.android.core;

import android.content.Context;
import com.voibiz.lanchat.core.model.ChatMessage;
import com.voibiz.lanchat.core.model.User;
import com.voibiz.lanchat.core.network.PeerRegistry;
import com.voibiz.lanchat.core.network.TCPConnectionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatManager {
    private static ChatManager instance;

    private PeerRegistry registry;
    private com.voibiz.lanchat.core.network.DiscoveryService discoveryService;
    private com.voibiz.lanchat.core.network.DiscoveryService udpDiscoveryService;
    private TCPConnectionManager tcpManager;
    private User localUser;
    private com.voibiz.lanchat.core.network.KnownPeerCache knownPeerCache;
    private com.voibiz.lanchat.android.data.AppDatabase appDatabase;
    private android.net.wifi.WifiManager.MulticastLock multicastLock;

    private final List<Consumer<User>> peerDiscoveryListeners = new ArrayList<>();
    private final List<Consumer<ChatMessage>> messageReceivedListeners = new ArrayList<>();
    private final List<java.util.function.BiConsumer<String, com.voibiz.lanchat.core.model.ChatMessage.MessageStatus>> messageReceiptListeners = new ArrayList<>();
    private ScheduledExecutorService refreshScheduler;

    private ChatManager() {
    }

    public static synchronized ChatManager getInstance() {
        if (instance == null) {
            instance = new ChatManager();
        }
        return instance;
    }

    public void init(User user, Context context) {
        this.localUser = user;
        this.knownPeerCache = new com.voibiz.lanchat.core.network.KnownPeerCache(new java.io.File(context.getFilesDir(), "known_peers.json"));
        this.appDatabase = com.voibiz.lanchat.android.data.AppDatabase.getInstance(context);
        
        android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            this.multicastLock = wifi.createMulticastLock("LanChatMulticastLock");
            this.multicastLock.setReferenceCounted(true);
            this.multicastLock.acquire();
        }

        try {
            this.registry = new PeerRegistry();
            this.tcpManager = new TCPConnectionManager(this.localUser, new com.voibiz.lanchat.core.network.TCPMessageHandler() {
                @Override
                public void onMessageReceived(com.voibiz.lanchat.core.protocol.ProtocolMessage message, String peerId) {
                    if (registry.getPeer(peerId) == null && message.getSender() != null) {
                        User sender = message.getSender();
                        com.voibiz.lanchat.core.model.Peer fallbackPeer = new com.voibiz.lanchat.core.model.Peer(
                            sender.getUserId(), sender.getDisplayName(), sender.getIpAddress(), sender.getPort(), com.voibiz.lanchat.core.model.Peer.PeerStatus.ONLINE
                        );
                        registry.addOrUpdatePeer(fallbackPeer);
                    }

                    if (message.getType() == com.voibiz.lanchat.core.protocol.MessageType.CHAT_MESSAGE) {
                        try {
                            ChatMessage chatMsg = new com.google.gson.Gson().fromJson(message.getPayload(), ChatMessage.class);
                            
                            com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                            payload.addProperty("messageId", chatMsg.getMessageId());
                            com.voibiz.lanchat.core.protocol.ProtocolMessage receipt = com.voibiz.lanchat.core.protocol.ProtocolMessage.create(com.voibiz.lanchat.core.protocol.MessageType.MESSAGE_DELIVERED, localUser, payload);
                            tcpManager.sendMessage(peerId, receipt);

                            // Save to database
                            if (appDatabase != null) {
                                new Thread(() -> appDatabase.messageDao().insert(new com.voibiz.lanchat.android.data.MessageEntity(chatMsg))).start();
                            }

                            notifyMessageReceived(chatMsg);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (message.getType() == com.voibiz.lanchat.core.protocol.MessageType.MESSAGE_DELIVERED) {
                        String messageId = null;
                        if (message.getPayload() != null && message.getPayload().has("messageId")) {
                            messageId = message.getPayload().get("messageId").getAsString();
                        }
                        if (messageId != null) {
                            String finalMessageId = messageId;
                            if (appDatabase != null) {
                                new Thread(() -> {
                                    appDatabase.messageDao().updateStatus(finalMessageId, com.voibiz.lanchat.core.model.ChatMessage.MessageStatus.DELIVERED.name());
                                }).start();
                            }
                            for (java.util.function.BiConsumer<String, ChatMessage.MessageStatus> listener : messageReceiptListeners) {
                                listener.accept(finalMessageId, ChatMessage.MessageStatus.DELIVERED);
                            }
                        }
                    } else if (message.getType() == com.voibiz.lanchat.core.protocol.MessageType.MESSAGE_READ) {
                        String messageId = null;
                        if (message.getPayload() != null && message.getPayload().has("messageId")) {
                            messageId = message.getPayload().get("messageId").getAsString();
                        }
                        if (messageId != null) {
                            String finalMessageId = messageId;
                            if (appDatabase != null) {
                                new Thread(() -> {
                                    appDatabase.messageDao().updateStatus(finalMessageId, com.voibiz.lanchat.core.model.ChatMessage.MessageStatus.READ.name());
                                }).start();
                            }
                            for (java.util.function.BiConsumer<String, ChatMessage.MessageStatus> listener : messageReceiptListeners) {
                                listener.accept(finalMessageId, ChatMessage.MessageStatus.READ);
                            }
                        }
                    }
                }
                @Override
                public void onPeerConnected(String peerId) {}
                @Override
                public void onPeerDisconnected(String peerId) {
                    if (registry != null) {
                        registry.removePeer(peerId);
                    }
                }
            });
            
            this.discoveryService = new com.voibiz.lanchat.core.network.JmDNSDiscoveryService(this.localUser, this.registry);
            this.udpDiscoveryService = new com.voibiz.lanchat.core.network.UDPBroadcastDiscoveryService(this.localUser, this.registry);

            this.registry.setListener(new com.voibiz.lanchat.core.network.DiscoveryListener() {
                @Override
                public void onPeerDiscovered(com.voibiz.lanchat.core.model.Peer peer) {
                    if (knownPeerCache != null) {
                        knownPeerCache.addPeer(peer);
                    }
                    notifyPeerDiscovered(peer);
                    new Thread(() -> {
                        try {
                            if (tcpManager != null) {
                                tcpManager.connectToPeer(peer.getUserId(), peer.getIpAddress(), peer.getPort());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }

                @Override
                public void onPeerUpdated(com.voibiz.lanchat.core.model.Peer peer) {
                    notifyPeerDiscovered(peer);
                }

                @Override
                public void onPeerLost(com.voibiz.lanchat.core.model.Peer peer) {
                    notifyPeerDiscovered(peer);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            if (tcpManager != null) {
                tcpManager.start();
                if (discoveryService != null) {
                    discoveryService.setPort(tcpManager.getLocalPort());
                }
                if (udpDiscoveryService != null) {
                    udpDiscoveryService.setPort(tcpManager.getLocalPort());
                }
                
                // Proactively connect to known peers
                if (knownPeerCache != null && localUser != null) {
                    for (com.voibiz.lanchat.core.model.Peer peer : knownPeerCache.getKnownPeers()) {
                        if (!peer.getUserId().equals(localUser.getUserId())) {
                            new Thread(() -> {
                                try {
                                    tcpManager.connectToPeer(peer.getUserId(), peer.getIpAddress(), peer.getPort());
                                } catch (Exception e) {
                                    // Expected if peer is offline
                                }
                            }).start();
                        }
                    }
                }
            }

            if (discoveryService != null) {
                try { discoveryService.start(); } catch (Exception e) {}
            }
            if (udpDiscoveryService != null) {
                try { udpDiscoveryService.start(); } catch (Exception e) {}
            }

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshDiscovery() {
        if (discoveryService != null) {
            new Thread(() -> {
                try {
                    discoveryService.stop();
                    if (udpDiscoveryService != null) udpDiscoveryService.stop();
                    // Small delay to let the OS unbind
                    Thread.sleep(500);
                    discoveryService.start();
                    if (udpDiscoveryService != null) udpDiscoveryService.start();
                    System.out.println("[ChatManager] Discovery service refreshed.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

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
        if (multicastLock != null && multicastLock.isHeld()) {
            multicastLock.release();
            multicastLock = null;
        }
    }

    public void addPeerDiscoveryListener(Consumer<User> listener) {
        peerDiscoveryListeners.add(listener);
    }

    public void removePeerDiscoveryListener(Consumer<User> listener) {
        peerDiscoveryListeners.remove(listener);
    }

    public void notifyPeerDiscovered(User user) {
        for (Consumer<User> listener : peerDiscoveryListeners) {
            listener.accept(user);
        }
    }

    public void addMessageReceivedListener(Consumer<ChatMessage> listener) {
        messageReceivedListeners.add(listener);
    }

    public void removeMessageReceivedListener(Consumer<ChatMessage> listener) {
        messageReceivedListeners.remove(listener);
    }

    public void notifyMessageReceived(ChatMessage message) {
        for (Consumer<ChatMessage> listener : messageReceivedListeners) {
            listener.accept(message);
        }
    }

    public void addMessageReceiptListener(java.util.function.BiConsumer<String, ChatMessage.MessageStatus> listener) {
        messageReceiptListeners.add(listener);
    }

    public void removeMessageReceiptListener(java.util.function.BiConsumer<String, ChatMessage.MessageStatus> listener) {
        messageReceiptListeners.remove(listener);
    }

    public void clearData() {
        if (knownPeerCache != null) {
            knownPeerCache.clear();
        }
    }

    public TCPConnectionManager getTcpManager() {
        return tcpManager;
    }

    public PeerRegistry getRegistry() {
        return registry;
    }

    public User getLocalUser() {
        return localUser;
    }

    public void sendMessage(String peerId, ChatMessage chatMessage) {
        if (tcpManager == null || registry == null) return;

        com.google.gson.JsonObject payload = (com.google.gson.JsonObject) new com.google.gson.Gson().toJsonTree(chatMessage);
        com.voibiz.lanchat.core.protocol.ProtocolMessage protocolMessage = com.voibiz.lanchat.core.protocol.ProtocolMessage.create(
                com.voibiz.lanchat.core.protocol.MessageType.CHAT_MESSAGE,
                localUser,
                payload
        );

        new Thread(() -> {
            try {
                if (appDatabase != null) {
                    appDatabase.messageDao().insert(new com.voibiz.lanchat.android.data.MessageEntity(chatMessage));
                }
                
                com.voibiz.lanchat.core.model.Peer peer = registry.getPeer(peerId);
                if (peer != null) {
                    tcpManager.connectToPeer(peerId, peer.getIpAddress(), peer.getPort());
                    tcpManager.sendMessage(peerId, protocolMessage);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void sendReceipt(String peerId, String messageId, com.voibiz.lanchat.core.protocol.MessageType receiptType) {
        if (tcpManager == null || registry == null) return;
        new Thread(() -> {
            try {
                com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                payload.addProperty("messageId", messageId);
                com.voibiz.lanchat.core.protocol.ProtocolMessage receipt = com.voibiz.lanchat.core.protocol.ProtocolMessage.create(
                        receiptType, localUser, payload
                );
                tcpManager.sendMessage(peerId, receipt);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public com.voibiz.lanchat.android.data.AppDatabase getDatabase() {
        return appDatabase;
    }
}
