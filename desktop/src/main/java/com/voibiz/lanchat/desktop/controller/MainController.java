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
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class MainController {

    @FXML
    private ListView<Peer> peerList;

    @FXML
    private VBox chatArea;

    @FXML
    private TextField messageField;

    @FXML
    private Button sendButton;

    private ChatService chatService;
    private User localUser;
    private HistoryService historyService;
    private final Gson gson = new Gson();

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
                            ChatBubble bubble = new ChatBubble(chatMsg, false);
                            chatArea.getChildren().add(bubble);
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
                        items.set(index, peer);
                    } else {
                        items.add(peer);
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
        Peer selectedPeer = peerList.getSelectionModel().getSelectedItem();

        if (text != null && !text.trim().isEmpty() && selectedPeer != null) {
            ChatMessage msg = new ChatMessage(
                    localUser.getUserId(),
                    localUser.getDisplayName(),
                    selectedPeer.getUserId(),
                    null,
                    text.trim()
            );

            chatService.sendMessage(selectedPeer.getUserId(), null, text.trim());

            ChatBubble bubble = new ChatBubble(msg, true);
            chatArea.getChildren().add(bubble);

            messageField.clear();
        }
    }
}
