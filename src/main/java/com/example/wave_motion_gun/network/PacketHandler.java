package com.example.wave_motion_gun.network;

import com.example.wave_motion_gun.ExampleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ExampleMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        // 注意: 新パケットは必ず末尾に追加すること (ID順序維持のため)
        int id = 0;

        // TriggerUpdatePacket (C→S)
        INSTANCE.registerMessage(id++, TriggerUpdatePacket.class, TriggerUpdatePacket::encode, TriggerUpdatePacket::decode, TriggerUpdatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // MessengerResponsePacket (C→S)
        INSTANCE.registerMessage(id++,
                MessengerResponsePacket.class,
                MessengerResponsePacket::toBytes,
                MessengerResponsePacket::new,
                MessengerResponsePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // MonitoringUnitUpdatePacket (C→S)
        INSTANCE.registerMessage(id++,
                MonitoringUnitUpdatePacket.class,
                MonitoringUnitUpdatePacket::encode,
                MonitoringUnitUpdatePacket::decode,
                MonitoringUnitUpdatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // ■ 修正: ShockCannonUpdatePacketの登録 (registerMessageを使用) (C→S)
        INSTANCE.registerMessage(id++,
                ShockCannonUpdatePacket.class,
                ShockCannonUpdatePacket::toBytes,
                ShockCannonUpdatePacket::new,
                ShockCannonUpdatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // ShockCannonFirePacket (C→S)
        INSTANCE.registerMessage(id++,
                ShockCannonFirePacket.class,
                ShockCannonFirePacket::toBytes,
                ShockCannonFirePacket::new,
                ShockCannonFirePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // AI生成問題セットの登録 (C→S)
        INSTANCE.registerMessage(id++,
                AiQuestionSetPacket.class,
                AiQuestionSetPacket::toBytes,
                AiQuestionSetPacket::new,
                AiQuestionSetPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}