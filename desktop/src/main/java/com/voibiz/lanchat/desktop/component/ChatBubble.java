package com.voibiz.lanchat.desktop.component;

import com.voibiz.lanchat.core.model.ChatMessage;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class ChatBubble extends HBox {

    public ChatBubble(ChatMessage message, boolean isSentByMe) {
        super();
        
        Label messageLabel = new Label(message.getText());
        messageLabel.setWrapText(true);
        
        if (isSentByMe) {
            messageLabel.getStyleClass().add("chat-bubble-sent");
            this.setAlignment(Pos.CENTER_RIGHT);
        } else {
            messageLabel.getStyleClass().add("chat-bubble-received");
            this.setAlignment(Pos.CENTER_LEFT);
        }
        
        this.getChildren().add(messageLabel);
    }
}
