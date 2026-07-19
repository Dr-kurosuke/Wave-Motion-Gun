package com.example.wave_motion_gun.client.widget;

import net.minecraft.network.chat.Component;

import com.example.wave_motion_gun.init.SoundInit;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * ShockCannonScreenのカラー選択用縦スイッチ。
 * enabledSupplier: 操作可能条件 (エナジー弾が装填されているか)
 */
public class VerticalSwitch extends Button {
    private static final int COLOR_BORDER_OFF = 0xFF444444;

    private final int activeColor;
    private final Supplier<Boolean> enabledSupplier;
    private final Supplier<Boolean> stateSupplier;
    private final Consumer<Boolean> onToggle;

    public VerticalSwitch(int x, int y, int width, int height, int activeColor, Supplier<Boolean> enabledSupplier, Supplier<Boolean> stateSupplier, Consumer<Boolean> onToggle) {
        super(x, y, width, height, Component.empty(), btn -> {}, DEFAULT_NARRATION);
        this.activeColor = activeColor;
        this.enabledSupplier = enabledSupplier;
        this.stateSupplier = stateSupplier;
        this.onToggle = onToggle;
    }

    @Override
    public void onPress() {
        if (enabledSupplier.get()) {
            if (!stateSupplier.get()) {
                onToggle.accept(true);
                playDownSound(Minecraft.getInstance().getSoundManager());
            }
        }
    }

    @Override
    public void playDownSound(net.minecraft.client.sounds.SoundManager handler) {
        handler.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundInit.WAVE_MOTION_GUN_SWITCH.get(), 1.0F));
    }

    private void drawVSteps(GuiGraphics guiGraphics, int x, int y, int w, int h, int[] colors, boolean reverse) {
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

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        boolean ammoCondition = enabledSupplier.get();
        boolean isOn = ammoCondition && stateSupplier.get();
        int borderColor = isOn ? activeColor : COLOR_BORDER_OFF;
        if (!ammoCondition) borderColor = 0xFF222222;

        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, borderColor);
        guiGraphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0xFF000000);

        int padding = 2;
        int swX = getX() + padding;
        int swY = getY() + padding;
        int swW = width - padding * 2;
        int swH = height - padding * 2;
        int knobH = 8;
        int trackH = swH - knobH;

        if (isOn) {
            drawVSteps(guiGraphics, swX, swY, swW, knobH, WidgetColors.GRADIENT_LIGHT, false);
            guiGraphics.fill(swX, swY + knobH, swX + swW, swY + swH, WidgetColors.COLOR_GAP_BOTTOM);
        } else {
            guiGraphics.fill(swX, swY, swX + swW, swY + trackH, WidgetColors.COLOR_GAP_TOP);
            if (ammoCondition) {
                drawVSteps(guiGraphics, swX, swY + trackH, swW, knobH, WidgetColors.GRADIENT_DARK, true);
            } else {
                guiGraphics.fill(swX, swY + trackH, swX + swW, swY + trackH + knobH, 0xFF333333);
            }
        }
    }
}
