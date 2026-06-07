package com.voibiz.lanchat.core.protocol;

/**
 * Constants shared across the LAN Chat protocol layer.
 *
 * <p>This is a utility class and cannot be instantiated.</p>
 *
 * <h2>Port Assignments</h2>
 * <ul>
 *   <li>{@link #DISCOVERY_PORT} — UDP port used for peer discovery broadcasts</li>
 *   <li>{@link #DEFAULT_TCP_PORT} — default TCP port for direct peer-to-peer communication</li>
 * </ul>
 *
 * <h2>Timing</h2>
 * <ul>
 *   <li>{@link #HEARTBEAT_INTERVAL_MS} — interval between heartbeat broadcasts</li>
 *   <li>{@link #PEER_TIMEOUT_MS} — duration after which a silent peer is considered offline</li>
 * </ul>
 */
public final class ProtocolConstants {

    // ── Port configuration ──────────────────────────────────────────────

    /** UDP port used for broadcasting and receiving discovery messages. */
    public static final int DISCOVERY_PORT = 50505;

    /** Default TCP port for chat and file-transfer connections. */
    public static final int DEFAULT_TCP_PORT = 50506;

    // ── Timing ──────────────────────────────────────────────────────────

    /** Interval in milliseconds between heartbeat / keep-alive broadcasts (5 seconds). */
    public static final int HEARTBEAT_INTERVAL_MS = 5000;

    /** Time in milliseconds after which a peer with no heartbeat is considered offline (15 seconds). */
    public static final int PEER_TIMEOUT_MS = 15000;

    // ── Protocol metadata ───────────────────────────────────────────────

    /** Current version of the LAN Chat protocol. */
    public static final int PROTOCOL_VERSION = 1;

    /** Character encoding used for all protocol messages. */
    public static final String CHARSET = "UTF-8";

    // ── I/O ─────────────────────────────────────────────────────────────

    /** Default buffer size in bytes for reading and writing network data. */
    public static final int BUFFER_SIZE = 4096;

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private ProtocolConstants() {
        throw new UnsupportedOperationException("ProtocolConstants is a utility class and cannot be instantiated.");
    }
}
