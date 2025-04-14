package com.examensarbete;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for applying code fixes to source files.
 */
public class CodePatcher {
    
    /**
     * Applies a code patch to the specified file.
     * 
     * @param filePath Path to the file to patch
     * @param correctedCode The corrected code snippet
     * @param bugPosition The position of the bug in the format "startLine-endLine"
     * @param apply Whether to apply the fix or not
     * @throws Exception If there's an error reading or writing the file
     */
    public static void applyPatch(String filePath, String correctedCode, String bugPosition, boolean apply) throws Exception {
        if (!apply) {
            System.out.println("Fix not applied.");
            return;
        }
        
        Path path = Path.of(filePath);
        List<String> lines = Files.readAllLines(path);
        
        // Create a backup of the original file
        String backupPath = filePath + ".bak";
        Files.copy(path, Path.of(backupPath), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Created backup at: " + backupPath);
        
        // Parse bug position
        String[] positions = bugPosition.split("-");
        if (positions.length != 2) {
            throw new IllegalArgumentException("Invalid bug position format: " + bugPosition);
        }
        
        int startLine = Integer.parseInt(positions[0]);
        int endLine = Integer.parseInt(positions[1]);
        
        // Adjust for zero-based indexing
        startLine = Math.max(1, startLine) - 1;
        endLine = Math.min(lines.size(), endLine);
        
        if (startLine < 0 || startLine >= lines.size() || endLine <= startLine || endLine > lines.size()) {
            throw new IllegalArgumentException("Bug position out of range: " + bugPosition);
        }
        
        // Split the corrected code into lines
        List<String> correctedLines = new ArrayList<>(List.of(correctedCode.split("\n")));
        
        // Replace the buggy lines with the corrected ones
        List<String> newFileContent = new ArrayList<>();
        newFileContent.addAll(lines.subList(0, startLine));
        newFileContent.addAll(correctedLines);
        newFileContent.addAll(lines.subList(endLine, lines.size()));
        
        // Write the updated content back to the file
        Files.write(path, newFileContent);
        
        System.out.println("Successfully applied fix to " + filePath);
        System.out.println("- Original file backed up to: " + backupPath);
        System.out.println("- Replaced lines " + (startLine + 1) + " to " + endLine);
    }
} 