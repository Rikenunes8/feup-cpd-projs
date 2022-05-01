import java.util.List;

public class MessageBuilder {
    static final char CR = 0xD;
    static final char LF = 0xA;

    public static String messageJoinLeave(String nodeIP, int port, int membershipCounter) {
        // TODO implement
        return null;
    }

    public static String messageMembership() {
        // TODO implement
        return null;
    }

    private static String buildHeader(List<String> headerLines) {
        StringBuilder stringBuilder = new StringBuilder();

        for (String line : headerLines) {
            stringBuilder.append(line);
            stringBuilder.append(CR);
            stringBuilder.append(LF);
        }
        stringBuilder.append(CR);
        stringBuilder.append(LF);
        return stringBuilder.toString();
    }

}
