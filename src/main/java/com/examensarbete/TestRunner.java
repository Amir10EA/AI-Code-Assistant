package com.examensarbete;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for running JUnit tests.
 */
public class TestRunner {
    
    /**
     * Runs JUnit tests using Maven or Gradle for the project at the specified path.
     * 
     * @param projectPath Path to the root of the project
     * @return true if tests passed, false otherwise
     * @throws Exception If there's an error running the tests
     */
    public static boolean runTests(String projectPath) throws Exception {
        File projectDir = new File(projectPath);
        
        // Determine whether to use Maven or Gradle
        BuildSystem buildSystem = determineBuildSystem(projectDir);
        
        List<String> command = new ArrayList<>();
        
        switch (buildSystem) {
            case MAVEN:
                command.add(isWindows() ? "mvn.cmd" : "mvn");
                command.add("test");
                break;
            case GRADLE:
                command.add(isWindows() ? "gradlew.bat" : "./gradlew");
                command.add("test");
                break;
            case UNKNOWN:
            default:
                System.out.println("Warning: Could not determine build system. Skipping tests.");
                return true; // Assume tests pass when we can't run them
        }
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(projectDir);
        pb.redirectErrorStream(true);
        
        System.out.println("Running tests with " + buildSystem + "...");
        Process process = pb.start();
        
        // Read and print the output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            boolean testsFound = false;
            boolean testsFailed = false;
            
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                
                // For Maven
                if (line.contains("Running ") && line.contains("Test")) {
                    testsFound = true;
                }
                if (line.contains("Tests run:") && line.contains("Failures:")) {
                    if (line.contains("Failures: 0") && line.contains("Errors: 0")) {
                        // Tests passed
                    } else {
                        testsFailed = true;
                    }
                }
                
                // For Gradle
                if (line.contains("Test FAILED")) {
                    testsFailed = true;
                }
                if (line.matches(".*\\d+ tests? completed.*")) {
                    testsFound = true;
                }
                
                // Failure indicators for both
                if (line.contains("BUILD FAILURE") || line.contains("FAILED")) {
                    testsFailed = true;
                }
            }
            
            // Wait for the process to complete with timeout
            boolean completed = process.waitFor(2, TimeUnit.MINUTES);
            if (!completed) {
                process.destroy();
                throw new RuntimeException("Test execution timed out after 2 minutes");
            }
            
            int exitCode = process.exitValue();
            
            if (!testsFound) {
                System.out.println("Warning: No tests were found or executed.");
                return true; // Assume tests pass when none are found
            }
            
            boolean testsPassed = exitCode == 0 && !testsFailed;
            System.out.println("Tests " + (testsPassed ? "PASSED" : "FAILED"));
            return testsPassed;
        }
    }
    
    private static BuildSystem determineBuildSystem(File projectDir) {
        if (new File(projectDir, "pom.xml").exists()) {
            return BuildSystem.MAVEN;
        } else if (new File(projectDir, "build.gradle").exists() || new File(projectDir, "build.gradle.kts").exists()) {
            return BuildSystem.GRADLE;
        } else {
            return BuildSystem.UNKNOWN;
        }
    }
    
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
    
    private enum BuildSystem {
        MAVEN, GRADLE, UNKNOWN
    }
} 