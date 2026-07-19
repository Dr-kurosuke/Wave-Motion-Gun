package com.example.wave_motion_gun.utils;

import com.example.wave_motion_gun.ExampleMod;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * コード側から実績(Advancement)を付与するためのユーティリティ。
 * 実績JSON側は criteria "code_granted" (minecraft:impossible) を持つ想定だが、
 * 残っている全criteriaを付与するため、criteria名には依存しない。
 * 付与済みの場合は何もしない(冪等)。
 */
public class AdvancementHelper {

    /**
     * 指定プレイヤーへ story/<path> の実績を付与する。
     * サーバー未取得・実績未登録の場合は何もしない(null安全)。
     */
    public static void grant(ServerPlayer player, String path) {
        if (player == null) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;

        ResourceLocation id = new ResourceLocation(ExampleMod.MODID, "story/" + path);
        Advancement advancement = server.getAdvancements().getAdvancement(id);
        if (advancement == null) return;

        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) return;

        // 残っている全criteriaを付与して達成扱いにする
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    /**
     * 指定座標から半径radius以内にいる全プレイヤーへ実績を付与する。
     * (充填完了・オーバーヒート等、ブロック起点で操作者を特定できないイベント用)
     */
    public static void grantNearby(ServerLevel level, BlockPos pos, double radius, String path) {
        if (level == null || pos == null) return;
        double radiusSqr = radius * radius;
        // VS2船上のブロック(シップヤード座標)でもプレイヤー(ワールド座標)に届くよう変換する
        Vec3 center = com.example.wave_motion_gun.compat.VSCompat.worldCenterOf(level, pos);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center) <= radiusSqr) {
                grant(player, path);
            }
        }
    }
}
