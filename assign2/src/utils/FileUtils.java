package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class FileUtils {

    private static final String STORAGE_ROOT = "network/";

    public static void createRoot() {
        File directory = new File(STORAGE_ROOT);

        boolean createdFlag = false;
        if (!Files.exists(Paths.get(STORAGE_ROOT))) {
            createdFlag = directory.mkdir();
        }

        System.out.println("Directory " + STORAGE_ROOT + " CREATED: " + createdFlag);
    }

    public static void createDirectory(String id) {
        File directory = new File(STORAGE_ROOT + id);

        boolean createdFlag = false;
        if (!Files.exists(Paths.get(STORAGE_ROOT + id))) {
            createdFlag = directory.mkdir();
        }

        System.out.println("Directory " + STORAGE_ROOT + id + " CREATED: " + createdFlag);
    }

    // TODO
    // JAVA NIO -> NON BLOCKING IO
    public static boolean saveFile(String id, String key, String value) {
        try {
            File keyFile = new File(STORAGE_ROOT + id + "/" + key + ".txt");

            if (!keyFile.exists()) {
                if (!keyFile.createNewFile()) {
                    System.out.println("An error occurred when creating keyFile in node " + id);
                    return false;
                }
            }

            if (!keyFile.canWrite()) {
                System.out.println("Permission denied! Can not write value of key "
                        + key + " in node " + id);
                return false;
            }

            FileWriter keyFileWriter = new FileWriter(STORAGE_ROOT + id + "/" + key + ".txt", false);

            keyFileWriter.write(value);
            keyFileWriter.close();

            System.out.println("Successfully saved key-value pair in node " + id);

        } catch (IOException e) {
            System.out.println("An error occurred when saving key-value pair in node " + id);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static String getFile(String id, String key) {
        try {
            File keyFile = new File(STORAGE_ROOT + id + "/" + key + ".txt");

            if (!keyFile.exists()) {
                System.out.println("In node " + id + " there doesn't exist any value associated with the key: " + key);
                return null;
            }
            if (!keyFile.canRead()){
                System.out.println("Permission denied! Can not read value of key " + key + " in node " + id);
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
            System.out.println("An error occurred when retrieving the value of key " + key + " in node " + id);
            e.printStackTrace();
            return null;
        }
    }

    public static boolean deleteFile(String id, String key) {
        File keyFile = new File(STORAGE_ROOT + id + "/" + key + ".txt");

        if (!keyFile.exists()) {
            System.out.println("In node " + id + " there doesn't exist any value associated with the key: " + key);
            return true;
        }

        if (keyFile.delete()) {
            System.out.println("Successfully deleted key-value pair in node " + id);
            return true;
        }

        System.out.println("An error occurred when deleting key-value pair in node " + id);
        return false;
    }
}
