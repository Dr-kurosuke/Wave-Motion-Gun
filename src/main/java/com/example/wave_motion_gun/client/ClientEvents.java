package com.example.wave_motion_gun.client;

import com.example.wave_motion_gun.ExampleMod;
import com.example.wave_motion_gun.entity.CameraEntity;
import com.example.wave_motion_gun.item.CoordinateDatapadItem;
import com.example.wave_motion_gun.world.data.SupplyDropManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ClientEvents {

    // --- HUD文字列キャッシュ (毎フレームのI18n.get/String.formatを回避) ---
    private static int cachedTargetX = Integer.MIN_VALUE;
    private static int cachedTargetZ = Integer.MIN_VALUE;
    private static String cachedCoordText = "";
    private static long cachedTimeKey = Long.MIN_VALUE; // 0.1秒粒度の残り時間 (着弾済みは-1)
    private static String cachedTimeText = "";
    private static int cachedCurrentX = Integer.MIN_VALUE;
    private static int cachedCurrentZ = Integer.MIN_VALUE;
    private static String cachedCurrentText = "";

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (Minecraft.getInstance().cameraEntity instanceof CameraEntity) {
            event.setCanceled(true);
        }
    }

    // ワールド退出時にAI問題のローカルキャッシュとセッションを破棄する
    // (エンティティIDはワールドごとに振り直されるため、持ち越すと誤表示の恐れがある)
    @SubscribeEvent
    public static void onLoggedOut(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        com.example.wave_motion_gun.client.ai.AiQuestionCache.clear();
        com.example.wave_motion_gun.client.ai.AiSessionManager.clearAll();
    }

    @SubscribeEvent
    public static void onRenderGameOverlay(RenderGuiEvent.Post event) {

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof CoordinateDatapadItem)) return;

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("TargetX")) return;

        int tx = tag.getInt("TargetX");
        int tz = tag.getInt("TargetZ");
        long timestamp = tag.getLong("Timestamp");
        long elapsed = player.level().getGameTime() - timestamp;
        long remaining = SupplyDropManager.EVENT_DURATION - elapsed;

        // 目標座標: 値が変わった時だけ文字列を作り直す
        if (tx != cachedTargetX || tz != cachedTargetZ) {
            cachedTargetX = tx;
            cachedTargetZ = tz;
            cachedCoordText = I18n.get("hud.wave_motion_gun_mod.target", tx, tz);
        }
        String coordText = cachedCoordText;

        boolean arrived = remaining <= 0;
        // 表示は0.1秒粒度なので、その粒度で値が変わった時だけフォーマットし直す
        long timeKey = arrived ? -1L : Math.round(remaining / 2.0);
        if (timeKey != cachedTimeKey) {
            cachedTimeKey = timeKey;
            if (arrived) {
                cachedTimeText = I18n.get("hud.wave_motion_gun_mod.arrived");
            } else {
                double totalSeconds = remaining / 20.0;
                String timeStr;
                if (totalSeconds >= 60.0) {
                    int minutes = (int) (totalSeconds / 60);
                    double seconds = totalSeconds % 60;
                    timeStr = I18n.get("time.wave_motion_gun_mod.min_sec", minutes, String.format("%.1f", seconds));
                } else {
                    timeStr = I18n.get("time.wave_motion_gun_mod.sec", String.format("%.1f", totalSeconds));
                }
                cachedTimeText = I18n.get("hud.wave_motion_gun_mod.eta", timeStr);
            }
        }
        String timeText = cachedTimeText;

        Font font = mc.font;
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int y = screenHeight / 2;
        int x = 5;

        // ★修正: 着弾済みなら現在地を表示しない
        if (!arrived) {
            int cx = player.blockPosition().getX();
            int cz = player.blockPosition().getZ();
            // 現在地: 座標が変わった時だけ文字列を作り直す
            if (cx != cachedCurrentX || cz != cachedCurrentZ) {
                cachedCurrentX = cx;
                cachedCurrentZ = cz;
                cachedCurrentText = I18n.get("hud.wave_motion_gun_mod.current", cx, cz);
            }
            guiGraphics.drawString(font, cachedCurrentText, x, y - 10, 0xFFFFFF, true);
        }

        guiGraphics.drawString(font, coordText, x, y, 0xFFFFFF, true);
        guiGraphics.drawString(font, timeText, x, y + 10, 0xFFFFFF, true);
    }
}