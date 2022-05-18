import messages.*;

import static messages.MessageBuilder.messageJoinLeave;
import static messages.MulticastMessager.*;

import utils.FileUtils;
import utils.HashUtils;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class Store implements IMembership, IService {
    private final InetAddress mcastAddr;
    private final int mcastPort;
    private final String nodeIP;
    private final int storePort;
    private final String hashedId;

    private int membershipCounter;
    private MembershipLog membershipLog;
    private MembershipTable membershipTable;

    private final NetworkInterface networkInterface;
    private final InetSocketAddress inetSocketAddress;
    private DatagramSocket rcvDatagramSocket;
    private DatagramSocket sndDatagramSocket;

    private ExecutorService executorMcast;

    // TODO everything
    public static void main(String[] args) {
        Store store = parseArgs(args);

        Runtime runtime = Runtime.getRuntime();
        // ExecutorService executor = Executors.newWorkStealingPool(8);
        ExecutorService executor = Executors.newFixedThreadPool(runtime.availableProcessors());
        // according to the number of processors available to the Java virtual machine

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
        this.membershipLog = new MembershipLog(); // TODO
        this.membershipTable = new MembershipTable(); // TODO

        this.rcvDatagramSocket = null;
        this.sndDatagramSocket = null;
        String networkInterfaceStr = "loopback"; // TODO

        this.hashedId = HashUtils.getHashedSha256(this.getNodeIPPort());

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
            sendMcastMessage(msg, this.sndDatagramSocket, this.mcastAddr, this.mcastPort);
            System.out.println("Join message sent!");

            this.executorMcast = Executors.newWorkStealingPool(1);
            Runnable work = new ListenerMcastThread(this);
            this.executorMcast.execute(work);

            // TODO make node directory here
            FileUtils.newDirectory(this.hashedId);

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

    @Override
    public String put(String key, String value) {

        String keyHashed = (key == null) ? HashUtils.getHashedSha256(value) : key;

        // File is saved in the closest node of the key
        String closestNode = this.membershipTable.getClosestMembershipInfo(keyHashed);
        if (closestNode.equals(this.getNodeIPPort())) {
            FileUtils.saveFile(this.hashedId, keyHashed, value);
            // TODO send to testClient the keyHashed who is responsible to display the key received of the file
            return keyHashed;
        } else {
            // TODO
            // otherwise, send a put request to the node that was found closest to the key
            String message = MessageBuilder.messageStore("PUT", keyHashed, value);
            // TcpMessager to the closestNode
        }

        return null;
    }

    @Override
    public String get(String key) {
        // Argument is the key returned by put

        // File (that was requested the content from) is stored in the closest node of the key
        String closestNode = this.membershipTable.getClosestMembershipInfo(key);
        if (closestNode.equals(this.getNodeIPPort())) {
            return FileUtils.getFile(this.hashedId, key);
        } else {
            // TODO
            // otherwise, send a get request to the node that was found closest to the key
            String message = MessageBuilder.messageStore("GET", key);
        }

        return null;
    }

    @Override
    public boolean delete(String key) {
        // Argument is the key returned by put

        // File (that was requested to be deleted) is stored in the closest node of the key
        String closestNode = this.membershipTable.getClosestMembershipInfo(key);
        if (closestNode.equals(this.getNodeIPPort())) {
            return FileUtils.deleteFile(this.hashedId, key);
        } else {
            // TODO
            // otherwise, send a delete request to the node that was found closest to the key
            String message = MessageBuilder.messageStore("DELETE", key);
        }

        return false;
    }

    private void initializeMembership() {
        // TODO
    }

    public void addJoinLeaveEvent(String nodeIP, int port, int membershipCounter) {
        if (membershipCounter % 2 == 0)
            this.membershipTable.addMembershipInfo(HashUtils.getHashedSha256(this.getNodeIPPort()), new MembershipInfo(nodeIP, port));
        else
            this.membershipTable.removeMembershipInfo(HashUtils.getHashedSha256(this.getNodeIPPort()));

        this.membershipLog.addMembershipInfo(new MembershipLogRecord(nodeIP, membershipCounter));
    }

    private String getNodeIPPort() {
        return this.nodeIP + ":" + this.storePort;
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

    public MembershipLog getMembershipLog() {
        return membershipLog;
    }

    public MembershipTable getMembershipTable() {
        return membershipTable;
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