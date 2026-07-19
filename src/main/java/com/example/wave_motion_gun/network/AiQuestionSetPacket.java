package com.example.wave_motion_gun.network;

import com.example.wave_motion_gun.config.AiServerConfig;
import com.example.wave_motion_gun.entity.MessengerEntity;
import com.example.wave_motion_gun.utils.QuestionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * クライアントがGeminiで生成した問題セットをサーバーへ登録するパケット(C→S)。
 * 判定はサーバー権威: サーバーが正解インデックスを保持し、回答時に照合する。
 * 生成内容は監査のためサーバーログに全文記録される。
 */
public class AiQuestionSetPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger("wave_motion_gun_mod/AI");

    private static final int MAX_QUESTIONS = 8;
    private static final int MAX_TEXT_LENGTH = 300;
    private static final int MAX_OPTION_LENGTH = 100;

    // 【追加】拒否パスのログ洪水対策: プレイヤーごとに3秒未満の連続試行は黙って捨てる
    private static final long ATTEMPT_COOLDOWN_MS = 3000;
    private static final Map<UUID, Long> lastAttempt = new ConcurrentHashMap<>();

    private final int entityId;
    private final int rankValue;
    private final List<QuestionManager.Question> questions;

    public AiQuestionSetPacket(int entityId, int rankValue, List<QuestionManager.Question> questions) {
        this.entityId = entityId;
        this.rankValue = rankValue;
        this.questions = questions;
    }

    public AiQuestionSetPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.rankValue = buf.readInt();
        int count = Math.min(buf.readInt(), MAX_QUESTIONS);
        this.questions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String text = buf.readUtf(MAX_TEXT_LENGTH * 4);
            int optCount = Math.min(buf.readInt(), 4);
            String[] options = new String[Math.max(0, optCount)];
            for (int j = 0; j < options.length; j++) {
                options[j] = buf.readUtf(MAX_OPTION_LENGTH * 4);
            }
            int correct = buf.readInt();
            this.questions.add(new QuestionManager.Question(text, options, correct));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(rankValue);
        buf.writeInt(questions.size());
        for (QuestionManager.Question q : questions) {
            buf.writeUtf(q.text, MAX_TEXT_LENGTH * 4);
            buf.writeInt(q.options.length);
            for (String opt : q.options) {
                buf.writeUtf(opt, MAX_OPTION_LENGTH * 4);
            }
            buf.writeInt(q.correctIndex);
        }
    }

    public static void handle(AiQuestionSetPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 0. クールダウン: 前回試行から3秒未満の再送は黙って捨てる (拒否ログの洪水防止)
            long now = System.currentTimeMillis();
            Long last = lastAttempt.put(player.getUUID(), now);
            if (last != null && now - last < ATTEMPT_COOLDOWN_MS) {
                return;
            }

            // 1. サーバーポリシー: AI問題の受け入れが許可されているか
            if (!AiServerConfig.ALLOW_AI_QUESTIONS.get()) {
                LOGGER.info("Rejected AI question set from {} (AI questions disabled on this server)", player.getName().getString());
                return;
            }

            // 2. セッション検証: 近くに認証済みのレディが存在するか
            Entity target = player.level().getEntity(msg.entityId);
            if (!(target instanceof MessengerEntity messenger)) return;
            if (!messenger.isActive() || messenger.isRemoved()) return;
            if (player.distanceToSqr(messenger) > 32 * 32) return;

            // 3. ランク検証: 認証時に確定したランクと一致するか
            if (msg.rankValue != messenger.getRewardRank()) {
                LOGGER.info("Rejected AI question set from {} (rank mismatch: sent {}, session {})",
                        player.getName().getString(), msg.rankValue, messenger.getRewardRank());
                return;
            }

            // 4. 内容検証: 問題数・選択肢数・文字数・正解インデックス
            if (msg.questions.size() != 5) return;
            for (QuestionManager.Question q : msg.questions) {
                if (q.text == null || q.text.isBlank() || q.text.length() > MAX_TEXT_LENGTH) return;
                if (q.options.length < 2 || q.options.length > 4) return;
                if (q.correctIndex < 0 || q.correctIndex >= q.options.length) return;
                for (String opt : q.options) {
                    if (opt == null || opt.isBlank() || opt.length() > MAX_OPTION_LENGTH) return;
                }
            }

            // 5. 受理: エンティティに保持させ、監査ログに全文記録
            if (!messenger.acceptAiQuestions(msg.questions)) {
                LOGGER.info("Rejected AI question set from {} (session already has an AI set)", player.getName().getString());
                return;
            }
            LOGGER.info("Accepted AI question set from {} (rank {}):", player.getName().getString(), msg.rankValue);
            for (int i = 0; i < msg.questions.size(); i++) {
                QuestionManager.Question q = msg.questions.get(i);
                LOGGER.info("  Q{}: {} | options={} | correct={}",
                        i + 1, q.text.replace("\n", " / "), String.join(" ; ", q.options), q.correctIndex);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
