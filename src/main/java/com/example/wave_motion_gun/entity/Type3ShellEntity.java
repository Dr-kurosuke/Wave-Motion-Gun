package com.example.wave_motion_gun.entity;

import com.example.wave_motion_gun.init.EntityInit;
import com.example.wave_motion_gun.init.ItemInit;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity; // 追加
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

// ■ 追加: Explosion, DamageSource関連
import net.minecraft.world.level.Explosion;
import net.minecraft.world.damagesource.DamageSource;
import java.util.List;

public class Type3ShellEntity extends AbstractHurtingProjectile implements ItemSupplier {
    private boolean hasPlayedSpawnEffect = false;
    public Type3ShellEntity(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ItemInit.TYPE_3_SHELL.get());
    }

    @Override
    public Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void tick() {
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
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    4.0F, (1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F) * 0.7F);

            float radius = 4.0F;
            float damageAmount = 20.0F;

            // 1. 通常の爆発 (地形破壊とノックバック用)
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), radius, Level.ExplosionInteraction.BLOCK);

            // 2. 範囲内の生命体に固定ダメージを与える処理 (三式弾の破片ダメージ)
            // 半径4.0ブロックの範囲を取得
            List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius));

            // ダメージソース作成 (爆発属性)
            DamageSource damageSource = this.level().damageSources().explosion(this, this.getOwner());

            for (LivingEntity entity : targets) {
                // 距離判定
                if (entity.distanceToSqr(this) <= radius * radius) {
                    entity.hurt(damageSource, damageAmount);
                }
            }

            this.discard();
        }
    }

    @Override
    public boolean isOnFire() { return false; }
}