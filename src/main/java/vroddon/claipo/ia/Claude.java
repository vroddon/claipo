package vroddon.claipo.ia;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import vroddon.claipo.util.Util;
 
/**
 * Implementa la IA con Claude
 * @author victor
 */
public class Claude {
    public static void main(String[] args) {
        // Lee la API key de la variable de entorno ANTHROPIC_API_KEY
        String a = "¿Cuáles son las fuentes de derecho? Quiero una única frase, breve si puede ser.";
        Util.log("YO____: " + a);
        String b = Claude.chat(a);
        Util.log("CLAUDE: "+b);
    }
    
    public static String chat(String prompt) {
        AnthropicClient client = AnthropicOkHttpClient.fromEnv();
        MessageCreateParams params = MessageCreateParams.builder()
            .model(Model.CLAUDE_SONNET_4_6)
            .maxTokens(1024L)
            .addUserMessage(prompt)
            .build();

        Message message = client.messages().create(params);
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : message.content()) {
            if (block.isText()) {
                sb.append(block.asText().text());
            }
        }
        String result = sb.toString();
        return result;
    }    
    
}