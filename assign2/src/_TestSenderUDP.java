import java.io.IOException;
import java.net.*;
import java.util.Date;

public class _TestSenderUDP {
    public static void main(String[] args) throws Exception {
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

        host = InetAddress.getByName(args[0]);
        group = InetAddress.getByName(args[1]);
        port = Integer.parseInt(args[2]);
        period = Integer.parseInt(args[3]);
        netIfstr = args[4];


        s = new DatagramSocket(0, host);
        netIf = NetworkInterface.getByName(netIfstr);
        if (netIf != null) {
            s.setOption(StandardSocketOptions.IP_MULTICAST_IF, netIf);
        }

        // Check the IP_MULTICAST_LOOP option
        System.out.println("IP_MULTICAST_LOOP: " + s.getOption(StandardSocketOptions.IP_MULTICAST_LOOP));

        while(true) {
            Date d = new Date();
            String date = d.toString();

            System.out.println("it is " + date);
            System.out.println("Sending message to " + group + ":" + port);

            byte[] sndBuffer = date.getBytes();

            DatagramPacket packet = new DatagramPacket(sndBuffer, sndBuffer.length, group, port);

            s.send(packet);


            Thread.sleep(period*1000);

        }
    }
}
