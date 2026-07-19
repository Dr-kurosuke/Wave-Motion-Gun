package com.example.wave_motion_gun.client.widget;

import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * 0〜15段階の縦スライダー (照度調整用)。
 * levelSupplier: サーバー同期済みの現在レベル (0-15)
 * onLevelChanged: ドラッグ中にレベルが変化した時に呼ばれる (パケット送信用)
 * onReleased: ドラッグを離した時に呼ばれる (アナウンス用)
 */
public class VerticalSlider extends Button {
    private boolean isDragging = false;
    private float sliderValue = 0.0f;
    private double clickOffsetY = 0.0;
    private int lastLevel = -1;

    private final IntSupplier levelSupplier;
    private final Consumer<Integer> onLevelChanged;
    private final Consumer<Integer> onReleased;

    public VerticalSlider(int x, int y, int width, int height, IntSupplier levelSupplier, Consumer<Integer> onLevelChanged, Consumer<Integer> onReleased) {
        super(x, y, width, height, Component.empty(), btn -> {}, DEFAULT_NARRATION);
        this.levelSupplier = levelSupplier;
        this.onLevelChanged = onLevelChanged;
        this.onReleased = onReleased;
    }

    @Override public void onPress() { }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            this.isDragging = true;
            double knobHeight = 6.0;
            double trackHeight = this.height - knobHeight;
            double currentKnobY = this.getY() + (1.0 - this.sliderValue) * trackHeight;
            this.clickOffsetY = mouseY - currentKnobY;
            return true;
        }
        return false;
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        if (this.isDragging) {
            this.isDragging = false;
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            // Trigger 7: Contrast Slider Released
            int currentLevel = Math.round(this.sliderValue * 15.0f);
            this.onReleased.accept(currentLevel);
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (this.isDragging) {
            double knobHeight = 6.0;
            double trackHeight = this.height - knobHeight;
            double targetKnobY = mouseY - this.clickOffsetY;
            double rawVal = 1.0 - (targetKnobY - this.getY()) / trackHeight;
            this.sliderValue = (float) Mth.clamp(rawVal, 0.0, 1.0);

            int currentLevel = Math.round(this.sliderValue * 15.0f);
            if (currentLevel != this.lastLevel) {
                this.lastLevel = currentLevel;
                this.onLevelChanged.accept(currentLevel);
            }
        } else {
            int sLight = this.levelSupplier.getAsInt();
            this.sliderValue = Mth.clamp(sLight / 15.0f, 0.0f, 1.0f);
            this.lastLevel = Math.round(this.sliderValue * 15.0f);
        }

        guiGraphics.fill(getX() + width / 2 - 2, getY(), getX() + width / 2 + 2, getY() + height, 0xFF222222);
        guiGraphics.fill(getX() + width / 2 - 1, getY() + 1, getX() + width / 2 + 1, getY() + height - 1, 0xFF000000);

        int knobHeight = 6;
        int knobWidth = width - 4;
        int knobY = getY() + (int)((1.0f - this.sliderValue) * (height - knobHeight));
        int knobX = getX() + 2;

        guiGraphics.fill(knobX, knobY, knobX + knobWidth, knobY + knobHeight, 0xFF888888);
        guiGraphics.fill(knobX, knobY, knobX + knobWidth, knobY + 1, 0xFFAAAAAA);
        guiGraphics.fill(knobX, knobY + knobHeight - 1, knobX + knobWidth, knobY + knobHeight, 0xFF444444);

        if (this.isHoveredOrFocused() || isDragging) {
            guiGraphics.fill(knobX, knobY, knobX + knobWidth, knobY + knobHeight, 0x44FFFFFF);
        }
    }
}
