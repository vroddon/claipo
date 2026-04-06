package vroddon.claipo.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Lanzar servidor así: mcp-server-filesystem d:\svn\victor\claipo\data
 */
public class MCPClient {

    public final Process process;
    private final BufferedWriter writer;
    private final BufferedReader reader;
    private final ObjectMapper mapper = new ObjectMapper();
    private int idCounter = 1;

    public static void main(String[] args) throws Exception {
        test();
    }

    public static void test() {
        try {
            MCPClient client = new MCPClient("cmd /c mcp-server-filesystem d:\\svn\\victor\\claipo");
            client.initialize();
            client.receive();                           // descartar respuesta de initialize

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode args = mapper.createObjectNode();
            args.put("path", ".\\data\\prueba.txt");
            args.put("content", "Hola mundo");
            client.callToolWithArgs("write_file", args);
            System.out.println(client.receive());

            client.callTool("list_directory", ".\\data");
            System.out.println(client.receive());
        } catch (Exception e) {
        };
    }

    public MCPClient(ProcessBuilder pb) throws IOException {
        process = pb.start();
        
        writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)
        );

        reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
        );            
        
    }
    
    public MCPClient(String command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command.split(" "));
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        process = pb.start();

        writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)
        );

        reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
        );
    }

    public void send(ObjectNode json) throws IOException {
        String message = mapper.writeValueAsString(json);
        writer.write(message);
        writer.write("\n");
        writer.flush();
    }

    public String receive() throws IOException {
        return reader.readLine();
    }

    public void initialize() throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", idCounter++);
        request.put("method", "initialize");

        ObjectNode params = mapper.createObjectNode();
        params.put("protocolVersion", "2024-11-05");
        params.set("capabilities", mapper.createObjectNode());
        ObjectNode clientInfo = mapper.createObjectNode();
        clientInfo.put("name", "claipo");
        clientInfo.put("version", "0.1.0");
        params.set("clientInfo", clientInfo);
        request.set("params", params);

        send(request);
    }

    public void sendInitialized() throws IOException {
        ObjectNode notification = mapper.createObjectNode();
        notification.put("jsonrpc", "2.0");
        notification.put("method", "notifications/initialized");
        send(notification);
    }

    public void listTools() throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", idCounter++);
        request.put("method", "tools/list");

        send(request);
    }

    public void callTool(String toolName, String path) throws IOException {

        ObjectNode request = mapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", idCounter++);
        request.put("method", "tools/call");

        ObjectNode params = mapper.createObjectNode();
        params.put("name", toolName);

        ObjectNode arguments = mapper.createObjectNode();
        arguments.put("path", path);

        params.set("arguments", arguments);
        request.set("params", params);

        send(request);
    }

    public void callToolWithArgs(String toolName, JsonNode arguments) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", idCounter++);
        request.put("method", "tools/call");
        ObjectNode params = mapper.createObjectNode();
        params.put("name", toolName);
        params.set("arguments", arguments);
        request.set("params", params);
        send(request);
    }

    /**
     * Le pregunta al servidor MCP que qué sabe hacer.
     */
    public ArrayNode getToolDescription2() {
        ArrayNode claudeTools = mapper.createArrayNode();
        try {
            listTools();
            JsonNode toolsResponse = mapper.readTree(receive());
            JsonNode toolsArray = toolsResponse.path("result").path("tools");
            for (JsonNode tool : toolsArray) {
                ObjectNode ct = mapper.createObjectNode();
                ct.put("name", tool.path("name").asText());
                ct.put("description", tool.path("description").asText());
                ct.set("input_schema", tool.path("inputSchema")); // renombrar aquí
                claudeTools.add(ct);
            }
//            System.out.println(claudeTools.toPrettyString());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return claudeTools;
    }

    //Solo si queremos dar menos habilidades...
    public static String getToolDescription() {
        String toolsJson = "["
                + "{"
                + "\"name\": \"list_directory\","
                + "\"description\": \"List contents of a directory\","
                + "\"input_schema\": {"
                + "\"type\": \"object\","
                + "\"properties\": {"
                + "\"path\": { \"type\": \"string\", \"description\": \"Directory path\" }"
                + "},"
                + "\"required\": [\"path\"]"
                + "}"
                + "},"
                + "{"
                + "\"name\": \"read_file\","
                + "\"description\": \"Read a text file\","
                + "\"input_schema\": {"
                + "\"type\": \"object\","
                + "\"properties\": {"
                + "\"path\": { \"type\": \"string\", \"description\": \"File path\" }"
                + "},"
                + "\"required\": [\"path\"]"
                + "}"
                + "}"
                + "]";
        return toolsJson;
    }

}
