package com.example.wave_motion_gun.client.widget;

import net.minecraft.network.chat.Component;

import com.example.wave_motion_gun.init.SoundInit;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

/** ラベル付き横スライダー (MonitoringUnitScreenのRange/Radius/Damage設定用) */
public class HorizontalSlider extends Button {
    private static final int COLOR_STEEL_BG = 0xFF555555;
    private static final int COLOR_STEEL_LIGHT = 0xFF888888;

    public boolean isDragging = false;
    private float sliderValue = 0.0f;
    private int minVal, maxVal;
    private final String label;
    private final Consumer<Integer> onUpdate;
    private int lastSentValue = Integer.MIN_VALUE;

    public HorizontalSlider(int x, int y, int w, int h, String label, int min, int max, int current, Consumer<Integer> onUpdate) {
        super(x, y, w, h, Component.empty(), b -> {}, DEFAULT_NARRATION);
        this.label = label;
        this.minVal = min;
        this.maxVal = max;
        this.onUpdate = onUpdate;
        updateValue(current);
    }

    public void updateLimits(int min, int max) {
        int current = getIntValue();
        this.minVal = min;
        this.maxVal = max;
        if (current > max) current = max;
        if (current < min) current = min;
        updateValue(current);
    }

    public void updateValue(int val) {
        if (maxVal <= minVal) {
            this.sliderValue = 0.0f;
        } else {
            this.sliderValue = (float)(val - minVal) / (float)(maxVal - minVal);
            this.sliderValue = Mth.clamp(this.sliderValue, 0.0f, 1.0f);
        }
        // ドラッグ中の差分判定基準を同期後の値に合わせる (TriggerUnitScreenのVerticalSliderと同様)
        this.lastSentValue = getIntValue();
    }

    public int getIntValue() { return minVal + Math.round(this.sliderValue * (maxVal - minVal)); }

    @Override public void onPress() {}

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) { isDragging = true; updateSliderFromMouse(mx); return true; }
        return false;
    }

    @Override public void onRelease(double mx, double my) {
        if (isDragging) {
            isDragging = false;
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundInit.WAVE_MOTION_GUN_SWITCH.get(), 1.0F));
            onUpdate.accept(getIntValue());
        }
    }

    @Override public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (isDragging) { updateSliderFromMouse(mx); return true; }
        return false;
    }

    private void updateSliderFromMouse(double mouseX) {
        double relativeX = mouseX - (this.getX() + 4);
        this.sliderValue = Mth.clamp((float)(relativeX / (double)(this.width - 8)), 0.0f, 1.0f);
        // 【最適化】int値が実際に変わった時だけ送信 (ドラッグ1ピクセル毎のパケット洪水を防ぐ)
        int newValue = getIntValue();
        if (newValue != this.lastSentValue) {
            this.lastSentValue = newValue;
            onUpdate.accept(newValue);
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(getX(), getY() + height / 2 - 2, getX() + width, getY() + height / 2 + 2, 0xFF222222);
        guiGraphics.fill(getX() + 1, getY() + height / 2 - 1, getX() + width - 1, getY() + height / 2 + 1, 0xFF000000);
        int knobX = getX() + (int)(this.sliderValue * (width - 8));
        guiGraphics.fill(knobX, getY(), knobX + 8, getY() + height, COLOR_STEEL_LIGHT);
        guiGraphics.fill(knobX + 1, getY() + 1, knobX + 7, getY() + height - 1, COLOR_STEEL_BG);
        if (isHoveredOrFocused() || isDragging) guiGraphics.fill(knobX, getY(), knobX + 8, getY() + height, 0x44FFFFFF);
        String unit = label.equals("Damage") ? "" : " m";
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, label + ": " + getIntValue() + unit, getX() + width / 2, getY() + (height - 8) / 2, WidgetColors.COLOR_TEXT_WHITE);
    }
}
