package com.example.wave_motion_gun.init;

import com.example.wave_motion_gun.ExampleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoundInit {
    // レジストリの作成
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ExampleMod.MOD_ID);

    // 登録名とResourceLocationは常に同名のため、ヘルパーで一括登録する
    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ExampleMod.MOD_ID, name)));
    }

    public static final RegistryObject<SoundEvent> WAVE_CHARGING = register("wave_charging");
    public static final RegistryObject<SoundEvent> WAVE_CHARGING_STABLE = register("wave_charging_stable");
    public static final RegistryObject<SoundEvent> WAVE_FINISHED = register("wave_finished");

    // 【追加】新しいループ用音源の登録
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_CHARGE = register("wave_motion_gun_charge");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_FIRE = register("wave_motion_gun_fire");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_TRAJECTORY = register("wave_motion_gun_trajectory");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_LIGHTNING = register("wave_motion_gun_lightning");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_HOLO_OFF = register("wave_motion_gun_holo_off");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_HOLO_ON = register("wave_motion_gun_holo_on");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_LEVER = register("wave_motion_gun_lever");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_SWITCH = register("wave_motion_gun_switch");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_TRIGGER = register("wave_motion_gun_trigger");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_GRIP = register("wave_motion_gun_grip");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_FORCED_INJECTOR = register("wave_motion_gun_forced_injector");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_FORCED_INJECTOR_OFF = register("wave_motion_gun_forced_injector_off");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_SAFETY = register("wave_motion_gun_safety");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_VALVE_CLOSE = register("wave_motion_gun_valve_close");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_VALVE_OPEN = register("wave_motion_gun_valve_open");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_CHARGING = register("wave_motion_gun_charging");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_EXHAUSTING = register("wave_motion_gun_exhausting");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_CRITICAL = register("wave_motion_gun_critical");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_SHOCK_CANNON = register("wave_motion_gun_shock_cannon");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_SHOCK_CANNON_TRAJECTORY = register("wave_motion_gun_shock_cannon_trajectory");
    public static final RegistryObject<SoundEvent> WAVE_MOTION_GUN_SHELL = register("wave_motion_gun_shell");
}
