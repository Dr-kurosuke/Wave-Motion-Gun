package com.example.wave_motion_gun.entity;

import com.example.wave_motion_gun.ExampleMod;
import com.example.wave_motion_gun.init.EntityInit;
import com.example.wave_motion_gun.init.SoundInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.IndirectEntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

public class WaveEnergyBall extends AbstractHurtingProjectile implements IEntityAdditionalSpawnData {
    // 着弾(消滅待機)状態フラグ
    private static final EntityDataAccessor<Boolean> IMPACTED = SynchedEntityData.defineId(WaveEnergyBall.class, EntityDataSerializers.BOOLEAN);

    private int penetrationCount = 0;
    private Vec3 powerVelocity = null;
    private Vec3 startPos = null;

    private Vec3 serverPrevPos = null;
    private Vec3 lastSoundPos = null;
    private static final double SOUND_INTERVAL = 8.0D;
    // 【負荷対策】1tickあたりのブロック破壊処理バジェット。
    // 旧値は4秒でサーバーが完全に固まっていた。10msに抑え、残りは次tickへ持ち越す。
    private static final long MAX_PROC_TIME_NS = 10 * 1_000_000L;

    // 【負荷対策】貫通中のヒットエフェクト(パーティクル+爆発音)の連発防止用クールダウン
    private int lastImpactEffectTick = Integer.MIN_VALUE;
    private static final int IMPACT_EFFECT_COOLDOWN_TICKS = 5;

    private boolean discardOnUnloadedChunk = true;

    private double maxRange = 150.0D;
    private float explosionRadius = 4.0F;
    private int MAX_PENETRATION = 500;

    // --- パラメータ ---
    private int damageValue = 100;
    private boolean isDestructive = true;
    private int innerColor = 0xFFFFFF;
    // ■■■ 修正: デフォルト色をレンダラーの見た目(水色)に合わせる ■■■
    private int outerColor = 0x3399FF;
    private boolean isSafetyDisabled = false;

    // 消滅までの猶予タイマー
    private int dyingTicks = 0;
    // 着弾後、クライアント側の軌跡演出を待ってから消滅するまでの猶予tick数
    private static final int DYING_GRACE_TICKS = 100;

    // 【修正】速度ゼロのまま放置された場合のデスポーン用カウンタ
    // (speed=0でスポーンした個体が永久に残り続けるのを防ぐ)
    private int zeroVelocityTicks = 0;
    private static final int ZERO_VELOCITY_DESPAWN_TICKS = 20;

    // ビーム軌跡セグメントの寿命(tick)
    private static final int SEGMENT_LIFETIME_TICKS = 300;

    // 前回セグメントを設置した位置
    private Vec3 lastSegmentPos = null;

    // セグメント設定
    private static final double SEGMENT_LENGTH = 50.0D;
    private static final double SEGMENT_STEP = 25.0D;

    // --- 【追加】チャンクローダー用 ---
    private ChunkPos loadedChunkPos = null;

