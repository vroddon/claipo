package vroddon.claipo.ia;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Gemma {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "gemma4";
    
    public static void main(String[] args) throws Exception {
        String text = Gemma.chat("¿Qué tal estás?");
        System.out.println(text);        
    }    
    

    public static String chat(String prompt) {
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