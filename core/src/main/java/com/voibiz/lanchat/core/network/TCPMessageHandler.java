package com.voibiz.lanchat.core.network;

import com.voibiz.lanchat.core.protocol.ProtocolMessage;

/**
 * Handler interface for TCP messaging events.
 *
 * <p>Implementations receive callbacks when protocol messages arrive over
 * established TCP connections, when peers connect, and when peers disconnect.
 * All methods may be invoked from background reader threads; implementations
 * must handle thread safety if interacting with UI or shared state.</p>
 *
 * @author Adi
 */
public interface TCPMessageHandler {

    /**
     * Called when a complete protocol message is received over a TCP connection.
     *
     * @param message the deserialized protocol message; never {@code null}
     * @param peerId  the unique user ID of the peer that sent the message; never {@code null}
     */
    void onMessageReceived(ProtocolMessage message, String peerId);

    /**
     * Called when a TCP connection to a peer is successfully established,
     * either by accepting an incoming connection or by initiating an outgoing one.
     *
     * @param peerId the unique user ID of the connected peer; never {@code null}
     */
    void onPeerConnected(String peerId);

    /**
     * Called when a TCP connection to a peer is lost or explicitly closed.
     *
     * @param peerId the unique user ID of the disconnected peer; never {@code null}
     */
    void onPeerDisconnected(String peerId);
}
