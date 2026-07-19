package com.example.wave_motion_gun.init;

import com.example.wave_motion_gun.ExampleMod;
import com.example.wave_motion_gun.blockentity.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockEntityInit {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ExampleMod.MODID);

    public static final RegistryObject<BlockEntityType<WaveCannonBlockEntity>> WAVE_CANNON_BE =
            BLOCK_ENTITIES.register("wave_cannon_be", () ->
                    BlockEntityType.Builder.of(WaveCannonBlockEntity::new, BlockInit.WAVE_CANNON.get()).build(null));

    public static final RegistryObject<BlockEntityType<WaveEnergyStorageBlockEntity>> WAVE_ENERGY_STORAGE_BE =
            BLOCK_ENTITIES.register("wave_energy_storage_be", () ->
                    BlockEntityType.Builder.of(WaveEnergyStorageBlockEntity::new, BlockInit.WAVE_ENERGY_STORAGE.get()).build(null));

    public static final RegistryObject<BlockEntityType<MonitoringUnitBlockEntity>> MONITORING_UNIT_BE =
            BLOCK_ENTITIES.register("monitoring_unit_be", () ->
                    BlockEntityType.Builder.of(MonitoringUnitBlockEntity::new, BlockInit.MONITORING_UNIT.get()).build(null));

    public static final RegistryObject<BlockEntityType<TriggerUnitBlockEntity>> TRIGGER_UNIT_BE =
            BLOCK_ENTITIES.register("trigger_unit_be", () ->
                    BlockEntityType.Builder.of(TriggerUnitBlockEntity::new, BlockInit.TRIGGER_UNIT.get()).build(null));

    // 【追加】補給コンテナ用
    public static final RegistryObject<BlockEntityType<SupplyCrateBlockEntity>> SUPPLY_CRATE_BE =
            BLOCK_ENTITIES.register("supply_crate_be", () ->
                    BlockEntityType.Builder.of(SupplyCrateBlockEntity::new, BlockInit.SUPPLY_CRATE.get()).build(null));

    public static final RegistryObject<BlockEntityType<ShockCannonBlockEntity>> SHOCK_CANNON_BE = BLOCK_ENTITIES.register("shock_cannon_be",
            () -> BlockEntityType.Builder.of(ShockCannonBlockEntity::new, BlockInit.SHOCK_CANNON.get()).build(null));
}