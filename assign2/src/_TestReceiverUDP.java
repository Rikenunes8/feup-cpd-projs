import java.net.*;


public class _TestReceiverUDP {
    static NetworkInterface inIf = null;

    static DatagramSocket join(InetAddress group, int port, String netIfStr) throws Exception {
        DatagramSocket s = new DatagramSocket(null);
        s.setReuseAddress(true);
        s.bind(new InetSocketAddress(port));
        inIf = NetworkInterface.getByName(netIfStr);
        s.joinGroup(new InetSocketAddress(group, port), inIf);
        return s;
    }

    public static void main(String[] args) throws Exception {
        DatagramSocket s = null;
        int port = 0;
        InetAddress group = null;
        String netIfStr = null;
        int no_packets = Integer.MAX_VALUE;

        if (args.length != 3 && args.length != 4) {
            System.out.println("Usage: java _TestReceiverUDP <multicast_group> <multicast_port> <network interface> [no_packets]");
            System.out.println("\t Use \"\" (empty string) for <network interface> to not to configure the network interface");
            System.exit(1);
        }

        group = InetAddress.getByName(args[0]);
        port = Integer.parseInt(args[1]);
        netIfStr = args[2];

        if (args.length == 4) {
            no_packets = Integer.parseInt(args[3]);
        }

        s = join(group, port, netIfStr);

        for (int n = 0; n < no_packets; n++) {
            byte[] recvBuffer = new byte[8092];
            String date;

            DatagramPacket recvPacket = new DatagramPacket(recvBuffer, recvBuffer.length);
            System.out.println("Waiting for a new packet: ...");
            s.receive(recvPacket);
            date = new String(recvPacket.getData(), 0, recvPacket.getLength());
            System.out.println("Right now @ " + recvPacket.getAddress() + " it is " + date);
        }

        s.leaveGroup(new InetSocketAddress(group, 0), inIf);

        System.exit(0);
    }
}
