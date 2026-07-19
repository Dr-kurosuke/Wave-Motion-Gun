package com.example.wave_motion_gun.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.IKeyConflictContext;

/**
 * 画面(Screen)を開いたままでも船の操舵を継続できるようにする。
 *
 * <p>VS2は {@code ShipMountingEntity.tick()} から毎tick {@code PacketPlayerDriving} を送るが、
 * その入力値は全て {@code KeyMapping.isDown()} 経由で読まれる。一方 {@code Minecraft.setScreen()} は
 * {@code KeyMapping.releaseAll()} を呼び、以降 {@code KeyboardHandler} は
 * {@code screen == null || screen.passEvents} の時しか {@code KeyMapping.set()} を呼ばない。
 * 結果、画面を開くと入力が全てfalseになり、船は「入力なし」ではなく
 * <b>明示的な停止命令</b>を毎tick受け取って止まる。
 *
 * <p>対策は2段階必要になる。
 * <ol>
 *   <li>{@link #poll(boolean)} で生のGLFW状態を読み {@code KeyMapping} の押下フラグを立てる</li>
 *   <li>Forgeは {@code isDown()} を
 *       {@code return this.isDown && isConflictContextAndModifierActive();} にパッチしている。
 *       バニラの移動キーは競合コンテキストが {@code IN_GAME} (= {@code screen == null}) なので、
 *       押下フラグを立てても画面表示中は {@code isDown()} がfalseに潰される。
 *       そこで操舵中だけ有効になるコンテキストで包む</li>
 * </ol>
 * VS2の下降(V)・巡航(C)は {@code new KeyMapping(...)} 既定の {@code UNIVERSAL} コンテキストのため
 * 2.は不要で、1.だけで動作する(実機でVのみ効いたのはこのため)。
 *
 * <p>{@code Screen.passEvents = true} でも解決するが、それだと
 * {@code Minecraft.tick()} の {@code handleKeybinds()} も走るようになり、
 * ホットバー切替・アイテムdrop・インベントリ開閉などが画面を開いたまま発火してしまう
 * (周波数欄に数字を打つとホットバーが動く)。
 */
@OnlyIn(Dist.CLIENT)
public final class ShipControlPassthrough {

    /**
     * VS2の下降・巡航キーが属するカテゴリ。
     * VS2のクラスを直接参照するとVS2未導入環境で解決エラーになるため、
     * 文字列一致で拾うことで依存を持たずに対応する。
     */
    private static final String VS2_DRIVING_CATEGORY = "category.valkyrienskies.driving";

    /** 操舵キーを通している間だけtrue。競合コンテキストの判定に使う。 */
    private static boolean steering = false;

    private static boolean installed = false;

    private ShipControlPassthrough() {}

    /**
     * 画面表示中に通すのは<b>A/Dの旋回だけ</b>。
     * 前進・後退・上昇・下降・巡航は、砲の照準中に機体が動いてしまわないよう明示的に解除する。
     *
     * <p>左右を個別に受けるのは、旋回可能角の上限に達した側だけを止めるため。
     * 両方落とすと制限角に張り付いた時に戻れなくなる。
     */
    public static void poll(boolean allowLeft, boolean allowRight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null || mc.options == null) return;

        install(mc.options);
        steering = allowLeft || allowRight;

        long window = mc.getWindow().getWindow();
        Options options = mc.options;

        // 旋回のみ通す
        apply(options.keyLeft, window, allowLeft);
        apply(options.keyRight, window, allowRight);

        // 前進・後退・上昇は常に解除
        apply(options.keyUp, window, false);
        apply(options.keyDown, window, false);
        apply(options.keyJump, window, false);

        // VS2の下降(V)・巡航(C)も常に解除。
        // これらは UNIVERSAL コンテキストのため、放置すると画面表示中でも生きてしまう
        for (KeyMapping km : options.keyMappings) {
            if (VS2_DRIVING_CATEGORY.equals(km.getCategory())) {
                apply(km, window, false);
            }
        }
    }

    /** 画面を閉じた時に呼ぶ。操舵状態を解除する(押下フラグ自体は残さない)。 */
    public static void release() {
        poll(false, false);
        steering = false;
    }

    private static void apply(KeyMapping mapping, long window, boolean enabled) {
        if (mapping == null) return;
        InputConstants.Key key = mapping.getKey();
        // マウスボタン等はGLFWのキー問い合わせ対象外。未割り当て(UNKNOWN)も除外する
        boolean pollable = key.getType() == InputConstants.Type.KEYSYM
                && key.getValue() != InputConstants.UNKNOWN.getValue();
        // KeyMapping.set() ではなく setDown() を直接使う。
        // set() は KeyBindingMap 経由で同一キーに紐づく全バインドを走査するため、
        // 意図しないキーバインドまで巻き込む可能性がある。
        mapping.setDown(enabled && pollable && InputConstants.isKeyDown(window, key.getValue()));
    }

    /**
     * 旋回キーの競合コンテキストを、操舵中も有効になるものへ差し替える(1回だけ)。
     * 通す必要があるのはA/Dだけなので、他のキーには手を触れない。
     */
    private static void install(Options options) {
        if (installed) return;
        installed = true;
        for (KeyMapping km : new KeyMapping[]{options.keyLeft, options.keyRight}) {
            if (km != null && !(km.getKeyConflictContext() instanceof SteeringContext)) {
                km.setKeyConflictContext(new SteeringContext(km.getKeyConflictContext()));
            }
        }
    }

    /**
     * 元のコンテキスト(通常は {@code IN_GAME})に「操舵中」を上乗せするラッパー。
     * 元の判定を維持するため、通常プレイ時の挙動は変わらない。
     */
    private record SteeringContext(IKeyConflictContext delegate) implements IKeyConflictContext {
        @Override
        public boolean isActive() {
            return delegate.isActive() || steering;
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            // 競合判定は元のコンテキストの意味論をそのまま使う
            return delegate.conflicts(other instanceof SteeringContext s ? s.delegate() : other);
        }
    }
}
