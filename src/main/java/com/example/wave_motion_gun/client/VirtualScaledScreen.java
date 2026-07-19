package com.example.wave_motion_gun.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * 仮想解像度(495x300)にスケーリングして描画するスクリーンの共通基底クラス。
 * TriggerUnitScreen / MonitoringUnitScreen で重複していた
 * スケーリング・マウス座標変換・シザー・テキスト/パネル描画ヘルパーを集約する。
 */
public abstract class VirtualScaledScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    // --- Layout Constants (Virtual Resolution) ---
    public static final int VIRTUAL_WIDTH = 495;
    public static final int VIRTUAL_HEIGHT = 300;

    // --- Common Colors ---
    protected static final int COLOR_STEEL_BG = 0xFF555555;
    protected static final int COLOR_STEEL_LIGHT = 0xFF888888;
    protected static final int COLOR_STEEL_DARK = 0xFF333333;
    protected static final int COLOR_HOLO_BG = 0x80006666;

    // Scaling variables
    protected float globalScale = 1.0f;
    protected float globalOffsetX = 0.0f;
    protected float globalOffsetY = 0.0f;

    // UI開閉アニメーション進行度 (シザーの表示範囲計算に使用)
    protected float uiOpenProgress = 0.0f;

    protected VirtualScaledScreen(T menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    /** init()から呼び、実解像度に対する仮想解像度のスケールとオフセットを計算する */
    protected void computeGlobalScale() {
        float scaleX = (float) this.width / VIRTUAL_WIDTH;
        float scaleY = (float) this.height / VIRTUAL_HEIGHT;
        this.globalScale = Math.min(scaleX, scaleY);
        this.globalOffsetX = (this.width - VIRTUAL_WIDTH * globalScale) / 2.0f;
        this.globalOffsetY = (this.height - VIRTUAL_HEIGHT * globalScale) / 2.0f;
    }

    // --- Mouse Coordinate Transformation ---
    protected double transformMouseX(double mouseX) { return (mouseX - globalOffsetX) / globalScale; }
    protected double transformMouseY(double mouseY) { return (mouseY - globalOffsetY) / globalScale; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(transformMouseX(mouseX), transformMouseY(mouseY), button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(transformMouseX(mouseX), transformMouseY(mouseY));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return mouseReleasedScaled(transformMouseX(mouseX), transformMouseY(mouseY), button);
    }

    /** 変換済み座標を受け取るフック。サブクラスはこちらをオーバーライドする (二重変換防止) */
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return mouseDraggedScaled(transformMouseX(mouseX), transformMouseY(mouseY), button, dragX / globalScale, dragY / globalScale);
    }

    /** 変換済み座標を受け取るフック。サブクラスはこちらをオーバーライドする (二重変換防止) */
    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    // --- Scissor Helpers ---
    // 戻り値がtrueの時のみ内容を描画し、endScissor()を呼ぶこと。
    // falseの時 (表示幅/高さが0以下) は内容の描画自体をスキップする。

    /** 横方向に開くシザー (uiOpenProgressで幅を制限) */
    protected boolean startScissor(int x, int y, int w, int h) {
        Window window = Minecraft.getInstance().getWindow();
        if (window == null) return false;

        double scale = window.getGuiScale();
        int winH = window.getHeight();

        float visibleW = w * uiOpenProgress;
        if (visibleW < 0) visibleW = 0;

        float sx = globalOffsetX + x * globalScale;
        float sy = globalOffsetY + y * globalScale;
        float sw = visibleW * globalScale;
        float sh = h * globalScale;

        return enableScissorArea(sx, sy, sw, sh, scale, winH);
    }

    /** 縦方向に開くシザー (uiOpenProgressで高さを制限、下端基準) */
    protected boolean startScissorVertical(int x, int y, int w, int h) {
        Window window = Minecraft.getInstance().getWindow();
        if (window == null) return false;

        double scale = window.getGuiScale();
        int winH = window.getHeight();

        float visibleH = h * uiOpenProgress;
        if (visibleH < 0) visibleH = 0;

        float sx = globalOffsetX + x * globalScale;
        float sy = globalOffsetY + (y + h - visibleH) * globalScale;

        float sw = w * globalScale;
        float sh = visibleH * globalScale;

        return enableScissorArea(sx, sy, sw, sh, scale, winH);
    }

    private boolean enableScissorArea(float sx, float sy, float sw, float sh, double scale, int winH) {
        int ix = (int)(sx * scale);
        int iy = (int)(winH - (sy + sh) * scale);
        int iw = (int)(sw * scale);
        int ih = (int)(sh * scale);

        if (iw < 0) iw = 0;
        if (ih < 0) ih = 0;
        if (ix < 0) { iw += ix; ix = 0; }
        if (iy < 0) { ih += iy; iy = 0; }

        if (iw > 0 && ih > 0) {
            RenderSystem.enableScissor(ix, iy, iw, ih);
            return true;
        }
        return false;
    }

    protected void endScissor() {
        RenderSystem.disableScissor();
    }

    // --- Scaled Text Helpers ---
    protected void drawScaledString(PoseStack poseStack, String text, float x, float y, int color) {
        drawScaledString(poseStack, text, x, y, color, 1.0f);
    }

    protected void drawScaledString(PoseStack poseStack, String text, float x, float y, int color, float scale) {
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.scale(scale, scale, 1.0f);
        this.font.draw(poseStack, text, 0, 0, color);
        poseStack.popPose();
    }

    /** 影付き・中央揃え */
    protected void drawScaledCenteredString(PoseStack poseStack, String text, float x, float y, int color) {
        drawScaledCenteredString(poseStack, text, x, y, color, 1.0f);
    }

    protected void drawScaledCenteredString(PoseStack poseStack, String text, float x, float y, int color, float scale) {
        int w = this.font.width(text);
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.scale(scale, scale, 1.0f);
        this.font.drawShadow(poseStack, text, -w / 2.0f, 0, color);
        poseStack.popPose();
    }

    /** 影なし・中央揃え */
    protected void drawScaledCenteredStringNoShadow(PoseStack poseStack, String text, float x, float y, int color) {
        drawScaledCenteredStringNoShadow(poseStack, text, x, y, color, 1.0f);
    }

    protected void drawScaledCenteredStringNoShadow(PoseStack poseStack, String text, float x, float y, int color, float scale) {
        int w = this.font.width(text);
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.scale(scale, scale, 1.0f);
        this.font.draw(poseStack, text, -w / 2.0f, 0, color);
        poseStack.popPose();
    }

    // --- Panel Helpers ---
    /**
     * 鋼板風パネル。
     * rightShadow=true で右端にも暗いエッジを描く (MonitoringUnitScreen仕様)。
     * TriggerUnitScreenは従来通り右エッジなし(false)。
     */
    protected void drawSteelBox(PoseStack poseStack, int x, int y, int w, int h, boolean rightShadow) {
        fill(poseStack, x, y, x + w, y + h, COLOR_STEEL_BG);
        fill(poseStack, x, y, x + w, y + 2, COLOR_STEEL_LIGHT);
        fill(poseStack, x, y, x + 2, y + h, COLOR_STEEL_LIGHT);
        fill(poseStack, x, y + h - 2, x + w, y + h, COLOR_STEEL_DARK);
        if (rightShadow) {
            fill(poseStack, x + w - 2, y, x + w, y + h, COLOR_STEEL_DARK);
        }
    }

    protected void drawHoloBox(PoseStack poseStack, int x, int y, int w, int h) {
        fill(poseStack, x, y, x + w, y + h, COLOR_HOLO_BG);
    }

    protected void drawLamp(PoseStack poseStack, int cx, int cy, boolean active) {
        int color = active ? 0xFF00FF00 : 0xFFFF0000;
        int r = 4;
        fill(poseStack, cx - r - 1, cy - r - 1, cx + r + 1, cy + r + 1, 0xFF888888);
        fill(poseStack, cx - r, cy - r, cx + r, cy + r, color);
        fill(poseStack, cx - 1, cy - 2, cx + 2, cy - 1, 0xCCFFFFFF);
    }
}
