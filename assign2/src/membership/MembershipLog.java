package membership;

import java.util.ArrayList;
import java.util.List;

public class MembershipLog {
    private final List<MembershipLogRecord> logs; // TODO is this the better structure?

    public MembershipLog() {
        this.logs = new ArrayList<>();
    }
    public MembershipLog(List<MembershipLogRecord> logs) {
        this.logs = logs;
    }

    public void addMembershipInfo(MembershipLogRecord log){
        var record = this.logs.stream().filter(l -> l.getNodeIP().equals(log.getNodeIP())).findFirst().orElse(null);
        if (record == null) {
            this.logs.add(log);
        } else if (record.getCounter() < log.getCounter()) {
            this.logs.remove(record);
            this.logs.add(log);
        }
    }

    public void removeMembershipInfo(MembershipLogRecord log){
        this.logs.remove(log);
    }

    public List<MembershipLogRecord> last32Logs() {
        return this.lastNLogs(32);
    }

    private List<MembershipLogRecord> lastNLogs(int n) {
        int size = this.logs.size();
        if (size < n) return this.logs;
        else return this.logs.subList(size-n, size);
    }

    public void mergeLogs(List<MembershipLogRecord> logs) {
        for (var log : logs) {
            this.addMembershipInfo(log);
        }
    }


    @Override
    public String toString(){
        StringBuilder ret = new StringBuilder();
        for (MembershipLogRecord entry : logs){
            ret.append(entry.toString());
            ret.append("\n");
        }
        return ret.toString();
    }
}
