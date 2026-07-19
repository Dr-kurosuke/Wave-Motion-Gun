package com.example.wave_motion_gun.client.renderer;

import com.example.wave_motion_gun.entity.WaveEnergyBall;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WaveEnergyBallRenderer extends EntityRenderer<WaveEnergyBall> {
    private static final ResourceLocation BEAM_TEXTURE = new ResourceLocation("wave_motion_gun_mod", "textures/particle/wave_beam_texture.png");
    private static final float PHASE_SCALE = 0.3f;

    public WaveEnergyBallRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WaveEnergyBall entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        double lerpX = Mth.lerp(partialTicks, entity.xo, entity.getX());
        double lerpY = Mth.lerp(partialTicks, entity.yo, entity.getY());
        double lerpZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());
        Vec3 currentEntityPos = new Vec3(lerpX, lerpY, lerpZ);

        Vec3 startPos = entity.getLastSegmentPos();

        if (startPos.distanceToSqr(currentEntityPos) < 0.01) return;

        poseStack.pushPose();
        VertexConsumer vertexBuilder = buffer.getBuffer(RenderType.entityTranslucent(this.getTextureLocation(entity)));

        float baseRadius = entity.getExplosionRadius() / 2.0f;
        if (baseRadius < 0.1f) baseRadius = 0.1f;
        int fullBright = BeamGeometryHelper.FULL_BRIGHT;

        float absTime = entity.tickCount + partialTicks;
        float phaseHead = absTime;
        float dist = (float) currentEntityPos.distanceTo(startPos);
        float phaseTail = phaseHead - (dist * PHASE_SCALE);

        // ■■■ エンティティから色を取得して分解 ■■■
        int inner = entity.getInnerColor();
        int outer = entity.getOuterColor();

        // レンダリング原点(エンティティ位置)基準の相対座標に変換
        Vec3 relStart = startPos.subtract(currentEntityPos);

        // 内側 (Inner)
        BeamGeometryHelper.drawSubdividedSegment(poseStack, vertexBuilder, relStart, Vec3.ZERO,
                baseRadius * 0.4f,
                BeamGeometryHelper.red(inner), BeamGeometryHelper.green(inner), BeamGeometryHelper.blue(inner),
                1.0f, fullBright, absTime, 0.6f, 0.6f, phaseTail, phaseHead);

        // 外側 (Outer)
        BeamGeometryHelper.drawSubdividedSegment(poseStack, vertexBuilder, relStart, Vec3.ZERO,
                baseRadius,
                BeamGeometryHelper.red(outer), BeamGeometryHelper.green(outer), BeamGeometryHelper.blue(outer),
                0.5f, fullBright, absTime, -0.3f, 0.5f, phaseTail, phaseHead);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(WaveEnergyBall entity, Frustum frustum, double camX, double camY, double camZ) {
        if (!entity.shouldRender(camX, camY, camZ)) return false;
        AABB box = entity.getBoundingBox().inflate(0.5D);
        Vec3 start = entity.getLastSegmentPos();
        box = box.minmax(new AABB(start.x, start.y, start.z, start.x, start.y, start.z));
        return frustum.isVisible(box.inflate(3.0D));
    }

    @Override
    public ResourceLocation getTextureLocation(WaveEnergyBall entity) {
        return BEAM_TEXTURE;
    }
}
