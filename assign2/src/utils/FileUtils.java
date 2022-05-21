package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class FileUtils {

    private static final String STORAGE_ROOT = "network/";

    public static void createDirectory(String hashedID) {
        File directory = new File(STORAGE_ROOT + hashedID);

        boolean createdFlag = false;
        if (!Files.exists(Paths.get(STORAGE_ROOT + hashedID))) {
            createdFlag = directory.mkdir();
        }

        System.out.println("Directory " + STORAGE_ROOT + hashedID + " CREATED: " + createdFlag);
    }

    // TODO
    // JAVA NIO -> NON BLOCKING IO
    public static boolean saveFile(String hashedID, String key, String value) {
        try {
            File keyFile = new File(STORAGE_ROOT + hashedID + "/" + key + ".txt");

            if (!keyFile.exists()) {
                if (!keyFile.createNewFile()) {
                    System.out.println("An error occurred when creating keyFile in node " + hashedID);
                    return false;
                }
            }

            if (!keyFile.canWrite()) {
                System.out.println("Permission denied! Can not write value of key "
                        + key + " in node " + hashedID);
                return false;
            }

            FileWriter keyFileWriter = new FileWriter(STORAGE_ROOT + hashedID + "/" + key + ".txt", false);

            keyFileWriter.write(value);
            keyFileWriter.close();

            System.out.println("Successfully saved key-value pair in node " + hashedID);

        } catch (IOException e) {
            System.out.println("An error occurred when saving key-value pair in node " + hashedID);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static String getFile(String hashedID, String key) {
        try {
            File keyFile = new File(STORAGE_ROOT + hashedID + "/" + key + ".txt");

            if (!keyFile.exists()) {
                System.out.println("In node " + hashedID + " there doesn't exist any value associated with the key: " + key);
                return null;
            }
            if (!keyFile.canRead()){
                System.out.println("Permission denied! Can not read value of key " + key + " in node " + hashedID);
                return null;
            }

            Scanner sc = new Scanner(keyFile);
            StringBuilder value = new StringBuilder();
            while (sc.hasNextLine()) {
                value.append(sc.nextLine());
            }
            sc.close();
            
            return value.toString();

        } catch (IOException e) {
            System.out.println("An error occurred when retrieving the value of key " + key + " in node " + hashedID);
            e.printStackTrace();
            return null;
        }
    }

    public static boolean deleteFile(String hashedID, String key) {
        File keyFile = new File(STORAGE_ROOT + hashedID + "/" + key + ".txt");

        if (!keyFile.exists()) {
            System.out.println("In node " + hashedID + " there doesn't exist any value associated with the key: " + key);
            return true;
        }

        if (keyFile.delete()) {
            System.out.println("Successfully deleted key-value pair in node " + hashedID);
            return true;
        }

        System.out.println("An error occurred when deleting key-value pair in node " + hashedID);
        return false;
    }
}
