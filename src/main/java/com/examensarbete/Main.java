package com.examensarbete;

import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "assistant", version = "assistant 1.0", mixinStandardHelpOptions = true)
public class Main implements Callable<Integer> {

    @CommandLine.Option(names = {"-m", "--model"}, description = "AI model to use (OpenAI, Claude, DeepSeek)", required = true)
    private String model;

    @CommandLine.Option(names = {"-f", "--file"}, description = "Path to the Java file to analyze", required = true)
    private File file;

    @CommandLine.Option(names = {"-c", "--command"}, description = "Command to execute (hitta-buggar, kor-test, fixa-kod)", required = true)
    private String command;

    private final FileReader fileReader = new FileReader();
    private final PromptBuilder promptBuilder = new PromptBuilder();
    private final CodePatcher codePatcher = new CodePatcher();
    // private final TestRunner testRunner = new TestRunner(); // Ingen instans behövs nu
    private final ResultLogger resultLogger = new ResultLogger();

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        String code = fileReader.readFile(file.getPath());

        switch (command.toLowerCase()) {
            case "hitta-buggar":
                findBugs(code);
                break;
            case "kor-test":
                System.out.println("Testkörning har inaktiverats.");
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

    private void runTests() {
        // Testkörning är inaktiverad
        System.out.println("Testkörning har inaktiverats.");
    }

    private void fixCode(String code) throws Exception {
        // runTests(); // Anrop till testkörning borttaget

        String bugFindingPrompt = promptBuilder.buildBugFindingPrompt(code);
        String bugFindingResponse = AIClient.sendRequest(model, bugFindingPrompt);
        AIClient.AIResponse bugFixResponse = AIClient.parseResponse(bugFindingResponse);

        if (bugFixResponse != null) {
            CodePatcher.applyPatch(file.getPath(), bugFixResponse.getCorrectedCode(), bugFixResponse.getBugPosition(), true);
            System.out.println("Korrigerad kod har applicerats på: " + file.getAbsolutePath());
            // runTests(); // Anrop till testkörning borttaget
        } else {
            System.out.println("Ingen bugg hittades, så ingen korrigering har applicerats.");
        }
    }
}