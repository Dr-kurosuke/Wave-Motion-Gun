package com.example.wave_motion_gun;

import com.example.wave_motion_gun.world.data.SupplyDropManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        // 【重要】Tickの終了時(END)かつサーバー側(SERVER)でのみ実行する
        // Phase.STARTで実行すると、他のワールド処理と競合して負荷がスパイクする原因になります。
        if (event.phase == TickEvent.Phase.END && event.side.isServer()) {
            if (event.level instanceof ServerLevel serverLevel) {
                // SupplyDropManagerの更新
                // ※SupplyDropManager側で「イベントがなければ即return」するようになっているため、
                //   ここでの呼び出し自体は軽量です。
                SupplyDropManager.get(serverLevel).tick(serverLevel);
            }
        }
    }
}