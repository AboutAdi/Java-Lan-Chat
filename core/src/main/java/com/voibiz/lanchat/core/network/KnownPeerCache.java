package com.voibiz.lanchat.core.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.voibiz.lanchat.core.model.Peer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistently stores known peers to bypass mDNS discovery delay on startup.
 */
public class KnownPeerCache {
    private final File storageFile;
    private final Gson gson = new Gson();
    private final ConcurrentHashMap<String, Peer> knownPeers = new ConcurrentHashMap<>();

    public KnownPeerCache(File storageFile) {
        this.storageFile = storageFile;
        load();
    }

    private void load() {
        if (!storageFile.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(storageFile)) {
            java.lang.reflect.Type type = new TypeToken<List<Peer>>(){}.getType();
            List<Peer> peers = gson.fromJson(reader, type);
            if (peers != null) {
                for (Peer p : peers) {
                    knownPeers.put(p.getUserId(), p);
                }
            }
        } catch (Exception e) {
            System.err.println("[KnownPeerCache] Failed to load known peers: " + e.getMessage());
        }
    }

    private synchronized void save() {
        try (FileWriter writer = new FileWriter(storageFile)) {
            List<Peer> list = new ArrayList<>(knownPeers.values());
            gson.toJson(list, writer);
        } catch (IOException e) {
            System.err.println("[KnownPeerCache] Failed to save known peers: " + e.getMessage());
        }
    }

    public void addPeer(Peer peer) {
        if (peer == null || peer.getIpAddress() == null || peer.getIpAddress().isEmpty()) {
            return;
        }
        knownPeers.put(peer.getUserId(), peer);
        save();
    }

    public List<Peer> getKnownPeers() {
        return new ArrayList<>(knownPeers.values());
    }

    public synchronized void clear() {
        knownPeers.clear();
        if (storageFile.exists()) {
            storageFile.delete();
        }
    }
}
