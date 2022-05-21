package membership;

import utils.HashUtils;

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

    public MembershipInfo getClosestMembershipInfo(String keyHashed) {
        return this.membershipTable.getClosestMembershipInfo(keyHashed);
    }

    public boolean isOnline(String keyHashed) {
        return this.membershipTable.hasStore(keyHashed);
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
            if (log.getCounter() % 2 == 0) continue; // TODO if a node was supposed to be in cluster do something
            var invTable = this.membershipTable.getMembershipInfoList();
            if (invTable.containsKey(log.getNodeIP())) {
                this.membershipTable.removeMembershipInfo(invTable.get(log.getNodeIP()));
            }
        }
    }

    public void updateMembershipInfo(String nodeIP, int port, int membershipCounter) {
        if (membershipCounter % 2 == 0)
            this.membershipTable.addMembershipInfo(HashUtils.getHashedSha256(HashUtils.joinIpPort(nodeIP, port)), new MembershipInfo(nodeIP, port));
        else
            this.membershipTable.removeMembershipInfo(HashUtils.getHashedSha256(HashUtils.joinIpPort(nodeIP, port)));

        this.membershipLog.addMembershipInfo(new MembershipLogRecord(nodeIP, membershipCounter));
    }
}
