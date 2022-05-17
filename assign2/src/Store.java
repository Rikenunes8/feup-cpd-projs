import membership.MembershipInfo;
import membership.MembershipLog;
import membership.MembershipLogRecord;
import membership.MembershipTable;

import static messages.MessageBuilder.messageJoinLeave;
import static messages.MulticastMessager.*;

import java.io.*;
import java.net.*;
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
    private MembershipTable membershipTable;

    private final NetworkInterface networkInterface;
    private final InetSocketAddress inetSocketAddress;
    private DatagramSocket rcvDatagramSocket;
    private DatagramSocket sndDatagramSocket;

    private ExecutorService executorMcast;
    private ExecutorService executorMembershipReceiver;

    // TODO everything
    public static void main(String[] args) {
        Store store = parseArgs(args);

        //ExecutorService executor = Executors.newWorkStealingPool(8);
        ExecutorService executor = Executors.newFixedThreadPool(8);

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

            this.executorMcast.execute(new MembershipCollectorThread(serverSocket, this));
            initMcastReceiver();
            this.executorMcast.execute(new ListenerMcastThread(this));


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
            String msg = messageJoinLeave(this.nodeIP, this.storePort, this.membershipCounter, 0);
            sendMcastMessage(msg, this.rcvDatagramSocket, this.mcastAddr, this.mcastPort);

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
            System.out.println("Forcing shut down");
            executorMcast.shutdownNow();
            System.out.println("Shut down complete");
        }

        this.rcvDatagramSocket.leaveGroup(this.inetSocketAddress, this.networkInterface);
        this.rcvDatagramSocket = null;
    }


    private void initializeMembership() {
        // TODO
    }

    public void addJoinLeaveEvent(String nodeIP, int port, int membershipCounter) {
        if (membershipCounter % 2 == 0)
            this.membershipTable.addMembershipInfo(new MembershipInfo(nodeIP, port));
        else
            this.membershipTable.removeMembershipInfo(new MembershipInfo(nodeIP, port));

        this.membershipLog.addMembershipInfo(new MembershipLogRecord(nodeIP, membershipCounter));
    }

    public String getNodeIP() {
        return nodeIP;
    }
    public int getStorePort() {
        return this.storePort;
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
    public MembershipLog getMembershipLog() {
        return membershipLog;
    }
    public MembershipTable getMembershipTable() {
        return membershipTable;
    }

    public void setMembershipLog(MembershipLog membershipLog) {
        this.membershipLog = membershipLog;
    }
    public void setMembershipTable(MembershipTable membershipTable) {
        this.membershipTable = membershipTable;
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