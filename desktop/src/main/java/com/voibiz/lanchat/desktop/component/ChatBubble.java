package com.voibiz.lanchat.desktop.component;

import com.voibiz.lanchat.core.model.ChatMessage;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class ChatBubble extends HBox {

    private final ChatMessage message;
    private final boolean isSentByMe;
    private final Label statusLabel;

    public ChatBubble(ChatMessage message, boolean isSentByMe) {
        super();
        this.message = message;
        this.isSentByMe = isSentByMe;
        
        this.getStyleClass().add("chat-bubble-container");
        
        javafx.scene.layout.VBox bubbleContainer = new javafx.scene.layout.VBox();
        bubbleContainer.setSpacing(2);
        if (isSentByMe) {
            bubbleContainer.getStyleClass().add("chat-bubble-sent");
        } else {
            bubbleContainer.getStyleClass().add("chat-bubble-received");
        }
        
        Label messageLabel = new Label(message.getText());
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("chat-text");
        messageLabel.setMaxWidth(400); // Prevent extremely wide bubbles
        
        HBox metaBox = new HBox();
        metaBox.setAlignment(Pos.CENTER_RIGHT);
        metaBox.setSpacing(4);
        
        // Very simple timestamp format using java.util.Date
        String timeStr = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date(message.getTimestamp()));
        Label timeLabel = new Label(timeStr);
        timeLabel.getStyleClass().add("chat-meta");
        
        statusLabel = new Label();
        statusLabel.getStyleClass().add("chat-meta");
        
        if (isSentByMe) {
            metaBox.getChildren().addAll(timeLabel, statusLabel);
        } else {
            metaBox.getChildren().add(timeLabel);
        }
        
        bubbleContainer.getChildren().addAll(messageLabel, metaBox);
        
        if (isSentByMe) {
            this.setAlignment(Pos.CENTER_RIGHT);
        } else {
            this.setAlignment(Pos.CENTER_LEFT);
        }
        
        this.getChildren().add(bubbleContainer);
        updateStatus();
    }

    public void updateStatus() {
        if (!isSentByMe) return;
        switch (message.getStatus()) {
            case SENDING: statusLabel.setText(""); break;
            case SENT: statusLabel.setText("✓"); statusLabel.setStyle("-fx-text-fill: #667781;"); break;
            case DELIVERED: statusLabel.setText("✓✓"); statusLabel.setStyle("-fx-text-fill: #667781;"); break;
            case READ: statusLabel.setText("✓✓"); statusLabel.setStyle("-fx-text-fill: #53bdeb;"); break;
        }
    }

    public ChatMessage getMessage() {
        return message;
    }
}
