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
        ipAndCounter = ipAndCounter.trim();
        Pattern p = Pattern.compile("[^\\d\\.]");
        Matcher m = p.matcher(ipAndCounter);
        if (m.find()) {
            this.nodeIP = ipAndCounter.substring(0, m.start());
            this.counter = Integer.parseInt(ipAndCounter.substring(m.start()+1));
        }
        else {
            this.nodeIP = "0.0.0.0";
            this.counter = -1;
        }
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
