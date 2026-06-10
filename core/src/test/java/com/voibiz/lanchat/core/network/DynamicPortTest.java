package com.voibiz.lanchat.core.network;
import org.junit.jupiter.api.Test;
import java.net.ServerSocket;
import static org.junit.jupiter.api.Assertions.assertTrue;
public class DynamicPortTest {
    @Test
    public void test() throws Exception {
        ServerSocket s1 = new ServerSocket(50506);
        ServerSocket s2 = new ServerSocket(0); // dynamic port
        assertTrue(s2.getLocalPort() != 50506);
        s1.close();
        s2.close();
    }
}
