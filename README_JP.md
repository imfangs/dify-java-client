# Dify Java Client

[![Maven Central](https://img.shields.io/maven-central/v/io.github.ferrymo/dify-java-client.svg)](https://search.maven.org/search?q=g:io.github.ferrymo%20AND%20a:dify-java-client)
[![License](https://img.shields.io/github/license/imfangs/dify-java-client)](https://github.com/imfangs/dify-java-client/blob/main/LICENSE)
[![Java](https://img.shields.io/badge/Java-8%2B-blue)](https://www.java.com)

[English](README_EN.md) | [简体中文](README.md) | 日本語

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
    <groupId>io.github.ferrymo</groupId>
    <artifactId>dify-java-client</artifactId>
    <version>1.2.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.ferrymo:dify-java-client:1.2.0'
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

## API リファレンス

### クライアントタイプ

| クライアントタイプ | 説明 | 主な機能 |
|-----------------|------|----------|
| `DifyClient` | 完全クライアント | すべてのAPI機能をサポート |
| `DifyChatClient` | チャットアプリケーションクライアント | 会話、会話管理、メッセージフィードバック |
| `DifyCompletionClient` | テキスト生成アプリケーションクライアント | テキスト生成、生成停止 |
| `DifyChatflowClient` | ワークフロー制御会話アプリケーションクライアント | ワークフロー制御会話 |
| `DifyWorkflowClient` | ワークフローアプリケーションクライアント | ワークフロー実行、ワークフロー管理 |
| `DifyDatasetsClient` | ナレッジベースクライアント | ナレッジベース管理、ドキュメント管理、検索 |

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
| `WorkflowStartedEvent` | ワークフロー開始イベント |
| `NodeStartedEvent` | ノード開始イベント |
| `NodeFinishedEvent` | ノード完了イベント |
| `WorkflowFinishedEvent` | ワークフロー完了イベント |
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
