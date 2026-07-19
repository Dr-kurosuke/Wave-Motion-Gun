package com.example.wave_motion_gun.client;

import net.minecraft.network.chat.Component;

import com.example.wave_motion_gun.client.ai.AiQuestionCache;
import com.example.wave_motion_gun.client.ai.AiSessionManager;
import com.example.wave_motion_gun.config.AiClientConfig;
import com.example.wave_motion_gun.entity.MessengerEntity;
import com.example.wave_motion_gun.network.MessengerResponsePacket;
import com.example.wave_motion_gun.network.PacketHandler;
import com.example.wave_motion_gun.utils.QuestionManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class MessengerScreen extends Screen {
    private final MessengerEntity entity;
    private int knownQuestionIndex = -1;
    private QuestionManager.Question currentQuestion;

    // 表示モード
    private enum Mode { STATIC, AI, AI_CONNECTING, AI_UNAVAILABLE }
    private Mode mode = Mode.STATIC;

    // AI失敗→静的問題へフォールバックしたことの通知表示
    private boolean showFallbackNotice = false;

    // 質問文の折り返し結果キャッシュ (毎フレームのI18n.get/splitを回避)
    // 折り返し幅はthis.widthに依存するため、生成時の幅も保持して変化時に再構築する
    private List<FormattedCharSequence> cachedQuestionLines = new ArrayList<>();
    private int cachedWrapWidth = -1;

    public MessengerScreen(MessengerEntity entity) {
        super(Component.translatable("gui.wave_motion_gun_mod.messenger.title"));
        this.entity = entity;
    }

    @Override
    protected void init() {
        super.init();
        // AIモードが設定済みで、まだAIセッションが受理されていなければ生成を開始
        if (!entity.isAiActive() && AiClientConfig.isConfigured() && !AiSessionManager.hasFailed(entity.getId())) {
            AiSessionManager.beginIfNeeded(entity);
        }
        refreshQuestion();
    }

    /** 現在の状況から表示モードを決める */
    private Mode resolveMode() {
        if (entity.isAiActive()) {
            // サーバー受理済み。生成元クライアントはローカルコピーで表示できる
            return AiQuestionCache.get(entity.getId()) != null ? Mode.AI : Mode.AI_UNAVAILABLE;
        }
        if (AiSessionManager.isBusy(entity.getId())) {
            return Mode.AI_CONNECTING;
        }
        showFallbackNotice = AiSessionManager.hasFailed(entity.getId());
        return Mode.STATIC;
    }

    /** 現在のモードで出題に使う問題リスト(質問表示状態でない場合はnull) */
    private List<QuestionManager.Question> activeQuestions() {
        return switch (mode) {
            case AI -> AiQuestionCache.get(entity.getId());
            case STATIC -> QuestionManager.getChain(entity.getChainIndex()).questions;
            default -> null;
        };
    }

    /** 質問文をlangキー解決→改行分割→幅で折り返してキャッシュする */
    private void rebuildQuestionLines() {
        List<FormattedCharSequence> lines = new ArrayList<>();
        if (currentQuestion != null) {
            // text はlangキー(STATIC)またはリテラル(AI)。I18n.getは未定義キーを
            // そのまま返すため共通経路で描画できる。\nは段落区切りとして維持しつつ、
            // 英語などAIが改行を入れない長文でも画面に収まるよう幅で自動折り返しする
            int maxWidth = Math.min(this.width - 40, 320);
            for (String part : I18n.get(currentQuestion.text).split("\n")) {
                lines.addAll(this.font.split(FormattedText.of(part), maxWidth));
            }
            this.cachedWrapWidth = maxWidth;
        } else {
            this.cachedWrapWidth = -1;
        }
        this.cachedQuestionLines = lines;
    }

    private void refreshQuestion() {
        this.clearWidgets();
        this.mode = resolveMode();
        this.currentQuestion = null;

        List<QuestionManager.Question> questions = activeQuestions();
        if (questions == null) {
            // 接続中 or 他プレイヤーのAIセッション: ボタン無し(renderで状態表示)
            this.knownQuestionIndex = -1;
            rebuildQuestionLines();
            return;
        }

        int qIdx = entity.getQuestionIndex();

        if (qIdx >= 0 && qIdx < questions.size()) {
            this.currentQuestion = questions.get(qIdx);
            this.knownQuestionIndex = qIdx;

            int centerX = this.width / 2;
            int centerY = this.height / 2;
            for (int i = 0; i < currentQuestion.options.length; i++) {
                final int index = i;
                // STATIC時はlangキー、AI時はリテラル文字列。TranslatableComponentは
                // 未定義キーをそのまま表示するため、どちらもこの1経路で描画できる
                this.addRenderableWidget(Button.builder(
                        Component.translatable(currentQuestion.options[i]), (btn) -> {
                    sendAnswer(index);
                }).bounds(centerX - 100, centerY + 20 + (i * 25), 200, 20).build());
            }
        } else if (qIdx >= questions.size()) {
            // 全問正解: 報酬はサーバー側の補給物資投下(SupplyDrop)で処理されるため、画面を閉じるだけでよい
            this.onClose();
        }
        rebuildQuestionLines();
    }

    private void sendAnswer(int index) {
        PacketHandler.INSTANCE.sendToServer(new MessengerResponsePacket(entity.getId(), index));
        // ボタン連打防止のために少し無効化してもよい
    }

    @Override
    public void tick() {
        super.tick();

        // ▼▼▼ 追加修正 ▼▼▼
        // 不正解コード (-1) を検知したら、即座にGUIを閉じる
        if (entity.getQuestionIndex() == -1) {
            AiSessionManager.cleanup(entity.getId());
            this.onClose();
            return;
        }
        // ▲▲▲ 追加修正 ▲▲▲

        if (!entity.isAlive()) {
            AiSessionManager.cleanup(entity.getId());
            this.onClose();
            return;
        }

        // モード遷移(生成完了・サーバー受理・失敗フォールバック)や問題進行を検知して再構築
        if (resolveMode() != mode || entity.getQuestionIndex() != knownQuestionIndex) {
            refreshQuestion();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        switch (mode) {
            case AI_CONNECTING -> {
                guiGraphics.drawCenteredString(this.font, I18n.get("gui.wave_motion_gun_mod.ai.connecting"),
                        centerX, centerY - 20, 0x00FFFF);
                // 没入感のためプロバイダ名(Gemini/ChatGPT等)は表示しない
                guiGraphics.drawCenteredString(this.font,
                        I18n.get("gui.wave_motion_gun_mod.ai.connecting_sub"),
                        centerX, centerY, 0xAAAAAA);
            }
            case AI_UNAVAILABLE -> guiGraphics.drawCenteredString(this.font,
                    I18n.get("gui.wave_motion_gun_mod.ai.busy"), centerX, centerY - 10, 0xAAAAAA);
            default -> {
                if (currentQuestion != null) {
                    int qNum = knownQuestionIndex + 1;
                    String header = mode == Mode.AI
                            ? I18n.get("gui.wave_motion_gun_mod.messenger.comm_code_ai", qNum)
                            : I18n.get("gui.wave_motion_gun_mod.messenger.comm_code", qNum);
                    guiGraphics.drawCenteredString(this.font, header, centerX, centerY - 60, 0x00FFFF);

                    if (showFallbackNotice) {
                        guiGraphics.drawCenteredString(this.font, I18n.get("gui.wave_motion_gun_mod.ai.fallback"),
                                centerX, centerY - 72, 0x777777);
                    }

                    // 折り返し済みの行キャッシュを使用 (画面幅が変わった時だけ再構築)
                    if (Math.min(this.width - 40, 320) != cachedWrapWidth) {
                        rebuildQuestionLines();
                    }
                    List<FormattedCharSequence> lines = cachedQuestionLines;
                    // 行数が多いときは選択肢ボタン(centerY+20〜)に重ならないよう上へずらす
                    int startY = Math.min(centerY - 40, centerY + 14 - lines.size() * 10);
                    for (int i = 0; i < lines.size(); i++) {
                        FormattedCharSequence line = lines.get(i);
                        guiGraphics.drawString(this.font, line,
                                centerX - this.font.width(line) / 2f, startY + (i * 10), 0xFFFFFF, true);
                    }
                }
            }
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void removed() {
        super.removed();
        // GUIを閉じたらセッションを後始末する。
        // 受理済み(AI_ACTIVE)の場合、ローカルの問題キャッシュは再表示に必要なため保持し、
        // 生成中セッションのみ破棄する。未受理なら両方破棄して次回は新規生成から始める。
        // 破棄後に届く生成コールバックはAiSessionManager側のトークン検証で無視される。
        if (entity.isAiActive()) {
            AiSessionManager.abortGeneration(entity.getId());
        } else {
            AiSessionManager.cleanup(entity.getId());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
