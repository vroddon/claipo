package vroddon.claipo.agenda;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * ESTO ESTA A MEDIO HACER
 */

public class AgendaMCPServer {
    static final String DATA_FILE = "agenda.json";
    static ObjectMapper mapper = new ObjectMapper();
    static List<JsonNode> events = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        loadEvents();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out));

        // First, send tool description
        ObjectNode desc = mapper.createObjectNode();
        desc.set("tools", mapper.readTree(getToolDescription()));
        writer.write(mapper.writeValueAsString(desc));
        writer.newLine();
        writer.flush();

        String line;
        while ((line = reader.readLine()) != null) {
            JsonNode request = mapper.readTree(line);
            ObjectNode response = handleRequest(request);
            writer.write(mapper.writeValueAsString(response));
            writer.newLine();
            writer.flush();
        }
    }

    static ObjectNode handleRequest(JsonNode request) throws IOException {
        String tool = request.path("tool").asText();
        JsonNode input = request.path("input");
        ObjectNode resp = mapper.createObjectNode();

        switch (tool) {
            case "get_events":
                resp.set("result", getEvents(input));
                break;
            case "add_event":
                resp.set("result", addEvent(input));
                break;
            default:
                resp.put("error", "Unknown tool: " + tool);
        }

        return resp;
    }

    static JsonNode getEvents(JsonNode input) {
        String start = input.path("start_date").asText();
        String end = input.path("end_date").asText();

        List<JsonNode> filtered = new ArrayList<>();
        for (JsonNode e : events) {
            String s = e.path("start_time").asText();
            if (s.compareTo(start) >= 0 && s.compareTo(end) <= 0) {
                filtered.add(e);
            }
        }

        return mapper.valueToTree(filtered);
    }

    static JsonNode addEvent(JsonNode input) throws IOException {
        events.add(input);
        saveEvents();
        return input;
    }

    static void loadEvents() {
        try {
            if (Files.exists(Paths.get(DATA_FILE))) {
                events = Arrays.asList(mapper.readValue(new File(DATA_FILE), JsonNode[].class));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void saveEvents() throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(DATA_FILE), events);
    }

    static String getToolDescription() {
        return "[\n" +
                "  {\n" +
                "    \"name\": \"get_events\",\n" +
                "    \"description\": \"Retrieve events between start_date and end_date\",\n" +
                "    \"parameters\": {\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": {\n" +
                "        \"start_date\": {\"type\": \"string\", \"format\": \"date\"},\n" +
                "        \"end_date\": {\"type\": \"string\", \"format\": \"date\"}\n" +
                "      },\n" +
                "      \"required\": [\"start_date\",\"end_date\"]\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"name\": \"add_event\",\n" +
                "    \"description\": \"Add a new event to the agenda\",\n" +
                "    \"parameters\": {\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": {\n" +
                "        \"title\": {\"type\": \"string\"},\n" +
                "        \"start_time\": {\"type\": \"string\", \"format\": \"date-time\"},\n" +
                "        \"end_time\": {\"type\": \"string\", \"format\": \"date-time\"},\n" +
                "        \"location\": {\"type\": \"string\"},\n" +
                "        \"notes\": {\"type\": \"string\"}\n" +
                "      },\n" +
                "      \"required\": [\"title\",\"start_time\",\"end_time\"]\n" +
                "    }\n" +
                "  }\n" +
                "]";
    }
}