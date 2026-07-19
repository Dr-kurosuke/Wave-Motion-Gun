package com.example.wave_motion_gun.client;

import com.example.wave_motion_gun.client.widget.HorizontalSlider;
import com.example.wave_motion_gun.client.widget.HorizontalSwitch;
import com.example.wave_motion_gun.init.SoundInit;
import com.example.wave_motion_gun.menu.MonitoringUnitMenu;
import com.example.wave_motion_gun.network.MonitoringUnitUpdatePacket;
import com.example.wave_motion_gun.network.PacketHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

public class MonitoringUnitScreen extends VirtualScaledScreen<MonitoringUnitMenu> {

    // --- Visual Constants ---
    private static final int COLOR_HOLO_FRAME = 0xB300FFFF;
    private static final int COLOR_HOLO_TEXT = 0xB300FFFF;

    private static final int COLOR_TEXT_WHITE = 0xE6FFFFFF;
    private static final int COLOR_TEXT_GREEN = 0xE655FF55;
    private static final int COLOR_TEXT_RED = 0xE6FF5555;
    private static final int COLOR_TEXT_GRAY = 0xFFAAAAAA;
    private static final int COLOR_TEXT_YELLOW = 0xE6FFAA00;

    // --- Widgets ---
    // [Top Area]
    private EditBox frequencyBox;
    private EditBox innerColorBox;
    private EditBox outerColorBox;

    // [Bottom Area]
    private HorizontalSlider rangeSlider;
    private HorizontalSlider radiusSlider;
    private HorizontalSlider damageSlider;

    private HorizontalSwitch destructiveSwitch;
    private HorizontalSwitch safetySwitch;

    // --- State ---
    private long lastUiUpdateTime = 0;

    // --- 文字列キャッシュ (毎フレームの連結/String.formatを回避) ---
    private int cachedBarrelCount = Integer.MIN_VALUE;
    private String cachedBarrelsStr = "";
    private int cachedTachyonCount = Integer.MIN_VALUE;
    private String cachedTachyonStr = "";
    private Boolean cachedHasChamber = null;
    private String cachedChamberStr = "";
    private int cachedMaxRangeLimit = Integer.MIN_VALUE;
    private String cachedRangeLimitStr = "";
    private int cachedMaxRadiusLimit = Integer.MIN_VALUE;
    private String cachedRadiusLimitStr = "";
    private int cachedMaxDamageLimit = Integer.MIN_VALUE;
    private String cachedDmgLimitStr = "";
    private Boolean cachedHasAdjuster = null;
    private String cachedAdjusterStr = "";
    private Boolean cachedDestructive = null;
    private String cachedModeStr = "";
    private Boolean cachedSafety = null;
    private String cachedSafetyStr = "";
    private long cachedEnergy = Long.MIN_VALUE;
    private long cachedMaxEnergy = Long.MIN_VALUE;
    private String cachedEnergyStr = "";

