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
    
    // Enhanced regex with flexible whitespace handling
    private static final Pattern BUG_BLOCK_PATTERN = Pattern.compile(
        "BUG\\s+LOCATION:\\s*(.+?)\\n" +
        "BUG\\s+TYPE:\\s*(.+?)\\n" +
        "EXPLANATION:\\s*([\\s\\S]*?)\\n" +
        "ORIGINAL\\s+CODE:\\s*```java\\s*([\\s\\S]*?)```\\s*\\n" +
        "CORRECTED\\s+CODE:\\s*```java\\s*([\\s\\S]*?)```",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static Properties loadApiKeys() {
        Properties properties = new Properties();
        try (InputStream in = AIClient.class.getClassLoader().getResourceAsStream("api-keys.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            // Silent fail - will try environment variables
        }
        return properties;
    }

    public static String sendRequest(String model, String prompt, boolean verbose) throws Exception {
        if (verbose) {
            System.out.println("\n=== SENDING REQUEST ===");
            System.out.println("[DEBUG] Model: " + model);
            System.out.println("[DEBUG] Prompt:\n" + prompt);
        }

        String apiKey = getApiKey(model);
        String apiUrl = getApiUrl(model);

        HttpClient client = HttpClient.newHttpClient();
        String requestBody = buildRequestBody(model, prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header(model.toLowerCase().equals("claude") ? "x-api-key" : "Authorization", 
                       model.toLowerCase().equals("claude") ? apiKey : "Bearer " + apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (verbose) {
            System.out.println("\n=== RAW RESPONSE ===");
            System.out.println("[DEBUG] Status Code: " + response.statusCode());
            System.out.println("[DEBUG] Response Body:\n" + response.body());
        }
        
        return response.body();
    }

    public static AIResponse parseResponse(String response, boolean verbose) throws Exception {
        if (verbose) {
            System.out.println("\n=== PARSING RESPONSE ===");
        }
        
        String content = extractContentFromApiResponse(response, verbose);
        
        if (content == null || content.trim().isEmpty()) {
            System.err.println("[ERROR] Empty or null content in response");
            return null;
        }

        if (verbose) {
            System.out.println("[DEBUG] Extracted Content:\n" + content);
        }
        
        List<AIResponse.BugFix> bugFixes = new ArrayList<>();
        Matcher matcher = BUG_BLOCK_PATTERN.matcher(content);
        int blockCount = 0;
        
        while (matcher.find()) {
            blockCount++;
            try {
                String bugPosition = matcher.group(1).trim();
                String bugType = matcher.group(2).trim();
                String explanation = matcher.group(3).trim();
                String originalCode = matcher.group(4).trim();
                String correctedCode = matcher.group(5).trim();

                // Basic validation
                if (correctedCode.isEmpty() || originalCode.isEmpty()) {
                    if (verbose) {
                        System.err.println("[WARN] Skipping block " + blockCount + ": Empty code snippet detected");
                    }
                    continue;
                }

                AIResponse.BugFix bugFix = new AIResponse.BugFix(
                    bugPosition, 
                    correctedCode,
                    bugType,
                    explanation,
                    originalCode
                );
                bugFixes.add(bugFix);
                
                if (verbose) {
                    System.out.println("[DEBUG] Parsed Bug Fix " + blockCount + ":");
                    System.out.println("- Position: " + bugPosition);
                    System.out.println("- Type: " + bugType);
                    System.out.println("- Explanation: " + explanation);
                }
                
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to parse bug block " + blockCount + ": " + e.getMessage());
            }
        }

        // Extract complete file
        String completeFile = null;
        Pattern completeFilePattern = Pattern.compile(
            "COMPLETE\\s+FILE:\\s*```java\\s*([\\s\\S]*?)```",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher completeFileMatcher = completeFilePattern.matcher(content);
        if (completeFileMatcher.find()) {
            completeFile = completeFileMatcher.group(1).trim();
        }

        if (!bugFixes.isEmpty()) {
            if (verbose) {
                System.out.println("[INFO] Successfully parsed " + bugFixes.size() + " bug fixes out of " + blockCount + " blocks");
            }
            return new AIResponse(bugFixes, completeFile);
        }
        
        if (verbose) {
            System.out.println("[ERROR] Failed to parse any bug fixes from " + blockCount + " potential blocks");
            System.out.println("[DEBUG] Full content for analysis:\n" + content);
        }
        return null;
    }

    private static String extractContentFromApiResponse(String response, boolean verbose) throws Exception {
        if (verbose) {
            System.out.println("\n=== EXTRACTING CONTENT ===");
        }
        
        try {
            JsonNode root = JSON_MAPPER.readTree(response);
            if (verbose) {
                System.out.println("[DEBUG] JSON Structure:\n" + root.toPrettyString());
            }

            if (root.has("choices")) {
                JsonNode choice = root.get("choices").get(0);
                if (choice.has("message") && choice.get("message").has("content")) {
                    String content = choice.get("message").get("content").asText();
                    if (verbose) {
                        System.out.println("[DEBUG] Extracted OpenAI content");
                    }
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
                if (verbose) {
                    System.out.println("[DEBUG] Extracted Anthropic content");
                }
                return contentBuilder.toString();
            }

            if (root.has("response")) {
                if (verbose) {
                    System.out.println("[DEBUG] Extracted DeepSeek content");
                }
                return root.get("response").asText();
            }

            if (verbose) {
                System.out.println("[WARN] Unknown response format, returning raw response");
            }
            return response;
            
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to parse JSON: " + e.getMessage());
            if (verbose) {
                System.out.println("[DEBUG] Raw response:\n" + response);
            }
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
        private final List<BugFix> bugFixes;
        private final String completeFile;
        
        public AIResponse(List<BugFix> bugFixes, String completeFile) {
            this.bugFixes = bugFixes;
            this.completeFile = completeFile;
        }

        public List<BugFix> getBugFixes() { 
            return bugFixes; 
        }

        public String getCompleteFile() {
            return completeFile;
        }

        public void printBugSummary() {
            // First, collect all bug locations
            StringBuilder locations = new StringBuilder("Bug locations: ");
            for (int i = 0; i < bugFixes.size(); i++) {
                BugFix bugFix = bugFixes.get(i);
                String[] positionParts = bugFix.getBugPosition().split(":");
                if (positionParts.length >= 2) {
                    String lineNumbers = positionParts[1].trim();
                    // Remove any file name that might have been included in the line numbers
                    lineNumbers = lineNumbers.replaceAll("\\s*,\\s*[^0-9-]+", "");
                    locations.append(lineNumbers);
                    if (i < bugFixes.size() - 1) {
                        locations.append(", ");
                    }
                }
            }
            System.out.println("\n" + locations.toString());
            
            // Then show each bug's details
            System.out.println("\nBug Details:");
            for (BugFix bugFix : bugFixes) {
                String[] positionParts = bugFix.getBugPosition().split(":");
                if (positionParts.length >= 2) {
                    String fileName = positionParts[0].trim();
                    String lineNumbers = positionParts[1].trim();
                    // Clean up the line numbers
                    lineNumbers = lineNumbers.replaceAll("\\s*,\\s*[^0-9-]+", "");
                    System.out.println("Location: " + fileName + ":" + lineNumbers);
                    System.out.println("Type: " + bugFix.getBugType());
                    System.out.println("Explanation: " + bugFix.getExplanation());
                    System.out.println();
                }
            }
            
            // Show the proposed changes
            System.out.println("Proposed Changes:");
            String fileName = bugFixes.get(0).getBugPosition().split(":")[0].trim();
            System.out.println("file: " + fileName);
            System.out.println("<<<<<<< SEARCH");
            for (BugFix bugFix : bugFixes) {
                System.out.println(bugFix.getOriginalCode());
            }
            System.out.println("=======");
            for (BugFix bugFix : bugFixes) {
                System.out.println(bugFix.getCorrectedCode());
            }
            System.out.println(">>>>>>> REPLACE");
        }

        public static class BugFix {
            private final String bugPosition;
            private final String correctedCode;
            private final String bugType;
            private final String explanation;
            private final String originalCode;
            
            public BugFix(String bugPosition, String correctedCode, 
                        String bugType, String explanation, String originalCode) {
                this.bugPosition = bugPosition;
                this.correctedCode = correctedCode;
                this.bugType = bugType;
                this.explanation = explanation;
                this.originalCode = originalCode;
            }

            public String getBugPosition() { return bugPosition; }
            public String getCorrectedCode() { return correctedCode; }
            public String getBugType() { return bugType; }
            public String getExplanation() { return explanation; }
            public String getOriginalCode() { return originalCode; }
        }
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
        public String model = "claude-3-7-sonnet-20250219";
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