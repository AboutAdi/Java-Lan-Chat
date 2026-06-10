package com.voibiz.lanchat.desktop.controller;

import com.google.gson.Gson;
import com.voibiz.lanchat.core.model.ChatMessage;
import com.voibiz.lanchat.core.model.Peer;
import com.voibiz.lanchat.core.model.User;
import com.voibiz.lanchat.core.protocol.MessageType;
import com.voibiz.lanchat.desktop.component.ChatBubble;
import com.voibiz.lanchat.desktop.service.ChatService;
import com.voibiz.lanchat.desktop.service.HistoryService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public class MainController {

    @FXML
    private ListView<Peer> peerList;

    @FXML
    private VBox chatArea;
    
    @FXML
    private ScrollPane chatScrollPane;
    
    @FXML
    private Label chatHeaderName;
    
    @FXML
    private Label chatHeaderStatus;

    @FXML
    private TextField messageField;

    @FXML
    private Button sendButton;

    private ChatService chatService;
    private User localUser;
    private HistoryService historyService;
    private final Gson gson = new Gson();
    private final java.util.Map<String, List<ChatMessage>> chatHistories = new java.util.HashMap<>();

    @FXML
    public void initialize() {
        peerList.setCellFactory(param -> new ListCell<Peer>() {
            @Override
            protected void updateItem(Peer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName() + " [" + item.getStatus() + "]");
                }
            }
        });

        peerList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            chatArea.getChildren().clear();
            if (newVal != null) {
                chatHeaderName.setText(newVal.getDisplayName());
                chatHeaderStatus.setText(newVal.getStatus().toString());
                
                List<ChatMessage> history = chatHistories.get(newVal.getUserId());
                if (history != null) {
                    for (ChatMessage msg : history) {
                        boolean isMine = msg.getSenderId().equals(localUser.getUserId());
                        
                        // If it's a message from them, and not read yet, mark read and send receipt
                        if (!isMine && msg.getStatus() != ChatMessage.MessageStatus.READ) {
                            msg.setStatus(ChatMessage.MessageStatus.READ);
                            if (chatService != null) {
                                chatService.sendReceipt(newVal.getUserId(), msg.getMessageId(), MessageType.MESSAGE_READ);
                            }
                        }
                        
                        ChatBubble bubble = new ChatBubble(msg, isMine);
                        chatArea.getChildren().add(bubble);
                    }
                }
            }
        });

        sendButton.disableProperty().bind(peerList.getSelectionModel().selectedItemProperty().isNull());
        messageField.disableProperty().bind(peerList.getSelectionModel().selectedItemProperty().isNull());
    }

    public void initUser(User user) {
        this.localUser = user;
        this.chatService = new ChatService(localUser);
        this.historyService = chatService.getHistoryService();

        try {
            chatService.start(
                message -> Platform.runLater(() -> {
                    if (message.getType() == MessageType.CHAT_MESSAGE && message.getPayload() != null) {
                        ChatMessage chatMsg = gson.fromJson(message.getPayload(), ChatMessage.class);
                        if (chatMsg != null) {
                            String senderId = chatMsg.getSenderId();
                            if (senderId.equals(localUser.getUserId())) {
                                senderId = chatMsg.getRecipientId();
                            }
                            
                            List<ChatMessage> history = chatHistories.get(senderId);
                            if (history == null) {
                                history = new java.util.ArrayList<>();
                                chatHistories.put(senderId, history);
                            }
                            history.add(chatMsg);

                            Peer selectedPeer = peerList.getSelectionModel().getSelectedItem();
                            if (selectedPeer != null && senderId.equals(selectedPeer.getUserId())) {
                                chatMsg.setStatus(ChatMessage.MessageStatus.READ);
                                chatService.sendReceipt(senderId, chatMsg.getMessageId(), MessageType.MESSAGE_READ);
                                ChatBubble bubble = new ChatBubble(chatMsg, false);
                                chatArea.getChildren().add(bubble);
                                
                                // Auto-scroll to bottom
                                Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
                            }
                        }
                    } else if (message.getType() == MessageType.MESSAGE_DELIVERED || message.getType() == MessageType.MESSAGE_READ) {
                        String messageId = null;
                        if (message.getPayload() != null && message.getPayload().has("messageId")) {
                            messageId = message.getPayload().get("messageId").getAsString();
                        }
                        if (messageId == null) return;
                        
                        String peerId = message.getSender().getUserId();
                        List<ChatMessage> history = chatHistories.get(peerId);
                        if (history != null) {
                            for (ChatMessage msg : history) {
                                if (msg.getMessageId().equals(messageId)) {
                                    msg.setStatus(message.getType() == MessageType.MESSAGE_DELIVERED ? 
                                        ChatMessage.MessageStatus.DELIVERED : ChatMessage.MessageStatus.READ);
                                    
                                    Peer selectedPeer = peerList.getSelectionModel().getSelectedItem();
                                    if (selectedPeer != null && peerId.equals(selectedPeer.getUserId())) {
                                        for (javafx.scene.Node node : chatArea.getChildren()) {
                                            if (node instanceof ChatBubble) {
                                                ChatBubble cb = (ChatBubble) node;
                                                if (cb.getMessage().getMessageId().equals(messageId)) {
                                                    cb.updateStatus();
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }),
                peer -> Platform.runLater(() -> {
                    ObservableList<Peer> items = peerList.getItems();
                    if (!items.contains(peer)) {
                        items.add(peer);
                    }
                }),
                peer -> Platform.runLater(() -> {
                    ObservableList<Peer> items = peerList.getItems();
                    int index = items.indexOf(peer);
                    if (index >= 0) {
                        Peer existing = items.get(index);
                        existing.setDisplayName(peer.getDisplayName());
                        existing.setIpAddress(peer.getIpAddress());
                        existing.setPort(peer.getPort());
                        existing.setStatus(peer.getStatus());
                        existing.setLastSeenTimestamp(peer.getLastSeenTimestamp());
                        peerList.refresh();
                    } else {
                        items.add(peer);
                    }
                    
                    // Update header if this peer is currently selected
                    Peer selectedPeer = peerList.getSelectionModel().getSelectedItem();
                    if (selectedPeer != null && selectedPeer.equals(peer)) {
                        chatHeaderName.setText(peer.getDisplayName());
                        chatHeaderStatus.setText(peer.getStatus().toString());
                    }
                }),
                peer -> Platform.runLater(() -> {
                    peerList.getItems().remove(peer);
                })
            );
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to start ChatService: " + e.getMessage());
        }
        
        // Load history for general or something, but skipping for now
    }

    @FXML
    public void onSend(ActionEvent event) {
        String text = messageField.getText();

        if (text != null && !text.trim().isEmpty()) {
            Peer selectedPeer = peerList.getSelectionModel().getSelectedItem();

            if (selectedPeer != null) {
                ChatMessage msg = new ChatMessage(
                        localUser.getUserId(),
                        localUser.getDisplayName(),
                        selectedPeer.getUserId(),
                        null,
                        text.trim()
                );

                chatService.sendMessage(selectedPeer.getUserId(), msg);

                String peerId = selectedPeer.getUserId();
                List<ChatMessage> history = chatHistories.get(peerId);
                if (history == null) {
                    history = new java.util.ArrayList<>();
                    chatHistories.put(peerId, history);
                }
                history.add(msg);

                ChatBubble bubble = new ChatBubble(msg, true);
                chatArea.getChildren().add(bubble);
                
                // Auto-scroll to bottom
                Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
            }

            messageField.clear();
        }
    }

    @FXML
    public void onRefresh(ActionEvent event) {
        if (chatService != null) {
            chatService.refreshDiscovery();
        }
    }

    @FXML
    public void onClearData(ActionEvent event) {
        if (chatService != null) {
            chatService.clearData();
            chatService.stop();
        }
        
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(com.voibiz.lanchat.desktop.App.class);
        prefs.remove("lanchat.username");
        prefs.remove("lanchat.userid");
        
        // Go back to login screen
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 400, 400);
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setTitle("LAN Chat");
            stage.setScene(scene);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        // Delete user_profile.json
        java.io.File profileFile = new java.io.File("user_profile.json");
        if (profileFile.exists()) {
            profileFile.delete();
        }

        // Delete lanchat.db
        java.io.File dbFile = new java.io.File("lanchat.db");
        if (dbFile.exists()) {
            dbFile.delete();
        }

        // Navigate back to Login
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root, 400, 500));
            stage.setTitle("LAN Chat - Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
