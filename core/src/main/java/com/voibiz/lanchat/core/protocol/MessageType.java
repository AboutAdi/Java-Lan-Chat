package com.voibiz.lanchat.core.protocol;

/**
 * Defines all message types in the LAN Chat protocol.
 *
 * <p>Message types are grouped by their transport layer and functional category:</p>
 * <ul>
 *   <li><strong>Discovery (UDP)</strong> — peer announcement, response, and goodbye</li>
 *   <li><strong>Chat (TCP)</strong> — chat messages and acknowledgements</li>
 *   <li><strong>File Transfer (TCP)</strong> — file transfer negotiation</li>
 *   <li><strong>Status (TCP/UDP)</strong> — typing indicators and presence updates</li>
 * </ul>
 */
public enum MessageType {

    // ── Discovery (UDP) ─────────────────────────────────────────────────

    /** Broadcast announcement of a peer joining the network. */
    DISCOVERY_ANNOUNCE,

    /** Unicast response to a discovery announcement. */
    DISCOVERY_RESPONSE,

    /** Notification that a peer is leaving the network. */
    DISCOVERY_GOODBYE,

    // ── Handshake (TCP) ─────────────────────────────────────────────────

    /** Initial handshake message sent upon TCP connection establishment. */
    HELLO,

    // ── Chat (TCP) ──────────────────────────────────────────────────────

    /** A text chat message sent between peers. */
    CHAT_MESSAGE,

    /** Acknowledgement that a chat message was received. */
    CHAT_ACK,

    // ── File Transfer (TCP) ─────────────────────────────────────────────

    /** Request to initiate a file transfer. */
    FILE_TRANSFER_REQUEST,

    /** Acceptance of a pending file transfer request. */
    FILE_TRANSFER_ACCEPT,

    /** Rejection of a pending file transfer request. */
    FILE_TRANSFER_REJECT,

    // ── Status (TCP/UDP) ────────────────────────────────────────────────

    /** Indicator that a peer is currently typing. */
    TYPING_INDICATOR,

    /** Update to a peer's online/away/busy presence status. */
    PRESENCE_UPDATE,

    // ── Heartbeat (TCP) ─────────────────────────────────────────────────

    /** Proactive ping to check if connection is alive. */
    HEARTBEAT_PING,

    /** Response to a ping to confirm connection is alive. */
    HEARTBEAT_PONG,

    // ── Message Receipts ────────────────────────────────────────────────

    /** Sent automatically when a message is successfully received. */
    MESSAGE_DELIVERED,

    /** Sent when the user views the message. */
    MESSAGE_READ
}
