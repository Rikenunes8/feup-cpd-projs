package membership;

import java.util.ArrayList;
import java.util.List;

public class MembershipLog {
    private List<MembershipLogRecord> logs; // TODO is this the better structure?

    public MembershipLog() {
        this.logs = new ArrayList<>();
    }

    public void addMembershipInfo(MembershipLogRecord mLogR){
        if (!this.logs.contains(mLogR)) {
            this.logs.add(mLogR);
        }
    }

    public void removeMembershipInfo(MembershipLogRecord mLogR){
        // TODO This could cause some problems, maybe need to remove at index
        this.logs.remove(mLogR);
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
