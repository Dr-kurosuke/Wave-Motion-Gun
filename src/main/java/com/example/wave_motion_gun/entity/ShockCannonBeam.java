package com.example.wave_motion_gun.entity;

import com.example.wave_motion_gun.init.EntityInit;
import com.example.wave_motion_gun.init.SoundInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.IndirectEntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

public class ShockCannonBeam extends AbstractHurtingProjectile implements IEntityAdditionalSpawnData {

    private Vec3 startPos = null;
    private Vec3 lastSegmentPos = null;

    // パラメータ
    private double maxRange = 150.0D;
    private float explosionRadius = 4.0F;
    private int damageValue = 100;
    private int innerColor = 0xFFFFFF;
    private int outerColor = 0x3399FF;

    // セグメント(軌跡)設定
    private static final double SEGMENT_LENGTH = 50.0D;
    private static final double SEGMENT_STEP = 25.0D;
    // 軌跡セグメントの寿命(tick)
    private static final int SEGMENT_LIFETIME_TICKS = 30;

    // SE再生用
    private Vec3 lastSoundPos = null;
    private static final double SOUND_INTERVAL = 8.0D;

    public ShockCannonBeam(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public EntityType<?> getType() {
        return EntityInit.SHOCK_CANNON_BEAM.get();
    }

    // --- データ同期 ---
    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeDouble(this.xPower);
        buffer.writeDouble(this.yPower);
        buffer.writeDouble(this.zPower);
        buffer.writeDouble(this.maxRange);
        buffer.writeFloat(this.explosionRadius);
        buffer.writeInt(this.damageValue);
        buffer.writeInt(this.innerColor);
        buffer.writeInt(this.outerColor);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        this.xPower = buffer.readDouble();
        this.yPower = buffer.readDouble();
        this.zPower = buffer.readDouble();
        this.maxRange = buffer.readDouble();
        this.explosionRadius = buffer.readFloat();
        this.damageValue = buffer.readInt();
        this.innerColor = buffer.readInt();
        this.outerColor = buffer.readInt();
        this.setDeltaMovement(this.xPower, this.yPower, this.zPower);
    }

    // --- パラメータ設定用 ---
    public void setCustomParams(int range, float radius) {
        this.maxRange = (double) range;
        this.explosionRadius = radius;
    }

    // 【修正】未使用だったdestructive/safetyDisabled引数を削除
    public void setAdditionalParams(int damage, int inner, int outer) {
        this.damageValue = damage;
        this.innerColor = inner;
        this.outerColor = outer;
    }

