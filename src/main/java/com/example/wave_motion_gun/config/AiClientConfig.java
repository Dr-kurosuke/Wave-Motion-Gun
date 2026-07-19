package com.example.wave_motion_gun.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * AIモードのクライアント設定。
 * CLIENTタイプのconfigはプレイヤー自身のPCにのみ保存され、サーバーへ同期されない。
 * APIキーをここに置くことで、キーがサーバーや他プレイヤーに渡ることを防ぐ。
 */
public class AiClientConfig {
    public static final ForgeConfigSpec SPEC;

    public static final String PROVIDER_GEMINI = "gemini";
    public static final String PROVIDER_OPENAI = "openai";
    public static final String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com";

    public static final ForgeConfigSpec.BooleanValue AI_MODE;
    public static final ForgeConfigSpec.ConfigValue<String> PROVIDER;
    public static final ForgeConfigSpec.ConfigValue<String> GEMINI_API_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> GEMINI_MODEL;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_API_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_MODEL;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_BASE_URL;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("AI Question Mode - client side only. API keys never leave this machine.")
                .push("ai");

        AI_MODE = builder
                .comment("Enable AI-generated quiz questions (requires an API key for the selected provider)")
                .define("aiMode", false);

        PROVIDER = builder
                .comment("AI provider: \"gemini\" or \"openai\" (OpenAI-compatible; works with ChatGPT, Grok, DeepSeek, Ollama, etc.)")
                .define("provider", PROVIDER_GEMINI,
                        o -> o instanceof String s && (s.equals(PROVIDER_GEMINI) || s.equals(PROVIDER_OPENAI)));

        GEMINI_API_KEY = builder
                .comment("Your Google Gemini API key. Stored locally only. NEVER shared with servers or other players.")
                .define("geminiApiKey", "");

        GEMINI_MODEL = builder
                .comment("Gemini model id used for question generation")
                .define("geminiModel", "gemini-3.5-flash");

        OPENAI_API_KEY = builder
                .comment("Your OpenAI (or compatible) API key. Stored locally only. May be empty for local endpoints like Ollama.")
                .define("openaiApiKey", "");

        OPENAI_MODEL = builder
                .comment("Model id for the OpenAI-compatible endpoint")
                .define("openaiModel", "gpt-5-nano");

        OPENAI_BASE_URL = builder
                .comment("Base URL of the OpenAI-compatible endpoint. Change for Grok / DeepSeek / Ollama / LM Studio etc.")
                .define("openaiBaseUrl", DEFAULT_OPENAI_BASE_URL);

        builder.pop();
        SPEC = builder.build();
    }

    public static boolean isConfigured() {
        if (!AI_MODE.get()) return false;
        if (PROVIDER_OPENAI.equals(PROVIDER.get())) {
            // 互換API(Ollama等)はキー不要のことがあるため、Base URLが変更済みならキー無しでも許可
            return !OPENAI_API_KEY.get().isBlank()
                    || !OPENAI_BASE_URL.get().equals(DEFAULT_OPENAI_BASE_URL);
        }
        return !GEMINI_API_KEY.get().isBlank();
    }
}
