package com.example.wave_motion_gun.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;

/**
 * TriggerUnitScreenの円弧ゲージ(チャージ/圧力)と圧力数値・オーバーヒート表示のレンダラー。
 * emitArcは1バッファへのバッチ描画に最適化済み (ドローコール削減)。
 */
public class GaugeRenderer extends GuiComponent {

    private static final int COLOR_HOLO_TEXT = 0xB300FFFF;
    private static final float TEXT_SCALE = 1.0f;

    // --- 回頭角インジケータ ---
    /** 円弧の中心角。270度が画面の真上 (チャージ/圧力ゲージと同じ中心)。 */
    private static final float TURN_ARC_CENTER = 270.0f;
    /** 画面上での円弧の開き。この両端が回頭角の±limitに対応する。 */
    private static final float TURN_ARC_SPAN = 70.0f;
    /**
     * 円弧の半径。内側は圧力数値(centerY-100から下へ描かれるので半径100が上端)、
     * 外側は圧力ゲージの目盛り(半径115)で挟まれた隙間に収める。
     * これより内側へ置くと発射シーケンスのアナウンス(半径70付近)と重なる。
     */
    private static final float TURN_ARC_R = 106.0f;
    /** 数字ラベルを置く半径。円弧の外側、圧力ゲージ目盛りの手前。 */
    private static final float TURN_LABEL_R = 111.0f;
    private static final float TURN_LABEL_SCALE = 0.4f;
    /** 数字ラベルと長い目盛りを何度おきに置くか。 */
    private static final float TURN_LABEL_STEP = 5.0f;

    private final Font font;

    // --- 文字列キャッシュ (毎フレームのString生成/フォーマットを回避) ---
    private int cachedPressureTenths = Integer.MIN_VALUE;
    private String cachedPressureIntStr = "";
    private String cachedPressureDecStr = "";

    public GaugeRenderer(Font font) {
        this.font = font;
    }

    /**
     * 円弧ゲージ一式を描画する。
     * @param showOverheat オーバーヒート警告を表示するか (remoteOverheat > 0 && !DEBUG)
     */
    public void renderArc(PoseStack poseStack, float percent, int remoteOverheat, boolean showOverheat, float dispPressure) {
        float centerX = VirtualScaledScreen.VIRTUAL_WIDTH / 2.0f;
        float centerY = (VirtualScaledScreen.VIRTUAL_HEIGHT / 2.0f) + 20.0f;
        renderSegmentedGauge(poseStack, centerX, centerY, 165.0f, 20.0f, 195.0f, 150.0f, percent, 0, 13);

        float pressPercent = dispPressure / 80.0f;
        renderSegmentedGauge(poseStack, centerX, centerY, 135.0f, 15.0f, 190.0f, 160.0f, pressPercent, 1, 9);

        // 0.1刻みの値が変わった時だけ文字列を作り直す (毎フレームのString.format/splitを回避)
        int tenths = Math.round(dispPressure * 10.0f);
        if (tenths != cachedPressureTenths) {
            cachedPressureTenths = tenths;
            cachedPressureIntStr = String.valueOf(tenths / 10);
            cachedPressureDecStr = "." + (tenths % 10) + "TPa";
        }
        String intPart = cachedPressureIntStr;
        String decPart = cachedPressureDecStr;

        float textY = centerY - 100.0f;
        float bigScale = 1.5f * TEXT_SCALE;
        float smallScale = 0.8f * TEXT_SCALE;
        int intW = this.font.width(intPart);
        int decW = this.font.width(decPart);
        float totalW = (intW * bigScale) + (decW * smallScale);

        float startX = centerX - (totalW / 2.0f);

        poseStack.pushPose();
        poseStack.translate(startX, textY, 0);
        poseStack.scale(bigScale, bigScale, 1.0f);
        this.font.draw(poseStack, intPart, 0, 0, COLOR_HOLO_TEXT);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(startX + (intW * bigScale), textY + (5 * bigScale * 0.5f), 0);
        poseStack.scale(smallScale, smallScale, 1.0f);
        this.font.draw(poseStack, decPart, 0, 0, COLOR_HOLO_TEXT);
        poseStack.popPose();

        if (showOverheat) {
            int ohAlpha = (int)(Math.abs(Math.sin(System.currentTimeMillis() / 200.0)) * 255);
            int ohColor = (ohAlpha << 24) | 0xFF5555;
            drawCenteredShadowString(poseStack, "< OVER HEAT >", centerX, centerY - 50, ohColor);

            long remainSec = remoteOverheat / 20;
            String timeStr = String.format("%02d:%02d", remainSec / 60, remainSec % 60);
            drawCenteredShadowString(poseStack, timeStr, centerX, centerY - 40, 0xFFAAAAAA);
        }
    }

