package vroddon.old;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

/**
 * Minimal Gemini client wrapper.
 * Requires GOOGLE_API_KEY in environment (or pass an API key to the ctor).
 */
public class LLMGemini {

    private final Client client;
    private final String model;

    public LLMGemini() {
        // Using default constructor picks up GOOGLE_API_KEY automatically
        // per the SDK docs. Alternatively, you could call:
        // Client.builder().apiKey("...").build();
        this.client = new Client();
        this.model = "gemini-2.5-flash";
    }

    public LLMGemini(String apiKey, String model) {
        this.client = (apiKey == null || apiKey.isBlank())
                ? new Client()
                : Client.builder().apiKey(apiKey).build();
        this.model = (model == null || model.isBlank()) ? "gemini-2.5-flash" : model;
    }

    public String replyToEmailThread(String threadText) {
        String prompt =
                "Please answer to the email thread that follows.\n\n" +
                "Requirements:\n" +
                "• Reply in the same language as the thread.\n" +
                "• Be concise, polite and actionable.\n" +
                "• Do NOT include quoted text unless necessary.\n" +
                "• Start directly with the reply (no salutations unless the thread requires it).\n\n" +
                "=== EMAIL THREAD START ===\n" +
                threadText +
                "\n=== EMAIL THREAD END ===";

        // SDK’s simple unary call; third parameter is config (null = defaults).
        GenerateContentResponse response =
                client.models.generateContent(model, prompt, /*config*/ null);

        // Convenience accessor: returns the concatenated text from candidates.
        return response.text();
    }
}
