package com.example.wave_motion_gun.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;

/**
 * TriggerUnitScreenのアナウンス表示(2スロット)・秒読みカウントダウン・
 * 状態遷移監視によるアナウンストリガーを管理するコントローラ。
 */
public class AnnouncementController {

    /** 中央揃えテキストの描画をスクリーン側に委譲するためのインターフェース */
    @FunctionalInterface
    public interface TextDrawer {
        void draw(GuiGraphics guiGraphics, String text, float x, float y, int color);
    }

    // --- Announcement System Variables ---
    private final boolean enabled = true; // 設定用ブーリアン
    private final String[] announceLines = { "", "" };
    private final long[] announceTimes = { 0, 0 };

    // Countdown Variables
    private boolean countdownActive = false;
    private boolean countdownFinished = false;
    private long countdownStartTime = 0;
    private int lastCount = -1;

    // State Monitoring for Announcement Triggers
    private boolean lastCircuit = false;
    private boolean lastValves = false;
    private boolean lastSafetyCond1 = false; // Inlet && Outlet
    private boolean lastSafetyCond2 = false; // Inlet && Outlet && Trigger
    private boolean lastInlet = false;  // 前回のInlet状態
    private boolean lastOutlet = false; // 前回のOutlet状態
    private float lastChargePercent = 0.0f;

    public boolean isEnabled() { return enabled; }
    public boolean isCountdownActive() { return countdownActive; }
    public boolean isCountdownFinished() { return countdownFinished; }

    // --- Announcement Logic ---
    public void addAnnouncement(String message) {
        if (!enabled) return;
        long now = System.currentTimeMillis();
        int targetIndex;

        // Check if lines are expired (> 3000ms)
        boolean line0Expired = (now - announceTimes[0] > 3000);
        boolean line1Expired = (now - announceTimes[1] > 3000);

        if (line0Expired) {
            targetIndex = 0;
        } else if (line1Expired) {
            targetIndex = 1;
        } else {
            // Both active: overwrite the older one
            targetIndex = (announceTimes[0] < announceTimes[1]) ? 0 : 1;
        }

        announceLines[targetIndex] = message;
        announceTimes[targetIndex] = now;
    }

    /** 上の行(0)に強制的にメッセージを表示する */
    public void setLine0(String message) {
        announceLines[0] = message;
        announceTimes[0] = System.currentTimeMillis();
    }

    // --- Countdown ---
    public void startCountdown() {
        this.countdownActive = true;
        this.countdownFinished = false;
        this.countdownStartTime = System.currentTimeMillis();
        this.lastCount = 11; // 初期値(10を表示させるため)
    }

    public void resetCountdown() {
        this.countdownActive = false;
        this.countdownFinished = false;
    }

    /** カウントダウンの進行処理 (毎フレーム呼び出し) */
    public void tickCountdown(long now) {
        if (!countdownActive) return;

        long elapsed = now - countdownStartTime;
        int count = 10 - (int)(elapsed / 1000);

        if (count < 0) {
            // 既に完了済み
        } else if (count == 0) {
            // "Wave Cannon, Fire" phase (0の次の1秒)
            if (lastCount != 0) {
                announceLines[0] = I18n.get("announce.wave_motion_gun_mod.fire");
                announceTimes[0] = now + 10000; // 長めに表示
                countdownFinished = true;
                countdownActive = false; // カウント終了
                lastCount = 0;
            }
        } else {
            // 10 to 1
            if (count != lastCount) {
                announceLines[0] = String.valueOf(count);
                announceTimes[0] = now; // タイムスタンプ更新
                lastCount = count;
            } else {
                // 表示を維持するためにタイムスタンプを更新し続ける
                announceTimes[0] = now;
            }
        }
    }

    // --- State Monitoring ---
    /** init()時の初期化 (アナウンスの誤発火防止) */
    public void initMonitorStates(boolean circuit, boolean valves, boolean inlet, boolean outlet, boolean triggerSafety) {
        this.lastCircuit = circuit;
        this.lastValves = valves;
        this.lastSafetyCond1 = inlet && outlet;
        this.lastSafetyCond2 = this.lastSafetyCond1 && triggerSafety;
        this.lastChargePercent = 0.0f; // Reset charge tracking on init
    }

