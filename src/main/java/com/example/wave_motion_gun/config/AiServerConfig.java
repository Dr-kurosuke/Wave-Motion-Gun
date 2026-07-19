package com.example.wave_motion_gun.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * AIモードのサーバー設定。
 * 秘密情報は含まない(SERVERタイプのconfigはクライアントへ同期されるため、APIキーは絶対に置かない)。
 */
public class AiServerConfig {
    public static final ForgeConfigSpec SPEC;

    /** サーバーがクライアント生成のAI問題を受け入れるかどうか */
    public static final ForgeConfigSpec.BooleanValue ALLOW_AI_QUESTIONS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("AI Question Mode - server side policy (no secrets here)")
                .push("ai");

        ALLOW_AI_QUESTIONS = builder
                .comment("Allow players to use client-side AI-generated quiz questions on this server")
                .define("allowAiQuestions", true);

        builder.pop();
        SPEC = builder.build();
    }
}
