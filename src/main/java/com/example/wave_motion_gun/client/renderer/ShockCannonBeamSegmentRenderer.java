package com.example.wave_motion_gun.client.renderer;

import com.example.wave_motion_gun.entity.ShockCannonBeamSegment;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class ShockCannonBeamSegmentRenderer extends EntityRenderer<ShockCannonBeamSegment> {
    private static final ResourceLocation BEAM_TEXTURE = new ResourceLocation("wave_motion_gun_mod", "textures/particle/wave_beam_texture.png");
    private static final float PHASE_SCALE = 0.3f;

    public ShockCannonBeamSegmentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(ShockCannonBeamSegment entity, Frustum frustum, double camX, double camY, double camZ) {
        // 基本的な距離チェック
        if (!entity.shouldRender(camX, camY, camZ)) return false;

        // ビームの太さ分だけ拡張してカリング判定 (安全マージンとして半径+2.0を確保)
        return BeamGeometryHelper.isBeamBoxVisible(frustum, entity.position(), entity.getEndDelta(), entity.getRadius() + 2.0);
    }

    @Override
    public void render(ShockCannonBeamSegment entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Vec3 endDelta = entity.getEndDelta();
        if (endDelta.lengthSqr() < 0.0001) return;

        poseStack.pushPose();
        VertexConsumer vertexBuilder = buffer.getBuffer(RenderType.entityTranslucent(this.getTextureLocation(entity)));

        float baseRadius = 0.8f;
        int fullBright = BeamGeometryHelper.FULL_BRIGHT;

        float absTime = entity.getTimeOffset() + entity.tickCount + partialTicks;
        float phaseEnd = entity.getTimeOffset();
        float length = (float) endDelta.length();
        float phaseStart = phaseEnd - (length * PHASE_SCALE);

        float lifeRatio = (float) entity.tickCount / entity.getMaxAge();
        float alpha = 1.0F - (lifeRatio * lifeRatio * lifeRatio);

        // --- 色情報の取得と分解 ---
        int inner = entity.getInnerColor();
        int outer = entity.getOuterColor();

        // 内側 (Inner)
        BeamGeometryHelper.drawSubdividedSegment(poseStack, vertexBuilder, Vec3.ZERO, endDelta, baseRadius * 0.8f,
                BeamGeometryHelper.red(inner), BeamGeometryHelper.green(inner), BeamGeometryHelper.blue(inner),
                alpha, fullBright, absTime, 0.6f, 0.6f, phaseStart, phaseEnd);

        // 外側 (Outer)
        BeamGeometryHelper.drawSubdividedSegment(poseStack, vertexBuilder, Vec3.ZERO, endDelta, baseRadius,
                BeamGeometryHelper.red(outer), BeamGeometryHelper.green(outer), BeamGeometryHelper.blue(outer),
                alpha * 0.5f, fullBright, absTime, -1.0f, 0.5f, phaseStart, phaseEnd);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ShockCannonBeamSegment entity) { return BEAM_TEXTURE; }
}
