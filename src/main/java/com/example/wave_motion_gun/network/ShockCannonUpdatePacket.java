package com.example.wave_motion_gun.network;

import com.example.wave_motion_gun.blockentity.ShockCannonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ShockCannonUpdatePacket {
    private final BlockPos pos;
    private final int type;  // 0: ColorMode, 1: FireDelay
    private final int value; // 設定値

    public ShockCannonUpdatePacket(BlockPos pos, int type, int value) {
        this.pos = pos;
        this.type = type;
        this.value = value;
    }

    // 互換性のため古いコンストラクタを一応残す（今回はすべて書き換えるので無くても良いが）
    public ShockCannonUpdatePacket(BlockPos pos, int colorMode) {
        this(pos, 0, colorMode);
    }

    public ShockCannonUpdatePacket(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.type = buffer.readInt();
        this.value = buffer.readInt();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeInt(type);
        buffer.writeInt(value);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();
                if (level.isLoaded(pos)) {
                    // 改造クライアント対策: 遠隔操作を拒否 (VS2船上のシップヤード座標を考慮)
                    if (!com.example.wave_motion_gun.compat.VSCompat.isWithinReach(player, pos, 64)) return;
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof ShockCannonBlockEntity cannon) {
                        if (type == 0) {
                            // colorModeは0-2のみ有効
                            cannon.setColorMode(net.minecraft.util.Mth.clamp(this.value, 0, 2));
                        } else if (type == 1) {
                            // 遅延は最大1分(1200tick)までに制限
                            cannon.setFireDelay(net.minecraft.util.Mth.clamp(this.value, 0, 1200));
                        }
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}