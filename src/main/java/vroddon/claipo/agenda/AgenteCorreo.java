package vroddon.claipo.agenda;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import vroddon.claipo.ia.Claude;
import vroddon.claipo.ia.GeminiSimpleChat;

/**
 * Agente que contesta correos
 * @author victor
 */
public class AgenteCorreo  {
    private final static GeminiSimpleChat chat = new GeminiSimpleChat();
    
    
    public static String chat(String hint)
    {
        String emails = AgenteCorreo.readEmailJson();
        // Build prompt; must be final for usage inside SwingWorker
        final String base =
                "You are Victor Rodriguez (vrodriguez@fi.upm.es, victor.rodriguez@upm.es or vroddon@gmail.com) and you have to answer this email thread. " +
                "Be concise, not too formal. Write only the body text, nothing else .";

        final String prompt = "The user added this hint: " + hint + ". END OF HINT. Email thread: ";
        return Claude.chat(base + prompt + emails);

//        return chat.chat(base + prompt + emails);
    }
    
    private static String readEmailJson() {
        String s ="";
        try{
            Path path = Path.of("D:\\svn\\victor\\claipo\\email.json");
            s=Files.readString(path, StandardCharsets.UTF_8);
        }catch(Exception e)
        {
            e.printStackTrace();
        }
            return s;
        }
    
}
