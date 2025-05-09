package com.examensarbete;

/**
 * Utility class to build prompts for AI models.
 */
public class PromptBuilder {
    
    private static final String SYSTEM_PROMPT = """
        You are an expert Java debugging assistant. Analyze the code for:
        1. Logical errors 2. Incorrect calculations 3. Field/method mismatches
        4. Inheritance issues 5. Incorrect API usage 6. Resource leaks
        7. Null handling 8. Type mismatches 9. Incorrect method overrides
        
        RESPONSE FORMAT:
        1. BUG LOCATION: [file name]:[start line]-[end line]
        2. BUG TYPE: [Logical Error | Calculation Error | Field Mismatch | etc]
        3. EXPLANATION: [Concise technical explanation]
        4. COMPLETE FILE: [The entire file with all fixes applied]
        
        Rules:
        - Preserve original code style and comments
        - Only fix what's broken
        - Maintain existing indentation
        - Consider test failure patterns
        - Return the complete file with all fixes applied""";
    
    /**
     * Builds a general bug finding prompt with embedded line numbers.
     */
    public String buildBugFindingPrompt(String code) {
        // Split the code into lines and add line numbers
        String[] lines = code.split("\n");
        StringBuilder numberedCode = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            numberedCode.append(String.format("%3d | %s\n", i + 1, lines[i]));
        }

        return """
            Analyze the following Java code and identify any bugs or issues. For each bug found, provide:
            
            LINE COUNTING RULES:
            1. The code is provided with line numbers in the format "  N | code", where N is the line number starting from 1.
            2. When reporting BUG LOCATION, use the line number shown before the pipe (|).
            3. Each bug must be reported with its exact starting line number as shown.
            4. Include line numbers in ORIGINAL CODE and CORRECTED CODE snippets.
            
            For each bug found, provide:
            1. BUG LOCATION: <filename>:<exact line number as shown>
            2. BUG TYPE: <type of bug>
            3. EXPLANATION: <detailed explanation of the bug>
            4. ORIGINAL CODE: <the problematic code snippet with line numbers>
            5. CORRECTED CODE: <the fixed version with line numbers>
            
            Format your response using the following template for each bug:
            
            BUG LOCATION: <filename>:<exact line number>
            BUG TYPE: <type of bug>
            EXPLANATION: <detailed explanation>
            
            ORIGINAL CODE:
            ```java
            <original code snippet with line numbers>
            ```
            
            CORRECTED CODE:
            ```java
            <corrected code snippet with line numbers>
            ```
            
            After identifying all bugs, provide the COMPLETE FILE with all fixes applied, WITHOUT line numbers:
            
            COMPLETE FILE:
            ```java
            <entire file with all fixes applied, without line numbers>
            ```
            
            IMPORTANT RULES:
            1. Preserve ALL Javadoc comments exactly as they are
            2. Keep the original code structure and formatting
            3. Only modify the specific buggy code sections
            4. Maintain all imports and package declarations
            5. Keep all class and method signatures unchanged except for the bug fixes
            6. NEVER use ranges or group related bugs together
            7. IMPORTANT: Line numbers must be exactly as shown before the pipe. Do not estimate or calculate them.
            
            EXAMPLE:
            If the code is:
            ```
             1 | public class Example {
             2 |     public void method() {
             3 |         System.out.println("Hello");
             4 |     }
             5 | }
            ```
            And there’s a bug at line 3, report it as:
            BUG LOCATION: Example.java:3
            
            Code to analyze:
            ```
            %s
            ```
            """.formatted(numberedCode.toString());
    }

    /**
     * Build prompt for code fixing with test context
     */
    public String buildFixCodePrompt(String code, String testOutput) {
        return String.format("""
            %s
            
            CODE TO FIX:
            ```java
            %s
            ```
            
            TEST FAILURES:
            %s
            
            RESPONSE REQUIREMENTS:
            1. Address all test failures with minimal changes
            2. Preserve original code structure
            3. Maintain existing comments
            4. Keep identical indentation
            5. Never introduce new dependencies
            6. Return the complete file with all fixes applied
            """, SYSTEM_PROMPT, code, testOutput);
    }

    /**
     * Build prompt from custom template
     */
    public static String buildPrompt(String fileContent, String userPrompt) {
        return String.format("""
            %s
            
            USER INSTRUCTIONS:
            %s
            
            CODE TO ANALYZE:
            ```java
            %s
            ```
            
            RESPONSE FORMAT:
            ``` 
            BUG LOCATION: [filename]:[start line]-[end line]
            BUG TYPE: [Category]
            EXPLANATION: [Technical rationale]
            COMPLETE FILE:
            ```java
            [ENTIRE FILE WITH ALL FIXES APPLIED]
            ```
            ```
            """, SYSTEM_PROMPT, userPrompt, fileContent);
    }
}