package com.example.wave_motion_gun.network;

import com.example.wave_motion_gun.blockentity.ShockCannonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ShockCannonFirePacket {
    private final BlockPos pos;

    public ShockCannonFirePacket(BlockPos pos) {
        this.pos = pos;
    }

    public ShockCannonFirePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // 改造クライアント対策: 未ロード座標への強制チャンクロードと遠隔発射を拒否
                if (!player.level().isLoaded(pos)) return;
                // (VS2船上のシップヤード座標を考慮)
                if (!com.example.wave_motion_gun.compat.VSCompat.isWithinReach(player, pos, 64)) return;
                BlockEntity be = player.level().getBlockEntity(pos);
                if (be instanceof ShockCannonBlockEntity cannon) {
                    cannon.attemptFire(player); // サーバー側で発射処理を実行 (発射者は実績付与に使用)
                }
            }
        });
        return true;
    }
}