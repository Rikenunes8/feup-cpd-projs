import membership.*;

import static messages.MessageBuilder.leaveMessage;
import messages.MessageBuilder;
import messages.TcpMessager;
import utils.FileUtils;
import utils.HashUtils;

import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.*;
import java.util.*;

import static messages.MulticastMessager.sendMcastMessage;


public class Store extends UnicastRemoteObject implements IMembership, IService {
    private static final int ALARM_PERIOD = 10;
    public static final int REPLICATION_FACTOR = 3;

    private final InetAddress mcastAddr;
    private final int mcastPort;
    private final String nodeIP;
    private final int storePort;
    private final String id;

    private final Set<String> keys;
    private int membershipCounter;
    private final MembershipView membershipView;
    private final ConcurrentLinkedQueue<DispatcherThread> pendingQueue;
    private final Set<String> alreadySent; // TODO could this be a String instead of Set??

    private final NetworkInterface networkInterface;
    private final InetSocketAddress inetSocketAddress;
    private final DatagramSocket sndDatagramSocket;
    private DatagramSocket rcvDatagramSocket;

    private ExecutorService executorMcast;
    private ScheduledExecutorService executorTimerTask;

    public static void main(String[] args) throws RemoteException {
        Store store = parseArgs(args);

        // ExecutorService executor = Executors.newWorkStealingPool(8);
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);
        // according to the number of processors available to the Java virtual machine

        Registry registry = LocateRegistry.getRegistry();
        registry.rebind(store.nodeIP+":"+store.storePort, store);

