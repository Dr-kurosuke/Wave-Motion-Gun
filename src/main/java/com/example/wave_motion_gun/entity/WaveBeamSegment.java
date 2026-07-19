package com.example.wave_motion_gun.entity;

import com.example.wave_motion_gun.init.EntityInit;
import com.example.wave_motion_gun.init.SoundInit;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class WaveBeamSegment extends AbstractBeamSegment {

    public WaveBeamSegment(EntityType<?> type, Level level) {
        super(type, level);
    }

    public WaveBeamSegment(Level level, Vec3 start, Vec3 end, float radius, int lifeTime, float timeOffset, int innerColor, int outerColor, int damageValue) {
        this(EntityInit.WAVE_BEAM_SEGMENT.get(), level);
        this.initBeam(start, end, radius, lifeTime, timeOffset, innerColor, outerColor, damageValue);
    }

    @Override
    protected SoundEvent getTrajectorySound() {
        return SoundInit.WAVE_MOTION_GUN_TRAJECTORY.get();
    }

    @Override
    protected float getTrajectoryVolume() {
        return 3.0F;
    }

    // --- 継続ダメージ処理 ---
    @Override
    protected void serverTick() {
        // damageValue<=0 のセグメントは演出専用(補給メテオの軌跡など)なのでダメージ処理を行わない。
        // 被ダメージ側に0.5秒(10tick)の無敵時間があるため、判定は10tickごとで十分
        if (this.damageValue > 0 && this.tickCount % 10 == 0) {
            Vec3 start = this.position();
            // endDelta は start から end へのベクトル (A to B)
            Vec3 ab = this.endDelta;
            double abLenSq = ab.lengthSqr();
            double rSq = this.radius * this.radius;

            // ビーム全長をカバーするAABBで取得(始点のBBだけだと終端側の対象が漏れる)
            List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().expandTowards(this.endDelta).inflate(this.radius + 1.0));

            // 弾本体(WaveEnergyBall)と同じ爆発属性ダメージソース。防具・防具系Modの軽減を有効にする
            // 死因メッセージは death.attack.wave_motion_gun (余剰次元に引き裂かれる)
            // 爆発属性は data/minecraft/tags/damage_type/is_explosion.json で付与
            net.minecraft.world.damagesource.DamageSource damageSource =
                    com.example.wave_motion_gun.utils.ModDamageTypes.waveMotionGun(this.level(), this, null);

            for (LivingEntity target : list) {
                // 1. 座席判定: 本MODのSeatEntityに乗っている場合は除外
                if (target.getVehicle() instanceof SeatEntity) {
                    continue;
                }

                Vec3 p = target.position();
                Vec3 ap = p.subtract(start);

                // 線分への投影係数 t を計算
                double t = (abLenSq == 0) ? 0.0 : ap.dot(ab) / abLenSq;

                // 2. 厳密な円筒判定: 始点(0.0)と終点(1.0)の間にある場合のみヒットとする
                if (t >= 0.0 && t <= 1.0) {
                    // 線分上の最近点
                    Vec3 closest = start.add(ab.scale(t));

                    // 垂直距離チェック
                    if (p.distanceToSqr(closest) <= rSq) {
                        // 3. ダメージ適用
                        // invulnerableTimeのリセットを削除したため、標準の0.5秒無敵が機能します
                        target.hurt(damageSource, (float)this.damageValue);
                        target.setSecondsOnFire(1);
                    }
                }
            }
        }
    }
}
