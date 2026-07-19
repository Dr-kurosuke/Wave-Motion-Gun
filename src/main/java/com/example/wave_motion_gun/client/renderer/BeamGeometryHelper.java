package com.example.wave_motion_gun.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * ビーム系レンダラー共通のジオメトリ描画ヘルパー。
 * 座標はすべてレンダリング原点(エンティティ位置)基準の相対座標で渡すこと。
 */
public final class BeamGeometryHelper {

    public static final int FULL_BRIGHT = 0xF000F0;

    private BeamGeometryHelper() {}

    // --- 色分解 (0xRRGGBB -> 0.0~1.0) ---
    public static float red(int color)   { return ((color >> 16) & 0xFF) / 255.0F; }
    public static float green(int color) { return ((color >> 8) & 0xFF) / 255.0F; }
    public static float blue(int color)  { return (color & 0xFF) / 255.0F; }

    /**
     * ビームの始点と終点を含むAABBを作成してカリング判定を行う。
     * AABBコンストラクタは自動的にmin/maxを処理します。
     */
    public static boolean isBeamBoxVisible(Frustum frustum, Vec3 start, Vec3 delta, double inflate) {
        AABB box = new AABB(start.x, start.y, start.z, start.x + delta.x, start.y + delta.y, start.z + delta.z);
        return frustum.isVisible(box.inflate(inflate));
    }

