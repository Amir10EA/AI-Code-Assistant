package com.examensarbete;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Utility class for applying code fixes to source files.
 */
public class CodePatcher {
    private static final Logger LOGGER = Logger.getLogger(CodePatcher.class.getName());

    /**
     * Applies a code patch by replacing the entire file content with the corrected version.
     *
     * @param filePath      Path to the file to patch
     * @param correctedCode The complete corrected file content
     * @param apply         Whether to apply the fix or not
     * @return true if the patch was successfully applied, false otherwise
     * @throws Exception If there's an error reading or writing the file
     */
    public static boolean applyPatch(String filePath, String correctedCode, boolean apply) throws Exception {
        LOGGER.setLevel(Level.ALL);

        if (!apply) {
            LOGGER.info("Fix not applied for file: " + filePath);
            System.out.println("Fix not applied.");
            return false;
        }

        LOGGER.info("Attempting to apply patch to: " + filePath);
        LOGGER.fine("Corrected code to apply:\n" + correctedCode);

        Path path = Path.of(filePath);
        
        // Create backup of original content
        String originalContent = Files.readString(path);
        
        // Validate the corrected code
        if (!validatePatch(correctedCode)) {
            LOGGER.warning("Patch validation failed - not applying changes");
            return false;
        }
        
        // Apply the changes
        Files.writeString(path, correctedCode);
        LOGGER.info("Successfully applied fix to: " + filePath);
        System.out.println("Successfully applied fix to " + filePath);
        return true;
    }
    
    /**
     * Validates the patched code by checking for basic syntax errors.
     */
    private static boolean validatePatch(String patchedCode) {
        // Basic validation - check for unmatched braces, brackets, etc.
        int braceCount = 0;
        int bracketCount = 0;
        int parenCount = 0;
        
        for (char c : patchedCode.toCharArray()) {
            switch (c) {
                case '{' -> braceCount++;
                case '}' -> braceCount--;
                case '[' -> bracketCount++;
                case ']' -> bracketCount--;
                case '(' -> parenCount++;
                case ')' -> parenCount--;
            }
        }
        
        return braceCount == 0 && bracketCount == 0 && parenCount == 0;
    }
}