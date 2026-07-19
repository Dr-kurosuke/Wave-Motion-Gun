package com.example.wave_motion_gun.item;

import com.example.wave_motion_gun.world.data.SupplyDropManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CoordinateDatapadItem extends Item {

    public CoordinateDatapadItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("TargetX")) {
                int x = tag.getInt("TargetX");
                int z = tag.getInt("TargetZ");
                long timestamp = tag.getLong("Timestamp");
                long currentTime = level.getGameTime();

                // 経過時間を計算
                long elapsed = currentTime - timestamp;
                long remaining = SupplyDropManager.EVENT_DURATION - elapsed;

                player.displayClientMessage(new TranslatableComponent("message.wave_motion_gun_mod.datapad.header").withStyle(ChatFormatting.AQUA), false);
                player.displayClientMessage(new TranslatableComponent("message.wave_motion_gun_mod.datapad.target", String.valueOf(x), String.valueOf(z)), false);

                if (remaining > 0) {
                    // ★修正: 時間フォーマットの変更
                    // 時刻の書式(「%s分 %s秒」等)はクライアント側の言語ファイルで解決される
                    int totalSeconds = (int) (remaining / 20.0);
                    Component timeText;
                    if (totalSeconds >= 60.0) {
                        int minutes = (int) (totalSeconds / 60);
                        int seconds = (int) (totalSeconds % 60);
                        timeText = new TranslatableComponent("time.wave_motion_gun_mod.min_sec", String.valueOf(minutes), String.valueOf(seconds));
                    } else {
                        timeText = new TranslatableComponent("time.wave_motion_gun_mod.sec", String.valueOf(totalSeconds));
                    }
                    player.displayClientMessage(new TranslatableComponent("message.wave_motion_gun_mod.datapad.eta", timeText).withStyle(ChatFormatting.YELLOW), false);
                } else {
                    player.displayClientMessage(new TranslatableComponent("message.wave_motion_gun_mod.datapad.arrived").withStyle(ChatFormatting.RED), false);
                }
            } else {
                player.displayClientMessage(new TranslatableComponent("message.wave_motion_gun_mod.datapad.no_data").withStyle(ChatFormatting.GRAY), true);
            }
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("TargetX")) {
            tooltipComponents.add(new TranslatableComponent("tooltip.wave_motion_gun_mod.datapad.target",
                    String.valueOf(tag.getInt("TargetX")), String.valueOf(tag.getInt("TargetZ"))).withStyle(ChatFormatting.GRAY));

            // ★追加: ツールチップにも残り時間を表示
            if (level != null && tag.contains("Timestamp")) {
                long timestamp = tag.getLong("Timestamp");
                long elapsed = level.getGameTime() - timestamp;
                long remaining = SupplyDropManager.EVENT_DURATION - elapsed;

                if (remaining > 0) {
                    double totalSeconds = remaining / 20.0;
                    Component timeStr;
                    if (totalSeconds >= 60.0) {
                        int minutes = (int) (totalSeconds / 60);
                        double seconds = totalSeconds % 60;
                        timeStr = new TranslatableComponent("time.wave_motion_gun_mod.min_sec", String.valueOf(minutes), String.format("%.1f", seconds));
                    } else {
                        timeStr = new TranslatableComponent("time.wave_motion_gun_mod.sec", String.format("%.1f", totalSeconds));
                    }
                    tooltipComponents.add(new TranslatableComponent("tooltip.wave_motion_gun_mod.datapad.time_left", timeStr).withStyle(ChatFormatting.YELLOW));
                } else {
                    tooltipComponents.add(new TranslatableComponent("tooltip.wave_motion_gun_mod.datapad.arrived").withStyle(ChatFormatting.RED));
                }
            }
        } else {
            tooltipComponents.add(new TranslatableComponent("tooltip.wave_motion_gun_mod.datapad.no_data").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}