    /**
     * 螺旋状に回転する分割ビームを描画する。
     * start/end はレンダリング原点基準の相対座標。
     */
    public static void drawSubdividedSegment(PoseStack poseStack, VertexConsumer builder, Vec3 start, Vec3 end,
                                             float radius, float r, float g, float b, float alpha, int packedLight,
                                             float time, float rotSpeed, float spiralFactor,
                                             float phaseStart, float phaseEnd) {

        Vec3 diff = end.subtract(start);
        float totalLength = (float) diff.length();
        int segments = Math.max(1, (int) (totalLength * 2.0f));

        Vector3f D = new Vector3f((float) diff.x, (float) diff.y, (float) diff.z);
        D.normalize();
        Vector3f ref = new Vector3f(0, 1, 0);
        if (Math.abs(D.y()) > 0.95f) ref = new Vector3f(1, 0, 0);
        Vector3f U = new Vector3f(D); U.cross(ref); U.normalize();
        Vector3f V = new Vector3f(D); V.cross(U);

        // ループ内でVector3fを確保しないよう、基底ベクトルをプリミティブに展開しておく
        float ux = U.x(), uy = U.y(), uz = U.z();
        float vx = V.x(), vy = V.y(), vz = V.z();

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        int sides = 6;

        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / segments;
            float t2 = (float) (i + 1) / segments;
            float p1 = Mth.lerp(t1, phaseStart, phaseEnd);
            float p2 = Mth.lerp(t2, phaseStart, phaseEnd);
            float rot1 = (time * rotSpeed) + (p1 * spiralFactor);
            float rot2 = (time * rotSpeed) + (p2 * spiralFactor);

            Vec3 pos1 = start.add(diff.scale(t1));
            Vec3 pos2 = start.add(diff.scale(t2));

            for (int j = 0; j < sides; j++) {
                float baseAngle1 = ((float) j / sides) * (float) Math.PI * 2.0F;
                float baseAngle2 = ((float) (j + 1) / sides) * (float) Math.PI * 2.0F;
                float ang1s = baseAngle1 + rot1;
                float ang2s = baseAngle2 + rot1;
                float ang1e = baseAngle1 + rot2;
                float ang2e = baseAngle2 + rot2;

                // オフセットをインライン計算 (Vector3fの一時オブジェクト確保を回避)
                float c1s = Mth.cos(ang1s) * radius, s1s = Mth.sin(ang1s) * radius;
                float c2s = Mth.cos(ang2s) * radius, s2s = Mth.sin(ang2s) * radius;
                float c1e = Mth.cos(ang1e) * radius, s1e = Mth.sin(ang1e) * radius;
                float c2e = Mth.cos(ang2e) * radius, s2e = Mth.sin(ang2e) * radius;

                float x1s = (float)pos1.x + ux * c1s + vx * s1s; float y1s = (float)pos1.y + uy * c1s + vy * s1s; float z1s = (float)pos1.z + uz * c1s + vz * s1s;
                float x2s = (float)pos1.x + ux * c2s + vx * s2s; float y2s = (float)pos1.y + uy * c2s + vy * s2s; float z2s = (float)pos1.z + uz * c2s + vz * s2s;
                float x1e = (float)pos2.x + ux * c1e + vx * s1e; float y1e = (float)pos2.y + uy * c1e + vy * s1e; float z1e = (float)pos2.z + uz * c1e + vz * s1e;
                float x2e = (float)pos2.x + ux * c2e + vx * s2e; float y2e = (float)pos2.y + uy * c2e + vy * s2e; float z2e = (float)pos2.z + uz * c2e + vz * s2e;

                // 四角形を描画 (entityTranslucentはカリング無効なので裏面用の重複描画は不要)
                vertex(builder, pose, normal, x1e, y1e, z1e, 0, 0, r, g, b, alpha, packedLight);
                vertex(builder, pose, normal, x1s, y1s, z1s, 0, 1, r, g, b, alpha, packedLight);
                vertex(builder, pose, normal, x2s, y2s, z2s, 1, 1, r, g, b, alpha, packedLight);
                vertex(builder, pose, normal, x2e, y2e, z2e, 1, 0, r, g, b, alpha, packedLight);
            }
        }
    }

    /**
     * 回転する六角柱を描画する。start/end はレンダリング原点基準の相対座標。
     */
    public static void drawCylinder(PoseStack poseStack, VertexConsumer builder, Vec3 start, Vec3 end,
                                    float radius, float r, float g, float b, float alpha, int packedLight,
                                    float time, float rotSpeed) {

        Vec3 diff = end.subtract(start);

        // 軸ベクトルの計算
        Vector3f D = new Vector3f((float) diff.x, (float) diff.y, (float) diff.z);
        D.normalize();

        // 軸に垂直なベクトル U, V を生成 (ビルボード用)
        Vector3f ref = new Vector3f(0, 1, 0);
        if (Math.abs(D.y()) > 0.95f) ref = new Vector3f(1, 0, 0);
        Vector3f U = new Vector3f(D); U.cross(ref); U.normalize();
        Vector3f V = new Vector3f(D); V.cross(U);

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        // 回転アニメーション
        float rotation = time * rotSpeed;

        int sides = 6; // 六角柱

        // 円筒の描画
        for (int j = 0; j < sides; j++) {
            float baseAngle1 = ((float) j / sides) * (float) Math.PI * 2.0F;
            float baseAngle2 = ((float) (j + 1) / sides) * (float) Math.PI * 2.0F;

            float ang1 = baseAngle1 + rotation;
            float ang2 = baseAngle2 + rotation;

            Vector3f v1 = getOffset(U, V, ang1, radius);
            Vector3f v2 = getOffset(U, V, ang2, radius);

            // 始点側の頂点
            float x1s = (float)start.x + v1.x(); float y1s = (float)start.y + v1.y(); float z1s = (float)start.z + v1.z();
            float x2s = (float)start.x + v2.x(); float y2s = (float)start.y + v2.y(); float z2s = (float)start.z + v2.z();

            // 終点側の頂点
            float x1e = (float)end.x + v1.x(); float y1e = (float)end.y + v1.y(); float z1e = (float)end.z + v1.z();
            float x2e = (float)end.x + v2.x(); float y2e = (float)end.y + v2.y(); float z2e = (float)end.z + v2.z();

            // クワッドを描画 (entityTranslucentはカリング無効なので裏面用の重複描画は不要)
            vertex(builder, pose, normal, x1e, y1e, z1e, 0, 0, r, g, b, alpha, packedLight);
            vertex(builder, pose, normal, x1s, y1s, z1s, 0, 1, r, g, b, alpha, packedLight);
            vertex(builder, pose, normal, x2s, y2s, z2s, 1, 1, r, g, b, alpha, packedLight);
            vertex(builder, pose, normal, x2e, y2e, z2e, 1, 0, r, g, b, alpha, packedLight);
        }
    }

    private static Vector3f getOffset(Vector3f U, Vector3f V, float angle, float radius) {
        Vector3f vec = new Vector3f(U);
        vec.mul(Mth.cos(angle) * radius);
        Vector3f temp = new Vector3f(V);
        temp.mul(Mth.sin(angle) * radius);
        vec.add(temp);
        return vec;
    }

    public static void vertex(VertexConsumer builder, Matrix4f pose, Matrix3f normal, float x, float y, float z, float u, float v, float r, float g, float b, float a, int packedLight) {
        builder.vertex(pose, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0).endVertex();
    }
}
