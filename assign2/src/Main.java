import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Main {

    public static void main(String[] args) {
	// write your code here
        String test = "abscd";
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] hash1 = digest.digest(test.getBytes(StandardCharsets.UTF_8));

        String keyHased = String.format("%064x", new BigInteger(1, digest.digest(test.getBytes(StandardCharsets.UTF_8))));
        System.out.println(keyHased);

    }
}
