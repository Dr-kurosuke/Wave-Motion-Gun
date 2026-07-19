package com.example.wave_motion_gun.init;

import com.example.wave_motion_gun.ExampleMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class CreativeTabInit {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ExampleMod.MODID);

    public static final RegistryObject<CreativeModeTab> WAVE_CANNON_TAB = TABS.register("wave_cannon_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.wave_cannon_tab"))
                    // タブのアイコンとして表示するアイテム（ここでは波動砲本体）
                    .icon(() -> new ItemStack(ItemInit.WAVE_CANNON.get()))
                    // ItemInitの宣言順と同じ並びで表示する
                    .displayItems((params, output) -> {
                        output.accept(ItemInit.WAVE_CANNON.get());
                        output.accept(ItemInit.TRIGGER_UNIT.get());
                        output.accept(ItemInit.MONITORING_UNIT.get());
                        output.accept(ItemInit.WAVE_ENERGY_STORAGE.get());
                        output.accept(ItemInit.BARREL_UNIT.get());
                        output.accept(ItemInit.SEAT_BLOCK.get());
                        output.accept(ItemInit.COMMUNICATION_BEACON.get());
                        output.accept(ItemInit.TACHYON_PARTICLE_COMPRESSOR.get());
                        output.accept(ItemInit.WAVELENGTH_ADJUSTER.get());
                        output.accept(ItemInit.WAVE_CHAMBER.get());
                        output.accept(ItemInit.COORDINATE_DATAPAD.get());
                        output.accept(ItemInit.SUPPLY_CRATE.get());
                        output.accept(ItemInit.RAW_COSMO_ORE.get());
                        output.accept(ItemInit.COSMO_BLEND.get());
                        output.accept(ItemInit.COSMO_STEEL.get());
                        output.accept(ItemInit.TACHYON_CRYSTAL.get());
                        output.accept(ItemInit.GRAVITY_LENS.get());
                        output.accept(ItemInit.QUANTUM_UNIT.get());
                        output.accept(ItemInit.WAVE_CORE.get());
                        output.accept(ItemInit.CLEARANCE_C.get());
                        output.accept(ItemInit.CLEARANCE_B.get());
                        output.accept(ItemInit.CLEARANCE_A.get());
                        output.accept(ItemInit.CLEARANCE_S.get());
                        output.accept(ItemInit.SHOCK_CANNON.get());
                        output.accept(ItemInit.SHOCK_CANNON_BARREL.get());
                        output.accept(ItemInit.SHOCK_CANNON_SHELL.get());
                        output.accept(ItemInit.TYPE_3_SHELL.get());
                        output.accept(ItemInit.WAVE_CARTRIDGE.get());
                        output.accept(ItemInit.WAVE_MANUAL.get());
                        output.accept(ItemInit.SHOCK_MANUAL.get());
                    })
                    .build());
}
