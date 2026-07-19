package com.example.wave_motion_gun.client.ai;

import com.example.wave_motion_gun.utils.QuestionManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * AIが返したJSONテキストの解析・サニタイズ・検証(プロバイダ共通)。
 * ルートが配列(Gemini)でも {"questions": [...]} オブジェクト(OpenAI strict)でも受け付ける。
 */
public class AiQuestionValidator {

    /** JSONテキストから問題リストを構築する。不正なら例外。 */
    public static List<QuestionManager.Question> fromJsonText(String jsonText) {
        JsonElement root = JsonParser.parseString(stripMarkdownFences(jsonText));
        JsonArray arr;
        if (root.isJsonArray()) {
            arr = root.getAsJsonArray();
        } else if (root.isJsonObject() && root.getAsJsonObject().has("questions")) {
            arr = root.getAsJsonObject().getAsJsonArray("questions");
        } else {
            throw new IllegalStateException("AI response is neither a JSON array nor a {questions:[...]} object");
        }

        List<QuestionManager.Question> result = new ArrayList<>();
        for (int i = 0; i < arr.size() && result.size() < AiQuestionPrompt.QUESTION_COUNT; i++) {
            JsonObject q = arr.get(i).getAsJsonObject();
            String text = sanitize(q.get("question").getAsString(), 300);
            JsonArray optArr = q.getAsJsonArray("options");
            String[] options = new String[optArr.size()];
            for (int j = 0; j < optArr.size(); j++) {
                options[j] = sanitize(optArr.get(j).getAsString(), 100);
            }
            int correct = q.get("correctIndex").getAsInt();
            if (text.isBlank() || options.length < 2 || options.length > 4
                    || correct < 0 || correct >= options.length) {
                throw new IllegalStateException("AI returned an invalid question at index " + i);
            }
            result.add(new QuestionManager.Question(text, options, correct));
        }
        if (result.size() != AiQuestionPrompt.QUESTION_COUNT) {
            throw new IllegalStateException("AI returned " + result.size() + " questions, expected " + AiQuestionPrompt.QUESTION_COUNT);
        }
        return result;
    }

    /**
     * 表示・パケット安全化:
     * - リテラルの「バックスラッシュ+n」を実際の改行に正規化(改行が\nと文字表示されるバグの修正)
     * - 制御文字(改行以外)と§(書式コード)を除去
     * - I18n/TranslatableComponent経由で描画されるため % をエスケープ
     */
    static String sanitize(String s, int maxLength) {
        String cleaned = s
                .replace("\\n", "\n")
                .replaceAll("[\\p{Cntrl}&&[^\n]]", "")
                .replace("§", "")
                .replace("%", "%%")
                .trim();
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }

    /**
     * APIキーらしき文字列を伏せる。
     *
     * レスポンス本文にキーが含まれないという保証は無い:
     *  - OpenAI公式の401本文は "Incorrect API key provided: sk-pr***XYZ." のようにキー断片を含む
     *  - baseUrl差し替えを許しているため、受け取った Authorization をそのまま
     *    エコーするゲートウェイ実装に当たる可能性がある
     * latest.log はバグ報告でそのまま貼られるので、ログに出す前に必ず通すこと。
     */
    private static String maskSecrets(String s) {
        return s.replaceAll("sk-[A-Za-z0-9_\\-]{8,}", "sk-***")
                .replaceAll("AIza[0-9A-Za-z_\\-]{10,}", "AIza***")
                .replaceAll("(?i)bearer\\s+\\S+", "Bearer ***");
    }

    /**
     * 診断ログ用にレスポンス本文を1行・短縮化する。
     * APIキーらしき文字列は maskSecrets で伏せてから返す。
     */
    public static String snippet(String body, int maxLength) {
        if (body == null) return "(empty)";
        String oneLine = maskSecrets(body.replaceAll("\\s+", " ").trim());
        if (oneLine.isEmpty()) return "(empty)";
        return oneLine.length() > maxLength ? oneLine.substring(0, maxLength) + "..." : oneLine;
    }

    /**
     * 例外チェーンの根本原因のメッセージを取り出す。
     * CompletableFutureは失敗を CompletionException で包むため、そのままだと理由が読みにくい。
     */
    public static String rootMessage(Throwable err) {
        if (err == null) return "no result";
        Throwable cause = err;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage();
        return msg != null ? msg : cause.toString();
    }

    /** ```json ... ``` のようなMarkdownフェンスに包まれた応答を剥がす(互換API対策) */
    private static String stripMarkdownFences(String s) {
        String trimmed = s.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            int lastFence = trimmed.lastIndexOf("```");
            if (lastFence >= 0) {
                trimmed = trimmed.substring(0, lastFence);
            }
        }
        return trimmed.trim();
    }
}
