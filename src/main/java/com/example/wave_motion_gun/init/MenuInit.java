package com.example.wave_motion_gun.init;

import com.example.wave_motion_gun.ExampleMod;
import com.example.wave_motion_gun.menu.MonitoringUnitMenu;
import com.example.wave_motion_gun.menu.ShockCannonMenu;
import com.example.wave_motion_gun.menu.SupplyCrateMenu; // 追加
import com.example.wave_motion_gun.menu.TriggerUnitMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MenuInit {
    // MODIDの参照を ExampleMod.MODID に統一 (MOD_IDではなくMODID)
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.CONTAINERS, ExampleMod.MODID);

    public static final RegistryObject<MenuType<MonitoringUnitMenu>> MONITORING_UNIT_MENU =
            MENUS.register("monitoring_unit_menu", () -> IForgeMenuType.create(MonitoringUnitMenu::new));

    public static final RegistryObject<MenuType<TriggerUnitMenu>> TRIGGER_UNIT_MENU =
            MENUS.register("trigger_unit_menu", () -> IForgeMenuType.create(TriggerUnitMenu::new));

    // 【追加】補給コンテナ用メニュー
    public static final RegistryObject<MenuType<SupplyCrateMenu>> SUPPLY_CRATE_MENU =
            MENUS.register("supply_crate_menu", () -> IForgeMenuType.create(SupplyCrateMenu::new));

    public static final RegistryObject<MenuType<ShockCannonMenu>> SHOCK_CANNON_MENU = MENUS.register("shock_cannon_menu",
            () -> IForgeMenuType.create(ShockCannonMenu::new));

}