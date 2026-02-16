package vroddon.claipo;

import java.io.FileInputStream;
import java.util.List;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;


public class GeminiSimpleChat {

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
     * CHAT BÁSICO (Texto a Texto)
     */
    public String chat(String prompt) {
        try{
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(JSON_PATH));
        
        // ESTA ES LA CLAVE: El Scope para evitar el error de autenticación
        if (credentials.createScopedRequired()) {
            credentials = credentials.createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        }

        try (VertexAI vertexAI = new VertexAI.Builder()
                .setProjectId(PROJECT_ID)
                .setLocation(LOCATION)
                .setCredentials(credentials)
                .build()) {

            GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAI);
            GenerateContentResponse response = model.generateContent(prompt);
            return ResponseHandler.getText(response);
        }
        }catch(Exception e)
        {
           return "error";
        }
    }
    
    
}
