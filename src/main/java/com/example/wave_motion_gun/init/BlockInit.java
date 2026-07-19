package com.example.wave_motion_gun.init;

import com.example.wave_motion_gun.ExampleMod;
import com.example.wave_motion_gun.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockInit {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ExampleMod.MODID);

    public static final RegistryObject<Block> WAVE_CANNON = BLOCKS.register("wave_cannon",
            () -> new WaveCannonBlock(BlockBehaviour.Properties.of(Material.METAL).strength(2.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistryObject<Block> WAVE_ENERGY_STORAGE = BLOCKS.register("wave_energy_storage",
            () -> new WaveEnergyStorageBlock(BlockBehaviour.Properties.of(Material.METAL).strength(2.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistryObject<Block> MONITORING_UNIT = BLOCKS.register("monitoring_unit",
            () -> new MonitoringUnitBlock(BlockBehaviour.Properties.of(Material.METAL).strength(2.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistryObject<Block> BARREL_UNIT = BLOCKS.register("barrel_unit",
            () -> new BarrelUnitBlock(BlockBehaviour.Properties.of(Material.METAL).strength(2.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistryObject<Block> TRIGGER_UNIT = BLOCKS.register("trigger_unit",
            () -> new TriggerUnitBlock(BlockBehaviour.Properties.of(Material.METAL).strength(2.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistryObject<Block> DECORATION_COVER = BLOCKS.register("decoration_cover",
            () -> new DecorationCoverBlock(BlockBehaviour.Properties.of(Material.METAL)
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
            ));

    public static final RegistryObject<Block> SEAT_BLOCK = BLOCKS.register("seat_block",
            () -> new SeatBlock(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
            ));

    public static final RegistryObject<Block> COMMUNICATION_BEACON = BLOCKS.register("communication_beacon",
            () -> new CommunicationBeaconBlock(
                    BlockBehaviour.Properties.of(Material.METAL)
                            .strength(5.0f)
                            .sound(SoundType.METAL)
                            .lightLevel(state -> 15)
                            .noOcclusion()
            ));

    public static final RegistryObject<Block> TACHYON_PARTICLE_COMPRESSOR = BLOCKS.register("tachyon_particle_compressor",
            () -> new WaveMachineBlock(BlockBehaviour.Properties.of(Material.METAL)
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Block> WAVELENGTH_ADJUSTER = BLOCKS.register("wavelength_adjuster",
            () -> new WaveMachineBlock(BlockBehaviour.Properties.of(Material.METAL)
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Block> WAVE_CHAMBER = BLOCKS.register("wave_chamber",
            () -> new WaveMachineBlock(BlockBehaviour.Properties.of(Material.METAL)
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    // 【追加】補給物資コンテナ (Supply Crate)
    // 落下時の爆発に耐えられるよう、ある程度の硬さを持たせる
    public static final RegistryObject<Block> SUPPLY_CRATE = BLOCKS.register("supply_crate",
            () -> new SupplyCrateBlock(BlockBehaviour.Properties.of(Material.METAL)
                    .strength(5.0f, 1200.0f) // 黒曜石並みの硬さと爆発耐性
                    .sound(SoundType.NETHERITE_BLOCK)
                    .lightLevel(state -> 12)
                    .noOcclusion()
            ));


    // 衝撃砲 台座 (硬さは波動砲の砲身に合わせている)
    public static final RegistryObject<Block> SHOCK_CANNON = BLOCKS.register("shock_cannon",
            () -> new ShockCannonBlock(BlockBehaviour.Properties.of(Material.METAL)
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    // 衝撃砲 砲身 (硬さは波動砲の砲身に合わせている)
    public static final RegistryObject<Block> SHOCK_CANNON_BARREL = BLOCKS.register("shock_cannon_barrel",
            () -> new ShockCannonBarrelBlock(BlockBehaviour.Properties.of(Material.METAL)
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

}