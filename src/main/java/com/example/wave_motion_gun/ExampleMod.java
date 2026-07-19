package com.example.wave_motion_gun;

import com.example.wave_motion_gun.config.AiClientConfig;
import com.example.wave_motion_gun.config.AiServerConfig;
import com.example.wave_motion_gun.init.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("wave_motion_gun_mod")
public class ExampleMod {
    public static final String MOD_ID = "wave_motion_gun_mod";
    public static final String MODID = MOD_ID;

    public ExampleMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        // 各種登録
        BlockInit.BLOCKS.register(bus);
        BlockEntityInit.BLOCK_ENTITIES.register(bus);
        ItemInit.ITEMS.register(bus);
        MenuInit.MENUS.register(bus);
        SoundInit.SOUNDS.register(bus);
        EntityInit.ENTITIES.register(bus);
        CreativeTabInit.TABS.register(bus);

        // AIモード設定
        // CLIENT: APIキーを含む。プレイヤーのPCにのみ保存され、サーバーへ同期されない
        // SERVER: 許可トグルのみ(秘密情報は置かない)
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, AiClientConfig.SPEC, MOD_ID + "-ai-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, AiServerConfig.SPEC, MOD_ID + "-ai-server.toml");

        // Mod一覧の「設定」ボタンからAI設定画面を開けるようにする(クライアントのみ)。
        // 【重要】ここにラムダで直接書くとExampleModの合成メソッドがScreenを参照してしまい、
        // 専用サーバーがクラスロードでクラッシュする。必ずメソッド参照でクライアント専用
        // クラス(AiConfigGuiRegistrar)へ委譲すること。
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> com.example.wave_motion_gun.client.AiConfigGuiRegistrar::register);

        // パケットの登録
        com.example.wave_motion_gun.network.PacketHandler.register();

        // クライアントセットアップ (clientSetup) は削除しました。
        // 画面登録などは com.example.wave_motion_gun.client.ClientModEvents で行われます。
    }
}