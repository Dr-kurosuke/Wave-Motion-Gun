package com.example.wave_motion_gun.client;

import com.example.wave_motion_gun.client.widget.FireButton;
import com.example.wave_motion_gun.client.widget.VerticalSwitch;
import com.example.wave_motion_gun.init.ItemInit;
import com.example.wave_motion_gun.init.SoundInit;
import com.example.wave_motion_gun.menu.ShockCannonMenu;
import com.example.wave_motion_gun.network.PacketHandler;
import com.example.wave_motion_gun.network.ShockCannonFirePacket;
import com.example.wave_motion_gun.network.ShockCannonUpdatePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ShockCannonScreen extends AbstractContainerScreen<ShockCannonMenu> {

    // Colors
    private static final int COLOR_STEEL_BG = 0xFF555555;
    private static final int COLOR_STEEL_LIGHT = 0xFF888888;
    private static final int COLOR_STEEL_DARK = 0xFF333333;

    private static final int COLOR_BORDER_GREEN = 0xFF55FF55;
    private static final int COLOR_BORDER_RED = 0xFFFF5555;
    private static final int COLOR_BORDER_BLUE = 0xFF5555FF;
    private static final int COLOR_TEXT_WHITE = 0xE6FFFFFF;

    private EditBox delayBox;

    public ShockCannonScreen(ShockCannonMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        int x = this.leftPos;
        int y = this.topPos;

        // 1. Slot: x=25

        // 2. FIRE Button (x=68, y=33)
        int fireBtnX = x + 68;
        int fireBtnY = y + 33;
        this.addRenderableWidget(new FireButton(fireBtnX, fireBtnY, 40, 22, new TextComponent("FIRE"), 1.0f, btn -> {
            // 【修正】BlockEntityが無効な場合はパケットを送らない
            if (isBlockEntityValid()) {
                PacketHandler.INSTANCE.sendToServer(new ShockCannonFirePacket(menu.blockEntity.getBlockPos()));
            }
        }));

        // 3. 右側にスイッチ (x=120〜)
        int swStartX = x + 120;
        int swY = y + 25;

        int swWidth = 12;
        int swHeight = 28;
        int gap = 16;

        // Green Switch
        this.addRenderableWidget(new VerticalSwitch(swStartX, swY, swWidth, swHeight, COLOR_BORDER_GREEN,
                this::isEnergyAmmo,
                () -> menu.getColorMode() == 0,
                (v) -> sendColorUpdate(0)));

        // Red Switch
        this.addRenderableWidget(new VerticalSwitch(swStartX + gap, swY, swWidth, swHeight, COLOR_BORDER_RED,
                this::isEnergyAmmo,
                () -> menu.getColorMode() == 1,
                (v) -> sendColorUpdate(1)));

        // Blue Switch
        this.addRenderableWidget(new VerticalSwitch(swStartX + gap * 2, swY, swWidth, swHeight, COLOR_BORDER_BLUE,
                this::isEnergyAmmo,
                () -> menu.getColorMode() == 2,
                (v) -> sendColorUpdate(2)));

        // ■ 4. Delay Setting (FIREボタンの下)
        int delayX = fireBtnX - 5;

        // ■ 修正: 高さ13pxに合わせてY座標を調整 (下辺の位置77付近を維持)
        // Y = 33 + 31 = 64
        // H = 13
        // 下辺 = 64 + 13 = 77
        int delayY = fireBtnY + 31;
        int delayH = 13;

        // 入力ボックス (数値のみ)
        this.delayBox = new EditBox(this.font, delayX, delayY, 30, delayH, new TextComponent("Delay"));
        this.delayBox.setValue(String.valueOf(menu.getFireDelay()));
        this.delayBox.setFilter(s -> s.matches("\\d*"));
        this.delayBox.setBordered(true);
        this.addRenderableWidget(delayBox);

        // 保存ボタン (小さな[S]ボタン)
        // ■ 修正: SmallButton を使用して、高さ13pxでも枠線を綺麗に描画する
        this.addRenderableWidget(new SmallButton(delayX + 32, delayY, 29, delayH, new TextComponent("save"), btn -> {
            try {
                if (delayBox.getValue() != null && !delayBox.getValue().isEmpty()) {
                    int val = Integer.parseInt(delayBox.getValue());
                    PacketHandler.INSTANCE.sendToServer(new ShockCannonUpdatePacket(menu.blockEntity.getBlockPos(), 1, val));
                    this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundInit.WAVE_MOTION_GUN_SWITCH.get(), 1.0F));
                }
            } catch (NumberFormatException ignored) {}
        }));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (this.delayBox != null && !this.delayBox.isFocused()) {
            String currentStr = this.delayBox.getValue();
            String serverStr = String.valueOf(menu.getFireDelay());
            if (!currentStr.equals(serverStr)) {
                this.delayBox.setValue(serverStr);
            }
        }
    }

    private boolean isBlockEntityValid() {
        return menu.blockEntity != null && !menu.blockEntity.isRemoved();
    }

    private void sendColorUpdate(int mode) {
        // 【修正】BlockEntityが無効な場合はパケットを送らない
        if (!isBlockEntityValid()) return;
        PacketHandler.INSTANCE.sendToServer(new ShockCannonUpdatePacket(menu.blockEntity.getBlockPos(), 0, mode));
    }

    private boolean isEnergyAmmo() {
        ItemStack stack = menu.getAmmo();
        return !stack.isEmpty() && stack.getItem() == ItemInit.SHOCK_CANNON_SHELL.get();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(poseStack, mouseX, mouseY);

        // ラベル描画
        int labelY = this.topPos + 15;
        int swStartX = this.leftPos + 120;
        int swWidth = 12;
        int gap = 16;
        float centerOffset = swWidth / 2.0f;

        drawCenteredScaledString(poseStack, "G", swStartX + centerOffset, labelY, 0xFF55FF55, 0.8f);
        drawCenteredScaledString(poseStack, "R", swStartX + gap + centerOffset, labelY, 0xFFFF5555, 0.8f);
        drawCenteredScaledString(poseStack, "B", swStartX + gap * 2 + centerOffset, labelY, 0xFF5555FF, 0.8f);

        // Delayラベル
        drawCenteredScaledString(poseStack, "Delay(tick)", this.leftPos + 83, this.topPos + 56, 0xFFCCCCCC, 0.7f);
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTicks, int mouseX, int mouseY) {
        drawSteelBox(poseStack, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

        int sx = this.leftPos + 24;
        int sy = this.topPos + 34;
        fill(poseStack, sx, sy, sx + 18, sy + 18, 0xFF222222);
        fill(poseStack, sx, sy, sx + 17, sy + 1, 0xFF333333);
        fill(poseStack, sx, sy, sx + 1, sy + 17, 0xFF333333);

        int pInvY = this.topPos + 83;
        fill(poseStack, this.leftPos + 7, pInvY, this.leftPos + 169, pInvY + 76, 0xFF444444);
    }

    private void drawSteelBox(PoseStack poseStack, int x, int y, int w, int h) {
        fill(poseStack, x, y, x + w, y + h, COLOR_STEEL_BG);
        fill(poseStack, x, y, x + w, y + 2, COLOR_STEEL_LIGHT);
        fill(poseStack, x, y, x + 2, y + h, COLOR_STEEL_LIGHT);
        fill(poseStack, x, y + h - 2, x + w, y + h, COLOR_STEEL_DARK);
        fill(poseStack, x + w - 2, y, x + w, y + h, COLOR_STEEL_DARK);
    }

    private void drawCenteredScaledString(PoseStack poseStack, String text, float x, float y, int color, float scale) {
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.scale(scale, scale, 1.0f);
        int w = this.font.width(text);
        this.font.draw(poseStack, text, -w / 2.0f, 0, color);
        poseStack.popPose();
    }

    // ■ 高さが低いボタンでも枠線を正しく描画するためのクラス
    private class SmallButton extends Button {
        public SmallButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress);
        }

        @Override
        public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
            Minecraft minecraft = Minecraft.getInstance();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);

            int i = this.getYImage(this.isHoveredOrFocused());
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();

            int texY = 46 + i * 20;
            int halfWidth = this.width / 2;
            int topHeight = this.height / 2;
            int bottomHeight = this.height - topHeight;

            // 左上
            blit(poseStack, this.x, this.y, halfWidth, topHeight, 0, texY, halfWidth, topHeight, 256, 256);
            // 右上
            blit(poseStack, this.x + halfWidth, this.y, this.width - halfWidth, topHeight, 200 - (this.width - halfWidth), texY, this.width - halfWidth, topHeight, 256, 256);

            // 左下 (テクスチャの下端から描画することで枠線を維持)
            blit(poseStack, this.x, this.y + topHeight, halfWidth, bottomHeight, 0, texY + 20 - bottomHeight, halfWidth, bottomHeight, 256, 256);
            // 右下
            blit(poseStack, this.x + halfWidth, this.y + topHeight, this.width - halfWidth, bottomHeight, 200 - (this.width - halfWidth), texY + 20 - bottomHeight, this.width - halfWidth, bottomHeight, 256, 256);

            int textColor = this.active ? COLOR_TEXT_WHITE : 0xFFAAAAAA;
            drawCenteredString(poseStack, minecraft.font, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, textColor);
        }
    }
}
