import messages.MembershipLog;

import static messages.MessageBuilder.messageJoinLeave;
import static messages.MulticastMessager.*;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class Store implements IMembership{
    private final InetAddress mcastAddr;
    private final int mcastPort;
    private final String nodeIP;
    private final int storePort;

    private int membershipCounter;
    private MembershipLog membershipLog;

    private final NetworkInterface networkInterface;
    private final InetSocketAddress inetSocketAddress;
    private DatagramSocket rcvDatagramSocket;
    private DatagramSocket sndDatagramSocket;

    private ExecutorService executorMcast;

    // TODO everything
    public static void main(String[] args) {
        Store store = parseArgs(args);

        // ExecutorService executor = Executors.newWorkStealingPool(1);
        ExecutorService executor = Executors.newFixedThreadPool(8);

        while (true) {
            System.out.println("New thread");

            try (ServerSocket serverSocket = new ServerSocket(store.getPort())){
                Socket socket = serverSocket.accept();

                Runnable work = new DispatcherThread(socket, store);
                executor.execute(work);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
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

        this.rcvDatagramSocket = null;
        this.sndDatagramSocket = null;
        String networkInterfaceStr = "loopback"; // TODO

        try {
            this.sndDatagramSocket = new DatagramSocket();
            this.networkInterface = NetworkInterface.getByName(networkInterfaceStr);
            this.inetSocketAddress = new InetSocketAddress(this.mcastAddr, this.mcastPort);

            if (this.networkInterface != null) {
                this.sndDatagramSocket.setOption(StandardSocketOptions.IP_MULTICAST_IF, this.networkInterface);
            }
            System.out.println("IP_MULTICAST_LOOP: " + this.sndDatagramSocket.getOption(StandardSocketOptions.IP_MULTICAST_LOOP));

        } catch (IOException e) {
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
            this.rcvDatagramSocket = new DatagramSocket(null);
            this.rcvDatagramSocket.setReuseAddress(true);
            this.rcvDatagramSocket.bind(new InetSocketAddress(this.mcastPort));
            this.rcvDatagramSocket.joinGroup(this.inetSocketAddress, this.networkInterface);

            // Notice cluster members of my join
            String msg = messageJoinLeave(this.nodeIP, this.storePort, this.membershipCounter);
            // String msg = "Joining " + this.nodeIP + ":" + this.storePort + " - " +new Date().toString();
            sendMcastMessage(msg, this.sndDatagramSocket, this.mcastAddr, this.mcastPort);
            System.out.println("Join message sent!");

            this.executorMcast = Executors.newWorkStealingPool(1);
            Runnable work = new ListenerMcastThread(this);
            this.executorMcast.execute(work);

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
            // String msg = "Leaving " + this.nodeIP + ":" + this.storePort + " - " +new Date().toString();
            sendMcastMessage(msg, this.rcvDatagramSocket, this.mcastAddr, this.mcastPort);

            this.executorMcast.shutdown();
            System.out.println("Shutting down...");
            executorMcast.awaitTermination(1, TimeUnit.SECONDS);
            System.out.println("Wait enough");
            if (!executorMcast.isTerminated()) {
                System.out.println("Forcing shut down");
                executorMcast.shutdownNow();
                System.out.println("Shut down complete");
            }

            this.rcvDatagramSocket.leaveGroup(this.inetSocketAddress, this.networkInterface);
            this.rcvDatagramSocket = null;

        } catch (Exception e) {
            System.out.println("Failure to leave " + this.mcastAddr + ":" + this.mcastPort);
            System.out.println(e);
            throw new RuntimeException(e);
        }
        return true;
    }



    public int getPort() {
        return this.storePort;
    }

    public DatagramSocket getRcvDatagramSocket() {
        return this.rcvDatagramSocket;
    }

    public int getMembershipCounter() {
        return this.membershipCounter;
    }
}



          // Start accepting TCP connections and collect membership views
//        MembershipColectorThread membershipColectorThread = new MembershipColectorThread(store.nodeIP, store.storePort);
//        membershipColectorThread.start();
//
//        store.join();
//
//
//        membershipColectorThread.join();
//        List<String> membershipViews = membershipColectorThread.getMembershipViews();
//        System.out.println("Printing views");
//        for (var view : membershipViews) {
//            System.out.println(view);
//        }
//
//        while (true) {
//            String msg = receiveMcastMessage(store.rcvDatagramSocket);
//            System.out.println(msg);
//            if (msg.equals("quit")) break;
//        }
//
//        store.leave();