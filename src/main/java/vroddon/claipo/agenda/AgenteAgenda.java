package vroddon.claipo.agenda;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import vroddon.claipo.ia.ChatClaude;
import vroddon.claipo.ia.ChatGemma;
import vroddon.claipo.util.Util;
import java.time.LocalDateTime;

/**
 *
 * @author victor
 */
public class AgenteAgenda {

    public static void main(String[] args) {
        String orden = "@agenda necesito un slot para la semana que viene, a partir del martes. de una hora.";
        String ia = chat(orden);
        Util.log("OK: " + ia);
    }

    public static String chat(String orden) {
        
        //Obtengo datos extra
        File ics = new File("./data/victor.ics");
        if (!ics.exists() || System.currentTimeMillis() - ics.lastModified() > 3600_000L) {
            Util.log("Obteniendo una nueva versión del calendario");
            downloadCalendar();
        }
        String base;
        try {
            base = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("./data/AGENDA.md")));
        } catch (java.io.IOException e) {
            base = "";
        }

        List<ICSEvent> events = AgendaParser.parseCalendar();
        String contexto = "Estos son mis eventos: " + AgendaParser.formatEvents(events) + "\n";

//        return new ChatClaude().chat(base + contexto + orden);
        return new ChatGemma().chat(base + contexto + orden);
    }

    public static boolean downloadCalendar() {
        try {
            String password = System.getenv("VRODDON_API_KEY");
            String credentials = Base64.getEncoder().encodeToString(("vroddon:" + password).getBytes());

            URL url = new URL("http://cosasbuenas.es:5232/vroddon/b9bf2aff-498c-5c57-cd9e-9d548d1a3193");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Basic " + credentials);

            new File("./data").mkdirs();
            try ( InputStream in = conn.getInputStream();  FileOutputStream out = new FileOutputStream("./data/victor.ics")) {
                in.transferTo(out);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    



    
    
}
