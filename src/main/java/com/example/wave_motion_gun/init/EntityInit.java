package com.example.wave_motion_gun.init;

import com.example.wave_motion_gun.ExampleMod;
import com.example.wave_motion_gun.entity.*; // 一括インポートに変更
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class EntityInit {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ExampleMod.MODID);

    public static final RegistryObject<EntityType<CameraEntity>> CAMERA = ENTITIES.register("camera",
            () -> EntityType.Builder.<CameraEntity>of(CameraEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .noSave()
                    .build("camera"));

    public static final RegistryObject<EntityType<SeatEntity>> SEAT = ENTITIES.register("seat",
            () -> EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
                    .sized(0.0F, 0.0F)
                    .build(new ResourceLocation(ExampleMod.MODID, "seat").toString()));

    public static final RegistryObject<EntityType<MessengerEntity>> MESSENGER = ENTITIES.register("messenger",
            () -> EntityType.Builder.<MessengerEntity>of(MessengerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(ExampleMod.MODID, "messenger").toString()));

    public static final RegistryObject<EntityType<WaveEnergyBall>> WAVE_ENERGY_BALL = ENTITIES.register("wave_energy_ball",
            () -> EntityType.Builder.<WaveEnergyBall>of(WaveEnergyBall::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(128)
                    .updateInterval(1)
                    .build(new ResourceLocation(ExampleMod.MODID, "wave_energy_ball").toString()));

    public static final RegistryObject<EntityType<WaveBeamSegment>> WAVE_BEAM_SEGMENT = ENTITIES.register("wave_beam_segment",
            () -> EntityType.Builder.<WaveBeamSegment>of(WaveBeamSegment::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(128)
                    .updateInterval(10)
                    .build(new ResourceLocation(ExampleMod.MODID, "wave_beam_segment").toString()));

    // 【追加】補給物資隕石（サプライメテオ）
    // 上空から高速で飛来し、遠くからでも視認できるように設定
    public static final RegistryObject<EntityType<SupplyMeteorEntity>> SUPPLY_METEOR = ENTITIES.register("supply_meteor",
            () -> EntityType.Builder.<SupplyMeteorEntity>of(SupplyMeteorEntity::new, MobCategory.MISC)
                    .sized(1.5F, 1.5F)        // 視認しやすいサイズ
                    .clientTrackingRange(128) // 描画距離を確保
                    .updateInterval(1)        // 滑らかな動きのために1tick更新
                    .fireImmune()             // 炎のエフェクトを纏うため耐性をつける
                    .build(new ResourceLocation(ExampleMod.MODID, "supply_meteor").toString()));

    // 【追加】ショックカノン用エンティティ
    public static final RegistryObject<EntityType<ShockCannonBeam>> SHOCK_CANNON_BEAM = ENTITIES.register("shock_cannon_beam",
            () -> EntityType.Builder.<ShockCannonBeam>of(ShockCannonBeam::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F) // 波動砲より少し細くする
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(new ResourceLocation(ExampleMod.MODID, "shock_cannon_beam").toString()));

    public static final RegistryObject<EntityType<ShockCannonBeamSegment>> SHOCK_CANNON_BEAM_SEGMENT = ENTITIES.register("shock_cannon_beam_segment",
            () -> EntityType.Builder.<ShockCannonBeamSegment>of(ShockCannonBeamSegment::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(64)
                    .updateInterval(10)
                    .build(new ResourceLocation(ExampleMod.MODID, "shock_cannon_beam_segment").toString()));

    // 三式弾エンティティ
    public static final RegistryObject<EntityType<Type3ShellEntity>> TYPE_3_SHELL_ENTITY = ENTITIES.register("type_3_shell_entity",
            () -> EntityType.Builder.<Type3ShellEntity>of(Type3ShellEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("type_3_shell_entity"));

    // 波動カートリッジ弾エンティティ
    public static final RegistryObject<EntityType<WaveCartridgeEntity>> WAVE_CARTRIDGE_ENTITY = ENTITIES.register("wave_cartridge_entity",
            () -> EntityType.Builder.<WaveCartridgeEntity>of(WaveCartridgeEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(10)
                    .build("wave_cartridge_entity"));

}