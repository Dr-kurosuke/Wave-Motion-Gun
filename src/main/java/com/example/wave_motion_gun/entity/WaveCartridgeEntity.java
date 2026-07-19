package com.example.wave_motion_gun.entity;

import com.example.wave_motion_gun.init.ItemInit;
import com.example.wave_motion_gun.init.SoundInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource; // ■ 追加
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

public class WaveCartridgeEntity extends AbstractHurtingProjectile implements ItemSupplier {

    private static final EntityDataAccessor<Boolean> IMPACTED = SynchedEntityData.defineId(WaveCartridgeEntity.class, EntityDataSerializers.BOOLEAN);
    private boolean hasPlayedSpawnEffect = false;
    public int impactTicks = 0;
    public static final int MAX_IMPACT_TICKS = 200;
    public static final int EXPAND_TICKS = 10;
    public static final float EXPLOSION_RADIUS = 40.0F;

    // 【負荷対策】半径40の球(約53万座標)を1tickで破壊するとサーバーが数秒停止するため、
    // 複数tickに分割して処理する。以下は分割処理の進行状態。
    private BlockPos vaporizeCenter = null;
    private int vapX = 0, vapY = 0, vapZ = 0;
    private boolean vaporizeDone = true;
    // 1tickあたりの処理時間バジェット (約8ms)
    private static final long VAPORIZE_BUDGET_NS = 8_000_000L;

    // 【負荷対策】着弾後ダメージ処理で毎tick生成していたDamageSourceを使い回す
    private DamageSource impactDamageSource = null;

    public WaveCartridgeEntity(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 512.0D;
        return distance < range * range;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IMPACTED, false);
    }

    public boolean isImpacted() {
        return this.entityData.get(IMPACTED);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ItemInit.WAVE_CARTRIDGE.get());
    }

    @Override
    public Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void tick() {

        if (isImpacted()) {
            this.setDeltaMovement(Vec3.ZERO);
            impactTicks++;

            if (!this.level().isClientSide) {
                // 【負荷対策】ブロック破壊を複数tickに分割して進める
                tickVaporize();

                // 範囲内の生命体に継続ダメージ (60ダメージ)
                // 【負荷対策】半径40のAABB走査を毎tick行うのは重いため、10tickに1回に間引く
                // (hurtには無敵時間があるため実質的なダメージ量はほぼ変わらない)
                if (this.impactTicks % 10 == 1) {
                    // ■ 修正: IndirectEntityDamageSource を使用して爆発ダメージを作成
                    // "explosion" という名前で、発生源はthis(弾)、間接的な原因はOwner(撃った人)
                    // 【負荷対策】DamageSourceは毎回生成せずフィールドに使い回す
                    if (this.impactDamageSource == null) {
                        this.impactDamageSource = this.level().damageSources().explosion(this, this.getOwner());
                    }

                    List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(EXPLOSION_RADIUS));

                    for (LivingEntity entity : targets) {
                        if (entity.distanceToSqr(this) <= EXPLOSION_RADIUS * EXPLOSION_RADIUS) {
                            entity.hurt(this.impactDamageSource, 60.0F);
                        }
                    }
                }

                // サウンド再生
                if (this.impactTicks % 60 == 0) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundInit.WAVE_MOTION_GUN_TRAJECTORY.get(),
                            SoundSource.NEUTRAL, 8.0F, 1.0F);
                }

                // 【修正】ブロック破壊が完了するまでは消滅させない
                if (impactTicks >= MAX_IMPACT_TICKS && this.vaporizeDone) {
                    this.discard();
                }
            }
        } else {
            super.tick();
            // ■ 追加: クライアントサイドでのみ、初回Tick時に爆発エフェクトを再生
            if (this.level().isClientSide && !this.hasPlayedSpawnEffect) {
                this.hasPlayedSpawnEffect = true;

                // 1. 巨大な爆発パーティクル (EXPLOSION_EMITTER はTNTなどの大爆発の見た目)
                this.level().addParticle(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1.0, 0.0, 0.0);

                // 2. 爆発音 (playLocalSoundを使うことで、サーバー側に通知せずクライアントだけで鳴らす)
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                        net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                        net.minecraft.sounds.SoundSource.BLOCKS,
                        4.0F, (1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F) * 0.7F, false);
            }
            if (this.level().isClientSide) {
                this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (isImpacted()) return;

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundInit.WAVE_MOTION_GUN_FIRE.get(), SoundSource.NEUTRAL, 10.0F, 1.0F);

        if (!this.level().isClientSide) {
            this.entityData.set(IMPACTED, true);
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), EXPLOSION_RADIUS, Level.ExplosionInteraction.NONE);
            // 【負荷対策】1tickでの一括破壊はサーバー停止を招くため、分割処理を開始するだけにする
            startVaporizeSphere();
        }
    }

    /**
     * 【負荷対策】球状破壊の分割処理を開始する。
     * 実際の破壊は tickVaporize() が毎tickの時間バジェット内で少しずつ進める。
     */
    private void startVaporizeSphere() {
        int r = (int) EXPLOSION_RADIUS;
        this.vaporizeCenter = this.blockPosition();
        this.vapX = -r;
        this.vapY = -r;
        this.vapZ = -r;
        this.vaporizeDone = false;
    }

    /**
     * 【負荷対策】球状破壊を時間バジェット付きで進める(約8ms/tick)。
     * WaveEnergyBallの分割処理と同様の方式。既に空気のブロックはスキップし、
     * 岩盤等(getDestroySpeed < 0)は破壊しない。
     * setBlockはフラグ 2|16 (クライアント通知のみ・隣接ブロック更新なし)で一括破壊のコストを抑える。
     */
    private void tickVaporize() {
        if (this.vaporizeDone || this.vaporizeCenter == null) return;

        long startTime = System.nanoTime();
        int r = (int) EXPLOSION_RADIUS;
        float rSq = EXPLOSION_RADIUS * EXPLOSION_RADIUS;
        int checkCounter = 0;

        while (this.vapY <= r) {
            while (this.vapX <= r) {
                while (this.vapZ <= r) {
                    int x = this.vapX;
                    int y = this.vapY;
                    int z = this.vapZ;
                    this.vapZ++;

                    if (x * x + y * y + z * z <= rSq) {
                        BlockPos targetPos = this.vaporizeCenter.offset(x, y, z);
                        BlockState state = this.level().getBlockState(targetPos);
                        if (!state.isAir() && state.getDestroySpeed(this.level(), targetPos) >= 0) {
                            this.level().setBlock(targetPos, Blocks.AIR.defaultBlockState(), 2 | 16);
                        }
                    }

                    // 一定間隔で時間バジェットを確認し、超過したら次tickへ持ち越す
                    if (++checkCounter % 256 == 0 && System.nanoTime() - startTime > VAPORIZE_BUDGET_NS) {
                        return;
                    }
                }
                this.vapZ = -r;
                this.vapX++;
            }
            this.vapX = -r;
            this.vapY++;
        }
        this.vaporizeDone = true;
    }

    @Override
    public boolean isOnFire() { return false; }
}