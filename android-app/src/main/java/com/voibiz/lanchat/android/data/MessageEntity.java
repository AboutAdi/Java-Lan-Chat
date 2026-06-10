package com.voibiz.lanchat.android.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.voibiz.lanchat.core.model.ChatMessage;

@Entity(tableName = "messages")
public class MessageEntity {

    @PrimaryKey
    @NonNull
    public String messageId;

    public String senderId;
    public String senderName;
    public String recipientId;
    public String roomId;
    public String text;
    public long timestamp;
    public String status;

    public MessageEntity() {}

    public MessageEntity(ChatMessage chatMessage) {
        this.messageId = chatMessage.getMessageId();
        this.senderId = chatMessage.getSenderId();
        this.senderName = chatMessage.getSenderName();
        this.recipientId = chatMessage.getRecipientId();
        this.roomId = chatMessage.getRoomId();
        this.text = chatMessage.getText();
        this.timestamp = chatMessage.getTimestamp();
        this.status = chatMessage.getStatus() != null ? chatMessage.getStatus().name() : null;
    }

    public ChatMessage toChatMessage() {
        return new ChatMessage(
                messageId,
                senderId,
                senderName,
                recipientId,
                roomId,
                text,
                timestamp,
                status != null ? ChatMessage.MessageStatus.valueOf(status) : null
        );
    }
}
