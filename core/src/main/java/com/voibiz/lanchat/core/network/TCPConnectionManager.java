package com.voibiz.lanchat.core.network;

import com.voibiz.lanchat.core.model.User;
import com.voibiz.lanchat.core.protocol.MessageType;
import com.voibiz.lanchat.core.protocol.ProtocolMessage;
import com.voibiz.lanchat.core.serialization.MessageSerializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages TCP connections to and from peers for reliable message delivery.
 *
 * <p>This class maintains a {@link ServerSocket} that accepts incoming peer
 * connections and a {@link ConcurrentHashMap} of active connections keyed by
 * peer user ID. Messages are sent as single-line JSON strings terminated by
 * a newline character, enabling simple {@link BufferedReader#readLine()} parsing
 * on the receiving end.</p>
 *
 * <p>All reader and acceptor threads are created as daemon threads so they do
 * not prevent JVM shutdown.</p>
 *
 * @author Adi
 */
public class TCPConnectionManager {

    /** The local user identity, used to identify outgoing connections. */
    private final User localUser;

    /** The handler that receives TCP message and connection events. */
    private final TCPMessageHandler handler;

    /** The port on which the server socket listens for incoming connections. */
    private int port;

    /** The server socket accepting incoming peer connections. */
    private ServerSocket serverSocket;

    /** Map of peerId → active connection info. */
    private final ConcurrentHashMap<String, ConnectionInfo> connections = new ConcurrentHashMap<>();

    /** Scheduler for the heartbeat mechanism. */
    private ScheduledExecutorService heartbeatScheduler;

    private void log(String message) {
        String time = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date());
        System.out.println("[" + time + "] " + message);
    }

    /**
     * Constructs a new {@code TCPConnectionManager}.
     *
     * @param localUser the local user identity; must not be {@code null}
     * @param handler   the handler for incoming messages and connection events;
     *                  must not be {@code null}
     */
    public TCPConnectionManager(User localUser, TCPMessageHandler handler) {
        this.localUser = localUser;
        this.handler = handler;
        this.port = 50505; // Default port
    }

    /**
     * Starts the TCP server by opening a {@link ServerSocket} and launching
     * a daemon acceptor thread that listens for incoming peer connections.
     *
     * <p>For each accepted connection, the first line is read to identify the
     * peer (via the sender field of the initial {@link ProtocolMessage}), then
     * a reader thread is spawned for ongoing message reception.</p>
     *
     * @throws IOException if the server socket cannot be created
     */
    public void start() throws IOException {
        int maxRetries = 100;
        int currentPort = port;
        boolean bound = false;
        while (!bound && currentPort < port + maxRetries) {
            try {
                serverSocket = new ServerSocket(currentPort);
                bound = true;
            } catch (java.net.BindException e) {
                currentPort++;
            }
        }
        if (!bound) {
            throw new IOException("Could not bind to any port in range " + port + "-" + (port + maxRetries));
        }
        this.port = serverSocket.getLocalPort();
        log("[TCPManager] Server listening on port: " + this.port);

        Thread acceptorThread = new Thread(this::runAcceptor, "TCP-Connection-Acceptor");
        acceptorThread.setDaemon(true);
        acceptorThread.start();

        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TCP-Heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatScheduler.scheduleAtFixedRate(this::checkHeartbeats, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * Returns the port the server is listening on.
     * @return the listening port
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Sends a {@link ProtocolMessage} to a specific peer.
     *
     * <p>If an active connection to the peer exists, the message is serialized
     * and sent immediately. If no connection exists, this method logs an error;
     * callers should use {@link #connectToPeer(String, String, int)} first.</p>
     *
     * @param peerId  the unique user ID of the target peer; must not be {@code null}
     * @param message the protocol message to send; must not be {@code null}
     */
    public void sendMessage(String peerId, ProtocolMessage message) {
        ConnectionInfo info = connections.get(peerId);
        if (info != null) {
            String json = MessageSerializer.serialize(message);
            info.writer.println(json);
            log("[TCPManager] Sent message to peer " + peerId + " of type " + message.getType());
        } else {
            System.err.println("[TCPManager] No active connection to peer: " + peerId
                    + ". Use connectToPeer() first.");
        }
    }

    /**
     * Establishes an outgoing TCP connection to a peer.
     *
     * <p>Creates a new {@link Socket} to the specified IP and port, stores the
     * connection, sends an initial identification message so the remote peer
     * knows who connected, and starts a reader thread for incoming messages.</p>
     *
     * @param peerId the unique user ID of the peer to connect to; must not be {@code null}
     * @param ip     the IP address of the peer; must not be {@code null}
     * @param port   the TCP port of the peer
     * @throws IOException if the connection cannot be established
     */
    public void connectToPeer(String peerId, String ip, int port) throws IOException {
        // Don't create a duplicate connection
        if (connections.containsKey(peerId)) {
            return;
        }

        log("[TCPManager] Attempting to connect to peer " + peerId + " at " + ip + ":" + port);
        Socket socket = new Socket(ip, port);
        PrintWriter writer = new PrintWriter(
                new java.io.OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        ConnectionInfo info = new ConnectionInfo(socket, writer, peerId);
        connections.put(peerId, info);

        log("[TCPManager] Successfully connected to peer " + peerId);

        // Send an initial identification message so the remote peer knows who we are
        ProtocolMessage identMessage = ProtocolMessage.create(MessageType.HELLO, localUser, null);
        writer.println(MessageSerializer.serialize(identMessage));
        log("[TCPManager] Handshake sent to: " + peerId);

        handler.onPeerConnected(peerId);
        startReaderThread(peerId, socket);
    }

    /**
     * Disconnects from a specific peer, closing the socket and removing the
     * connection from the active connections map.
     *
     * @param peerId the unique user ID of the peer to disconnect from;
     *               must not be {@code null}
     */
    public void disconnectPeer(String peerId) {
        ConnectionInfo info = connections.remove(peerId);
        if (info != null) {
            try {
                if (!info.socket.isClosed()) {
                    info.socket.close();
                }
            } catch (IOException e) {
                System.err.println("[TCPManager] Error closing socket for peer " + peerId
                        + ": " + e.getMessage());
            }
            handler.onPeerDisconnected(peerId);
        }
    }

    public int getLocalPort() {
        return serverSocket != null ? serverSocket.getLocalPort() : port;
    }

    /**
     * Stops the TCP connection manager by closing all active peer connections
     * and the server socket.
     */
    public void stop() {
        // Close all active connections
        for (String peerId : connections.keySet()) {
            ConnectionInfo info = connections.remove(peerId);
            if (info != null) {
                try {
                    if (!info.socket.isClosed()) {
                        info.socket.close();
                    }
                } catch (IOException e) {
                    System.err.println("[TCPManager] Error closing connection to peer "
                            + peerId + ": " + e.getMessage());
                }
            }
        }

        // Close the server socket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("[TCPManager] Error closing server socket: " + e.getMessage());
            }
        }

        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
        }
    }

    /**
     * Acceptor loop that listens for incoming TCP connections on the server socket.
     *
     * <p>For each accepted connection, the first line is read to extract the
     * sender's identity from the initial {@link ProtocolMessage}. The connection
     * is then stored and a reader thread is started.</p>
     */
    private void runAcceptor() {
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();

                // Read the first line to identify the peer
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                String firstLine = reader.readLine();

                if (firstLine == null || firstLine.isEmpty()) {
                    clientSocket.close();
                    continue;
                }

                ProtocolMessage firstMessage = MessageSerializer.deserialize(firstLine);
                if (firstMessage == null || firstMessage.getSender() == null) {
                    clientSocket.close();
                    continue;
                }

                String peerId = firstMessage.getSender().getUserId();
                PrintWriter writer = new PrintWriter(
                        new java.io.OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);

                ConnectionInfo info = new ConnectionInfo(clientSocket, writer, peerId);
                connections.put(peerId, info);

                log("[TCPManager] Accepted incoming connection from peer: " + peerId + " (" + clientSocket.getInetAddress().getHostAddress() + ")");
                log("[TCPManager] Handshake received from: " + peerId);

                // Reply with our own HELLO
                ProtocolMessage identReply = ProtocolMessage.create(MessageType.HELLO, localUser, null);
                writer.println(MessageSerializer.serialize(identReply));

                handler.onPeerConnected(peerId);
                handler.onMessageReceived(firstMessage, peerId);

                // Start a reader thread for subsequent messages, reusing the
                // existing BufferedReader that already consumed the first line
                startReaderThreadWithReader(peerId, clientSocket, reader);

            } catch (SocketException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("[TCPManager] Acceptor socket error: " + e.getMessage());
                }
                break;
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("[TCPManager] Acceptor I/O error: " + e.getMessage());
                }
            } catch (Exception e) {
                System.err.println("[TCPManager] Acceptor unexpected error: " + e.getMessage());
            }
        }
    }

    /**
     * Starts a daemon reader thread for an outgoing connection.
     * Creates a new {@link BufferedReader} from the socket's input stream.
     *
     * @param peerId the peer's unique user ID
     * @param socket the connected socket to read from
     */
    private void startReaderThread(String peerId, Socket socket) {
        Thread readerThread = new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                readLoop(peerId, reader);
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.err.println("[TCPManager] Reader thread setup error for peer "
                            + peerId + ": " + e.getMessage());
                }
                disconnectPeer(peerId);
            }
        }, "TCP-Reader-" + peerId);
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * Starts a daemon reader thread for an accepted (incoming) connection,
     * reusing an existing {@link BufferedReader} that has already consumed
     * the first identification line.
     *
     * @param peerId the peer's unique user ID
     * @param socket the connected socket
     * @param reader the buffered reader already associated with the socket's input stream
     */
    private void startReaderThreadWithReader(String peerId, Socket socket, BufferedReader reader) {
        Thread readerThread = new Thread(() -> readLoop(peerId, reader),
                "TCP-Reader-" + peerId);
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * Continuously reads lines from the given reader, deserializes each line
     * into a {@link ProtocolMessage}, and dispatches to the handler.
     *
     * <p>On {@link IOException} (e.g., connection reset), the peer is
     * disconnected via {@link #disconnectPeer(String)}.</p>
     *
     * @param peerId the peer's unique user ID
     * @param reader the buffered reader to read lines from
     */
    private void readLoop(String peerId, BufferedReader reader) {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                try {
                    ProtocolMessage message = MessageSerializer.deserialize(line);
                    if (message != null) {
                        ConnectionInfo info = connections.get(peerId);
                        if (info != null) {
                            info.lastActivityTime = System.currentTimeMillis();
                        }

                        if (message.getType() == MessageType.HEARTBEAT_PING) {
                            // Reply with PONG
                            ProtocolMessage pong = ProtocolMessage.create(MessageType.HEARTBEAT_PONG, localUser, null);
                            if (info != null) {
                                info.writer.println(MessageSerializer.serialize(pong));
                            }
                        } else if (message.getType() == MessageType.HEARTBEAT_PONG) {
                            // Already updated lastActivityTime, do nothing else
                        } else if (message.getType() == MessageType.HELLO) {
                            log("[TCPManager] Received HELLO from peer " + peerId);
                            handler.onMessageReceived(message, peerId);
                        } else {
                            log("[TCPManager] Received message from peer " + peerId + " of type " + message.getType());
                            handler.onMessageReceived(message, peerId);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[TCPManager] Failed to deserialize message from peer "
                            + peerId + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            // Connection lost
            if (connections.containsKey(peerId)) {
                System.err.println("[TCPManager] Connection lost to peer " + peerId
                        + ": " + e.getMessage());
            }
        }
        disconnectPeer(peerId);
    }

    private void checkHeartbeats() {
        long now = System.currentTimeMillis();
        for (String peerId : connections.keySet()) {
            ConnectionInfo info = connections.get(peerId);
            if (info != null) {
                if (now - info.lastActivityTime > 25000) {
                    System.err.println("[TCPManager] Peer " + peerId + " heartbeat timeout. Disconnecting.");
                    disconnectPeer(peerId);
                } else {
                    ProtocolMessage ping = ProtocolMessage.create(MessageType.HEARTBEAT_PING, localUser, null);
                    info.writer.println(MessageSerializer.serialize(ping));
                }
            }
        }
    }

    /**
     * Holds the state for a single TCP connection to a peer.
     */
    private static class ConnectionInfo {

        /** The underlying TCP socket. */
        final Socket socket;

        /** The writer for sending JSON lines to the peer (autoFlush enabled). */
        final PrintWriter writer;

        /** The unique user ID of the connected peer. */
        final String peerId;

        /** Timestamp of the last received message from this peer. */
        volatile long lastActivityTime;

        /**
         * Constructs a new {@code ConnectionInfo}.
         *
         * @param socket the TCP socket; must not be {@code null}
         * @param writer the print writer for the socket's output stream; must not be {@code null}
         * @param peerId the peer's unique user ID; must not be {@code null}
         */
        ConnectionInfo(Socket socket, PrintWriter writer, String peerId) {
            this.socket = socket;
            this.writer = writer;
            this.peerId = peerId;
            this.lastActivityTime = System.currentTimeMillis();
        }
    }
}
