package com.voibiz.lanchat.core.network;
import org.junit.jupiter.api.Test;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
public class JmDNSTest {
    @Test
    public void test() throws Exception {
        JmDNS j1 = JmDNS.create();
        JmDNS j2 = JmDNS.create();
        
        CountDownLatch latch = new CountDownLatch(1);
        j1.addServiceListener("_http._tcp.local.", new ServiceListener() {
            @Override
            public void serviceAdded(ServiceEvent event) {
                System.out.println("serviceAdded called");
                j1.requestServiceInfo(event.getType(), event.getName(), 1);
            }
            @Override
            public void serviceRemoved(ServiceEvent event) {}
            @Override
            public void serviceResolved(ServiceEvent event) {
                System.out.println("serviceResolved! " + event.getInfo());
                latch.countDown();
            }
        });
        
        ServiceInfo info = ServiceInfo.create("_http._tcp.local.", "test", 8080, "text");
        j2.registerService(info);
        
        latch.await(5, TimeUnit.SECONDS);
    }
}
