package com.examensarbete;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TestRunner {
    private static int totalTests = 0;
    private static int failedTests = 0;
    private static int errorTests = 0;
    private static int skippedTests = 0;
    
    private static final int TEST_TIMEOUT_MINUTES = 15;
    
    public static boolean runTests(String projectPath) throws Exception {
        File projectDir = new File(projectPath);
        BuildSystem buildSystem = determineBuildSystem(projectDir);
        List<String> command = buildCommand(buildSystem);
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(projectDir);
        pb.redirectErrorStream(true);
        
        System.out.println("\n🔄 Starting test execution...");
        System.out.println("⏳ Timeout set to: " + TEST_TIMEOUT_MINUTES + " minutes");
        System.out.println("📂 Project root: " + projectDir.getAbsolutePath());
        
        long startTime = System.currentTimeMillis();
        Process process = pb.start();
        
        Thread outputThread = new Thread(() -> readStream(process.getInputStream()));
        outputThread.start();
        
        boolean completed = process.waitFor(TEST_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        long durationSeconds = (System.currentTimeMillis() - startTime) / 1000;
        
        if (!completed) {
            System.err.println("\n⛔ Timeout after " + durationSeconds + " seconds!");
            process.destroyForcibly();
            throw new RuntimeException("Test execution timed out");
        }
        
        int exitCode = process.exitValue();
        System.out.println("[DEBUG] Process exit code: " + exitCode);
        System.out.println("\n✅ Test execution completed in " + durationSeconds + " seconds");
        
        parseTestResults(projectPath);
        
        boolean testsExecuted = Files.walk(Path.of(projectPath, "target", "surefire-reports"))
            .anyMatch(p -> p.toString().endsWith(".xml"));
        
        if (!testsExecuted) {
            System.err.println("\n⛔ No test reports found. Possible compilation failure.");
            return false;
        }
        
        printTestSummary();
        return (failedTests + errorTests) == 0;
    }

    private static void readStream(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[BUILD OUTPUT] " + line);
                if (line.contains("COMPILATION ERROR") || line.contains("Failed to execute goal")) {
                    System.err.println("[ERROR] Build issue detected: " + line);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading build output: " + e.getMessage());
        }
    }

    private static void parseTestResults(String projectPath) throws Exception {
        Path reportsDir = Path.of(projectPath, "target", "surefire-reports");
        totalTests = 0;
        failedTests = 0;
        errorTests = 0;
        skippedTests = 0;
    
        if (!Files.exists(reportsDir)) {
            System.err.println("⚠️ No test reports directory found");
            return;
        }
    
        Files.walk(reportsDir)
            .filter(path -> path.toString().endsWith(".xml"))
            .forEach(path -> {
                try {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(false);
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(path.toFile());
    
                    NodeList testCases = doc.getElementsByTagName("testcase");
                    totalTests += testCases.getLength();
    
                    for (int i = 0; i < testCases.getLength(); i++) {
                        Element testCase = (Element) testCases.item(i);
                        
                        // Corrected checks using getLength()
                        if (testCase.getElementsByTagName("failure").getLength() > 0) {
                            failedTests++;
                        } else if (testCase.getElementsByTagName("error").getLength() > 0) {
                            errorTests++;
                        } else if (testCase.getElementsByTagName("skipped").getLength() > 0) {
                            skippedTests++;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Error parsing " + path + ": " + e.getMessage());
                }
            });
        
        System.out.println("[DEBUG] Parsed results - Total: " + totalTests 
            + ", Failed: " + failedTests 
            + ", Errors: " + errorTests 
            + ", Skipped: " + skippedTests);
    }

    private static void printTestSummary() {
        System.out.println("\n=== TEST SUMMARY ===");
        System.out.println("✅ Passed:  " + (totalTests - failedTests - errorTests - skippedTests));
        System.out.println("❌ Failed:  " + failedTests);
        System.out.println("⚠️ Errors:  " + errorTests);
        System.out.println("⏩ Skipped: " + skippedTests);
        System.out.println("Total:     " + totalTests + " tests");
        
        if ((failedTests + errorTests) > 0) {
            System.out.println("\n❌ Some tests failed or had errors");
        }
    }

    private static List<String> buildCommand(BuildSystem buildSystem) {
        List<String> command = new ArrayList<>();
        if (buildSystem == BuildSystem.MAVEN) {
            command.add(isWindows() ? "mvn.cmd" : "mvn");
            command.add("clean");
            command.add("test");
        } else if (buildSystem == BuildSystem.GRADLE) {
            command.add(isWindows() ? "gradlew.bat" : "./gradlew");
            command.add("test");
        } else {
            throw new IllegalStateException("Unsupported build system");
        }
        return command;
    }

    private static BuildSystem determineBuildSystem(File projectDir) {
        if (new File(projectDir, "pom.xml").exists()) return BuildSystem.MAVEN;
        if (new File(projectDir, "build.gradle").exists() || 
            new File(projectDir, "build.gradle.kts").exists()) return BuildSystem.GRADLE;
        return BuildSystem.UNKNOWN;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private enum BuildSystem {
        MAVEN, GRADLE, UNKNOWN
    }
}