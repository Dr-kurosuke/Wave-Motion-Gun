package com.example.wave_motion_gun.client.ai;

import com.example.wave_motion_gun.entity.MessengerEntity;
import com.example.wave_motion_gun.network.AiQuestionSetPacket;
import com.example.wave_motion_gun.network.PacketHandler;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * クライアント側のAI問題生成セッション管理。
 * 生成(非同期) → サーバーへ送信 → サーバー受理(AI_ACTIVE同期)待ち、の状態を追跡する。
 * 失敗・タイムアウト時は静的問題集へのフォールバックを画面側に知らせる。
 */
public class AiSessionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("wave_motion_gun_mod/AI");
    private static final long ACK_TIMEOUT_MS = 5000;

    private enum State { GENERATING, AWAITING_ACK, FAILED }

    private record Session(State state, long deadline) {}

    private static final Map<Integer, Session> SESSIONS = new HashMap<>();

    /** GUIを開いた時に呼ぶ。未生成なら非同期生成を開始する(多重起動防止付き)。 */
    public static void beginIfNeeded(MessengerEntity entity) {
        int id = entity.getId();
        if (SESSIONS.containsKey(id) || AiQuestionCache.get(id) != null) return;

        // このセッションインスタンスをトークンとして保持し、コールバック時に同一性を検証する
        Session token = new Session(State.GENERATING, 0);
        SESSIONS.put(id, token);
        int rank = entity.getRewardRank();
        String lang = Minecraft.getInstance().options.languageCode;

        AiQuestionService.generate(rank, lang).whenComplete((questions, err) ->
                Minecraft.getInstance().execute(() -> {
                    // 【修正】cleanup等で破棄済み(または別セッションに差し替え済み)なら、
                    // 遅れて届いた結果を黙って捨てる (掃除済みセッションの復活防止)
                    if (SESSIONS.get(id) != token) return;

                    if (err != null || questions == null) {
                        LOGGER.warn("AI question generation failed, falling back to static questions: {}",
                                AiQuestionValidator.rootMessage(err));
                        SESSIONS.put(id, new Session(State.FAILED, 0));
                        // 原因を推定してチャットに通知(画面表示は没入感のため詳細を出さない)
                        notifyFailure(AiErrorClassifier.classify(err));
                        return;
                    }
                    AiQuestionCache.put(id, questions);
                    PacketHandler.INSTANCE.sendToServer(new AiQuestionSetPacket(id, rank, questions));
                    SESSIONS.put(id, new Session(State.AWAITING_ACK, System.currentTimeMillis() + ACK_TIMEOUT_MS));
                }));
    }

    /** 生成中またはサーバー受理待ちならtrue(タイムアウト時はFAILEDへ遷移させる)。 */
    public static boolean isBusy(int entityId) {
        Session s = SESSIONS.get(entityId);
        if (s == null || s.state == State.FAILED) return false;
        if (s.state == State.AWAITING_ACK && System.currentTimeMillis() > s.deadline) {
            // サーバーが受理しなかった(AI禁止サーバー等) → 静的問題集へ
            SESSIONS.put(entityId, new Session(State.FAILED, 0));
            AiQuestionCache.remove(entityId);
            notifyFailure("server_rejected");
            return false;
        }
        return true;
    }

    /** 生成/受理に失敗して静的問題集へフォールバックしたか。 */
    public static boolean hasFailed(int entityId) {
        Session s = SESSIONS.get(entityId);
        return s != null && s.state == State.FAILED;
    }

    /** セッション終了時の後始末。 */
    public static void cleanup(int entityId) {
        SESSIONS.remove(entityId);
        AiQuestionCache.remove(entityId);
    }

    /**
     * 生成中/受理待ちのセッションのみ破棄する(ローカルの問題キャッシュは保持)。
     * 受理済み(AI_ACTIVE)セッションのGUIを閉じた時に使う。
     * 破棄後に届く生成コールバックはトークン検証で自動的に捨てられる。
     */
    public static void abortGeneration(int entityId) {
        SESSIONS.remove(entityId);
    }

    /** ワールド退出時に全セッションを破棄する (エンティティIDはワールド毎に振り直されるため)。 */
    public static void clearAll() {
        SESSIONS.clear();
    }

    /** 回線確立失敗の推定原因をチャット欄に通知する(標準問題へのフォールバック案内込み) */
    private static void notifyFailure(String reasonKey) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent(
                "message.wave_motion_gun_mod.ai.link_failed",
                new net.minecraft.network.chat.TranslatableComponent(
                        "message.wave_motion_gun_mod.ai.reason." + reasonKey)), false);
    }
}
