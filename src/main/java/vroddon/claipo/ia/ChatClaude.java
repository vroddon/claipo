package vroddon.claipo.ia;

import java.util.AbstractMap.SimpleEntry;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashSet;
import java.util.Set;
import vroddon.claipo.test.MCPClient;
import vroddon.claipo.util.Util;

/**
 * Implementa la IA con Claude
 *
 * @author victor
 */
public class ChatClaude extends Chat {

    private static final String apiKey = System.getenv("ANTHROPIC_API_KEY");
    private MCPClient mcpFileSystem = null;
    private MCPClient mcpServerMemory = null;
    private Set<String> memoryTools = new HashSet<>();
    private Set<String> fileTools = new HashSet<>();
    private ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        // Lee la API key de la variable de entorno ANTHROPIC_API_KEY
        String a = "¿Cuáles son las fuentes de derecho? Quiero una única frase, breve si puede ser.";
        Util.log("YO____: " + a);
        String b = new ChatClaude().chat(a);
        Util.log("CLAUDE: " + b);
    }

    public ChatClaude() {
        init();
    }

    public void init() {
        try {
            //MCP File System
            mcpFileSystem = new MCPClient("cmd /c mcp-server-filesystem d:\\svn\\victor\\claipo");
            mcpFileSystem.initialize();
            mcpFileSystem.receive();
            for (JsonNode t : mcpFileSystem.getToolDescription2()) {
                fileTools.add(t.path("name").asText());
            }

            //MCP Server Memory. D:\svn\victor\claipo\mcpdata.jsonl Guarda la información en %MEMORY_FILE_PATH%, que tiene que estar previamente fijado...
            mcpServerMemory = new MCPClient("cmd /c npx -y @modelcontextprotocol/server-memory");
            mcpServerMemory.initialize();
            mcpServerMemory.receive();
            for (JsonNode t : mcpServerMemory.getToolDescription2()) {
                memoryTools.add(t.path("name").asText());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Este método utiliza la librería de Anthropic
    @Override
    public String chat(String prompt) {
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

    @Override
    public ObjectNode getJSONBodyWithTools(ArrayNode messages) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", "claude-sonnet-4-5-20250929");
        body.put("max_tokens", 1024);
        ArrayNode allTools = mapper.createArrayNode();
        mcpFileSystem.getToolDescription2().forEach(allTools::add);
        mcpServerMemory.getToolDescription2().forEach(allTools::add);
        body.set("tools", allTools);
        body.set("messages", messages);
        return body;
    }

    /**
     * Este método llama a la API a pelo
     */
    @Override
    public String chatWithJSON(String bodyJson) {
        try {

            java.net.URL url = new java.net.URL("https://api.anthropic.com/v1/messages");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-api-key", apiKey);
            conn.setRequestProperty("anthropic-version", "2023-06-01");
            conn.setDoOutput(true);

            try ( java.io.OutputStream os = conn.getOutputStream()) {
                os.write(bodyJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            java.io.InputStream is = conn.getResponseCode() < 400 ? conn.getInputStream() : conn.getErrorStream();
            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    public String getTextFromResponse(JsonNode response) {
        String res = "";
        for (JsonNode block : response.path("content")) {
            if ("text".equals(block.path("type").asText())) {
                res += block.path("text").asText() + "\n";
            }
        }
        return res;
    }

    public boolean hasFinishedFromResponse(JsonNode response) {
        String stopReason = response.path("stop_reason").asText();
        if ("end_turn".equals(stopReason)) {
            return true;
        }
        if (!"tool_use".equals(stopReason)) {
            return true;
        }
        return false;
    }
    
    public SimpleEntry<ObjectNode, String> invokeTools(JsonNode response) {
        SimpleEntry<ObjectNode, String> ret = new SimpleEntry<>(null, "");
        String res = "";
        try {
            ArrayNode toolResults = mapper.createArrayNode();
            for (JsonNode block : response.path("content")) {
                if (!"tool_use".equals(block.path("type").asText())) {
                    continue;
                }
                String toolName = block.path("name").asText();
                JsonNode input = block.path("input");
                String toolId = block.path("id").asText();

                //     System.out.println("→ MCP: " + toolName + " " + input);                
                res += "[MCP " + toolName + "] \n";

                MCPClient mcp = memoryTools.contains(toolName) ? mcpServerMemory : mcpFileSystem;
                mcp.callToolWithArgs(toolName, (ObjectNode) input);

                String mcpRaw = mcp.receive();
                JsonNode mcpResp = mapper.readTree(mcpRaw);
                String result = mcpResp.path("result").path("content").path(0).path("text").asText(mcpRaw);
                res += result + "\n";
                ObjectNode toolResult = mapper.createObjectNode();
                toolResult.put("type", "tool_result");
                toolResult.put("tool_use_id", toolId);
                toolResult.put("content", result);
                toolResults.add(toolResult);
            }
            
            ObjectNode userMsg = mapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.set("content", toolResults);
            ret = new SimpleEntry<>(userMsg, ret.getValue());
            

        } catch (Exception e) {
            e.printStackTrace();
        }
        ret.setValue(res);
        return ret;
    }

}
