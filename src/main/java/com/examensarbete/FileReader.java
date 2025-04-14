package com.examensarbete;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class to read file contents.
 */
public class FileReader {
    
    /**
     * Reads the content of a file at the specified path.
     * 
     * @param filePath Path to the file
     * @return The content of the file as a string
     * @throws Exception If the file cannot be read
     */
    public static String readFile(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }
        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("File is not readable: " + filePath);
        }
        return Files.readString(path);
    }
} 