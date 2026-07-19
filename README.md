# Wave Motion Gun Mod

宇宙戦艦をテーマにした、波動砲・ショックカノンMod (Minecraft Forge)。
マルチブロックの主砲を建造し、エネルギーを充填して発射します。**Valkyrien Skies / Eureka!** の船に搭載して、動く砲艦として運用できます。

> ⚠️ **免責事項 / Disclaimer**
> 本Modは非営利のファン制作物です。「宇宙戦艦ヤマト」シリーズおよびその権利者とは一切関係がなく、公認・提携されたものではありません。公式のロゴ・画像・音楽は含まれていません。
> This is an unofficial, non-commercial fan-made mod, not affiliated with or endorsed by the "Space Battleship Yamato" franchise or its rights holders. No official logos, artwork, or music are included.

## 特徴

- **波動砲**: 砲門→砲身→変調/増幅→波動エンジン→制御盤 を組み上げるマルチブロック主砲。射程・半径・威力を調整して発射
- **ショックカノン(陽電子衝撃砲)**: レッドストーン信号でも発射できる速射砲。エネルギー弾・三式弾・波動カートリッジ弾に対応
- **クイズ補給システム**: 通信ビーコンから補給を要請。標準問題に加え、**AIクイズ生成**にも対応(任意)
- **整備手帳(ゲーム内マニュアル)**: 波動砲・ショックカノンの構造と操作を解説する専用アイテム
- **Valkyrien Skies対応**: 船上に主砲を載せても周波数設定・発射方向が正しく動作
- **多言語対応**: 12言語

## 対応バージョン

| Minecraft | ローダー | ブランチ |
|---|---|---|
| 1.20.1 | Forge 47.x | `main` / `1.20.1` |
| 1.18.2 | Forge 40.x | `1.18.2` |

## 推奨併用Mod(任意)

いずれも**必須ではありません**が、導入すると本来の遊び方ができます。

- **[Valkyrien Skies 2](https://modrinth.com/mod/valkyrien-skies)** + **[Eureka!](https://modrinth.com/mod/eureka)** … 船に主砲を搭載する遊びの前提
- **[Mekanism](https://modrinth.com/mod/mekanism)** など FE(Forge Energy)を供給できる工業Mod … 主砲のエネルギー源

## AIクイズ機能について

補給クイズをAIで生成する機能を搭載しています(任意)。この機能を有効にすると、**外部API(Google Gemini / OpenAI互換)へ通信**します。利用には**ユーザー自身のAPIキー**が必要で、キーはクライアント設定にのみ保存されます。機能を使わない場合は通信は行わず、標準問題が使用されます。

## ライセンス

ソースコードは [MIT License](LICENSE)。ファン作品としての免責事項もLICENSEファイルを参照してください。
