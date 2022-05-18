package membership;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MembershipInfo {
    //TODO: GUARDAR DATA DA INFORMAÇÂO
    //TODO: GUARDAR ESTADO DO NÓ (ADORMECIDO OU NÂO)

    private final String ip;
    private final int port;

    public MembershipInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }
    public MembershipInfo(String ipAndPort) {
        ipAndPort = ipAndPort.trim();
        Pattern p = Pattern.compile("[^\\d\\.]");
        Matcher m = p.matcher(ipAndPort);
        if (m.find()) {
            this.ip = ipAndPort.substring(0, m.start());
            this.port = Integer.parseInt(ipAndPort.substring(m.start()+1));
        }
        else {
            this.ip = "0.0.0.0";
            this.port = 0;
        }
    }

    public String getIP() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    @Override
    public String toString(){
        return this.ip + ":" + this.port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MembershipInfo that = (MembershipInfo) o;
        return this.port == that.port && Objects.equals(this.ip, that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }
}
