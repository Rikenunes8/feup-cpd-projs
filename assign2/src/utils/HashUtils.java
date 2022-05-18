package utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

    public static String getHashedSha256(String value) {
        try {
            // TODO check if it is thread safe
            MessageDigest encoder = MessageDigest.getInstance("SHA-256");

            return String.format("%064x", new BigInteger(1,
                encoder.digest(value.getBytes(StandardCharsets.UTF_8))));

        } catch (NoSuchAlgorithmException e) {
            System.out.println("SHA-256 doesn't exist!");
            return null;
        }
    }
}
