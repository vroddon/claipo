/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vroddon.claipo.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.AbstractMap;

/**
 *
 * @author victor
 */
public class Chat {
    
    /**
     * Answers a chat question without any memory.
     * @param query in text form (natural language).
     * @return Text with the result
     */
    public String chat(String query)
    {
        return "No LLM provider selected";
    }

    /**
     * Chats with the body 
     * @param jBody JSON Body of the call, which also includes a tools element
     * @return Text with the result
     */
    public String chatWithJSON(String jBody)
    {
        return "No LLM provider selected";
    }

    public ObjectNode getJSONBodyWithTools(ArrayNode messages) {
        return null;
    }
        public String getTextFromResponse(JsonNode response) {
            return "";
        }
    public boolean hasFinishedFromResponse(JsonNode response)
    {
        return true;
    }
    public AbstractMap.SimpleEntry<ObjectNode, String> invokeTools(JsonNode response) {
        return new AbstractMap.SimpleEntry<>(null, "");
    }

    /**
     * Builds the assistant message to append to the conversation history.
     * Each Chat implementation may format the response differently.
     */
    public ObjectNode buildAssistantMessage(ObjectMapper mapper, JsonNode response) {
        ObjectNode msg = mapper.createObjectNode();
        msg.put("role", "assistant");
        msg.set("content", response.path("content"));
        return msg;
    }

    
    
}
