package com.example.wave_motion_gun.init;

import com.example.wave_motion_gun.ExampleMod;
import com.example.wave_motion_gun.item.CoordinateDatapadItem; // 新規インポート
import com.example.wave_motion_gun.item.ManualItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemInit {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ExampleMod.MODID);

    // --- 既存アイテム ---
    public static final RegistryObject<Item> WAVE_CANNON = ITEMS.register("wave_cannon",
            () -> new BlockItem(BlockInit.WAVE_CANNON.get(), new Item.Properties()));

    public static final RegistryObject<Item> TRIGGER_UNIT = ITEMS.register("trigger_unit",
            () -> new BlockItem(BlockInit.TRIGGER_UNIT.get(), new Item.Properties()));

    public static final RegistryObject<Item> MONITORING_UNIT = ITEMS.register("monitoring_unit",
            () -> new BlockItem(BlockInit.MONITORING_UNIT.get(), new Item.Properties()));

    public static final RegistryObject<Item> WAVE_ENERGY_STORAGE = ITEMS.register("wave_energy_storage",
            () -> new BlockItem(BlockInit.WAVE_ENERGY_STORAGE.get(), new Item.Properties()));

    public static final RegistryObject<Item> BARREL_UNIT = ITEMS.register("barrel_unit",
            () -> new BlockItem(BlockInit.BARREL_UNIT.get(), new Item.Properties()));

    public static final RegistryObject<Item> SEAT_BLOCK = ITEMS.register("seat_block",
            () -> new BlockItem(BlockInit.SEAT_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> COMMUNICATION_BEACON = ITEMS.register("communication_beacon",
            () -> new BlockItem(BlockInit.COMMUNICATION_BEACON.get(),
                    new Item.Properties()));

    // --- 新規機械ブロック ---
    public static final RegistryObject<Item> TACHYON_PARTICLE_COMPRESSOR = ITEMS.register("tachyon_particle_compressor",
            () -> new BlockItem(BlockInit.TACHYON_PARTICLE_COMPRESSOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> WAVELENGTH_ADJUSTER = ITEMS.register("wavelength_adjuster",
            () -> new BlockItem(BlockInit.WAVELENGTH_ADJUSTER.get(), new Item.Properties()));

    public static final RegistryObject<Item> WAVE_CHAMBER = ITEMS.register("wave_chamber",
            () -> new BlockItem(BlockInit.WAVE_CHAMBER.get(), new Item.Properties()));

    // --- 【追加】Supply Drop関連 ---

    // 座標データパッド: イベントアイテムなのでスタック不可
    public static final RegistryObject<Item> COORDINATE_DATAPAD = ITEMS.register("coordinate_datapad",
            () -> new CoordinateDatapadItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // 補給コンテナ (アイテム版)
    public static final RegistryObject<Item> SUPPLY_CRATE = ITEMS.register("supply_crate",
            () -> new BlockItem(BlockInit.SUPPLY_CRATE.get(), new Item.Properties().rarity(Rarity.RARE)));


    // --- 素材・その他アイテム ---
    public static final RegistryObject<Item> RAW_COSMO_ORE = ITEMS.register("raw_cosmo_ore",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> COSMO_BLEND = ITEMS.register("cosmo_blend",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> COSMO_STEEL = ITEMS.register("cosmo_steel",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> TACHYON_CRYSTAL = ITEMS.register("tachyon_crystal",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> GRAVITY_LENS = ITEMS.register("gravity_lens",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> QUANTUM_UNIT = ITEMS.register("quantum_unit",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> WAVE_CORE = ITEMS.register("wave_core",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    // --- クリアランスキー ---
    public static final RegistryObject<Item> CLEARANCE_C = ITEMS.register("clearance_c",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CLEARANCE_B = ITEMS.register("clearance_b",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> CLEARANCE_A = ITEMS.register("clearance_a",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> CLEARANCE_S = ITEMS.register("clearance_s",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    // 衝撃砲 台座（アイテム）
    public static final RegistryObject<Item> SHOCK_CANNON = ITEMS.register("shock_cannon",
            () -> new BlockItem(BlockInit.SHOCK_CANNON.get(), new Item.Properties()));

    // 衝撃砲 砲身（アイテム）
    public static final RegistryObject<Item> SHOCK_CANNON_BARREL = ITEMS.register("shock_cannon_barrel",
            () -> new BlockItem(BlockInit.SHOCK_CANNON_BARREL.get(), new Item.Properties()));

    // 衝撃砲弾（弾薬アイテム）
    public static final RegistryObject<Item> SHOCK_CANNON_SHELL = ITEMS.register("shock_cannon_shell",
            () -> new Item(new Item.Properties().stacksTo(16)));

    // 三式弾
    public static final RegistryObject<Item> TYPE_3_SHELL = ITEMS.register("type_3_shell",
            () -> new Item(new Item.Properties().stacksTo(16)));

    // 波動カートリッジ弾
    public static final RegistryObject<Item> WAVE_CARTRIDGE = ITEMS.register("wave_cartridge",
            () -> new Item(new Item.Properties().stacksTo(1)));

    // 波動砲 整備手帳(マニュアル)
    public static final RegistryObject<Item> WAVE_MANUAL = ITEMS.register("wave_manual",
            () -> new ManualItem(new Item.Properties().stacksTo(1),
                    ManualItem.BookType.WAVE));

    // ショックカノン 整備手帳(マニュアル)
    public static final RegistryObject<Item> SHOCK_MANUAL = ITEMS.register("shock_manual",
            () -> new ManualItem(new Item.Properties().stacksTo(1),
                    ManualItem.BookType.SHOCK));
}