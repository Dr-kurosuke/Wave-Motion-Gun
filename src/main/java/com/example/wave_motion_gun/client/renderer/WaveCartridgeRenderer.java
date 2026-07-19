package com.example.wave_motion_gun.client.renderer;

import com.mojang.math.Axis;

import com.example.wave_motion_gun.entity.WaveCartridgeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class WaveCartridgeRenderer extends EntityRenderer<WaveCartridgeEntity> {
    private static final ResourceLocation BEAM_TEXTURE = new ResourceLocation("wave_motion_gun_mod", "textures/particle/wave_beam_texture.png");
    private final Random random = new Random();

    public WaveCartridgeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    // ■■■ 追加: 描画判定のオーバーライド ■■■
    @Override
    public boolean shouldRender(WaveCartridgeEntity entity, Frustum frustum, double x, double y, double z) {
        if (entity.isImpacted()) {
            // 着弾中は爆発半径(30ブロック)分、描画判定を広げる
            AABB bigBox = entity.getBoundingBox().inflate(WaveCartridgeEntity.EXPLOSION_RADIUS);
            return frustum.isVisible(bigBox);
        }
        // 通常時は親クラスの判定に従う
        return super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(WaveCartridgeEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        double lerpX = Mth.lerp(partialTicks, entity.xo, entity.getX());
        double lerpY = Mth.lerp(partialTicks, entity.yo, entity.getY());
        double lerpZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());

        if (entity.isImpacted()) {
            float age = entity.impactTicks + partialTicks;
            float expandTicks = (float) WaveCartridgeEntity.EXPAND_TICKS;
            float maxAge = (float) WaveCartridgeEntity.MAX_IMPACT_TICKS;

            // 拡大フェーズ
            float expandProgress = Mth.clamp(age / expandTicks, 0.0F, 1.0F);
            expandProgress = (float) Math.sin(expandProgress * Math.PI / 2);

            // ■ 修正: 徐々に薄くなって消える (拡大終了後から消滅まで)
            float alpha = 1.0F;
            if (age > expandTicks) {
                float fadeDuration = maxAge - expandTicks;
                float timeAfterExpand = age - expandTicks;
                alpha = 1.0F - (timeAfterExpand / fadeDuration);
            }
            alpha = Mth.clamp(alpha, 0.0F, 1.0F);

            float maxRadius = WaveCartridgeEntity.EXPLOSION_RADIUS;
            float currentRadius = expandProgress * maxRadius;

            VertexConsumer vertexBuilder = buffer.getBuffer(RenderType.entityTranslucent(this.getTextureLocation(entity)));

            random.setSeed(entity.getId());
            float rotX = (age * 5.0F + random.nextFloat() * 360.0F) % 360.0F;
            float rotY = (age * 3.0F + random.nextFloat() * 360.0F) % 360.0F;
            float rotZ = (age * 7.0F + random.nextFloat() * 360.0F) % 360.0F;

            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
            poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));

            // 内側の球
            drawSphere(poseStack, vertexBuilder, currentRadius * 0.6F, 1.0F, 1.0F, 1.0F, alpha, 0xF000F0, 8, 8);

            poseStack.popPose();

            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(-rotX * 0.5F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-rotY * 0.8F));

            // 外側の球 (0x00CCFF)
            drawSphere(poseStack, vertexBuilder, currentRadius, 0.0F, 0.8F, 1.0F, alpha * 0.5F, 0xF000F0, 12, 12);

            poseStack.popPose();

        } else {
            // 飛翔中 (変更なし)
            Vec3 currentPos = new Vec3(lerpX, lerpY, lerpZ);
            Vec3 motion = entity.getDeltaMovement();
            if (motion.lengthSqr() < 0.0001) motion = new Vec3(0, 0, 1);
            Vec3 dir = motion.normalize();

            double beamLength = 2.0D;
            Vec3 endPos = currentPos;
            Vec3 startPos = currentPos.subtract(dir.scale(beamLength));

            VertexConsumer vertexBuilder = buffer.getBuffer(RenderType.entityTranslucent(this.getTextureLocation(entity)));
            float absTime = entity.tickCount + partialTicks;

            float rIn = 1.0F, gIn = 1.0F, bIn = 0.8F;
            float rOut = 1.0F, gOut = 0.6F, bOut = 0.0F;
            float radius = 0.35F;
            int fullBright = 0xF000F0;

            // エンティティのレンダリング位置基準の相対座標に変換
            Vec3 relStart = startPos.subtract(currentPos);
            Vec3 relEnd = endPos.subtract(currentPos);

            BeamGeometryHelper.drawCylinder(poseStack, vertexBuilder, relStart, relEnd, radius * 0.6f, rIn, gIn, bIn, 1.0f, fullBright, absTime, 0.5f);
            BeamGeometryHelper.drawCylinder(poseStack, vertexBuilder, relStart, relEnd, radius, rOut, gOut, bOut, 0.6f, fullBright, absTime, -0.3f);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void drawSphere(PoseStack poseStack, VertexConsumer builder, float r, float red, float green, float blue, float alpha, int packedLight, int longs, int lats) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        for (int i = 0; i < lats; i++) {
            float lat0 = (float) (Math.PI * (-0.5 + (double) (i - 1) / lats));
            float z0 = Mth.sin(lat0);
            float zr0 = Mth.cos(lat0);

            float lat1 = (float) (Math.PI * (-0.5 + (double) i / lats));
            float z1 = Mth.sin(lat1);
            float zr1 = Mth.cos(lat1);

            for (int j = 0; j < longs; j++) {
                float lng0 = (float) (2 * Math.PI * (double) (j - 1) / longs);
                float x0 = Mth.cos(lng0);
                float y0 = Mth.sin(lng0);

                float lng1 = (float) (2 * Math.PI * (double) j / longs);
                float x1 = Mth.cos(lng1);
                float y1 = Mth.sin(lng1);

                builder.vertex(pose, x0 * zr0 * r, y0 * zr0 * r, z0 * r).color(red, green, blue, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, x0 * zr0, y0 * zr0, z0).endVertex();
                builder.vertex(pose, x0 * zr1 * r, y0 * zr1 * r, z1 * r).color(red, green, blue, alpha).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, x0 * zr1, y0 * zr1, z1).endVertex();
                builder.vertex(pose, x1 * zr1 * r, y1 * zr1 * r, z1 * r).color(red, green, blue, alpha).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, x1 * zr1, y1 * zr1, z1).endVertex();
                builder.vertex(pose, x1 * zr0 * r, y1 * zr0 * r, z0 * r).color(red, green, blue, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, x1 * zr0, y1 * zr0, z0).endVertex();
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(WaveCartridgeEntity entity) {
        return BEAM_TEXTURE;
    }
}