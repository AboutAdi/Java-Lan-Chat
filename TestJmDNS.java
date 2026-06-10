import javax.jmdns.JmDNS;
public class TestJmDNS {
    public static void main(String[] args) {
        JmDNS j = null;
        j.requestServiceInfo("type", "name", 1);
    }
}
