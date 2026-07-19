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
            ServerLevel playerLevel = sender.serverLevel();

            // 改造クライアント対策: 遠隔操作を拒否 (VS2船上のシップヤード座標を考慮)
            if (!com.example.wave_motion_gun.compat.VSCompat.isWithinReach(sender, msg.pos, 64)) return;

            // 【重要】以降の処理はすべて「msg.pos に実在するトリガーユニット」を起点に行う。
            // セクション2をこのブロックの外に出すと、トリガーユニットが存在しない座標
            // (例: 攻撃者の足元。距離チェックは必ず通過する)を指定するだけで、
            // 周波数さえ合えば任意のストレージを遠隔操作できてしまう。
            if (!playerLevel.isLoaded(msg.pos)) return;
            BlockEntity be = playerLevel.getBlockEntity(msg.pos);
            if (!(be instanceof TriggerUnitBlockEntity trigger)) return;

            // 1. トリガーユニット自身の更新
            // targetFrequency を更新 (StorageModeは0-2のみ有効。lightLevelはsetter側でクランプ済み)
            trigger.setStorageData(Math.max(0, msg.targetFreq),
                    net.minecraft.util.Mth.clamp(msg.storageMode, 0, 2));
            trigger.setLightLevel(msg.lightLevel);

            if (msg.type == 1) {
                // 発射者を実績付与のために引き渡す
                trigger.fireCannonSignal(sender);
            }

            // 2. ストレージへの遠隔設定反映 (MonitoringUnit経由)
            // 検索キーには msg.targetFreq ではなく、サーバー側が保持する値を使う。
            // クライアント由来の周波数をそのまま検索キーにすると、上のsetStorageDataを
            // 経由せずに任意周波数のネットワークを引ける余地が残るため。
            // getReceivers は同一Level・ロード済みチャンクのモニターのみを返す。
            Set<MonitoringUnitBlockEntity> monitors =
                    FrequencyManager.getReceivers(playerLevel, trigger.targetFrequency);

            for (MonitoringUnitBlockEntity monitor : monitors) {
                if (monitor == null || monitor.isRemoved() || monitor.getLevel() == null) continue;

                // モニターに紐付いているストレージを取得
                WaveEnergyStorageBlockEntity storage = monitor.getNearbyStorage();
                if (storage != null) {
                    // 【修正】直接フィールド代入を廃止し、Setter経由で変更する
                    // Setter内でSE再生と状態更新(setChanged/sendBlockUpdated)が行われる
                    storage.setCircuitToWMG(msg.circuitToWMG);
                    storage.setEmergencyValves(msg.emergencyValves);
                    storage.setInletSafety(msg.inletSafety);
                    storage.setOutletSafety(msg.outletSafety);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}