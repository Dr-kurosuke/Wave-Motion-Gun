package com.example.wave_motion_gun.client.ai;

import com.example.wave_motion_gun.config.AiClientConfig;
import com.example.wave_motion_gun.utils.QuestionManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * AI問題生成の入口。configのプロバイダ設定に応じて実装を選ぶ。
 * サーバー側はプロバイダを一切関知しない(届いた問題セットの検証・判定のみ)。
 */
public class AiQuestionService {

    public static CompletableFuture<List<QuestionManager.Question>> generate(int rankValue, String languageCode) {
        return switch (AiClientConfig.PROVIDER.get()) {
            case "openai" -> OpenAiQuestionGenerator.generate(rankValue, languageCode);
            default -> GeminiQuestionGenerator.generate(rankValue, languageCode);
        };
    }
}
