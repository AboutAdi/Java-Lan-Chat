package com.voibiz.lanchat.core.network;

import java.io.IOException;

/**
 * Common interface for peer discovery services.
 */
public interface DiscoveryService {
    /**
     * Starts the discovery service.
     * @throws IOException if an error occurs while starting
     */
    void start() throws IOException;

    /**
     * Stops the discovery service and cleans up resources.
     */
    void stop();
    /**
     * Sets the local port that the TCP connection manager is listening on.
     */
    void setPort(int port);

    /**
     * Silently requests a network refresh without unregistering the local service.
     */
    void silentRefresh();
}