    public MonitoringUnitScreen(MonitoringUnitMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    private void playSound(SoundEvent sound) {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(sound, 1.0F));
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        Minecraft.getInstance().options.hideGui = true;

        // Calculate Scale
        computeGlobalScale();

        // Initial Values
        int currentFreq = menu.getFrequency();
        int currentRange = menu.getCurrentRange();
        int currentRadius = menu.getCurrentRadius();
        int maxRange = menu.getMaxRangeLimit();
        int maxRadius = menu.getMaxRadiusLimit();
        int currentDamage = menu.blockEntity.damageValue;
        int maxDamage = menu.getMaxDamageLimit();

        String innerHex = Integer.toHexString(menu.blockEntity.innerColor).toUpperCase();
        String outerHex = Integer.toHexString(menu.blockEntity.outerColor).toUpperCase();

        // --- Layout Strategy ---
        // Top Band (Y: 10-50): Freq, Colors
        // Holo Panels (Y: 60-130): 3 Panels
        // Energy Bar (Y: 140-160): Thick Bar
        // Sliders (Y: 170-240): Controls
        // Switches (Y: 250-280): Controls

        // 1. Top Area: Frequency & Colors
        int topY = 25;

        // Freq
        this.frequencyBox = new EditBox(this.font, 40, topY, 50, 20, Component.literal("Freq"));
        this.frequencyBox.setValue(String.valueOf(currentFreq));
        // 上限は FrequencyManager.MAX_FREQUENCY (16bit同期の制約)。超過入力は受け付けない
        // 先に桁数で弾くことで parseInt のオーバーフロー/例外を防ぐ (MAX_FREQUENCY は5桁)
        this.frequencyBox.setFilter(s -> s.matches("\\d*") && s.length() <= 5
                && (s.isEmpty() || Integer.parseInt(s) <= com.example.wave_motion_gun.utils.FrequencyManager.MAX_FREQUENCY));
        this.frequencyBox.setBordered(true);
        this.addRenderableWidget(this.frequencyBox);

        this.addRenderableWidget(Button.builder(Component.literal("SET"), btn -> {
            sendUpdatePacket(null, null);
            playSound(SoundInit.WAVE_MOTION_GUN_SWITCH.get());
        }).bounds(95, topY, 30, 20).build());

        // Colors
        int colorX = 280;
        this.innerColorBox = new EditBox(this.font, colorX, topY, 60, 20, Component.literal("Inner"));
        this.innerColorBox.setValue(innerHex);
        this.innerColorBox.setMaxLength(6);
        this.addRenderableWidget(this.innerColorBox);

        this.outerColorBox = new EditBox(this.font, colorX + 70, topY, 60, 20, Component.literal("Outer"));
        this.outerColorBox.setValue(outerHex);
        this.outerColorBox.setMaxLength(6);
        this.addRenderableWidget(this.outerColorBox);

        // --- 追加: カラー設定用のSETボタン ---
        // OuterBoxの右側 (X: 280 + 70 + 60 + 5 = 435) に配置
        this.addRenderableWidget(Button.builder(Component.literal("SET"), btn -> {
            // テキストボックスの内容を読み取ってパケットを送信
            sendUpdatePacket(null, null);
            playSound(SoundInit.WAVE_MOTION_GUN_SWITCH.get());
        }).bounds(colorX + 135, topY, 30, 20).build());


        // 2. Bottom Area: Sliders & Switches
        int sliderStartX = 110;
        int sliderStartY = 180;
        int sliderGap = 25;
        int sliderW = 275;

        this.rangeSlider = new HorizontalSlider(sliderStartX, sliderStartY, sliderW, 20, "Range", 1, maxRange, currentRange, (val) -> sendUpdatePacket(null, null));
        this.addRenderableWidget(this.rangeSlider);

        this.radiusSlider = new HorizontalSlider(sliderStartX, sliderStartY + sliderGap, sliderW, 20, "Radius", 1, maxRadius, currentRadius, (val) -> sendUpdatePacket(null, null));
        this.addRenderableWidget(this.radiusSlider);

        this.damageSlider = new HorizontalSlider(sliderStartX, sliderStartY + sliderGap * 2, sliderW, 20, "Damage", 0, maxDamage, currentDamage, (val) -> sendUpdatePacket(null, null));
        this.addRenderableWidget(this.damageSlider);

        // Switches (Bottom Flanks)
        int switchY = 260;

        // Destructive (Left Bottom)
        this.destructiveSwitch = new HorizontalSwitch(50, switchY, 40, 20,
                () -> menu.blockEntity.isDestructive,
                (val) -> sendUpdatePacket(!menu.blockEntity.isDestructive, null));
        this.addRenderableWidget(this.destructiveSwitch);

        // Safety (Right Bottom)
        this.safetySwitch = new HorizontalSwitch(400, switchY, 40, 20,
                () -> menu.blockEntity.isSafetyDisabled,
                (val) -> sendUpdatePacket(null, !menu.blockEntity.isSafetyDisabled));
        this.addRenderableWidget(this.safetySwitch);

        updateWidgetStates();
    }

