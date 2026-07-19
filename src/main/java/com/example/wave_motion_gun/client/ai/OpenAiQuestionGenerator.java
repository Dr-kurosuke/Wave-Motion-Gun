package com.example.wave_motion_gun.client.ai;

import com.example.wave_motion_gun.config.AiClientConfig;
import com.example.wave_motion_gun.utils.QuestionManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * OpenAI (ChatGPT) および OpenAI互換API (Grok / DeepSeek / Ollama / LM Studio 等) 用の問題生成。
 * Base URLをconfigで差し替えることで互換APIに対応する。
 * 1回目は構造化出力(json_schema)付きで要求し、互換APIが未対応で失敗した場合は
 * response_format無しで再試行する(出力形式はプロンプトでも指示済み)。
 */
public class OpenAiQuestionGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger("wave_motion_gun_mod/AI");
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    // ローカルLLMは応答が遅いことがあるため、Geminiより長めに取る
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(40);

    // OpenAIのstrictモードはルートがオブジェクト必須のため {"questions":[...]} でラップする
    // (バリデータが配列ルート・questionsラップの両方を受け付ける)
    // リクエスト毎のパースを避けるため一度だけ組み立てる
    private static final JsonObject RESPONSE_SCHEMA = JsonParser.parseString("""
            {
              "type": "object",
              "properties": {
                "questions": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "question": {"type": "string"},
                      "options": {"type": "array", "items": {"type": "string"}},
                      "correctIndex": {"type": "integer"}
                    },
                    "required": ["question", "options", "correctIndex"],
                    "additionalProperties": false
                  }
                }
              },
              "required": ["questions"],
              "additionalProperties": false
            }
            """).getAsJsonObject();

    /** ランクに応じた問題セットを非同期生成する。 */
    public static CompletableFuture<List<QuestionManager.Question>> generate(int rankValue, String languageCode) {
        String prompt = AiQuestionPrompt.build(rankValue, languageCode);
        // 1回目: json_schema強制 / 2回目: 互換APIフォールバック(response_formatなし)
        return requestOnce(prompt, true)
                .exceptionallyCompose(err -> {
                    // 1回目の失敗理由もログに残す(2回目の理由に上書きされて消えるのを防ぐ)
                    LOGGER.warn("OpenAI first attempt (with json_schema) failed, retrying without it: {}",
                            AiQuestionValidator.rootMessage(err));
                    return requestOnce(prompt, false);
                });
    }

    /**
     * baseUrl はユーザーが自由に設定できるため、APIキーを平文で送り出さないか検証する。
     *
     * http:// のままだと Authorization: Bearer &lt;キー&gt; がネットワーク上を平文で流れる。
     * ただしOllama等のローカルAPI利用(キー不要)は正当なユースケースなので、
     * ループバック宛て、またはキーを送らない場合に限り http を許可する。
     */
    private static void requireSafeEndpoint(URI uri, boolean sendsApiKey) {
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("Invalid API base URL (missing scheme): " + uri);
        }
        if (scheme.equalsIgnoreCase("https")) return;
        if (!scheme.equalsIgnoreCase("http")) {
            throw new IllegalArgumentException("Unsupported API base URL scheme: " + scheme);
        }

        String host = uri.getHost();
        boolean loopback = host != null && (host.equalsIgnoreCase("localhost")
                || host.equals("127.0.0.1") || host.equals("::1") || host.equals("[::1]"));
        if (sendsApiKey && !loopback) {
            throw new IllegalArgumentException(
                    "Refusing to send the API key over plain http to " + host
                            + ". Use https, or clear the API key for local endpoints.");
        }
    }

    private static CompletableFuture<List<QuestionManager.Question>> requestOnce(String prompt, boolean useSchema) {
        // リクエスト組み立て中の同期例外(不正なBase URL等)もfailedFutureに変換する
        try {
            String apiKey = AiClientConfig.OPENAI_API_KEY.get();
            String model = AiClientConfig.OPENAI_MODEL.get();
            String baseUrl = AiClientConfig.OPENAI_BASE_URL.get().trim();
            while (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            URI endpoint = URI.create(baseUrl + "/v1/chat/completions");
            requireSafeEndpoint(endpoint, !apiKey.isBlank());

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(endpoint)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(model, prompt, useSchema)));

            // Ollama等のローカルAPIはキー不要。設定されている場合のみ付与する
            if (!apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }

            return HTTP.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            throw new IllegalStateException("OpenAI-compatible API error: HTTP " + response.statusCode()
                                    + " (schema=" + useSchema + ") body=" + AiQuestionValidator.snippet(response.body(), 300));
                        }
                        return AiQuestionValidator.fromJsonText(extractText(response.body()));
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static String buildRequestBody(String model, String prompt, boolean useSchema) {
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject root = new JsonObject();
        root.addProperty("model", model);
        root.add("messages", messages);

        if (useSchema) {
            JsonObject jsonSchema = new JsonObject();
            jsonSchema.addProperty("name", "quiz");
            jsonSchema.addProperty("strict", true);
            jsonSchema.add("schema", RESPONSE_SCHEMA);

            JsonObject responseFormat = new JsonObject();
            responseFormat.addProperty("type", "json_schema");
            responseFormat.add("json_schema", jsonSchema);
            root.add("response_format", responseFormat);
        }

        return GSON.toJson(root);
    }

    private static String extractText(String responseBody) {
        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
        return root.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
    }
}
