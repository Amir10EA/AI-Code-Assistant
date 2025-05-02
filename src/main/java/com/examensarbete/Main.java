package com.examensarbete;

import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "assistant", version = "assistant 1.0", mixinStandardHelpOptions = true)
public class Main implements Callable<Integer> {

    @CommandLine.Option(
    names = {"-m", "--model"}, 
    description = "AI model to use (OpenAI, Claude, DeepSeek)", 
    required = false // Changed from required=true
)
    private String model;

    @CommandLine.Option(names = {"-f", "--file"}, description = "Path to the Java file to analyze", required = true)
    private File file;

    @CommandLine.Option(names = {"-c", "--command"}, description = "Command to execute (hitta-buggar, kor-test, fixa-kod)", required = true)
    private String command;

    private final FileReader fileReader = new FileReader();
    private final PromptBuilder promptBuilder = new PromptBuilder();
    private final CodePatcher codePatcher = new CodePatcher();
    private final ResultLogger resultLogger = new ResultLogger();

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    private String getProjectRoot() throws Exception {
        Path filePath = file.toPath().toAbsolutePath();
        Path currentDir = filePath.getParent();
        
        while (currentDir != null) {
            if (Files.exists(currentDir.resolve("pom.xml")) || 
                Files.exists(currentDir.resolve("build.gradle"))) {
                return currentDir.toString();
            }
            currentDir = currentDir.getParent();
        }
        throw new RuntimeException("Project root (Maven/Gradle) not found for file: " + file);
    }

    @Override
public Integer call() throws Exception {
    // Add validation for commands that need model
    if (command.equalsIgnoreCase("hitta-buggar") || 
        command.equalsIgnoreCase("fixa-kod")) {
        if (model == null) {
            System.out.println("Error: Model required for this command");
            return 1;
        }
    }
        String code = fileReader.readFile(file.getPath());

        switch (command.toLowerCase()) {
            case "hitta-buggar":
                findBugs(code);
                break;
            case "kor-test":
                runTests();
                break;
            case "fixa-kod":
                fixCode(code);
                break;
            default:
                System.out.println("Ogiltigt kommando: " + command);
                return 1;
        }
        return 0;
    }

    private void findBugs(String code) throws Exception {
        String prompt = promptBuilder.buildBugFindingPrompt(code);
        String response = AIClient.sendRequest(model, prompt);
        AIClient.AIResponse parsedResponse = AIClient.parseResponse(response);

        if (parsedResponse != null) {
            System.out.println("Buggposition: " + parsedResponse.getBugPosition());
            System.out.println("Föreslagen korrigering:\n" + parsedResponse.getCorrectedCode());

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Vill du applicera denna ändring? (y/N): ");
            String answer = reader.readLine();

            if (answer.trim().equalsIgnoreCase("y")) {
                CodePatcher.applyPatch(file.getPath(), parsedResponse.getCorrectedCode(), parsedResponse.getBugPosition(), true);
                System.out.println("Ändringen har applicerats.");
            } else {
                System.out.println("Ändringen har inte applicerats.");
            }
        } else {
            System.out.println("Ingen bugg hittades eller kunde inte extrahera bugginformation.");
        }
    }

    private void runTests() throws Exception {
        String projectPath = getProjectRoot();
        try {
            boolean testsPassed = TestRunner.runTests(projectPath);
            System.out.println("Testresultat: " + (testsPassed ? "ALLT GRÖNT ✅" : "MISSLYCKADES ❌"));
        } catch (RuntimeException e) {
            System.err.println("[ERROR] Test execution failed: " + e.getMessage());
            System.out.println("Testresultat: MISSLYCKADES ❌");
        }
    }

    private void fixCode(String code) throws Exception {
        String projectPath = getProjectRoot();
        
        // Run initial tests
        System.out.println("\n=== KÖR TESTER INNAN KORRIGERING ===");
        boolean initialTestsPassed = TestRunner.runTests(projectPath);
        
        // Find and apply fix
        String bugFindingPrompt = promptBuilder.buildBugFindingPrompt(code);
        String bugFindingResponse = AIClient.sendRequest(model, bugFindingPrompt);
        AIClient.AIResponse bugFixResponse = AIClient.parseResponse(bugFindingResponse);

        boolean fixApplied = false;
        if (bugFixResponse != null) {
            CodePatcher.applyPatch(file.getPath(), 
                bugFixResponse.getCorrectedCode(), 
                bugFixResponse.getBugPosition(), 
                true
            );
            fixApplied = true;
            
            // Run post-fix tests
            System.out.println("\n=== KÖR TESTER EFTER KORRIGERING ===");
            boolean finalTestsPassed = TestRunner.runTests(projectPath);
            
            // Log results
            resultLogger.logResult(
                "debug.log",
                bugFixResponse.getBugPosition(),
                bugFixResponse.getCorrectedCode(),
                fixApplied,
                initialTestsPassed,
                finalTestsPassed
            );
            
            System.out.println("\nSAMMANFATTNING:");
            System.out.println("Initiala tester: " + (initialTestsPassed ? "Lyckades" : "Misslyckades"));
            System.out.println("Tester efter fix: " + (finalTestsPassed ? "Lyckades" : "Misslyckades"));
        } else {
            System.out.println("Ingen bugg hittades - ingen åtgärd vidtogs.");
        }
    }
}