    @Override
    public void removed() {
        super.removed();
        Minecraft.getInstance().options.hideGui = false;
    }

    private void sendUpdatePacket(Boolean overrideDestructive, Boolean overrideSafety) {
        int freq = menu.getFrequency();
        try { if (!frequencyBox.getValue().isEmpty()) freq = Integer.parseInt(frequencyBox.getValue()); } catch (Exception ignored) {}

        int damage = (damageSlider != null) ? damageSlider.getIntValue() : menu.blockEntity.damageValue;
        boolean dest = (overrideDestructive != null) ? overrideDestructive : menu.blockEntity.isDestructive;
        boolean safety = (overrideSafety != null) ? overrideSafety : menu.blockEntity.isSafetyDisabled;

        int inner = menu.blockEntity.innerColor;
        try { inner = Integer.parseInt(innerColorBox.getValue(), 16); } catch (Exception ignored) {}
        int outer = menu.blockEntity.outerColor;
        try { outer = Integer.parseInt(outerColorBox.getValue(), 16); } catch (Exception ignored) {}

        int rng = (rangeSlider != null) ? rangeSlider.getIntValue() : menu.getCurrentRange();
        int rad = (radiusSlider != null) ? radiusSlider.getIntValue() : menu.getCurrentRadius();

        PacketHandler.INSTANCE.sendToServer(new MonitoringUnitUpdatePacket(
                menu.blockEntity.getBlockPos(),
                freq, rng, rad,
                damage, dest, inner, outer, safety
        ));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        updateWidgetStates();

        // Sync Logic
        syncEditBox(this.frequencyBox, menu.getFrequency());
        if (this.innerColorBox != null && !this.innerColorBox.isFocused()) {
            String current = this.innerColorBox.getValue();
            String server = Integer.toHexString(menu.blockEntity.innerColor).toUpperCase();
            if (!current.equalsIgnoreCase(server)) this.innerColorBox.setValue(server);
        }
        if (this.outerColorBox != null && !this.outerColorBox.isFocused()) {
            String current = this.outerColorBox.getValue();
            String server = Integer.toHexString(menu.blockEntity.outerColor).toUpperCase();
            if (!current.equalsIgnoreCase(server)) this.outerColorBox.setValue(server);
        }

        if (this.rangeSlider != null && !this.rangeSlider.isDragging) {
            int menuVal = menu.getCurrentRange();
            if (this.rangeSlider.getIntValue() != menuVal) this.rangeSlider.updateValue(menuVal);
        }
        if (this.radiusSlider != null && !this.radiusSlider.isDragging) {
            int menuVal = menu.getCurrentRadius();
            if (this.radiusSlider.getIntValue() != menuVal) this.radiusSlider.updateValue(menuVal);
        }
        if (this.damageSlider != null && !this.damageSlider.isDragging) {
            int menuVal = menu.blockEntity.damageValue;
            if (this.damageSlider.getIntValue() != menuVal) this.damageSlider.updateValue(menuVal);
        }
    }

    private void updateWidgetStates() {
        int maxR = menu.getMaxRangeLimit();
        int maxRad = menu.getMaxRadiusLimit();

        if (this.rangeSlider != null) this.rangeSlider.updateLimits(1, maxR);
        if (this.radiusSlider != null) this.radiusSlider.updateLimits(1, maxRad);

        int maxDmg = menu.getMaxDamageLimit();
        if (this.damageSlider != null) this.damageSlider.updateLimits(0, maxDmg);

        if (this.destructiveSwitch != null) {
            this.destructiveSwitch.active = menu.hasChamber();
        }

        if (this.innerColorBox != null && this.outerColorBox != null) {
            boolean hasAdjuster = menu.hasAdjuster();
            this.innerColorBox.setVisible(hasAdjuster);
            this.outerColorBox.setVisible(hasAdjuster);
        }
    }

