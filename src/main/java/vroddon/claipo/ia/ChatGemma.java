package vroddon.claipo.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.Set;
import vroddon.claipo.test.MCPClient;

public class ChatGemma extends Chat {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "gemma4";
    private ObjectMapper mapper = new ObjectMapper();
    
    private MCPClient mcpFileSystem = null;
    private MCPClient mcpServerMemory = null;
    private Set<String> memoryTools = new HashSet<>();
    private Set<String> fileTools = new HashSet<>();
    
    public static void main(String[] args) throws Exception {
        String text = new ChatGemma().chat("¿Qué tal estás?");
        System.out.println(text);        
    }    
    
    public ChatGemma()
    {
        init();
    }

    private void init() {
        try {
            Path projectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
            Path filesystemRoot = projectRoot.resolve("data");
            if (!Files.exists(filesystemRoot)) {
                filesystemRoot = projectRoot;
            }

            // MCP File System: expose data/ if available
            ProcessBuilder pbFs = new ProcessBuilder("cmd", "/c", "mcp-server-filesystem", filesystemRoot.toString());
            pbFs.redirectError(ProcessBuilder.Redirect.INHERIT);
            mcpFileSystem = new MCPClient(pbFs);
            mcpFileSystem.initialize();
            String fsInitResp = mcpFileSystem.receive();
            if (fsInitResp == null || fsInitResp.contains("\"error\"")) {
                throw new IOException("MCP filesystem init failed: " + fsInitResp);
            }
            mcpFileSystem.sendInitialized();
            for (JsonNode t : mcpFileSystem.getToolDescription2()) {
                fileTools.add(t.path("name").asText());
            }
            if (fileTools.isEmpty()) {
                throw new IOException("MCP filesystem returned no tools");
            }

            // MCP Server Memory. Guarda la información en %MEMORY_FILE_PATH%, que tiene que estar previamente fijado...
            ProcessBuilder pbMem = new ProcessBuilder("cmd", "/c", "npx", "-y", "@modelcontextprotocol/server-memory");
            pbMem.redirectError(ProcessBuilder.Redirect.INHERIT);
            mcpServerMemory = new MCPClient(pbMem);
            mcpServerMemory.initialize();
            String memInitResp = mcpServerMemory.receive();
            if (memInitResp == null || memInitResp.contains("\"error\"")) {
                throw new IOException("MCP memory init failed: " + memInitResp);
            }
            mcpServerMemory.sendInitialized();
            for (JsonNode t : mcpServerMemory.getToolDescription2()) {
                memoryTools.add(t.path("name").asText());
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize MCP support for Gemma", e);
        }
    }

    
    @Override
    public String chat(String prompt) {
        prompt = prompt.replace("\r\n", " ").replace("\n", " ").replace("\r", " ").replace("\t", " ");
        String responseText = "";

        try {
            URL url = new URL(OLLAMA_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInput = "{"
                    + "\"model\": \"" + MODEL + "\","
                    + "\"prompt\": \"" + escapeJson(prompt) + "\","
                    + "\"stream\": false"
                    + "}";

            OutputStream os = conn.getOutputStream();
            os.write(jsonInput.getBytes("utf-8"));
            os.flush();
            os.close();

            int status = conn.getResponseCode();

            BufferedReader reader;
            if (status >= 200 && status < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
            }

            String line;
            StringBuilder response = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                response.append(line.trim());
            }

            reader.close();

            // Extraer el campo "response" del JSON (sin usar librerías externas)
            responseText = extractResponse(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
            responseText = "Error llamando a Ollama: " + e.getMessage();
        }

        return responseText;
    }

    @Override
    public ObjectNode getJSONBodyWithTools(ArrayNode messages) {
        ObjectNode body = mapper.createObjectNode();
        body.put("max_tokens", 1024);
        body.set("messages", messages);
        body.put("model", "gemma4");
        body.put("stream", false);

        // Añadir las tools MCP disponibles para que Gemma sepa que puede llamarlas
        ArrayNode tools = mapper.createArrayNode();
        if (mcpFileSystem != null) {
            for (JsonNode t : mcpFileSystem.getToolDescription2()) {
                tools.add(t);
            }
        }
        if (mcpServerMemory != null) {
            for (JsonNode t : mcpServerMemory.getToolDescription2()) {
                tools.add(t);
            }
        }
        if (tools.size() > 0) {
            body.set("tools", tools);
        }

        return body;
    }

@Override
public boolean hasFinishedFromResponse(JsonNode response) {
    String doneReason = response.path("done_reason").asText("");
    if (doneReason.isEmpty()) {
        return true;
    }
    if ("tool_call".equals(doneReason)) {
        return false;   // aún no ha terminado, espera a que ejecutes la tool
    }
    return true;
}


@Override
public String getTextFromResponse(JsonNode response) {
    // Gemma pone el texto en: message.content
    return response.path("message").path("content").asText("");
}



@Override
public SimpleEntry<ObjectNode, String> invokeTools(JsonNode response) {

    SimpleEntry<ObjectNode, String> ret = new SimpleEntry<>(null, "");
    StringBuilder res = new StringBuilder();

    try {
        ArrayNode toolResults = mapper.createArrayNode();

        // Gemma: las tools vienen en: message.tool_calls[]
        JsonNode toolCalls = response
                .path("message")
                .path("tool_calls");

        if (!toolCalls.isArray()) {
            ret.setValue("");
            return ret; // no hay tools
        }

        for (JsonNode toolCall : toolCalls) {

            String toolId = toolCall.path("id").asText();
            String toolName = toolCall.path("function").path("name").asText();
            String rawArgs = toolCall.path("function").path("arguments").asText("{}");

            // Los argumentos vienen como STRING → hay que parsearlos
            ObjectNode args = (ObjectNode) mapper.readTree(rawArgs);

            res.append("[MCP ").append(toolName).append("]\n");

            // ¿Qué servidor MCP usar?
            MCPClient mcp = memoryTools.contains(toolName) ? mcpServerMemory : mcpFileSystem;
            if (mcp == null) {
                throw new IOException("No available MCP server for tool: " + toolName);
            }

            // Ejecutar tool
            mcp.callToolWithArgs(toolName, args);

            // Recibir y parsear la respuesta de MCP
            String mcpRaw = mcp.receive();
            JsonNode mcpResp = mapper.readTree(mcpRaw);

            // MemoryServer responde así:
            // { result: { content: [ { text: "..." } ] } }
            String resultText =
                    mcpResp.path("result")
                           .path("content")
                           .path(0)
                           .path("text")
                           .asText(mcpRaw);

            res.append(resultText).append("\n");

            // Cada resultado es un mensaje independiente con role "tool"
            ObjectNode toolResult = mapper.createObjectNode();
            toolResult.put("role", "tool");
            toolResult.put("tool_call_id", toolId);
            toolResult.put("content", resultText);

            toolResults.add(toolResult);
        }

        // Gemma espera un mensaje por cada tool_result, no un único mensaje con array.
        // Devolvemos el primer resultado; el caller debe iterar toolResults si hay varios.
        // Para mantener la interfaz SimpleEntry, devolvemos el array como nodo raíz
        // y dejamos que el Orquestador lo añada a messages directamente.
        ObjectNode wrapper = mapper.createObjectNode();
        wrapper.put("role", "tool_results");
        wrapper.set("tool_results", toolResults);

        ret = new SimpleEntry<>(wrapper, res.toString());

    } catch (Exception e) {
        e.printStackTrace();
        ret.setValue("ERROR invoking tools: " + e.getMessage());
    }

    return ret;
}

    
    
    
     @Override
    public String chatWithJSON(String bodyJson) {
        try{
            
            //Añadimos el modelo
/*            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jbody = (ObjectNode) mapper.readTree(bodyJson);
            jbody.put("model", "gemma4");
            jbody.put("stream", false);
            bodyJson = mapper.writeValueAsString(jbody);*/
            //Fin de añadimos el modelo
            
            
        java.net.URL url = new java.net.URL("http://localhost:11434/api/chat");
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
//        conn.setRequestProperty("x-api-key", apiKey);
//        conn.setRequestProperty("anthropic-version", "2023-06-01");
        conn.setDoOutput(true);

        try ( java.io.OutputStream os = conn.getOutputStream()) {
            os.write(bodyJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        java.io.InputStream is = conn.getResponseCode() < 400 ? conn.getInputStream() : conn.getErrorStream();
        return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }catch(Exception e)
        {
            e.printStackTrace();
            return "error";
        }
    }   
    
    
    // Escapa comillas para JSON básico
    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\"", "\\\"");
    }

    // Extrae el campo "response" del JSON devuelto por Ollama
    private static String extractResponse(String json) {
        String key = "\"response\":\"";
        int start = json.indexOf(key);
        if (start == -1) return json;

        start += key.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return json;

        return json.substring(start, end);
    }
}