    // --- セーブ/ロード ---
    // チャンク再ロードでパラメータと射程計測がリセットされないようにする(WaveEnergyBallと同様)
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("MaxRange", this.maxRange);
        tag.putFloat("ExplosionRadius", this.explosionRadius);
        tag.putInt("DamageValue", this.damageValue);
        tag.putInt("InnerColor", this.innerColor);
        tag.putInt("OuterColor", this.outerColor);
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
        if (tag.contains("InnerColor")) this.innerColor = tag.getInt("InnerColor");
        if (tag.contains("OuterColor")) this.outerColor = tag.getInt("OuterColor");
        if (tag.contains("StartX")) {
            this.startPos = new Vec3(tag.getDouble("StartX"), tag.getDouble("StartY"), tag.getDouble("StartZ"));
        }
    }

    // レンダラー用
    public int getInnerColor() { return this.innerColor; }
    public int getOuterColor() { return this.outerColor; }
    public float getExplosionRadius() { return this.explosionRadius; }
    public Vec3 getLastSegmentPos() {
        return this.lastSegmentPos != null ? this.lastSegmentPos : this.position();
    }

    @Override
    public void tick() {
        this.baseTick();

        Vec3 currentPos = this.position();
        if (this.startPos == null) {
            this.startPos = currentPos;
            this.lastSegmentPos = currentPos;
        }
        if (this.lastSegmentPos == null) {
            this.lastSegmentPos = currentPos;
        }

        // 1. 移動処理
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() < 0.0001) {
            motion = new Vec3(this.xPower, this.yPower, this.zPower);
            this.setDeltaMovement(motion);
        }
        Vec3 nextPos = currentPos.add(motion);

        // 2. 消滅判定
        if (!this.level.isClientSide) {
            BlockPos nextBlockPos = new BlockPos(nextPos);
            boolean isUnloaded = !this.level.isLoaded(nextBlockPos);
            boolean isOutOfRange = this.startPos.distanceToSqr(nextPos) > this.maxRange * this.maxRange;

            if (isUnloaded || isOutOfRange) {
                spawnBeamSegment(this.lastSegmentPos, currentPos);
                this.discard();
                return;
            }
        }

        // 3. 衝突判定 (修正箇所)

        // A. ブロック衝突チェック
        HitResult hitResult = this.level.clip(new ClipContext(
                currentPos, nextPos,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        // B. エンティティ衝突チェック
        if (!this.level.isClientSide) {
            Vec3 endSearchPos = (hitResult.getType() != HitResult.Type.MISS) ? hitResult.getLocation() : nextPos;

            net.minecraft.world.phys.EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                    this.level,
                    this,
                    currentPos,
                    endSearchPos,
                    this.getBoundingBox().expandTowards(motion).inflate(1.0D),
                    // ■ 修正: 衝突対象から ShockCannonBeam を除外する条件を追加
                    (e) -> !e.isSpectator() && e.isPickable()
                            && (e != this.getOwner() || this.tickCount >= 5)
                            && !(e instanceof ShockCannonBeam)
            );

            if (entityHit != null) {
                hitResult = entityHit;
            }
        }

        // C. 衝突時の処理実行
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onHit(hitResult);
            if (this.isRemoved()) return;
        }

        // 4. 位置更新
        this.setPos(nextPos.x, nextPos.y, nextPos.z);

        // 5. 軌跡(セグメント)の生成
        while (currentPos.distanceToSqr(this.lastSegmentPos) >= SEGMENT_LENGTH * SEGMENT_LENGTH) {
            Vec3 dir = currentPos.subtract(this.lastSegmentPos).normalize();
            Vec3 segmentEndPos = this.lastSegmentPos.add(dir.scale(SEGMENT_LENGTH));

            if (!this.level.isClientSide) {
                spawnBeamSegment(this.lastSegmentPos, segmentEndPos);
            }
            this.lastSegmentPos = this.lastSegmentPos.add(dir.scale(SEGMENT_STEP));
        }

        // 6. エフェクトとサウンド
        if (this.level.isClientSide) {
            if (this.random.nextFloat() < 0.3F) {
                this.level.addParticle(ParticleTypes.FLASH, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            }
        } else {
            playPassingSound(currentPos);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (this.level.isClientSide) return;

        // ブロック衝突時：3ブロック貫通 -> 爆発
        if (result.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) result;
            BlockPos hitBlockPos = blockHit.getBlockPos();

            // 貫通処理
            Vec3 direction = this.getDeltaMovement().normalize();
            if (direction.lengthSqr() == 0) direction = new Vec3(0, 0, 1);

            Vec3 startPenetration = Vec3.atCenterOf(hitBlockPos);
            Vec3 endPenetration = startPenetration.add(direction.scale(3.0));

            vaporizeLine(startPenetration, endPenetration);

            this.setPos(endPenetration);

            explode();
            this.discard();
        }
        else if (result.getType() == HitResult.Type.ENTITY) {
            // エンティティ命中時は即爆発
            explode();
            this.discard();
        }
    }

    private void vaporizeLine(Vec3 start, Vec3 end) {
        Vec3 diff = end.subtract(start);
        double length = diff.length();
        Vec3 dir = diff.normalize();

        for (double d = 0; d <= length; d += 0.5D) {
            BlockPos pos = new BlockPos(start.add(dir.scale(d)));
            BlockState state = this.level.getBlockState(pos);
            if (!state.isAir() && state.getDestroySpeed(this.level, pos) >= 0) {
                this.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private void explode() {
        if (this.level.isClientSide) return;

        spawnBeamSegment(this.lastSegmentPos, this.position());

        vaporizeSphere(this.explosionRadius);
        applyAreaDamage(this.explosionRadius);
        scatterFire(this.explosionRadius);

        if (this.level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.BLOCKS, 4.0F, (1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F) * 0.7F);
        }
    }

    private void vaporizeSphere(float radius) {
        BlockPos center = this.blockPosition();
        int r = (int) Math.ceil(radius);
        float rSq = radius * radius;

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z <= rSq) {
                        BlockPos targetPos = center.offset(x, y, z);
                        BlockState state = this.level.getBlockState(targetPos);
                        if (!state.isAir() && state.getDestroySpeed(this.level, targetPos) >= 0) {
                            this.level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    private void scatterFire(float radius) {
        BlockPos center = this.blockPosition();
        int r = (int) Math.ceil(radius);
        float rSq = radius * radius;

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z <= rSq) {
                        if (this.random.nextFloat() < 0.15F) {
                            BlockPos targetPos = center.offset(x, y, z);
                            if (this.level.getBlockState(targetPos).isAir() &&
                                    this.level.getBlockState(targetPos.below()).isSolidRender(this.level, targetPos.below())) {
                                this.level.setBlock(targetPos, Blocks.FIRE.defaultBlockState(), 3);
                            }
                        }
                    }
                }
            }
        }
    }

    private void applyAreaDamage(float radius) {
        List<Entity> list = this.level.getEntities(this, this.getBoundingBox().inflate(radius));
        IndirectEntityDamageSource src = new IndirectEntityDamageSource("explosion", this, this.getOwner());
        src.setExplosion();

        for (Entity e : list) {
            if (e instanceof LivingEntity living) {
                if (this.distanceToSqr(living) <= radius * radius) {
                    living.hurt(src, (float) this.damageValue);
                    living.setSecondsOnFire(5);
                }
            }
        }
    }

    private void spawnBeamSegment(Vec3 start, Vec3 end) {
        if (start.distanceToSqr(end) < 0.01) return;

        float timeOffset = (float)this.tickCount;
        ShockCannonBeamSegment segment = new ShockCannonBeamSegment(this.level, start, end, 0.8F, SEGMENT_LIFETIME_TICKS, timeOffset, this.innerColor, this.outerColor, this.damageValue);
        this.level.addFreshEntity(segment);
    }

    private void playPassingSound(Vec3 nowPos) {
        if (this.lastSoundPos == null) {
            this.lastSoundPos = nowPos;
            return;
        }
        double distSq = this.lastSoundPos.distanceToSqr(nowPos);
        if (distSq > SOUND_INTERVAL * SOUND_INTERVAL) {
            this.level.playSound(null, nowPos.x, nowPos.y, nowPos.z,
                    SoundInit.WAVE_MOTION_GUN_SHOCK_CANNON_TRAJECTORY.get(),
                    net.minecraft.sounds.SoundSource.NEUTRAL,
                    1.5F, 1.0F);
            this.lastSoundPos = nowPos;
        }
    }

    @Override public boolean isOnFire() { return false; }
    @Override public boolean displayFireAnimation() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double dist) { return dist < 4096.0D; }
}