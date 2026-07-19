package com.example.wave_motion_gun.client.ai;

import com.example.wave_motion_gun.config.AiClientConfig;
import com.example.wave_motion_gun.utils.QuestionManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Google Gemini API用の問題生成(クライアント側のみで動作)。
 * プロンプト生成と応答の検証は AiQuestionPrompt / AiQuestionValidator(共通)に委譲する。
 * - APIキーはこのクラスの外に出ない(URLではなくヘッダで送る・ログにも出さない)
 * - 呼び出しは完全非同期。ゲームスレッドをブロックしない
 */
public class GeminiQuestionGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger("wave_motion_gun_mod/AI");
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    // responseSchema でJSON構造を強制する (リクエスト毎のパースを避けるため一度だけ組み立てる)
    private static final JsonObject RESPONSE_SCHEMA = JsonParser.parseString("""
            {
              "type": "ARRAY",
              "items": {
                "type": "OBJECT",
                "properties": {
                  "question": {"type": "STRING"},
                  "options": {"type": "ARRAY", "items": {"type": "STRING"}},
                  "correctIndex": {"type": "INTEGER"}
                },
                "required": ["question", "options", "correctIndex"]
              }
            }
            """).getAsJsonObject();

    /** ランクに応じた問題セットを非同期生成する(1回だけ内部リトライ)。 */
    public static CompletableFuture<List<QuestionManager.Question>> generate(int rankValue, String languageCode) {
        return requestOnce(rankValue, languageCode)
                .exceptionallyCompose(err -> {
                    // 1回目の失敗理由もログに残す (OpenAI側と同様)
                    LOGGER.warn("Gemini first attempt failed, retrying: {}",
                            AiQuestionValidator.rootMessage(err));
                    return requestOnce(rankValue, languageCode);
                });
    }

    private static CompletableFuture<List<QuestionManager.Question>> requestOnce(int rankValue, String languageCode) {
        // リクエスト組み立て中の同期例外もfailedFutureに変換する (呼び出し元スレッドを壊さない)
        try {
            String apiKey = AiClientConfig.GEMINI_API_KEY.get();
            String model = AiClientConfig.GEMINI_MODEL.get();
            if (apiKey.isBlank()) {
                return CompletableFuture.failedFuture(new IllegalStateException("Gemini API key is not set"));
            }

            String body = buildRequestBody(rankValue, languageCode);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    // キーはURLに含めず、ヘッダで送る(ログやプロキシにURLとして残らないように)
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            throw new IllegalStateException("Gemini API error: HTTP " + response.statusCode()
                                    + " body=" + AiQuestionValidator.snippet(response.body(), 300));
                        }
                        return AiQuestionValidator.fromJsonText(extractText(response.body()));
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static String buildRequestBody(int rankValue, String languageCode) {
        String prompt = AiQuestionPrompt.build(rankValue, languageCode);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("responseMimeType", "application/json");
        generationConfig.add("responseSchema", RESPONSE_SCHEMA);

        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        JsonArray parts = new JsonArray();
        parts.add(part);
        JsonObject content = new JsonObject();
        content.add("parts", parts);
        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject root = new JsonObject();
        root.add("contents", contents);
        root.add("generationConfig", generationConfig);
        return GSON.toJson(root);
    }

    private static String extractText(String responseBody) {
        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
        return root.getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString();
    }
}
