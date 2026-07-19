package com.example.wave_motion_gun.entity;

import com.example.wave_motion_gun.init.EntityInit;
import com.example.wave_motion_gun.init.SoundInit;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ShockCannonBeamSegment extends AbstractBeamSegment {

    public ShockCannonBeamSegment(EntityType<?> type, Level level) {
        super(type, level);
    }

    public ShockCannonBeamSegment(Level level, Vec3 start, Vec3 end, float radius, int lifeTime, float timeOffset, int innerColor, int outerColor, int damageValue) {
        this(EntityInit.SHOCK_CANNON_BEAM_SEGMENT.get(), level);
        this.initBeam(start, end, radius, lifeTime, timeOffset, innerColor, outerColor, damageValue);
    }

    @Override
    protected SoundEvent getTrajectorySound() {
        return SoundInit.WAVE_MOTION_GUN_SHOCK_CANNON_TRAJECTORY.get();
    }

    @Override
    protected float getTrajectoryVolume() {
        return 1.5F;
    }
}
