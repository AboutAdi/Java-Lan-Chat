package com.voibiz.lanchat.android.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {

    @PrimaryKey
    @NonNull
    private String id;
    
    private String senderId;
    private String senderName;
    private String roomId;
    private String text;
    private long timestamp;
    private String status;

    public MessageEntity() {
    }

    @androidx.room.Ignore
    public MessageEntity(@NonNull String id, String senderId, String senderName, String roomId, String text, long timestamp, String status) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.roomId = roomId;
        this.text = text;
        this.timestamp = timestamp;
        this.status = status;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
