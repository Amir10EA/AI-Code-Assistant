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

public class AIClient {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Properties API_KEYS = loadApiKeys();
    private static final Pattern BUG_POSITION_PATTERN = 
    Pattern.compile("Bug Position:\\s*(\\d+)-(\\d+)", Pattern.CASE_INSENSITIVE);
    
private static final Pattern CORRECTED_CODE_PATTERN = 
    Pattern.compile("Corrected Code:\\s*```java\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static Properties loadApiKeys() {
        Properties properties = new Properties();
        try (InputStream in = AIClient.class.getClassLoader().getResourceAsStream("api-keys.properties")) {
            if (in != null) {
                properties.load(in);
                System.out.println("[DEBUG] Loaded API keys from properties file");
            }
        } catch (IOException e) {
            System.out.println("[WARN] No api-keys.properties found");
        }
        return properties;
    }

    public static String sendRequest(String model, String prompt) throws Exception {
        System.out.println("\n=== SENDING REQUEST ===");
        System.out.println("[DEBUG] Model: " + model);
        System.out.println("[DEBUG] Prompt:\n" + prompt);

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

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("\n=== RAW RESPONSE ===");
        System.out.println("[DEBUG] Status Code: " + response.statusCode());
        System.out.println("[DEBUG] Response Body:\n" + response.body());
        
        return response.body();
    }

    public static AIResponse parseResponse(String response) throws Exception {
        System.out.println("\n=== PARSING RESPONSE ===");
        String content = extractContentFromApiResponse(response);
        
        if (content == null || content.isBlank()) {
            System.out.println("[ERROR] Empty content in response");
            return null;
        }

        System.out.println("[DEBUG] Extracted Content:\n" + content);

        if (content.toLowerCase().contains("no bugs found") || 
            content.toLowerCase().contains("no issues")) {
            System.out.println("[INFO] AI reported no bugs found");
            return null;
        }

        Matcher posMatcher = BUG_POSITION_PATTERN.matcher(content);
        Matcher codeMatcher = CORRECTED_CODE_PATTERN.matcher(content);

        boolean hasPosition = posMatcher.find();
        boolean hasCode = codeMatcher.find();

        System.out.println("[DEBUG] Position match: " + hasPosition);
        System.out.println("[DEBUG] Code match: " + hasCode);

        if (hasPosition && hasCode) {
            String bugPosition = posMatcher.group(1) + "-" + posMatcher.group(2);
            String correctedCode = codeMatcher.group(1).trim();
            
            System.out.println("[DEBUG] Parsed Position: " + bugPosition);
            System.out.println("[DEBUG] Parsed Code:\n" + correctedCode);
            
            return new AIResponse(bugPosition, correctedCode);
        }

        System.out.println("[ERROR] Failed to parse required elements");
        System.out.println("[DEBUG] Full content for analysis:\n" + content);
        return null;
    }

    private static String extractContentFromApiResponse(String response) throws Exception {
        System.out.println("\n=== EXTRACTING CONTENT ===");
        try {
            JsonNode root = JSON_MAPPER.readTree(response);
            System.out.println("[DEBUG] JSON Structure:\n" + root.toPrettyString());

            if (root.has("choices")) {
                JsonNode choice = root.get("choices").get(0);
                if (choice.has("message") && choice.get("message").has("content")) {
                    String content = choice.get("message").get("content").asText();
                    System.out.println("[DEBUG] Extracted OpenAI content");
                    return content;
                }
            }

            if (root.has("content")) {
                StringBuilder contentBuilder = new StringBuilder();
                for (JsonNode item : root.get("content")) {
                    if (item.has("text")) {
                        contentBuilder.append(item.get("text").asText());
                    }
                }
                System.out.println("[DEBUG] Extracted Anthropic content");
                return contentBuilder.toString();
            }

            if (root.has("response")) {
                System.out.println("[DEBUG] Extracted DeepSeek content");
                return root.get("response").asText();
            }

            System.out.println("[WARN] Unknown response format");
            return response;
            
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to parse JSON: " + e.getMessage());
            System.out.println("[DEBUG] Raw response:\n" + response);
            return response;
        }
    }

    private static String getApiKey(String model) {
        String keyName = model.toUpperCase() + "_API_KEY";
        String apiKey = API_KEYS.getProperty(keyName, System.getenv(keyName));
        
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Missing API key for " + model);
        }
        return apiKey;
    }

    private static String getApiUrl(String model) {
        return switch (model.toLowerCase()) {
            case "openai" -> "https://api.openai.com/v1/chat/completions";
            case "claude" -> "https://api.anthropic.com/v1/messages";
            case "deepseek" -> "https://api.deepseek.com/v1/chat/completions";
            default -> throw new IllegalArgumentException("Unsupported model: " + model);
        };
    }

    private static String buildRequestBody(String model, String prompt) throws Exception {
        return switch (model.toLowerCase()) {
            case "openai" -> JSON_MAPPER.writeValueAsString(new OpenAIRequest(prompt));
            case "claude" -> JSON_MAPPER.writeValueAsString(new ClaudeRequest(prompt));
            case "deepseek" -> JSON_MAPPER.writeValueAsString(new DeepSeekRequest(prompt));
            default -> throw new IllegalArgumentException("Unsupported model: " + model);
        };
    }

    public static class AIResponse {
        private final String bugPosition;
        private final String correctedCode;

        public AIResponse(String bugPosition, String correctedCode) {
            this.bugPosition = bugPosition;
            this.correctedCode = correctedCode;
        }

        public String getBugPosition() { return bugPosition; }
        public String getCorrectedCode() { return correctedCode; }
    }

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
        public int max_tokens = 4000;
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