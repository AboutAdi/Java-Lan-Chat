package com.voibiz.lanchat.core.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a user in the LAN chat application.
 *
 * <p>Each user is uniquely identified by a {@code userId}, which is a UUID string.
 * The user also carries network addressing information (IP address and port) used
 * for peer-to-peer communication on the local network.</p>
 *
 * <p>Equality and hash code are based solely on {@code userId}, so two {@code User}
 * instances with the same ID are considered equal regardless of other field values.</p>
 *
 * @author Adi
 */
public class User {

    /** Unique identifier for this user (UUID string). */
    private String userId;

    /** Human-readable display name shown in the chat UI. */
    private String displayName;

    /** IP address of the user on the local network. */
    private String ipAddress;

    /** Port number the user is listening on for incoming connections. */
    private int port;

    /**
     * Constructs a new {@code User} with all fields specified.
     *
     * @param userId      the unique identifier (UUID string)
     * @param displayName the human-readable display name
     * @param ipAddress   the IP address on the local network
     * @param port        the listening port number
     */
    public User(String userId, String displayName, String ipAddress, int port) {
        this.userId = userId;
        this.displayName = displayName;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    /**
     * Constructs a new {@code User} with an auto-generated UUID, an empty IP address,
     * and port set to {@code 0}. This is useful when creating a local user before
     * network details are known.
     *
     * @param displayName the human-readable display name
     */
    public User(String displayName) {
        this(UUID.randomUUID().toString(), displayName, "", 0);
    }

    /**
     * Returns the unique identifier for this user.
     *
     * @return the user ID (UUID string)
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the unique identifier for this user.
     *
     * @param userId the user ID (UUID string)
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Returns the display name of this user.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the display name of this user.
     *
     * @param displayName the display name
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the IP address of this user on the local network.
     *
     * @return the IP address
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Sets the IP address of this user on the local network.
     *
     * @param ipAddress the IP address
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Returns the port number this user is listening on.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port number this user listens on.
     *
     * @param port the port number
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Compares this user to another object for equality.
     * Two users are considered equal if and only if they have the same {@code userId}.
     *
     * @param o the object to compare with
     * @return {@code true} if the other object is a {@code User} with the same userId
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    /**
     * Returns a hash code based on the {@code userId}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    /**
     * Returns a string representation of this user, including all fields.
     *
     * @return a human-readable string describing this user
     */
    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", displayName='" + displayName + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                '}';
    }
}
