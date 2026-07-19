package com.example.wave_motion_gun.client;

import com.example.wave_motion_gun.init.ItemInit;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

/**
 * 整備手帳(マニュアル)の表示画面。ページめくり式で、全テキストはlangキー駆動
 * (バニラの記入済みの本と違いNBT生文字列を持たないので多言語対応が効く)。
 *
 * 波動砲用・ショックカノン用など複数の「本」をBook定義で切り替えられる。
 * 章の本文がページに収まらない場合、あふれた行は「見出しなしの続きページ」へ
 * 自動で流し込む(翻訳言語の文章量差をレイアウト側で吸収するため)。
 * ページ構成はinit時に組み立てる(リサイズ・言語切替で再構築される)。
 */
public class ManualScreen extends Screen {

    // 横長パネル。小さい画面では画面幅に合わせて縮む(panelW()参照)
    private static final int MAX_PANEL_W = 340;
    private static final int PANEL_H = 234;
    private static final int MARGIN = 16;
    private static final int LINE_H = 10;

    // アイコン描画サイズ(16pxの2倍)
    private static final float ICON_SCALE = 2.0f;
    private static final int ICON_SIZE = (int) (16 * ICON_SCALE);
    private static final int ICON_GAP = 6;

    // 続きページ(見出しなし)の本文開始位置
    private static final int CONT_BODY_TOP = 14;

    // 既存GUI(スチール筐体+ホロ表示)に合わせた近代的な配色
    private static final int COL_BORDER = 0xFF1a1d20;   // 外枠(ダークスチール)
    private static final int COL_FRAME = 0xFF4a5055;    // 筐体エッジ
    private static final int COL_PAPER = 0xF00d1418;    // ディスプレイ面(ダークネイビー)
    private static final int COL_TITLE = 0xFF00e5ff;    // タイトル(ホロシアン)
    private static final int COL_RULE = 0xFF008fa3;     // 区切り線(シアン)
    private static final int COL_ACCENT = 0x4000e5ff;   // コーナー装飾(半透明シアン)
    private static final int COL_TEXT = 0xFFd8e0e8;     // 本文(ライトグレー)
    private static final int COL_PAGENUM = 0xFF5a8a95;  // ページ番号

    /** 章定義。key="p1"等。schematic=trueの章は先頭ページの本文前に模式図を描く */
    private record Chapter(String key, boolean schematic, ItemStack[] icons) {}

    /** 本の定義。keyBase配下に .pN.title/.pN.body のlangキーを持つ */
    private record Book(String keyBase, List<Chapter> chapters,
                        ItemLike[] schematicChain, String[] schematicLabelKeys) {}

    /** 実際に表示する1ページ分。first=falseは見出しなしの続きページ */
    private record RenderPage(Chapter chapter, List<FormattedCharSequence> lines, boolean first) {}

    private final Book book;
    private final List<RenderPage> renderPages = new ArrayList<>();
    private int pageIndex = 0;
    private Button prevButton;
    private Button nextButton;
    // 現在ページで解説しているアイテムのアイコン(ページ切替時に更新)
    private ItemStack[] currentIcons = new ItemStack[0];

    private ManualScreen(String titleKey, Book book) {
        super(new TranslatableComponent(titleKey));
        this.book = book;
    }

    /** 波動砲 整備手帳 */
    public static ManualScreen wave() {
        String base = "manual.wave_motion_gun_mod";
        List<Chapter> ch = List.of(
                new Chapter("p1", false, stacks(ItemInit.WAVE_CORE.get())),                 // 波動砲とは
                new Chapter("p2", false, stacks(ItemInit.COMMUNICATION_BEACON.get())),     // 補給と認証
                new Chapter("p3", true, new ItemStack[0]),                                 // 全体構成(模式図)
                new Chapter("p4", false, stacks(ItemInit.WAVE_CANNON.get())),              // 砲門
                new Chapter("p5", false, stacks(ItemInit.BARREL_UNIT.get())),              // 砲身
                new Chapter("p6", false, stacks(ItemInit.WAVELENGTH_ADJUSTER.get(), ItemInit.WAVE_CHAMBER.get())), // 変調機
                new Chapter("p7", false, stacks(ItemInit.TACHYON_PARTICLE_COMPRESSOR.get())), // 増幅機
                new Chapter("p8", false, stacks(ItemInit.WAVE_ENERGY_STORAGE.get())),      // 波動エンジン
                new Chapter("p9", false, stacks(ItemInit.MONITORING_UNIT.get())),          // 制御盤
                new Chapter("p10", false, stacks(ItemInit.TRIGGER_UNIT.get(), ItemInit.SEAT_BLOCK.get())), // トリガー(一)
                new Chapter("p11", false, stacks(ItemInit.TRIGGER_UNIT.get())),            // トリガー(二)
                new Chapter("p12", false, new ItemStack[0]));                              // 発射手順
        ItemLike[] chain = {
                ItemInit.WAVE_CANNON.get(), ItemInit.BARREL_UNIT.get(),
                ItemInit.TACHYON_PARTICLE_COMPRESSOR.get(), ItemInit.WAVE_ENERGY_STORAGE.get(),
                ItemInit.MONITORING_UNIT.get() };
        String[] labels = {
                base + ".schematic.muzzle", base + ".schematic.barrel", base + ".schematic.mod",
                base + ".schematic.engine", base + ".schematic.control" };
        return new ManualScreen(base + ".title", new Book(base, ch, chain, labels));
    }

