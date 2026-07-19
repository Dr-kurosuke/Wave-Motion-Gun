package com.example.wave_motion_gun.utils;

import com.example.wave_motion_gun.blockentity.MonitoringUnitBlockEntity;
import com.example.wave_motion_gun.blockentity.TriggerUnitBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber
public class FrequencyManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Integer, Set<MonitoringUnitBlockEntity>> receivers = new HashMap<>();
    private static final Map<Integer, Set<TriggerUnitBlockEntity>> triggers = new HashMap<>();

    // --- MonitoringUnit用 ---
    public static void register(int freq, MonitoringUnitBlockEntity be) {
        // 登録時に既存の無効なエントリを掃除する
        cleanUp(freq);
        receivers.computeIfAbsent(freq, k -> new HashSet<>()).add(be);
        LOGGER.debug("[FREQ-MGR] Registered MonitoringUnit at {} Freq: {}", be.getBlockPos(), freq);
    }

    public static void unregister(int freq, MonitoringUnitBlockEntity be) {
        Set<MonitoringUnitBlockEntity> set = receivers.get(freq);
        if (set != null) {
            set.remove(be);
            if (set.isEmpty()) {
                receivers.remove(freq);
            }
        }
    }

    public static void updateFrequency(int oldFreq, int newFreq, MonitoringUnitBlockEntity be) {
        unregister(oldFreq, be);
        register(newFreq, be);
    }

    // --- TriggerUnit用 ---
    public static void registerTrigger(int freq, TriggerUnitBlockEntity be) {
        triggers.computeIfAbsent(freq, k -> new HashSet<>()).add(be);
    }

    public static void unregisterTrigger(int freq, TriggerUnitBlockEntity be) {
        Set<TriggerUnitBlockEntity> set = triggers.get(freq);
        if (set != null) {
            set.remove(be);
            if (set.isEmpty()) {
                triggers.remove(freq);
            }
        }
    }

    public static void updateTriggerFrequency(int oldFreq, int newFreq, TriggerUnitBlockEntity be) {
        unregisterTrigger(oldFreq, be);
        registerTrigger(newFreq, be);
    }

    public static void feedbackOverheat(int freq) {
        Set<TriggerUnitBlockEntity> set = triggers.get(freq);
        if (set != null) {
            for (TriggerUnitBlockEntity trigger : new HashSet<>(set)) {
                if (trigger != null && !trigger.isRemoved() && trigger.hasLevel()) {
                    trigger.setStorageMode(0); // 0 = EXHAUST
                }
            }
        }
    }

    // --- 信号送信 ---
    public static void sendSignal(int freq, int type) {
        sendSignal(freq, type, null);
    }

    /** 発射者(実績付与用)を指定して信号を送る。firerはnull可 */
    public static void sendSignal(int freq, int type, @Nullable ServerPlayer firer) {
        // getReceiversは未登録ならemptySetを返すため、事前のcontainsKeyチェックは不要
        // 受信側のリストをコピーして反復処理
        for (MonitoringUnitBlockEntity be : new HashSet<>(getReceivers(freq))) {
            // getReceiversで厳密なチェック済みのため、ここでは基本的なチェックのみで良いが念のため
            if (be != null && !be.isRemoved() && be.hasLevel()) {
                be.activate(type, firer);
            }
        }
    }

    public static void sendSignal(int freq) {
        sendSignal(freq, 1);
    }

    /**
     * 指定周波数の受信機セットを取得し、同時に無効なインスタンス（幽霊BE）を削除する
     */
    public static Set<MonitoringUnitBlockEntity> getReceivers(int freq) {
        Set<MonitoringUnitBlockEntity> set = receivers.get(freq);
        if (set == null) return Collections.emptySet();

        // 厳密なバリデーション：リスト内のBEが「現在のワールドのその座標にあるBE」と一致するか確認
        set.removeIf(e -> {
            if (e == null || e.isRemoved() || !e.hasLevel()) return true;

            // チャンクがロードされている場合、ワールド内のBEを確認する
            if (e.getLevel().isLoaded(e.getBlockPos())) {
                BlockEntity currentBe = e.getLevel().getBlockEntity(e.getBlockPos());
                // インスタンス不一致なら、それは古い幽霊データ
                return currentBe != e;
            }
            // チャンク未ロードの場合は、一旦保留する（削除しない）が、
            // マルチプレイでの不安定さを防ぐため、基本的にはロード済みでないと通信対象にしない運用とする
            return false;
        });

        if (set.isEmpty()) {
            receivers.remove(freq);
            return Collections.emptySet();
        }
        return set;
    }

    private static void cleanUp(int freq) {
        getReceivers(freq); // getReceivers内部でremoveIfが走る (未登録ならemptySetが返るだけ)
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        receivers.clear();
        triggers.clear();
    }
}