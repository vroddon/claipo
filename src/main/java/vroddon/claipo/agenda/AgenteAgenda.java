package vroddon.claipo.agenda;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import vroddon.claipo.ia.Claude;
import vroddon.claipo.ia.Gemma;
import vroddon.claipo.util.Util;

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
        File ics = new File("./data/victor.ics");
        if (!ics.exists() || System.currentTimeMillis() - ics.lastModified() > 3600_000L) {
            downloadCalendar();
        }
        String base = "Empiezo a trabajar a las 09:30, aunque si es posible, no quiero reuniones antes de las 10:00. Prefiero dejarme los viernes libres. Entre las 13:00 y las 15:00 necesito 1 hora para comer, si es posible a las 13:00. Me gusta acabar de trabajar a las 15:30. ";
        base += "Si te pido un slot, solo quiero las posibilidades, una por línea. Dame a lo sumo 3 posibilidades. Ejemplo: Lunes 26 a las 10:00. Quiero la salida limpia, sin más comentarios.\n";

        List<ICSEvent> events = AgendaParser.parseCalendar();
        String contexto = "Estos son mis eventos: " + AgendaParser.formatEvents(events) + "\n";

        return Claude.chat(base + contexto + orden);
//        return Gemma.chat(base + contexto + orden);
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
