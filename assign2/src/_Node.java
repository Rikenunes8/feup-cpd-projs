import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class _Node implements IService {

    private final String id;
    MessageDigest encoder;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getIdHashed() {
        byte[] hash1 = encoder.digest(id.getBytes(StandardCharsets.UTF_8));

        return String.format("%064x", new BigInteger(1, hash1));
    }

    // Constructors
    public _Node(String id){
        this.id = id;
        try {
            encoder = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("SHA-256 doesn't exist!");
        }
    }

    @Override
    public String put(String key, String value) {
        try {
            String keyHashed = String.format("%064x", new BigInteger(1, encoder.digest(key.getBytes(StandardCharsets.UTF_8))));

            File keyFile = new File("network/" + this.getIdHashed() + "/" + keyHashed);

            if (!keyFile.exists()) if(!keyFile.createNewFile()) {
                System.out.println("An error occurred when creating keyFile in node " + this.getIdHashed());
                return null;
            }

            if (!keyFile.canWrite()){
                System.out.println("Permission denied! Can not write value of key " + key + " in node " + this.getIdHashed());
                return null;
            }

            FileWriter keyFileWriter = new FileWriter("network/" + this.getIdHashed() + "/" + keyHashed, false);

            keyFileWriter.write(value);
            keyFileWriter.close();

            System.out.println("Successfully saved key-value pair in node " + this.getIdHashed());

            return keyHashed;
        } catch (IOException e) {
            System.out.println("An error occurred when saving key-value pair in node " + this.getIdHashed());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String get(String key) {
        try {
            File keyFile = new File("network/" + this.getIdHashed() + "/" + key);

            if (!keyFile.exists()) {
                System.out.println("In node " + this.getIdHashed() + " there doesn't exist any value associated with the key: " + key);
                return null;
            }
            if (!keyFile.canRead()){
                System.out.println("Permission denied! Can not read value of key " + key + " in node " + this.getIdHashed());
                return null;
            }

            Scanner sc = new Scanner(keyFile);
            StringBuilder value = new StringBuilder();
            while (sc.hasNextLine()) {
                value.append(sc.nextLine());
            }
            return value.toString();
        } catch (IOException e) {
            System.out.println("An error occurred when retrieving the value of key " + key + " in node " + this.getIdHashed());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean delete(String key) {
        File keyFile = new File("network/" + this.getIdHashed() + "/" + key);

        if (!keyFile.exists()) {
            System.out.println("In node " + this.getIdHashed() + " there doesn't exist any value associated with the key: " + key);
            return false;
        }

        if(keyFile.delete()){
            System.out.println("Successfully deleted key-value pair in node " + this.getIdHashed());
            return true;
        }
        else {
            System.out.println("An error occurred when deleting key-value pair in node " + this.getIdHashed());
            return false;
        }
    }
}