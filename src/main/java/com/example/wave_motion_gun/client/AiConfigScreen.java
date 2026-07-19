package com.example.wave_motion_gun.client;

import net.minecraft.network.chat.Component;

import com.example.wave_motion_gun.config.AiClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

/**
 * AIモード設定画面(Mod一覧の「設定」ボタンから開く)。
 * プロバイダ(Gemini / OpenAI互換)を切り替えられる。キーはプロバイダごとに保持し、
 * マスク表示のうえ、このPCのクライアントconfigにのみ保存される。
 */
public class AiConfigScreen extends Screen {
    @Nullable
    private final Screen parent;

    private boolean aiMode;
    private String provider;
    // プロバイダごとの入力値(切替時に消えないよう画面側で保持)
    private String geminiKey;
    private String geminiModel;
    private String openaiKey;
    private String openaiModel;
    private String openaiBase;

    private EditBox apiKeyField;
    private EditBox modelField;
    private EditBox baseUrlField;
    private boolean revealKey = false;

    public AiConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("gui.wave_motion_gun_mod.ai_config.title"));
        this.parent = parent;
        this.aiMode = AiClientConfig.AI_MODE.get();
        this.provider = AiClientConfig.PROVIDER.get();
        this.geminiKey = AiClientConfig.GEMINI_API_KEY.get();
        this.geminiModel = AiClientConfig.GEMINI_MODEL.get();
        this.openaiKey = AiClientConfig.OPENAI_API_KEY.get();
        this.openaiModel = AiClientConfig.OPENAI_MODEL.get();
        this.openaiBase = AiClientConfig.OPENAI_BASE_URL.get();
    }

    private boolean isOpenAi() {
        return AiClientConfig.PROVIDER_OPENAI.equals(provider);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        // AIモード ON/OFF
        this.addRenderableWidget(Button.builder(modeLabel(), btn -> {
            this.aiMode = !this.aiMode;
            btn.setMessage(modeLabel());
        }).bounds(centerX - 100, 35, 200, 20).build());

        // プロバイダ切替
        this.addRenderableWidget(Button.builder(providerLabel(), btn -> {
            captureFields();
            this.provider = isOpenAi() ? AiClientConfig.PROVIDER_GEMINI : AiClientConfig.PROVIDER_OPENAI;
            rebuild();
        }).bounds(centerX - 100, 60, 200, 20).build());

        // APIキー(マスク表示)
        this.apiKeyField = new EditBox(this.font, centerX - 100, 98, 176, 20,
                Component.translatable("gui.wave_motion_gun_mod.ai_config.api_key"));
        this.apiKeyField.setMaxLength(200);
        this.apiKeyField.setValue(isOpenAi() ? openaiKey : geminiKey);
        this.apiKeyField.setFormatter((text, pos) -> revealKey
                ? FormattedCharSequence.forward(text, Style.EMPTY)
                : FormattedCharSequence.forward("*".repeat(text.length()), Style.EMPTY));
        this.addRenderableWidget(this.apiKeyField);

        // マスク解除ボタン(*: マスク中 / A: 表示中)
        this.addRenderableWidget(Button.builder(
                Component.literal(revealKey ? "A" : "*"), btn -> {
            this.revealKey = !this.revealKey;
            btn.setMessage(Component.literal(this.revealKey ? "A" : "*"));
        }).bounds(centerX + 80, 98, 20, 20).build());

        // モデルID
        this.modelField = new EditBox(this.font, centerX - 100, 138, 200, 20,
                Component.translatable("gui.wave_motion_gun_mod.ai_config.model"));
        this.modelField.setMaxLength(100);
        this.modelField.setValue(isOpenAi() ? openaiModel : geminiModel);
        this.addRenderableWidget(this.modelField);

        // Base URL(OpenAI互換のみ)
        if (isOpenAi()) {
            this.baseUrlField = new EditBox(this.font, centerX - 100, 178, 200, 20,
                    Component.translatable("gui.wave_motion_gun_mod.ai_config.base_url"));
            this.baseUrlField.setMaxLength(200);
            this.baseUrlField.setValue(openaiBase);
            this.addRenderableWidget(this.baseUrlField);
        } else {
            this.baseUrlField = null;
        }

        // 完了(保存)
        this.addRenderableWidget(Button.builder(
                CommonComponents.GUI_DONE, btn -> this.onClose())
                .bounds(centerX - 100, this.height - 28, 200, 20).build());
    }

    private Component modeLabel() {
        return Component.translatable("gui.wave_motion_gun_mod.ai_config.mode",
                I18n.get(aiMode ? "options.on" : "options.off"));
    }

    private Component providerLabel() {
        String name = I18n.get(isOpenAi()
                ? "gui.wave_motion_gun_mod.ai_config.provider_openai"
                : "gui.wave_motion_gun_mod.ai_config.provider_gemini");
        return Component.translatable("gui.wave_motion_gun_mod.ai_config.provider", name);
    }

    /** 現在の入力欄の値を、選択中プロバイダの保持変数へ書き戻す */
    private void captureFields() {
        if (apiKeyField == null || modelField == null) return;
        if (isOpenAi()) {
            openaiKey = apiKeyField.getValue().trim();
            if (!modelField.getValue().isBlank()) openaiModel = modelField.getValue().trim();
            if (baseUrlField != null && !baseUrlField.getValue().isBlank()) {
                openaiBase = baseUrlField.getValue().trim();
            }
        } else {
            geminiKey = apiKeyField.getValue().trim();
            if (!modelField.getValue().isBlank()) geminiModel = modelField.getValue().trim();
        }
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    // 【修正】ウィンドウリサイズ時はEditBoxが作り直されるため、
    // 入力中の値を保持変数へ退避してから再構築する (入力消失防止)
    @Override
    public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        captureFields();
        super.resize(minecraft, width, height);
    }

    @Override
    public void onClose() {
        captureFields();

        // 保存(CLIENT configファイルへ。サーバーには送信されない)
        AiClientConfig.AI_MODE.set(this.aiMode);
        AiClientConfig.PROVIDER.set(this.provider);
        AiClientConfig.GEMINI_API_KEY.set(this.geminiKey);
        AiClientConfig.GEMINI_MODEL.set(this.geminiModel);
        AiClientConfig.OPENAI_API_KEY.set(this.openaiKey);
        AiClientConfig.OPENAI_MODEL.set(this.openaiModel);
        AiClientConfig.OPENAI_BASE_URL.set(this.openaiBase);
        AiClientConfig.SPEC.save();

        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int centerX = this.width / 2;

        guiGraphics.drawCenteredString(this.font, this.title.getString(), centerX, 15, 0xFFFFFF);

        // ラベル(各入力欄の直上)
        guiGraphics.drawString(this.font, I18n.get("gui.wave_motion_gun_mod.ai_config.api_key"), centerX - 100, 88, 0xAAAAAA);
        guiGraphics.drawString(this.font, I18n.get("gui.wave_motion_gun_mod.ai_config.model"), centerX - 100, 128, 0xAAAAAA);
        if (isOpenAi()) {
            guiGraphics.drawString(this.font, I18n.get("gui.wave_motion_gun_mod.ai_config.base_url"), centerX - 100, 168, 0xAAAAAA);
        }

        // 注意書き(画面が狭い場合は省略)
        if (this.height >= 270) {
            guiGraphics.drawCenteredString(this.font, I18n.get("gui.wave_motion_gun_mod.ai_config.note1"), centerX, this.height - 64, 0x777777);
            guiGraphics.drawCenteredString(this.font, I18n.get("gui.wave_motion_gun_mod.ai_config.note2"), centerX, this.height - 52, 0x777777);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
