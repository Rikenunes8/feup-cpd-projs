package membership;

import java.util.ArrayList;
import java.util.List;

public class MembershipLog {
    private final List<MembershipLogRecord> logs;

    public MembershipLog() {
        this.logs = new ArrayList<>();
    }
    public MembershipLog(List<MembershipLogRecord> logs) {
        this.logs = logs;
    }

    public List<MembershipLogRecord> getLogs() {
        return logs;
    }

    public void addMembershipInfo(MembershipLogRecord log){
        var record = this.logs.stream().filter(l -> l.getNodeID().equals(log.getNodeID())).findFirst().orElse(null);
        if (record == null) {
            this.logs.add(log);
        } else if (record.getCounter() < log.getCounter()) {
            this.logs.remove(record);
            this.logs.add(log);
        }
    }

    public List<MembershipLogRecord> last32Logs() {
        return this.lastNLogs(32);
    }

    private List<MembershipLogRecord> lastNLogs(int n) {
        int size = this.logs.size();
        if (size <= n) return this.logs;
        else return this.logs.subList(size-n, size);
    }

    public void mergeLogs(List<MembershipLogRecord> logs) {
        for (var log : logs) {
            this.addMembershipInfo(log);
        }
    }

    public void removeLastRecord() {
        this.logs.remove(this.logs.size()-1);
    }

    public List<MembershipLogRecord> changes(MembershipLog oldMembershipLog) {
        List<MembershipLogRecord> changesList = new ArrayList<>();
        int size1 = this.logs.size();
        int size2 = oldMembershipLog.logs.size();
        if (size2 == 0) {
            changesList.addAll(this.logs);
            return changesList;
        }
        int i = 1;
        // Assuming size2 can't be greater than size1
        while (size2-i >= 0 && !this.logs.get(size1-i).equals(oldMembershipLog.logs.get(size2-i))) {
            changesList.add(this.logs.get(size1-i));
            i++;
        }
        return changesList;
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

    @Override
    public boolean equals(Object obj){
        if(!obj.getClass().equals(this.getClass()))
            return false;

        if(this.logs.size() != ((MembershipLog) obj).getLogs().size())
            return false;

        for(int i = 0; i < this.logs.size(); i++)
            if(!this.logs.get(i).equals(((MembershipLog) obj).getLogs().get(i)))
                return false;

        return true;
    }
}
