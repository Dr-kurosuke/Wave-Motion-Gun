package com.example.wave_motion_gun.entity;

import net.minecraft.network.chat.Component;

import com.example.wave_motion_gun.ExampleMod;
import com.example.wave_motion_gun.init.BlockInit;
import com.example.wave_motion_gun.init.EntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public class SupplyMeteorEntity extends Entity {
    private static final EntityDataAccessor<Integer> REWARD_RANK = SynchedEntityData.defineId(SupplyMeteorEntity.class, EntityDataSerializers.INT);

    private int targetX, targetY, targetZ;
    private int lifeTime = 0;

    // ★追加: 所有者のUUID
    private UUID ownerUUID = null;

    private Vec3 lastSegmentPos = null;
    private static final double SEGMENT_LENGTH = 15.0D;
    private static final double SEGMENT_STEP = 10.0D;

    private ChunkPos loadedChunkPos = null;

    public SupplyMeteorEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    // ★修正: UUIDを受け取る
    public void initialize(Vec3 startPos, BlockPos targetPos, int rank, UUID ownerId) {
        this.setPos(startPos);
        this.lastSegmentPos = startPos;
        this.targetX = targetPos.getX();
        this.targetY = targetPos.getY();
        this.targetZ = targetPos.getZ();
        this.entityData.set(REWARD_RANK, rank);
        this.ownerUUID = ownerId; // 保存

        Vec3 targetVec = new Vec3(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        Vec3 direction = targetVec.subtract(startPos).normalize();
        this.setDeltaMovement(direction.scale(2.0));

        this.setYRot((float)(Math.atan2(direction.x, direction.z) * (180D / Math.PI)));
        this.setXRot((float)(Math.asin(direction.y) * (180D / Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(REWARD_RANK, 0);
    }

    // ランクに応じた色 (現状は内外同色のため共通ヘルパーに集約)
    private int getRankColor() {
        int rank = this.entityData.get(REWARD_RANK);
        switch (rank) {
            case 4: return 0xA020F0;
            case 3: return 0xFF0000;
            case 2: return 0x0000FF;
            case 1: return 0x00FF00;
            default: return 0x808080;
        }
    }

    public int getOuterColor() {
        return getRankColor();
    }

    public int getInnerColor() {
        return getRankColor();
    }

    @Override
    public void tick() {
        this.baseTick();

        if (!this.level().isClientSide) {
            updateChunkLoading();

            lifeTime++;
            if (lifeTime > 800) { this.discard(); return; }
        }

        Vec3 currentPos = this.position();
        Vec3 motion = this.getDeltaMovement();
        Vec3 nextPos = currentPos.add(motion);

        BlockHitResult hitResult = this.level().clip(new ClipContext(
                currentPos,
                nextPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                this
        ));

        if (hitResult.getType() != HitResult.Type.MISS) {
            this.setPos(hitResult.getLocation());
            if (!this.level().isClientSide) {
                impact(hitResult.getBlockPos());
            }
            return;
        }

        this.setPos(nextPos);

        if (lastSegmentPos == null) lastSegmentPos = currentPos;

        if (!this.level().isClientSide) {
            while (this.position().distanceToSqr(this.lastSegmentPos) >= SEGMENT_LENGTH * SEGMENT_LENGTH) {
                Vec3 dir = this.position().subtract(this.lastSegmentPos).normalize();
                Vec3 segmentEnd = this.lastSegmentPos.add(dir.scale(SEGMENT_LENGTH));

                spawnSegment(this.lastSegmentPos, segmentEnd);
                this.lastSegmentPos = this.lastSegmentPos.add(dir.scale(SEGMENT_STEP));
            }

            if (this.blockPosition().distManhattan(new BlockPos(targetX, targetY, targetZ)) < 3) {
                impact(this.blockPosition());
            }
        }
    }

    private void spawnSegment(Vec3 start, Vec3 end) {
        int inner = getInnerColor();
        int outer = getOuterColor();
        WaveBeamSegment segment = new WaveBeamSegment(this.level(), start, end, 2.0F, 300, (float)this.tickCount, inner, outer, 0);
        this.level().addFreshEntity(segment);
    }

    private void impact(BlockPos pos) {
        if (!this.level().isClientSide) {
            ((ServerLevel)this.level()).sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, 0.7F);

            BlockPos placePos = pos;
            if (this.level().getBlockState(placePos).isSolid()) {
                placePos = placePos.above();
            }

            BlockState crateState = BlockInit.SUPPLY_CRATE.get().defaultBlockState();
            boolean placed = false;

            for(int i=0; i<5; i++) {
                if (this.level().getBlockState(placePos).canBeReplaced()) {
                    this.level().setBlock(placePos, crateState, 3);
                    if (this.level().getBlockEntity(placePos) instanceof com.example.wave_motion_gun.blockentity.SupplyCrateBlockEntity crateBe) {
                        crateBe.setRewardRank(this.entityData.get(REWARD_RANK));
                    }
                    placed = true;
                    break;
                }
                placePos = placePos.above();
            }

            if (placed) {
                // ★追加: プレイヤーの到着待ちフラグを解除
                notifyOwner("message.wave_motion_gun_mod.supply.arrived");

                BlockPos center = placePos.below();
                for (int x = -4; x <= 4; x++) {
                    for (int z = -4; z <= 4; z++) {
                        for (int y = -2; y <= 2; y++) {
                            BlockPos target = center.offset(x, y, z);
                            if (target.distSqr(center) <= 16.0) {
                                if (this.level().getBlockState(target).isAir() &&
                                        this.level().getBlockState(target.below()).isSolid() &&
                                        this.random.nextInt(3) == 0) {
                                    this.level().setBlock(target, Blocks.FIRE.defaultBlockState(), 3);
                                }
                            }
                        }
                    }
                }
            } else {
                // 【修正】設置に全て失敗した場合もフラグを解除しないと、以後の補給要請が3分間ブロックされ続ける
                notifyOwner("message.wave_motion_gun_mod.supply.placement_failed");
            }

            this.discard();
        }
    }

    // 所有者プレイヤーの到着待ちフラグを解除し、結果メッセージを表示する
    private void notifyOwner(String messageKey) {
        if (this.ownerUUID != null && this.level() instanceof ServerLevel serverLevel) {
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(this.ownerUUID);
            if (player != null) {
                CompoundTag data = player.getPersistentData();
                data.putBoolean("WaveMotionGun_IsWaitingDrop", false);
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(messageKey), true);
            }
        }
    }

    private void updateChunkLoading() {
        if (this.level() instanceof ServerLevel serverLevel) {
            ChunkPos currentChunk = new ChunkPos(this.blockPosition());
            if (this.loadedChunkPos == null || !this.loadedChunkPos.equals(currentChunk)) {
                if (this.loadedChunkPos != null) {
                    ForgeChunkManager.forceChunk(serverLevel, ExampleMod.MODID, this, this.loadedChunkPos.x, this.loadedChunkPos.z, false, true);
                }
                ForgeChunkManager.forceChunk(serverLevel, ExampleMod.MODID, this, currentChunk.x, currentChunk.z, true, true);
                this.loadedChunkPos = currentChunk;
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && this.loadedChunkPos != null && this.level() instanceof ServerLevel serverLevel) {
            ForgeChunkManager.forceChunk(serverLevel, ExampleMod.MODID, this, this.loadedChunkPos.x, this.loadedChunkPos.z, false, true);
            this.loadedChunkPos = null;
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("RewardRank")) this.entityData.set(REWARD_RANK, tag.getInt("RewardRank"));
        this.targetX = tag.getInt("TargetX");
        this.targetY = tag.getInt("TargetY");
        this.targetZ = tag.getInt("TargetZ");
        if (tag.contains("DeltaX")) this.setDeltaMovement(tag.getDouble("DeltaX"), tag.getDouble("DeltaY"), tag.getDouble("DeltaZ"));
        // ★追加: UUID読み込み
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("RewardRank", this.entityData.get(REWARD_RANK));
        tag.putInt("TargetX", targetX);
        tag.putInt("TargetY", targetY);
        tag.putInt("TargetZ", targetZ);
        Vec3 delta = this.getDeltaMovement();
        tag.putDouble("DeltaX", delta.x);
        tag.putDouble("DeltaY", delta.y);
        tag.putDouble("DeltaZ", delta.z);
        // ★追加: UUID保存
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
    }

    @Override public Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean isPickable() { return true; }
}