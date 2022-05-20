package membership;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MembershipLogRecord {
    private final String nodeIP;
    private final int counter;

    public MembershipLogRecord(String nodeIP, int counter) {
        this.nodeIP = nodeIP;
        this.counter = counter;
    }
    public MembershipLogRecord(String ipAndCounter) {
        this.nodeIP = getIPFromString(ipAndCounter);
        this.counter = getCounterFromString(ipAndCounter);
    }

    private static String getIPFromString(String rec) {
        return rec.split("\\|")[0].trim();
    }
    private static int getCounterFromString(String rec) {
        return Integer.parseInt(rec.split("\\|")[1].trim());
    }

    public String getNodeIP() {
        return nodeIP;
    }
    public int getCounter() {
        return counter;
    }

    @Override
    public String toString(){
        return nodeIP + "|" + counter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MembershipLogRecord that = (MembershipLogRecord) o;
        return counter == that.counter && Objects.equals(nodeIP, that.nodeIP);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeIP, counter);
    }
}