    private void syncEditBox(EditBox box, int serverVal) {
        if (box != null && !box.isFocused()) {
            String currentStr = box.getValue();
            String serverStr = String.valueOf(serverVal);
            if (!currentStr.equals(serverStr)) {
                box.setValue(serverStr);
            }
        }
    }

    @Override
    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // ドラッグ中のスライダーには変換済み座標を直接渡す (dragX/dragYはスライダー側で未使用)
        if (this.rangeSlider != null && this.rangeSlider.isDragging) return this.rangeSlider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (this.radiusSlider != null && this.radiusSlider.isDragging) return this.radiusSlider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (this.damageSlider != null && this.damageSlider.isDragging) return this.damageSlider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return super.mouseDraggedScaled(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        PoseStack poseStack = guiGraphics.pose();
        long now = System.currentTimeMillis();
        if (lastUiUpdateTime == 0) lastUiUpdateTime = now;
        float dt = (now - lastUiUpdateTime) / 1000.0f;
        lastUiUpdateTime = now;
        uiOpenProgress += dt * 3.0f;
        uiOpenProgress = Mth.clamp(uiOpenProgress, 0.0f, 1.0f);

        this.renderBackground(guiGraphics);

        poseStack.pushPose();
        poseStack.translate(globalOffsetX, globalOffsetY, 0);
        poseStack.scale(globalScale, globalScale, 1.0f);

        int virtualMouseX = (int) transformMouseX(mouseX);
        int virtualMouseY = (int) transformMouseY(mouseY);

        // --- 1. Background & Frames ---
        // Top Frame (Freq, Colors)
        drawSteelBox(guiGraphics, 20, 10, 455, 45, true);
        drawScaledString(guiGraphics, "Frequency", 40, 15, COLOR_TEXT_WHITE, 0.8f);
        boolean hasAdjuster = menu.hasAdjuster();
        drawScaledString(guiGraphics, hasAdjuster ? "Beam Colors (Hex)" : "Colors (Req. Adjuster)", 300, 15, hasAdjuster ? COLOR_TEXT_WHITE : COLOR_TEXT_GRAY, 0.8f);

        // Bottom Frame (Sliders)
        drawSteelBox(guiGraphics, 20, 170, 455, 120, true);

        // --- 2. Holo Panels (Center) ---
        int holoY = 65;
        int holoH = 70;
        int panelGap = 10;
        int panelW = 145;

        // Panel 1: Structure Status (Left)
        // 各表示文字列は値が変わった時だけ作り直す (毎フレームの連結を回避)
        int p1x = 20;
        drawHoloPanel(guiGraphics, p1x, holoY, panelW, holoH, "STRUCTURE");
        int lineY = holoY + 20;
        int barrelCount = menu.getBarrelCount();
        if (barrelCount != cachedBarrelCount) { cachedBarrelCount = barrelCount; cachedBarrelsStr = "Barrels: " + barrelCount; }
        drawScaledString(guiGraphics, cachedBarrelsStr, p1x + 10, lineY, COLOR_HOLO_TEXT, 1.0f);
        lineY += 12;
        int tCount = menu.getTachyonCount();
        if (tCount != cachedTachyonCount) { cachedTachyonCount = tCount; cachedTachyonStr = "Tachyon: " + tCount + "/2"; }
        drawScaledString(guiGraphics, cachedTachyonStr, p1x + 10, lineY, (tCount > 0) ? COLOR_TEXT_GREEN : COLOR_TEXT_GRAY, 1.0f);
        lineY += 12;
        boolean hasChamber = menu.hasChamber();
        if (cachedHasChamber == null || cachedHasChamber != hasChamber) { cachedHasChamber = hasChamber; cachedChamberStr = "Chamber: " + (hasChamber ? "INSTALLED" : "NONE"); }
        drawScaledString(guiGraphics, cachedChamberStr, p1x + 10, lineY, hasChamber ? COLOR_TEXT_GREEN : COLOR_TEXT_GRAY, 1.0f);

        // Panel 2: Ballistics Status (Center)
        int p2x = p1x + panelW + panelGap;
        drawHoloPanel(guiGraphics, p2x, holoY, panelW, holoH, "BALLISTICS");
        lineY = holoY + 20;
        int maxRangeLimit = menu.getMaxRangeLimit();
        if (maxRangeLimit != cachedMaxRangeLimit) { cachedMaxRangeLimit = maxRangeLimit; cachedRangeLimitStr = "Range Limit: " + maxRangeLimit + "m"; }
        drawScaledString(guiGraphics, cachedRangeLimitStr, p2x + 10, lineY, COLOR_HOLO_TEXT, 1.0f);
        lineY += 12;
        int maxRadiusLimit = menu.getMaxRadiusLimit();
        if (maxRadiusLimit != cachedMaxRadiusLimit) { cachedMaxRadiusLimit = maxRadiusLimit; cachedRadiusLimitStr = "Radius Limit: " + maxRadiusLimit + "m"; }
        drawScaledString(guiGraphics, cachedRadiusLimitStr, p2x + 10, lineY, COLOR_HOLO_TEXT, 1.0f);
        lineY += 12;
        int maxDamageLimit = menu.getMaxDamageLimit();
        if (maxDamageLimit != cachedMaxDamageLimit) { cachedMaxDamageLimit = maxDamageLimit; cachedDmgLimitStr = "Dmg Limit: " + maxDamageLimit; }
        drawScaledString(guiGraphics, cachedDmgLimitStr, p2x + 10, lineY, COLOR_HOLO_TEXT, 1.0f);

        // Panel 3: System Status (Right)
        int p3x = p2x + panelW + panelGap;
        drawHoloPanel(guiGraphics, p3x, holoY, panelW, holoH, "SYSTEM");
        lineY = holoY + 20;
        if (cachedHasAdjuster == null || cachedHasAdjuster != hasAdjuster) { cachedHasAdjuster = hasAdjuster; cachedAdjusterStr = "Adjuster: " + (hasAdjuster ? "ONLINE" : "OFFLINE"); }
        drawScaledString(guiGraphics, cachedAdjusterStr, p3x + 10, lineY, hasAdjuster ? COLOR_TEXT_GREEN : COLOR_TEXT_GRAY, 1.0f);
        lineY += 12;
        boolean destructive = menu.blockEntity.isDestructive;
        if (cachedDestructive == null || cachedDestructive != destructive) { cachedDestructive = destructive; cachedModeStr = "Mode: " + (destructive ? "DESTRUCTIVE" : "NON-LETHAL"); }
        drawScaledString(guiGraphics, cachedModeStr, p3x + 10, lineY, destructive ? COLOR_TEXT_RED : COLOR_TEXT_GREEN, 1.0f);
        lineY += 12;
        boolean safety = menu.blockEntity.isSafetyDisabled;
        if (cachedSafety == null || cachedSafety != safety) { cachedSafety = safety; cachedSafetyStr = "Safety: " + (safety ? "RELEASED" : "ACTIVE"); }
        drawScaledString(guiGraphics, cachedSafetyStr, p3x + 10, lineY, safety ? COLOR_TEXT_YELLOW : COLOR_TEXT_GREEN, 1.0f);

        // --- 3. Energy Bar (The Core Feature) ---
        int barY = 145;
        int barH = 20;
        int barX = 20;
        int barTotalW = 455;

        drawEnergyBar(guiGraphics, barX, barY, barTotalW, barH);

        // Energy Text Overlay
        long en = menu.getStoredEnergy();
        long maxEn = menu.getMaxEnergy();
        if (en != cachedEnergy || maxEn != cachedMaxEnergy) {
            cachedEnergy = en;
            cachedMaxEnergy = maxEn;
            cachedEnergyStr = String.format("ENERGY: %,d / %,d FE", en, maxEn);
        }
        drawScaledCenteredStringNoShadow(guiGraphics, cachedEnergyStr, VIRTUAL_WIDTH / 2, barY + 6, COLOR_TEXT_WHITE, 1.0f);

        // --- 4. Render Widgets ---
        // Labels for bottom switches
        drawScaledString(guiGraphics, "Destructive Mode", 95, 266, COLOR_TEXT_WHITE, 1.0f);
        if (!menu.hasChamber()) drawScaledString(guiGraphics, "(Req. Chamber)", 95, 276, COLOR_TEXT_RED, 0.7f);

        drawScaledString(guiGraphics, "Safety Release", 330, 266, COLOR_TEXT_WHITE, 1.0f);

        super.render(guiGraphics, virtualMouseX, virtualMouseY, partialTicks);
        this.renderTooltip(guiGraphics, virtualMouseX, virtualMouseY);
        poseStack.popPose();
    }

