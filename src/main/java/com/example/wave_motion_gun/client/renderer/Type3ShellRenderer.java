package com.example.wave_motion_gun.client.renderer;

import com.example.wave_motion_gun.entity.Type3ShellEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class Type3ShellRenderer extends EntityRenderer<Type3ShellEntity> {
    private static final ResourceLocation BEAM_TEXTURE = new ResourceLocation("wave_motion_gun_mod", "textures/particle/wave_beam_texture.png");

    public Type3ShellRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Type3ShellEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // 1. 現在位置の補間 (後追いレンダリング用)
        double lerpX = Mth.lerp(partialTicks, entity.xo, entity.getX());
        double lerpY = Mth.lerp(partialTicks, entity.yo, entity.getY());
        double lerpZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());
        Vec3 currentPos = new Vec3(lerpX, lerpY, lerpZ);

        // 2. 進行方向の計算
        Vec3 motion = entity.getDeltaMovement();
        if (motion.lengthSqr() < 0.0001) {
            // 初速がない場合はPowerを参照
            motion = new Vec3(entity.xPower, entity.yPower, entity.zPower);
        }
        if (motion.lengthSqr() < 0.0001) {
            motion = new Vec3(0, 0, 1); // デフォルト
        }
        Vec3 dir = motion.normalize();

        // 3. ビームの始点と終点 (長さ2ブロック)
        // currentPosを先端とし、そこから後ろに2ブロック分伸ばす
        double beamLength = 2.0D;
        Vec3 endPos = currentPos;
        Vec3 startPos = currentPos.subtract(dir.scale(beamLength));

        VertexConsumer vertexBuilder = buffer.getBuffer(RenderType.entityTranslucent(this.getTextureLocation(entity)));

        float absTime = entity.tickCount + partialTicks;

        // 色設定: 三式弾 (燃えるような赤オレンジ)
        // Inner: 白に近い黄色, Outer: 赤オレンジ
        float rIn = 1.0F, gIn = 0.9F, bIn = 0.5F;
        float rOut = 1.0F, gOut = 0.3F, bOut = 0.0F;

        float radius = 0.3F; // ビームの太さ
        int fullBright = BeamGeometryHelper.FULL_BRIGHT; // 常に明るく

        // エンティティのレンダリング位置基準の相対座標に変換
        Vec3 relStart = startPos.subtract(currentPos);
        Vec3 relEnd = endPos.subtract(currentPos);

        // 描画実行
        // 内側 (芯)
        BeamGeometryHelper.drawCylinder(poseStack, vertexBuilder, relStart, relEnd,
                radius * 0.6f, rIn, gIn, bIn, 1.0f, fullBright, absTime, 0.5f);

        // 外側 (光彩)
        BeamGeometryHelper.drawCylinder(poseStack, vertexBuilder, relStart, relEnd,
                radius, rOut, gOut, bOut, 0.6f, fullBright, absTime, -0.3f);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(Type3ShellEntity entity) {
        return BEAM_TEXTURE;
    }
}