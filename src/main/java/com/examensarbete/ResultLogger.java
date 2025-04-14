package com.examensarbete;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for logging debugging results.
 */
public class ResultLogger {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Logs the debugging results to a file.
     * 
     * @param logFilePath Path to the log file
     * @param bugPosition The position of the bug
     * @param correctedCode The corrected code snippet
     * @param applied Whether the fix was applied
     * @param testsPassed Whether the tests passed after applying the fix
     * @throws Exception If there's an error writing to the log file
     */
    public static void logResult(String logFilePath, String bugPosition, String correctedCode, boolean applied, boolean testsPassed) throws Exception {
        StringBuilder log = new StringBuilder();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        
        log.append("==================================================\n");
        log.append("DEBUGGING SESSION: ").append(timestamp).append("\n");
        log.append("==================================================\n\n");
        
        log.append("Bug Position: ").append(bugPosition).append("\n\n");
        
        log.append("Corrected Code:\n```java\n").append(correctedCode).append("\n```\n\n");
        
        log.append("Fix Applied: ").append(applied ? "YES" : "NO").append("\n");
        
        if (applied) {
            log.append("Tests Passed: ").append(testsPassed ? "YES" : "NO").append("\n");
        }
        
        log.append("\n--------------------------------------------------\n\n");
        
        Path path = Path.of(logFilePath);
        
        // Create parent directories if they don't exist
        Files.createDirectories(path.getParent() != null ? path.getParent() : Path.of("."));
        
        // Write to log file, creating it if it doesn't exist
        Files.writeString(
            path, 
            log.toString(), 
            StandardOpenOption.CREATE, 
            StandardOpenOption.APPEND
        );
        
        System.out.println("Results logged to: " + logFilePath);
    }
} 