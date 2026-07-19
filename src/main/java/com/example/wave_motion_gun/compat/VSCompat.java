package com.example.wave_motion_gun.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

/**
 * Valkyrien Skies 2 (VS2) 互換レイヤー。
 *
 * VS2の船に載ったブロックは、見た目の位置とは別の「シップヤード」座標(超遠方)に
 * 実体が置かれる。そのためブロック座標を素のまま使うと、
 *  - 距離検証: 船上のGUI操作パケットが「遠隔操作」として誤って拒否される
 *  - エンティティ生成: ビーム等が見た目の砲口ではなくシップヤードに出現する
 *  - 付近プレイヤー検索/効果音: プレイヤーはワールド座標にいるため届かない
 * といった問題が起きる。VS2導入時はVS2公式APIへ委譲して船の変換行列を考慮し、
 * 未導入時は素の座標をそのまま使うフォールバックとする。
 */
public final class VSCompat {
    private static final boolean VS2_LOADED = ModList.get().isLoaded("valkyrienskies");

    private VSCompat() {
    }

    /**
     * プレイヤーが指定ブロックの操作可能範囲内にいるかを、VS2の船を考慮して判定する。
     * パケットハンドラのサーバー権威検証(改造クライアント対策)用。
     */
    public static boolean isWithinReach(Player player, BlockPos pos, double maxDistance) {
        Vec3 target = Vec3.atCenterOf(pos);
        double distSqr = VS2_LOADED
                ? VS2Handler.squaredDistanceInclShips(player, target)
                : player.distanceToSqr(target);
        return distSqr <= maxDistance * maxDistance;
    }

    /**
     * コンテナGUIを開き続けてよいかの判定 (AbstractContainerMenu#stillValid のVS2対応版)。
     *
     * バニラの stillValid は素の player.distanceToSqr で 8ブロックを見るため、
     * シップヤード座標にある船上ブロックでは必ず false になり、
     * GUIを開いた次のtickでサーバーから強制クローズされてしまう。
     * パケット側の距離検証をVS2対応しても、ここが素のままだと船上では何も操作できない。
     *
     * ブロックの同一性チェックはシップヤード座標のまま行うのが正しい
     * (ワールド座標側にはそのブロックは存在しないため)。
     */
    public static boolean stillValidContainer(Level level, BlockPos pos, Player player,
                                              net.minecraft.world.level.block.Block expected) {
        if (!level.getBlockState(pos).is(expected)) return false;
        return isWithinReach(player, pos, 8.0); // バニラの ContainerLevelAccess と同じ 8ブロック
    }

    /**
     * シップヤード座標をワールド座標へ変換する。船上でなければそのまま返す。
     * エンティティ生成位置・効果音位置・付近プレイヤー検索の基準点に使う。
     */
    public static Vec3 toWorldPos(Level level, Vec3 pos) {
        return VS2_LOADED ? VS2Handler.toWorld(level, pos) : pos;
    }

    /** ブロック中心のワールド座標版ショートカット。 */
    public static Vec3 worldCenterOf(Level level, BlockPos pos) {
        return toWorldPos(level, Vec3.atCenterOf(pos));
    }

    /**
     * シップヤード座標系の方向ベクトルを、船の回転を考慮したワールド座標系の方向へ変換する。
     * 基点refPosと先端(refPos+dir)の2点をそれぞれ変換して差分を取るため、
     * 船が傾いていても正しい向きになる。船上でなければdirをそのまま返す。
     */
    public static Vec3 toWorldDirection(Level level, Vec3 dir, Vec3 refPos) {
        if (!VS2_LOADED) return dir;
        Vec3 base = VS2Handler.toWorld(level, refPos);
        Vec3 tip = VS2Handler.toWorld(level, refPos.add(dir));
        Vec3 d = tip.subtract(base);
        // 変換の丸め誤差で長さが変わらないよう、元の長さに正規化する
        return d.lengthSqr() < 1.0e-8 ? dir : d.normalize().scale(dir.length());
    }

    /**
     * VS2クラスへの参照はこの内部クラスに隔離する。
     * 外側クラスからはVS2_LOADED確認後にのみ触ること
     * (VS2未導入環境でVS2クラスの解決が走りNoClassDefFoundErrorになる事故を防ぐ)。
     */
    private static final class VS2Handler {
        /**
         * toWorldCoordinatesにはShip型を引数に取るオーバーロードがあり、Shipは
         * VS2 jar内部のネストjar(jarjar)にしか無いため直接呼ぶとコンパイルできない。
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
                // VS2側のAPI変更時は座標変換なしで動作継続する(距離検証は別APIなので影響しない)
                com.mojang.logging.LogUtils.getLogger()
                        .warn("[wave_motion_gun] VS2 toWorldCoordinates not found; ship coordinate transform disabled", e);
                return null;
            }
        }

        static double squaredDistanceInclShips(Player player, Vec3 target) {
            return org.valkyrienskies.mod.common.VSGameUtilsKt.squaredDistanceBetweenInclShips(
                    player.level(),
                    player.getX(), player.getY(), player.getZ(),
                    target.x, target.y, target.z);
        }

        /**
         * 呼び出し失敗の回数。毎tick呼ばれる経路があるためログは間引くが、
         * 「1回だけ」にすると恒久的な故障が以後まったくログに出なくなる。
         * 変換に失敗したときの症状は「レディが湧かない・音が鳴らない・弾が出ない」という
         * 無反応であり例外のように目立たないため、手がかりを定期的に残す必要がある。
         */
        private static final java.util.concurrent.atomic.AtomicLong invokeFailures =
                new java.util.concurrent.atomic.AtomicLong();

        static Vec3 toWorld(Level level, Vec3 pos) {
            if (TO_WORLD == null) return pos;
            try {
                return (Vec3) TO_WORLD.invokeExact(level, pos);
            } catch (Throwable t) {
                // lookup失敗時は「変換なしで継続」なのに、invoke失敗時だけ例外で落とすのは不整合。
                // この経路はパーティクル等の毎tick処理からも呼ばれるため、投げるとログが溢れるか
                // サーバーが落ちる。変換なしにフォールバックする。
                long n = invokeFailures.incrementAndGet();
                // 初回と、以後は指数的に間引いて記録する(1, 10, 100, 1000, ...回目)
                if (n == 1 || isPowerOfTen(n)) {
                    com.mojang.logging.LogUtils.getLogger()
                            .warn("[wave_motion_gun] VS2 toWorldCoordinates call failed ({} times); "
                                    + "falling back to raw coordinates "
                                    + "(ship-mounted blocks will misbehave)", n, t);
                }
                return pos;
            }
        }

        private static boolean isPowerOfTen(long n) {
            for (long p = 10; p <= n; p *= 10) {
                if (p == n) return true;
            }
            return false;
        }
    }
}
