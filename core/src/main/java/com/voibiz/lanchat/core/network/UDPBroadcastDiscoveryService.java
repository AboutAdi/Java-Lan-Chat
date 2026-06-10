package com.voibiz.lanchat.core.network;

import com.voibiz.lanchat.core.model.Peer;
import com.voibiz.lanchat.core.model.User;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class UDPBroadcastDiscoveryService implements DiscoveryService {
    private static final int BROADCAST_PORT = 44444;
    private static final String BROADCAST_MAGIC = "LANCHAT_DISCOVERY:";

    private final User localUser;
    private final PeerRegistry registry;
    private int tcpPort;
    private DatagramSocket socket;
    private Thread listenerThread;
    private boolean running = false;

    public UDPBroadcastDiscoveryService(User localUser, PeerRegistry registry) {
        this.localUser = localUser;
        this.registry = registry;
    }

    private void log(String message) {
        String time = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date());
        System.out.println("[" + time + "] [UDPBroadcast] " + message);
    }

    @Override
    public void setPort(int port) {
        this.tcpPort = port;
    }

    @Override
    public void start() throws IOException {
        running = true;
        try {
            socket = new DatagramSocket(BROADCAST_PORT);
            socket.setBroadcast(true);
            log("Listening for UDP broadcasts on port " + BROADCAST_PORT);
        } catch (BindException e) {
            // Port might be in use if multiple instances run on same machine, try random port for listening
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            log("Port " + BROADCAST_PORT + " in use, listening on random port " + socket.getLocalPort());
        }

        listenerThread = new Thread(this::listenLoop, "UDP-Discovery-Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        broadcastPresence();
    }

    @Override
    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    @Override
    public void silentRefresh() {
        broadcastPresence();
    }

    private void broadcastPresence() {
        if (socket == null || socket.isClosed()) return;
        new Thread(() -> {
            try {
                String payload = BROADCAST_MAGIC + localUser.getUserId() + ":" + localUser.getDisplayName() + ":" + tcpPort;
                byte[] data = payload.getBytes(StandardCharsets.UTF_8);
                
                boolean sent = false;
                java.util.Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                        continue;
                    }
                    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                        InetAddress broadcast = interfaceAddress.getBroadcast();
                        if (broadcast == null) {
                            continue;
                        }
                        try {
                            DatagramPacket packet = new DatagramPacket(data, data.length, broadcast, BROADCAST_PORT);
                            socket.send(packet);
                            sent = true;
                        } catch (Exception ignored) {}
                    }
                }
                
                // Fallback to global broadcast if no specific interfaces found
                if (!sent) {
                    DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), BROADCAST_PORT);
                    socket.send(packet);
                }
                
                log("Broadcasted presence: " + localUser.getUserId() + " on TCP port " + tcpPort);
            } catch (Exception e) {
                log("Failed to broadcast presence: " + e.getMessage());
            }
        }).start();
    }

    private void listenLoop() {
        byte[] buffer = new byte[1024];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                if (message.startsWith(BROADCAST_MAGIC)) {
                    String[] parts = message.substring(BROADCAST_MAGIC.length()).split(":");
                    if (parts.length >= 3) {
                        String peerId = parts[0];
                        String displayName = parts[1];
                        int port = Integer.parseInt(parts[2]);
                        String ip = packet.getAddress().getHostAddress();

                        if (!peerId.equals(localUser.getUserId())) {
                            log("Discovered peer via UDP: " + peerId + " at " + ip + ":" + port);
                            Peer peer = new Peer(peerId, displayName, ip, port, Peer.PeerStatus.ONLINE);
                            registry.addOrUpdatePeer(peer);
                        }
                    }
                }
            } catch (Exception e) {
                if (running) {
                    log("Listen error: " + e.getMessage());
                }
            }
        }
    }
}
