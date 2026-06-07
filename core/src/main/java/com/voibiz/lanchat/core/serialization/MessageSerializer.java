package com.voibiz.lanchat.core.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.voibiz.lanchat.core.protocol.ProtocolMessage;

/**
 * Utility class for serializing and deserializing {@link ProtocolMessage} instances
 * to and from single-line JSON strings using Gson.
 *
 * <p>This class is not instantiable. All methods are static and thread-safe,
 * backed by a single shared {@link Gson} instance.</p>
 *
 * @author Adi
 */
public final class MessageSerializer {

    /** Shared Gson instance used for all serialization operations. */
    private static final Gson GSON = new GsonBuilder().create();

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws AssertionError always, if called via reflection
     */
    private MessageSerializer() {
        throw new AssertionError("MessageSerializer is a utility class and cannot be instantiated.");
    }

    /**
     * Serializes a {@link ProtocolMessage} into a single-line JSON string.
     *
     * @param msg the protocol message to serialize; must not be {@code null}
     * @return a single-line JSON string representation of the message
     */
    public static String serialize(ProtocolMessage msg) {
        return GSON.toJson(msg);
    }

    /**
     * Deserializes a JSON string into a {@link ProtocolMessage}.
     *
     * @param json the JSON string to deserialize; must not be {@code null}
     * @return the deserialized {@link ProtocolMessage}
     */
    public static ProtocolMessage deserialize(String json) {
        return GSON.fromJson(json, ProtocolMessage.class);
    }

    /**
     * Extracts a typed field from the payload {@link JsonObject} of a {@link ProtocolMessage}.
     *
     * <p>This is a convenience helper for pulling individual fields out of the
     * protocol message's free-form payload without manually interacting with
     * the {@link JsonObject} API.</p>
     *
     * @param <T>   the expected type of the field value
     * @param msg   the protocol message whose payload to inspect; must not be {@code null}
     * @param field the name of the field to extract from the payload
     * @param clazz the {@link Class} object representing type {@code T}
     * @return the field value deserialized as type {@code T}, or {@code null}
     *         if the payload is {@code null} or the field is absent
     */
    public static <T> T extractPayloadField(ProtocolMessage msg, String field, Class<T> clazz) {
        JsonObject payload = msg.getPayload();
        if (payload == null || !payload.has(field)) {
            return null;
        }
        return GSON.fromJson(payload.get(field), clazz);
    }
}
