package com.voibiz.lanchat.core.network;

import com.voibiz.lanchat.core.model.Peer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of discovered peers on the local network.
 *
 * <p>Maintains a {@link ConcurrentHashMap} keyed by user ID, providing
 * concurrent add, update, remove, and query operations. When a
 * {@link DiscoveryListener} is registered, the registry fires appropriate
 * callbacks on peer state changes.</p>
 *
 * <p>Stale peers (those whose {@code lastSeenTimestamp} exceeds a given
 * timeout) can be evicted in bulk via {@link #evictStalePeers(long)}.</p>
 *
 * @author Adi
 */
public class PeerRegistry {

    /** Internal map of userId → Peer. */
    private final ConcurrentHashMap<String, Peer> peers = new ConcurrentHashMap<>();

    /** Optional listener for discovery events; may be {@code null}. */
    private DiscoveryListener listener;

    /**
     * Constructs a new, empty {@code PeerRegistry} with no listener.
     */
    public PeerRegistry() {
        // default — no listener
    }

    /**
     * Returns the currently registered {@link DiscoveryListener}, or {@code null}.
     *
     * @return the current listener, or {@code null} if none is set
     */
    public DiscoveryListener getListener() {
        return listener;
    }

    /**
     * Sets the {@link DiscoveryListener} to receive peer lifecycle callbacks.
     *
     * @param listener the listener to register, or {@code null} to unregister
     */
    public void setListener(DiscoveryListener listener) {
        this.listener = listener;
    }

    /**
     * Adds a new peer or updates an existing peer in the registry.
     *
     * <p>If the peer's {@code userId} is not yet present, the peer is inserted
     * and {@link DiscoveryListener#onPeerDiscovered(Peer)} is fired. If the
     * peer already exists, the existing entry's {@code lastSeenTimestamp} and
     * {@code status} fields are updated and
     * {@link DiscoveryListener#onPeerUpdated(Peer)} is fired.</p>
     *
     * @param peer the peer to add or update; must not be {@code null}
     */
    public void addOrUpdatePeer(Peer peer) {
        Peer existing = peers.get(peer.getUserId());
        if (existing == null) {
            peers.put(peer.getUserId(), peer);
            DiscoveryListener currentListener = listener;
            if (currentListener != null) {
                currentListener.onPeerDiscovered(peer);
            }
        } else {
            existing.updateLastSeen();
            existing.setStatus(peer.getStatus());
            if (peer.getDisplayName() != null) {
                existing.setDisplayName(peer.getDisplayName());
            }
            DiscoveryListener currentListener = listener;
            if (currentListener != null) {
                currentListener.onPeerUpdated(existing);
            }
        }
    }

    /**
     * Removes a peer from the registry by user ID.
     *
     * <p>If the peer was present, {@link DiscoveryListener#onPeerLost(Peer)}
     * is fired with the removed peer.</p>
     *
     * @param userId the unique user ID of the peer to remove; must not be {@code null}
     */
    public void removePeer(String userId) {
        Peer removed = peers.remove(userId);
        if (removed != null) {
            DiscoveryListener currentListener = listener;
            if (currentListener != null) {
                currentListener.onPeerLost(removed);
            }
        }
    }

    /**
     * Retrieves a peer by user ID.
     *
     * @param userId the unique user ID to look up; must not be {@code null}
     * @return the {@link Peer} if found, or {@code null} if no peer with that ID exists
     */
    public Peer getPeer(String userId) {
        return peers.get(userId);
    }

    /**
     * Returns a snapshot copy of all currently registered peers.
     *
     * <p>The returned list is a new {@link ArrayList} and can be modified
     * freely without affecting the registry.</p>
     *
     * @return a snapshot list of all peers; never {@code null}
     */
    public List<Peer> getAllPeers() {
        return new ArrayList<>(peers.values());
    }

    /**
     * Returns a snapshot of all peers whose status is not
     * {@link Peer.PeerStatus#OFFLINE}.
     *
     * @return a list of online (non-offline) peers; never {@code null}
     */
    public List<Peer> getOnlinePeers() {
        List<Peer> onlinePeers = new ArrayList<>();
        for (Peer peer : peers.values()) {
            if (peer.getStatus() != Peer.PeerStatus.OFFLINE) {
                onlinePeers.add(peer);
            }
        }
        return onlinePeers;
    }

    /**
     * Evicts all peers whose {@code lastSeenTimestamp} is older than
     * {@code System.currentTimeMillis() - timeoutMs}.
     *
     * <p>For each evicted peer, {@link DiscoveryListener#onPeerLost(Peer)}
     * is fired.</p>
     *
     * @param timeoutMs the staleness threshold in milliseconds; peers not seen
     *                  within this window are evicted
     * @return a list of peers that were evicted; never {@code null}
     */
    public List<Peer> evictStalePeers(long timeoutMs) {
        List<Peer> evicted = new ArrayList<>();
        for (Peer peer : peers.values()) {
            if (peer.isStale(timeoutMs)) {
                Peer removed = peers.remove(peer.getUserId());
                if (removed != null) {
                    evicted.add(removed);
                    DiscoveryListener currentListener = listener;
                    if (currentListener != null) {
                        currentListener.onPeerLost(removed);
                    }
                }
            }
        }
        return evicted;
    }
}
