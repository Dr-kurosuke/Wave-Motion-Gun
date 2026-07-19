package com.example.wave_motion_gun.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;

/**
 * TriggerUnitScreenの円弧ゲージ(チャージ/圧力)と圧力数値・オーバーヒート表示のレンダラー。
 * emitArcは1バッファへのバッチ描画に最適化済み (ドローコール削減)。
 */
public class GaugeRenderer {

    private static final int COLOR_HOLO_TEXT = 0xB300FFFF;
    private static final float TEXT_SCALE = 1.0f;

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
    public void renderArc(GuiGraphics guiGraphics, float percent, int remoteOverheat, boolean showOverheat, float dispPressure) {
        PoseStack poseStack = guiGraphics.pose();
        float centerX = VirtualScaledScreen.VIRTUAL_WIDTH / 2.0f;
        float centerY = (VirtualScaledScreen.VIRTUAL_HEIGHT / 2.0f) + 20.0f;
        renderSegmentedGauge(guiGraphics, centerX, centerY, 165.0f, 20.0f, 195.0f, 150.0f, percent, 0, 13);

        float pressPercent = dispPressure / 80.0f;
        renderSegmentedGauge(guiGraphics, centerX, centerY, 135.0f, 15.0f, 190.0f, 160.0f, pressPercent, 1, 9);

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
        guiGraphics.drawString(this.font, intPart, 0, 0, COLOR_HOLO_TEXT, false);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(startX + (intW * bigScale), textY + (5 * bigScale * 0.5f), 0);
        poseStack.scale(smallScale, smallScale, 1.0f);
        guiGraphics.drawString(this.font, decPart, 0, 0, COLOR_HOLO_TEXT, false);
        poseStack.popPose();

        if (showOverheat) {
            int ohAlpha = (int)(Math.abs(Math.sin(System.currentTimeMillis() / 200.0)) * 255);
            int ohColor = (ohAlpha << 24) | 0xFF5555;
            drawCenteredShadowString(guiGraphics, "< OVER HEAT >", centerX, centerY - 50, ohColor);

            long remainSec = remoteOverheat / 20;
            String timeStr = String.format("%02d:%02d", remainSec / 60, remainSec % 60);
            drawCenteredShadowString(guiGraphics, timeStr, centerX, centerY - 40, 0xFFAAAAAA);
        }
    }

    private void drawCenteredShadowString(GuiGraphics guiGraphics, String text, float x, float y, int color) {
        int w = this.font.width(text);
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(x - (w / 2.0f) * TEXT_SCALE, y, 0);
        poseStack.scale(TEXT_SCALE, TEXT_SCALE, 1.0f);
        guiGraphics.drawString(this.font, text, 0, 0, color, true);
        poseStack.popPose();
    }

    private void renderSegmentedGauge(GuiGraphics guiGraphics, float cx, float cy, float rOut, float thickness, float startAngle, float totalAngle, float percent, int colorType, int tickCount) {
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
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = guiGraphics.pose().last().pose();
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
        RenderSystem.disableBlend();

        if (tickCount > 0) {
            float tickStep = totalAngle / (tickCount - 1);
            float tickLen = 4.0f;

            for (int i = 0; i < tickCount; i++) {
                float angle = startAngle + i * tickStep;
                float x1 = cx + (tickBaseR) * (float)Math.cos(Math.toRadians(angle));
                float y1 = cy + (tickBaseR) * (float)Math.sin(Math.toRadians(angle));

                PoseStack poseStack = guiGraphics.pose();
                poseStack.pushPose();
                poseStack.translate(x1, y1, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
                guiGraphics.fill(0, 0, (int)tickLen, 1, frameColor);
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
