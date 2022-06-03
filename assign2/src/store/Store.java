package store;

import membership.*;

import static messages.Message.ackMessage;
import static messages.MulticastMessager.*;
import static messages.MessageStore.leaveMessage;
import static utils.FileUtils.sub;

import messages.Message;
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
    public static final int ALARM_PERIOD = 10000; // Milliseconds
    public static final int REPLICATION_FACTOR = 3; // Including coordinator

    private final InetAddress mcastAddr;
    private final int mcastPort;
    private final String nodeIP;
    private final int storePort;
    private final String id;

    private final Set<String> keys;
    private int membershipCounter;
    private final MembershipView membershipView;
    private double sendMembershipProbability;

    private String lastSent;
    private String smallestOnline;
    private final PendingRequests pendingRequests;


    private final NetworkInterface networkInterface;
    private final InetSocketAddress inetSocketAddress;
    private final DatagramSocket sndDatagramSocket;
    private DatagramSocket rcvDatagramSocket;

    private Future<?> listenerMcastFuture;
    private final ExecutorService executor;
    private ScheduledExecutorService executorTimerTask;

    public static void main(String[] args) throws RemoteException {
        Store store = parseArgs(args);

        Registry registry = LocateRegistry.getRegistry();
        registry.rebind(store.nodeIP+":"+store.storePort, store);

        // If the store went down due to a crash then automatically join on start up
        if (store.isJoined()) store.execute(new StoreCrashJoinThread(store));

        try (ServerSocket serverSocket = new ServerSocket(store.storePort, 0, InetAddress.getByName(store.nodeIP))){
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Main connection accepted");

                store.executor.execute(new DispatcherThread(socket, store));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String usage() {
        return "Usage:\n\t java Store.Store <IP_mcast_addr> <IP_mcast_port> <node_id>  <Store_port>";
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
        this.membershipView = new MembershipView(new MembershipTable(), new MembershipLog());
        this.sendMembershipProbability = 1.0;
        this.pendingRequests = new PendingRequests();

        this.id = HashUtils.getHashedSha256(this.getNodeIPPort());
        System.out.println("ID: " + id);
        FileUtils.createRoot();
        FileUtils.createDirectory(this.id);

        this.startMSCounter();
        this.loadLogs();
        this.loadKeys();

        try {
            this.sndDatagramSocket = new DatagramSocket();
            this.networkInterface = NetworkInterface.getByName("loopback");
            this.inetSocketAddress = new InetSocketAddress(this.mcastAddr, this.mcastPort);

            if (this.networkInterface != null) {
                this.sndDatagramSocket.setOption(StandardSocketOptions.IP_MULTICAST_IF, this.networkInterface);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.executor = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors());
    }

    // ---------- MEMBERSHIP PROTOCOL -------------

    @Override
    public boolean join() {
        if (this.isJoined()) {
            System.out.println("This node already belongs to a multicast group");
            return false;
        }
        try {
            System.out.println("Joining...");
            ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getByName(this.nodeIP));
            this.incrementMSCounter();
            this.updateMembershipView(this.id, this.nodeIP, this.storePort, this.membershipCounter);

            MembershipCollector.collect(serverSocket, this);
            initMcastReceiver();
            this.listenerMcastFuture = this.executor.submit(new ListenerMcastThread(this));

            this.selectAlarmer(false, null); // Decide if this node should trigger the alarm

        } catch (Exception e) {
            System.out.println("Failure joining multicast group " + this.mcastAddr + ":" + this.mcastPort);
            System.out.println(e.getMessage());
        }
        return true;
    }

    @Override
    public boolean leave() {
        if (!this.isJoined()) {
            System.out.println("This node does not belong to a multicast group");
            return false;
        }
        try {
            System.out.println("Leaving...");
            this.incrementMSCounter();
            this.updateMembershipView(this.id, this.nodeIP, this.storePort, this.membershipCounter);

            transferOwnershipOnLeave();
            transferPendingRequests();

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

    public void rejoin() {
        try {
            System.out.println("Rejoining...");
            ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getByName(this.nodeIP));
            this.membershipView.setMembershipTable(new MembershipTable(this.id, new MembershipInfo(this.nodeIP, this.storePort)));
            MembershipCollector.collectLight(serverSocket, this, false);
            initMcastReceiver();
            this.listenerMcastFuture = this.executor.submit(new ListenerMcastThread(this));

            this.selectAlarmer(false, null); // Decide if this node should trigger the alarm

            this.transferOwnershipOnRejoin();
        }
        catch (IOException e) {throw new RuntimeException(e);}
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

    public void updateMembershipView(String id, String ip, int port, int membershipCounter) {
        var oldLogs = new ArrayList<>(this.membershipView.getMembershipLog().getLogs());
        this.membershipView.updateMembershipInfo(id, ip, port, membershipCounter);
        posUpdate(oldLogs, false);
        this.timerTask();
    }
    public void updateMembershipView(MembershipTable membershipTable, MembershipLog membershipLog) {
        var oldLogs = new ArrayList<>(this.membershipView.getMembershipLog().getLogs());
        this.membershipView.merge(membershipTable, membershipLog);
        int nChanges = posUpdate(oldLogs, true);
        this.updateSendMembershipProbability(nChanges);
    }
    public void mergeMembershipViews(Map<String, MembershipView> membershipViews) {
        var oldLogs = new ArrayList<>(this.membershipView.getMembershipLog().getLogs());
        this.membershipView.mergeMembershipViews(membershipViews);
        int nChanges = posUpdate(oldLogs, false);
        this.updateSendMembershipProbability(nChanges);
    }
    private int posUpdate(List<MembershipLogRecord> oldLogs, boolean membership) {
        System.out.println(this.getMembershipView());
        var changes = this.membershipView.getMembershipLog().changes(new MembershipLog(oldLogs));
        if (!changes.isEmpty()) {
            if (membership) {
                System.out.println("Transferring ownership on membership message");
                for (var record : changes) if (record.getCounter() % 2 == 0) this.transferOwnershipOnJoin(record.getNodeID());
            }
            this.lastSent = null;
            this.saveLogs();
        }
        return changes.size();
    }

    private void timerTask() {
        // Compare hashedID with the smaller hashedID online in the cluster view
        boolean smaller = this.isOnline() && id.equals(this.smallestOnline);
        boolean shouldAlarm = this.shouldSendMembershipMessage();
        synchronized (this) {
            if (this.executorTimerTask == null && smaller) {
                if (!shouldAlarm) return;
                System.out.println("Alarm set");
                this.executorTimerTask = Executors.newScheduledThreadPool(1);
                this.executorTimerTask.scheduleAtFixedRate(new AlarmThread(this), 0, ALARM_PERIOD, TimeUnit.MILLISECONDS);
            }
            else if (this.executorTimerTask != null && (!smaller || !shouldAlarm)) {
                System.out.println("Alarm canceled");
                this.executorTimerTask.shutdown();
                try { this.executorTimerTask.awaitTermination(1, TimeUnit.SECONDS);}
                catch (InterruptedException e) {System.out.println("Alarm shutdown interrupted");}

                if (!this.executorTimerTask.isTerminated()) this.executorTimerTask.shutdownNow();
                this.executorTimerTask = null;
            }
        }
    }
    public void selectAlarmer(boolean increment, String nodeID) {
        if (nodeID != null) {
            if (nodeID.compareTo(smallestOnline) < 0) smallestOnline = nodeID;
        } else if (increment) {
            smallestOnline = this.membershipView.getMembershipTable()
                    .getNextClosestMembershipInfo(smallestOnline).getKey();
        } else {
            smallestOnline = this.membershipView.getMembershipTable().getSmallestMembershipNode();
        }
        this.timerTask();
    }

    // ---------- END OF MEMBERSHIP PROTOCOL ---------------


    // ---------- SERVICE PROTOCOL -------------

    @Override
    public String put(String key, String value) {
        if (key == null) return ackMessage("FAILURE - Key \"null\" doesn't exist");

        String response;
        var closestNode = this.getClosestMembershipInfo(key);
        var preferenceList = this.getPreferenceList(key);
        if (closestNode.getKey().equals(this.id)) {
            if (FileUtils.saveFile(this.id, key, value)) {
                this.keys.add(key);
                response = ackMessage("SUCCESS");
            }
            else {
                return ackMessage("FAILURE - Couldn't save the file");
            }

            String replicaPutMessage = MessageStore.replicaPutMessage(key, value);
            for (var replica : preferenceList) {
                if (replica.equals(this.id)) continue;
                this.executor.execute(new OperationReplicatorThread(this, replica, replicaPutMessage));
            }
        }
        else {
            response = this.redirect(closestNode.getValue(), MessageStore.putMessage(key, value));
            if (response == null) { // Assume coordination
                String replicaPutMessage = MessageStore.replicaPutMessage(key, value);
                for (var replica : preferenceList) {
                    if (replica.equals(this.id)) continue;
                    this.executor.execute(new OperationReplicatorThread(this, replica, replicaPutMessage));
                }
                response = ackMessage("SUCCESS");
            }
        }
        return response;
    }
    @Override
    public String get(String key) {
        if (key == null) return ackMessage("FAILURE - Key \"null\" doesn't exist");
        if (this.keys.contains(key)) {
            return ackMessage(FileUtils.getFile(this.id, key));
        }

        var preferenceList = getPreferenceList(key);
        String requestMessage = MessageStore.replicaGetMessage(key);
        for (var nodeKey : preferenceList) {
            if (nodeKey.equals(this.id)) continue;
            MembershipInfo node = this.getMembershipInfo(nodeKey);
            try (Socket socket = new Socket(node.getIP(), node.getPort())) {
                TcpMessager.sendMessage(socket, requestMessage);
                socket.setSoTimeout(2000);
                String resp = TcpMessager.receiveMessage(socket);
                if (new Message(resp).getBody().startsWith("FAILURE -")) continue; // If a node has not the key try other replica
                return resp;
            }  catch (IOException e) {
                System.out.println("Node with the file is unreachable");
            }
        }
        return ackMessage("FAILURE - Couldn't find the file required");
    }
    @Override
    public String delete(String key) {
        if (key == null) return ackMessage("FAILURE - Key \"null\" doesn't exist");
        String response;
        var closestNode = this.getClosestMembershipInfo(key);
        var preferenceList = this.getPreferenceList(key);

        if (closestNode.getKey().equals(this.id)) {
            if (this.keys.contains(key)) {
                if (!FileUtils.deleteFile(this.id, key)) {
                    return ackMessage("FAILURE - Couldn't delete the file");
                }
                this.keys.remove(key);
            }
            response = ackMessage("SUCCESS");

            String delReplicaMessage = MessageStore.replicaDelMessage(key);
            for (var replica : preferenceList) {
                if (replica.equals(this.id)) continue;
                this.executor.execute(new OperationReplicatorThread(this, replica, delReplicaMessage));
            }
        }
        else {
            response = this.redirect(closestNode.getValue(), MessageStore.deleteMessage(key));
            if (response == null) { // Assume coordination
                String replicaDelMessage = MessageStore.replicaDelMessage(key);
                for (var replica : preferenceList) {
                    if (replica.equals(this.id)) this.replicaDel(key);
                    else this.executor.execute(new OperationReplicatorThread(this, replica, replicaDelMessage));
                }
                response = ackMessage("SUCCESS");
            }
        }
        return response;
    }

    public String replicaPut(String key, String value) {
        if (FileUtils.saveFile(this.id, key, value)) {
            this.keys.add(key);
            return ackMessage("SUCCESS");
        }
        return ackMessage("FAILURE - Couldn't save the file");
    }
    public String replicaDel(String key) {
        if (!this.keys.contains(key)) return ackMessage("Not exists");
        if (FileUtils.deleteFile(this.id, key)) {
            this.keys.remove(key);
            return ackMessage("SUCCESS");
        }
        return ackMessage("FAILURE - Couldn't delete the file");
    }
    public String replicaGet(String key) {
        if (this.keys.contains(key)) {
            return ackMessage(FileUtils.getFile(this.id, key));
        }
        return ackMessage("FAILURE - Couldn't find the file required");
    }

    public void transfer(String nodeID, String key, boolean delete) {
        String value = FileUtils.getFile(this.id, key);
        if (value == null) return;
        String message = MessageStore.replicaPutMessage(key, value);
        String resp = this.redirect(getMembershipInfo(nodeID), message);
        if (resp == null) this.addPendingRequest(nodeID, message);
        if (delete) {
            if (FileUtils.deleteFile(this.id, key)) this.keys.remove(key);
        }
        System.out.println("Key " + sub(key) + " transferred to node " + sub(nodeID) + (delete ? " with deletion" : ""));
    }

    public String redirect(MembershipInfo node, String requestMessage) {
        final int MAX_TRIES = 3;
        for (int i = 0; i < MAX_TRIES; i++) {
            try { return TcpMessager.sendAndReceiveMessage(node.getIP(), node.getPort(), requestMessage, 500); }
            catch (SocketTimeoutException e) { System.out.println("Message response missed"); }
            catch (ConnectException e) { System.out.println("Connection refused"); }
            catch (IOException e) { e.printStackTrace(); }
        }
        System.out.println("Node " + node.getIP() + " is down");
        return null;
    }

    public void transferOwnershipOnJoin(String nodeID) {
        if (this.getClusterSize() <= 1) return;
        var keysCopy = new HashSet<>(this.getKeys());
        for (String key : keysCopy) {
            var preferenceList = this.getPreferenceList(key);
            // If nodes are less than replication factor and this node was the closest before new node arrival copy file
            if (this.getClusterSize() <= REPLICATION_FACTOR) {
                var prevClosestNode = preferenceList.stream()
                        .filter(nodeKey -> !nodeKey.equals(nodeID)).findFirst().get();
                if (prevClosestNode.equals(this.id)) {
                    this.transfer(nodeID, key, false);
                }
            }
            else {
                if (!preferenceList.contains(nodeID) || preferenceList.contains(this.id))
                    continue;
                if (this.keys.contains(key) && !preferenceList.contains(this.id)) {
                    this.transfer(nodeID, key, true);
                }
            }
        }
    }
    private void transferOwnershipOnLeave() {
        var keysCopy = new HashSet<>(this.keys);
        for (String key : keysCopy) {
            var preferenceList = this.getPreferenceList(key);
            if (preferenceList.size() < REPLICATION_FACTOR) this.replicaDel(key);
            else this.transfer(preferenceList.get(REPLICATION_FACTOR-1), key, true);
        }
    }
    private void transferOwnershipOnRejoin() {
        if (this.getClusterSize() <= 1) return;

        var keysCopy = new HashSet<>(this.getKeys());
        for (String key : keysCopy) {
            var preferenceList = this.getPreferenceList(key);
            if (!preferenceList.contains(this.id)) {
                for (var replica : preferenceList) {
                    this.transfer(replica, key, false);
                }
                this.replicaDel(key);
            }
        }
    }

    private void transferPendingRequests() {
        for (int i = 0; i < this.getClusterSize(); i++) {
            var next = this.membershipView.getNextClosestMembershipInfo(this.id);
            if (!this.pendingRequests.hasPendingRequests(next.getKey())) {
                System.out.println("Transferring pending requests to " + next.getValue());
                this.redirect(next.getValue(), MessageStore.pendingRequestsMessage(this.pendingRequests.getPendingMessages()));
                break;
            }
        }
        this.pendingRequests.clear();
    }


    // ---------- END OF SERVICE PROTOCOL ---------------

    // ----------- UTILS ---------------

    public boolean isOnline() {
        return this.membershipView.isInMembershipTable(this.id);
    }
    private boolean isJoined() {
        return this.membershipCounter % 2 == 0;
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

    public void addPendingRequest(String nodeID, String message) {
        this.pendingRequests.addRequest(nodeID, message);
    }
    public void emptyPendingRequests(String nodeID) {
        this.pendingRequests.empty(nodeID);
    }
    public void applyPendingRequests(String nodeID) {
        for (var message : this.pendingRequests.getNodePendingRequests(nodeID)){
            this.redirect(this.getMembershipInfo(nodeID), message);
        }
    }
    public void mergePendingRequests(Message message) {
        var receivedRequests = MessageStore.parsePendingRequestsMessage(message);
        for (var key : receivedRequests.keySet()) {
            if (this.pendingRequests.hasPendingRequests(key)) {
                this.pendingRequests.getNodePendingRequests(key).addAll(receivedRequests.get(key));
            } else {
                this.pendingRequests.getPendingMessages().put(key, receivedRequests.get(key));
            }
        }
        System.out.println("Pending requests merged");
    }

    public void updateSendMembershipProbability(double nChanges) {
        if (nChanges == 0) {
            this.sendMembershipProbability = Math.min(this.sendMembershipProbability + 0.1, 1.0);
        } else {
            if (nChanges >= 32) {
                try {
                    ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getByName(this.nodeIP));
                    MembershipCollector.collectLight(serverSocket, this, true);
                } catch (IOException e) {System.out.println("FAILURE - Collecting all membership");}
            }
            this.sendMembershipProbability = Math.max(this.sendMembershipProbability - 0.2, 0.0);
        }
    }
    public boolean shouldSendMembershipMessage() {
        return (this.sendMembershipProbability > 0.5);
    }

    // -------------- END OF UTILS ----------------


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


}
