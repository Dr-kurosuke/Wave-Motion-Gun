package com.example.wave_motion_gun.client.renderer;

import com.example.wave_motion_gun.blockentity.WaveCannonBlockEntity;
import com.example.wave_motion_gun.blockentity.WaveEnergyStorageBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class WaveCannonRenderer implements BlockEntityRenderer<WaveCannonBlockEntity> {

    public WaveCannonRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WaveCannonBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        WaveEnergyStorageBlockEntity storage = be.getCachedStorage();

        if (storage == null || storage.isRemoved()) {
            return;
        }

        WaveEnergyStorageBlockEntity.ExecutionMode mode = storage.getExecutionMode();

        // 【修正2】Exhaustモードも描画対象に含める（フェードアウト演出のため）
        if (mode != WaveEnergyStorageBlockEntity.ExecutionMode.CHARGE &&
                mode != WaveEnergyStorageBlockEntity.ExecutionMode.STAY &&
                mode != WaveEnergyStorageBlockEntity.ExecutionMode.EXHAUST) {
            return;
        }

        long stored = storage.getEnergyStored();
        long max = storage.getMaxEnergyStored();
        if (max <= 0) return;

        double ratio = (double) stored / (double) max;

        // 【修正5】描画開始は 0.01 (1%) から
        if (ratio < 0.01) return;

        // 【修正4】Exhaustモードかつ10%以下の場合は問答無用で消す
        if (mode == WaveEnergyStorageBlockEntity.ExecutionMode.EXHAUST && ratio <= 0.1) {
            return;
        }

        float displayRatio = (float) Math.min(ratio, 1.5);

        // --- 描画開始 ---

        Direction facing = be.getBlockState().getValue(BlockStateProperties.FACING);
        long time = be.getLevel().getGameTime() & 0xFFFFF;
        float animTime = time + partialTick;

        poseStack.pushPose();

        // ブロック中心へ移動
        poseStack.translate(0.5, 0.5, 0.5);

        // 向きの補正 (Z+方向を正面とする)
        switch (facing) {
            case NORTH: poseStack.mulPose(Vector3f.YP.rotationDegrees(180)); break;
            case SOUTH: break;
            case EAST:  poseStack.mulPose(Vector3f.YP.rotationDegrees(90)); break;
            case WEST:  poseStack.mulPose(Vector3f.YP.rotationDegrees(-90)); break;
            case UP:    poseStack.mulPose(Vector3f.XP.rotationDegrees(-90)); break;
            case DOWN:  poseStack.mulPose(Vector3f.XP.rotationDegrees(90)); break;
        }

        // 砲口の位置へ
        poseStack.translate(0.0, 0.0, 0.3);

        // 描画バッファ (発光)
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());

        // --- 1. 中心部の光の玉 (Core) ---
        // Exhaust時はコアも少し小さくするか、そのままにするか。ここではそのまま描画。
        renderCore(poseStack, consumer, displayRatio, animTime);

        // --- 2. 吸い込みパーティクル ---

        // 【修正1】パーティクル数を3次関数的に増やす
        // チャージ率が高くなると急激に増える
        // 例: ratio=0.5 -> 0.125, ratio=1.0 -> 1.0, ratio=1.2 -> 1.728
        double curve = Math.pow(displayRatio, 4.0);
        // 【負荷対策】頂点数が膨れ上がらないよう上限80個でキャップする
        int particleCount = Math.min(80, 1 + (int)(curve * 60));

        // Exhaustモードかどうか
        boolean isExhaust = (mode == WaveEnergyStorageBlockEntity.ExecutionMode.EXHAUST);

        for (int i = 0; i < particleCount; i++) {
            long baseSeed = (long)i * 31337L + be.getBlockPos().asLong();
            float randOffset = noise(baseSeed);

            // 速度固定
            float cycleTicks = 40.0f;

            float totalTicks = animTime + (randOffset * cycleTicks);
            long loopCount = (long)(totalTicks / cycleTicks);
            long dynamicSeed = baseSeed ^ (loopCount * 0x5DEECE66DL);

            float progress = totalTicks % cycleTicks / cycleTicks;

            // 【修正4】Exhaustモード時のフェードアウト挙動
            // 新しくスポーンさせない = 生まれたて(progressが小さい)のパーティクルを描画しない
            // progress: 0.0(外側) -> 1.0(中心)
            // Exhaust時は 0.4 未満（外側4割）を描画カットすることで、
            // 「吸い込まれている途中」のものだけが表示され、新規供給が止まったように見せる
            if (isExhaust && progress < 0.4f) {
                continue;
            }

            float distance = 1.0f - progress;
            float moveScale = distance * distance;

            float randAngle1 = noise(dynamicSeed);
            float randAngle2 = noise(dynamicSeed + 1000);
            float randRadius = noise(dynamicSeed + 2000);

            // 発生半径
            double radius = 10.0 + randRadius * 3.0;

            double theta = randAngle1 * Math.PI * 2.0;
            double phi = randAngle2 * (Math.PI / 3.0);

            double x = Math.cos(theta) * Math.sin(phi) * radius * moveScale;
            double y = Math.sin(theta) * Math.sin(phi) * radius * moveScale;
            double z = Math.cos(phi) * radius * moveScale;

            poseStack.pushPose();
            poseStack.translate(x, y, z);

            // ランダム回転
            poseStack.mulPose(Vector3f.XP.rotationDegrees((animTime + i * 10) * 5.0f)); // 回転も速く
            poseStack.mulPose(Vector3f.YP.rotationDegrees((animTime + i * 20) * 5.0f));

            // 【修正3】サイズを極小にする
            // 基本サイズ 0.015 + ランダム 0.01 -> 最大でも 0.025 程度
            float baseScale = 0.015f + randRadius * 0.01f;
            float chargeBonus = 1.0f + (displayRatio * 0.2f); // サイズ増加は控えめに
            float scale = baseScale * chargeBonus; // 距離による縮小はあえて外して視認性を確保、あるいは distance を掛けても良い

            poseStack.scale(scale, scale, scale);

            // フェードイン / フェードアウト
            float alpha = 1.0f;
            if (progress < 0.2f) alpha = progress / 0.2f;

            // Exhaust時は全体的に薄くする
            if (isExhaust) alpha *= 0.5f;

            // 【修正2】パーティクルを二層構造の球体にする
            renderParticleSphere(poseStack, consumer, alpha, scale);

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    // 中心核の描画
    private void renderCore(PoseStack poseStack, VertexConsumer consumer, float ratio, float animTime) {
        poseStack.pushPose();

        float maxRadius = 0.2f;
        float currentRadius = maxRadius * ratio;
        if (currentRadius < 0.02f) currentRadius = 0.02f;

        // 外側：白っぽい水色
        poseStack.pushPose();
        poseStack.scale(currentRadius, currentRadius, currentRadius);
        renderSphere(poseStack, consumer, 0.6f, 0.9f, 1.0f, 0.4f, 10, 10);
        poseStack.popPose();

        // 内側：黄色っぽい白色
        float innerRadius = currentRadius * 0.7f;
        poseStack.pushPose();
        poseStack.scale(innerRadius, innerRadius, innerRadius);
        renderSphere(poseStack, consumer, 1.0f, 1.0f, 0.8f, 0.9f, 10, 10);
        poseStack.popPose();

        poseStack.popPose();
    }

    // 【新規】パーティクル用の二層球体描画
    private void renderParticleSphere(PoseStack poseStack, VertexConsumer consumer, float alpha, float scale) {
        // 【負荷対策】極小パーティクルは形状の粗さが視認できないため分割数を大きく落とす
        int seg = (scale < 0.02f) ? 4 : 6;

        // 外側：白 (半透明)
        poseStack.pushPose();
        // 少し大きく
        poseStack.scale(1.5f, 1.5f, 1.5f);
        renderSphere(poseStack, consumer, 1.0f, 1.0f, 1.0f, alpha * 0.1f, seg, seg);
        poseStack.popPose();

        // 内側：黄色 (不透明に近い)
        poseStack.pushPose();
        renderSphere(poseStack, consumer, 1.0f, 1.0f, 0.2f, alpha, seg, seg);
        poseStack.popPose();
    }

    private float noise(long x) {
        x = (x ^ (x >> 30)) * 0xbf58476d1ce4e5b9L;
        x = (x ^ (x >> 27)) * 0x94d049bb133111ebL;
        x = x ^ (x >> 31);
        return (float)((x >>> 1) & 0x7FFFFFFF) / (float)0x7FFFFFFF;
    }

    private void renderSphere(PoseStack poseStack, VertexConsumer consumer, float r, float g, float b, float a, int stacks, int slices) {
        Matrix4f matrix = poseStack.last().pose();

        // 分割数は呼び出し側で制御する (Coreは10x10、極小パーティクルは4x4〜6x6に落として負荷軽減)
        float radius = 0.7f;

        for (int i = 0; i < stacks; i++) {
            float lat0 = (float) Math.PI * (-0.5f + (float) (i) / stacks);
            float z0 = (float) Math.sin(lat0) * radius;
            float zr0 = (float) Math.cos(lat0) * radius;

            float lat1 = (float) Math.PI * (-0.5f + (float) (i + 1) / stacks);
            float z1 = (float) Math.sin(lat1) * radius;
            float zr1 = (float) Math.cos(lat1) * radius;

            for (int j = 0; j < slices; j++) {
                float lng0 = 2 * (float) Math.PI * (float) (j) / slices;
                float x0 = (float) Math.cos(lng0);
                float y0 = (float) Math.sin(lng0);

                float lng1 = 2 * (float) Math.PI * (float) (j + 1) / slices;
                float x1 = (float) Math.cos(lng1);
                float y1 = (float) Math.sin(lng1);

                vertex(consumer, matrix, x0 * zr0, y0 * zr0, z0, r, g, b, a);
                vertex(consumer, matrix, x0 * zr1, y0 * zr1, z1, r, g, b, a);
                vertex(consumer, matrix, x1 * zr1, y1 * zr1, z1, r, g, b, a);
                vertex(consumer, matrix, x1 * zr0, y1 * zr0, z0, r, g, b, a);
            }
        }
    }

    private void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, float r, float g, float b, float a) {
        consumer.vertex(matrix, x, y, z).color(r, g, b, a).endVertex();
    }
}