    /**
     * 回頭角インジケータ。圧力ゲージのすぐ内側に、目盛り付きの細い円弧と指針を描く。
     *
     * <p>画面上は約{@value #TURN_ARC_SPAN}度の円弧だが、その両端が回頭角の±limitに対応する。
     * 指針が中央(0度)からどちらへどれだけ振れているかで、基準方位からのずれが読める。
     *
     * <p>目盛りの数字は内部の回頭角と符号を反転して表示する。指針が動く向きは
     * 実際の回頭方向のままなので、右へ回頭すれば指針も右へ動き、そこの数字が負になる。
     *
     * @param delta 基準方位からの回頭角(度)。負が左、正が右(表示上の符号とは逆)
     * @param limit 片側の上限角(度)。円弧の端点がこの値になる
     */
    public void renderTurnIndicator(PoseStack poseStack, float delta, float limit) {
        if (limit <= 0.0f) return;

        float cx = VirtualScaledScreen.VIRTUAL_WIDTH / 2.0f;
        float cy = (VirtualScaledScreen.VIRTUAL_HEIGHT / 2.0f) + 20.0f;

        float startAngle = TURN_ARC_CENTER - TURN_ARC_SPAN / 2.0f;
        float endAngle = TURN_ARC_CENTER + TURN_ARC_SPAN / 2.0f;

        // 上限を超えた分は端で頭打ちにする(慣性で行き過ぎても指針が飛び出さない)
        float clamped = Math.max(-limit, Math.min(limit, delta));
        float needleAngle = TURN_ARC_CENTER + (clamped / limit) * (TURN_ARC_SPAN / 2.0f);
        boolean atLimit = Math.abs(delta) >= limit;

        int frameColor = 0x8000FFFF;
        int tickColor = 0xB300FFFF;
        int needleColor = atLimit ? 0xE6FF5555 : 0xE655FF55;

        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        // 円弧本体 (1px相当の細い線)
        emitArc(buffer, matrix, cx, cy, TURN_ARC_R, TURN_ARC_R + 1.0f, startAngle, endAngle, frameColor);

        // 目盛り: 1度刻みの小目盛りを内向きに生やす。5度刻みは長くする
        int minorCount = (int) (limit * 2.0f) + 1;
        for (int i = 0; i < minorCount; i++) {
            float value = -limit + i;
            float angle = TURN_ARC_CENTER + (value / limit) * (TURN_ARC_SPAN / 2.0f);
            boolean major = Math.abs(value % TURN_LABEL_STEP) < 0.001f;
            float len = major ? 4.5f : 2.5f;
            emitArc(buffer, matrix, cx, cy, TURN_ARC_R - len, TURN_ARC_R, angle - 0.15f, angle + 0.15f, tickColor);
        }

        // 指針: 円弧をまたぐ形で引く。外側はラベル(半径111)に触れない範囲まで
        emitArc(buffer, matrix, cx, cy, TURN_ARC_R - 5.5f, TURN_ARC_R + 2.0f,
                needleAngle - 0.45f, needleAngle + 0.45f, needleColor);

        tesselator.end();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();

        // 目盛りの数字。円弧に沿って傾ける。
        // 角度aの位置での接線方向は a+90 度なので、その分回転させると弧に沿う
        for (float value = -limit; value <= limit + 0.001f; value += TURN_LABEL_STEP) {
            float angle = TURN_ARC_CENTER + (value / limit) * (TURN_ARC_SPAN / 2.0f);
            double rad = Math.toRadians(angle);
            float lx = cx + (float) Math.cos(rad) * TURN_LABEL_R;
            float ly = cy + (float) Math.sin(rad) * TURN_LABEL_R;

            // 表示上の符号は内部の回頭角と逆にする(指針の動く向きは実際の回頭方向のまま)
            int shown = Math.round(-value);
            String label = shown > 0 ? "+" + shown : String.valueOf(shown);
            int w = this.font.width(label);

            poseStack.pushPose();
            poseStack.translate(lx, ly, 0);
            poseStack.mulPose(Vector3f.ZP.rotationDegrees(angle + 90.0f));
            poseStack.scale(TURN_LABEL_SCALE, TURN_LABEL_SCALE, 1.0f);
            this.font.draw(poseStack, label, -w / 2.0f, -4.0f, COLOR_HOLO_TEXT);
            poseStack.popPose();
        }
    }

    private void drawCenteredShadowString(PoseStack poseStack, String text, float x, float y, int color) {
        int w = this.font.width(text);
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.scale(TEXT_SCALE, TEXT_SCALE, 1.0f);
        this.font.drawShadow(poseStack, text, -w / 2.0f, 0, color);
        poseStack.popPose();
    }

