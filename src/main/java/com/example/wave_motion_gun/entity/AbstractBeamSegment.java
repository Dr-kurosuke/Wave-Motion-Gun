package com.example.wave_motion_gun.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

/**
 * ビームの軌跡セグメントエンティティの共通基底クラス。
 * 位置・色・寿命の同期、軌跡SEのクールダウン再生、描画距離判定を担当する。
 */
public abstract class AbstractBeamSegment extends Entity implements IEntityAdditionalSpawnData {
    protected Vec3 endDelta = Vec3.ZERO;
    protected float radius = 1.0F;
    protected int maxAge = 60;
    protected float timeOffset = 0.0F;
    // --- 色情報とダメージ ---
    protected int innerColor = 0xFFFFFF;
    protected int outerColor = 0x0000FF;
    protected int damageValue = 0;

    // ■■■ SE再生用クールダウンタイマー ■■■
    // playedSoundフラグの代わりに、再生まわりを管理するカウンターを使用
    private int soundCooldown = 0;

    protected AbstractBeamSegment(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    /** サブクラスのコンストラクタから呼ぶ共通初期化 */
    protected void initBeam(Vec3 start, Vec3 end, float radius, int lifeTime, float timeOffset, int innerColor, int outerColor, int damageValue) {
        this.setPos(start.x, start.y, start.z);
        this.endDelta = end.subtract(start);
        this.radius = radius;
        this.maxAge = lifeTime;
        this.timeOffset = timeOffset;
        this.innerColor = innerColor;
        this.outerColor = outerColor;
        this.damageValue = damageValue;
    }

    /** 軌跡SE */
    protected abstract SoundEvent getTrajectorySound();

    /** 軌跡SEの音量 */
    protected abstract float getTrajectoryVolume();

    /** サーバー側の毎tick処理(ダメージ判定など)。デフォルトは何もしない */
    protected void serverTick() {}

    @Override
    public void tick() {
        super.tick();

        if (!this.level.isClientSide) {
            this.serverTick();
        }

        // ■■■ プレイヤーが近くにいる間、3秒おきにSEを再生 ■■■
        if (this.level.isClientSide) {
            if (this.soundCooldown > 0) {
                this.soundCooldown--;
            } else {
                // クールダウンが明けたらプレイヤー判定
                Player player = this.level.getNearestPlayer(this, 20.0D);

                if (player != null) {
                    Vec3 center = this.position().add(this.endDelta.scale(0.5));

                    // 音を再生
                    this.level.playSound(player, center.x, center.y, center.z,
                            this.getTrajectorySound(),
                            SoundSource.NEUTRAL, this.getTrajectoryVolume(), 1.0F);

                    // クールダウンを3秒(60tick)に設定
                    this.soundCooldown = 60;
                }
            }
        }

        if (this.tickCount >= this.maxAge) {
            this.discard();
        }
    }

    // 描画範囲判定の拡張
    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        double d0 = this.getBoundingBox().getSize() * 10.0D;
        if (Double.isNaN(d0)) {
            d0 = 1.0D;
        }
        d0 *= 128.0D;
        return dist < d0 * d0;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeDouble(this.endDelta.x);
        buffer.writeDouble(this.endDelta.y);
        buffer.writeDouble(this.endDelta.z);
        buffer.writeFloat(this.radius);
        buffer.writeInt(this.maxAge);
        buffer.writeFloat(this.timeOffset);
        buffer.writeInt(this.innerColor);
        buffer.writeInt(this.outerColor);
        buffer.writeInt(this.damageValue);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        this.endDelta = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        this.radius = buffer.readFloat();
        this.maxAge = buffer.readInt();
        this.timeOffset = buffer.readFloat();
        this.innerColor = buffer.readInt();
        this.outerColor = buffer.readInt();
        this.damageValue = buffer.readInt();
    }

    public Vec3 getEndDelta() { return endDelta; }
    public float getRadius() { return radius; }
    public float getTimeOffset() { return timeOffset; }
    public int getMaxAge() { return maxAge; }
    public int getInnerColor() { return innerColor; }
    public int getOuterColor() { return outerColor; }
}
