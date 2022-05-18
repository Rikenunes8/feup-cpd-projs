package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class FileUtils {

    public static void createDirectory(String storage) {
        File directory = new File("network/" + storage);

        boolean createdFlag = false;
        if (!Files.exists(Paths.get("network/" + storage))) {
            createdFlag = directory.mkdir();
        }

        System.out.println("Directory network/" + storage + " CREATED: " + createdFlag);
    }

    // TODO
    // JAVA NIO -> NON BLOCKING IO
    public static void saveFile(String storage, String key, String value) {
        try {
            File keyFile = new File("network/" + storage + "/" + key + ".txt");

            if (!keyFile.exists()) {
                if (!keyFile.createNewFile()) {
                    System.out.println("An error occurred when creating keyFile in node " + storage);
                    return;
                }
            }

            if (!keyFile.canWrite()) {
                System.out.println("Permission denied! Can not write value of key "
                        + key + " in node " + storage);
                return;
            }

            FileWriter keyFileWriter = new FileWriter("network/" + storage + "/" + key + ".txt", false);

            keyFileWriter.write(value);
            keyFileWriter.close();

            System.out.println("Successfully saved key-value pair in node " + storage);

        } catch (IOException e) {
            System.out.println("An error occurred when saving key-value pair in node " + storage);
            e.printStackTrace();
        }
    }

    public static String getFile(String storage, String key) {
        try {
            File keyFile = new File("network/" + storage + "/" + key + ".txt");

            if (!keyFile.exists()) {
                System.out.println("In node " + storage + " there doesn't exist any value associated with the key: " + key);
                return null;
            }
            if (!keyFile.canRead()){
                System.out.println("Permission denied! Can not read value of key " + key + " in node " + storage);
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
            System.out.println("An error occurred when retrieving the value of key " + key + " in node " + storage);
            e.printStackTrace();
            return null;
        }
    }

    public static boolean deleteFile(String storage, String key) {
        File keyFile = new File("network/" + storage + "/" + key + ".txt");

        if (!keyFile.exists()) {
            System.out.println("In node " + storage + " there doesn't exist any value associated with the key: " + key);
            return false;
        }

        if (keyFile.delete()) {
            System.out.println("Successfully deleted key-value pair in node " + storage);
            return true;
        }

        System.out.println("An error occurred when deleting key-value pair in node " + storage);
        return false;
    }
}
