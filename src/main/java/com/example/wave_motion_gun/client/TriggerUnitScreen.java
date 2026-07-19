package com.example.wave_motion_gun.client;

import com.example.wave_motion_gun.blockentity.WaveCannonBlockEntity;
import com.example.wave_motion_gun.compat.VSCompat;
import com.example.wave_motion_gun.blockentity.WaveEnergyStorageBlockEntity;
import com.example.wave_motion_gun.client.widget.FireButton;
import com.example.wave_motion_gun.utils.FrequencyManager;
import com.example.wave_motion_gun.client.widget.HorizontalSwitch;
import com.example.wave_motion_gun.client.widget.InjectorLever;
import com.example.wave_motion_gun.client.widget.MechanicalSwitch;
import com.example.wave_motion_gun.client.widget.VerticalSlider;
import com.example.wave_motion_gun.init.SoundInit;
import com.example.wave_motion_gun.menu.TriggerUnitMenu;
import com.example.wave_motion_gun.network.PacketHandler;
import com.example.wave_motion_gun.network.TriggerUpdatePacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class TriggerUnitScreen extends VirtualScaledScreen<TriggerUnitMenu> {

    // --- Colors ---
    private static final int COLOR_HOLO_TEXT = 0xB300FFFF;
    private static final int COLOR_TEXT_GREEN = 0xE655FF55;
    private static final int COLOR_TEXT_RED = 0xE6FF5555;
    private static final int COLOR_TEXT_YELLOW = 0xE6FFAA00;
    private static final int COLOR_TEXT_WHITE = 0xE6FFFFFF;
    private static final int COLOR_TEXT_GRAY = 0xE6AAAAAA;

    private static final boolean DEBUG = false;

    // --- Scaling Variables ---
    private static final float TEXT_SCALE = 1.0f;

    private static final int PANEL_GAP = 5;

    // --- Panel Layout Constants (init/render共通) ---
    private static final int BOTTOM_BASE_Y = VIRTUAL_HEIGHT - 5;
    private static final int FREQ_W = 66;
    private static final int FREQ_H = 110;
    private static final int CV_H = 70;
    private static final int CONT_W = 39;
    private static final int CONT_H = CV_H;
    private static final int SAFETY_W = 120;
    private static final int SAFETY_H = 60;
    private static final int FIRE_W = 100;
    private static final int FIRE_H = 40;
    private static final int INJ_W = 120;
    private static final int INJ_H = 60;
    private static final int CV_W = 110;

    private static final int FREQ_X = 10;
    private static final int CONT_X = FREQ_X + FREQ_W + PANEL_GAP; // 81
    private static final int SAFETY_X = 125; // Fixed
    private static final int INJ_X = SAFETY_X + SAFETY_W + PANEL_GAP;
    private static final int CV_X = INJ_X + INJ_W + PANEL_GAP;
    private static final int FIRE_X = (VIRTUAL_WIDTH - FIRE_W) / 2;

    private static final int FREQ_Y = BOTTOM_BASE_Y - FREQ_H;
    private static final int CONT_Y = BOTTOM_BASE_Y - CONT_H;
    private static final int SAFETY_Y = BOTTOM_BASE_Y - SAFETY_H;
    private static final int FIRE_Y = SAFETY_Y - PANEL_GAP - FIRE_H;
    private static final int INJ_Y = BOTTOM_BASE_Y - INJ_H;
    private static final int CV_Y = BOTTOM_BASE_Y - CV_H;

    // Safety Panel rows
    private static final int SAFETY_ROW_GAP = 14;
    private static final int SAFETY_ROW1_Y = SAFETY_Y + 20;
    private static final int SAFETY_ROW2_Y = SAFETY_ROW1_Y + SAFETY_ROW_GAP;
    private static final int SAFETY_ROW3_Y = SAFETY_ROW2_Y + SAFETY_ROW_GAP;

    // --- Widgets ---
    private EditBox frequencyBox;
    private Button saveButton;
    private Button fireCannonButton;
    private VerticalSlider lightSlider;
    private Button btnStartCountdown; // 秒読み開始ボタン

    // Custom Switches
    private MechanicalSwitch btnCircuitSwitch;
    private MechanicalSwitch btnValveSwitch;
    private MechanicalSwitch btnRegulateSwitch;
    private MechanicalSwitch btnPostLockSwitch;

    // Safety Switches (Horizontal)
    private HorizontalSwitch btnSafeInlet;
    private HorizontalSwitch btnSafeOutlet;
    private HorizontalSwitch btnSafeTrigger;

    // Lever
    private InjectorLever btnInitiateLever;

    // State Variables
    private boolean triggerSafety = false;
    private boolean postureLock = false;

    /**
     * 画面を開いたままA/Dで旋回できる、基準方位からの片側の上限角(度)。
     * キー入力を止めても船の回転慣性で行き過ぎるため、狙いの可動域より小さめに取る。
     */
    private static final float TURN_LIMIT_DEGREES = 10.0F;
    /** 旋回制限の基準方位。画面を開いた時点の船の方位を保持する(NaN = 未取得)。 */
    private float steeringReferenceYaw = Float.NaN;
    private boolean wasOverheated = false;
    private long lastAutoSwitchTime = 0;

    // Pressure Gauge State
    private float currentDispPressure = 0.0f;
    private float pressureCircuit = 0.0f;
    private float pressureValve = 0.0f;
    private float pressureEnergy = 0.0f;
    private float currentPressureNoise = 0.0f;
    private long lastNoiseUpdateTime = 0;

    // Animation State
    private long lastUiUpdateTime = 0;

    // --- Announcement / Gauge Sub-systems ---
    private final AnnouncementController announcements = new AnnouncementController();
    private GaugeRenderer gauges;

    // --- 文字列キャッシュ (毎フレームのString生成/フォーマットを回避) ---
    private int cachedPctInt = Integer.MIN_VALUE;
    private int cachedPctDec = -1;
    private String cachedPctIntStr = "";
    private String cachedPctDecStr = "";
    private long cachedRange = Long.MIN_VALUE;
    private String cachedRangeStr = "";
    private long cachedRadius = Long.MIN_VALUE;
    private String cachedRadiusStr = "";
    private long cachedCost = Long.MIN_VALUE;
    private String cachedCostStr = "";

    public TriggerUnitScreen(TriggerUnitMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    // Helper method for playing UI sounds
    private void playSound(SoundEvent sound) {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(sound, 1.0F));
    }

    private void startCountdown() {
        announcements.startCountdown();
        this.btnStartCountdown.visible = false;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        Minecraft.getInstance().options.hideGui = true;

        // 【JEI対策・決定版】
        // GUIの論理的な位置を「画面の左外」にずらし、幅を「画面幅＋余白」に設定します。
        // これにより、JEIは「左にも右にも表示スペースがない」と判断し、何も表示できなくなります。
        // 画面左端よりさらに左(-1000)から開始
        this.leftPos = -1000;
        this.topPos = 0;
        // 幅を画面幅 + 2000 にすることで、右端も画面外(width + 1000)まで到達させる
        this.imageWidth = this.width + 2000;
        this.imageHeight = this.height;

        forceLookAtBlock();

        // --- Calculate Global Scaling ---
        computeGlobalScale();

        if (this.gauges == null) {
            this.gauges = new GaugeRenderer(this.font);
        }

        // Initialize monitoring states
        announcements.initMonitorStates(menu.getRemoteCircuit(), menu.getRemoteValves(),
                menu.getRemoteInlet(), menu.getRemoteOutlet(), this.triggerSafety);

        // 初期値取得
        String freqVal = String.valueOf(menu.getTargetFreq());

        // --- 1. Frequency Panel (Unified) ---
        int freqCenterX = FREQ_X + FREQ_W / 2;
        int boxW = 50;

        this.frequencyBox = new EditBox(this.font, freqCenterX - boxW / 2, FREQ_Y + 40, boxW, 20, new TextComponent("Freq"));
        this.frequencyBox.setValue(freqVal);
        // 上限は FrequencyManager.MAX_FREQUENCY (16bit同期の制約)。超過入力は受け付けない
        // 先に桁数で弾くことで parseInt のオーバーフロー/例外を防ぐ (MAX_FREQUENCY は5桁)
        this.frequencyBox.setFilter(s -> s.matches("\\d*") && s.length() <= 5
                && (s.isEmpty() || Integer.parseInt(s) <= FrequencyManager.MAX_FREQUENCY));
        this.frequencyBox.setBordered(true);
        this.addRenderableWidget(frequencyBox);

        int saveBtnW = 50;
        // SAVEボタン: 入力ボックスの値をサーバーに送信する
        this.saveButton = this.addRenderableWidget(new Button(freqCenterX - saveBtnW / 2, FREQ_Y + 80, saveBtnW, 20, new TextComponent("SAVE"),
                btn -> {
                    // 入力値をパース
                    int val = 0;
                    try {
                        if (frequencyBox.getValue() != null && !frequencyBox.getValue().isEmpty()) {
                            val = Integer.parseInt(frequencyBox.getValue());
                        }
                    } catch (NumberFormatException ignored) {}

                    final int newFreq = val; // 再代入されない変数（事実上のfinal）にする

                    // SAVE時のみ新しい周波数を送る
                    tryPress(() -> sendPacketWithLogic(
                            newFreq,
                            menu.getRemoteCircuit(), menu.getRemoteValves(),
                            menu.getRemoteInlet(), menu.getRemoteOutlet(),
                            getSelectedMode()));
                }
        ));

        // --- 1.5 Contrast Panel (Slider) ---
        int sliderW = 20;
        int sliderH = 40;
        int sliderX = CONT_X + (CONT_W - sliderW) / 2;
        int sliderY = CONT_Y + 20;

        this.lightSlider = this.addRenderableWidget(new VerticalSlider(sliderX, sliderY, sliderW, sliderH,
                () -> (menu != null) ? menu.getLightLevel() : 0,
                // ドラッグ中: レベルが変わるたびにパケット送信
                level -> sendPacketFull(0, menu.getTargetFreq(), getSelectedMode(), menu.getRemoteCircuit(),
                        menu.getRemoteValves(), menu.getRemoteInlet(), menu.getRemoteOutlet(), level),
                // リリース時: アナウンス表示
                level -> announcements.addAnnouncement(I18n.get("announce.wave_motion_gun_mod.crossgauge_brightness", level * 10))
        ));

        // --- 2. Safety Panel ---
        int swH = 10;
        int swW = 30;

        // 各スイッチ類: 常に「現在の同期済み周波数 (menu.getTargetFreq())」を使用してパケットを送る
        this.btnSafeInlet = this.addRenderableWidget(new HorizontalSwitch(SAFETY_X + 30, SAFETY_ROW1_Y - 5, swW, swH,
                () -> menu.getRemoteInlet(),
                val -> tryPress(() -> sendPacketWithLogic(menu.getTargetFreq(), menu.getRemoteCircuit(), menu.getRemoteValves(), val, menu.getRemoteOutlet(), getSelectedMode()))
        ));

        this.btnSafeOutlet = this.addRenderableWidget(new HorizontalSwitch(SAFETY_X + 30, SAFETY_ROW2_Y - 5, swW, swH,
                () -> menu.getRemoteOutlet(),
                val -> tryPress(() -> sendPacketWithLogic(menu.getTargetFreq(), menu.getRemoteCircuit(), menu.getRemoteValves(), menu.getRemoteInlet(), val, getSelectedMode()))
        ));

        this.btnSafeTrigger = this.addRenderableWidget(new HorizontalSwitch(SAFETY_X + 30, SAFETY_ROW3_Y - 5, swW, swH,
                () -> this.triggerSafety,
                val -> tryPress(() -> this.triggerSafety = val)
        ));

        // --- 3. Fire & Lock Panel ---
        int partsH = 30;
        int firePartsY = FIRE_Y + (FIRE_H - partsH) / 2;
        int lockSwH = 20;
        int lockSwY = firePartsY + partsH - lockSwH;

        this.btnPostLockSwitch = this.addRenderableWidget(new MechanicalSwitch(FIRE_X + 5, lockSwY, 25, lockSwH,
                () -> this.postureLock,
                val -> tryPress(() -> {
                    this.postureLock = val;
                    // Trigger 6: Posture Lock
                    if (val) {
                        // 【修正】上の行(0)に強制的に「姿勢制御固定」を表示
                        announcements.setLine0(I18n.get("announce.wave_motion_gun_mod.posture_lock"));

                        // もう一つのメッセージは通常ロジックで追加（index 0が最新のため、index 1に追加される）
                        announcements.addAnnouncement(I18n.get("announce.wave_motion_gun_mod.shock_flash_defense"));
                    }
                }, true)
        ));

        this.fireCannonButton = this.addRenderableWidget(new FireButton(FIRE_X + 35, firePartsY, FIRE_W - 40, partsH, new TextComponent("FIRE"), 1.5f,
                btn -> {
                    if (this.postureLock) fireCannon();
                }
        ));
        this.fireCannonButton.active = false;

        // 秒読み開始ボタン (中央配置)
        int centerX = VIRTUAL_WIDTH / 2;
        int centerY = VIRTUAL_HEIGHT / 2;
        this.btnStartCountdown = this.addRenderableWidget(new Button(centerX - 40, centerY - 10, 80, 20, new TranslatableComponent("gui.wave_motion_gun_mod.start_countdown"), btn -> {
            startCountdown();
        }));
        this.btnStartCountdown.visible = false;

        // --- 4. Injector Panel ---
        this.btnInitiateLever = this.addRenderableWidget(new InjectorLever(INJ_X + 10, INJ_Y + 15, 55, 40,
                () -> getSelectedMode() != 0,
                isOn -> {
                    int current = getSelectedMode();
                    if (isOn && current == 0) {
                        // Trigger 3: Forced Injector
                        announcements.addAnnouncement(I18n.get("announce.wave_motion_gun_mod.injector_on"));
                        sendPacketWithLogic(menu.getTargetFreq(), menu.getRemoteCircuit(), menu.getRemoteValves(),
                                menu.getRemoteInlet(), menu.getRemoteOutlet(), 1);
                    } else if (!isOn && current != 0) {
                        sendPacketWithLogic(menu.getTargetFreq(), menu.getRemoteCircuit(), menu.getRemoteValves(),
                                menu.getRemoteInlet(), menu.getRemoteOutlet(), 0);
                    }
                }
        ));

        this.btnRegulateSwitch = this.addRenderableWidget(new MechanicalSwitch(INJ_X + 80, INJ_Y + 25, 25, 25,
                () -> getSelectedMode() == 2,
                val -> {
                    // Trigger 4: Regulate Switch
                    if (val) announcements.addAnnouncement(I18n.get("announce.wave_motion_gun_mod.pressure_lock"));

                    int current = getSelectedMode();
                    int nextMode = current;

                    if (current == 1) {
                        nextMode = 2;
                    } else if (current == 2) {
                        nextMode = 1;
                    }

                    if (nextMode != current) {
                        sendPacketWithLogic(
                                menu.getTargetFreq(),
                                menu.getRemoteCircuit(),
                                menu.getRemoteValves(),
                                menu.getRemoteInlet(),
                                menu.getRemoteOutlet(),
                                nextMode
                        );
                    }
                }
        ));

        // --- 5. Circuit & Valves ---
        this.btnCircuitSwitch = this.addRenderableWidget(new MechanicalSwitch(CV_X + 10, CV_Y + 15, 40, 40,
                () -> menu.getRemoteCircuit(),
                val -> {
                    // Note: Announcement for Circuit is handled in monitorStates() to confirm remote state change
                    tryPress(() -> sendPacketWithLogic(menu.getTargetFreq(), val, menu.getRemoteValves(), menu.getRemoteInlet(), menu.getRemoteOutlet(), getSelectedMode()));
                    if (val) {
                        playSound(SoundInit.WAVE_MOTION_GUN_HOLO_ON.get());
                    } else {
                        playSound(SoundInit.WAVE_MOTION_GUN_HOLO_OFF.get());
                    }
                }
        ));

        this.btnValveSwitch = this.addRenderableWidget(new MechanicalSwitch(CV_X + 60, CV_Y + 15, 30, 40,
                () -> menu.getRemoteValves(),
                val -> tryPress(() -> sendPacketWithLogic(menu.getTargetFreq(), menu.getRemoteCircuit(), val, menu.getRemoteInlet(), menu.getRemoteOutlet(), getSelectedMode()))
        ));
    }

    // --- Tick Synchronization ---
    @Override
    public void containerTick() {
        super.containerTick();

        updateShipSteering();

        // 入力ボックスにフォーカスがない場合、常にサーバーの値を反映させる
        if (this.frequencyBox != null && !this.frequencyBox.isFocused()) {
            String currentStr = this.frequencyBox.getValue();
            String serverStr = String.valueOf(menu.getTargetFreq());

            if (!currentStr.equals(serverStr)) {
                this.frequencyBox.setValue(serverStr);
            }
        }

        // 【修正】オーバーヒート時の緊急遮断と120%到達時の自動STAY切替は
        // フレームレート非依存にするためrender()ではなくtick処理で行う
        if (isBlockEntityValid()) {
            float percent = computeChargePercent();
            boolean isOverheated = (menu.getRemoteOverheat() > 0) && !DEBUG;

            if (isOverheated && !this.wasOverheated) emergencyShutdown();
            this.wasOverheated = isOverheated;

            if (!isOverheated && percent >= WaveCannonBlockEntity.OVERFILL_RATIO && menu.getStorageMode() == 1) {
                long now = System.currentTimeMillis();
                if (now - lastAutoSwitchTime > 5000) {
                    sendPacketWithLogic(menu.getTargetFreq(), menu.getRemoteCircuit(), menu.getRemoteValves(), menu.getRemoteInlet(), menu.getRemoteOutlet(), 2);
                    lastAutoSwitchTime = now;
                }
            }
        }
    }

    // --- Safety Checks & Logic ---
    private boolean isBlockEntityValid() { return menu != null && menu.getBlockEntity() != null && !menu.getBlockEntity().isRemoved(); }

    /**
     * この画面を開いたままでも船の旋回(A/D)だけは続けられるようにする。
     *
     * <p>旋回できるのは画面を開いた時点の方位から ±{@link #TURN_LIMIT_DEGREES} 度まで。
     * 上限に達した側の入力だけを止めるので、常に元の方位へ戻る操作は残る。
     *
     * <p>止める条件:
     * <ul>
     *   <li>着席していない(舵を握っていない)</li>
     *   <li>姿勢制御固定(postureLock)がON — 機体を動かさない運用のため</li>
     *   <li>周波数入力中 — AとDがテキスト操作と二重に効いてしまうため</li>
     * </ul>
     */
    private void updateShipSteering() {
        boolean canSteer = this.minecraft != null
                && this.minecraft.player != null
                && this.minecraft.player.isPassenger()
                && !this.postureLock
                && (this.frequencyBox == null || !this.frequencyBox.isFocused());

        if (!canSteer) {
            ShipControlPassthrough.poll(false, false);
            return;
        }

        boolean allowLeft = true;
        boolean allowRight = true;

        if (isBlockEntityValid() && this.minecraft.level != null) {
            float yaw = VSCompat.shipYawAt(this.minecraft.level, menu.getBlockEntity().getBlockPos());
            if (!Float.isNaN(yaw)) {
                if (Float.isNaN(this.steeringReferenceYaw)) {
                    this.steeringReferenceYaw = yaw;
                }
                // 基準からの回頭量。Mth.degreesDifference は -180..180 に正規化する
                float delta = Mth.degreesDifference(this.steeringReferenceYaw, yaw);
                // Minecraftのyawは右回りで増加するため、A(左)は減少方向・D(右)は増加方向
                if (delta >= TURN_LIMIT_DEGREES) allowRight = false;
                if (delta <= -TURN_LIMIT_DEGREES) allowLeft = false;
            }
        }

        ShipControlPassthrough.poll(allowLeft, allowRight);
    }

    /** チャージ率 = 蓄電エネルギー / 発射コスト */
    private float computeChargePercent() {
        long energy = menu.getRemoteEnergy();
        long cost = WaveCannonBlockEntity.calcFireCost(menu.getRemoteRange(), menu.getRemoteRadius());
        return (cost > 0) ? (float) ((double) energy / (double) cost) : 0.0f;
    }

    private void tryPress(Runnable action) { tryPress(action, false); }
    private void tryPress(Runnable action, boolean isLockSwitch) {
        if (this.postureLock) {
            if (isLockSwitch) { action.run(); return; }
            if (Minecraft.getInstance().player != null) { Minecraft.getInstance().gui.setOverlayMessage(new TextComponent("§c[SYSTEM LOCKED] DISENGAGE POSTURE LOCK"), false); }
        } else { action.run(); }
    }

    private void updateButtonStates(float energyPercent, boolean isOverheated) {
        if (!isBlockEntityValid()) return;
        boolean circuitOn = menu.getRemoteCircuit();
        int mode = menu.getStorageMode();
        boolean isChargingOrStay = (mode == 1 || mode == 2);
        boolean safetiesOn = menu.getRemoteInlet() && menu.getRemoteOutlet() && this.triggerSafety;

        boolean locked = this.postureLock;

        this.btnValveSwitch.active = !locked && circuitOn && !isOverheated;
        boolean canInit = circuitOn && menu.getRemoteValves();
        this.btnInitiateLever.active = !locked && canInit && !isOverheated;
        this.btnRegulateSwitch.active = !locked && canInit && !isOverheated && isChargingOrStay && energyPercent < WaveCannonBlockEntity.OVERFILL_RATIO;

        this.btnSafeInlet.active = !locked && isChargingOrStay;
        this.btnSafeOutlet.active = !locked && isChargingOrStay;
        this.btnSafeTrigger.active = !locked && isChargingOrStay;

        if (locked) this.btnPostLockSwitch.active = true; else this.btnPostLockSwitch.active = safetiesOn;

        // 【修正】FIREボタンはカウントダウン完了後のみ有効
        this.fireCannonButton.active = locked && energyPercent >= 1.0f && announcements.isCountdownFinished();
        this.btnCircuitSwitch.active = !locked;
    }

    private void fireCannon() {
        if (!isBlockEntityValid()) return;
        int currentLight = menu.getLightLevel();
        // 発射時は現在の周波数(menu.getTargetFreq())を使う
        sendPacketFull(1, menu.getTargetFreq(), 0, menu.getRemoteCircuit(), false, false, false, currentLight);
        this.triggerSafety = false;
        // カウントダウン状態もリセット
        announcements.resetCountdown();
    }

    private void emergencyShutdown() {
        if (!isBlockEntityValid()) return;
        int currentLight = menu.getLightLevel();
        // シャットダウン時も現在の周波数を使う
        sendPacketFull(0, menu.getTargetFreq(), 0, menu.getRemoteCircuit(), false, false, false, currentLight);
        this.triggerSafety = false;
    }

    private int getSelectedMode() { return (menu != null) ? menu.getStorageMode() : 0; }

    private void sendPacketWithLogic(int targetFreq, boolean newCircuit, boolean newValves, boolean newInlet, boolean newOutlet, int newMode) {
        if (!isBlockEntityValid()) return;
        if (!newCircuit) { newValves = false; newInlet = false; newOutlet = false; newMode = 0; this.triggerSafety = false; }
        else if (!newValves) { newInlet = false; newOutlet = false; newMode = 0; this.triggerSafety = false; }
        else if (newMode == 0) { newInlet = false; newOutlet = false; this.triggerSafety = false; }
        if (newMode == 0) this.triggerSafety = false;
        if (newMode < 0 || newMode > 2) newMode = 0;

        int currentLight = menu.getLightLevel();
        sendPacketFull(0, targetFreq, newMode, newCircuit, newValves, newInlet, newOutlet, currentLight);
    }

    private void sendPacketFull(int type, int freq, int mode, boolean c, boolean v, boolean i, boolean o, int lightLevel) {
        if (!isBlockEntityValid()) return;
        BlockPos pos = menu.getBlockEntity().getBlockPos();
        if (pos == null) return;

        try {
            PacketHandler.INSTANCE.sendToServer(new TriggerUpdatePacket(pos, type, freq, mode, c, v, i, o, lightLevel));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void forceLookAtBlock() {
        if (isBlockEntityValid() && this.minecraft != null && this.minecraft.player != null) {
            BlockPos pos = menu.getBlockEntity().getBlockPos();
            if (pos != null && this.minecraft.level != null && this.minecraft.level.isLoaded(pos)) {
                BlockState state = this.minecraft.level.getBlockState(pos);
                if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                    Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
                    float targetYaw = facing.getOpposite().toYRot();
                    this.minecraft.player.setYRot(targetYaw);
                    this.minecraft.player.setXRot(0.0F);
                    this.minecraft.player.yRotO = targetYaw;
                    this.minecraft.player.xRotO = 0.0F;
                }
            }
        }
    }

    @Override
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        // レバーがドラッグ中なら、カーソル位置に関係なく強制的にリリースイベントを渡す
        if (this.btnInitiateLever != null && this.btnInitiateLever.isDragging) {
            return this.btnInitiateLever.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleasedScaled(mouseX, mouseY, button);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        // 【修正】BlockEntityが無効になったら壊れた表示を出し続けずに画面を閉じる
        if (!isBlockEntityValid()) {
            this.onClose();
            return;
        }
        this.renderBackground(poseStack);

        long now = System.currentTimeMillis();
        if (lastUiUpdateTime == 0) lastUiUpdateTime = now;
        float dt = (now - lastUiUpdateTime) / 1000.0f;
        lastUiUpdateTime = now;

        if (menu.getRemoteCircuit()) {
            uiOpenProgress += dt * 2.0f;
        } else {
            uiOpenProgress -= dt * 2.0f;
        }
        uiOpenProgress = Mth.clamp(uiOpenProgress, 0.0f, 1.0f);

        boolean circuitOn = menu.getRemoteCircuit();
        boolean valvesOn = menu.getRemoteValves();
        long energy = menu.getRemoteEnergy();
        long range = menu.getRemoteRange();
        long radius = menu.getRemoteRadius();
        long cost = WaveCannonBlockEntity.calcFireCost(menu.getRemoteRange(), menu.getRemoteRadius());
        float percent = computeChargePercent();

        // 姿勢制御が解除されたらカウントダウン関連をリセット
        if (!postureLock || percent < 1.0f) {
            announcements.resetCountdown();
        }

        // 秒読み開始ボタンの表示制御
        if (btnStartCountdown != null) {
            btnStartCountdown.visible = postureLock && percent >= 1.0f
                    && !announcements.isCountdownActive() && !announcements.isCountdownFinished();
        }

        // カウントダウン処理
        announcements.tickCountdown(now);

        float targetCircuit = circuitOn ? 18.0f : 0.0f;
        float targetValve = valvesOn ? 10.0f : 0.0f;
        float targetEnergy = (circuitOn || valvesOn)
                ? (float)(48.0 * Math.sqrt((double)energy / (double) WaveEnergyStorageBlockEntity.getCapacity())) : 0.0f;

        this.pressureCircuit += (targetCircuit - this.pressureCircuit) * 0.1f;
        this.pressureValve += (targetValve - this.pressureValve) * 0.02f;
        this.pressureEnergy += (targetEnergy - this.pressureEnergy) * 0.1f;

        if (circuitOn || valvesOn) {
            if (now - lastNoiseUpdateTime > 200) {
                currentPressureNoise = (float) (Math.random() * 2.0f);
                lastNoiseUpdateTime = now;
            }
        } else {
            currentPressureNoise = 0.0f;
        }

        this.currentDispPressure = this.pressureCircuit + this.pressureValve + this.pressureEnergy + this.currentPressureNoise;
        if (this.currentDispPressure < 0) this.currentDispPressure = 0;

        poseStack.pushPose();
        poseStack.translate(globalOffsetX, globalOffsetY, 0);
        poseStack.scale(globalScale, globalScale, 1.0f);

        int virtualMouseX = (int) transformMouseX(mouseX);
        int virtualMouseY = (int) transformMouseY(mouseY);

        int remoteOverheat = menu.getRemoteOverheat();
        boolean isOverheated = (remoteOverheat > 0) && !DEBUG;

        // Monitor states for announcements
        announcements.monitorStates(circuitOn, valvesOn, menu.getRemoteInlet(), menu.getRemoteOutlet(),
                this.triggerSafety, menu.getStorageMode(), range, radius, percent);

        updateButtonStates(percent, isOverheated);

        // --- Guidance Blink Logic ---
        this.btnCircuitSwitch.blinking = false;
        this.btnValveSwitch.blinking = false;
        this.btnInitiateLever.blinking = false;
        this.btnSafeInlet.blinking = false;
        this.btnSafeOutlet.blinking = false;
        this.btnSafeTrigger.blinking = false;
        this.btnPostLockSwitch.blinking = false;

        if (isBlockEntityValid()) {
            if (this.postureLock) {
                if (percent < 1.0f) {
                    this.btnPostLockSwitch.blinking = true;
                }
            } else {
                if (!menu.getRemoteCircuit()) {
                    this.btnCircuitSwitch.blinking = true;
                } else if (!menu.getRemoteValves()) {
                    if (!isOverheated) {
                        this.btnValveSwitch.blinking = true;
                    }
                } else if (getSelectedMode() == 0) {
                    if (percent > 0.0f) {
                        this.btnInitiateLever.blinking = true;
                    } else {
                        this.btnCircuitSwitch.blinking = true;
                    }
                    if (!isOverheated) {
                        this.btnInitiateLever.blinking = true;
                        this.btnCircuitSwitch.blinking = false;
                    }
                } else {
                    boolean s1 = menu.getRemoteInlet();
                    boolean s2 = menu.getRemoteOutlet();
                    boolean s3 = this.triggerSafety;

                    if (!s1 || !s2 || !s3) {
                        if (!s1) this.btnSafeInlet.blinking = true;
                        if (!s2) this.btnSafeOutlet.blinking = true;
                        if (!s3) this.btnSafeTrigger.blinking = true;
                    } else {
                        this.btnPostLockSwitch.blinking = true;
                    }
                }
            }
        }

        // 3. Contrast用ホログラムボックス表示 (Freqパネルの上)
        int holoContrastH = FREQ_H / 3;
        int holoContrastY = FREQ_Y - PANEL_GAP - holoContrastH - 8;
        if (uiOpenProgress > 0.0f && startScissorVertical(FREQ_X, holoContrastY, FREQ_W, holoContrastH)) {
            drawHoloBox(poseStack, FREQ_X, holoContrastY, FREQ_W, holoContrastH);

            drawScaledCenteredStringNoShadow(poseStack, "CONTRAST", FREQ_X + FREQ_W / 2, holoContrastY + 5, COLOR_HOLO_TEXT);

            int lightVal = menu.getLightLevel() * 10;
            String lightStr = String.valueOf(lightVal);

            poseStack.pushPose();
            float cx = FREQ_X + FREQ_W / 2.0f;
            float cy = holoContrastY + 18.0f;
            poseStack.translate(cx, cy, 0);
            poseStack.scale(1.5f, 1.5f, 1.0f);
            int strW = this.font.width(lightStr);
            this.font.draw(poseStack, lightStr, -strW / 2.0f, 0, COLOR_HOLO_TEXT);
            poseStack.popPose();

            endScissor();
        }

        int freqCenterX = FREQ_X + FREQ_W / 2;
        drawSteelBox(poseStack, FREQ_X, FREQ_Y, FREQ_W, FREQ_H, false);
        drawScaledCenteredStringNoShadow(poseStack, "FREQ.", freqCenterX, FREQ_Y + 4, COLOR_TEXT_WHITE);
        drawScaledCenteredStringNoShadow(poseStack, "Target", freqCenterX, FREQ_Y + 25, COLOR_TEXT_GREEN);

        drawSteelBox(poseStack, CONT_X, CONT_Y, CONT_W, CONT_H, false);
        drawScaledCenteredStringNoShadow(poseStack, "CONT.", CONT_X + CONT_W / 2, CONT_Y + 4, COLOR_TEXT_WHITE);

        drawSteelBox(poseStack, SAFETY_X, SAFETY_Y, SAFETY_W, SAFETY_H, false);
        drawScaledCenteredString(poseStack, "SAFETY", SAFETY_X + 60, SAFETY_Y + 4, COLOR_TEXT_WHITE);

        int lampX = SAFETY_X + 14;
        int textX = SAFETY_X + 65;

        drawLamp(poseStack, lampX, SAFETY_ROW1_Y, menu.getRemoteInlet());
        drawScaledString(poseStack, "Inlet", textX, SAFETY_ROW1_Y - 4, COLOR_TEXT_WHITE);
        drawLamp(poseStack, lampX, SAFETY_ROW2_Y, menu.getRemoteOutlet());
        drawScaledString(poseStack, "Outlet", textX, SAFETY_ROW2_Y - 4, COLOR_TEXT_WHITE);
        drawLamp(poseStack, lampX, SAFETY_ROW3_Y, this.triggerSafety);
        drawScaledString(poseStack, "Trigger", textX, SAFETY_ROW3_Y - 4, COLOR_TEXT_WHITE);

        drawSteelBox(poseStack, FIRE_X, FIRE_Y, FIRE_W, FIRE_H, false);
        drawScaledString(poseStack, "Lock", FIRE_X + 5, FIRE_Y + 5, COLOR_TEXT_WHITE);

        // LOCK UIの位置
        int lockHudY = (VIRTUAL_HEIGHT / 2) - ((VIRTUAL_HEIGHT / 2) - 20) + 10 + 90 - 15 - 20;
        if (uiOpenProgress > 0.0f && startScissor(FIRE_X - 50, lockHudY, 200, 20)) {
            if (postureLock) {
                drawScaledCenteredString(poseStack, "<<< LOCK ON >>>", VIRTUAL_WIDTH / 2, lockHudY, COLOR_TEXT_RED);
            } else {
                drawScaledCenteredString(poseStack, "LOCK OFF", VIRTUAL_WIDTH / 2, lockHudY, COLOR_TEXT_GRAY);
            }
            endScissor();
        }

        // --- Render Announcements ---
        if (uiOpenProgress > 0.0f && announcements.isEnabled()) {
            announcements.renderAnnouncements(poseStack, now, this::drawScaledCenteredString);
        }

        drawSteelBox(poseStack, INJ_X, INJ_Y, INJ_W, 60, false);
        drawScaledCenteredString(poseStack, "FORCED INJECTOR", INJ_X + INJ_W / 2, INJ_Y + 4, COLOR_TEXT_WHITE);

        if (uiOpenProgress > 0.0f && startScissorVertical(CV_X, CV_Y - 60, CV_W, 60)) {
            drawHoloBox(poseStack, CV_X, CV_Y - 60, CV_W, 60);

            int hY = CV_Y - 60 + 5;
            int lampX2 = CV_X + 10;
            drawLamp(poseStack, lampX2, hY + 5, menu.getRemoteCircuit());
            drawScaledString(poseStack, "Circuit to WMG", CV_X + 18, hY, COLOR_HOLO_TEXT);
            drawScaledString(poseStack, menu.getRemoteCircuit() ? "open" : "closed", CV_X + 18, hY + 10, menu.getRemoteCircuit() ? COLOR_TEXT_GREEN : COLOR_TEXT_RED);

            int hY2 = hY + 28;
            drawLamp(poseStack, lampX2, hY2 + 5, menu.getRemoteValves());
            drawScaledString(poseStack, "Emergency Valves", CV_X + 18, hY2, COLOR_HOLO_TEXT);
            drawScaledString(poseStack, menu.getRemoteValves() ? "closed" : "open", CV_X + 18, hY2 + 10, menu.getRemoteValves() ? COLOR_TEXT_GREEN : COLOR_TEXT_RED);
            endScissor();
        }
        drawSteelBox(poseStack, CV_X, CV_Y, CV_W, 70, false);

        int perX = VIRTUAL_WIDTH - 110;
        int perY = 10;
        int genH = 60;
        if (uiOpenProgress > 0.0f && startScissorVertical(perX, perY, 100, genH)) {
            drawHoloBox(poseStack, perX, perY, 100, genH);
            poseStack.pushPose();
            poseStack.translate(perX + 5, perY + 5, 0);
            poseStack.scale(1.5f, 1.5f, 1.0f);
            this.font.draw(poseStack, "GENERATOR", 0, 0, COLOR_HOLO_TEXT);
            poseStack.popPose();

            float drawPercent = percent * 100f;
            if (drawPercent > 120.0f) drawPercent = 120.0f;
            // 値が変わった時だけフォーマットし直す (毎フレームのString.formatを回避)
            int pctInt = (int) drawPercent;
            int pctDec = (int) ((drawPercent - pctInt) * 100);
            if (pctInt != cachedPctInt || pctDec != cachedPctDec) {
                cachedPctInt = pctInt;
                cachedPctDec = pctDec;
                cachedPctIntStr = String.valueOf(pctInt);
                cachedPctDecStr = String.format(".%02d", pctDec);
            }
            String pInt = cachedPctIntStr;
            String pDec = cachedPctDecStr;

            poseStack.pushPose();
            poseStack.translate(perX + 5, perY + 28, 0);
            poseStack.scale(3.0f * TEXT_SCALE, 3.0f * TEXT_SCALE, 1.0f);
            this.font.draw(poseStack, pInt, 0, 0, COLOR_HOLO_TEXT);
            int intWidth = this.font.width(pInt);
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(perX + 5 + (intWidth * 3.0f * TEXT_SCALE), perY + 37, 0);
            poseStack.scale(1.5f * TEXT_SCALE, 1.5f * TEXT_SCALE, 1.0f);
            this.font.draw(poseStack, pDec, 0, 0, COLOR_HOLO_TEXT);
            int decWidth = this.font.width(pDec);
            this.font.draw(poseStack, "%", decWidth + 2, 0, COLOR_HOLO_TEXT);
            poseStack.popPose();
            endScissor();
        }

        int infoX = 10;
        int infoY = 10;
        int infoW = 103;
        int infoH = 60;
        if (uiOpenProgress > 0.0f && startScissorVertical(infoX, infoY, infoW, infoH)) {
            drawHoloBox(poseStack, infoX, infoY, infoW, infoH);

            int lineStep = 12;
            int currentY = infoY + 5;
            drawScaledString(poseStack, "STATUS MONITOR", infoX + 3, currentY, COLOR_HOLO_TEXT);
            currentY += 15;
            // 値が変わった時だけ文字列を作り直す (毎フレームの連結を回避)
            if (range != cachedRange) { cachedRange = range; cachedRangeStr = "Range: " + range + " m"; }
            if (radius != cachedRadius) { cachedRadius = radius; cachedRadiusStr = "Radius: " + radius + " m"; }
            if (cost != cachedCost) { cachedCost = cost; cachedCostStr = "Cost: " + cost + " FE"; }
            drawScaledString(poseStack, cachedRangeStr, infoX + 3, currentY, COLOR_HOLO_TEXT);
            currentY += lineStep;
            drawScaledString(poseStack, cachedRadiusStr, infoX + 3, currentY, COLOR_HOLO_TEXT);
            currentY += lineStep;
            drawScaledString(poseStack, cachedCostStr, infoX + 3, currentY, COLOR_HOLO_TEXT);

            endScissor();
        }

        if (uiOpenProgress > 0.0f && startScissor(70, 0, 360, 200)) {
            gauges.renderArc(poseStack, percent, remoteOverheat, isOverheated, currentDispPressure);
            endScissor();
        }

        String modeStatus = "Off";
        int modeColor = COLOR_TEXT_RED;
        int mm = getSelectedMode();
        if (mm == 1) { modeStatus = "Active"; modeColor = COLOR_TEXT_GREEN; }
        else if (mm == 2) { modeStatus = "Regulated"; modeColor = COLOR_TEXT_YELLOW; }

        drawScaledCenteredStringNoShadow(poseStack, modeStatus, INJ_X + INJ_W / 2 + 33, INJ_Y + 14, modeColor);

        super.render(poseStack, virtualMouseX, virtualMouseY, partialTicks);
        this.renderTooltip(poseStack, virtualMouseX, virtualMouseY);

        poseStack.popPose();
    }

    // 【追加】背景描画メソッドをオーバーライドして、暗転を回避します
    // 【重要】完全透明な背景を描画
    // Plugin側の座標計算が正しければ、不透明度は0でも問題ありません。
    // 【重要】背景描画をオーバーライド
    @Override
    public void renderBackground(PoseStack poseStack, int vOffset) {
        if (this.minecraft.level != null) {
            // アルファ値を「4」(0x04) に設定。
            // 完全透明(0)や1だと環境によってはスキップされる可能性があるため、
            // わずかに色を乗せることで確実に描画パイプラインを通します。
            // 4/255 は約1.5%の不透明度で、肉眼ではほぼ気になりません。
            this.fillGradient(poseStack, 0, 0, this.width, this.height, 0x04000000, 0x04000000);
        } else {
            this.renderDirtBackground(vOffset);
        }
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTicks, int mouseX, int mouseY) {}
    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {}

    @Override
    public void removed() {
        Minecraft.getInstance().options.hideGui = false;
        // 画面を閉じたら操舵の肩代わりを解除し、通常のキー入力処理へ戻す
        ShipControlPassthrough.release();
        super.removed();
    }
}
