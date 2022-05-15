package membership;

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
}
