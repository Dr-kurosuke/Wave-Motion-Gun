package com.example.wave_motion_gun.client.widget;

/**
 * カスタムウィジェット共通の配色定数とガイダンス(点滅)色ロジック。
 * TriggerUnitScreen / MonitoringUnitScreen / ShockCannonScreen で重複していた定数を集約。
 */
public final class WidgetColors {

    // Switch Gradient Colors
    public static final int[] GRADIENT_LIGHT = { 0xFF666666 , 0xFF888888, 0xFFAAAAAA, 0xFFCCCCCC};
    public static final int[] GRADIENT_DARK = { 0xFF777777, 0xFF666666, 0xFF555555, 0xFF444444 };

    // Gap Colors
    public static final int COLOR_GAP_TOP = 0xFF222222;
    public static final int COLOR_GAP_BOTTOM = 0xFF222222;

    public static final int COLOR_BORDER_GREEN_TR = 0x8055FF55;
    public static final int COLOR_BORDER_RED_TR = 0x80FF5555;
    public static final int COLOR_BORDER_GREEN_BRIGHT = 0xE699FF99; // 点滅用の明るい緑

    public static final int COLOR_TEXT_WHITE = 0xE6FFFFFF;

    private WidgetColors() {}

    // --- Helper for Blinking Border ---
    public static int getGuidanceColor(boolean isActive, boolean isBlinking) {
        if (isBlinking) {
            long time = System.currentTimeMillis();
            boolean bright = (time % 1000) < 500;
            return bright ? COLOR_BORDER_GREEN_BRIGHT : COLOR_BORDER_GREEN_TR;
        }
        return isActive ? COLOR_BORDER_GREEN_TR : COLOR_BORDER_RED_TR;
    }
}
