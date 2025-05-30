# AI Java Debugging Assistant

A command-line tool that leverages AI models (OpenAI, Claude, DeepSeek) to detect and fix bugs in Java source code.

## Features

- Detects bugs in Java source files using AI models
- Suggests code fixes for identified bugs
- Optionally applies fixes automatically
- Runs tests before and after applying changes
- Backs up original files
- Logs results for future reference

## Setup

### Prerequisites

- Java 17 or higher
- Maven
- API keys for the AI models you wish to use

### Building the Project

```bash
cd assistant
mvn clean package
```

This will create a JAR file with all dependencies in the `target` directory.

### Setting API Keys

There are two ways to provide API keys:

#### 1. Properties File (Recommended)

1. Go to `src/main/resources/`
2. Copy `api-keys.properties.template` to `api-keys.properties` (remove the `.template` extension)
3. Edit `api-keys.properties` and fill in your API keys:

```properties
OPENAI_API_KEY=your_openai_api_key
CLAUDE_API_KEY=your_claude_api_key
DEEPSEEK_API_KEY=your_deepseek_api_key
```

The `api-keys.properties` file is included in `.gitignore` so your keys won't be pushed to version control.

#### 2. Environment Variables

Alternatively, you can export your AI model API keys as environment variables:

```bash
# For OpenAI
export OPENAI_API_KEY=your_openai_api_key

# For Claude
export CLAUDE_API_KEY=your_claude_api_key

# For DeepSeek
export DEEPSEEK_API_KEY=your_deepseek_api_key
```

The tool will first check the properties file, then fall back to environment variables if needed.

## Usage

Run the assistant with the following command:

```bash
java -jar target/assistant-1.0-SNAPSHOT-jar-with-dependencies.jar -m OpenAI -f path/to/BuggyClass.java -c command
```

### Command Line Options

- `-m` or `--model`: AI model to use (OpenAI, Claude, DeepSeek)
- `-f` or `--file`: Path to the buggy Java file
- `-c` or `--command`: Command to run (see below)

### Available Commands

- `kor-test`: Run tests on the Java file to check if there are failing tests
- `hitta-buggar`: Find bugs in the Java file and display detailed information about them with proposed fixes
- `fixa-kod`: Find bugs, automatically apply fixes, and run tests to verify the changes

### Examples

```bash
# Run tests on a Java file
java -jar target/assistant-1.0-SNAPSHOT-jar-with-dependencies.jar -m OpenAI -f path/to/BuggyClass.java -c kor-test

# Find bugs in a Java file
java -jar target/assistant-1.0-SNAPSHOT-jar-with-dependencies.jar -m OpenAI -f path/to/BuggyClass.java -c hitta-buggar

# Fix bugs in a Java file
java -jar target/assistant-1.0-SNAPSHOT-jar-with-dependencies.jar -m OpenAI -f path/to/BuggyClass.java -c fixa-kod
```

## Workflow

### When using `hitta-buggar`:
1. The tool reads the content of the specified Java file
2. It sends the file content to the AI model
3. The AI analyzes the code and identifies bugs
4. The tool displays the bug locations, details, and suggested fixes
5. You can choose whether to apply the fixes

### When using `fixa-kod`:
1. The tool runs tests to check the current state of the code
2. It identifies bugs using AI
3. It automatically applies the suggested fixes
4. It runs tests again to verify that the fixes resolved the issues
5. Results are logged to a file for future reference

## Notes

- The tool provides output in Swedish
- Bug details include line numbers, type of bug, and explanations
- Proposed changes show side-by-side comparisons of the original and fixed code
- Results are logged to debug.log 

## Video Demonstration

Below is a video demonstrating how the tool works:

![Video Demo](assets/AI%20assistant%20interaction.mp4)

