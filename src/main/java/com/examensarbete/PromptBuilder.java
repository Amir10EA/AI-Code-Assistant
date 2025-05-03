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
     * Builds a general bug finding prompt
     */
    public String buildBugFindingPrompt(String fileContent) {
        return String.format("""
            %s
            
            CODE TO ANALYZE:
            ```java
            %s
            ```
            
            COMMON TEST FAILURE PATTERNS TO CONSIDER:
            1. Unexpected null values
            2. Incorrect calculations/formulas
            3. Mismatched field/method names
            4. Off-by-one errors
            5. Incorrect conditional logic
            6. Type conversion issues
            
            REQUIRED RESPONSE FORMAT:
            ``` 
            BUG LOCATION: [filename]:[start line]-[end line]
            BUG TYPE: [Logical Error | Calculation Error | Field Mismatch | ...]
            EXPLANATION: [Clear technical reason]
            COMPLETE FILE:
            ```java
            [ENTIRE FILE WITH ALL FIXES APPLIED]
            ```
            ```
            """, SYSTEM_PROMPT, fileContent);
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