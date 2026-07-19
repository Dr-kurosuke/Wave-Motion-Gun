package com.example.wave_motion_gun.client.widget;

import net.minecraft.network.chat.Component;

import com.example.wave_motion_gun.init.SoundInit;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** 縦方向にノブが動くメカニカルトグルスイッチ */
public class MechanicalSwitch extends Button {
    public boolean blinking = false;
    private final Supplier<Boolean> stateSupplier;
    private final Consumer<Boolean> onToggle;

    public MechanicalSwitch(int x, int y, int width, int height, Supplier<Boolean> stateSupplier, Consumer<Boolean> onToggle) {
        super(x, y, width, height, Component.empty(), btn -> {}, DEFAULT_NARRATION);
        this.stateSupplier = stateSupplier;
        this.onToggle = onToggle;
    }

    @Override public void onPress() { if (this.active) onToggle.accept(!stateSupplier.get()); }

    // Sound Implementation: Use SWITCH sound
    @Override
    public void playDownSound(net.minecraft.client.sounds.SoundManager handler) {
        handler.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundInit.WAVE_MOTION_GUN_SWITCH.get(), 1.0F));
    }

    private void drawSteps(GuiGraphics guiGraphics, int x, int y, int w, int h, int[] colors, boolean reverse) {
        int steps = colors.length;
        float stepH = (float)h / steps;
        for (int i = 0; i < steps; i++) {
            int color = reverse ? colors[steps - 1 - i] : colors[i];
            int y1 = y + (int)(i * stepH);
            int y2 = y + (int)((i + 1) * stepH);
            if (i == steps - 1) y2 = y + h;
            guiGraphics.fill(x, y1, x + w, y2, color);
        }
    }

    @Override public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        boolean isOn = stateSupplier.get();
        // 【重要】枠線色を操作可能性(this.active)で判定
        int borderColor = WidgetColors.getGuidanceColor(this.active, this.blinking);
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, borderColor);
        guiGraphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0xFF000000);
        int padding = 2; int swX = getX() + padding; int swY = getY() + padding; int swW = width - padding * 2; int swH = height - padding * 2; float unit = swH / 10.0f;
        if (isOn) {
            int knobH = (int)(unit * 8);
            drawSteps(guiGraphics, swX, swY, swW, knobH / 2, WidgetColors.GRADIENT_LIGHT, false);
            drawSteps(guiGraphics, swX, swY + knobH / 2, swW, knobH / 2, WidgetColors.GRADIENT_DARK, true);
            guiGraphics.fill(swX, swY + knobH, swX + swW, swY + swH, WidgetColors.COLOR_GAP_BOTTOM);
        } else {
            int gapH = (int)(unit * 2);
            int knobH = swH - gapH;
            guiGraphics.fill(swX, swY, swX + swW, swY + gapH, WidgetColors.COLOR_GAP_TOP);
            drawSteps(guiGraphics, swX, swY + gapH, swW, knobH / 2, WidgetColors.GRADIENT_LIGHT, false);
            drawSteps(guiGraphics, swX, swY + gapH + knobH / 2, swW, knobH / 2, WidgetColors.GRADIENT_DARK, true);
        }
    }
}
