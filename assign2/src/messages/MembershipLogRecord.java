package messages;

public class MembershipLogRecord {
    private String nodeIP;
    private int counter;

    public MembershipLogRecord(String nodeIP, int counter) {
        this.nodeIP = nodeIP;
        this.counter = counter;
    }

    public String getNodeIP() {
        return nodeIP;
    }
    public int getCounter() {
        return counter;
    }

    @Override
    public String toString(){
        return "NodeIP: " + nodeIP + ", MembershipCounter: " + counter;
    }
}
