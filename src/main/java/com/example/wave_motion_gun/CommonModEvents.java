package com.example.wave_motion_gun;

import com.example.wave_motion_gun.entity.MessengerEntity;
import com.example.wave_motion_gun.init.EntityInit;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// サーバー・クライアント共通のイベントバス
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonModEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        // ここでエンティティと属性(Attribute)を紐付けます
        event.put(EntityInit.MESSENGER.get(), MessengerEntity.createAttributes().build());
    }
}