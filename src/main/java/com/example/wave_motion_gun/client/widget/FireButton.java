package com.example.wave_motion_gun.client.widget;

import com.example.wave_motion_gun.init.SoundInit;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

/**
 * 発射ボタン。バニラのボタンテクスチャを使いつつ、TRIGGER SEと拡大テキストで描画する。
 * textScale: TriggerUnitScreenでは1.5f、ShockCannonScreenでは1.0f。
 */
public class FireButton extends Button {
    private final Component message;
    private final float textScale;

    public FireButton(int x, int y, int width, int height, Component message, float textScale, OnPress onPress) {
        super(x, y, width, height, TextComponent.EMPTY, onPress);
        this.message = message;
        this.textScale = textScale;
    }

    @Override public Component getMessage() { return this.message; }

    // Sound Implementation: Use TRIGGER sound
    @Override
    public void playDownSound(net.minecraft.client.sounds.SoundManager handler) {
        handler.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundInit.WAVE_MOTION_GUN_TRIGGER.get(), 1.0F));
    }

    @Override public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        int i = this.getYImage(this.isHoveredOrFocused());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        blit(poseStack, this.x, this.y, this.width / 2, this.height, 0, 46 + i * 20, this.width / 2, 20, 256, 256);
        blit(poseStack, this.x + this.width / 2, this.y, this.width / 2, this.height, 200 - this.width / 2, 46 + i * 20, this.width / 2, 20, 256, 256);
        int textColor = this.active ? WidgetColors.COLOR_TEXT_WHITE : 0xFFAAAAAA;
        poseStack.pushPose();
        float cx = this.x + this.width / 2.0f;
        float cy = this.y + (this.height - 8) / 2.0f;
        poseStack.translate(cx, cy, 0);
        poseStack.scale(this.textScale, this.textScale, 1.0f);
        drawCenteredString(poseStack, minecraft.font, this.getMessage(), 0, 0, textColor);
        poseStack.popPose();
    }
}
