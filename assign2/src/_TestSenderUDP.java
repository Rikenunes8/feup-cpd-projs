import java.io.IOException;
import java.net.*;
import java.util.Date;

public class _TestSenderUDP {
    public static void main(String[] args) throws IOException {
        DatagramSocket s = null;
        int port = 0, period = 0;
        InetAddress group = null, host = null;
        String netIfstr = null;
        NetworkInterface netIf = null;

        if( args.length != 5 ) {
            System.out.println("Usage: java DateServer3 <host_addr <multicast_group> <multicast_port> <period> <net intert>");
            System.out.println("\t use \"\" (empty string) for <host_addr> to not to bind the sending socket");
            System.out.println("\t use \"1(empty string) for <network interface> to not to configure the network interface");
            System.exit(1);
        }
        try {
            if( args[0].length() != 0 ) {
                host = InetAddress.getByName(args[0]);
            }
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host: " + args[0]);
            System.exit(1);
        }
        try {
            group = InetAddress.getByName(args[1]);
            port = Integer.parseInt(args[2]);
            period = Integer.parseInt(args[3]);
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host: " + args[1]);
            System.exit(1);
        } catch (NumberFormatException e) {
            System.err.println("Error: "+ args[1] + " is not a property formatted integer");
            System.exit(1);
        }

        if (port > 65535 ) {
            System.out.println("Error: port number must be smaller than 65536");
            System.exit(1);
        }
        netIfstr = args[4];
        try {
            s = new DatagramSocket();
            netIf = NetworkInterface.getByName(netIfstr);
            if (netIf != null) {
                s.setOption(StandardSocketOptions.IP_MULTICAST_IF, netIf);
            }
        } catch (SocketException e) {
            System.err.println("Failure to socket.");
            System.err.println(e);
            System.exit(1);
        }
        // Check the IP_MULTICAST_LOOP option
        System.out.println("IP_MULTICAST_LOOP: " + s.getOption(StandardSocketOptions.IP_MULTICAST_LOOP));

        while(true) {
            DatagramPacket packet = null;
            byte[] sndBuffer = null;
            Date d = new Date();
            String date = d.toString();

            System.out.println("it is " + date);
            System.out.println("Sending message to " + group + ":" + port);

            sndBuffer = date.getBytes();

            packet = new DatagramPacket(sndBuffer, sndBuffer.length, group, port);

            s.send(packet);

            try {
                Thread.sleep(period*1000);
            } catch (Exception e) {
                System.out.println(e);
                System.exit(1);
            }
        }
    }
}
