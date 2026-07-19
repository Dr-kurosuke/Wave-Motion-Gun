package com.example.wave_motion_gun.utils;

import com.example.wave_motion_gun.blockentity.MonitoringUnitBlockEntity;
import com.example.wave_motion_gun.blockentity.TriggerUnitBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
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

    /**
     * 周波数の上限。ContainerData は ClientboundContainerSetDataPacket の writeShort で
     * 同期されるため、16bit に収まらない値はクライアント側で負値に化ける。
     */
    public static final int MAX_FREQUENCY = Short.MAX_VALUE;
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

    /** originと同一Levelのトリガーユニットにのみオーバーヒートを伝える */
    public static void feedbackOverheat(Level origin, int freq) {
        Set<TriggerUnitBlockEntity> set = triggers.get(freq);
        if (set != null) {
            for (TriggerUnitBlockEntity trigger : new HashSet<>(set)) {
                if (trigger != null && !trigger.isRemoved() && trigger.hasLevel()
                        && isSameLevelAndLoaded(trigger, origin)) {
                    trigger.setStorageMode(0); // 0 = EXHAUST
                }
            }
        }
    }

    // --- 信号送信 ---
    public static void sendSignal(Level origin, int freq, int type) {
        sendSignal(origin, freq, type, null);
    }

    /** 発射者(実績付与用)を指定して信号を送る。firerはnull可 */
    public static void sendSignal(Level origin, int freq, int type, @Nullable ServerPlayer firer) {
        // getReceiversは未登録ならemptySetを返すため、事前のcontainsKeyチェックは不要
        // 受信側のリストをコピーして反復処理
        for (MonitoringUnitBlockEntity be : new HashSet<>(getReceivers(origin, freq))) {
            // getReceiversで厳密なチェック済みのため、ここでは基本的なチェックのみで良いが念のため
            if (be != null && !be.isRemoved() && be.hasLevel()) {
                be.activate(type, firer);
            }
        }
    }

    public static void sendSignal(Level origin, int freq) {
        sendSignal(origin, freq, 1);
    }

    /**
     * BEがoriginと同一のLevelに属し、かつそのチャンクがロード済みかを判定する。
     *
     * Levelインスタンスの同一性(==)で比較しているのは意図的で、次元跨ぎを防ぐと同時に
     * クライアント側Levelとサーバー側Levelの取り違えも同時に弾くため
     * (dimension()の比較では両者が同一視されてしまう)。
     */
    private static boolean isSameLevelAndLoaded(BlockEntity be, @Nullable Level origin) {
        if (origin == null) return true; // 呼び出し元がLevelを特定できない場合は従来通り
        Level beLevel = be.getLevel();
        if (beLevel != origin) return false;
        // 未ロードチャンクへのアクセスは強制ロードを誘発するため対象外にする
        return beLevel.isLoaded(be.getBlockPos());
    }

    /**
     * 指定周波数の受信機セットを取得し、同時に無効なインスタンス（幽霊BE）を削除する。
     *
     * originを渡すと、同一Level かつ ロード済みチャンク のBEのみに絞り込む。
     * これが無いと、周波数さえ合えば別次元・未ロードチャンクのユニットまで
     * 遠隔操作できてしまう(未ロードチャンクの強制ロードにも繋がる)。
     */
    public static Set<MonitoringUnitBlockEntity> getReceivers(@Nullable Level origin, int freq) {
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

        // 【必ずコピーを返すこと】内部セットの実体を返すとCMEになる。
        // 呼び出し元が反復中に activate() 等を呼ぶと、その先の getBlockEntity() が
        // 未ロードチャンクを同期ロードし、そこに MonitoringUnit があれば
        // onLoad→register でこのセットが構造変更されるため。
        if (origin == null) return new HashSet<>(set);

        // 同一Level かつ ロード済み のものだけを新しいSetにして返す
        Set<MonitoringUnitBlockEntity> scoped = new HashSet<>();
        for (MonitoringUnitBlockEntity be : set) {
            if (isSameLevelAndLoaded(be, origin)) scoped.add(be);
        }
        return scoped;
    }

    private static void cleanUp(int freq) {
        // 幽霊BEの掃除が目的なのでLevel絞り込みはしない(null渡し)
        getReceivers(null, freq); // getReceivers内部でremoveIfが走る (未登録ならemptySetが返るだけ)
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        receivers.clear();
        triggers.clear();
    }
}