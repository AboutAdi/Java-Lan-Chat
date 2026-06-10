package com.voibiz.lanchat.core.network;

import com.voibiz.lanchat.core.model.Peer;
import com.voibiz.lanchat.core.model.User;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;

public class JmDNSDiscoveryService implements DiscoveryService {
    public static final String SERVICE_TYPE = "_lanchat._tcp.local.";
    
    private final User localUser;
    private final PeerRegistry registry;
    private JmDNS jmdns;
    private int port = 50506;

    public JmDNSDiscoveryService(User localUser, PeerRegistry registry) {
        this.localUser = localUser;
        this.registry = registry;
    }

    private void log(String message) {
        String time = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date());
        System.out.println("[" + time + "] " + message);
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void start() throws IOException {
        InetAddress address = getLocalIpAddress();
        if (address == null) {
            address = InetAddress.getLocalHost();
        }
        log("[JmDNSDiscovery] Creating JmDNS instance on address: " + address.getHostAddress());
        jmdns = JmDNS.create(address);
        log("[JmDNSDiscovery] JmDNS instance created successfully.");

        ServiceInfo serviceInfo = ServiceInfo.create(
                SERVICE_TYPE,
                localUser.getUserId(),
                this.port,
                0, 0,
                "name=" + localUser.getDisplayName()
        );
        jmdns.registerService(serviceInfo);
        log("[JmDNSDiscovery] Registered local service '" + localUser.getUserId() + "' on port " + this.port + " at IP " + address.getHostAddress());

        jmdns.addServiceListener(SERVICE_TYPE, new ServiceListener() {
            @Override
            public void serviceAdded(ServiceEvent event) {
                if (event.getName().equals(localUser.getUserId())) return;
                log("[JmDNSDiscovery] Service found: " + event.getName() + ". Attempting to resolve...");
                new Thread(() -> jmdns.requestServiceInfo(event.getType(), event.getName(), 5000)).start();
            }

            @Override
            public void serviceRemoved(ServiceEvent event) {
                if (event.getName().equals(localUser.getUserId())) return;
                registry.removePeer(event.getName());
            }

            @Override
            public void serviceResolved(ServiceEvent event) {
                if (event.getName().equals(localUser.getUserId())) return;

                ServiceInfo info = event.getInfo();
                if (info != null) {
                    log("[JmDNSDiscovery] Service resolved: " + event.getName() + " on port " + info.getPort());
                    String displayName = info.getPropertyString("name");
                    if (displayName == null) displayName = event.getName();
                    
                    String[] ips = info.getHostAddresses();
                    if (ips.length > 0) {
                        Peer peer = new Peer(
                                event.getName(),
                                displayName,
                                ips[0],
                                info.getPort(),
                                Peer.PeerStatus.ONLINE
                        );
                        registry.addOrUpdatePeer(peer);
                    }
                }
            }
        });
    }

    @Override
    public void stop() {
        if (jmdns != null) {
            jmdns.unregisterAllServices();
            try {
                jmdns.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void silentRefresh() {
        if (jmdns != null) {
            new Thread(() -> {
                // Calling list() forces JmDNS to actively query the network for this type
                jmdns.list(SERVICE_TYPE, 1500);
            }).start();
        }
    }

    private InetAddress getLocalIpAddress() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> en = java.net.NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                java.net.NetworkInterface intf = en.nextElement();
                String name = intf.getName().toLowerCase();
                if (name.startsWith("docker") || name.startsWith("virbr") || name.startsWith("br-") || name.startsWith("veth") || name.startsWith("tun") || name.startsWith("tap")) {
                    continue;
                }
                
                java.util.Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements()) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof java.net.Inet4Address) {
                        return inetAddress;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
