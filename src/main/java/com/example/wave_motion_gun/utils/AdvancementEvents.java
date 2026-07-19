package com.example.wave_motion_gun.utils;

import com.example.wave_motion_gun.ExampleMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 実績付与用のFORGEバスイベントハンドラ。
 * LivingDeathEventはサーバー側でのみ発火するためDist指定は不要。
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class AdvancementEvents {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // 波動砲のダメージソースでプレイヤーが死亡した場合、隠し実績を付与
        if (event.getEntityLiving() instanceof ServerPlayer player
                && event.getSource().getMsgId().equals("wave_motion_gun")) {
            AdvancementHelper.grant(player, "wave_death");
        }
    }
}
