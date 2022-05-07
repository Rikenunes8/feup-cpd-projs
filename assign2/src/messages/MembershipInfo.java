package messages;

public class MembershipInfo {

    private String IP;
    private int Port;

    @Override
    public String toString(){
        return IP + ": " + Port;
    }

    public static String getIPFromString(String rec){
        return rec.substring(rec.length() - 7);
    }

    public static String getPortFromString(String rec){
        return rec.substring(rec.length() - 5, rec.length()-1);
    }

}
