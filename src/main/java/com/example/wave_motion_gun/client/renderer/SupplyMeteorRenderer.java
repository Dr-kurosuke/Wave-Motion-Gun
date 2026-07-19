package com.example.wave_motion_gun.client.renderer;

import com.mojang.math.Axis;

import com.example.wave_motion_gun.ExampleMod;
import com.example.wave_motion_gun.entity.SupplyMeteorEntity;
import com.example.wave_motion_gun.init.BlockInit; // 追加
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.minecraft.client.Minecraft; // 追加
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher; // 追加
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState; // 追加
import net.minecraftforge.client.model.data.ModelData;
import net.minecraft.world.inventory.InventoryMenu; // 【追加】

public class SupplyMeteorRenderer extends EntityRenderer<SupplyMeteorEntity> {
    // ブロックテクスチャを使うので、このリソースは直接は使わなくなるが、getTextureLocationのために残すか、
    // TextureAtlas.LOCATION_BLOCKS を返すように変更する
    private static final ResourceLocation BEAM_TEXTURE = new ResourceLocation(ExampleMod.MODID, "textures/particle/wave_beam_texture.png");

    public SupplyMeteorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F; // 影をつける
    }

    @Override
    public void render(SupplyMeteorEntity entity, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        matrixStack.pushPose();

        // 1. 回転と位置調整
        float yRot = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        // 進行方向へ回転
        matrixStack.mulPose(Axis.YP.rotationDegrees(180.0F - yRot));
        matrixStack.mulPose(Axis.XP.rotationDegrees(-xRot));

        // 2. ブロック（コンテナ）の描画
        renderBlockModel(matrixStack, buffer, packedLight);

        // 3. 軌跡の描画 (残す場合)
        // コンテナの後ろから軌跡が出るように位置を微調整
        matrixStack.translate(0, 0, 0.5);
        renderShortTrail(entity, matrixStack, buffer, LightTexture.pack(15, 15)); // 軌跡は常に明るく

        matrixStack.popPose();
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
    }

    private void renderBlockModel(PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        matrixStack.pushPose();

        // ブロックのサイズ調整 (少し大きくして目立たせるなど)
        float scale = 1.5F;
        matrixStack.scale(scale, scale, scale);

        // 中心を合わせる (ブロックの原点は角なので、-0.5して中心に持ってくる)
        matrixStack.translate(-0.5D, -0.5D, -0.5D);

        BlockState blockState = BlockInit.SUPPLY_CRATE.get().defaultBlockState();
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();

        // ブロックモデルの描画
        dispatcher.renderSingleBlock(blockState, matrixStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);

        matrixStack.popPose();
    }

    private void renderShortTrail(SupplyMeteorEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int light) {
        matrixStack.pushPose();

        // 後ろ(-Z)に伸ばす
        float length = 8.0F;
        float width = 1.2F;
        matrixStack.scale(width, width, length);

        VertexConsumer builder = buffer.getBuffer(RenderType.entityTranslucent(BEAM_TEXTURE));

        // ランク色を取得
        int color = entity.getOuterColor();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        for (int i = 0; i < 2; i++) {
            matrixStack.pushPose();
            if (i == 1) matrixStack.mulPose(Axis.ZP.rotationDegrees(90.0F));

            PoseStack.Pose pose = matrixStack.last();
            Matrix4f p = pose.pose();
            Matrix3f n = pose.normal();

            // Z: 0.0 -> -1.0 (length倍される)
            // 先頭(0.0)は透明度高め、後ろ(-1.0)は消える
            drawVertex(p, n, builder, 0, -0.5f, 0, 0, 0, r, g, b, 0.6f, light);
            drawVertex(p, n, builder, 0, -0.5f, -1, 1, 0, r, g, b, 0.0f, light);
            drawVertex(p, n, builder, 0, 0.5f, -1, 1, 1, r, g, b, 0.0f, light);
            drawVertex(p, n, builder, 0, 0.5f, 0, 0, 1, r, g, b, 0.6f, light);

            matrixStack.popPose();
        }
        matrixStack.popPose();
    }

    private void drawVertex(Matrix4f pose, Matrix3f normal, VertexConsumer builder, float x, float y, float z, float u, float v, float r, float g, float b, float a, int light) {
        builder.vertex(pose, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SupplyMeteorEntity entity) {
        // ブロックテクスチャのアトラスを返すのが一般的
        return InventoryMenu.BLOCK_ATLAS;
    }
}