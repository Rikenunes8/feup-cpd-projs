package membership;

import java.util.Map;

public class MembershipView {
    MembershipTable membershipTable;
    MembershipLog membershipLog;

    public MembershipView(MembershipTable membershipTable, MembershipLog membershipLog) {
        this.membershipTable = membershipTable;
        this.membershipLog = membershipLog;
    }

    public MembershipTable getMembershipTable() {
        return membershipTable;
    }
    public MembershipLog getMembershipLog() {
        return membershipLog;
    }
    public void setMembershipTable(MembershipTable membershipTable) {
        this.membershipTable = membershipTable;
    }
    public void setMembershipLog(MembershipLog membershipLog) {
        this.membershipLog = membershipLog;
    }

    public Map.Entry<String, MembershipInfo> getClosestMembershipInfo(String keyHashed) {
        return this.membershipTable.getClosestMembershipInfo(keyHashed);
    }
    public Map.Entry<String, MembershipInfo> getNextClosestMembershipInfo(String keyHashed) {
        return this.membershipTable.getNextClosestMembershipInfo(keyHashed);
    }

    public boolean isOnline(String keyHashed) {
        return this.membershipTable.getMembershipInfoMap().containsKey(keyHashed);
    }

    public void mergeMembershipViews(Map<String, MembershipView> membershipViews) {
        for (var pair : membershipViews.entrySet()) {
            MembershipView view = pair.getValue();
            this.merge(view.getMembershipTable(), view.getMembershipLog());
        }
    }

    public void merge(MembershipTable membershipTable, MembershipLog membershipLog) {
        this.membershipTable.mergeTable(membershipTable);
        this.membershipLog.mergeLogs(membershipLog.last32Logs());
        this.synchronizeTable();
    }

    public void synchronizeTable() {
        for (var log : this.membershipLog.getLogs()) {
            if (log.getCounter() % 2 == 0) continue; // TODO if a node was supposed to be in cluster should do something
            if (this.membershipTable.getMembershipInfoMap().containsKey(log.getNodeID())) {
                this.membershipTable.removeMembershipInfo(log.getNodeID());
            }
        }
    }

    public void updateMembershipInfo(String id, String ip, int port, int membershipCounter) {
        if (membershipCounter % 2 == 0)
            this.membershipTable.addMembershipInfo(id, new MembershipInfo(ip, port));
        else
            this.membershipTable.removeMembershipInfo(id);

        this.membershipLog.addMembershipInfo(new MembershipLogRecord(id, membershipCounter));
    }

    @Override
    public String toString() {
        return "+----\n" + this.getMembershipTable().toString() + "-----\n" + this.getMembershipLog().toString() + "+-----\n";
    }
}
