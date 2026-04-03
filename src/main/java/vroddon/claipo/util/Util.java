package vroddon.claipo.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 *
 * @author victor
 */
public class Util {
    

    public static void logf(String s) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"));
        String line = timestamp + " " + s.replace("\r\n", "\t").replace("\n", "\t").replace("\r", "\t");
        try (PrintWriter pw = new PrintWriter(new FileWriter("claipo.log", true))) {
            pw.println(line);
        } catch (IOException e) {
            System.err.println("Error writing to claipo.log: " + e.getMessage());
        }
    }

    public static void log(String s)
    {
        String hora = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println(hora+" "+s);
    }
    
      public static String getRandomHex4() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(4);

        for (int i = 0; i < 4; i++) {
            int n = random.nextInt(16); // 0..15
            sb.append(Integer.toHexString(n));
        }

        return sb.toString();
    } 
}
