package com.example.wave_motion_gun.network;

import com.example.wave_motion_gun.blockentity.MonitoringUnitBlockEntity;
import com.example.wave_motion_gun.blockentity.TriggerUnitBlockEntity;
import com.example.wave_motion_gun.blockentity.WaveEnergyStorageBlockEntity;
import com.example.wave_motion_gun.utils.FrequencyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.Set;
import java.util.function.Supplier;

public class TriggerUpdatePacket {
    private final BlockPos pos;
    private final int type;
    private final int targetFreq;
    private final int storageMode;

    private final boolean circuitToWMG;
    private final boolean emergencyValves;
    private final boolean inletSafety;
    private final boolean outletSafety;

    private final int lightLevel;

    public TriggerUpdatePacket(BlockPos pos, int type, int targetFreq, int storageMode,
                               boolean circuitToWMG, boolean emergencyValves, boolean inletSafety, boolean outletSafety, int lightLevel) {
        this.pos = pos;
        this.type = type;
        this.targetFreq = targetFreq;
        this.storageMode = storageMode;
        this.circuitToWMG = circuitToWMG;
        this.emergencyValves = emergencyValves;
        this.inletSafety = inletSafety;
        this.outletSafety = outletSafety;
        this.lightLevel = lightLevel;
    }

    public static void encode(TriggerUpdatePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.type);
        buf.writeInt(msg.targetFreq);
        buf.writeInt(msg.storageMode);
        buf.writeBoolean(msg.circuitToWMG);
        buf.writeBoolean(msg.emergencyValves);
        buf.writeBoolean(msg.inletSafety);
        buf.writeBoolean(msg.outletSafety);
        buf.writeInt(msg.lightLevel);
    }

    public static TriggerUpdatePacket decode(FriendlyByteBuf buf) {
        return new TriggerUpdatePacket(
                buf.readBlockPos(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readInt()
        );
    }

    public static void handle(TriggerUpdatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraft.server.level.ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return; // 処理前に切断された場合
            ServerLevel playerLevel = sender.getLevel();

            // 改造クライアント対策: 遠隔操作を拒否
            if (sender.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(msg.pos)) > 64 * 64) return;

            // 1. トリガーユニット自身の更新
            if (playerLevel.isLoaded(msg.pos)) {
                BlockEntity be = playerLevel.getBlockEntity(msg.pos);
                if (be instanceof TriggerUnitBlockEntity trigger) {
                    // targetFrequency を更新 (StorageModeは0-2のみ有効。lightLevelはsetter側でクランプ済み)
                    trigger.setStorageData(net.minecraft.util.Mth.clamp(msg.targetFreq, 0, FrequencyManager.MAX_FREQUENCY),
                            net.minecraft.util.Mth.clamp(msg.storageMode, 0, 2));
                    trigger.setLightLevel(msg.lightLevel);

                    if (msg.type == 1) {
                        // 発射者を実績付与のために引き渡す
                        trigger.fireCannonSignal(sender);
                    }

                    // 2. ストレージへの遠隔設定反映 (MonitoringUnit経由)
                    // 【重要】必ず実在するトリガーユニット経由でのみ行う。
                    // msg.targetFreq を直接使うと、ブロックを一つも持たないクライアントが
                    // 全次元の任意周波数のストレージを遠隔操作できてしまう。
                    applyStorageSettings(msg, trigger.targetFrequency, playerLevel);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * トリガーユニットに紐付く周波数のモニタリングユニット経由で、ストレージ設定を反映する。
     *
     * 周波数マップはプロセス全体で共有されるため、送信者と同一次元・ロード済みチャンクの
     * モニターのみを対象とする必要がある。その絞り込みは getReceivers(Level, int) が行う
     * (発射信号など他の経路でも同じ保護が要るため、FrequencyManager 側に集約している)。
     */
    private static void applyStorageSettings(TriggerUpdatePacket msg, int freq, ServerLevel playerLevel) {
        Set<MonitoringUnitBlockEntity> monitors = FrequencyManager.getReceivers(playerLevel, freq);
        if (monitors == null) return;

        for (MonitoringUnitBlockEntity monitor : monitors) {
            if (monitor == null || monitor.isRemoved() || monitor.getLevel() == null) continue;

            // モニターに紐付いているストレージを取得
            WaveEnergyStorageBlockEntity storage = monitor.getNearbyStorage();
            if (storage != null) {
                // 直接フィールド代入を廃止し、Setter経由で変更する
                // Setter内でSE再生と状態更新(setChanged/sendBlockUpdated)が行われる
                storage.setCircuitToWMG(msg.circuitToWMG);
                storage.setEmergencyValves(msg.emergencyValves);
                storage.setInletSafety(msg.inletSafety);
                storage.setOutletSafety(msg.outletSafety);
            }
        }
    }
}