package messages;

import java.util.HashSet;
import java.util.Set;

public class MembershipTable {

    private Set<MembershipInfo> membershipInfoList;

    public MembershipTable() {
        this.membershipInfoList = new HashSet<>();
    }

    public void addMembershipInfo(MembershipInfo mInfo){
        membershipInfoList.add(mInfo);
    }

    public void removeMembershipInfo(MembershipInfo mInfo){
        membershipInfoList.remove(mInfo);
    }

    public Set<MembershipInfo> getMembershipInfoList() {
        return membershipInfoList;
    }

    @Override
    public String toString(){
        StringBuilder ret = new StringBuilder();
        for (MembershipInfo entry : membershipInfoList){
            ret.append(entry.toString());
            ret.append("\n");
        }
        return ret.toString();
    }
}