    /** ショックカノン 整備手帳 */
    public static ManualScreen shock() {
        String base = "manual.wave_motion_gun_mod.shock";
        List<Chapter> ch = List.of(
                new Chapter("p1", false, stacks(ItemInit.SHOCK_CANNON.get())),             // ショックカノンとは
                new Chapter("p2", true, new ItemStack[0]),                                 // 全体構成(模式図)
                new Chapter("p3", false, stacks(ItemInit.SHOCK_CANNON.get())),             // 砲塔
                new Chapter("p4", false, stacks(ItemInit.SHOCK_CANNON_BARREL.get())),      // 砲身
                new Chapter("p5", false, stacks(ItemInit.SHOCK_CANNON_SHELL.get(),
                        ItemInit.TYPE_3_SHELL.get(), ItemInit.WAVE_CARTRIDGE.get())),      // 弾薬
                new Chapter("p6", false, stacks(ItemInit.SHOCK_CANNON.get())),             // 操作盤
                new Chapter("p7", false, new ItemStack[0]));                               // 射撃運用
        ItemLike[] chain = { ItemInit.SHOCK_CANNON.get(), ItemInit.SHOCK_CANNON_BARREL.get() };
        String[] labels = { base + ".schematic.turret", base + ".schematic.barrel" };
        return new ManualScreen(base + ".title", new Book(base, ch, chain, labels));
    }