        while (true) {
            try (ServerSocket serverSocket = new ServerSocket(store.storePort, 0, InetAddress.getByName(store.nodeIP))){
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
    private static Store parseArgs(String[] args) throws RemoteException{
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

    public Store(InetAddress mcastAddr, int mcastPort, String nodeIP, int storePort) throws RemoteException {
        super();
        this.mcastAddr = mcastAddr;
        this.mcastPort = mcastPort;
        this.nodeIP = nodeIP;
        this.storePort = storePort;

        this.keys = new HashSet<>();
        this.pendingQueue = new ConcurrentLinkedQueue<>();
        this.alreadySent = Collections.synchronizedSet(new HashSet<>());
        this.membershipView = new MembershipView(new MembershipTable(), new MembershipLog());

        this.id = HashUtils.getHashedSha256(this.getNodeIPPort());
        System.out.println("ID: " + id); // TODO DEBUG
        FileUtils.createRoot();
        FileUtils.createDirectory(this.id);

        this.startMSCounter();
        this.loadLogs();

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

        // If the store went down due to a crash then automatically join on start up
        if (this.membershipCounter % 2 == 0) {
            try {
                initMcastReceiver();
                this.executorMcast.execute(new ListenerMcastThread(this));
            }
            catch (IOException e) {throw new RuntimeException(e);}
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
            ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getByName(this.nodeIP));
            this.incrementMSCounter();

            this.executorMcast = Executors.newWorkStealingPool(2);
            MembershipCollector.collect(serverSocket, this);
            initMcastReceiver();
            this.executorMcast.execute(new ListenerMcastThread(this));

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
            this.incrementMSCounter();

            // Notice cluster members of my leave
            String msg = leaveMessage(this.id, this.nodeIP, this.storePort, this.membershipCounter);
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
        var oldLogs = new ArrayList<>(this.membershipView.getMembershipLog().getLogs());
        this.membershipView.merge(membershipTable, membershipLog);
        if (this.membershipView.getMembershipLog().hasChanged(new MembershipLog(oldLogs))) {
            this.alreadySent.clear();
            this.saveLogs();
        }
        timerTask();
    }
    public void updateMembershipView(String id, String ip, int port, int membershipCounter) {
        var oldLogs = new ArrayList<>(this.membershipView.getMembershipLog().getLogs());
        this.membershipView.updateMembershipInfo(id, ip, port, membershipCounter);
        if (this.membershipView.getMembershipLog().hasChanged(new MembershipLog(oldLogs))){
            this.alreadySent.clear();
            this.saveLogs();
        }
        timerTask();
    }
    public void mergeMembershipViews(ConcurrentHashMap<String, MembershipView> membershipViews) {
        var oldLogs = new ArrayList<>(this.membershipView.getMembershipLog().getLogs());
        this.membershipView.mergeMembershipViews(membershipViews);
        if (this.membershipView.getMembershipLog().hasChanged(new MembershipLog(oldLogs))){
            this.alreadySent.clear();
            this.saveLogs();
        }
        timerTask();
    }

    private void timerTask() {
        // Compare hashedID with the smaller hashedID in the cluster view
        var infoMap = this.membershipView.getMembershipTable().getMembershipInfoMap();
        boolean smaller = !infoMap.isEmpty() && id.equals(infoMap.firstKey());
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
        // In order to discard duplicated requests
        if (this.keys.contains(key)) return;

        // File is stored in the closest node of the key and its replicas
        Map.Entry<String, MembershipInfo> closestEntry = this.membershipView.getClosestMembershipInfo(key);
        Map<String, MembershipInfo> replicationEntries = this.getReplicationEntries(closestEntry);

        if (closestEntry.getKey().equals(this.id)) {
            this.saveFile(key, value);

            // the closest node of the key is the one responsible for the coordination of the replication
            for (var nextKey : replicationEntries.keySet())
                this.redirectService(replicationEntries.get(nextKey), MessageBuilder.storeMessage("PUT", key, value));
        } else {
            if (replicationEntries.containsKey(this.id)) this.saveFile(key, value);
            this.redirectService(closestEntry.getValue(), MessageBuilder.storeMessage("PUT", key, value));
        }
    }

    @Override
    public String get(String key) {
        // File (that was requested the content from) is stored in the closest node of the key
        if (this.keys.contains(key)) {
            return this.getFile(key);
        }

        Map.Entry<String, MembershipInfo> closestEntry = this.membershipView.getClosestMembershipInfo(key);
        Map<String, MembershipInfo> replicationEntries = this.getReplicationEntries(closestEntry);
        replicationEntries.put(closestEntry.getKey(), closestEntry.getValue());

        String requestMessage = MessageBuilder.storeMessage("GET", key);
        for (var node : replicationEntries.values()) {
            try (Socket socket = new Socket(node.getIP(), node.getPort())) {
                TcpMessager.sendMessage(socket, requestMessage);
                socket.setSoTimeout(2000);
                String response = TcpMessager.receiveMessage(socket);
                if (!response.equals("null")) return response;
            }  catch (IOException e) {
                System.out.println("Node of get unreachable");
            }
        }

        return null;
    }

    @Override
    public void delete(String key) {
        // File (that was requested to be deleted) is stored in the closest node of the key and its replicas
        Map.Entry<String, MembershipInfo> closestEntry = this.membershipView.getClosestMembershipInfo(key);
        Map<String, MembershipInfo> replicationEntries = this.getReplicationEntries(closestEntry);

        if (closestEntry.getKey().equals(this.id)) {
            // In order to discard duplicated requests
            if (!this.keys.contains(key)) return;
            this.deleteFile(key);

            // the closest node of the key is the one responsible for the coordination of the replication
            for (var nextKey : replicationEntries.keySet())
                this.redirectService(replicationEntries.get(nextKey), MessageBuilder.storeMessage("DELETE", key));
        } else {
            if (replicationEntries.containsKey(this.id)) this.deleteFile(key);
            this.redirectService(closestEntry.getValue(), MessageBuilder.storeMessage("DELETE", key));
        }
    }

    public boolean transferFile(String key) {
        var value = this.getFile(key);
        if (value == null) return false;

        Map.Entry<String, MembershipInfo> closestEntry = this.membershipView.getClosestMembershipInfo(key);
        if (closestEntry == null) {
            this.deleteFile(key);
            return true;
        }

        Map<String, MembershipInfo> replicationEntries = this.getReplicationEntries(closestEntry);
        // TODO think of a better way to reduce the number of messages exchange
        String requestMessage = MessageBuilder.storeMessage("PUT", key, value);
        this.redirectService(closestEntry.getValue(), requestMessage);
        for (var nextKey : replicationEntries.keySet())
            this.redirectService(replicationEntries.get(nextKey), requestMessage);
        this.deleteFile(key);
        return true;
    }
    public boolean copyFile(String key, String nodeID) {
        var membershipInfo =  this.getMembershipView().getMembershipTable().getMembershipInfoMap().get(nodeID);
        var value = this.getFile(key);
        if (value == null) return false;
        this.redirectService(membershipInfo, MessageBuilder.storeMessage("PUT", key, value));
        return true;
    }
    public void saveFile(String key, String value) {
        if (FileUtils.saveFile(this.id, key, value)) {
            this.keys.add(key);
        }
    }
    public String getFile(String key) {
        return FileUtils.getFile(this.id, key);
    }
    public void deleteFile(String key) {
        if (FileUtils.deleteFile(this.id, key)) {
            this.keys.remove(key);
        }
    }

    public void redirectService(MembershipInfo node, String requestMessage) {
        try {
            TcpMessager.sendMessage(node.getIP(), node.getPort(), requestMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, MembershipInfo> getReplicationEntries(Map.Entry<String, MembershipInfo> closestEntry) {
        Map<String, MembershipInfo> replicationEntries = new HashMap<>();

        Map.Entry<String, MembershipInfo> lastEntry = closestEntry;
        if (lastEntry == null) return replicationEntries;
        for (int i = 0; i < REPLICATION_FACTOR - 1; i++) {
            lastEntry = this.membershipView.getNextClosestMembershipInfo(lastEntry.getKey());
            if (lastEntry.getKey().equals(closestEntry.getKey())) break;
            replicationEntries.put(lastEntry.getKey(), lastEntry.getValue());
        }

        return replicationEntries;
    }

    // ---------- END OF SERVICE PROTOCOL ---------------

    // -------------- GETS AND SETS ---------------------

    public String getNodeIP() {
        return nodeIP;
    }
    public int getStorePort() {
        return this.storePort;
    }
    private String getNodeIPPort() {
        return this.nodeIP + ":" + this.storePort;
    }
    public String getId() {
        return this.id;
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
    public Set<String> getAlreadySent() {
        return this.alreadySent;
    }
    public Map.Entry<String, MembershipInfo> getClosestMembershipInfo(String keyHashed) {
        return this.membershipView.getClosestMembershipInfo(keyHashed);
    }
    public Map.Entry<String, MembershipInfo> getNextClosestMembershipInfo(String keyHashed) {
        return this.membershipView.getNextClosestMembershipInfo(keyHashed);
    }
    public int getClusterSize() {
        return this.membershipView.getMembershipTable().getMembershipInfoMap().size();
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
        return this.membershipView.isOnline(this.id);
    }

    public void startMSCounter(){
        // check if counter is stored in NV-memory
        String storedCounter = this.getFile("membershipCounter");
        this.membershipCounter = storedCounter == null ? -1 : Integer.parseInt(storedCounter.trim());
    }

    public void incrementMSCounter(){
        this.membershipCounter++;
        FileUtils.saveFile(this.id, "membershipCounter", String.valueOf(this.membershipCounter));
    }

    public void saveLogs(){
        FileUtils.saveFile(this.id, "membershipLogs", this.membershipView.getMembershipLog().toString());
    }

    public void loadLogs(){
        String storedLogs = this.getFile("membershipLogs");
        if (storedLogs == null) {
            System.out.println("No previous logs found...\n");
            return;
        }

        // Compare current logs with stored logs
        MembershipLog membershipLog = new MembershipLog();
        for (String log : storedLogs.split("\n")) {
            membershipLog.addMembershipInfo(new MembershipLogRecord(log));
        }
        this.membershipView.setMembershipLog(membershipLog);
    }
}
