package com.voibiz.lanchat.android.core;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import com.voibiz.lanchat.core.model.Peer;
import com.voibiz.lanchat.core.model.User;
import com.voibiz.lanchat.core.network.DiscoveryService;
import com.voibiz.lanchat.core.network.PeerRegistry;

public class NsdDiscoveryService implements DiscoveryService {
    public static final String SERVICE_TYPE = "_lanchat._tcp.";
    
    private final Context context;
    private final User localUser;
    private final PeerRegistry registry;
    private NsdManager nsdManager;
    private NsdManager.RegistrationListener registrationListener;
    private NsdManager.DiscoveryListener discoveryListener;
    private String registeredServiceName;
    private android.net.wifi.WifiManager.MulticastLock multicastLock;
    private int port = 50506;
    private final java.util.Queue<NsdServiceInfo> resolveQueue = new java.util.LinkedList<>();
    private boolean isResolving = false;

    public NsdDiscoveryService(Context context, User localUser, PeerRegistry registry) {
        this.context = context;
        this.localUser = localUser;
        this.registry = registry;
        this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        
        android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            multicastLock = wifi.createMulticastLock("LanChatMulticastLock");
            multicastLock.setReferenceCounted(true);
        }
    }

    @Override
    public void start() {
        if (multicastLock != null && !multicastLock.isHeld()) {
            multicastLock.acquire();
        }
        registerService();
        discoverServices();
    }

    @Override
    public void stop() {
        if (multicastLock != null && multicastLock.isHeld()) {
            multicastLock.release();
        }
        if (nsdManager != null) {
            if (discoveryListener != null) {
                try {
                    nsdManager.stopServiceDiscovery(discoveryListener);
                } catch (Exception e) {}
            }
            if (registrationListener != null) {
                try {
                    nsdManager.unregisterService(registrationListener);
                } catch (Exception e) {}
            }
        }
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    private void resolveNext() {
        NsdServiceInfo next = null;
        synchronized (resolveQueue) {
            if (isResolving || resolveQueue.isEmpty()) return;
            isResolving = true;
            next = resolveQueue.poll();
        }
        if (next != null) {
            try {
                nsdManager.resolveService(next, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        synchronized (resolveQueue) { isResolving = false; }
                        resolveNext();
                    }

                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {
                        if (serviceInfo.getServiceName().equals(localUser.getUserId()) || serviceInfo.getServiceName().equals(registeredServiceName)) {
                            synchronized (resolveQueue) { isResolving = false; }
                            resolveNext();
                            return;
                        }

                        System.out.println("[NSDDiscovery] Service resolved: " + serviceInfo.getServiceName() + " on port " + serviceInfo.getPort());

                        String displayName = serviceInfo.getServiceName(); // Fallback
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            java.util.Map<String, byte[]> attributes = serviceInfo.getAttributes();
                            if (attributes.containsKey("name")) {
                                byte[] nameBytes = attributes.get("name");
                                if (nameBytes != null) {
                                    displayName = new String(nameBytes);
                                }
                            }
                        }

                        Peer peer = new Peer(
                                serviceInfo.getServiceName(),
                                displayName,
                                serviceInfo.getHost().getHostAddress(),
                                serviceInfo.getPort(),
                                Peer.PeerStatus.ONLINE
                        );
                        registry.addOrUpdatePeer(peer);
                        
                        synchronized (resolveQueue) { isResolving = false; }
                        resolveNext();
                    }
                });
            } catch (Exception e) {
                synchronized (resolveQueue) { isResolving = false; }
                resolveNext();
            }
        }
    }

    private void registerService() {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(localUser.getUserId());
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(this.port);
        // On Android API 21+, we can set attributes
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            String name = localUser.getDisplayName() != null ? localUser.getDisplayName() : "Unknown";
            serviceInfo.setAttribute("name", name);
        }

        registrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                registeredServiceName = NsdServiceInfo.getServiceName();
                System.out.println("[NSDDiscovery] Registered local service '" + registeredServiceName + "' on port " + port);
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {}

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {}

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {}
        };

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }

    private void discoverServices() {
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {}

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // Ignore self
                if (service.getServiceName().equals(localUser.getUserId()) || service.getServiceName().equals(registeredServiceName)) {
                    return;
                }
                
                System.out.println("[NSDDiscovery] Service found: " + service.getServiceName() + ". Attempting to resolve...");
                
                synchronized (resolveQueue) {
                    resolveQueue.offer(service);
                }
                resolveNext();
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                if (service.getServiceName().equals(localUser.getUserId()) || service.getServiceName().equals(registeredServiceName)) {
                    return;
                }
                registry.removePeer(service.getServiceName());
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {}

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                nsdManager.stopServiceDiscovery(this);
            }
        };

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    @Override
    public void silentRefresh() {
        if (nsdManager != null && discoveryListener != null) {
            new Thread(() -> {
                try {
                    nsdManager.stopServiceDiscovery(discoveryListener);
                    Thread.sleep(500);
                    discoverServices();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