    private static ItemStack[] stacks(ItemLike... items) {
        ItemStack[] result = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) result[i] = new ItemStack(items[i]);
        return result;
    }

    private int panelW() { return Math.min(MAX_PANEL_W, this.width - 16); }
    private int left() { return (this.width - panelW()) / 2; }
    private int top() { return (this.height - PANEL_H) / 2; }

    /** 本文が使える下端(パネル上端基準。ボタン帯の直上) */
    private static int contentBottom() { return PANEL_H - 26 - 4; }

    /** 章の先頭ページの本文開始位置(パネル上端基準) */
    private int firstBodyTop(Chapter ch) {
        int y = 34;
        if (ch.schematic()) y += 52;
        if (ch.icons().length > 0) y += ICON_SIZE + 4;
        return y;
    }

    @Override
    protected void init() {
        super.init();
        buildPages();
        int cx = this.width / 2;
        int by = top() + PANEL_H - 26;
        this.prevButton = this.addRenderableWidget(new Button(cx - 100, by, 40, 20,
                new TextComponent("<"), b -> turn(-1)));
        this.nextButton = this.addRenderableWidget(new Button(cx + 60, by, 40, 20,
                new TextComponent(">"), b -> turn(1)));
        this.addRenderableWidget(new Button(cx - 40, by, 80, 20,
                new TranslatableComponent("gui.done"), b -> this.onClose()));
        updateButtons();
    }

    /** 全章の本文を折り返し、収まらない分を続きページへ分割する */
    private void buildPages() {
        renderPages.clear();
        int wrapW = panelW() - MARGIN * 2;
        for (Chapter ch : book.chapters()) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            String body = I18n.get(book.keyBase() + "." + ch.key() + ".body");
            for (String paragraph : body.split("\n")) {
                lines.addAll(this.font.split(FormattedText.of(paragraph), wrapW));
            }
            int firstCap = Math.max(1, (contentBottom() - firstBodyTop(ch)) / LINE_H);
            int contCap = Math.max(1, (contentBottom() - CONT_BODY_TOP) / LINE_H);

            int idx = Math.min(firstCap, lines.size());
            renderPages.add(new RenderPage(ch, List.copyOf(lines.subList(0, idx)), true));
            while (idx < lines.size()) {
                int end = Math.min(lines.size(), idx + contCap);
                renderPages.add(new RenderPage(ch, List.copyOf(lines.subList(idx, end)), false));
                idx = end;
            }
        }
        pageIndex = Math.min(pageIndex, renderPages.size() - 1);
    }

    private void turn(int delta) {
        pageIndex = Math.max(0, Math.min(renderPages.size() - 1, pageIndex + delta));
        updateButtons();
    }

    private void updateButtons() {
        prevButton.active = pageIndex > 0;
        nextButton.active = pageIndex < renderPages.size() - 1;
        RenderPage rp = renderPages.get(pageIndex);
        this.currentIcons = rp.first() ? rp.chapter().icons() : new ItemStack[0];
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partial) {
        this.renderBackground(pose);
        int left = left();
        int top = top();

        // 筐体とディスプレイ面
        int pw = panelW();
        fill(pose, left - 2, top - 2, left + pw + 2, top + PANEL_H + 2, COL_BORDER);
        fill(pose, left, top, left + pw, top + PANEL_H, COL_FRAME);
        fill(pose, left + 3, top + 3, left + pw - 3, top + PANEL_H - 3, COL_PAPER);
        // コーナーのアクセント(ホロディスプレイ風)
        fill(pose, left + 3, top + 3, left + 27, top + 5, COL_ACCENT);
        fill(pose, left + 3, top + 3, left + 5, top + 27, COL_ACCENT);
        fill(pose, left + pw - 27, top + PANEL_H - 5, left + pw - 3, top + PANEL_H - 3, COL_ACCENT);
        fill(pose, left + pw - 5, top + PANEL_H - 27, left + pw - 3, top + PANEL_H - 3, COL_ACCENT);

        RenderPage rp = renderPages.get(pageIndex);
        Chapter chapter = rp.chapter();
        int contentTop;

        if (rp.first()) {
            // タイトルと区切り線
            String title = I18n.get(book.keyBase() + "." + chapter.key() + ".title");
            drawCenteredString(pose, this.font, title, this.width / 2, top + 12, COL_TITLE);
            fill(pose, left + MARGIN, top + 26, left + pw - MARGIN, top + 27, COL_RULE);

            contentTop = top + 34;
            if (chapter.schematic()) {
                drawSchematic(pose, left, contentTop);
                contentTop += 52;
            }

            // 解説対象アイテムのアイコン行(タイトル直下・中央寄せ・2倍サイズで描画)
            if (currentIcons.length > 0) {
                int iconRowW = currentIcons.length * (ICON_SIZE + ICON_GAP) - ICON_GAP;
                int ix = this.width / 2 - iconRowW / 2;
                // renderGuiItemはPoseStackを取らないため、モデルビュー行列側でスケールする
                var mv = RenderSystem.getModelViewStack();
                mv.pushPose();
                mv.scale(ICON_SCALE, ICON_SCALE, 1.0f);
                RenderSystem.applyModelViewMatrix();
                for (ItemStack stack : currentIcons) {
                    this.minecraft.getItemRenderer().renderGuiItem(stack,
                            (int) (ix / ICON_SCALE), (int) (contentTop / ICON_SCALE));
                    ix += ICON_SIZE + ICON_GAP;
                }
                mv.popPose();
                RenderSystem.applyModelViewMatrix();
                contentTop += ICON_SIZE + 4;
            }
        } else {
            // 続きページ: 見出しなしで本文のみ
            contentTop = top + CONT_BODY_TOP;
        }

        // 本文(buildPagesで折り返し・分割済み)
        int y = contentTop;
        for (FormattedCharSequence line : rp.lines()) {
            this.font.draw(pose, line, left + MARGIN, y, COL_TEXT);
            y += LINE_H;
        }

        // ページ番号(右上)
        String pn = (pageIndex + 1) + "/" + renderPages.size();
        this.font.draw(pose, pn, left + pw - MARGIN - this.font.width(pn), top + 12, COL_PAGENUM);

        super.render(pose, mouseX, mouseY, partial);
    }

    /** 本ごとの構成チェーンをブロックアイコン+矢印で描く模式図 */
    private void drawSchematic(PoseStack pose, int left, int y) {
        ItemLike[] chain = book.schematicChain();
        String[] labelKeys = book.schematicLabelKeys();
        int n = chain.length;
        int slot = 44;
        int rowLeft = left + (panelW() - slot * n) / 2;

        for (int i = 0; i < n; i++) {
            int ix = rowLeft + slot * i + (slot - 16) / 2;
            this.minecraft.getItemRenderer().renderGuiItem(new ItemStack(chain[i]), ix, y);

            // アイコン下のラベル(0.5倍で中央寄せ)
            String label = I18n.get(labelKeys[i]);
            pose.pushPose();
            pose.translate(ix + 8, y + 19, 0);
            pose.scale(0.5f, 0.5f, 1.0f);
            this.font.draw(pose, label, -this.font.width(label) / 2.0f, 0, COL_TEXT);
            pose.popPose();

            // 矢印(最後以外)
            if (i < n - 1) {
                this.font.draw(pose, "→", ix + 20, y + 4, COL_TEXT);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
