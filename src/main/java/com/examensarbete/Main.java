package com.examensarbete;

import picocli.CommandLine;
import java.io.File;
import java.util.Scanner;
import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "assistant",
    description = "AI Assistant for debugging Java code",
    mixinStandardHelpOptions = true
)
public class Main implements Callable<Integer> {
    @CommandLine.Option(names = {"--model", "-m"}, required = true, description = "AI model to use (e.g., OpenAI, Claude, DeepSeek)")
    private String model;

    @CommandLine.Option(names = {"--file", "-f"}, required = true, description = "Path to the buggy Java file")
    private String filePath;

    @CommandLine.Option(names = {"--prompt", "-p"}, required = true, description = "User-defined prompt")
    private String prompt;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        try {
            // Step 1: Read file
            String content = FileReader.readFile(filePath);

            // Step 2: Build prompt
            String aiPrompt = PromptBuilder.buildPrompt(content, prompt);

            // Step 3: Query AI
            String aiResponse = AIClient.sendRequest(model, aiPrompt);
            AIClient.AIResponse parsedResponse = AIClient.parseResponse(aiResponse);

            // Step 4: Show bug and fix, ask for confirmation
            System.out.println("\nBug Position: " + parsedResponse.getBugPosition());
            System.out.println("\nCorrected Code:\n" + parsedResponse.getCorrectedCode());
            
            Scanner scanner = new Scanner(System.in);
            System.out.print("\nApply this fix? (y/n): ");
            boolean apply = scanner.nextLine().trim().equalsIgnoreCase("y");

            // Step 5: Apply patch if confirmed
            CodePatcher.applyPatch(filePath, parsedResponse.getCorrectedCode(), parsedResponse.getBugPosition(), apply);

            // Step 6: Run tests and log results if fix was applied
            boolean testsPassed = false;
            if (apply) {
                System.out.println("\nRunning tests...");
                testsPassed = TestRunner.runTests(new File(filePath).getParent());
                System.out.println("\nTests " + (testsPassed ? "PASSED" : "FAILED"));
            }
            
            // Step 7: Log results
            ResultLogger.logResult("debug_log.txt", parsedResponse.getBugPosition(), 
                    parsedResponse.getCorrectedCode(), apply, testsPassed);
            
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
}