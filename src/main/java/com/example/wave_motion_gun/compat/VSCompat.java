package com.example.wave_motion_gun.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

/**
 * Valkyrien Skies 2 連携。VS2未導入環境でも安全に動作する。
 *
 * <p>1.18.2版は 1.20.1版と違いフル機能のVSCompatを持たない。ここにあるのは
 * 「船の現在の方位角を得る」ためだけの最小実装。
 *
 * <p>VS2の {@code Ship} / {@code ShipTransform} はVS2 jar内部のネストjar(jarjar)にしか
 * 存在せずコンパイルクラスパスに乗らないため、回転を直接問い合わせることはできない。
 * 代わりに、メインjarにある {@code toWorldCoordinates(Level, Vec3)} だけを使い、
 * シップヤード座標上の2点をワールド変換して差分から方位角を求める。
 */
public final class VSCompat {

    private static final boolean VS2_LOADED = ModList.get().isLoaded("valkyrienskies");

    private VSCompat() {}

    public static boolean isLoaded() {
        return VS2_LOADED;
    }

    /**
     * 指定位置の船が、ワールド座標系で向いている方位角(度)を返す。
     *
     * <p>シップヤード座標のローカル+Z軸をワールドへ変換し、その向きをMinecraftの
     * yaw規約(南=0/西=90/北=180/東=270)に直したもの。船が回頭すると値が変化するので、
     * 2時点の差を取れば回頭量が求まる。
     *
     * @return 方位角(度)。VS2未導入・変換不可・機首が真上/真下を向いている場合は {@code NaN}
     */
    public static float shipYawAt(Level level, BlockPos pos) {
        if (!VS2_LOADED || level == null || pos == null) return Float.NaN;

        Vec3 base = Vec3.atCenterOf(pos);
        Vec3 originWorld = VS2Handler.toWorld(level, base);
        Vec3 forwardWorld = VS2Handler.toWorld(level, base.add(0.0D, 0.0D, 1.0D));
        if (originWorld == null || forwardWorld == null) return Float.NaN;

        Vec3 dir = forwardWorld.subtract(originWorld);
        // 機首が真上/真下を向いていると水平成分が消え、方位角が定義できない
        double horizontalSqr = dir.x * dir.x + dir.z * dir.z;
        if (horizontalSqr < 1.0e-8D) return Float.NaN;

        return (float) (Mth.atan2(dir.z, dir.x) * (180.0D / Math.PI)) - 90.0F;
    }

    /**
     * VS2クラスへの参照はこの内部クラスに隔離する。
     * 外側クラスからはVS2_LOADED確認後にのみ触ること
     * (VS2未導入環境でVS2クラスの解決が走りNoClassDefFoundErrorになる事故を防ぐ)。
     */
    private static final class VS2Handler {
        /**
         * toWorldCoordinatesにはShip型を引数に取るオーバーロードがあり、Shipは
         * VS2 jar内部のネストjarにしか無いため直接呼ぶとコンパイルできない。
         * シグネチャを(Level, Vec3)に固定できるMethodHandle経由で呼び出す。
         */
        private static final java.lang.invoke.MethodHandle TO_WORLD = lookupToWorld();

        private static java.lang.invoke.MethodHandle lookupToWorld() {
            try {
                return java.lang.invoke.MethodHandles.publicLookup().findStatic(
                        org.valkyrienskies.mod.common.VSGameUtilsKt.class,
                        "toWorldCoordinates",
                        java.lang.invoke.MethodType.methodType(Vec3.class, Level.class, Vec3.class));
            } catch (ReflectiveOperationException e) {
                // VS2側のAPI変更時は方位角取得を諦める(旋回制限が効かなくなるだけ)
                com.mojang.logging.LogUtils.getLogger()
                        .warn("[wave_motion_gun] VS2 toWorldCoordinates not found; ship yaw unavailable", e);
                return null;
            }
        }

        static Vec3 toWorld(Level level, Vec3 pos) {
            if (TO_WORLD == null) return null;
            try {
                return (Vec3) TO_WORLD.invokeExact(level, pos);
            } catch (Throwable t) {
                return null;
            }
        }
    }
}
