package com.example.wave_motion_gun.client.sound;

import com.example.wave_motion_gun.blockentity.TriggerUnitBlockEntity;
import com.example.wave_motion_gun.init.SoundInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class TriggerUnitSound extends AbstractTickableSoundInstance {
    private final TriggerUnitBlockEntity blockEntity;

    public TriggerUnitSound(TriggerUnitBlockEntity be) {
        // SoundInit.WAVE_MOTION_GUN_CHARGE (モノラル版) を使用
        super(SoundInit.WAVE_MOTION_GUN_CHARGE.get(), SoundSource.BLOCKS, net.minecraft.util.RandomSource.create());
        this.blockEntity = be;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01f;
        this.pitch = 0.5f;

        // 【重要】システムによる自動減衰を無効化（これで「遠すぎて聞こえない」を防ぐ）
        this.attenuation = Attenuation.NONE;

        updatePosition();
    }

    /**
     * 音源位置をワールド座標で更新する。
     *
     * 素のブロック座標を使うと、船上ではシップヤード座標(超遠方)になり距離減衰が破綻する。
     * VS2未導入時や船上でない場合は worldCenterOf がそのまま中心座標を返すため、
     * 常にこのメソッドを通してよい。
     */
    private void updatePosition() {
        net.minecraft.world.phys.Vec3 c = com.example.wave_motion_gun.compat.VSCompat
                .worldCenterOf(this.blockEntity.getLevel(), this.blockEntity.getBlockPos());
        this.x = c.x;
        this.y = c.y;
        this.z = c.z;
    }

    @Override
    public void tick() {
        if (this.blockEntity.isRemoved()) {
            this.stop();
            return;
        }

        // 座標更新 (船が動くため毎tick取り直す)
        updatePosition();

        // --- 擬似的な距離減衰の実装 ---
        // 音源・カメラともワールド座標なので、素直な距離計算で正しく減衰する。
        // 以前あった「距離>1,000,000ならVS座標ズレとみなして音量MAX」という特例は、
        // 400〜1,000,000の帯に落ちると無音になり、効いた場合は何百ブロック離れても
        // 音量MAXのままという不安定なものだった。座標変換で根本解決したため削除。
        float distFactor = 1.0f;
        Entity camera = Minecraft.getInstance().getCameraEntity();

        if (camera != null) {
            double distSq = camera.distanceToSqr(this.x, this.y, this.z);

            // 減衰の設定（最大20ブロック離れたら聞こえなくなる）
            float maxDist = 20.0F;

            if (distSq < maxDist * maxDist) {
                float dist = (float) Math.sqrt(distSq);
                distFactor = 1.0F - (dist / maxDist);
                if (distFactor < 0) distFactor = 0;
            } else {
                distFactor = 0.0f;
            }
        }

        // --- チャージ進行度による音量計算 ---
        float progress = this.blockEntity.clientChargeProgress;

        // ピッチ制御
        this.pitch = 0.5f + (progress * 1.5f);

        // 基本音量
        float baseVolume = 0.0f;
        if (progress > 0.001f) {
            baseVolume = 0.6f + (progress * 1.0f);
        }

        // 最終音量 = チャージ音量 × 距離減衰係数
        // 変化を滑らかにする
        float targetVolume = baseVolume * distFactor;
        this.volume = Mth.lerp(0.1f, this.volume, targetVolume);

        // 停止判定
        if (this.volume < 0.01f && progress <= 0.05f) {
            this.stop();
        }
    }

    public boolean isStopped() {
        return super.isStopped();
    }
}