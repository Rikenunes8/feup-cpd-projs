import membership.*;

import static messages.MessageBuilder.messageJoinLeave;
import static messages.MulticastMessager.*;

import messages.MessageBuilder;
import utils.FileUtils;
import utils.HashUtils;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;


public class Store implements IMembership, IService {
    private final InetAddress mcastAddr;
    private final int mcastPort;
    private final String nodeIP;
    private final int storePort;
    private final String hashedId;

    private int membershipCounter;
    private MembershipView membershipView;

    private final NetworkInterface networkInterface;
    private final InetSocketAddress inetSocketAddress;
    private final DatagramSocket sndDatagramSocket;
    private DatagramSocket rcvDatagramSocket;

    private ExecutorService executorMcast;
    private ScheduledExecutorService executorTimerTask;

    // TODO everything
    public static void main(String[] args) {
        Store store = parseArgs(args);

        Runtime runtime = Runtime.getRuntime();
        // ExecutorService executor = Executors.newWorkStealingPool(8);
        ExecutorService executor = Executors.newFixedThreadPool(runtime.availableProcessors());
        // according to the number of processors available to the Java virtual machine

        while (true) {
            try (ServerSocket serverSocket = new ServerSocket(store.getStorePort())){
                Socket socket = serverSocket.accept();
                System.out.println("Main connection accepted");

                Runnable work = new DispatcherThread(socket, store);
                executor.execute(work);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static String usage() {
        return "Usage:\n\t java Store <IP_mcast_addr> <IP_mcast_port> <node_id>  <Store_port>";
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

    public Store(InetAddress mcastAddr, int mcastPort, String nodeIP, int storePort) {
        this.mcastAddr = mcastAddr;
        this.mcastPort = mcastPort;
        this.nodeIP = nodeIP;
        this.storePort = storePort;

        this.membershipCounter = -1;
        this.membershipView = new MembershipView(new MembershipTable(), new MembershipLog());

        String networkInterfaceStr = "loopback"; // TODO

        this.hashedId = HashUtils.getHashedSha256(this.getNodeIPPort());
        System.out.println("HashID: " + hashedId);

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
            ServerSocket serverSocket = new ServerSocket(0);
            this.membershipCounter++;

            this.executorMcast = Executors.newWorkStealingPool(2);
            // this.executorMcast.execute(new MembershipCollectorThread(serverSocket, this));
            MembershipCollector.collect(serverSocket, this);
            initMcastReceiver();
            this.executorMcast.execute(new ListenerMcastThread(this));

            // TODO make node directory here
            FileUtils.newDirectory(this.hashedId);

        } catch (Exception e) {
            System.out.println("Failure to join multicast group " + this.mcastAddr + ":" + this.mcastPort);
            System.out.println(e.getMessage());
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
            String msg = messageJoinLeave(this.nodeIP, this.storePort, this.membershipCounter, 0);
            sendMcastMessage(msg, this.sndDatagramSocket, this.mcastAddr, this.mcastPort);

            endMcastReceiver();

        } catch (Exception e) {
            System.out.println("Failure to leave " + this.mcastAddr + ":" + this.mcastPort);
            System.out.println(e);
            throw new RuntimeException(e);
        }
        return true;
    }

    private void initMcastReceiver() throws IOException {
        this.rcvDatagramSocket = new DatagramSocket(null);
        this.rcvDatagramSocket.setReuseAddress(true);
        this.rcvDatagramSocket.bind(new InetSocketAddress(this.mcastPort));
        this.rcvDatagramSocket.joinGroup(this.inetSocketAddress, this.networkInterface);
    }

    private void endMcastReceiver() throws InterruptedException, IOException {
        this.executorMcast.shutdown();
        System.out.println("Shutting down...");
        executorMcast.awaitTermination(1, TimeUnit.SECONDS);
        System.out.println("Wait enough");
        if (!executorMcast.isTerminated()) {
            System.out.println("Forcing shutdown");
            executorMcast.shutdownNow();
            System.out.println("Shutdown complete");
        }

        this.rcvDatagramSocket.leaveGroup(this.inetSocketAddress, this.networkInterface);
        this.rcvDatagramSocket = null;
    }

    @Override
    public String put(String key, String value) {

        String keyHashed = (key == null) ? HashUtils.getHashedSha256(value) : key;

        // File is saved in the closest node of the key
        String closestNode = this.membershipView.getClosestMembershipInfo(keyHashed);
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
        String closestNode = this.membershipView.getClosestMembershipInfo(key);
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
        String closestNode = this.membershipView.getClosestMembershipInfo(key);
        if (closestNode.equals(this.getNodeIPPort())) {
            return FileUtils.deleteFile(this.hashedId, key);
        } else {
            // TODO
            // otherwise, send a delete request to the node that was found closest to the key
            String message = MessageBuilder.messageStore("DELETE", key);
        }

        return false;
    }

    public void updateMembershipView(MembershipTable membershipTable, MembershipLog membershipLog) {
        this.membershipView.merge(membershipTable, membershipLog);
        timerTask();
    }

    public void updateMembershipView(String nodeIP, int port, int membershipCounter) {
        this.membershipView.updateMembershipInfo(nodeIP, port, membershipCounter);
        timerTask();
    }
    public void mergeMembershipViews(ConcurrentHashMap<String, MembershipView> membershipViews) {
        this.membershipView.mergeMembershipViews(membershipViews);
        timerTask();
    }

    private void timerTask() {
        // Compare hashedID with the smaller hashedID in the cluster view
        var infoMap = this.membershipView.getMembershipTable().getMembershipInfoMap();
        boolean smaller = !infoMap.isEmpty() && hashedId.equals(infoMap.firstKey());
        System.out.println("Am I the smaller: " + smaller);

        if (this.executorTimerTask == null && smaller) {
            System.out.println("Alarm setted");
            this.executorTimerTask = Executors.newScheduledThreadPool(1);
            String msg = MessageBuilder.membershipMessage(this.membershipView, nodeIP);
            this.executorTimerTask.scheduleAtFixedRate(new AlarmThread(this, msg), 0, 10, TimeUnit.SECONDS);
        }
        else if (this.executorTimerTask != null && !smaller) {
            System.out.println("Alarm canceled");
            // TODO does this work
            this.executorTimerTask.shutdown();
            try {
                this.executorTimerTask.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace(); // TODO
            }
            if (!this.executorTimerTask.isTerminated()) this.executorTimerTask.shutdownNow();
            this.executorTimerTask = null;
        }
    }

    public String getNodeIP() {
        return nodeIP;
    }
    public int getStorePort() {
        return this.storePort;
    }
    private String getNodeIPPort() {
        return this.nodeIP + ":" + this.storePort;
    }
    public InetAddress getMcastAddr() {
        return mcastAddr;
    }
    public int getMcastPort() {
        return mcastPort;
    }
    public DatagramSocket getRcvDatagramSocket() {
        return this.rcvDatagramSocket;
    }
    public DatagramSocket getSndDatagramSocket() {
        return sndDatagramSocket;
    }
    public int getMembershipCounter() {
        return this.membershipCounter;
    }
    public MembershipView getMembershipView() {
        return membershipView;
    }

    public void setMembershipView(MembershipView membershipView) {
        this.membershipView = membershipView;
    }
    public void setMembershipView(MembershipTable membershipTable, MembershipLog membershipLog) {
        this.membershipView.setMembershipTable(membershipTable);
        this.membershipView.setMembershipLog(membershipLog);
    }
}
