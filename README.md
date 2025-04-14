# AI Java Debugging Assistant

A command-line tool that leverages AI models (OpenAI, Claude, DeepSeek) to detect and fix bugs in Java source code.

## Features

- Detects bugs in Java source files using AI models
- Suggests code fixes for identified bugs
- Asks for confirmation before applying changes
- Backs up original files
- Validates fixes by running JUnit tests
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
2. Copy `api-keys.properties.template` to `api-keys.properties`
3. Edit `api-keys.properties` and fill in your API keys:

```properties
OPENAI_API_KEY=your_openai_api_key_here
CLAUDE_API_KEY=your_claude_api_key_here
DEEPSEEK_API_KEY=your_deepseek_api_key_here
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
java -jar target/assistant-1.0-SNAPSHOT-jar-with-dependencies.jar --model OpenAI --file path/to/BuggyClass.java --prompt "Fix the bugs in this file"
```

### Command Line Options

- `--model` or `-m`: AI model to use (OpenAI, Claude, DeepSeek)
- `--file` or `-f`: Path to the buggy Java file
- `--prompt` or `-p`: User-defined prompt for the AI

### Example

```bash
java -jar target/assistant-1.0-SNAPSHOT-jar-with-dependencies.jar --model OpenAI --file src/main/java/com/example/BuggyClass.java --prompt "Find and fix any logical errors in this code"
```

## Workflow

1. The tool reads the content of the specified Java file
2. It sends the file content along with your prompt to the AI model
3. The AI analyzes the code and identifies bugs
4. The tool displays the bug locations and suggested fixes
5. You can choose whether to apply the fixes
6. If applied, the tool runs JUnit tests to validate the changes
7. Results are logged to a file for future reference

## Notes

- The original file is backed up before applying any changes
- The tool defaults to using Maven for running tests but also supports Gradle
- If no tests are found, the fix is considered successful 