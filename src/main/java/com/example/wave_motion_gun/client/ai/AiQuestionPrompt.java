package com.example.wave_motion_gun.client.ai;

/**
 * クイズ生成プロンプトの組み立て(プロバイダ共通)。
 * ランク別テーマとクライアント言語(日/英)の切替を担当する。
 */
public class AiQuestionPrompt {
    public static final int QUESTION_COUNT = 5;
    public static final int OPTION_COUNT = 3;

    private static final String[] RANK_NAMES = {"D", "C", "B", "A", "S"};

    public static String build(int rankValue, String languageCode) {
        boolean japanese = languageCode != null && languageCode.startsWith("ja");
        String rank = RANK_NAMES[Math.max(0, Math.min(4, rankValue))];

        String theme = switch (rankValue) {
            case 1 -> japanese ? "波動砲の運用手順、および基礎的な物理(てこ・ばね・電気・光と音)"
                               : "wave motion gun operating procedures, and basic physics (levers, springs, electricity, light and sound)";
            case 2 -> japanese ? "太陽系・月・ロケット工学・宇宙環境などの宇宙科学"
                               : "space science: the solar system, the Moon, rocketry, and the space environment";
            case 3 -> japanese ? "恒星の一生・ブラックホール・宇宙論・光の物理・極限状態の物質"
                               : "stellar evolution, black holes, cosmology, the physics of light, and matter in extreme states";
            case 4 -> japanese ? "相対性理論・量子論・SF的な宇宙技術(ワープ、ダイソン球など)"
                               : "relativity, quantum theory, and sci-fi space technology (warp drives, Dyson spheres, etc.)";
            default -> japanese ? "巨大兵器を扱う者としての心構えと、ごく基礎的な科学"
                                : "the mindset required of someone wielding a superweapon, plus very basic science";
        };

        String body;
        if (japanese) {
            body = """
                    あなたは超文明を持った宇宙の彼方の星からの使者「レディ」です。地球のプレイヤーに「波動砲」を使う資格があるか試験します。
                    クリアランス階級%s向けのクイズを%d問、日本語で作成してください。

                    条件:
                    - テーマ: %s
                    - 対象: 中学生が楽しめる難易度
                    - 各問題は選択肢%d個、正解は1つ
                    - 問題文は2行以内(改行は\\nで最大1回)、選択肢は短く
                    - 問題文の先頭に「<%s> 」を付ける
                    - 科学的に正確であること。時々ユーモアの混じった誤答選択肢を入れてよい
                    """.formatted(rank, QUESTION_COUNT, theme, OPTION_COUNT, rank);
        } else {
            // 日本語以外はすべてこの英語プロンプトで対応し、出力言語だけをクライアント言語に指定する。
            // これによりlangファイルの有無に関係なく、任意のクライアント言語でAI問題を生成できる
            String outputLanguage = displayLanguageName(languageCode);
            body = """
                    You are "the Lady", an envoy from a distant star ruled by a hyper-advanced civilization, testing whether a player from Earth is qualified to use the Wave Motion Gun.
                    Write a quiz of %d questions for clearance rank %s.

                    Rules:
                    - Write ALL question text and answer options in %s
                    - Theme: %s
                    - Audience: middle school students
                    - Each question has %d options with exactly one correct answer
                    - Question text fits in 2 lines (at most one \\n), options are short
                    - Prefix each question with "<%s> "
                    - Be scientifically accurate; the occasional humorous wrong option is welcome
                    """.formatted(QUESTION_COUNT, rank, outputLanguage, theme, OPTION_COUNT, rank);
        }

        // 構造化出力非対応の互換APIでも動くよう、出力形式は常にプロンプトでも明示する
        return body + """

                Output format: respond with ONLY a JSON array (no prose, no markdown fences), where each element is
                {"question": string, "options": [string, ...], "correctIndex": integer (0-based)}.
                """;
    }

    /** Minecraftのロケールコードから、AIへの指示に使う言語名(英語表記)を得る */
    private static String displayLanguageName(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) return "English";
        // 中国語は簡体/繁体を地域コードで区別する
        String lower = languageCode.toLowerCase();
        if (lower.startsWith("zh")) {
            return (lower.equals("zh_tw") || lower.equals("zh_hk")) ? "Traditional Chinese" : "Simplified Chinese";
        }
        String prefix = lower.contains("_") ? lower.substring(0, lower.indexOf('_')) : lower;
        return switch (prefix) {
            case "en" -> "English";
            case "de" -> "German";
            case "fr" -> "French";
            case "es" -> "Spanish";
            case "pt" -> "Portuguese";
            case "it" -> "Italian";
            case "ru" -> "Russian";
            case "ko" -> "Korean";
            case "nl" -> "Dutch";
            case "pl" -> "Polish";
            case "sv" -> "Swedish";
            case "tr" -> "Turkish";
            case "uk" -> "Ukrainian";
            case "cs" -> "Czech";
            case "fi" -> "Finnish";
            case "da" -> "Danish";
            case "no", "nb", "nn" -> "Norwegian";
            case "hu" -> "Hungarian";
            case "el" -> "Greek";
            case "ar" -> "Arabic";
            case "he" -> "Hebrew";
            case "hi" -> "Hindi";
            case "th" -> "Thai";
            case "vi" -> "Vietnamese";
            case "id" -> "Indonesian";
            case "ms" -> "Malay";
            case "ro" -> "Romanian";
            case "bg" -> "Bulgarian";
            // 未知のコードはロケールコードごとAIに伝える(主要モデルはコードを理解できる)
            default -> "the language of the Minecraft locale \"" + languageCode + "\"";
        };
    }
}
