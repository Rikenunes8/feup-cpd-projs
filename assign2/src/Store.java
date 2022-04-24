import java.util.regex.Pattern;

public class Store {
    // java Store <IP_mcast_addr> <IP_mcast_port> <node_id>  <Store_port>
    public static void main(String[] args) {
        if (args.length != 4) {
            throw new RuntimeException("Wrong number of expected arguments.\nUsage: java Store <IP_mcast_addr> <IP_mcast_port> <node_id> <Store_port>");
        }
        String ipMcastAddr = args[0];
        String ipMcastPort = args[1];
        String nodeId = args[2];
        String storePort = args[3];
    }
}
