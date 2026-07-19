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
 * {@code screen == null} の時しか {@code KeyMapping.set()} を呼ばない。
 * 結果、画面を開くと入力が全てfalseになり、船は「入力なし」ではなく
 * <b>明示的な停止命令</b>を毎tick受け取って止まる。
 *
 * <p>対策は2段階必要になる。
 * <ol>
 *   <li>{@link #poll(boolean, boolean)} で生のGLFW状態を読み {@code KeyMapping} の押下フラグを立てる</li>
 *   <li>Forgeは {@code isDown()} を
 *       {@code return this.isDown && isConflictContextAndModifierActive();} にパッチしている。
 *       バニラの移動キーは競合コンテキストが {@code IN_GAME} (= {@code screen == null}) なので、
 *       押下フラグを立てても画面表示中は {@code isDown()} がfalseに潰される。
 *       そこで操舵中だけ有効になるコンテキストで包む</li>
 * </ol>
 * <p><b>触る対象はA/Dだけに限ること。</b> 前進・後退・上昇やVS2の下降/巡航まで
 * {@code setDown()} で毎tick潰す実装にしたところ、Eureka船が動かなくなる不具合を起こした。
 * 画面表示中は releaseAll() で既に全キーがfalseなので、そもそも潰す必要が無い。
 *
 * <p>1.18.2版では {@code Screen.passEvents = true} という代替手段があったが、
 * それだと {@code Minecraft.tick()} の {@code handleKeybinds()} も走るようになり、
 * ホットバー切替・アイテムdrop・インベントリ開閉などが画面を開いたまま発火してしまう
 * (周波数欄に数字を打つとホットバーが動く)ため採用しなかった。
 * なお {@code passEvents} は1.20.1では削除されており、そもそも選択肢に無い。
 */
@OnlyIn(Dist.CLIENT)
public final class ShipControlPassthrough {

    /** 操舵キーを通している間だけtrue。競合コンテキストの判定に使う。 */
    private static boolean steering = false;

    private static boolean installed = false;
    /** 差し替える前の競合コンテキスト。解除時に必ず元へ戻すため保持する。 */
    private static IKeyConflictContext originalLeft;
    private static IKeyConflictContext originalRight;

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

        // 触るのは旋回に使うA/Dだけに限定する。
        //
        // 前進・後退・上昇やVS2の下降/巡航には手を出さない。画面表示中は
        // Minecraft.setScreen() の releaseAll() で既に全キーがfalseになっており、
        // KeyboardHandler も screen != null の間は set() を呼ばないため、放っておけば押されない。
        // ここで余計にsetDown()を呼ぶと、他Mod(Eureka等)が管理している操船状態まで
        // 巻き込むおそれがある。実際、以前は毎tick広範囲のキーを潰しており、
        // Eureka船が動かなくなる不具合を起こした。
        apply(options.keyLeft, window, allowLeft);
        apply(options.keyRight, window, allowRight);
    }

    /**
     * 画面を閉じた時に呼ぶ。差し替えた競合コンテキストを元へ戻し、押下フラグも落とす。
     *
     * <p>コンテキストの差し替えはグローバルかつセッション中ずっと残る変更なので、
     * 使い終わったら必ず元に戻すこと。戻さないと通常プレイのキー判定に影響が残る。
     */
    public static void release() {
        steering = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;

        Options options = mc.options;
        if (options.keyLeft != null) options.keyLeft.setDown(false);
        if (options.keyRight != null) options.keyRight.setDown(false);
        uninstall(options);
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
        if (options.keyLeft != null) {
            originalLeft = options.keyLeft.getKeyConflictContext();
            options.keyLeft.setKeyConflictContext(new SteeringContext(originalLeft));
        }
        if (options.keyRight != null) {
            originalRight = options.keyRight.getKeyConflictContext();
            options.keyRight.setKeyConflictContext(new SteeringContext(originalRight));
        }
    }

    /** install() で差し替えた競合コンテキストを元へ戻す。 */
    private static void uninstall(Options options) {
        if (!installed) return;
        installed = false;
        if (options.keyLeft != null && originalLeft != null) {
            options.keyLeft.setKeyConflictContext(originalLeft);
        }
        if (options.keyRight != null && originalRight != null) {
            options.keyRight.setKeyConflictContext(originalRight);
        }
        originalLeft = null;
        originalRight = null;
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
