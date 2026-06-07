package com.voibiz.lanchat.core.model;

/**
 * Represents a discovered peer on the local network.
 *
 * <p>{@code Peer} extends {@link User} with presence-tracking fields: a
 * {@link PeerStatus} indicating the peer's current availability and a
 * {@code lastSeenTimestamp} recording when the peer was last heard from.
 * Together these allow the application to detect stale (unresponsive) peers
 * and update the UI accordingly.</p>
 *
 * @author Adi
 * @see User
 */
public class Peer extends User {

    /**
     * Enumerates the possible presence states of a peer on the network.
     */
    public enum PeerStatus {
        /** The peer is online and available. */
        ONLINE,
        /** The peer is online but temporarily away. */
        AWAY,
        /** The peer is online but busy and may not respond promptly. */
        BUSY,
        /** The peer is offline or unreachable. */
        OFFLINE
    }

    /** Current presence status of this peer. */
    private PeerStatus status;

    /** Epoch millisecond timestamp of the last heartbeat or message from this peer. */
    private long lastSeenTimestamp;

    /**
     * Constructs a new {@code Peer} with the specified identity, network details,
     * and initial status. The {@code lastSeenTimestamp} is initialised to the
     * current system time.
     *
     * @param userId      the unique identifier (UUID string)
     * @param displayName the human-readable display name
     * @param ipAddress   the IP address on the local network
     * @param port        the listening port number
     * @param status      the initial presence status
     */
    public Peer(String userId, String displayName, String ipAddress, int port, PeerStatus status) {
        super(userId, displayName, ipAddress, port);
        this.status = status;
        this.lastSeenTimestamp = System.currentTimeMillis();
    }

    /**
     * Returns the current presence status of this peer.
     *
     * @return the peer status
     */
    public PeerStatus getStatus() {
        return status;
    }

    /**
     * Sets the presence status of this peer.
     *
     * @param status the new peer status
     */
    public void setStatus(PeerStatus status) {
        this.status = status;
    }

    /**
     * Returns the epoch millisecond timestamp when this peer was last seen.
     *
     * @return the last-seen timestamp
     */
    public long getLastSeenTimestamp() {
        return lastSeenTimestamp;
    }

    /**
     * Sets the epoch millisecond timestamp when this peer was last seen.
     *
     * @param lastSeenTimestamp the last-seen timestamp
     */
    public void setLastSeenTimestamp(long lastSeenTimestamp) {
        this.lastSeenTimestamp = lastSeenTimestamp;
    }

    /**
     * Checks whether this peer should be considered stale (unresponsive).
     *
     * <p>A peer is stale if the elapsed time since its {@code lastSeenTimestamp}
     * exceeds the given {@code timeoutMs} threshold.</p>
     *
     * @param timeoutMs the timeout threshold in milliseconds
     * @return {@code true} if the peer has not been seen within the timeout window
     */
    public boolean isStale(long timeoutMs) {
        return System.currentTimeMillis() - lastSeenTimestamp > timeoutMs;
    }

    /**
     * Updates the {@code lastSeenTimestamp} to the current system time.
     * Call this method whenever a heartbeat or message is received from the peer.
     */
    public void updateLastSeen() {
        this.lastSeenTimestamp = System.currentTimeMillis();
    }

    /**
     * Returns a string representation of this peer, including inherited user fields,
     * status, and last-seen timestamp.
     *
     * @return a human-readable string describing this peer
     */
    @Override
    public String toString() {
        return "Peer{" +
                "userId='" + getUserId() + '\'' +
                ", displayName='" + getDisplayName() + '\'' +
                ", ipAddress='" + getIpAddress() + '\'' +
                ", port=" + getPort() +
                ", status=" + status +
                ", lastSeenTimestamp=" + lastSeenTimestamp +
                '}';
    }
}
