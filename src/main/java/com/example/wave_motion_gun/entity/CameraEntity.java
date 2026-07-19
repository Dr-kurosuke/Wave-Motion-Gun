package com.example.wave_motion_gun.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CameraEntity extends Entity {
    private Vec3 lockPos = null;
    private float lockYRot = 0;
    private float lockXRot = 0;

    public CameraEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvisible(true);
    }

    public void setTracking(BlockPos pos, Direction facing, Direction outDir) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        if (outDir != null) {
            x += outDir.getStepX() * 0.6;
            y += outDir.getStepY() * 0.6;
            z += outDir.getStepZ() * 0.6;
        }

        float yRot = facing != null ? facing.toYRot() : 0;
        float xRot = 0.0F;

        this.setLock(x, y, z, yRot, xRot);
    }

    public void setLock(double x, double y, double z, float yRot, float xRot) {
        this.lockPos = new Vec3(x, y, z);
        this.lockYRot = yRot;
        this.lockXRot = xRot;
        this.forceSetPositionRotation(x, y, z, yRot, xRot);
    }

    private void forceSetPositionRotation(double x, double y, double z, float yRot, float xRot) {
        this.setPos(x, y, z);
        this.setYRot(yRot);
        this.setXRot(xRot);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.yRotO = yRot;
        this.xRotO = xRot;
        this.xOld = x;
        this.yOld = y;
        this.zOld = z;
    }

    @Override
    public void tick() {
        this.setDeltaMovement(Vec3.ZERO);
        if (this.lockPos != null) {
            forceSetPositionRotation(lockPos.x, lockPos.y, lockPos.z, lockYRot, lockXRot);
        }
    }

    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() { return new ClientboundAddEntityPacket(this); }
    @Override public void move(MoverType type, Vec3 pos) {}
    @Override public void push(Entity entity) {}
    @Override public boolean isPushedByFluid() { return false; }
    @Override public float getEyeHeight(Pose pose, EntityDimensions dimensions) { return 0.0F; }
}