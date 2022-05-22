package membership;

import java.util.Objects;

public class MembershipLogRecord {
    private final String nodeID;
    private final int counter;

    public MembershipLogRecord(String nodeID, int counter) {
        this.nodeID = nodeID;
        this.counter = counter;
    }
    public MembershipLogRecord(String counterAndId) {
        this.nodeID = getIDFromString(counterAndId);
        this.counter = getCounterFromString(counterAndId);
    }

    private static String getIDFromString(String rec) {
        return rec.split("\\|")[1].trim();
    }
    private static int getCounterFromString(String rec) {
        return Integer.parseInt(rec.split("\\|")[0].trim());
    }

    public String getNodeID() {
        return nodeID;
    }
    public int getCounter() {
        return counter;
    }

    @Override
    public String toString(){
        return counter + "|" + nodeID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MembershipLogRecord that = (MembershipLogRecord) o;
        return counter == that.counter && Objects.equals(nodeID, that.nodeID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeID, counter);
    }
}
