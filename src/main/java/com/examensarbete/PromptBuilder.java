package com.examensarbete;

/**
 * Utility class to build prompts for AI models.
 */
public class PromptBuilder {

    /**
     * Builds a prompt for bug finding, emphasizing logical errors based on intended functionality.
     *
     * @param fileContent The content of the Java file to analyze
     * @return A formatted prompt for the AI model
     */
    public String buildBugFindingPrompt(String fileContent) {
      return """
          Analyze this Java code for CRITICAL bugs. Focus on the `add` method.
          
          REQUIRED BEHAVIOR:
          - The add() method MUST return the sum of parameters 'a' and 'b'
          - Any other return value is a SEVERE ERROR
          
          CURRENT BUG EXAMPLE:
          ```java
          return 0; // ← THIS IS WRONG AND MUST BE FIXED
          ```
          
          RESPONSE FORMAT:
          Bug Position: [start line]-[end line]
          Corrected Code:
          ```java
          [EXACT CORRECTED LINE(S)]
          ```
          Explanation: [Concise reason]
          
          CODE TO ANALYZE:
          ```java
          %s
          ```
      """.formatted(fileContent);
  }

    /**
     * Builds a highly detailed prompt for bug fixing, emphasizing identification of all error types and precise correction, including expected behavior.
     *
     * @param code The content of the Java file to analyze
     * @return A formatted prompt for the AI model
     */
    public String buildFixCodePrompt(String code) {
        return """
            You are an expert-level Java software engineer with a specialization in debugging and code correction. Your mission is to meticulously analyze the following Java code to identify and provide precise fixes for all types of errors, including critical syntax errors that prevent compilation, logical errors leading to incorrect behavior, and potential runtime exceptions.

            Consider the intended functionality of the `add` method: it should return the sum of the two input integers.

            Your analysis MUST be thorough and your corrections must be minimal and targeted, affecting only the lines of code that contain the error. You must also explain the nature of the bug and the rationale behind your correction.

            Specifically, you need to:
            1. **Identify ALL bugs** present in the provided Java code, without exception. This includes syntax errors, logical flaws (where the code does not perform its intended function), and potential runtime issues.
            2. **Pinpoint the exact location** of each bug, specifying the file name and the precise line number(s) where the error occurs.
            3. **Provide a corrected version of ONLY the line(s) of code** that need to be changed to fix the bug. The corrected code MUST maintain the original indentation and coding style of the surrounding code.
            4. **Clearly and concisely explain** the nature of the bug: what is the error, why does it occur, and what negative impact does it have on the program's functionality or correctness (e.g., incorrect calculation).
            5. **Justify your correction:** explain exactly how your corrected code resolves the identified bug and why it is the correct solution (e.g., it performs the intended addition).

            Your response for each bug MUST adhere STRICTLY to the following format:

            ```
            Bug Location: [file name]:[start line]-[end line]
            Bug Type: [Syntax Error | Logical Error | Potential Runtime Error]
            Explanation: [Detailed explanation of the bug]
            Corrected Code:
            ```java
            [corrected line(s) of code with original indentation]
            ```
            Reasoning for Correction: [Explanation of why the corrected code is the correct solution]

            If no bugs are found in the provided code after your thorough analysis, you MUST respond with the single phrase: "No bugs found."

            Remember to:
            - Preserve all import statements and the package declaration.
            - Adhere to the existing coding style and indentation.
            - Only provide the corrected lines of code.
            - Explain both the bug and your reasoning for the fix, keeping in mind the intended functionality of the `add` method.

            ORIGINAL CODE:
            ```java
            %s
            ```
        """.formatted(code);
    }

    /**
     * Legacy method to maintain compatibility with existing code
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