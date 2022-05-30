import membership.*;

import static messages.Message.ackMessage;
import static messages.MulticastMessager.*;
import static messages.MessageStore.leaveMessage;
import static utils.FileUtils.sub;

import messages.MessageStore;
import messages.TcpMessager;
import utils.FileUtils;
import utils.HashUtils;

import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.*;
import java.util.*;


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
    private String lastSent;

    private final NetworkInterface networkInterface;
    private final InetSocketAddress inetSocketAddress;
    private final DatagramSocket sndDatagramSocket;
    private DatagramSocket rcvDatagramSocket;

    private Future<?> listenerMcastFuture;
    private ExecutorService executor;
    private ScheduledExecutorService executorTimerTask;

    public static void main(String[] args) throws RemoteException {
        Store store = parseArgs(args);

        Registry registry = LocateRegistry.getRegistry();
        registry.rebind(store.nodeIP+":"+store.storePort, store);

        while (true) {
            try (ServerSocket serverSocket = new ServerSocket(store.storePort, 0, InetAddress.getByName(store.nodeIP))){
                Socket socket = serverSocket.accept();
                System.out.println("Main connection accepted");

                store.executor.execute(new DispatcherThread(socket, store));
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
        this.membershipView = new MembershipView(new MembershipTable(), new MembershipLog());

        this.id = HashUtils.getHashedSha256(this.getNodeIPPort());
        System.out.println("ID: " + id);
        FileUtils.createRoot();
        FileUtils.createDirectory(this.id);

        this.startMSCounter();
        this.loadLogs();
        this.loadKeys();

        String networkInterfaceStr = "loopback"; // TODO
        try {
            this.sndDatagramSocket = new DatagramSocket();
            this.networkInterface = NetworkInterface.getByName(networkInterfaceStr);
            this.inetSocketAddress = new InetSocketAddress(this.mcastAddr, this.mcastPort);

            if (this.networkInterface != null) {
                this.sndDatagramSocket.setOption(StandardSocketOptions.IP_MULTICAST_IF, this.networkInterface);
            }
            // System.out.println("IP_MULTICAST_LOOP: " + this.sndDatagramSocket.getOption(StandardSocketOptions.IP_MULTICAST_LOOP)); // TODO

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.executor = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors());

        // If the store went down due to a crash then automatically join on start up
        if (this.membershipCounter % 2 == 0) this.joinAfterCrash();
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

            MembershipCollector.collect(serverSocket, this);
            initMcastReceiver();
            this.listenerMcastFuture = this.executor.submit(new ListenerMcastThread(this));

            while (!this.isEmptyPendingQueue()) {
                this.removeFromPendingQueue().processMessage();
            }
        } catch (Exception e) {
            System.out.println("Failure joining multicast group " + this.mcastAddr + ":" + this.mcastPort);
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
            this.updateMembershipView(this.id, this.nodeIP, this.storePort, this.membershipCounter);


            var keysCopy = new HashSet<>(this.keys);
            for (String key : keysCopy) {
                var preferenceList = this.getPreferenceList(key);
                if (preferenceList.size() < REPLICATION_FACTOR) this.delReplica(key);
                else this.transfer(preferenceList.get(REPLICATION_FACTOR-1), key, true);
            }

            // Notice cluster members of my leave
            String msg = leaveMessage(this.id, this.nodeIP, this.storePort, this.membershipCounter);
            sendMcastMessage(msg, this.sndDatagramSocket, this.mcastAddr, this.mcastPort);

            endMcastReceiver();
        } catch (Exception e) {
            System.out.println("Failure leaving " + this.mcastAddr + ":" + this.mcastPort);
            System.out.println(e.getMessage());
        }
        return true;
    }

    private void initMcastReceiver() throws IOException {
        this.rcvDatagramSocket = new DatagramSocket(null);
        this.rcvDatagramSocket.setReuseAddress(true);
        this.rcvDatagramSocket.bind(new InetSocketAddress(this.mcastPort));
        this.rcvDatagramSocket.joinGroup(this.inetSocketAddress, this.networkInterface);
    }
    private void endMcastReceiver() throws IOException {
        this.listenerMcastFuture.cancel(true);
        this.rcvDatagramSocket.leaveGroup(this.inetSocketAddress, this.networkInterface);
        this.rcvDatagramSocket = null;
    }
    private void joinAfterCrash() {
        try {
            this.updateMembershipView(this.id, this.nodeIP, this.storePort, this.membershipCounter);
            initMcastReceiver();
            this.executor.execute(new ListenerMcastThread(this));
        }
        catch (IOException e) {throw new RuntimeException(e);}
    }

    public void updateMembershipView(MembershipTable membershipTable, MembershipLog membershipLog) {
        var oldLogs = new ArrayList<>(this.membershipView.getMembershipLog().getLogs());
        this.membershipView.merge(membershipTable, membershipLog);
        posUpdate(oldLogs);
    }
    public void updateMembershipView(String id, String ip, int port, int membershipCounter) {
        var oldLogs = new ArrayList<>(this.membershipView.getMembershipLog().getLogs());
        this.membershipView.updateMembershipInfo(id, ip, port, membershipCounter);
        posUpdate(oldLogs);
    }
    public void mergeMembershipViews(ConcurrentHashMap<String, MembershipView> membershipViews) {
        var oldLogs = new ArrayList<>(this.membershipView.getMembershipLog().getLogs());
        this.membershipView.mergeMembershipViews(membershipViews);
        posUpdate(oldLogs);
    }
    private void posUpdate(List<MembershipLogRecord> oldLogs) {
        if (this.membershipView.getMembershipLog().hasChanged(new MembershipLog(oldLogs))) {
            this.lastSent = null;
            this.saveLogs();
        }
        timerTask();
    }
    private void timerTask() {
        // Compare hashedID with the smaller hashedID in the cluster view
        var infoMap = this.membershipView.getMembershipTable().getMembershipInfoMap();
        boolean smaller = !infoMap.isEmpty() && id.equals(infoMap.firstKey());

        if (this.executorTimerTask == null && smaller) {
            System.out.println("Alarm set");
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
    public String put(String key, String value) {
        String response;
        var closestNode = this.getClosestMembershipInfo(key);
        if (closestNode.getKey().equals(this.id)) {
            if (FileUtils.saveFile(this.id, key, value)) {
                this.keys.add(key);
                response = ackMessage("SUCCESS");
            }
            else {
                return ackMessage("FAILURE - Couldn't save the file");
            }

            // TODO This must be in a thread and guarantee replication
            for (var replicaKey : this.getPreferenceList(key)) {
                if (replicaKey.equals(this.id)) continue;
                String resp = this.redirect(this.getMembershipInfo(replicaKey), MessageStore.replicaMessage(key, value));
            }

        }
        else {
            response = this.redirect(closestNode.getValue(), MessageStore.putMessage(key, value));
        }
        return response;
    }

    @Override
    public String get(String key) {
        if (this.keys.contains(key)) {
            return ackMessage(FileUtils.getFile(this.id, key));
        }

        var preferenceList = getPreferenceList(key);
        String requestMessage = MessageStore.getMessage(key);
        for (var nodeKey : preferenceList) {
            if (nodeKey.equals(this.id)) continue;
            MembershipInfo node = this.getMembershipInfo(nodeKey);
            try (Socket socket = new Socket(node.getIP(), node.getPort())) {
                TcpMessager.sendMessage(socket, requestMessage);
                socket.setSoTimeout(2000);
                return TcpMessager.receiveMessage(socket);
            }  catch (IOException e) {
                System.out.println("Node of get unreachable");
            }
        }

        return ackMessage("FAILURE - Couldn't find the file required");
    }

    @Override
    public String delete(String key) {
        String response;
        var closestNode = this.getClosestMembershipInfo(key);

        if (this.keys.contains(key)) {
            if (!FileUtils.deleteFile(this.id, key)) {
                this.keys.remove(key);
                return ackMessage("FAILURE - Couldn't delete the file");
            }
            this.keys.remove(key);
            response = ackMessage("SUCCESS");

            // TODO This must be in a thread
            for (var replica : this.getPreferenceList(key)) {
                if (replica.equals(this.id)) continue;
                String resp = this.redirect(this.getMembershipInfo(replica), MessageStore.delReplicaMessage(key));
            }
        }
        else {
            response = this.redirect(closestNode.getValue(), MessageStore.deleteMessage(key));
        }
        return response;
    }

    public String replica(String key, String value) {
        if (FileUtils.saveFile(this.id, key, value)) {
            this.keys.add(key);
            return ackMessage("SUCCESS");
        }
        return ackMessage("FAILURE - Couldn't save the file");
    }
    public String delReplica(String key) {
        if (!this.keys.contains(key)) return ackMessage("Not exists");
        if (FileUtils.deleteFile(this.id, key)) {
            this.keys.remove(key);
            return ackMessage("SUCCESS");
        }
        return ackMessage("FAILURE - Couldn't delete the file");
    }

    public void transfer(String nodeID, String key, boolean delete) {
        String value = FileUtils.getFile(this.id, key);
        if (value == null) return;
        String response = this.redirect(getMembershipInfo(nodeID), MessageStore.replicaMessage(key, value));
        if (delete) FileUtils.deleteFile(this.id, key);
        System.out.println("Key " + sub(key) + " transferred to node " + sub(nodeID) + (delete ? " with deletion" : ""));
    }

    public String redirect(MembershipInfo node, String requestMessage) {
        try { return TcpMessager.sendAndReceiveMessage(node.getIP(), node.getPort(), requestMessage); }
        catch (ConnectException e) { System.out.println("Node " + nodeIP + " is down"); } // TODO do something
        catch (IOException e) { e.printStackTrace(); }
        return null;
    }

    // ---------- END OF SERVICE PROTOCOL ---------------

    // -------------- GETS AND SETS ---------------------

    public String getId() {
        return this.id;
    }
    public String getNodeIP() {
        return this.nodeIP;
    }
    private String getNodeIPPort() {
        return this.nodeIP + ":" + this.storePort;
    }
    public InetAddress getMcastAddr() {
        return this.mcastAddr;
    }
    public int getStorePort() {
        return this.storePort;
    }
    public int getMcastPort() {
        return this.mcastPort;
    }
    public int getMembershipCounter() {
        return this.membershipCounter;
    }
    public int getClusterSize() {
        return this.membershipView.getMembershipTable().getMembershipInfoMap().size();
    }
    public DatagramSocket getRcvDatagramSocket() {
        return this.rcvDatagramSocket;
    }
    public DatagramSocket getSndDatagramSocket() {
        return this.sndDatagramSocket;
    }
    public MembershipView getMembershipView() {
        return this.membershipView;
    }
    public String getLastSent() {
        return this.lastSent;
    }
    public Set<String> getKeys() {
        return this.keys;
    }
    public List<String> getPreferenceList(String key) {
        List<String> preferenceList = new ArrayList<>();
        var closestEntry = this.getClosestMembershipInfo(key);
        if (closestEntry == null) return preferenceList;
        String closestKey = closestEntry.getKey();
        preferenceList.add(closestKey);
        String lastEntry = closestKey;
        for (int i = 0; i < REPLICATION_FACTOR - 1; i++) {
            lastEntry = this.membershipView.getNextClosestMembershipInfo(lastEntry).getKey();
            if (lastEntry.equals(closestKey)) break;
            preferenceList.add(lastEntry);
        }
        return preferenceList;
    }
    public MembershipInfo getMembershipInfo(String nodeID) {
        return this.membershipView.getMembershipTable().getMembershipInfoMap().getOrDefault(nodeID, null);
    }
    public Map.Entry<String, MembershipInfo> getClosestMembershipInfo(String keyHashed) {
        return this.membershipView.getClosestMembershipInfo(keyHashed);
    }

    public void setLastSent(String nodeID) {
        this.lastSent = nodeID;
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

    public void execute(Runnable runnable) {
        this.executor.execute(runnable);
    }

    public void startMSCounter(){
        // check if counter is stored in NV-memory
        String storedCounter = FileUtils.getFile(this.id, "membershipCounter");
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
        String storedLogs = FileUtils.getFile(this.id, "membershipLogs");
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

    public void loadKeys() {
        List<String> files = FileUtils.listFiles(this.id);
        List<String> fileKeys = files.stream()
                .filter(name -> !name.startsWith("membership"))
                .map(name -> name.split("\\.")[0]).toList();
        this.keys.addAll(fileKeys);
    }
}
