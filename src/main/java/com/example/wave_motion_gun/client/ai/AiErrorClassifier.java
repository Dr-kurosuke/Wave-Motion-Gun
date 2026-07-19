package com.example.wave_motion_gun.client.ai;

/**
 * AI通信失敗の原因を推定し、チャット表示用langキーのサフィックスへ分類する。
 * 判定材料は例外型チェーンと、生成器が例外メッセージに積む "HTTP xxx body=..." 文字列
 * (bodyにはinsufficient_quota等のプロバイダ側エラーコードが含まれる)。
 */
public class AiErrorClassifier {

    /** 返り値は message.wave_motion_gun_mod.ai.reason.&lt;suffix&gt; のサフィックス */
    public static String classify(Throwable err) {
        if (err == null) return "unknown";

        // 例外型ベースの判定(接続系)
        for (Throwable t = err; t != null; t = t.getCause()) {
            if (t instanceof java.net.http.HttpTimeoutException) return "timeout";
            if (t instanceof java.net.UnknownHostException || t instanceof java.net.ConnectException) {
                return "network";
            }
        }

        String all = collectMessages(err).toLowerCase();

        // 認証系: OpenAIは401/403、Geminiはキー不正でも 400 + API_KEY_INVALID を返す
        if (all.contains("http 401") || all.contains("http 403")
                || all.contains("api_key_invalid") || all.contains("api key not valid")
                || all.contains("invalid_api_key") || all.contains("incorrect api key")) {
            return "auth";
        }
        // クレジット/無料枠の残高不足 (429だがレート制限とは別物)
        if (all.contains("insufficient_quota") || (all.contains("http 429") && all.contains("quota"))) {
            return "quota";
        }
        if (all.contains("http 429") || all.contains("resource_exhausted")) return "rate_limit";
        // モデルID誤り/BaseURLのパス誤りはどちらも404になることが多い
        if (all.contains("http 404") || all.contains("model_not_found")
                || all.contains("is not found for api version")) {
            return "model_or_url";
        }
        if (all.contains("http 400") || all.contains("http 4")) return "bad_request";
        if (all.contains("http 5")) return "server_error";
        for (Throwable t = err; t != null; t = t.getCause()) {
            if (t instanceof java.io.IOException) return "network";
        }
        return "unknown";
    }

    /** 原因チェーン全体のメッセージを連結する */
    private static String collectMessages(Throwable err) {
        StringBuilder sb = new StringBuilder();
        for (Throwable t = err; t != null; t = t.getCause()) {
            sb.append(t.getClass().getSimpleName()).append(": ");
            if (t.getMessage() != null) sb.append(t.getMessage());
            sb.append(" | ");
        }
        return sb.toString();
    }
}
