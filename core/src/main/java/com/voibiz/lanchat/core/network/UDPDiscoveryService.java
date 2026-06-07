package com.voibiz.lanchat.core.network;

import com.voibiz.lanchat.core.model.Peer;
import com.voibiz.lanchat.core.model.User;
import com.voibiz.lanchat.core.protocol.MessageType;
import com.voibiz.lanchat.core.protocol.ProtocolConstants;
import com.voibiz.lanchat.core.protocol.ProtocolMessage;
import com.voibiz.lanchat.core.serialization.MessageSerializer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/**
 * UDP-based peer discovery service for the LAN chat application.
 *
 * <p>This service uses UDP broadcast to announce the local user's presence
 * on the network and listens for announcements from other peers. It manages
 * two daemon threads:</p>
 * <ol>
 *   <li><b>Broadcaster thread</b> — periodically sends {@code DISCOVERY_ANNOUNCE}
 *       messages via UDP broadcast and evicts stale peers from the registry.</li>
 *   <li><b>Listener thread</b> — binds to the discovery port and processes
 *       incoming {@code DISCOVERY_ANNOUNCE}, {@code DISCOVERY_RESPONSE}, and
 *       {@code DISCOVERY_GOODBYE} messages.</li>
 * </ol>
 *
 * <p>Messages from the local user are ignored to prevent self-discovery.</p>
 *
 * @author Adi
 */
public class UDPDiscoveryService {

    /** Maximum size of a UDP datagram payload in bytes. */
    private static final int BUFFER_SIZE = 4096;

    /** The local user whose presence is being announced. */
    private final User localUser;

    /** The registry of known peers on the network. */
    private final PeerRegistry registry;

    /** The UDP socket used for both sending and receiving discovery messages. */
    private DatagramSocket socket;

    /** Flag indicating whether the service is actively running. */
    private volatile boolean running;

    /**
     * Constructs a new {@code UDPDiscoveryService}.
     *
     * @param localUser the local user to announce on the network; must not be {@code null}
     * @param registry  the peer registry to update when peers are discovered, updated, or lost;
     *                  must not be {@code null}
     */
    public UDPDiscoveryService(User localUser, PeerRegistry registry) {
        this.localUser = localUser;
        this.registry = registry;
    }

