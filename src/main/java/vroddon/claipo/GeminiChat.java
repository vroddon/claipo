package vroddon.claipo;

import java.util.List;

import java.util.Map;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;


public class GeminiChat {

    // Configura tus IDs de Google Cloud aquí
    private static final String PROJECT_ID = "smallwebs-1508966024969"; // 
    private static final String LOCATION = "us-central1";
    private static final String MODEL_NAME = "gemini-2.5-pro";
    //private static final String MODEL_NAME = "gemini-1.5-pro-002"; // El mejor para razonamiento
    private static final String JSON_PATH = "D:\\svn\\uh\\google.json";

    public static void main(String[] args) {
        try {




String pregunta = "¿Cuáles son las fuentes de derecho? Quiero una única frase, breve si puede ser.";
            System.out.println("Consultando a Gemini...");
            String respuesta = new GeminiSimpleChat().chat(pregunta);
            System.out.println("\n--- RESPUESTA ---\n" + respuesta);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
/**
 * Connects to an MCP server over HTTP, handshakes, lists tools, and
 * attempts to call an echo/ask/qa tool with a simple question.
 *
 * @param mcpUrl   The MCP endpoint (e.g., "http://localhost:8080/mcp" or base URL used by your server)
 *                 For classic SSE-style endpoints, many servers expose a single path.
 * @throws Exception on network/JSON errors
 */
public void test(String mcpUrl) throws Exception {
    // ---- Config ----
    final String question = "¿Cuál es la capital de España?";
    final String protocolVersion = "2025-06-18"; // MCP spec revision we claim (OK for clients) [2](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/multimodal/control-generated-output)

    // ---- HTTP & JSON helpers ----
    HttpClient http = HttpClient.newHttpClient();
    ObjectMapper mapper = new ObjectMapper();

    // Helper to POST a single JSON-RPC message and return the JSON tree
    class Rpc {
        int id = 1;

        JsonNode post(Object payload) throws Exception {
            String body = mapper.writeValueAsString(payload);
            HttpRequest req = HttpRequest.newBuilder(URI.create(mcpUrl))
                    .header("Content-Type", "application/json")
                    // Per spec, clients SHOULD accept both JSON and event streams for streamable servers. [2](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/multimodal/control-generated-output)
                    .header("Accept", "application/json, text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() / 100 != 2) {
                throw new IllegalStateException("HTTP " + resp.statusCode() + " -> " + resp.body());
            }
            // Notifications (no id) may return an empty body; guard for that
            if (resp.body() == null || resp.body().isEmpty()) {
                return mapper.createObjectNode();
            }
            return mapper.readTree(resp.body());
        }

        Map<String, Object> req(String method, Map<String, Object> params) {
            return Map.of(
                    "jsonrpc", "2.0",
                    "id", id++,
                    "method", method,
                    "params", params == null ? Map.of() : params
            );
        }

        Map<String, Object> notify(String method, Map<String, Object> params) {
            // JSON-RPC notification (no "id")
            return Map.of(
                    "jsonrpc", "2.0",
                    "method", method,
                    "params", params == null ? Map.of() : params
            );
        }
    }

    Rpc rpc = new Rpc();

    // ---- 1) initialize (handshake) ----
    // Spec: client sends "initialize" with protocolVersion & capabilities; server replies; client sends "initialized". [2](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/multimodal/control-generated-output)[3](https://learn.microsoft.com/en-us/microsoft-365-copilot/extensibility/copilot-apis-overview)
    var initParams = Map.of(
            "protocolVersion", protocolVersion,
            "capabilities", Map.of(), // you can declare client capabilities here if needed
            "clientInfo", Map.of("name", "java11-mcp-client", "version", "0.1")
    );
    JsonNode initResp = rpc.post(rpc.req("initialize", initParams));
    System.out.println("initialize → " + initResp.toPrettyString());

    // Send "initialized" notification (no id)
    rpc.post(rpc.notify("initialized", Map.of()));
    System.out.println("initialized (notification) sent.");

    // ---- 2) tools/list ----
    JsonNode toolsList = rpc.post(rpc.req("tools/list", Map.of()));
    System.out.println("tools/list → " + toolsList.toPrettyString());

    // Extract available tool names (best-effort)
    List<Map<String, Object>> tools = List.of();
    JsonNode resultNode = toolsList.get("result");
    if (resultNode != null && resultNode.has("tools") && resultNode.get("tools").isArray()) {
        tools = mapper.convertValue(resultNode.get("tools"), new TypeReference<List<Map<String, Object>>>() {});
    }

    // Choose a tool: prefer *echo*, then *ask* or *qa*
    String chosenTool = null;
    if (!tools.isEmpty()) {
        for (Map<String, Object> t : tools) {
            String name = String.valueOf(t.getOrDefault("name", "")).toLowerCase();
            if (name.contains("echo")) { chosenTool = String.valueOf(t.get("name")); break; }
        }
        if (chosenTool == null) {
            for (Map<String, Object> t : tools) {
                String name = String.valueOf(t.getOrDefault("name", "")).toLowerCase();
                if (name.contains("ask") || name.contains("qa")) { chosenTool = String.valueOf(t.get("name")); break; }
            }
        }
    }

    if (chosenTool == null) {
        System.out.println("No echo/ask/qa tool found. Available tools:");
        for (Map<String, Object> t : tools) {
            System.out.println(" • " + t.get("name") + " — " + t.getOrDefault("description", ""));
        }
        return;
    }

    // ---- 3) tools/call ----
    // MCP JSON-RPC call for tool execution is "tools/call" with params { name, arguments } (arguments schema is server-defined). [4](https://techcommunity.microsoft.com/blog/educationblog/updates-on-copilot-eligibility-for-education-customers/4099802)
    Map<String, Object> arguments = Map.of("input", question); // many echo tools accept "input"; adjust if your server differs
    var callParams = Map.of("name", chosenTool, "arguments", arguments);

    JsonNode callResp = rpc.post(rpc.req("tools/call", callParams));
    System.out.println("tools/call → " + callResp.toPrettyString());

    // Best-effort pretty print of textual result (depends on the server’s content format)
    JsonNode callResult = callResp.get("result");
    if (callResult != null && callResult.has("content")) {
        System.out.println("\n--- TOOL RESULT ---");
        System.out.println(callResult.get("content").toPrettyString());
    } else {
        System.out.println("\nNo 'result.content' in response; full payload printed above.");
    }
}
    
}
