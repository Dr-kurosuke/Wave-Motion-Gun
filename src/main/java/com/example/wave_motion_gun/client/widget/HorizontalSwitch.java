package com.example.wave_motion_gun.client.widget;

import com.example.wave_motion_gun.init.SoundInit;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.TextComponent;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** 横方向にノブが動くメカニカルトグルスイッチ (TriggerUnitScreen / MonitoringUnitScreen 共用) */
public class HorizontalSwitch extends Button {
    public boolean blinking = false;
    private final Supplier<Boolean> stateSupplier;
    private final Consumer<Boolean> onToggle;

    public HorizontalSwitch(int x, int y, int width, int height, Supplier<Boolean> stateSupplier, Consumer<Boolean> onToggle) {
        super(x, y, width, height, TextComponent.EMPTY, btn -> {});
        this.stateSupplier = stateSupplier;
        this.onToggle = onToggle;
    }

    @Override public void onPress() { if (this.active) onToggle.accept(!stateSupplier.get()); }

    // Sound Implementation: Use SWITCH sound
    @Override
    public void playDownSound(net.minecraft.client.sounds.SoundManager handler) {
        handler.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundInit.WAVE_MOTION_GUN_SWITCH.get(), 1.0F));
    }

    private void drawHSteps(PoseStack poseStack, int x, int y, int w, int h, int[] colors, boolean reverse) {
        int steps = colors.length;
        float stepW = (float)w / steps;
        for (int i = 0; i < steps; i++) {
            int color = reverse ? colors[steps - 1 - i] : colors[i];
            int x1 = x + (int)(i * stepW);
            int x2 = x + (int)((i + 1) * stepW);
            if (i == steps - 1) x2 = x + w;
            fill(poseStack, x1, y, x2, y + h, color);
        }
    }

    @Override public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        boolean isOn = stateSupplier.get();
        // 【重要】枠線色を操作可能性(this.active)で判定
        int borderColor = WidgetColors.getGuidanceColor(this.active, this.blinking);
        fill(poseStack, x, y, x + width, y + height, borderColor);
        fill(poseStack, x + 1, y + 1, x + width - 1, y + height - 1, 0xFF000000);
        int padding = 2; int swX = x + padding; int swY = y + padding; int swW = width - padding * 2; int swH = height - padding * 2; float unit = swW / 10.0f;
        if (isOn) {
            int gapW = (int)(unit * 2);
            int knobW = swW - gapW;
            fill(poseStack, swX, swY, swX + gapW, swY + swH, WidgetColors.COLOR_GAP_TOP);
            drawHSteps(poseStack, swX + gapW, swY, knobW / 2, swH, WidgetColors.GRADIENT_DARK, false);
            drawHSteps(poseStack, swX + gapW + knobW / 2, swY, knobW / 2, swH, WidgetColors.GRADIENT_LIGHT, true);
        } else {
            int knobW = (int)(unit * 8);
            drawHSteps(poseStack, swX, swY, knobW / 2, swH, WidgetColors.GRADIENT_DARK, false);
            drawHSteps(poseStack, swX + knobW / 2, swY, knobW / 2, swH, WidgetColors.GRADIENT_LIGHT, true);
            fill(poseStack, swX + knobW, swY, swX + swW, swY + swH, WidgetColors.COLOR_GAP_BOTTOM);
        }
    }
}
