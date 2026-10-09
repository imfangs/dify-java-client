# Dify Java Client

[![Maven Central](https://img.shields.io/maven-central/v/io.github.imfangs/dify-java-client.svg)](https://search.maven.org/search?q=g:io.github.imfangs%20AND%20a:dify-java-client)
[![License](https://img.shields.io/github/license/imfangs/dify-java-client)](https://github.com/imfangs/dify-java-client/blob/main/LICENSE)
[![Java](https://img.shields.io/badge/Java-8%2B-blue)](https://www.java.com)

[English](README_EN.md) | [简体中文](README.md) | 日本語

**1.7.0** ではストリーム完了とデータソースのコールバックを追加し、Chatflow の終了イベント処理を修正しました。

Dify Java Clientは、[Dify](https://dify.ai)プラットフォームと連携するためのJavaクライアントライブラリです。DifyアプリケーションAPIとナレッジベースAPIの完全なサポートを提供し、Java開発者がDifyの生成AIの機能を自らのアプリケーションに簡単に統合できるようにします。

## 機能

Dify Java Clientは以下の主要機能を提供します：

### 1. 複数のアプリケーションタイプのサポート

- **チャットアプリケーション**: `DifyChatClient`を通じて会話型アプリケーションを利用し、会話管理、メッセージフィードバックなどの機能をサポート
- **テキスト生成アプリケーション**: `DifyCompletionClient`を通じてテキスト生成アプリケーションを利用
- **ワークフロー制御会話**: `DifyChatflowClient`を通じてワークフロー制御の会話型アプリケーションを利用
- **ワークフローアプリケーション**: `DifyWorkflowClient`を通じてワークフローアプリケーションを利用
- **ナレッジベース管理**: `DifyDatasetsClient`を通じてナレッジベース、ドキュメント、検索を管理

### 2. 豊富なインタラクションモード

- **ブロッキングモード**: 同期的にAPIを呼び出し、完全な応答を待機
- **ストリーミングモード**: コールバックを通じてリアルタイムで生成されたコンテンツを受信し、タイプライター効果をサポート
- **ファイル処理**: ファイルのアップロード、音声からテキスト、テキストから音声などのマルチメディア機能をサポート

### 3. 完全な会話管理

- 会話の作成と管理
- 履歴メッセージの取得
- 会話の名前変更
- メッセージフィードバック（いいね/悪いね）
- 提案質問の取得

### 4. ナレッジベースの完全サポート

- ナレッジベースの作成と管理
- ドキュメントのアップロードと管理
- ドキュメントセグメント管理
- セマンティック検索

### 5. 柔軟な設定オプション

- カスタム接続タイムアウト
- カスタム読み取り/書き込みタイムアウト
- カスタムHTTPクライアント

## インストール

### システム要件

- Java 8以上
- Maven 3.x または Gradle 4.x以上

### Maven

```xml
<dependency>
    <groupId>io.github.imfangs</groupId>
    <artifactId>dify-java-client</artifactId>
    <version>1.7.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.imfangs:dify-java-client:1.7.0'
```

## クイックスタート

### クライアントの作成

```java
// 完全なDifyクライアントの作成
DifyClient client = DifyClientFactory.createClient("https://api.dify.ai/v1", "your-api-key");

// 特定タイプのクライアントの作成
DifyChatClient chatClient = DifyClientFactory.createChatClient("https://api.dify.ai/v1", "your-api-key");
DifyCompletionClient completionClient = DifyClientFactory.createCompletionClient("https://api.dify.ai/v1", "your-api-key");
DifyChatflowClient chatflowClient = DifyClientFactory.createChatWorkflowClient("https://api.dify.ai/v1", "your-api-key");
DifyWorkflowClient workflowClient = DifyClientFactory.createWorkflowClient("https://api.dify.ai/v1", "your-api-key");
DifyDatasetsClient datasetsClient = DifyClientFactory.createDatasetsClient("https://api.dify.ai/v1", "your-api-key");

// カスタム設定でクライアントを作成
DifyConfig config = DifyConfig.builder()
    .baseUrl("https://api.dify.ai/v1")
    .apiKey("your-api-key")
    .connectTimeout(5000)
    .readTimeout(60000)
    .writeTimeout(30000)
    .build();

DifyClient clientWithConfig = DifyClientFactory.createClient(config);
```

## 使用例

### 1. チャットアプリケーション

#### ブロッキングモード

```java
// チャットクライアントの作成
DifyChatClient chatClient = DifyClientFactory.createChatClient("https://api.dify.ai/v1", "your-api-key");

// チャットメッセージの作成
ChatMessage message = ChatMessage.builder()
    .query("こんにちは、自己紹介をお願いします")
    .user("user-123")
    .responseMode(ResponseMode.BLOCKING)
    .build();

// メッセージを送信して応答を取得
ChatMessageResponse response = chatClient.sendChatMessage(message);
System.out.println("返信: " + response.getAnswer());
System.out.println("会話ID: " + response.getConversationId());
System.out.println("メッセージID: " + response.getMessageId());
```

#### ストリーミングモード

```java
// チャットメッセージの作成
ChatMessage message = ChatMessage.builder()
    .query("短い物語を教えてください")
    .user("user-123")
    .responseMode(ResponseMode.STREAMING)
    .build();

// ストリーミングメッセージの送信
chatClient.sendChatMessageStream(message, new ChatStreamCallback() {
    @Override
    public void onMessage(MessageEvent event) {
        System.out.println("メッセージチャンク受信: " + event.getAnswer());
    }

    @Override
    public void onMessageEnd(MessageEndEvent event) {
        System.out.println("メッセージ終了、完全なメッセージID: " + event.getMessageId());
    }

    @Override
    public void onError(ErrorEvent event) {
        System.err.println("エラー: " + event.getMessage());
    }

    @Override
    public void onException(Throwable throwable) {
        System.err.println("例外: " + throwable.getMessage());
    }
});
```

#### 会話管理

```java
// 会話履歴メッセージの取得
MessageListResponse messages = chatClient.getMessages(conversationId, "user-123", null, 10);

// 会話リストの取得
ConversationListResponse conversations = chatClient.getConversations("user-123", null, 10, "-updated_at");

// 会話の名前変更
Conversation renamedConversation = chatClient.renameConversation(conversationId, "新しい会話名", false, "user-123");

// 会話の削除
SimpleResponse deleteResponse = chatClient.deleteConversation(conversationId, "user-123");
```

#### メッセージフィードバック

```java
// メッセージフィードバックの送信（いいね）
SimpleResponse feedbackResponse = chatClient.feedbackMessage(messageId, "like", "user-123", "これは良い回答です");

// 提案質問の取得
SuggestedQuestionsResponse suggestedQuestions = chatClient.getSuggestedQuestions(messageId, "user-123");
```

#### 音声変換

```java
// 音声からテキスト
AudioToTextResponse textResponse = chatClient.audioToText(audioFile, "user-123");
System.out.println("変換されたテキスト: " + textResponse.getText());

// テキストから音声
byte[] audioData = chatClient.textToAudio(null, "これはテストテキストです", "user-123");
```

### 2. テキスト生成アプリケーション

#### ブロッキングモード

```java
// テキスト生成クライアントの作成
DifyCompletionClient completionClient = DifyClientFactory.createCompletionClient("https://api.dify.ai/v1", "your-api-key");

// リクエストの作成
Map<String, Object> inputs = new HashMap<>();
inputs.put("content", "なす");

CompletionRequest request = CompletionRequest.builder()
    .inputs(inputs)
    .responseMode(ResponseMode.BLOCKING)
    .user("user-123")
    .build();

// リクエストを送信して応答を取得
CompletionResponse response = completionClient.sendCompletionMessage(request);
System.out.println("生成されたテキスト: " + response.getAnswer());
```

#### ストリーミングモード

```java
// リクエストの作成
Map<String, Object> inputs = new HashMap<>();
inputs.put("content", "なす");

CompletionRequest request = CompletionRequest.builder()
    .inputs(inputs)
    .responseMode(ResponseMode.STREAMING)
    .user("user-123")
    .build();

// ストリーミングリクエストの送信
completionClient.sendCompletionMessageStream(request, new CompletionStreamCallback() {
    @Override
    public void onMessage(MessageEvent event) {
        System.out.println("メッセージチャンク受信: " + event.getAnswer());
    }

    @Override
    public void onMessageEnd(MessageEndEvent event) {
        System.out.println("メッセージ終了、完全なメッセージID: " + event.getMessageId());
    }

    @Override
    public void onError(ErrorEvent event) {
        System.err.println("エラー: " + event.getMessage());
    }

    @Override
    public void onException(Throwable throwable) {
        System.err.println("例外: " + throwable.getMessage());
    }
});
```

#### 生成の停止

```java
// テキスト生成の停止
SimpleResponse stopResponse = completionClient.stopCompletion(taskId, "user-123");
```

### 3. ワークフローアプリケーション

#### ブロッキングモード

```java
// ワークフロークライアントの作成
DifyWorkflowClient workflowClient = DifyClientFactory.createWorkflowClient("https://api.dify.ai/v1", "your-api-key");

// ワークフローリクエストの作成
Map<String, Object> inputs = new HashMap<>();
inputs.put("content", "AIアプリケーションのシナリオを紹介してください");

WorkflowRunRequest request = WorkflowRunRequest.builder()
    .inputs(inputs)
    .responseMode(ResponseMode.BLOCKING)
    .user("user-123")
    .build();

// ワークフローを実行して応答を取得
WorkflowRunResponse response = workflowClient.runWorkflow(request);
System.out.println("ワークフロー実行ID: " + response.getTaskId());

// 結果の出力
if (response.getData() != null) {
    for (Map.Entry<String, Object> entry : response.getData().getOutputs().entrySet()) {
        System.out.println(entry.getKey() + ": " + entry.getValue());
    }
}
```

#### ストリーミングモード

```java
// ワークフローリクエストの作成
Map<String, Object> inputs = new HashMap<>();
inputs.put("content", "機械学習の基本原理を詳しく説明してください");

WorkflowRunRequest request = WorkflowRunRequest.builder()
    .inputs(inputs)
    .responseMode(ResponseMode.STREAMING)
    .user("user-123")
    .build();

// ワークフローストリーミングリクエストの実行
workflowClient.runWorkflowStream(request, new WorkflowStreamCallback() {
    @Override
    public void onWorkflowStarted(WorkflowStartedEvent event) {
        System.out.println("ワークフロー開始: " + event);
    }

    @Override
    public void onNodeStarted(NodeStartedEvent event) {
        System.out.println("ノード開始: " + event);
    }

    @Override
    public void onNodeFinished(NodeFinishedEvent event) {
        System.out.println("ノード完了: " + event);
    }

    @Override
    public void onWorkflowFinished(WorkflowFinishedEvent event) {
        System.out.println("ワークフロー完了: " + event);
    }

    @Override
    public void onError(ErrorEvent event) {
        System.err.println("エラー: " + event.getMessage());
    }

    @Override
    public void onException(Throwable throwable) {
        System.err.println("例外: " + throwable.getMessage());
    }
});
```

#### ワークフロー管理

```java
// ワークフローの停止
WorkflowStopResponse stopResponse = workflowClient.stopWorkflow(taskId, "user-123");

// ワークフロー実行状況の取得
WorkflowRunStatusResponse statusResponse = workflowClient.getWorkflowRun(workflowRunId);

// ワークフローログの取得
WorkflowLogsResponse logsResponse = workflowClient.getWorkflowLogs(null, null, 1, 10);
```

### 4. ナレッジベース管理

#### ナレッジベースの作成と管理

```java
// ナレッジベースクライアントの作成
DifyDatasetsClient datasetsClient = DifyClientFactory.createDatasetsClient("https://api.dify.ai/v1", "your-api-key");

// ナレッジベースの作成
CreateDatasetRequest createRequest = CreateDatasetRequest.builder()
    .name("テストナレッジベース-" + System.currentTimeMillis())
    .description("これはテストナレッジベースです")
    .indexingTechnique("high_quality")
    .permission("only_me")
    .provider("vendor")
    .build();

DatasetResponse dataset = datasetsClient.createDataset(createRequest);
System.out.println("作成されたナレッジベースID: " + dataset.getId());

// ナレッジベースリストの取得
DatasetListResponse datasetList = datasetsClient.getDatasets(1, 10);
System.out.println("ナレッジベース総数: " + datasetList.getTotal());
```

#### ドキュメント管理

```java
// テキストによるドキュメント作成 - 自動モード（推奨）
CreateDocumentByTextRequest docRequest = CreateDocumentByTextRequest.builder()
    .name("テストドキュメント-" + System.currentTimeMillis())
    .text("これはテストドキュメントの内容です。\nこれは2行目の内容です。\nこれは3行目の内容です。")
    .indexingTechnique("high_quality")
    .docForm("text_model")
    .docLanguage("Japanese")
    .processRule(ProcessRule.builder()
        .mode("automatic")  // 自動処理モードを使用
        .build())
    .build();

DocumentResponse docResponse = datasetsClient.createDocumentByText(datasetId, docRequest);
System.out.println("作成されたドキュメントID: " + docResponse.getDocument().getId());

// ドキュメントリストの取得
DocumentListResponse docList = datasetsClient.getDocuments(datasetId, null, 1, 10);
System.out.println("ドキュメント総数: " + docList.getTotal());

// ドキュメントの削除
SimpleResponse deleteResponse = datasetsClient.deleteDocument(datasetId, documentId);
```

#### ナレッジベース検索

```java
// 検索リクエストの作成
RetrievalModel retrievalModel = new RetrievalModel();
retrievalModel.setTopK(3);
retrievalModel.setScoreThreshold(0.5f);

RetrieveRequest retrieveRequest = RetrieveRequest.builder()
    .query("人工知能とは何ですか")
    .retrievalModel(retrievalModel)
    .build();

// 検索リクエストの送信
RetrieveResponse retrieveResponse = datasetsClient.retrieveDataset(datasetId, retrieveRequest);

// 検索結果の処理
System.out.println("検索クエリ: " + retrieveResponse.getQuery().getContent());
System.out.println("検索結果数: " + retrieveResponse.getRecords().size());
retrieveResponse.getRecords().forEach(record -> {
    System.out.println("スコア: " + record.getScore());
    System.out.println("内容: " + record.getSegment().getContent());
});
```

#### ドキュメントのダウンロード

```java
// 単一ドキュメント: 署名付きダウンロード URL を取得
DocumentDownloadUrlResponse url = datasetsClient.getDocumentDownloadUrl(datasetId, documentId);

// バッチ: 最大 100 件のドキュメントを ZIP としてダウンロード
DocumentBatchDownloadRequest batchRequest = DocumentBatchDownloadRequest.builder()
    .documentIds(java.util.Arrays.asList(documentId1, documentId2))
    .build();
try (FilePreviewResponse zip = datasetsClient.downloadDocumentsAsZip(datasetId, batchRequest)) {
    java.nio.file.Files.write(java.nio.file.Paths.get(zip.getFileName()), zip.getContentAsBytes());
}
```

#### ナレッジベース Pipeline (RAG Pipeline)

```java
// 1) Pipeline に設定済みのデータソースノードを一覧
List<DatasourcePluginResponse> nodes = datasetsClient.listPipelineDatasourcePlugins(datasetId, true);
String startNodeId = nodes.stream()
    .filter(node -> "local_file".equals(node.getDatasourceType()))
    .findFirst().orElseThrow(() -> new IllegalStateException("Configure a local_file node first"))
    .getNodeId();

// 2) Pipeline 用のファイルをアップロード
PipelineFileUploadResponse uploaded = datasetsClient.uploadPipelineFile(new java.io.File("./doc.pdf"));

// 3) 公開済み Pipeline を投入（JSON のキュー受付結果を返す）
java.util.Map<String, Object> item = new java.util.HashMap<>();
item.put("reference", uploaded.getId());
item.put("name", uploaded.getName());

PipelineRunRequest runRequest = PipelineRunRequest.builder()
    .inputs(new java.util.HashMap<>())
    .datasourceType("local_file")
    .datasourceInfoList(java.util.Arrays.asList(item))
    .startNodeId(startNodeId)
    .isPublished(true)
    .build();

java.util.Map<String, Object> result = datasetsClient.runPipeline(datasetId, runRequest);

// SSE はドラフト用。ノード ID は公開済みバージョンと異なる場合がある
String draftStartNodeId = datasetsClient.listPipelineDatasourcePlugins(datasetId, false).stream()
    .filter(node -> "local_file".equals(node.getDatasourceType()))
    .findFirst().orElseThrow(() -> new IllegalStateException("Configure a draft local_file node first"))
    .getNodeId();
PipelineRunRequest draftRequest = PipelineRunRequest.builder()
    .inputs(new java.util.HashMap<>())
    .datasourceType("local_file")
    .datasourceInfoList(java.util.Arrays.asList(item))
    .startNodeId(draftStartNodeId)
    .isPublished(false)
    .build();
datasetsClient.runPipelineStream(datasetId, draftRequest, callback);

// 4) 設定済みの online_drive ノードを実行（以下のコールバックは 1.7.0 が必要）
DatasourcePluginResponse driveNode = nodes.stream()
    .filter(node -> "online_drive".equals(node.getDatasourceType()))
    .findFirst().orElseThrow(() -> new IllegalStateException("Configure an online_drive node first"));
java.util.Map<String, Object> driveInputs = new java.util.HashMap<>();
// driveNode.getUserInputVariables() に従って入力を設定し、認証情報は Dify で事前に設定
DatasourceNodeRunRequest nodeRequest = DatasourceNodeRunRequest.builder()
    .inputs(driveInputs)
    .datasourceType("online_drive")
    .isPublished(true)
    .build();
datasetsClient.runPipelineDatasourceNodeStream(datasetId, driveNode.getNodeId(), nodeRequest,
    new WorkflowStreamCallback() {
        @Override
        public void onDatasourceCompleted(DatasourceCompletedEvent event) {
            System.out.println(event.getData());
        }

        @Override
        public void onDatasourceError(DatasourceErrorEvent event) {
            System.err.println(event.getError());
        }

        @Override
        public void onStreamComplete() {
            System.out.println("Stream complete");
        }
    });
```

### 5. エンドユーザー (End Users)

```java
// ID でエンドユーザー詳細を取得（他 API の created_by がエンドユーザー ID の場合の逆引きに便利）
EndUserResponse endUser = client.getEndUser(endUserId);
```

### 6. 人間の介入 (Human Input Flow)

Dify 1.14.2+ ではワークフロー/チャットフローに Human Input ノードを組み込んで、フォームの提出を待つ運用が可能です。

API でフォームを取得・送信するには、Human Input ノードで WebApp 配信を有効にしてください。他の承認チャネルでは `form_token` が提供されない場合があります。

```java
// 1) ワークフローに購読し、人間介入到達時に onHumanInputRequired が発火
workflowClient.runWorkflowStream(request, new WorkflowStreamCallback() {
    @Override
    public void onHumanInputRequired(HumanInputRequiredEvent event) {
        String formToken = event.getData().getFormToken();
        String workflowRunId = event.getWorkflowRunId();
        handoffToReviewer(formToken, workflowRunId);
    }
});

// 2) 一時停止中のフォームを取得
HumanInputFormResponse form = client.getHumanInputForm(formToken);

// 3) フォームを送信してワークフローを再開
java.util.Map<String, Object> inputs = new java.util.HashMap<>();
inputs.put("decision", "approve");
HumanInputFormSubmitRequest submit = HumanInputFormSubmitRequest.builder()
    .inputs(inputs)
    .action("action-approve")
    .user("reviewer-alice")
    .build();
client.submitHumanInputForm(formToken, submit);

// 4) 再開後のイベント購読
// 再開時は承認者ではなく、元の実行を開始した user を使用
workflowClient.streamWorkflowEvents(workflowRunId, request.getUser(), true, false, callback);
```

### 7. 思考ストリーム (reasoning_chunk)

Chatflow の LLM ノードで `reasoning_format=separated` を有効にすると、モデルの思考内容が `reasoning_chunk` イベントとして本文と並行してストリーミングされます。

```java
chatflowClient.sendChatMessageStream(message, new ChatflowStreamCallback() {
    @Override
    public void onReasoningChunk(ReasoningChunkEvent event) {
        renderThinking(event.getData().getReasoning(),
                       Boolean.TRUE.equals(event.getData().getIsFinal()));
    }
});
```

## API リファレンス

### クライアントタイプ

| クライアントタイプ | 説明 | 主な機能 |
|-----------------|------|----------|
| `DifyClient` | 完全クライアント | すべてのAPI機能をサポート |
| `DifyChatClient` | チャットアプリケーションクライアント | 会話、会話管理、メッセージフィードバック |
| `DifyCompletionClient` | テキスト生成アプリケーションクライアント | テキスト生成、生成停止 |
| `DifyChatflowClient` | ワークフロー制御会話アプリケーションクライアント | ワークフロー制御会話 |
| `DifyWorkflowClient` | ワークフローアプリケーションクライアント | ワークフロー実行、ワークフロー管理 |
| `DifyDatasetsClient` | ナレッジベースクライアント | ナレッジベース管理、ドキュメント管理、検索、バッチ/署名付きダウンロード、RAG Pipeline |

### レスポンスモード

| モード | 列挙値 | 説明 |
|-------|--------|------|
| ブロッキングモード | `ResponseMode.BLOCKING` | 同期呼び出し、完全な応答を待機 |
| ストリーミングモード | `ResponseMode.STREAMING` | コールバックを通じてリアルタイムで生成されたコンテンツを受信 |

### イベントタイプ

| イベントタイプ | 説明 |
|--------------|------|
| `MessageEvent` | メッセージイベント、生成されたテキストチャンクを含む |
| `MessageEndEvent` | メッセージ終了イベント、完全なメッセージIDを含む |
| `MessageFileEvent` | ファイルメッセージイベント、ファイル情報を含む |
| `TtsMessageEvent` | テキストから音声へのイベント |
| `TtsMessageEndEvent` | テキストから音声への終了イベント |
| `MessageReplaceEvent` | メッセージ置換イベント |
| `AgentMessageEvent` | エージェントメッセージイベント |
| `AgentThoughtEvent` | エージェント思考イベント |
| `ReasoningChunkEvent` | LLM 思考ストリームイベント（reasoning_format=separated） |
| `WorkflowStartedEvent` | ワークフロー開始イベント |
| `NodeStartedEvent` | ノード開始イベント |
| `NodeFinishedEvent` | ノード完了イベント |
| `NodeRetryEvent` | ノードリトライイベント |
| `WorkflowFinishedEvent` | ワークフロー完了イベント |
| `WorkflowPausedEvent` | ワークフロー一時停止イベント（人間介入） |
| `HumanInputRequiredEvent` | 人間介入フォーム要求イベント |
| `HumanInputFormFilledEvent` | フォーム提出済みイベント |
| `HumanInputFormTimeoutEvent` | フォームタイムアウトイベント |
| `ErrorEvent` | エラーイベント |
| `PingEvent` | ピングイベント |

## 高度な設定

### カスタムHTTPクライアント

```java
// カスタム設定の作成
DifyConfig config = DifyConfig.builder()
    .baseUrl("https://api.dify.ai/v1")
    .apiKey("your-api-key")
    .connectTimeout(5000)  // 接続タイムアウト（ミリ秒）
    .readTimeout(60000)    // 読み取りタイムアウト（ミリ秒）
    .writeTimeout(30000)   // 書き込みタイムアウト（ミリ秒）
    .build();

// カスタム設定でクライアントを作成
DifyClient client = DifyClientFactory.createClient(config);
```

## 詳細ドキュメント

- [チャットアプリケーション API 例](src/test/java/io/github/imfangs/dify/client/DifyChatClientTest.java)
    - メッセージ送信（ブロッキング/ストリーミング）
    - 会話管理
    - メッセージフィードバック
    - 音声変換
    - 提案質問

- [テキスト生成アプリケーション API 例](src/test/java/io/github/imfangs/dify/client/DifyCompletionClientTest.java)
    - テキスト生成（ブロッキング/ストリーミング）
    - 生成停止
    - ファイル処理
    - テキストから音声

- [ワークフローアプリケーション API 例](src/test/java/io/github/imfangs/dify/client/DifyWorkflowClientTest.java)
    - ワークフロー実行（ブロッキング/ストリーミング）
    - ワークフロー停止
    - ワークフロー状態
    - ワークフローログ

- [ナレッジベース API 例](src/test/java/io/github/imfangs/dify/client/DifyDatasetsClientTest.java)
    - ナレッジベース管理
    - ドキュメント管理
    - セマンティック検索

- [イベントとコールバックの例](src/test/java/io/github/imfangs/dify/client/DifyChatflowClientTest.java)
    - メッセージイベント
    - ファイルイベント
    - TTSイベント
    - ワークフローイベント
    - エラー処理

## 開発と検証

現在のバージョンは **1.7.0** です。上記の Maven と Gradle の例もこのバージョンを使用しています。
ビルドには Maven 3.9.2+ が必要です。CI は JDK 8、17、21、25、26 を対象に設定しています。

```bash
mvn -B -ntp clean verify
```

通常のビルドはローカル回帰テストのみを実行し、Dify の認証情報や GPG は不要です。
実サービスのテストには `integration` タグがあります。専用のテスト環境を
`src/test/resources/dify-test-config.properties` に設定し、`mvn -Pintegration-tests test` で明示的に実行します。
これらのテストは Dify データを作成・変更・削除します。

1.7.0 から `onStreamComplete()` が追加され、Chatflow の両終了イベントの受信後、または正常な EOF 時に呼ばれます。
読み取り完了を表し、業務処理の成功は保証しません。API エラー、通信・解析例外では呼ばれません。
データソースの通知は `onDatasourceProcessing`、`onDatasourceCompleted`、`onDatasourceError` で受信します。
`datasource_completed` はページごとに複数回届く場合があります。根拠と検証範囲は [CURRENT.md](CURRENT.md) を参照してください。

## 貢献

コード貢献、問題報告、または改善提案を歓迎します。GitHub IssuesまたはPull Requestsを通じてプロジェクト開発に参加してください。

## ライセンス

このプロジェクトは[Apache License 2.0](LICENSE)の下でライセンスされています。

## 関連リンク

- [Dify ウェブサイト](https://dify.ai)
- [Dify ドキュメント](https://docs.dify.ai)
- [Dify GitHub](https://github.com/langgenius/dify) 

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=imfangs/dify-java-client&type=Date)](https://www.star-history.com/#imfangs/dify-java-client&Date)
