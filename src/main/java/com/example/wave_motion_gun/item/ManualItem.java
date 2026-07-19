package com.example.wave_motion_gun.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 波動砲の建造・運用を解説するマニュアルアイテム。右クリックで専用画面を開く。
 *
 * 画面(Screen)の参照はクライアント専用クラスなので、@OnlyIn(Dist.CLIENT) の
 * 別メソッドに隔離する。専用サーバーではこのメソッドごと除去されるため安全
 * (MessengerEntity.openGui と同じ実績のあるパターン)。
 */
public class ManualItem extends Item {

    /** どの整備手帳か(波動砲用/ショックカノン用) */
    public enum BookType { WAVE, SHOCK }

    private final BookType bookType;

    public ManualItem(Properties properties, BookType bookType) {
        super(properties);
        this.bookType = bookType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            openScreen();
        }
        // クライアントでは手を振らせ、サーバー側では何もしない
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @OnlyIn(Dist.CLIENT)
    private void openScreen() {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                bookType == BookType.SHOCK
                        ? com.example.wave_motion_gun.client.ManualScreen.shock()
                        : com.example.wave_motion_gun.client.ManualScreen.wave());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.wave_motion_gun_mod.manual.hint").withStyle(ChatFormatting.GRAY));
    }
}
