package com.example.wave_motion_gun.init;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class CreativeTabInit {
    public static final CreativeModeTab WAVE_CANNON_TAB = new CreativeModeTab("wave_cannon_tab") {
        @Override
        public ItemStack makeIcon() {
            // タブのアイコンとして表示するアイテム（ここでは波動砲本体）
            return new ItemStack(ItemInit.WAVE_CANNON.get());
        }
    };
}