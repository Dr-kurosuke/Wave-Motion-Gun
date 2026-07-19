# Wave Motion Gun Mod (Minecraft 1.18.2)

宇宙戦艦をテーマにした、波動砲・ショックカノンMod (Minecraft Forge 1.18.2)。
マルチブロックの主砲を建造し、エネルギーを充填して発射します。

> ⚠️ **免責事項 / Disclaimer**
> 本Modは非営利のファン制作物です。「宇宙戦艦ヤマト」シリーズおよびその権利者とは一切関係がなく、公認・提携されたものではありません。公式のロゴ・画像・音楽は含まれていません。
> This is an unofficial, non-commercial fan-made mod, not affiliated with or endorsed by the "Space Battleship Yamato" franchise or its rights holders. No official logos, artwork, or music are included.

> ℹ️ **バージョンについて**
> これは **1.18.2** ブランチです。**そのまま問題なく遊べます**。船上での主砲運用については、1.20.1版で Valkyrien Skies の座標対応を強化していますが、1.18.2でも実機テストで不具合は確認されていません(念のための補足です)。

## 特徴

- **波動砲**: 砲門→砲身→変調/増幅→波動エンジン→制御盤 を組み上げるマルチブロック主砲
- **ショックカノン(陽電子衝撃砲)**: レッドストーン信号でも発射できる速射砲
- **クイズ補給システム**: 通信ビーコンから補給を要請(AIクイズ生成に対応・任意)
- **整備手帳(ゲーム内マニュアル)**
- **多言語対応**

## 対応バージョン

| Minecraft | ローダー | ブランチ |
|---|---|---|
| 1.20.1 | Forge 47.x | **`main`**(開発本線) / `1.20.1`(ミラー) |
| 1.18.2 | Forge 40.x | `1.18.2`(このブランチ) |

1.20.1版の開発本線は `main` です(`1.20.1` ブランチは同内容のミラー)。主開発は1.20.1版で行っています。

## 必須の併用Mod

- **FE(Forge Energy)を供給できる工業Mod** … **必須です。**
  本Mod自体は発電手段を持たず、波動エネルギー貯蔵ユニットが外部からFEを受け取る作りになっています。FE を供給できるModが無いと主砲を充填できず、**発射まで到達できません。**
  [Mekanism](https://modrinth.com/mod/mekanism) / [Thermal Expansion](https://modrinth.com/mod/thermal-expansion) / [Immersive Engineering](https://modrinth.com/mod/immersive-engineering) など、FEを出力できるModであれば何でも構いません(特定のModに依存していません)。

## 推奨併用Mod(任意)

- **Valkyrien Skies 2** + **Eureka!** … 船に主砲を載せる遊び。未導入でも単体で動作します

## AIクイズ機能について

補給クイズをAIで生成する機能を搭載しています(任意)。有効にすると**外部API(Google Gemini / OpenAI互換)へ通信**し、利用には**ユーザー自身のAPIキー**が必要です。キーはクライアント設定にのみ保存されます。使わない場合は通信せず、標準問題が使用されます。

## ライセンス

ソースコードは [MIT License](LICENSE)。ファン作品としての免責事項もLICENSEファイルを参照してください。
