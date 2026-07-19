package com.example.wave_motion_gun.utils;

import com.example.wave_motion_gun.ExampleMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * 1.19.4以降のデータ駆動DamageType対応。
 * 死因メッセージは従来の death.attack.wave_motion_gun 系のlangキーを維持する
 * (data/wave_motion_gun_mod/damage_type/wave_motion_gun.json の message_id 参照)。
 */
public class ModDamageTypes {
    public static final ResourceKey<DamageType> WAVE_MOTION_GUN = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation(ExampleMod.MODID, "wave_motion_gun"));

    /** 波動砲系の爆発属性ダメージソースを生成する */
    public static DamageSource waveMotionGun(Level level, @Nullable Entity direct, @Nullable Entity causing) {
        return new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(WAVE_MOTION_GUN),
                direct, causing);
    }
}
