package vroddon.claipo.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.AbstractMap;
import vroddon.claipo.util.Util;

/**
 * Gestiona una sesión completa con un Chat (memoria en las interacciones). Es
 * capaz de decirle a Claude de la existencia de una herramienta. Cuando claude
 * la utiliza, se ejecuta, y se le dice a claude de los resultados. En bucle
 * infinito hasta que Claude deja de invocar herramientas.
 *
 * @author victor
 */
public class Agente {

    ObjectMapper mapper = new ObjectMapper();
    ArrayNode messages = null;
    Chat chat = new Chat();
    

    public static void main(String[] args) {
        Agente agente = new Agente(new ChatClaude());
//        agente.testMemoriaEnConversacion();
        agente.testMemoriaPermanente();
        if (true)
            return;
    }
    
    public void testMemoriaPermanente()
    {
        String m1 = "", n1 = "";
   /*             m1 = "Hola, quiero que recuerdes que Víctor es una persona";
        Util.log("YO: " + m1);
        n1 = chat(m1);
        Util.log("IA: " + n1);*/
        m1 = "¿Hay algún archivo de texto en la carpeta .\\data\\ ¿qué dice?";
        Util.log("YO: " + m1);
        n1 = chat(m1);
        Util.log("IA: " + n1);
         
  /*      m1 = "¿Donde trabaja Victor?";
        Util.log("YO: " + m1);
        n1 = chat(m1);
        Util.log("IA: " + n1);*/

//        claude.chat("Tengo un archivo que se llama .\\data\\prueba.txt ¿qué dice?");
//        claude.chat("¿Qué archivos hay en el directorio .\\data?");
    }
    
    public void testMemoriaEnConversacion()
    {
        String m1 = "", n1 = "";
        m1 = "Hola, me llamo Pedro";
        Util.log("YO: " + m1);
        n1 = chat(m1);
        Util.log("IA: " + n1);
        m1 = "Hola, ¿como te he dicho que me llamo?";
        Util.log("YO: " + m1);
        n1 = chat(m1);
        Util.log("IA: " + n1);
    }
    

    public Agente(Chat specificchat) {
        chat = specificchat;
        messages = mapper.createArrayNode();
        
    }

    /**
     * Envía la petición. Procesa la respuesta de Claude. Si hay invocación de
     * alguna Tool, se ejecuta y se le pasa de nuevo a Claude
     */
    public String chat(String orden) {
        String res = "";
        try {
            ObjectNode firstMsg = mapper.createObjectNode();
            firstMsg.put("role", "user");
            firstMsg.put("content", orden);
            messages.add(firstMsg);

            while (true) {
                //Obtengo el body json de la llamada, que puede ser distinto para las distintas implementaciones (anthropic / openai)
                ObjectNode body = chat.getJSONBodyWithTools(messages);

                //Invocamos chat with tools aqui
                String responseJson = chat.chatWithJSON(mapper.writeValueAsString(body));
                //Fin de invocar chat with tools aquí

                JsonNode response = mapper.readTree(responseJson);
                res += chat.getTextFromResponse(response);
                
                boolean bfinished = chat.hasFinishedFromResponse(response);
                if (bfinished)
                    break;

                messages.add(chat.buildAssistantMessage(mapper, response));

                AbstractMap.SimpleEntry<ObjectNode, String> ret = chat.invokeTools(response);
                res += ret.getValue();
                // ChatGemma devuelve un wrapper "tool_results" con múltiples mensajes;
                // otros backends devuelven un único ObjectNode directamente.
                ObjectNode toolMsg = ret.getKey();
                if (toolMsg != null && toolMsg.has("tool_results")) {
                    toolMsg.path("tool_results").forEach(messages::add);
                } else if (toolMsg != null) {
                    messages.add(toolMsg);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

}
