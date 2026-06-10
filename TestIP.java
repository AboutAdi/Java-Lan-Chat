import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class TestIP {
    public static void main(String[] args) {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface intf = en.nextElement();
                System.out.println("Interface: " + intf.getName() + " - " + intf.getDisplayName());
                Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements()) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    System.out.println("  IP: " + inetAddress.getHostAddress() + " | Loopback: " + inetAddress.isLoopbackAddress() + " | IPv4: " + (inetAddress instanceof java.net.Inet4Address));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
