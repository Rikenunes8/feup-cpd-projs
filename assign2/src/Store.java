import messages.MembershipLog;

import static messages.MessageBuilder.*;
import static messages.MulticastMessager.*;

import java.io.*;
import java.net.*;
import java.util.Date;


public class Store implements IMembership{
    private final InetAddress mcastAddr;
    private final int mcastPort;
    private final String nodeIP;
    private final int storePort;

    private int membershipCounter;
    private MembershipLog membershipLog;

    private final NetworkInterface networkInterface;
    private final InetSocketAddress inetSocketAddress;
    private DatagramSocket datagramSocket;

    // TODO everything
    public static void main(String[] args) throws IOException {
        Store store = parseArgs(args);
        //store.join();
        store.readTCP();
        for (int i = 0; i < 0; i++) { // TODO
            String date = receiveMcastMessage(store.datagramSocket);
            System.out.println("Right now it is " + date);
        }

        //store.leave();
    }

    private static Store parseArgs(String[] args) {
        InetAddress mcastAddr;
        int mcastPort;
        String nodeIP;
        int storePort;

        if (args.length != 4) System.out.println(usage());
        try {
            mcastAddr = InetAddress.getByName(args[0]);
            mcastPort = Integer.parseInt(args[1]);
            nodeIP    = args[2];
            storePort = Integer.parseInt(args[3]);
        } catch (UnknownHostException e) {
            System.out.println(usage());
            throw new RuntimeException(e);
        }
        return new Store(mcastAddr, mcastPort, nodeIP, storePort);
    }
    private static String usage() {
        return "Usage:\n\t java Store <IP_mcast_addr> <IP_mcast_port> <node_id>  <Store_port>";
    }

    public Store(InetAddress mcastAddr, int mcastPort, String nodeIP, int storePort) {
        this.mcastAddr = mcastAddr;
        this.mcastPort = mcastPort;
        this.nodeIP = nodeIP;
        this.storePort = storePort;

        this.membershipCounter = -1;
        this.membershipLog = null; // TODO

        this.datagramSocket = null;

        String networkInterfaceStr = "loopback"; // TODO
        try {
            this.networkInterface = NetworkInterface.getByName(networkInterfaceStr);
            this.inetSocketAddress = new InetSocketAddress(this.mcastAddr, 0);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeMembership() {
        // TODO
    }

    @Override
    public boolean join() {
        if (this.membershipCounter % 2 == 0) {
            System.out.println("This node already belongs to a multicast group");
            return false;
        }
        try {
            this.membershipCounter++;
            this.datagramSocket = new DatagramSocket(this.mcastPort);
            this.datagramSocket.setReuseAddress(true);
            this.datagramSocket.joinGroup(this.inetSocketAddress, this.networkInterface);

            // Notice cluster members of my join
            String msg = messageJoinLeave(this.nodeIP, this.storePort, this.membershipCounter);
            sendMcastMessage(msg, this.datagramSocket);

        } catch (Exception e) {
            System.out.println("Failure to join multicast group " + this.mcastAddr + ":" + this.mcastPort);
            System.out.println(e);
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public boolean leave() {
        if (this.membershipCounter % 2 == 1) {
            System.out.println("This node does not belong to a multicast group");
            return false;
        }
        try {
            this.membershipCounter++;

            // Notice cluster members of my leave
            String msg = messageJoinLeave(this.nodeIP, this.storePort, this.membershipCounter);
            sendMcastMessage(msg, this.datagramSocket);

            this.datagramSocket.leaveGroup(this.inetSocketAddress, this.networkInterface);
            this.datagramSocket = null;
        } catch (Exception e) {
            System.out.println("Failure to leave " + this.mcastAddr + ":" + this.mcastPort);
            System.out.println(e);
            throw new RuntimeException(e);
        }
        return true;
    }

    public String readTCP() {
        try (ServerSocket serverSocket = new ServerSocket(this.storePort)) {

            System.out.println("Server is listening on port " + this.storePort);
            int i = 0;
            while (i < 3) {
                Socket socket = serverSocket.accept();

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                String time = reader.readLine();

                System.out.println("New client connected: "+ time);

                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                writer.println(new Date().toString());
                i++;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
