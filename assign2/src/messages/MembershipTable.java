package messages;

import java.util.List;

public class MembershipTable {
    private List<MembershipInfo> membershipInfoList;

    public void addMembershipInfo(MembershipInfo mInfo){
        membershipInfoList.add(mInfo);
    }

    public void removeMembershipInfo(MembershipInfo mInfo){
        // This could cause some problems, maybe need to remove at index
        membershipInfoList.remove(mInfo);
    }

    public List<MembershipInfo> getMembershipInfoList() {
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
