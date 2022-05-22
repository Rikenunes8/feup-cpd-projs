import membership.*;

import static messages.MessageBuilder.joinLeaveMessage;
import static messages.MulticastMessager.*;

import messages.MessageBuilder;
import messages.TcpMessager;
import utils.FileUtils;
import utils.HashUtils;

import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;


public class Store implements IMembership, IService {
    private static final int ALARM_PERIOD = 10;
    private static final int REPLICATION = 3;

    private final InetAddress mcastAddr;
    private final int mcastPort;
    private final String nodeIP;
    private final int storePort;
    private final String hashedId;

    private final Set<String> keys;
    private int membershipCounter;
    private final MembershipView membershipView;
    private final ConcurrentLinkedQueue<DispatcherThread> pendingQueue;

    private final NetworkInterface networkInterface;
    private final InetSocketAddress inetSocketAddress;
    private final DatagramSocket sndDatagramSocket;
    private DatagramSocket rcvDatagramSocket;

    private ExecutorService executorMcast;
    private ScheduledExecutorService executorTimerTask;

    public static void main(String[] args) {
        Store store = parseArgs(args);

        // ExecutorService executor = Executors.newWorkStealingPool(8);
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);
        // according to the number of processors available to the Java virtual machine

        while (true) {
            try (ServerSocket serverSocket = new ServerSocket(store.storePort)){
                Socket socket = serverSocket.accept();
                System.out.println("Main connection accepted");

                executor.execute(new DispatcherThread(socket, store));
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

        this.keys = new HashSet<>();
        this.pendingQueue = new ConcurrentLinkedQueue<>();
        this.membershipCounter = -1;
        this.membershipView = new MembershipView(new MembershipTable(), new MembershipLog());

        this.hashedId = HashUtils.getHashedSha256(this.getNodeIPPort());
        System.out.println("HashID: " + hashedId); // TODO DEBUG

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


    // ---------- MEMBERSHIP PROTOCOL -------------

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
            MembershipCollector.collect(serverSocket, this);
            initMcastReceiver();
            this.executorMcast.execute(new ListenerMcastThread(this));

            FileUtils.createDirectory(this.hashedId);
            while (!this.isEmptyPendingQueue()) {
                this.removeFromPendingQueue().processMessage();
            }
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
            String msg = joinLeaveMessage(this.nodeIP, this.storePort, this.membershipCounter, 0);
            sendMcastMessage(msg, this.sndDatagramSocket, this.mcastAddr, this.mcastPort);

            endMcastReceiver();

            var keysCopy = new HashSet<String>(this.keys);
            for (String key : keysCopy) {
                this.transferFile(key);
            }
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
        if (!executorMcast.isTerminated()) executorMcast.shutdownNow();

        System.out.println("Shutdown complete");

        this.rcvDatagramSocket.leaveGroup(this.inetSocketAddress, this.networkInterface);
        this.rcvDatagramSocket = null;
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
            this.executorTimerTask.scheduleAtFixedRate(new AlarmThread(this), 0, ALARM_PERIOD, TimeUnit.SECONDS);
        }
        else if (this.executorTimerTask != null && !smaller) {
            System.out.println("Alarm canceled");
            this.executorTimerTask.shutdown();
            try { this.executorTimerTask.awaitTermination(1, TimeUnit.SECONDS);}
            catch (InterruptedException e) { e.printStackTrace(); } // TODO

            if (!this.executorTimerTask.isTerminated()) this.executorTimerTask.shutdownNow();
            this.executorTimerTask = null;
        }
    }

    // ---------- END OF MEMBERSHIP PROTOCOL ---------------


    // ---------- SERVICE PROTOCOL -------------

    @Override
    public void put(String key, String value) {
        // String keyHashed = (key == null || key.equals("null")) ? HashUtils.getHashedSha256(value) : key;

        // FILE IS SAVED IN THE CLOSEST NODE FROM THE KEY
        MembershipInfo closestNode = this.membershipView.getClosestMembershipInfo(key);

        if (closestNode.toString().equals(this.getNodeIPPort())) {
            this.saveFile(key, value);
        } else {
            try {
                // REDIRECT THE PUT REQUEST TO THE CLOSEST NODE OF THE KEY THAT I FOUND
                String requestMessage = MessageBuilder.storeMessage("PUT", key, value);
                TcpMessager.sendMessage(closestNode.getIP(), closestNode.getPort(), requestMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String get(String key) {
        // File (that was requested the content from) is stored in the closest node of the key
        MembershipInfo closestNode = this.membershipView.getClosestMembershipInfo(key);

        if (closestNode.toString().equals(this.getNodeIPPort())) {
            return this.getFile(key);
        } else {
            // REDIRECT THE GET REQUEST TO THE CLOSEST NODE OF THE KEY THAT I FOUND
            try (Socket socket = new Socket(closestNode.getIP(), closestNode.getPort())) {
                String requestMessage = MessageBuilder.storeMessage("GET", key);
                TcpMessager.sendMessage(socket, requestMessage);
                return TcpMessager.receiveMessage(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public void delete(String key) {
        // File (that was requested to be deleted) is stored in the closest node of the key
        MembershipInfo closestNode = this.membershipView.getClosestMembershipInfo(key);

        if (closestNode.toString().equals(this.getNodeIPPort())) {
            this.deleteFile(key);
        } else {
            try {
                // REDIRECT THE DELETE REQUEST TO THE CLOSEST NODE OF THE KEY THAT I FOUND
                String message = MessageBuilder.storeMessage("DELETE", key);
                TcpMessager.sendMessage(closestNode.getIP(), closestNode.getPort(), message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean transferFile(String key) {
        var value = this.getFile(key);
        if (value == null) return false;
        this.put(key, value);
        this.deleteFile(key);
        return true;
    }
    public void saveFile(String key, String value) {
        if (FileUtils.saveFile(this.hashedId, key, value)) {
            this.keys.add(key);
        }
    }
    public String getFile(String key) {
        return FileUtils.getFile(this.hashedId, key);
    }
    public void deleteFile(String key) {
        if (FileUtils.deleteFile(this.hashedId, key)) {
            this.keys.remove(key);
        }
    }

    // ---------- END OF SERVICE PROTOCOL ---------------

    // -------------- GETS AND SETS ---------------------

    public String getNodeIP() {
        return nodeIP;
    }
    public int getStorePort() {
        return this.storePort;
    }
    public String getNodeIPPort() {
        return HashUtils.joinIpPort(this.nodeIP, this.storePort);
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
    public Set<String> getKeys() {
        return keys;
    }
    public MembershipInfo getClosestMembershipInfo(String keyHashed) {
        return this.membershipView.getClosestMembershipInfo(keyHashed);
    }

    public void setMembershipView(MembershipTable membershipTable, MembershipLog membershipLog) {
        this.membershipView.setMembershipTable(membershipTable);
        this.membershipView.setMembershipLog(membershipLog);
    }

    public void addToPendingQueue(DispatcherThread dispatcherThread) {
        this.pendingQueue.add(dispatcherThread);
    }
    public DispatcherThread removeFromPendingQueue() {
        return this.pendingQueue.remove();
    }
    public boolean isEmptyPendingQueue() {
        return this.pendingQueue.isEmpty();
    }

    public boolean isOnline() {
        return this.membershipView.isOnline(this.hashedId);
    }
}
