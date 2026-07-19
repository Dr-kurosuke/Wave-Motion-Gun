package com.example.wave_motion_gun.compat;

import com.example.wave_motion_gun.ExampleMod;
import com.example.wave_motion_gun.init.ItemInit;
// --- 対象の画面クラス ---
import com.example.wave_motion_gun.client.MonitoringUnitScreen;
import com.example.wave_motion_gun.client.TriggerUnitScreen;
import com.example.wave_motion_gun.client.MessengerScreen;
import com.example.wave_motion_gun.client.ShockCannonScreen;
import com.example.wave_motion_gun.client.SupplyCrateScreen;
// --------------------
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@JeiPlugin
public class WaveCannonJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(ExampleMod.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new CommunicationBeaconRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<ItemStack> rewards = new ArrayList<>();
        rewards.add(new ItemStack(ItemInit.RAW_COSMO_ORE.get()));
        rewards.add(new ItemStack(ItemInit.TACHYON_CRYSTAL.get()));
        rewards.add(new ItemStack(ItemInit.GRAVITY_LENS.get()));
        rewards.add(new ItemStack(ItemInit.QUANTUM_UNIT.get()));
        rewards.add(new ItemStack(ItemInit.WAVE_CORE.get()));
        rewards.add(new ItemStack(ItemInit.CLEARANCE_S.get()));
        rewards.add(new ItemStack(ItemInit.CLEARANCE_A.get()));
        rewards.add(new ItemStack(ItemInit.CLEARANCE_B.get()));
        rewards.add(new ItemStack(ItemInit.CLEARANCE_C.get()));

        registration.addRecipes(CommunicationBeaconRecipeCategory.RECIPE_TYPE, rewards);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ItemInit.COMMUNICATION_BEACON.get()), CommunicationBeaconRecipeCategory.RECIPE_TYPE);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // グローバルハンドラを使用し、画面内外を含む超巨大な領域を除外指定します。
        registration.addGlobalGuiHandler(new IGlobalGuiHandler() {
            @Override
            public Collection<Rect2i> getGuiExtraAreas() {
                Minecraft mc = Minecraft.getInstance();
                Screen screen = mc.screen;

                // 対象の画面クラス、または GUI非表示モードの場合
                if (mc.options.hideGui ||
                        screen instanceof TriggerUnitScreen ||
                        screen instanceof MonitoringUnitScreen ||
                        screen instanceof MessengerScreen ||
                        screen instanceof ShockCannonScreen ||
                        screen instanceof SupplyCrateScreen) {

                    // 【修正】画面内だけでなく、画面外(マイナス座標)も含めた超巨大な領域を指定します。
                    // 左側のブックマークリストは画面左端(x=0付近)に表示されるため、
                    // マイナス座標から塗りつぶすことで、JEIに「左側にもスペースはない」と認識させます。
                    return Collections.singletonList(new Rect2i(-10000, -10000, 20000, 20000));
                }

                return Collections.emptyList();
            }
        });
    }
}