package com.example.wave_motion_gun.network;

import com.example.wave_motion_gun.blockentity.MonitoringUnitBlockEntity;
import com.example.wave_motion_gun.blockentity.WaveCannonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MonitoringUnitUpdatePacket {
    private final BlockPos pos;
    private final int frequency;
    private final int range;
    private final int radius;
    private final int damage;
    private final boolean isDestructive;
    private final int innerColor;
    private final int outerColor;
    // --- 【追加】 ---
    private final boolean isSafetyDisabled;

    public MonitoringUnitUpdatePacket(BlockPos pos, int frequency, int range, int radius, int damage, boolean isDestructive, int innerColor, int outerColor, boolean isSafetyDisabled) {
        this.pos = pos;
        this.frequency = frequency;
        this.range = range;
        this.radius = radius;
        this.damage = damage;
        this.isDestructive = isDestructive;
        this.innerColor = innerColor;
        this.outerColor = outerColor;
        this.isSafetyDisabled = isSafetyDisabled;
    }

    public static void encode(MonitoringUnitUpdatePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.frequency);
        buf.writeInt(msg.range);
        buf.writeInt(msg.radius);
        buf.writeInt(msg.damage);
        buf.writeBoolean(msg.isDestructive);
        buf.writeInt(msg.innerColor);
        buf.writeInt(msg.outerColor);
        // --- 【追加】 ---
        buf.writeBoolean(msg.isSafetyDisabled);
    }

    public static MonitoringUnitUpdatePacket decode(FriendlyByteBuf buf) {
        return new MonitoringUnitUpdatePacket(
                buf.readBlockPos(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readInt(),
                buf.readInt(),
                // --- 【追加】 ---
                buf.readBoolean()
        );
    }

    public static void handle(MonitoringUnitUpdatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level.isLoaded(msg.pos)) {
                // 改造クライアント対策: 遠隔操作を拒否
                if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(msg.pos)) > 64 * 64) return;
                BlockEntity be = player.level.getBlockEntity(msg.pos);
                if (be instanceof MonitoringUnitBlockEntity monitor) {
                    monitor.setFrequency(msg.frequency);

                    // 改造クライアント対策: サーバー権威の上限(構造由来)でクランプする
                    monitor.damageValue = net.minecraft.util.Mth.clamp(msg.damage, 0, monitor.getMaxDamageLimit());
                    monitor.isDestructive = msg.isDestructive;
                    monitor.innerColor = msg.innerColor & 0xFFFFFF;
                    monitor.outerColor = msg.outerColor & 0xFFFFFF;
                    // --- 【追加】更新 ---
                    monitor.isSafetyDisabled = msg.isSafetyDisabled;

                    monitor.setChanged();
                    monitor.getLevel().sendBlockUpdated(monitor.getBlockPos(), monitor.getBlockState(), monitor.getBlockState(), 3);

                    WaveCannonBlockEntity cannon = monitor.getNearbyCannon();
                    if (cannon != null) {
                        cannon.currentRange = net.minecraft.util.Mth.clamp(msg.range, 1, monitor.getRangeLimit());
                        cannon.currentRadius = net.minecraft.util.Mth.clamp(msg.radius, 1, monitor.getRadiusLimit());
                        cannon.setChanged();
                        cannon.getLevel().sendBlockUpdated(cannon.getBlockPos(), cannon.getBlockState(), cannon.getBlockState(), 3);

                        player.displayClientMessage(new TextComponent("§aSettings Updated."), true);
                    } else {
                        player.displayClientMessage(new TextComponent("§aMonitor Settings Updated (Cannon Not Found)"), true);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}