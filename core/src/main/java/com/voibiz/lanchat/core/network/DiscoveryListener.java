package com.voibiz.lanchat.core.network;

import com.voibiz.lanchat.core.model.Peer;

/**
 * Listener interface for peer discovery events on the local network.
 *
 * <p>Implementations receive callbacks when peers are discovered, updated, or
 * lost via the UDP discovery mechanism. All methods may be invoked from
 * background networking threads; implementations must handle thread safety
 * if interacting with UI or shared state.</p>
 *
 * @author Adi
 */
public interface DiscoveryListener {

    /**
     * Called when a new peer is discovered on the network for the first time.
     *
     * @param peer the newly discovered peer; never {@code null}
     */
    void onPeerDiscovered(Peer peer);

    /**
     * Called when an already-known peer sends an updated heartbeat or status change.
     *
     * @param peer the updated peer with refreshed fields (e.g., lastSeen); never {@code null}
     */
    void onPeerUpdated(Peer peer);

    /**
     * Called when a peer is no longer reachable — either by graceful goodbye
     * or by stale-peer eviction after a heartbeat timeout.
     *
     * @param peer the peer that was lost; never {@code null}
     */
    void onPeerLost(Peer peer);
}
