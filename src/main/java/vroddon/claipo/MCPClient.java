package vroddon.claipo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.nio.charset.StandardCharsets;


/**
 * Lanzar servidor así: mcp-server-filesystem d:\svn\victor\claipo\data
 */
public class MCPClient {

    private final Process process;
    private final BufferedWriter writer;
    private final BufferedReader reader;
    private final ObjectMapper mapper = new ObjectMapper();
    private int idCounter = 1;
    
    
    public static void main(String[] args) throws Exception {

    MCPClient client = new MCPClient("cmd /c mcp-server-filesystem d:\\svn\\victor\\claipo");

    client.listTools();
    System.out.println("TOOLS:");
    System.out.println(client.receive());

    // Now list directory
    client.callTool("list_directory", ".\\data");
    System.out.println("DIRECTORY CONTENT:");
    System.out.println(client.receive());
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

        send(request);
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
}