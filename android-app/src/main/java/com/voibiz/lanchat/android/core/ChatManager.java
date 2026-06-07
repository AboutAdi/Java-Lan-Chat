package com.voibiz.lanchat.android.core;

import android.content.Context;
import com.voibiz.lanchat.core.model.ChatMessage;
import com.voibiz.lanchat.core.model.User;
import com.voibiz.lanchat.core.network.PeerRegistry;
import com.voibiz.lanchat.core.network.TCPConnectionManager;
import com.voibiz.lanchat.core.network.UDPDiscoveryService;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChatManager {
    private static ChatManager instance;

    private PeerRegistry registry;
    private UDPDiscoveryService udpService;
    private TCPConnectionManager tcpManager;
    private User localUser;

    private final List<Consumer<User>> peerDiscoveryListeners = new ArrayList<>();
    private final List<Consumer<ChatMessage>> messageReceivedListeners = new ArrayList<>();

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
        try {
            this.registry = new PeerRegistry();
            this.tcpManager = new TCPConnectionManager(this.localUser, new com.voibiz.lanchat.core.network.TCPMessageHandler() {
                @Override
                public void onMessageReceived(com.voibiz.lanchat.core.protocol.ProtocolMessage message, String peerId) {
                    if (message.getType() == com.voibiz.lanchat.core.protocol.MessageType.CHAT_MESSAGE && message.getPayload() != null) {
                        ChatMessage chatMsg = new com.google.gson.Gson().fromJson(message.getPayload(), ChatMessage.class);
                        if (chatMsg != null) {
                            notifyMessageReceived(chatMsg);
                        }
                    }
                }
                @Override
                public void onPeerConnected(String peerId) {}
                @Override
                public void onPeerDisconnected(String peerId) {}
            }, 50506);
            this.udpService = new UDPDiscoveryService(this.localUser, this.registry);

            // Optional: Listeners configuration depending on your core implementation
            // this.registry.setPeerDiscoveredListener(discoveredUser -> {
            //     for (Consumer<User> listener : peerDiscoveryListeners) {
            //         listener.accept(discoveredUser);
            //     }
            // });
            //
            // this.tcpManager.setMessageReceivedListener(message -> {
            //     for (Consumer<Message> listener : messageReceivedListeners) {
            //         listener.accept(message);
            //     }
            // });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            if (udpService != null) {
                udpService.start();
            }
            if (tcpManager != null) {
                tcpManager.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (udpService != null) {
            udpService.stop();
        }
        if (tcpManager != null) {
            tcpManager.stop();
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

    public TCPConnectionManager getTcpManager() {
        return tcpManager;
    }

    public PeerRegistry getRegistry() {
        return registry;
    }

    public User getLocalUser() {
        return localUser;
    }
}
