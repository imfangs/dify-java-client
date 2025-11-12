# Dify Java Client

[![Maven Central](https://img.shields.io/maven-central/v/io.github.ferrymo/dify-java-client.svg)](https://search.maven.org/search?q=g:io.github.ferrymo%20AND%20a:dify-java-client)
[![License](https://img.shields.io/github/license/imfangs/dify-java-client)](https://github.com/imfangs/dify-java-client/blob/main/LICENSE)
[![Java](https://img.shields.io/badge/Java-8%2B-blue)](https://www.java.com)

English | [简体中文](README.md) | [日本語](README_JP.md)

Dify Java Client is a Java client library for interacting with the [Dify](https://dify.ai) platform. It provides complete support for Dify Application APIs and Knowledge Base APIs, enabling Java developers to easily integrate Dify's generative AI capabilities into their applications.

## Features

Dify Java Client provides the following core features:

### 1. Multiple Application Types Support

- **Chat Applications**: Interact with conversational applications via `DifyChatClient`, supporting conversation management, message feedback, and more
- **Completion Applications**: Call text generation applications via `DifyCompletionClient`
- **Chatflow Applications**: Call workflow-orchestrated conversational applications via `DifyChatflowClient`
- **Workflow Applications**: Call workflow applications via `DifyWorkflowClient`
- **Knowledge Base Management**: Manage knowledge bases, documents, and retrieval via `DifyDatasetsClient`

### 2. Rich Interaction Modes

- **Blocking Mode**: Synchronous API calls, waiting for complete responses
- **Streaming Mode**: Receive real-time generated content through callbacks, supporting typewriter effects
- **File Processing**: Support for file uploads, speech-to-text, text-to-speech, and other multimedia functions

### 3. Complete Conversation Management

- Create and manage conversations
- Retrieve message history
- Rename conversations
- Message feedback (like/dislike)
- Get suggested questions

### 4. Full Knowledge Base Support

- Create and manage knowledge bases
- Upload and manage documents
- Document segment management
- Semantic retrieval

### 5. Flexible Configuration Options

- Custom connection timeout
- Custom read/write timeout
- Custom HTTP client

## Installation

### System Requirements

- Java 8 or higher
- Maven 3.x or Gradle 4.x+

### Maven

```xml
<dependency>
    <groupId>io.github.ferrymo</groupId>
    <artifactId>dify-java-client</artifactId>
    <version>1.2.1</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.ferrymo:dify-java-client:1.2.1'
```

## Quick Start

### Creating Clients

```java
// Create a complete Dify client
DifyClient client = DifyClientFactory.createClient("https://api.dify.ai/v1", "your-api-key");

// Create specific types of clients
DifyChatClient chatClient = DifyClientFactory.createChatClient("https://api.dify.ai/v1", "your-api-key");
DifyCompletionClient completionClient = DifyClientFactory.createCompletionClient("https://api.dify.ai/v1", "your-api-key");
DifyChatflowClient chatflowClient = DifyClientFactory.createChatWorkflowClient("https://api.dify.ai/v1", "your-api-key");
DifyWorkflowClient workflowClient = DifyClientFactory.createWorkflowClient("https://api.dify.ai/v1", "your-api-key");
DifyDatasetsClient datasetsClient = DifyClientFactory.createDatasetsClient("https://api.dify.ai/v1", "your-api-key");

// Create client with custom configuration
DifyConfig config = DifyConfig.builder()
    .baseUrl("https://api.dify.ai/v1")
    .apiKey("your-api-key")
    .connectTimeout(5000)
    .readTimeout(60000)
    .writeTimeout(30000)
    .build();

DifyClient clientWithConfig = DifyClientFactory.createClient(config);
```

## Usage Examples

### 1. Chat Applications

#### Blocking Mode

```java
// Create chat client
DifyChatClient chatClient = DifyClientFactory.createChatClient("https://api.dify.ai/v1", "your-api-key");

// Create chat message
ChatMessage message = ChatMessage.builder()
    .query("Hello, please introduce yourself")
    .user("user-123")
    .responseMode(ResponseMode.BLOCKING)
    .build();

// Send message and get response
ChatMessageResponse response = chatClient.sendChatMessage(message);
System.out.println("Reply: " + response.getAnswer());
System.out.println("Conversation ID: " + response.getConversationId());
System.out.println("Message ID: " + response.getMessageId());
```

#### Streaming Mode

```java
// Create chat message
ChatMessage message = ChatMessage.builder()
    .query("Please tell me a short story")
    .user("user-123")
    .responseMode(ResponseMode.STREAMING)
    .build();

// Send streaming message
chatClient.sendChatMessageStream(message, new ChatStreamCallback() {
    @Override
    public void onMessage(MessageEvent event) {
        System.out.println("Received message chunk: " + event.getAnswer());
    }

    @Override
    public void onMessageEnd(MessageEndEvent event) {
        System.out.println("Message ended, complete message ID: " + event.getMessageId());
    }

    @Override
    public void onError(ErrorEvent event) {
        System.err.println("Error: " + event.getMessage());
    }

    @Override
    public void onException(Throwable throwable) {
        System.err.println("Exception: " + throwable.getMessage());
    }
});
```

#### Conversation Management

```java
// Get conversation history messages
MessageListResponse messages = chatClient.getMessages(conversationId, "user-123", null, 10);

// Get conversation list
ConversationListResponse conversations = chatClient.getConversations("user-123", null, 10, "-updated_at");

// Rename conversation
Conversation renamedConversation = chatClient.renameConversation(conversationId, "New Conversation Name", false, "user-123");

// Delete conversation
SimpleResponse deleteResponse = chatClient.deleteConversation(conversationId, "user-123");
```

#### Message Feedback

```java
// Send message feedback (like)
SimpleResponse feedbackResponse = chatClient.feedbackMessage(messageId, "like", "user-123", "This is a great answer");

// Get suggested questions
SuggestedQuestionsResponse suggestedQuestions = chatClient.getSuggestedQuestions(messageId, "user-123");
```

#### Voice Conversion

```java
// Speech to text
AudioToTextResponse textResponse = chatClient.audioToText(audioFile, "user-123");
System.out.println("Converted text: " + textResponse.getText());

// Text to speech
byte[] audioData = chatClient.textToAudio(null, "This is a test text", "user-123");
```

### 2. Completion Applications

#### Blocking Mode

```java
// Create completion client
DifyCompletionClient completionClient = DifyClientFactory.createCompletionClient("https://api.dify.ai/v1", "your-api-key");

// Create request
Map<String, Object> inputs = new HashMap<>();
inputs.put("content", "eggplant");

CompletionRequest request = CompletionRequest.builder()
    .inputs(inputs)
    .responseMode(ResponseMode.BLOCKING)
    .user("user-123")
    .build();

// Send request and get response
CompletionResponse response = completionClient.sendCompletionMessage(request);
System.out.println("Generated text: " + response.getAnswer());
```

#### Streaming Mode

```java
// Create request
Map<String, Object> inputs = new HashMap<>();
inputs.put("content", "eggplant");

CompletionRequest request = CompletionRequest.builder()
    .inputs(inputs)
    .responseMode(ResponseMode.STREAMING)
    .user("user-123")
    .build();

// Send streaming request
completionClient.sendCompletionMessageStream(request, new CompletionStreamCallback() {
    @Override
    public void onMessage(MessageEvent event) {
        System.out.println("Received message chunk: " + event.getAnswer());
    }

    @Override
    public void onMessageEnd(MessageEndEvent event) {
        System.out.println("Message ended, complete message ID: " + event.getMessageId());
    }

    @Override
    public void onError(ErrorEvent event) {
        System.err.println("Error: " + event.getMessage());
    }

    @Override
    public void onException(Throwable throwable) {
        System.err.println("Exception: " + throwable.getMessage());
    }
});
```

#### Stop Generation

```java
// Stop text generation
SimpleResponse stopResponse = completionClient.stopCompletion(taskId, "user-123");
```

### 3. Workflow Applications

#### Blocking Mode

```java
// Create workflow client
DifyWorkflowClient workflowClient = DifyClientFactory.createWorkflowClient("https://api.dify.ai/v1", "your-api-key");

// Create workflow request
Map<String, Object> inputs = new HashMap<>();
inputs.put("content", "Please introduce AI application scenarios");

WorkflowRunRequest request = WorkflowRunRequest.builder()
    .inputs(inputs)
    .responseMode(ResponseMode.BLOCKING)
    .user("user-123")
    .build();

// Run workflow and get response
WorkflowRunResponse response = workflowClient.runWorkflow(request);
System.out.println("Workflow execution ID: " + response.getTaskId());

// Output results
if (response.getData() != null) {
    for (Map.Entry<String, Object> entry : response.getData().getOutputs().entrySet()) {
        System.out.println(entry.getKey() + ": " + entry.getValue());
    }
}
```

#### Streaming Mode

```java
// Create workflow request
Map<String, Object> inputs = new HashMap<>();
inputs.put("content", "Please explain the basic principles of machine learning in detail");

WorkflowRunRequest request = WorkflowRunRequest.builder()
    .inputs(inputs)
    .responseMode(ResponseMode.STREAMING)
    .user("user-123")
    .build();

// Run workflow streaming request
workflowClient.runWorkflowStream(request, new WorkflowStreamCallback() {
    @Override
    public void onWorkflowStarted(WorkflowStartedEvent event) {
        System.out.println("Workflow started: " + event);
    }

    @Override
    public void onNodeStarted(NodeStartedEvent event) {
        System.out.println("Node started: " + event);
    }

    @Override
    public void onNodeFinished(NodeFinishedEvent event) {
        System.out.println("Node finished: " + event);
    }

    @Override
    public void onWorkflowFinished(WorkflowFinishedEvent event) {
        System.out.println("Workflow finished: " + event);
    }

    @Override
    public void onError(ErrorEvent event) {
        System.err.println("Error: " + event.getMessage());
    }

    @Override
    public void onException(Throwable throwable) {
        System.err.println("Exception: " + throwable.getMessage());
    }
});
```

#### Workflow Management

```java
// Stop workflow
WorkflowStopResponse stopResponse = workflowClient.stopWorkflow(taskId, "user-123");

// Get workflow execution status
WorkflowRunStatusResponse statusResponse = workflowClient.getWorkflowRun(workflowRunId);

// Get workflow logs
WorkflowLogsResponse logsResponse = workflowClient.getWorkflowLogs(null, null, 1, 10);
```

### 4. Knowledge Base Management

#### Create and Manage Knowledge Bases

```java
// Create datasets client
DifyDatasetsClient datasetsClient = DifyClientFactory.createDatasetsClient("https://api.dify.ai/v1", "your-api-key");

// Create knowledge base
CreateDatasetRequest createRequest = CreateDatasetRequest.builder()
    .name("Test Knowledge Base-" + System.currentTimeMillis())
    .description("This is a test knowledge base")
    .indexingTechnique("high_quality")
    .permission("only_me")
    .provider("vendor")
    .build();

DatasetResponse dataset = datasetsClient.createDataset(createRequest);
System.out.println("Created knowledge base ID: " + dataset.getId());

// Get knowledge base list
DatasetListResponse datasetList = datasetsClient.getDatasets(1, 10);
System.out.println("Total knowledge bases: " + datasetList.getTotal());
```

#### Document Management

```java
// Create document by text - Using automatic mode (recommended)
CreateDocumentByTextRequest docRequest = CreateDocumentByTextRequest.builder()
    .name("Test Document-" + System.currentTimeMillis())
    .text("This is the content of a test document.\nThis is the second line.\nThis is the third line.")
    .indexingTechnique("high_quality")
    .docForm("text_model")
    .docLanguage("English")
    .processRule(ProcessRule.builder()
        .mode("automatic")  // Use automatic processing mode
        .build())
    .build();

DocumentResponse docResponse = datasetsClient.createDocumentByText(datasetId, docRequest);
System.out.println("Created document ID: " + docResponse.getDocument().getId());

// Get document list
DocumentListResponse docList = datasetsClient.getDocuments(datasetId, null, 1, 10);
System.out.println("Total documents: " + docList.getTotal());

// Delete document
SimpleResponse deleteResponse = datasetsClient.deleteDocument(datasetId, documentId);
```

#### Knowledge Base Retrieval

```java
// Create retrieval request
RetrievalModel retrievalModel = new RetrievalModel();
retrievalModel.setTopK(3);
retrievalModel.setScoreThreshold(0.5f);

RetrieveRequest retrieveRequest = RetrieveRequest.builder()
    .query("What is artificial intelligence")
    .retrievalModel(retrievalModel)
    .build();

// Send retrieval request
RetrieveResponse retrieveResponse = datasetsClient.retrieveDataset(datasetId, retrieveRequest);

// Process retrieval results
System.out.println("Retrieval query: " + retrieveResponse.getQuery().getContent());
System.out.println("Number of retrieval results: " + retrieveResponse.getRecords().size());
retrieveResponse.getRecords().forEach(record -> {
    System.out.println("Score: " + record.getScore());
    System.out.println("Content: " + record.getSegment().getContent());
});
```

## API Reference

### Client Types

| Client Type | Description | Main Features |
|-------------|-------------|---------------|
| `DifyClient` | Complete client | Supports all API features |
| `DifyChatClient` | Chat application client | Conversations, conversation management, message feedback |
| `DifyCompletionClient` | Text generation application client | Text generation, stop generation |
| `DifyChatflowClient` | Workflow-orchestrated chat application client | Workflow-orchestrated conversations |
| `DifyWorkflowClient` | Workflow application client | Execute workflows, workflow management |
| `DifyDatasetsClient` | Knowledge base client | Knowledge base management, document management, retrieval |

### Response Modes

| Mode | Enum Value | Description |
|------|------------|-------------|
| Blocking Mode | `ResponseMode.BLOCKING` | Synchronous call, waiting for complete response |
| Streaming Mode | `ResponseMode.STREAMING` | Receive real-time generated content through callbacks |

### Event Types

| Event Type | Description |
|------------|-------------|
| `MessageEvent` | Message event, containing generated text chunk |
| `MessageEndEvent` | Message end event, containing complete message ID |
| `MessageFileEvent` | File message event, containing file information |
| `TtsMessageEvent` | Text-to-speech event |
| `TtsMessageEndEvent` | Text-to-speech end event |
| `MessageReplaceEvent` | Message replacement event |
| `AgentMessageEvent` | Agent message event |
| `AgentThoughtEvent` | Agent thought event |
| `WorkflowStartedEvent` | Workflow started event |
| `NodeStartedEvent` | Node started event |
| `NodeFinishedEvent` | Node finished event |
| `WorkflowFinishedEvent` | Workflow finished event |
| `ErrorEvent` | Error event |
| `PingEvent` | Ping event |

## Advanced Configuration

### Custom HTTP Client

```java
// Create custom configuration
DifyConfig config = DifyConfig.builder()
    .baseUrl("https://api.dify.ai/v1")
    .apiKey("your-api-key")
    .connectTimeout(5000)  // Connection timeout (milliseconds)
    .readTimeout(60000)    // Read timeout (milliseconds)
    .writeTimeout(30000)   // Write timeout (milliseconds)
    .build();

// Create client with custom configuration
DifyClient client = DifyClientFactory.createClient(config);
```

## More Documentation

- [Chat Application API Examples](src/test/java/io/github/imfangs/dify/client/DifyChatClientTest.java)
    - Message sending (blocking/streaming)
    - Conversation management
    - Message feedback
    - Voice conversion
    - Suggested questions

- [Completion Application API Examples](src/test/java/io/github/imfangs/dify/client/DifyCompletionClientTest.java)
    - Text generation (blocking/streaming)
    - Stop generation
    - File processing
    - Text to speech

- [Workflow Application API Examples](src/test/java/io/github/imfangs/dify/client/DifyWorkflowClientTest.java)
    - Workflow execution (blocking/streaming)
    - Stop workflow
    - Workflow status
    - Workflow logs

- [Knowledge Base API Examples](src/test/java/io/github/imfangs/dify/client/DifyDatasetsClientTest.java)
    - Knowledge base management
    - Document management
    - Semantic retrieval

- [Events and Callbacks Examples](src/test/java/io/github/imfangs/dify/client/DifyChatflowClientTest.java)
    - Message events
    - File events
    - TTS events
    - Workflow events
    - Error handling

## Contributing

Contributions of code, issue reports, or improvement suggestions are welcome. Please participate in project development through GitHub Issues or Pull Requests.

## License

This project is licensed under the [Apache License 2.0](LICENSE).

## Related Links

- [Dify Website](https://dify.ai)
- [Dify Documentation](https://docs.dify.ai)
- [Dify GitHub](https://github.com/langgenius/dify)

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=imfangs/dify-java-client&type=Date)](https://www.star-history.com/#imfangs/dify-java-client&Date)