    /** 状態遷移を監視してアナウンスを発火する (毎フレーム呼び出し) */
    public void monitorStates(boolean currentCircuit, boolean currentValves, boolean currentInlet, boolean currentOutlet,
                              boolean triggerSafety, int storageMode, long range, long radius, float currentPercent) {
        if (!enabled) return;

        // 1. Circuit
        if (!lastCircuit && currentCircuit) {
            addAnnouncement(I18n.get("announce.wave_motion_gun_mod.circuit_open"));
        }
        lastCircuit = currentCircuit;

        // 2. Emergency Valves
        if (!lastValves && currentValves) {
            addAnnouncement(I18n.get("announce.wave_motion_gun_mod.valves_closed"));
        }
        lastValves = currentValves;

        // 5. Safety
        boolean currentSafetyCond1 = currentInlet && currentOutlet; // 両方ON
        boolean currentSafetyCond2 = currentSafetyCond1 && triggerSafety; // 全部ON

        // セーフティが一つだけ解除された時の判定 (0 -> 1)
        boolean lastBothOff = !lastInlet && !lastOutlet;
        boolean currentOneOnly = currentInlet ^ currentOutlet; // XOR: 片方だけtrueならtrue

        if (lastBothOff && currentOneOnly) {
            addAnnouncement(I18n.get("announce.wave_motion_gun_mod.safety_release"));
        }

        if (!lastSafetyCond1 && currentSafetyCond1 && !currentSafetyCond2) {
            addAnnouncement(I18n.get("announce.wave_motion_gun_mod.safety_lock_zero"));
        }
        if (!lastSafetyCond2 && currentSafetyCond2) {
            addAnnouncement(I18n.get("announce.wave_motion_gun_mod.final_safety_release"));
        }

        // 状態更新
        lastSafetyCond1 = currentSafetyCond1;
        lastSafetyCond2 = currentSafetyCond2;
        lastInlet = currentInlet;
        lastOutlet = currentOutlet;

        // 8, 9, 10. Charge Percent
        if (storageMode == 1) { // CHARGE mode
            if (lastChargePercent < 0.20f && currentPercent >= 0.20f) {
                addAnnouncement(I18n.get("announce.wave_motion_gun_mod.charge20"));
            }
            if (lastChargePercent < 0.5f && currentPercent >= 0.5f) {
                addAnnouncement(I18n.get("announce.wave_motion_gun_mod.charge50"));
            }
            if (lastChargePercent < 0.65f && currentPercent >= 0.65f) {
                addAnnouncement(I18n.get("announce.wave_motion_gun_mod.target_params", range, radius));
            }
            if (lastChargePercent < 0.8f && currentPercent >= 0.8f) {
                addAnnouncement(I18n.get("announce.wave_motion_gun_mod.charge80"));
            }
            if (lastChargePercent < 1.0f && currentPercent >= 1.0f) {
                addAnnouncement(I18n.get("announce.wave_motion_gun_mod.charge100"));
            }
            if (lastChargePercent < com.example.wave_motion_gun.blockentity.WaveCannonBlockEntity.OVERFILL_RATIO
                    && currentPercent >= com.example.wave_motion_gun.blockentity.WaveCannonBlockEntity.OVERFILL_RATIO) {
                addAnnouncement(I18n.get("announce.wave_motion_gun_mod.charge120"));
            }
        }
        lastChargePercent = currentPercent;
    }

    // --- Rendering ---
    /** アナウンス2スロットを縦フローで描画する */
    public void renderAnnouncements(GuiGraphics guiGraphics, long now, TextDrawer drawer) {
        // アナウンス表示位置をさらに10ピクセル上げて -50 とする
        int annY = (VirtualScaledScreen.VIRTUAL_HEIGHT / 2) - 50;
        int lineHeight = 12;

        // 2スロットを縦フローで描画。改行(\n)を含むメッセージは物理行数ぶんカーソルを進めるため重ならない
        // 折り返しは言語ファイル側の \n で明示指定する(日本語は改行なしで1行表示)
        int cursorY = annY;
        for (int i = 0; i < 2; i++) {
            String msg = announceLines[i];
            if (msg == null || msg.isEmpty()) continue;

            long age = now - announceTimes[i];
            if (age >= 3000) continue;

            float alpha;
            // 2.5秒(2500ms)までは不透明、残りの500msでフェードアウト
            if (age < 2500) {
                alpha = 1.0f;
            } else {
                float fadeProgress = (age - 2500) / 500.0f;
                alpha = 1.0f - fadeProgress;
            }
            alpha = Mth.clamp(alpha, 0.0f, 1.0f);

            int alphaInt = (int)(alpha * 255);
            if (alphaInt <= 4) continue;

            // ベースカラーは白 (0xFFFFFF)
            int fullColor = (alphaInt << 24) | 0xFFFFFF;

            for (String line : msg.split("\n")) {
                // 従来と同じくint除算 (VIRTUAL_WIDTH / 2 = 247)
                drawer.draw(guiGraphics, line, VirtualScaledScreen.VIRTUAL_WIDTH / 2, cursorY, fullColor);
                cursorY += lineHeight;
            }
        }
    }
}
