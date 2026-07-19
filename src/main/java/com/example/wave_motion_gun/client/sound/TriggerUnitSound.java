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

        this.x = be.getBlockPos().getX() + 0.5;
        this.y = be.getBlockPos().getY() + 0.5;
        this.z = be.getBlockPos().getZ() + 0.5;
    }

    @Override
    public void tick() {
        if (this.blockEntity.isRemoved()) {
            this.stop();
            return;
        }

        // 座標更新
        this.x = this.blockEntity.getBlockPos().getX() + 0.5;
        this.y = this.blockEntity.getBlockPos().getY() + 0.5;
        this.z = this.blockEntity.getBlockPos().getZ() + 0.5;

        // --- 擬似的な距離減衰の実装 ---
        float distFactor = 1.0f;
        Entity camera = Minecraft.getInstance().getCameraEntity();

        if (camera != null) {
            // プレイヤーとブロックの距離を計算
            double distSq = camera.distanceToSqr(this.x, this.y, this.z);

            // 減衰の設定（例：最大20ブロック離れたら聞こえなくなる）
            float maxDist = 20.0F;

            // VS対策: 距離が異常に遠い(Shipyard座標ズレ)場合は、
            // とりあえず減衰させずに聞こえるようにする（あるいは諦めて0にする）判断が必要。
            // ここでは「距離が100ブロック以内なら減衰計算を適用、それ以上なら近くにいるとみなす（バグ回避）」
            // という安全策をとるか、単純に計算するかですが、まずは単純計算で試します。

            // ※もしこれでまた「音が鳴らない」場合は、VSの座標変換がうまくいっていないので
            // distSq が数千万になっている可能性があります。その時は以下のif文を調整します。

            if (distSq < maxDist * maxDist) {
                // 距離が近い場合：距離に応じて音量を下げる
                float dist = (float) Math.sqrt(distSq);
                distFactor = 1.0F - (dist / maxDist);
                if (distFactor < 0) distFactor = 0;
            } else if (distSq > 1000000) {
                // 距離が1000以上（異常値＝VS座標ズレの可能性）の場合
                // プレイヤーが船の上にいるなら、本来は近くにいるはずなので音量MAXにする特例措置
                // （これがないと、船の上でも座標系がズレて無音になる可能性があります）
                distFactor = 1.0f;
            } else {
                // 通常の「遠くにいる」場合
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