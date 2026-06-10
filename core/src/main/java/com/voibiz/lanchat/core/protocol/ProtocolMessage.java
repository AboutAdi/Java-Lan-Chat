package com.voibiz.lanchat.core.protocol;

import com.google.gson.JsonObject;
import com.voibiz.lanchat.core.model.User;

import java.util.UUID;

/**
 * Represents a single message exchanged over the LAN Chat protocol.
 *
 * <p>Every protocol message carries a {@link #version protocol version},
 * a {@link #type message type}, a unique {@link #messageId}, the
 * {@link #sender} who originated it, a {@link #timestamp} in epoch
 * milliseconds, and an optional JSON {@link #payload}.</p>
 *
 * <p>Use the factory method {@link #create(MessageType, User, JsonObject)}
 * for convenient construction with auto-generated ID and timestamp.</p>
 */
public class ProtocolMessage {

    /** Protocol version this message conforms to. */
    private final int version;

    /** The type of this protocol message. */
    private final MessageType type;

    /** Unique identifier for this message (UUID string). */
    private final String messageId;

    /** The user who sent this message. */
    private final User sender;

    /** Creation timestamp in epoch milliseconds. */
    private final long timestamp;

    /** Arbitrary JSON payload associated with this message; may be {@code null}. */
    private final JsonObject payload;

    /** Default constructor required for Gson deserialization. */
    public ProtocolMessage() {
        this.version = 0;
        this.type = null;
        this.messageId = null;
        this.sender = null;
        this.timestamp = 0;
        this.payload = null;
    }

    /**
     * Constructs a {@code ProtocolMessage} with all fields specified explicitly.
     *
     * @param version   protocol version
     * @param type      message type, must not be {@code null}
     * @param messageId unique message identifier (UUID string)
     * @param sender    the user who sent this message, must not be {@code null}
     * @param timestamp creation time in epoch milliseconds
     * @param payload   optional JSON payload, may be {@code null}
     */
    public ProtocolMessage(int version,
                           MessageType type,
                           String messageId,
                           User sender,
                           long timestamp,
                           JsonObject payload) {
        this.version = version;
        this.type = type;
        this.messageId = messageId;
        this.sender = sender;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    /**
     * Factory method that creates a new {@code ProtocolMessage} with an
     * auto-generated UUID, the current system time, and the protocol
     * version defined in {@link ProtocolConstants#PROTOCOL_VERSION}.
     *
     * @param type    message type, must not be {@code null}
     * @param sender  the user who is sending this message, must not be {@code null}
     * @param payload optional JSON payload, may be {@code null}
     * @return a fully initialised {@code ProtocolMessage}
     */
    public static ProtocolMessage create(MessageType type, User sender, JsonObject payload) {
        return new ProtocolMessage(
                ProtocolConstants.PROTOCOL_VERSION,
                type,
                UUID.randomUUID().toString(),
                sender,
                System.currentTimeMillis(),
                payload
        );
    }

    // ── Getters ─────────────────────────────────────────────────────────

    /**
     * Returns the protocol version of this message.
     *
     * @return protocol version number
     */
    public int getVersion() {
        return version;
    }

    /**
     * Returns the type of this protocol message.
     *
     * @return the {@link MessageType}
     */
    public MessageType getType() {
        return type;
    }

    /**
     * Returns the unique identifier for this message.
     *
     * @return UUID string
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Returns the user who sent this message.
     *
     * @return the {@link User} sender
     */
    public User getSender() {
        return sender;
    }

    /**
     * Returns the creation timestamp in epoch milliseconds.
     *
     * @return timestamp in milliseconds since the Unix epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the optional JSON payload associated with this message.
     *
     * @return a {@link JsonObject}, or {@code null} if no payload was set
     */
    public JsonObject getPayload() {
        return payload;
    }

    // ── Object overrides ────────────────────────────────────────────────

    /**
     * Returns a human-readable string representation useful for debugging.
     *
     * @return debug string containing all field values
     */
    @Override
    public String toString() {
        return "ProtocolMessage{" +
                "version=" + version +
                ", type=" + type +
                ", messageId='" + messageId + '\'' +
                ", sender=" + sender +
                ", timestamp=" + timestamp +
                ", payload=" + payload +
                '}';
    }
}
