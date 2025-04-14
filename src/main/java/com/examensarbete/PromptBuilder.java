package com.examensarbete;

/**
 * Utility class to build prompts for AI models.
 */
public class PromptBuilder {
    
    /**
     * Builds a prompt for the AI model combining the file content with the user's prompt.
     * Includes instructions for the AI to format its response in a specific way.
     * 
     * @param fileContent The content of the Java file to analyze
     * @param userPrompt The user-defined prompt
     * @return A formatted prompt for the AI model
     */
    public static String buildPrompt(String fileContent, String userPrompt) {
        StringBuilder prompt = new StringBuilder();
        
        // Add user prompt
        prompt.append(userPrompt).append("\n\n");
        
        // Add the code to analyze
        prompt.append("Code to analyze:\n```java\n")
              .append(fileContent)
              .append("\n```\n\n");
        
        // Add instructions for response format
        prompt.append("Instructions:\n")
              .append("1. Identify bugs in the Java code above.\n")
              .append("2. For each bug, provide its position (line numbers) and a corrected code snippet.\n")
              .append("3. Format your response EXACTLY as follows:\n\n")
              .append("Bug Position: [start line]-[end line]\n")
              .append("Corrected Code:\n")
              .append("```java\n")
              .append("[corrected code snippet with proper indentation]\n")
              .append("```\n\n")
              .append("4. If you find multiple bugs, list them separately with the same format.\n")
              .append("5. Only include the exact lines that need to be changed in the corrected code snippets.\n")
              .append("6. Ensure your corrected code maintains the original indentation style.\n");
        
        return prompt.toString();
    }
} 