package com.voibiz.lanchat.core.network;

import com.voibiz.lanchat.core.model.Peer;
import com.voibiz.lanchat.core.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JmDNSDiscoveryTest {

    private JmDNSDiscoveryService service1;
    private JmDNSDiscoveryService service2;
    private PeerRegistry registry1;
    private PeerRegistry registry2;

    @BeforeEach
    void setup() throws Exception {
        User u1 = new User(UUID.randomUUID().toString(), "Peer1", "127.0.0.1", 50506);
        User u2 = new User(UUID.randomUUID().toString(), "Peer2", "127.0.0.1", 50507);
        registry1 = new PeerRegistry();
        registry2 = new PeerRegistry();
        
        service1 = new JmDNSDiscoveryService(u1, registry1);
        service2 = new JmDNSDiscoveryService(u2, registry2);
    }

    @AfterEach
    void tearDown() {
        service1.stop();
        service2.stop();
    }

    @Test
    void testDiscovery() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        registry1.setListener(new DiscoveryListener() {
            @Override
            public void onPeerDiscovered(Peer peer) {
                if (peer.getDisplayName().equals("Peer2")) {
                    latch.countDown();
                }
            }

            @Override
            public void onPeerUpdated(Peer peer) {
                if (peer.getDisplayName().equals("Peer2")) {
                    latch.countDown();
                }
            }

            @Override
            public void onPeerLost(Peer peer) { }
        });

        service1.start();
        service2.start();

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Peer 2 should be discovered by Peer 1");
    }
}