    // --- Custom Render Methods ---

    private void drawEnergyBar(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        // Draw Background Strip
        guiGraphics.fill(x, y, x + w, y + h, 0xFF111111);

        long energy = menu.getStoredEnergy();
        long maxEnergy = menu.getMaxEnergy();
        double ratio = (double) energy / Math.max(1, maxEnergy);

        int segmentCount = 60;
        float gap = 2.0f;
        float segmentW = (w - (gap * (segmentCount - 1))) / segmentCount;

        int activeSegments = (int)(ratio * segmentCount);

        for (int i = 0; i < segmentCount; i++) {
            float sx = x + i * (segmentW + gap);
            float ex = sx + segmentW;

            int color;
            if (i < activeSegments) {
                // Active Gradient (Green to Blue or Solid Green)
                color = (ratio > 0.95) ? 0xFF55FF55 : 0xFF00AAFF; // Full=Green, Charging=Blue
                // Slight flash effect
                if (ratio >= 1.0 && (System.currentTimeMillis() / 200) % 2 == 0) {
                    color = 0xFFCCFFCC;
                }
            } else {
                color = 0xFF222222; // Inactive
            }

            guiGraphics.fill((int)sx, y + 2, (int)ex, y + h - 2, color);
        }

        // Frame
        int border = 0xFF444444;
        guiGraphics.fill(x, y, x + w, y + 1, border);
        guiGraphics.fill(x, y + h - 1, x + w, y + h, border);
    }

