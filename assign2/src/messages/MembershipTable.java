package messages;

import utils.HashUtils;

import static utils.HashUtils.*;

import java.util.*;

public class MembershipTable {

    private TreeMap<String, MembershipInfo> membershipInfoMap;

    public MembershipTable() {
        this.membershipInfoMap = new TreeMap<String, MembershipInfo>();
    }

    public void addMembershipInfo(MembershipInfo mInfo) {
        String hashedId = HashUtils.getHashedSha256(mInfo.getIP());
        this.membershipInfoMap.put(hashedId, mInfo);
    }

    public void removeMembershipInfo(String hashedId) {
        this.membershipInfoMap.remove(hashedId);
    }

    public Set<MembershipInfo> getMembershipInfoList() {
        return new HashSet<>(this.membershipInfoMap.values());
    }

    public TreeMap<String, MembershipInfo> getMembershipInfoMap() {
        return this.membershipInfoMap;
    }

    public String getClosestMembershipInfo(String key) {
        // TODO: binary search to find the IP:PORT of the node closest to the key (comparing the hashIds of the MembershipInfo)
        // if not equal, find the interval that it should be and return the successor
        // extreme case - when it would be after the last one send the first one

        Map.Entry<String, MembershipInfo> closestNode = this.membershipInfoMap.ceilingEntry(key);
        // the key is after the last hashedId in the tree
        if (closestNode == null) {
            // : reassign the closest value as the first one in the tree
            closestNode = this.membershipInfoMap.firstEntry();
        }

        return closestNode.getValue().toString();
    }

    @Override
    public String toString(){
        StringBuilder ret = new StringBuilder();
        for (MembershipInfo entry : this.membershipInfoMap.values()){
            ret.append(entry.toString());
            ret.append("\n");
        }
        return ret.toString();
    }
}