    private void renderSegmentedGauge(PoseStack poseStack, float cx, float cy, float rOut, float thickness, float startAngle, float totalAngle, float percent, int colorType, int tickCount) {
        float rIn = rOut - thickness;
        int segments = 60;
        float segmentStep = totalAngle / segments;
        float gap = 0.5f;
        float drawStep = segmentStep - gap;
        int darkColor = (0x4D << 24) | 0x161616;

        int chargeColor;
        if (colorType == 0) {
            int baseColor;
            if (percent < 0.3f) baseColor = 0xFF5555;
            else if (percent < 0.8f) baseColor = 0xFFAA00;
            else baseColor = 0x55FF55;

            if (percent >= 1.0f && (System.currentTimeMillis() / 200) % 2 == 0) {
                chargeColor = (0xCC << 24) | 0xBBFFBB;
            } else {
                chargeColor = (0xCC << 24) | baseColor;
            }
        } else {
            chargeColor = (0xCC << 24) | 0x00FFFF;
        }

        float norm = (colorType == 0) ? 1.2f : 1.0f;
        float drawPercent = percent;
        if (drawPercent > norm) drawPercent = norm;

        int activeSegments = (int)((drawPercent / norm) * segments);
        if (activeSegments > segments) activeSegments = segments;

        int frameColor = 0x8000FFFF;

        // 【最適化】全セグメントを1つのバッファにまとめて描画する (セグメント毎のbegin/endによるドローコール増加を回避)
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < segments; i++) {
            float sA = startAngle + i * segmentStep;
            float eA = sA + drawStep;
            emitArc(buffer, matrix, cx, cy, rIn, rOut, sA, eA, darkColor);
        }

        for (int i = 0; i < activeSegments; i++) {
            float sA = startAngle + i * segmentStep;
            float eA = sA + drawStep;
            emitArc(buffer, matrix, cx, cy, rIn, rOut, sA, eA, chargeColor);
        }

        emitArc(buffer, matrix, cx, cy, rIn, rOut, startAngle - 1, startAngle, frameColor);
        emitArc(buffer, matrix, cx, cy, rIn, rOut, startAngle + totalAngle, startAngle + totalAngle + 1, frameColor);

        float tickBaseR = rIn - 5.0f;
        if (tickCount > 0) {
            emitArc(buffer, matrix, cx, cy, tickBaseR, tickBaseR + 1, startAngle, startAngle + totalAngle, frameColor);
        }

        tesselator.end();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();

        if (tickCount > 0) {
            float tickStep = totalAngle / (tickCount - 1);
            float tickLen = 4.0f;

            for (int i = 0; i < tickCount; i++) {
                float angle = startAngle + i * tickStep;
                float x1 = cx + (tickBaseR) * (float)Math.cos(Math.toRadians(angle));
                float y1 = cy + (tickBaseR) * (float)Math.sin(Math.toRadians(angle));

                poseStack.pushPose();
                poseStack.translate(x1, y1, 0);
                poseStack.mulPose(Vector3f.ZP.rotationDegrees(angle));
                fill(poseStack, 0, 0, (int)tickLen, 1, frameColor);
                poseStack.popPose();
            }
        }
    }

    // 円弧を三角形としてバッファに追加する (呼び出し側で begin/end を一括管理する)
    // 端数の最終スライスも endAngle まで確実に描画する
    private void emitArc(BufferBuilder buffer, Matrix4f matrix, float x, float y, float innerRadius, float outerRadius, float startAngle, float endAngle, int color) {
        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red   = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue  = (float)(color & 255) / 255.0F;

        float step = 0.5F;
        float prevAngle = startAngle;
        double prevRad = Math.toRadians(prevAngle);
        float prevCos = (float) Math.cos(prevRad);
        float prevSin = (float) Math.sin(prevRad);

        while (prevAngle < endAngle) {
            float nextAngle = Math.min(prevAngle + step, endAngle);
            double nextRad = Math.toRadians(nextAngle);
            float nextCos = (float) Math.cos(nextRad);
            float nextSin = (float) Math.sin(nextRad);

            float o1x = x + prevCos * outerRadius, o1y = y + prevSin * outerRadius;
            float i1x = x + prevCos * innerRadius, i1y = y + prevSin * innerRadius;
            float o2x = x + nextCos * outerRadius, o2y = y + nextSin * outerRadius;
            float i2x = x + nextCos * innerRadius, i2y = y + nextSin * innerRadius;

            // TRIANGLE_STRIP時代と同じ頂点順序で2つの三角形を追加
            buffer.vertex(matrix, o1x, o1y, 0).color(red, green, blue, alpha).endVertex();
            buffer.vertex(matrix, i1x, i1y, 0).color(red, green, blue, alpha).endVertex();
            buffer.vertex(matrix, o2x, o2y, 0).color(red, green, blue, alpha).endVertex();

            buffer.vertex(matrix, o2x, o2y, 0).color(red, green, blue, alpha).endVertex();
            buffer.vertex(matrix, i1x, i1y, 0).color(red, green, blue, alpha).endVertex();
            buffer.vertex(matrix, i2x, i2y, 0).color(red, green, blue, alpha).endVertex();

            prevAngle = nextAngle;
            prevCos = nextCos;
            prevSin = nextSin;
        }
    }
}
