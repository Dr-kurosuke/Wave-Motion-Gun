package com.example.wave_motion_gun.compat;

import com.example.wave_motion_gun.ExampleMod;
import com.example.wave_motion_gun.init.ItemInit;
import com.example.wave_motion_gun.utils.QuestionManager; // QuestionManagerをインポート
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommunicationBeaconRecipeCategory implements IRecipeCategory<ItemStack> {

    public static final RecipeType<ItemStack> RECIPE_TYPE = RecipeType.create(ExampleMod.MODID, "communication_beacon", ItemStack.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slotDrawable;
    private final Component title;

    public CommunicationBeaconRecipeCategory(IGuiHelper guiHelper) {
        // 背景: 無地のキャンバス (幅140, 高さ50)
        this.background = guiHelper.createBlankDrawable(140, 50);

        // スロット枠
        this.slotDrawable = guiHelper.getSlotDrawable();

        // アイコン
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ItemInit.COMMUNICATION_BEACON.get()));
        this.title = Component.translatable("jei.wave_motion_gun_mod.communication_beacon.title");
    }

    @Override
    public RecipeType<ItemStack> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ItemStack recipe, IFocusGroup focuses) {
        // Outputスロット（報酬）
        builder.addSlot(RecipeIngredientRole.OUTPUT, 110, 15)
                .addItemStack(recipe)
                .setBackground(slotDrawable, -1, -1);
    }

    @Override
    public void draw(ItemStack recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = guiGraphics.pose();

        // 左側の "?" マーク
        poseStack.pushPose();
        poseStack.scale(2.5f, 2.5f, 1.0f);
        guiGraphics.drawString(mc.font, "?", 6, 5, 0xFF000000, false);
        poseStack.popPose();

        // 中央の矢印とメッセージ
        guiGraphics.drawString(mc.font, ">>>", 60, 20, 0xFF404040, false);
        guiGraphics.drawString(mc.font, Component.translatable("jei.wave_motion_gun_mod.communication_beacon.communicate"), 40, 5, 0xFF555555, false);

        // --- 下部のコメント欄（クリアランス表示） ---

        // 1. アイテムのクリアランスランクを判定
        String rank = getClearanceLevel(recipe);

        // 2. ランクに応じた表示色とテキストを決定
        Component displayText;
        int color;

        switch (rank) {
            case "S":
                displayText = Component.translatable("jei.wave_motion_gun_mod.communication_beacon.clearance_s");
                color = 0xFFFFAA00; // Gold
                break;
            case "A":
                displayText = Component.translatable("jei.wave_motion_gun_mod.communication_beacon.clearance_a");
                color = 0xFF55FFFF; // Aqua
                break;
            case "B":
                displayText = Component.translatable("jei.wave_motion_gun_mod.communication_beacon.clearance_b");
                color = 0xFFFFFF55; // Yellow
                break;
            case "C":
                displayText = Component.translatable("jei.wave_motion_gun_mod.communication_beacon.clearance_c");
                color = 0xFFFFFFFF; // White
                break;
            case "D":
                displayText = Component.translatable("jei.wave_motion_gun_mod.communication_beacon.clearance_d");
                color = 0xFFAAAAAA; // Gray
                break;
            default:
                displayText = Component.translatable("jei.wave_motion_gun_mod.communication_beacon.clearance_unknown");
                color = 0xFF555555; // Dark Gray
                break;
        }

        // 3. 描画
        float scale = 0.7f;
        poseStack.pushPose();
        poseStack.scale(scale, scale, 1.0f);
        guiGraphics.drawString(mc.font, displayText, 5, 60, color, false);
        poseStack.popPose();
    }

    // 【最適化】アイテム→クリアランスランクの対応表。
    // 以前はdraw()が毎フレーム全問題チェーンを走査していたため、初回に一度だけ構築してキャッシュする
    private static Map<Item, String> clearanceCache = null;

    /**
     * ItemStackを受け取り、対応するクリアランスランク(S~D)を返す。
     * 低ランク(D)から順に登録するため、複数グループに含まれるアイテムは入手可能な最低ランクになる。
     */
    private static String getClearanceLevel(ItemStack stack) {
        if (clearanceCache == null) {
            Map<Item, String> map = new HashMap<>();
            String[] ranks = {"D", "C", "B", "A", "S"}; // Group 1..5 に対応
            for (int groupId = 1; groupId <= 5; groupId++) {
                List<Integer> chainIndices = QuestionManager.GROUP_MAP.get(groupId);
                if (chainIndices == null) continue;
                for (int index : chainIndices) {
                    QuestionManager.QuestionChain chain = QuestionManager.getChain(index);
                    if (chain != null && chain.rewards != null) {
                        for (ItemStack reward : chain.rewards) {
                            // 低ランク優先 (既登録のアイテムは上書きしない)。個数やNBTは無視
                            map.putIfAbsent(reward.getItem(), ranks[groupId - 1]);
                        }
                    }
                }
            }
            clearanceCache = map;
        }
        return clearanceCache.getOrDefault(stack.getItem(), "Unknown");
    }
}