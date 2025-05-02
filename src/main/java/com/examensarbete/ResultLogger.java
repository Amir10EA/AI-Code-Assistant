package com.examensarbete;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ResultLogger {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static void logResult(String logFilePath, String bugPosition, String correctedCode, 
    boolean applied, boolean initialTestsPassed, boolean finalTestsPassed) throws Exception {
        StringBuilder log = new StringBuilder();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        
        log.append("==================================================\n");
        log.append("DEBUGGING SESSION: ").append(timestamp).append("\n");
        log.append("==================================================\n\n");
        
        log.append("Bug Position: ").append(bugPosition).append("\n\n");
        
        log.append("Corrected Code:\n```java\n").append(correctedCode).append("\n```\n\n");
        
        log.append("Fix Applied: ").append(applied ? "YES" : "NO").append("\n");
        
        if (applied) {
            log.append("Initial Tests: ").append(initialTestsPassed ? "PASSED" : "FAILED").append("\n");
            log.append("Post-Fix Tests: ").append(finalTestsPassed ? "PASSED" : "FAILED").append("\n");
        }
        
        log.append("\n--------------------------------------------------\n\n");
        
        Path path = Path.of(logFilePath);
        
        // Create parent directories if they don't exist
        Files.createDirectories(path.getParent() != null ? path.getParent() : Path.of("."));
        
        // Write to log file
        Files.writeString(
            path, 
            log.toString(), 
            StandardOpenOption.CREATE, 
            StandardOpenOption.APPEND
        );
        
        System.out.println("Results logged to: " + logFilePath);
    }
}