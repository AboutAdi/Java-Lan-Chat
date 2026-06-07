package com.voibiz.lanchat.core.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single chat message exchanged between users.
 *
 * <p>Each message is uniquely identified by a {@code messageId} (UUID string) and
 * carries sender/recipient addressing, the message text, a creation timestamp,
 * and a {@link MessageStatus} tracking delivery progress.</p>
 *
 * <p>A message may target a specific recipient (direct message) or a chat room
 * (group message). When sent to a room, {@code recipientId} is {@code null} and
 * {@code roomId} identifies the destination.</p>
 *
 * <p>Equality and hash code are based solely on {@code messageId}.</p>
 *
 * @author Adi
 */
public class ChatMessage {

    /**
     * Enumerates the delivery lifecycle states of a chat message.
     */
    public enum MessageStatus {
        /** The message is being transmitted to the recipient. */
        SENDING,
        /** The message has been sent to the network. */
        SENT,
        /** The message has been delivered to the recipient's device. */
        DELIVERED,
        /** The message has been read by the recipient. */
        READ
    }

    /** Unique identifier for this message (UUID string). */
    private String messageId;

    /** User ID of the sender. */
    private String senderId;

    /** Display name of the sender at the time of sending. */
    private String senderName;

    /** User ID of the recipient, or {@code null} for room/group messages. */
    private String recipientId;

    /** ID of the chat room this message belongs to. */
    private String roomId;

    /** The textual content of the message. */
    private String text;

    /** Epoch millisecond timestamp when the message was created. */
    private long timestamp;

    /** Current delivery status of the message. */
    private MessageStatus status;

    /**
     * Constructs a new {@code ChatMessage} with an auto-generated message ID
     * and the current system time as the timestamp. The initial status is set
     * to {@link MessageStatus#SENDING}.
     *
     * @param senderId    the user ID of the sender
     * @param senderName  the display name of the sender
     * @param recipientId the user ID of the recipient, or {@code null} for group messages
     * @param roomId      the chat room ID
     * @param text        the message text
     */
    public ChatMessage(String senderId, String senderName, String recipientId,
                       String roomId, String text) {
        this(UUID.randomUUID().toString(), senderId, senderName, recipientId,
                roomId, text, System.currentTimeMillis(), MessageStatus.SENDING);
    }

    /**
     * Constructs a {@code ChatMessage} with all fields explicitly specified.
     * Use this constructor when reconstructing a message from storage or network data.
     *
     * @param messageId   the unique message identifier (UUID string)
     * @param senderId    the user ID of the sender
     * @param senderName  the display name of the sender
     * @param recipientId the user ID of the recipient, or {@code null} for group messages
     * @param roomId      the chat room ID
     * @param text        the message text
     * @param timestamp   the epoch millisecond creation timestamp
     * @param status      the current delivery status
     */
    public ChatMessage(String messageId, String senderId, String senderName,
                       String recipientId, String roomId, String text,
                       long timestamp, MessageStatus status) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.recipientId = recipientId;
        this.roomId = roomId;
        this.text = text;
        this.timestamp = timestamp;
        this.status = status;
    }

    /**
     * Returns the unique identifier for this message.
     *
     * @return the message ID (UUID string)
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Sets the unique identifier for this message.
     *
     * @param messageId the message ID (UUID string)
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * Returns the user ID of the sender.
     *
     * @return the sender's user ID
     */
    public String getSenderId() {
        return senderId;
    }

    /**
     * Sets the user ID of the sender.
     *
     * @param senderId the sender's user ID
     */
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    /**
     * Returns the display name of the sender.
     *
     * @return the sender's display name
     */
    public String getSenderName() {
        return senderName;
    }

    /**
     * Sets the display name of the sender.
     *
     * @param senderName the sender's display name
     */
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    /**
     * Returns the user ID of the recipient, or {@code null} if this is a group message.
     *
     * @return the recipient's user ID, or {@code null}
     */
    public String getRecipientId() {
        return recipientId;
    }

    /**
     * Sets the user ID of the recipient.
     *
     * @param recipientId the recipient's user ID, or {@code null} for group messages
     */
    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    /**
     * Returns the ID of the chat room this message belongs to.
     *
     * @return the room ID
     */
    public String getRoomId() {
        return roomId;
    }

    /**
     * Sets the ID of the chat room this message belongs to.
     *
     * @param roomId the room ID
     */
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    /**
     * Returns the textual content of this message.
     *
     * @return the message text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the textual content of this message.
     *
     * @param text the message text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Returns the epoch millisecond timestamp when this message was created.
     *
     * @return the creation timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the creation timestamp for this message.
     *
     * @param timestamp the epoch millisecond timestamp
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the current delivery status of this message.
     *
     * @return the message status
     */
    public MessageStatus getStatus() {
        return status;
    }

    /**
     * Sets the delivery status of this message.
     *
     * @param status the new message status
     */
    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    /**
     * Compares this message to another object for equality.
     * Two messages are considered equal if and only if they have the same {@code messageId}.
     *
     * @param o the object to compare with
     * @return {@code true} if the other object is a {@code ChatMessage} with the same messageId
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChatMessage that = (ChatMessage) o;
        return Objects.equals(messageId, that.messageId);
    }

    /**
     * Returns a hash code based on the {@code messageId}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }

    /**
     * Returns a string representation of this message, including all fields.
     *
     * @return a human-readable string describing this message
     */
    @Override
    public String toString() {
        return "ChatMessage{" +
                "messageId='" + messageId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", senderName='" + senderName + '\'' +
                ", recipientId='" + recipientId + '\'' +
                ", roomId='" + roomId + '\'' +
                ", text='" + text + '\'' +
                ", timestamp=" + timestamp +
                ", status=" + status +
                '}';
    }
}
