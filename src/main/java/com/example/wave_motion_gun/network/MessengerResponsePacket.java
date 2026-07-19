package com.example.wave_motion_gun.network;

import com.example.wave_motion_gun.entity.MessengerEntity;
import com.example.wave_motion_gun.utils.QuestionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MessengerResponsePacket {
    private final int entityId;
    private final int answerIndex;

    public MessengerResponsePacket(int entityId, int answerIndex) {
        this.entityId = entityId;
        this.answerIndex = answerIndex;
    }

    // デコード用コンストラクタ
    public MessengerResponsePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.answerIndex = buf.readInt();
    }

    // エンコード用メソッド
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(answerIndex);
    }

    public static void handle(MessengerResponsePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // パケットに含まれるIDからエンティティを取得
                Entity target = player.level.getEntity(msg.entityId);

                // AiQuestionSetPacketと同様の検証: 未認証・消滅済み・遠隔(32ブロック超)からの回答を拒否
                // (他プレイヤーのレディを遠隔で誤答→消滅させるグリーフィング対策)
                if (target instanceof MessengerEntity messenger
                        && messenger.isActive()
                        && !messenger.isRemoved()
                        && player.distanceToSqr(messenger) <= 32 * 32) {
                    messenger.processAnswer(player, msg.answerIndex);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}