    private void drawHoloPanel(GuiGraphics guiGraphics, int x, int y, int w, int h, String title) {
        if (uiOpenProgress < 0.1f) return;

        // Holographic Background
        guiGraphics.fill(x, y, x + w, y + h, COLOR_HOLO_BG);

        // Frame
        guiGraphics.fill(x, y, x + w, y + 1, COLOR_HOLO_FRAME); // Top
        guiGraphics.fill(x, y + h - 1, x + w, y + h, COLOR_HOLO_FRAME); // Bottom
        guiGraphics.fill(x, y, x + 1, y + h, COLOR_HOLO_FRAME); // Left
        guiGraphics.fill(x + w - 1, y, x + w, y + h, COLOR_HOLO_FRAME); // Right

        // Title Box
        guiGraphics.fill(x, y, x + w, y + 12, COLOR_HOLO_FRAME);
        drawScaledCenteredStringNoShadow(guiGraphics, title, x + w / 2, y + 2, 0xFF000033, 0.9f);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.player.closeContainer();
            // 【修正】closeContainer後にイベントを消費して二重クローズを防ぐ
            return true;
        }
        if (this.frequencyBox.keyPressed(keyCode, scanCode, modifiers) ||
                this.innerColorBox.keyPressed(keyCode, scanCode, modifiers) ||
                this.outerColorBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override protected void renderBg(GuiGraphics p, float pt, int x, int y) {}
    @Override protected void renderLabels(GuiGraphics p, int x, int y) {}
}