    public WaveEnergyBall(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IMPACTED, false);
    }

    public boolean isImpacted() {
        return this.entityData.get(IMPACTED);
    }

    public void startImpact() {
        if (!this.level.isClientSide) {
            this.entityData.set(IMPACTED, true);
        }
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public EntityType<?> getType() {
        return EntityInit.WAVE_ENERGY_BALL.get();
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeDouble(this.xPower);
        buffer.writeDouble(this.yPower);
        buffer.writeDouble(this.zPower);
        buffer.writeDouble(this.maxRange);
        buffer.writeFloat(this.explosionRadius);
        buffer.writeInt(this.damageValue);
        buffer.writeBoolean(this.isDestructive);
        buffer.writeInt(this.innerColor);
        buffer.writeInt(this.outerColor);
        buffer.writeBoolean(this.isSafetyDisabled);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        this.xPower = additionalData.readDouble();
        this.yPower = additionalData.readDouble();
        this.zPower = additionalData.readDouble();
        this.maxRange = additionalData.readDouble();
        this.explosionRadius = additionalData.readFloat();
        this.powerVelocity = new Vec3(this.xPower, this.yPower, this.zPower);
        this.damageValue = additionalData.readInt();
        this.isDestructive = additionalData.readBoolean();
        this.innerColor = additionalData.readInt();
        this.outerColor = additionalData.readInt();
        this.isSafetyDisabled = additionalData.readBoolean();
    }

    public void setCustomParams(int range, float radius) {
        this.maxRange = (double) range;
        this.explosionRadius = radius;
        this.MAX_PENETRATION = (int) range / 2;
    }

    public void setAdditionalParams(int damage, boolean destructive, int inner, int outer, boolean safetyDisabled) {
        this.damageValue = damage;
        this.isDestructive = destructive;
        this.innerColor = inner;
        this.outerColor = outer;
        this.isSafetyDisabled = safetyDisabled;
    }

    public float getExplosionRadius() {
        return this.explosionRadius;
    }

    // --- セーブ/ロード ---
    // これがないとチャンク再ロードで全パラメータがデフォルト値(damage=100/破壊ON/射程150)に戻り、
    // 射程も再ロード地点から再計測されてしまう
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("MaxRange", this.maxRange);
        tag.putFloat("ExplosionRadius", this.explosionRadius);
        tag.putInt("DamageValue", this.damageValue);
        tag.putBoolean("Destructive", this.isDestructive);
        tag.putInt("InnerColor", this.innerColor);
        tag.putInt("OuterColor", this.outerColor);
        tag.putBoolean("SafetyDisabled", this.isSafetyDisabled);
        tag.putInt("PenetrationCount", this.penetrationCount);
        tag.putInt("MaxPenetration", this.MAX_PENETRATION);
        if (this.startPos != null) {
            tag.putDouble("StartX", this.startPos.x);
            tag.putDouble("StartY", this.startPos.y);
            tag.putDouble("StartZ", this.startPos.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("MaxRange")) this.maxRange = tag.getDouble("MaxRange");
        if (tag.contains("ExplosionRadius")) this.explosionRadius = tag.getFloat("ExplosionRadius");
        if (tag.contains("DamageValue")) this.damageValue = tag.getInt("DamageValue");
        if (tag.contains("Destructive")) this.isDestructive = tag.getBoolean("Destructive");
        if (tag.contains("InnerColor")) this.innerColor = tag.getInt("InnerColor");
        if (tag.contains("OuterColor")) this.outerColor = tag.getInt("OuterColor");
        if (tag.contains("SafetyDisabled")) this.isSafetyDisabled = tag.getBoolean("SafetyDisabled");
        if (tag.contains("PenetrationCount")) this.penetrationCount = tag.getInt("PenetrationCount");
        if (tag.contains("MaxPenetration")) this.MAX_PENETRATION = tag.getInt("MaxPenetration");
        if (tag.contains("StartX")) {
            this.startPos = new Vec3(tag.getDouble("StartX"), tag.getDouble("StartY"), tag.getDouble("StartZ"));
        }
    }

    // ■■■ 追加: レンダラー参照用ゲッター ■■■
    public int getInnerColor() { return this.innerColor; }
    public int getOuterColor() { return this.outerColor; }

    public Vec3 getLastSegmentPos() {
        return this.lastSegmentPos != null ? this.lastSegmentPos : this.position();
    }

    @Override
    public void tick() {
        this.baseTick();

        // --- 【追加】チャンクローダー処理 ---
        // サーバー側かつセーフティ解除時のみ、現在のチャンクを強制ロード
        if (!this.level.isClientSide && this.isSafetyDisabled) {
            updateChunkLoading();
        }

        Vec3 currentPos = this.position();

        if (this.startPos == null) {
            this.startPos = currentPos;
            this.lastSegmentPos = currentPos;
        }
        if (this.lastSegmentPos == null) {
            this.lastSegmentPos = currentPos;
        }

        // --- 消滅待機状態 ---
        if (this.isImpacted()) {
            this.setDeltaMovement(Vec3.ZERO);

            if (this.level.isClientSide) {
                Vec3 dir = currentPos.subtract(this.lastSegmentPos);
                if (dir.lengthSqr() > 0.01) {
                    this.lastSegmentPos = this.lastSegmentPos.add(dir.scale(0.25));
                } else {
                    this.lastSegmentPos = currentPos;
                }
            } else {
                this.dyingTicks++;
                if (this.dyingTicks > DYING_GRACE_TICKS) {
                    this.discard();
                }
            }
            return;
        }

        // --- 移動処理 ---
        if (this.powerVelocity == null || this.powerVelocity.lengthSqr() < 0.0001) {
            this.powerVelocity = new Vec3(this.xPower, this.yPower, this.zPower);
        }
        if (this.powerVelocity.lengthSqr() < 0.0001) {
            // 【修正】速度ゼロが20tick続いたら消滅させる (永久に残留するのを防ぐ)
            if (!this.level.isClientSide && ++this.zeroVelocityTicks >= ZERO_VELOCITY_DESPAWN_TICKS) {
                this.discard();
            }
            return;
        }
        this.zeroVelocityTicks = 0;

        Vec3 nextPos = currentPos.add(this.powerVelocity);

        // 未ロードチャンク判定
        if (!this.level.isClientSide && this.discardOnUnloadedChunk) {
            BlockPos nextBlockPos = new BlockPos((int)Math.floor(nextPos.x), (int)Math.floor(nextPos.y), (int)Math.floor(nextPos.z));
            if (!this.level.isLoaded(nextBlockPos)) {
                // セーフティ解除なら消滅せず待機 (チャンクローダーが効いていればロードされるはず)
                if (!this.isSafetyDisabled) {
                    this.startImpact();
                }
                // ロード待ちのためreturnするが、チャンクローダーにより次tick以降で進めるようになる
                return;
            }
        }

        // 衝突判定
        BlockHitResult blockHit = this.level.clip(new ClipContext(
                currentPos, nextPos,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        if (blockHit.getType() != HitResult.Type.MISS) {
            this.onHit(blockHit);
            if (this.isRemoved() || this.isImpacted()) return;
        }

        this.setPos(nextPos.x, nextPos.y, nextPos.z);

        // セグメント設置
        while (currentPos.distanceToSqr(this.lastSegmentPos) >= SEGMENT_LENGTH * SEGMENT_LENGTH) {
            Vec3 dir = currentPos.subtract(this.lastSegmentPos).normalize();
            Vec3 segmentEndPos = this.lastSegmentPos.add(dir.scale(SEGMENT_LENGTH));

            if (!this.level.isClientSide) {
                spawnBeamSegment(this.lastSegmentPos, segmentEndPos);
            }
            this.lastSegmentPos = this.lastSegmentPos.add(dir.scale(SEGMENT_STEP));
        }

        if (this.level.isClientSide) {
            if (this.random.nextFloat() < 0.3F) {
                this.level.addParticle(ParticleTypes.FLASH, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            }
        }

        if (!this.level.isClientSide) {
            handleServerTickLogic(this.position());
        }
    }

    // --- 【追加】チャンクローダー更新メソッド ---
    private void updateChunkLoading() {
        if (this.level instanceof ServerLevel serverLevel) {
            ChunkPos currentChunk = new ChunkPos(this.blockPosition());

            // チャンクが変わった場合、または初回の場合
            if (this.loadedChunkPos == null || !this.loadedChunkPos.equals(currentChunk)) {

                // 古いチャンクのロードを解除
                if (this.loadedChunkPos != null) {
                    ForgeChunkManager.forceChunk(serverLevel, ExampleMod.MODID, this, this.loadedChunkPos.x, this.loadedChunkPos.z, false, true);
                }

                // 新しいチャンクをロード
                // forceChunk(level, modId, entity, x, z, add, ticking)
                ForgeChunkManager.forceChunk(serverLevel, ExampleMod.MODID, this, currentChunk.x, currentChunk.z, true, true);
                this.loadedChunkPos = currentChunk;
            }
        }
    }

    // --- 【追加】エンティティ削除時にロード解除 ---
    @Override
    public void remove(RemovalReason reason) {
        if (!this.level.isClientSide && this.loadedChunkPos != null && this.level instanceof ServerLevel serverLevel) {
            ForgeChunkManager.forceChunk(serverLevel, ExampleMod.MODID, this, this.loadedChunkPos.x, this.loadedChunkPos.z, false, true);
            this.loadedChunkPos = null;
        }
        super.remove(reason);
    }

    private void spawnBeamSegment(Vec3 start, Vec3 end) {
        float timeOffset = (float)this.tickCount;
        WaveBeamSegment segment = new WaveBeamSegment(this.level, start, end, this.explosionRadius, SEGMENT_LIFETIME_TICKS, timeOffset, this.innerColor, this.outerColor, this.damageValue);
        this.level.addFreshEntity(segment);
    }

    private void handleServerTickLogic(Vec3 nowPos) {
        if (this.lastSoundPos == null) {
            this.lastSoundPos = nowPos;
        } else {
            double distSqToLastSound = this.lastSoundPos.distanceToSqr(nowPos);
            if (distSqToLastSound > SOUND_INTERVAL * SOUND_INTERVAL) {
                double dist = Math.sqrt(distSqToLastSound);
                Vec3 dir = nowPos.subtract(this.lastSoundPos).normalize();
                double d = SOUND_INTERVAL;
                while (d <= dist) {
                    Vec3 soundPos = this.lastSoundPos.add(dir.scale(d));
                    this.level.playSound(null, soundPos.x, soundPos.y, soundPos.z,
                            SoundInit.WAVE_MOTION_GUN_TRAJECTORY.get(),
                            net.minecraft.sounds.SoundSource.NEUTRAL,
                            3.0F, 1.0F);
                    d += SOUND_INTERVAL;
                }
                this.lastSoundPos = nowPos;
            }
        }

        if (this.serverPrevPos == null) {
            this.serverPrevPos = nowPos;
        }

        double distSq = nowPos.distanceToSqr(this.serverPrevPos);
        double interval = this.explosionRadius * 0.5;
        double intervalSq = interval * interval;

        if (distSq > intervalSq) {
            double safeDist = this.explosionRadius * 1.1;
            double safeDistSq = safeDist * safeDist;

            if (this.serverPrevPos.distanceToSqr(this.startPos) > safeDistSq) {
                long startTime = System.nanoTime();
                boolean completed = this.vaporizeCylinder(this.serverPrevPos, nowPos, this.explosionRadius, startTime);

                this.applyAreaDamage(this.explosionRadius, true);

                // 【負荷対策】バジェット超過で未完了の場合、serverPrevPosを進めずに
                // 次tickで同じ区間を再処理する(破壊済みブロックは空気スキップされるため再走査は軽い)。
                // 旧実装ではここでstartImpact()していたが、バジェットを10msに縮めた結果
                // 大口径ビームが即座に停止してしまうため、持ち越し方式に変更。
                if (!completed) {
                    return;
                }
                this.serverPrevPos = nowPos;
            } else {
                this.serverPrevPos = nowPos;
            }
        }

        if (this.distanceToSqr(this.startPos) > maxRange * maxRange) {
            this.handleFinalExplosion();
        }
        this.checkInsideBlocks();
    }

    private boolean vaporizeCylinder(Vec3 start, Vec3 end, float radius, long startTime) {
        if (!this.isDestructive) return true;

        double minX = Math.min(start.x, end.x) - radius;
        double minY = Math.min(start.y, end.y) - radius;
        double minZ = Math.min(start.z, end.z) - radius;
        double maxX = Math.max(start.x, end.x) + radius;
        double maxY = Math.max(start.y, end.y) + radius;
        double maxZ = Math.max(start.z, end.z) + radius;

        BlockPos minPos = new BlockPos((int)Math.floor(minX), (int)Math.floor(minY), (int)Math.floor(minZ));
        BlockPos maxPos = new BlockPos((int)Math.ceil(maxX), (int)Math.ceil(maxY), (int)Math.ceil(maxZ));

        float radiusSq = radius * radius;
        int checkCounter = 0;

        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            if (++checkCounter % 100 == 0) {
                if (System.nanoTime() - startTime > MAX_PROC_TIME_NS) {
                    return false;
                }
            }
            Vec3 blockCenter = Vec3.atCenterOf(pos);
            double distSq = getDistSqToSegment(blockCenter, start, end);

            if (distSq <= radiusSq) {
                BlockState state = this.level.getBlockState(pos);
                // 【修正】岩盤等の破壊不可ブロック(getDestroySpeed < 0)は破壊しない (ShockCannonBeamと同様)
                if (!state.isAir() && state.getDestroySpeed(this.level, pos) >= 0) {
                    this.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        return true;
    }

    private double getDistSqToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        Vec3 ap = p.subtract(a);
        double abLenSq = ab.lengthSqr();
        if (abLenSq == 0) return p.distanceToSqr(a);
        double t = ap.dot(ab) / abLenSq;
        if (t < 0.0) t = 0.0;
        else if (t > 1.0) t = 1.0;
        Vec3 closest = a.add(ab.scale(t));
        return p.distanceToSqr(closest);
    }

    /**
     * 範囲ダメージを与える。
     * @param directional trueなら進行方向の前方半球のみ (飛翔中用)。falseなら全方位 (最終爆発用)。
     */
    private void applyAreaDamage(float radius, boolean directional) {
        List<Entity> list = this.level.getEntities(this, this.getBoundingBox().inflate(radius));
        IndirectEntityDamageSource src = new IndirectEntityDamageSource("wave_motion_gun", this, this.getOwner());
        src.setExplosion();

        Vec3 dir = this.getDeltaMovement();
        if (dir.lengthSqr() < 0.0001 && this.powerVelocity != null) {
            dir = this.powerVelocity;
        }
        dir = dir.normalize();

        double radiusSq = radius * radius;

        for (Entity e : list) {
            if (e instanceof LivingEntity living) {
                if (this.distanceToSqr(living) > radiusSq) continue;
                Vec3 toTarget = living.position().subtract(this.position()).normalize();
                if (!directional || dir.dot(toTarget) >= 0.0) {
                    living.hurt(src, (float) this.damageValue);
                    living.setSecondsOnFire(10);
                }
            }
        }
    }

    private void vaporizeSphere(float radius) {
        if (!this.isDestructive) return;

        BlockPos center = this.blockPosition();
        int r = (int) Math.ceil(radius);
        float rSq = radius * radius;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z <= rSq) {
                        BlockPos targetPos = center.offset(x, y, z);
                        BlockState state = this.level.getBlockState(targetPos);
                        // 【修正】岩盤等の破壊不可ブロック(getDestroySpeed < 0)は破壊しない (ShockCannonBeamと同様)
                        if (!state.isAir() && state.getDestroySpeed(this.level, targetPos) >= 0) {
                            this.level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (this.level.isClientSide) return;
        if (this.isImpacted()) return;

        penetrationCount++;

        if (penetrationCount >= MAX_PENETRATION) {
            this.handleFinalExplosion();
            return;
        }

        // 【負荷対策】地中貫通中は毎tick onHitが呼ばれるため、
        // エフェクト(パーティクル20個+爆発音の全クライアント送信)は5tickに1回までに制限する
        if (this.level instanceof ServerLevel serverLevel) {
            if (this.tickCount - this.lastImpactEffectTick >= IMPACT_EFFECT_COOLDOWN_TICKS) {
                this.lastImpactEffectTick = this.tickCount;
                this.playImpactEffects(serverLevel);
            }
        }

        if (result.getType() == HitResult.Type.BLOCK) {
            if (result instanceof BlockHitResult blockHit) {
                BlockState state = this.level.getBlockState(blockHit.getBlockPos());
                if (state.getDestroySpeed(this.level, blockHit.getBlockPos()) < 0) {
                    this.handleFinalExplosion();
                }
            }
        }
    }

    private void playImpactEffects(ServerLevel serverLevel) {
        int m = (penetrationCount >= MAX_PENETRATION) ? 1 : 10;
        serverLevel.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY(), this.getZ(), 200/m, 2.0D, 2.0D, 2.0D, 0.1D);
        float vol = (penetrationCount >= MAX_PENETRATION) ? 10.0F : 1.5F;
        float pitch = (penetrationCount >= MAX_PENETRATION) ? 0.1F : 0.6F;
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.BLOCKS, vol, pitch);
    }

    private void handleFinalExplosion() {
        if (!this.level.isClientSide) {
            float finalRadius = explosionRadius * 1.3F;
            this.vaporizeSphere(finalRadius);
            // 【修正】最終爆発は全方位にダメージを与える (前方半球フィルタを適用しない)
            this.applyAreaDamage(finalRadius, false);

            if (this.level instanceof ServerLevel serverLevel) {
                this.playImpactEffects(serverLevel);
            }
            this.startImpact();
        }
    }

    @Override public boolean isOnFire() { return false; }
    @Override public boolean displayFireAnimation() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        double d0 = this.getBoundingBox().getSize() * 10.0D;
        if (Double.isNaN(d0)) { d0 = 1.0D; }
        d0 = d0 * 64.0D;
        return dist < d0 * d0;
    }
}