    /**
     * Starts the discovery service by launching the broadcaster and listener
     * daemon threads.
     *
     * <p>The broadcaster thread sends a {@code DISCOVERY_ANNOUNCE} message every
     * {@link ProtocolConstants#HEARTBEAT_INTERVAL_MS} milliseconds and evicts
     * stale peers from the registry. The listener thread binds to
     * {@link ProtocolConstants#DISCOVERY_PORT} and processes incoming packets.</p>
     *
     * @throws SocketException if the UDP socket cannot be bound to the discovery port
     */
    public void start() throws SocketException {
        socket = new DatagramSocket(ProtocolConstants.DISCOVERY_PORT);
        socket.setBroadcast(true);
        running = true;

        Thread broadcasterThread = new Thread(this::runBroadcaster, "UDP-Discovery-Broadcaster");
        broadcasterThread.setDaemon(true);
        broadcasterThread.start();

        Thread listenerThread = new Thread(this::runListener, "UDP-Discovery-Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Stops the discovery service by setting the running flag to {@code false}
     * and closing the UDP socket. Both the broadcaster and listener threads
     * will terminate shortly after.
     */
    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * Sends a {@code DISCOVERY_GOODBYE} broadcast to inform all peers on the
     * network that this user is leaving. This should be called before the
     * application shuts down for a graceful disconnect.
     */
    public void sendGoodbye() {
        try {
            ProtocolMessage goodbyeMessage = createDiscoveryMessage(MessageType.DISCOVERY_GOODBYE);
            sendBroadcast(goodbyeMessage);
        } catch (IOException e) {
            System.err.println("[UDPDiscovery] Failed to send goodbye broadcast: " + e.getMessage());
        }
    }

    /**
     * Broadcaster thread loop. Sends periodic {@code DISCOVERY_ANNOUNCE}
     * broadcasts and evicts stale peers from the registry.
     */
    private void runBroadcaster() {
        while (running) {
            try {
                ProtocolMessage announceMessage = createDiscoveryMessage(MessageType.DISCOVERY_ANNOUNCE);
                sendBroadcast(announceMessage);

                registry.evictStalePeers(ProtocolConstants.PEER_TIMEOUT_MS);

                Thread.sleep(ProtocolConstants.HEARTBEAT_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                if (running) {
                    System.err.println("[UDPDiscovery] Broadcaster error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Listener thread loop. Receives incoming UDP packets and processes
     * discovery messages.
     */
    private void runListener() {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String json = new String(packet.getData(), packet.getOffset(),
                        packet.getLength(), StandardCharsets.UTF_8);
                ProtocolMessage message = MessageSerializer.deserialize(json);

                if (message == null || message.getSender() == null) {
                    continue;
                }

                // Ignore messages from self
                String senderId = message.getSender().getUserId();
                if (localUser.getUserId().equals(senderId)) {
                    continue;
                }

                handleDiscoveryMessage(message, packet.getAddress());
            } catch (SocketException e) {
                if (running) {
                    System.err.println("[UDPDiscovery] Listener socket error: " + e.getMessage());
                }
                // Socket closed — exit loop
                break;
            } catch (IOException e) {
                if (running) {
                    System.err.println("[UDPDiscovery] Listener I/O error: " + e.getMessage());
                }
            } catch (Exception e) {
                if (running) {
                    System.err.println("[UDPDiscovery] Listener unexpected error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Handles a received discovery message based on its type.
     *
     * @param message       the received protocol message
     * @param senderAddress the IP address of the sender
     */
    private void handleDiscoveryMessage(ProtocolMessage message, InetAddress senderAddress) {
        MessageType type = message.getType();
        User sender = message.getSender();

        if (type == MessageType.DISCOVERY_ANNOUNCE || type == MessageType.DISCOVERY_RESPONSE) {
            Peer peer = new Peer(
                    sender.getUserId(),
                    sender.getDisplayName(),
                    senderAddress.getHostAddress(),
                    sender.getPort(),
                    Peer.PeerStatus.ONLINE
            );

            boolean isNewPeer = registry.getPeer(sender.getUserId()) == null;
            registry.addOrUpdatePeer(peer);

            // If it's an ANNOUNCE from a new peer, send a RESPONSE back via unicast
            if (type == MessageType.DISCOVERY_ANNOUNCE && isNewPeer) {
                try {
                    ProtocolMessage response = createDiscoveryMessage(MessageType.DISCOVERY_RESPONSE);
                    sendUnicast(response, senderAddress);
                } catch (IOException e) {
                    System.err.println("[UDPDiscovery] Failed to send response to "
                            + senderAddress.getHostAddress() + ": " + e.getMessage());
                }
            }
        } else if (type == MessageType.DISCOVERY_GOODBYE) {
            registry.removePeer(sender.getUserId());
        }
    }

    /**
     * Creates a discovery {@link ProtocolMessage} with the local user as sender.
     *
     * @param type the message type (e.g., DISCOVERY_ANNOUNCE, DISCOVERY_RESPONSE, DISCOVERY_GOODBYE)
     * @return a new {@link ProtocolMessage} ready to be serialized and sent
     */
    private ProtocolMessage createDiscoveryMessage(MessageType type) {
        return ProtocolMessage.create(type, localUser, null);
    }

    /**
     * Sends a protocol message as a UDP broadcast to all devices on the LAN.
     *
     * @param message the message to broadcast
     * @throws IOException if the broadcast fails
     */
    private void sendBroadcast(ProtocolMessage message) throws IOException {
        String json = MessageSerializer.serialize(message);
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
        DatagramPacket packet = new DatagramPacket(
                data, data.length, broadcastAddress, ProtocolConstants.DISCOVERY_PORT);
        socket.send(packet);
    }

    /**
     * Sends a protocol message as a UDP unicast to a specific address.
     *
     * @param message the message to send
     * @param address the target IP address
     * @throws IOException if the send fails
     */
    private void sendUnicast(ProtocolMessage message, InetAddress address) throws IOException {
        String json = MessageSerializer.serialize(message);
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(
                data, data.length, address, ProtocolConstants.DISCOVERY_PORT);
        socket.send(packet);
    }
}
