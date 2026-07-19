package com.example.wave_motion_gun.client.widget;

import net.minecraft.network.chat.Component;

import com.example.wave_motion_gun.init.SoundInit;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** ドラッグで上下に操作する注入レバー */
public class InjectorLever extends Button {
    public boolean blinking = false;
    private final Supplier<Boolean> stateSupplier;
    private final Consumer<Boolean> onStateChange;

    // 外部(TriggerUnitScreen)からアクセスできるよう可視性を確保
    public boolean isDragging = false;

    private float visualPos = 0.0f;
    private long lastActionTime = 0;
    private boolean predictedState = false;

    public InjectorLever(int x, int y, int width, int height, Supplier<Boolean> stateSupplier, Consumer<Boolean> onStateChange) {
        super(x, y, width, height, Component.empty(), btn -> {}, DEFAULT_NARRATION);
        this.stateSupplier = stateSupplier;
        this.onStateChange = onStateChange;
        this.visualPos = stateSupplier.get() ? 1.0f : 0.0f;
    }

    @Override public void onPress() { if (this.active) this.isDragging = true; }

    @Override
    public void playDownSound(net.minecraft.client.sounds.SoundManager handler) {
        handler.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundInit.WAVE_MOTION_GUN_GRIP.get(), 1.0F));
    }

    private void playSound(SoundEvent sound) {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(sound, 1.0F));
    }

    // TriggerUnitScreenから直接呼ばれるため、ここで全ての判定を行う
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isDragging) {
            this.isDragging = false;

            // リリース時の座標で位置を確定
            double relY = mouseY - this.getY();
            float rawPos = 1.0f - (float)(relY / (double)this.height);
            this.visualPos = Mth.clamp(rawPos, 0.0f, 1.0f);

            // 判定ロジック
            if (this.visualPos > 0.8f) {
                onStateChange.accept(true);
                playSound(SoundInit.WAVE_MOTION_GUN_LEVER.get());
                this.visualPos = 1.0f;
                this.predictedState = true;
                this.lastActionTime = System.currentTimeMillis();
            } else if (this.visualPos < 0.2f) {
                onStateChange.accept(false);
                playSound(SoundInit.WAVE_MOTION_GUN_LEVER.get());
                this.visualPos = 0.0f;
                this.predictedState = false;
                this.lastActionTime = System.currentTimeMillis();
            }
            return true; // イベント消費
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // onReleaseは不要なのでオーバーライドしない、または空にする

    @Override public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!isDragging) {
            boolean actual = stateSupplier.get();
            float target = actual ? 1.0f : 0.0f;
            long now = System.currentTimeMillis();
            if (now - lastActionTime < 1000 && predictedState != actual) {
                target = predictedState ? 1.0f : 0.0f;
            }
            visualPos = Mth.lerp(0.4f, visualPos, target);
        } else {
            double relY = mouseY - this.getY();
            float rawPos = 1.0f - (float)(relY / (double)this.height);
            visualPos = Mth.clamp(rawPos, 0.0f, 1.0f);
        }

        int borderColor = WidgetColors.getGuidanceColor(this.active, this.blinking);
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, borderColor);
        guiGraphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0xFF000000);

        int innerX = getX() + 1;
        int innerY = getY() + 1;
        int innerW = width - 2;
        int innerH = height - 2;

        guiGraphics.fill(innerX, innerY, innerX + innerW, innerY + innerH, 0xFF444444);
        int slitW = innerW / 3; int slitX = innerX + (innerW - slitW) / 2;
        guiGraphics.fill(slitX, innerY + 2, slitX + slitW, innerY + innerH - 2, 0xFF222222);

        int handleH = 10; int handleW = innerW - 4;
        int travelDist = innerH - handleH - 4;
        int handleY = (int) (innerY + 2 + (1.0f - visualPos) * travelDist);
        int handleX = innerX + 2;

        guiGraphics.fill(handleX, handleY, handleX + handleW, handleY + handleH, 0xFF888888);
        guiGraphics.fill(handleX, handleY, handleX + handleW, handleY + 3, 0xFFAAAAAA);
        guiGraphics.fill(handleX, handleY + handleH - 3, handleX + handleW, handleY + handleH, 0xFF555555);

        if (this.isHoveredOrFocused() || isDragging) {
            guiGraphics.fill(handleX, handleY, handleX + handleW, handleY + handleH, 0x44FFFFFF);
        }
    }
}
