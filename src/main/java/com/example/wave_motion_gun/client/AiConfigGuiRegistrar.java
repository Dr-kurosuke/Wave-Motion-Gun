package com.example.wave_motion_gun.client;

import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * Mod一覧の「設定」ボタンからAI設定画面を開くための登録処理。
 *
 * 【重要】ConfigGuiFactory/Screen等のクライアント専用クラスを参照するため、
 * このクラスは絶対にExampleMod等の共通クラスから直接参照してはならない。
 * ラムダとして共通クラスに書くと合成メソッドの検証時に専用サーバーがクラッシュする
 * (DistExecutorの実行ガードではクラスロードを防げない)。
 * ExampleModからは DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> AiConfigGuiRegistrar::register)
 * の形でのみ呼び出すこと。
 */
public class AiConfigGuiRegistrar {
    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory((mc, parent) -> new AiConfigScreen(parent)));
    }
}
