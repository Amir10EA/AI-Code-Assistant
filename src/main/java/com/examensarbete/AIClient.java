package com.examensarbete;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client for interacting with AI model APIs.
 */
public class AIClient {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Properties API_KEYS = loadApiKeys();
    
    // Regex patterns for extracting information from AI responses
    private static final Pattern BUG_POSITION_PATTERN = Pattern.compile("Bug Position:\\s*(\\d+)-(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CORRECTED_CODE_PATTERN = Pattern.compile("Corrected Code:\\s*```java\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    
    /**
     * Loads API keys from properties file or environment variables.
     */
    private static Properties loadApiKeys() {
        Properties properties = new Properties();
        
        // Try to load from properties file first
        try (InputStream in = AIClient.class.getClassLoader().getResourceAsStream("api-keys.properties")) {
            if (in != null) {
                properties.load(in);
                System.out.println("Loaded API keys from properties file");
            }
        } catch (IOException e) {
            System.out.println("No api-keys.properties file found, will use environment variables");
        }
        
        return properties;
    }
    
    /**
     * Sends a request to the AI model's API.
     * 
     * @param model The AI model to use
     * @param prompt The prompt to send
     * @return The raw response from the AI model
     * @throws Exception If there's an error communicating with the API
     */
    public static String sendRequest(String model, String prompt) throws Exception {
        String apiKey = getApiKey(model);
        String apiUrl = getApiUrl(model);
        
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = buildRequestBody(model, prompt);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        System.out.println("Sending request to AI model...");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new RuntimeException("API request failed with status code " + response.statusCode() + ": " + response.body());
        }
        
        return response.body();
    }
    
    /**
     * Parses the AI response to extract information.
     * 
     * @param response The raw AI response
     * @return Structured response object
     */
    public static AIResponse parseResponse(String response) throws Exception {
        // Parse the response to extract content from JSON if necessary
        String content = extractContentFromApiResponse(response);
        
        // Extract bug positions and corrected code snippets
        List<String> bugPositions = new ArrayList<>();
        List<String> correctedCodeSnippets = new ArrayList<>();
        
        Matcher posMatcher = BUG_POSITION_PATTERN.matcher(content);
        while (posMatcher.find()) {
            bugPositions.add(posMatcher.group(1) + "-" + posMatcher.group(2));
        }
        
        Matcher codeMatcher = CORRECTED_CODE_PATTERN.matcher(content);
        while (codeMatcher.find()) {
            correctedCodeSnippets.add(codeMatcher.group(1).trim());
        }
        
        // For now, we only handle the first bug found
        if (bugPositions.isEmpty() || correctedCodeSnippets.isEmpty()) {
            throw new RuntimeException("Could not extract bug position and corrected code from AI response");
        }
        
        return new AIResponse(bugPositions.get(0), correctedCodeSnippets.get(0));
    }
    
    /**
     * Extracts content from API response based on the model's response format.
     */
    private static String extractContentFromApiResponse(String response) throws Exception {
        JsonNode root = JSON_MAPPER.readTree(response);
        
        // Handle OpenAI format
        if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
            JsonNode message = root.get("choices").get(0).get("message");
            if (message != null && message.has("content")) {
                return message.get("content").asText();
            }
        }
        
        // Handle Anthropic Claude format
        if (root.has("content") && root.get("content").isArray()) {
            StringBuilder contentBuilder = new StringBuilder();
            JsonNode contentArray = root.get("content");
            for (JsonNode item : contentArray) {
                if (item.has("text")) {
                    contentBuilder.append(item.get("text").asText());
                }
            }
            if (contentBuilder.length() > 0) {
                return contentBuilder.toString();
            }
        }
        
        // Handle DeepSeek format
        if (root.has("response")) {
            return root.get("response").asText();
        }
        
        // Fallback - just return the raw response if we can't parse it
        return response;
    }
    
    /**
     * Gets the API key for the specified model.
     */
    private static String getApiKey(String model) {
        // Convert model name to uppercase for naming consistency
        String keyName = model.toUpperCase() + "_API_KEY";
        
        // Try to get from properties file first
        String apiKey = API_KEYS.getProperty(keyName);
        
        // If not found in properties, try environment variable
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv(keyName);
        }
        
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("API key not found. Please set it in api-keys.properties or as the " 
                    + keyName + " environment variable.");
        }
        
        return apiKey;
    }
    
    /**
     * Gets the API URL for the specified model.
     */
    private static String getApiUrl(String model) {
        model = model.toLowerCase();
        
        switch (model) {
            case "openai":
                return "https://api.openai.com/v1/chat/completions";
            case "claude":
                return "https://api.anthropic.com/v1/messages";
            case "deepseek":
                return "https://api.deepseek.com/v1/chat/completions";
            default:
                throw new IllegalArgumentException("Unsupported AI model: " + model);
        }
    }
    
    /**
     * Builds the request body for the specified model and prompt.
     */
    private static String buildRequestBody(String model, String prompt) throws Exception {
        model = model.toLowerCase();
        
        switch (model) {
            case "openai":
                return JSON_MAPPER.writeValueAsString(new OpenAIRequest(prompt));
            case "claude":
                return JSON_MAPPER.writeValueAsString(new ClaudeRequest(prompt));
            case "deepseek":
                return JSON_MAPPER.writeValueAsString(new DeepSeekRequest(prompt));
            default:
                throw new IllegalArgumentException("Unsupported AI model: " + model);
        }
    }
    
    /**
     * Class representing the structured response from the AI.
     */
    public static class AIResponse {
        private final String bugPosition;
        private final String correctedCode;
        
        public AIResponse(String bugPosition, String correctedCode) {
            this.bugPosition = bugPosition;
            this.correctedCode = correctedCode;
        }
        
        public String getBugPosition() {
            return bugPosition;
        }
        
        public String getCorrectedCode() {
            return correctedCode;
        }
    }
    
    /**
     * Classes for different AI model request formats
     */
    private static class OpenAIRequest {
        public String model = "gpt-4";
        public List<Message> messages;
        
        public OpenAIRequest(String prompt) {
            this.messages = List.of(new Message("user", prompt));
        }
        
        static class Message {
            public String role;
            public String content;
            
            public Message(String role, String content) {
                this.role = role;
                this.content = content;
            }
        }
    }
    
    private static class ClaudeRequest {
        public String model = "claude-3-opus-20240229";
        public String max_tokens = "4000";
        public List<Message> messages;
        
        public ClaudeRequest(String prompt) {
            this.messages = List.of(new Message(prompt));
        }
        
        static class Message {
            public String role = "user";
            public List<Content> content;
            
            public Message(String prompt) {
                this.content = List.of(new Content(prompt));
            }
            
            static class Content {
                public String type = "text";
                public String text;
                
                public Content(String text) {
                    this.text = text;
                }
            }
        }
    }
    
    private static class DeepSeekRequest {
        public String model = "deepseek-coder";
        public List<Message> messages;
        
        public DeepSeekRequest(String prompt) {
            this.messages = List.of(new Message("user", prompt));
        }
        
        static class Message {
            public String role;
            public String content;
            
            public Message(String role, String content) {
                this.role = role;
                this.content = content;
            }
        }
    }
} 