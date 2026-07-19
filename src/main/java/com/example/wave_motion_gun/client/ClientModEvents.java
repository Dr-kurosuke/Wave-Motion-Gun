package com.example.wave_motion_gun.client;

import com.example.wave_motion_gun.ExampleMod;
import com.example.wave_motion_gun.client.renderer.*;
import com.example.wave_motion_gun.init.BlockEntityInit;
import com.example.wave_motion_gun.init.EntityInit;
import com.example.wave_motion_gun.init.MenuInit;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers; // 追加
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // SeatEntity
        event.registerEntityRenderer(EntityInit.SEAT.get(), SeatRenderer::new);

        // MessengerEntity (Lady)
        event.registerEntityRenderer(EntityInit.MESSENGER.get(), MessengerRenderer::new);

        // Wave Cannon Projectiles
        event.registerEntityRenderer(EntityInit.WAVE_ENERGY_BALL.get(), WaveEnergyBallRenderer::new);
        event.registerEntityRenderer(EntityInit.WAVE_BEAM_SEGMENT.get(), WaveBeamSegmentRenderer::new);

        // 【追加】ショックカノンのレンダラー登録
        event.registerEntityRenderer(EntityInit.SHOCK_CANNON_BEAM.get(), ShockCannonBeamRenderer::new);
        event.registerEntityRenderer(EntityInit.SHOCK_CANNON_BEAM_SEGMENT.get(), ShockCannonBeamSegmentRenderer::new);

        // ClientModEvents.java の registerRenderers 内に追加

        EntityRenderers.register(EntityInit.TYPE_3_SHELL_ENTITY.get(), Type3ShellRenderer::new);
        EntityRenderers.register(EntityInit.WAVE_CARTRIDGE_ENTITY.get(), WaveCartridgeRenderer::new);

        // Supply Meteor
        event.registerEntityRenderer(EntityInit.SUPPLY_METEOR.get(), SupplyMeteorRenderer::new);

        // --- 【追加】Wave Cannon BlockEntity Renderer ---
        event.registerBlockEntityRenderer(BlockEntityInit.WAVE_CANNON_BE.get(), WaveCannonRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 各種メニュー画面(GUI)の登録
            // これらはクライアント側でのみ実行される必要があります

            // Supply Crate Screen
            MenuScreens.register(MenuInit.SUPPLY_CRATE_MENU.get(), SupplyCrateScreen::new);

            // Monitoring Unit Screen
            MenuScreens.register(MenuInit.MONITORING_UNIT_MENU.get(), MonitoringUnitScreen::new);

            // Trigger Unit Screen
            MenuScreens.register(MenuInit.TRIGGER_UNIT_MENU.get(), TriggerUnitScreen::new);


            MenuScreens.register(MenuInit.SHOCK_CANNON_MENU.get(), ShockCannonScreen::new);
        });
    }
}