package com.example.wave_motion_gun.utils;

import com.example.wave_motion_gun.init.ItemInit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public class QuestionManager {
    // 全ての問題チェーンを保持するマスターリスト
    public static final List<QuestionChain> CHAINS = new ArrayList<>();

    // グループID -> そのグループに含まれるChainのインデックスリスト
    public static final Map<Integer, List<Integer>> GROUP_MAP = new HashMap<>();

    private static final Random RANDOM = new Random();

    /**
     * 問題チェーンを登録するメソッド
     * @param groupId 所属させる問題集のID
     * @param chain 登録するクイズセット
     */
    private static void register(int groupId, QuestionChain chain) {
        int index = CHAINS.size(); // 現在の末尾がIDになる
        CHAINS.add(chain);

        // グループマップにIDを追加
        GROUP_MAP.computeIfAbsent(groupId, k -> new ArrayList<>()).add(index);
    }

    static {
        // ... (問題定義部分は変更なし、長いので省略し、既存コードをそのまま使用します) ...
        // 以前のチャットでの指示通り、Level D と Level C の内容が更新されている前提です。
        // ここではコード構造の変更に焦点を当てます。

        // ==========================================
        // Group 1: Level D (Clearance D -> C)
        // テーマ: 構造と理論 (Modチュートリアル・基礎)
        // ==========================================
        List<ItemStack> rewardsG1 = new ArrayList<>();
        rewardsG1.add(new ItemStack(ItemInit.RAW_COSMO_ORE.get(), 1));
        rewardsG1.add(new ItemStack(ItemInit.RAW_COSMO_ORE.get(), 1));
        rewardsG1.add(new ItemStack(ItemInit.RAW_COSMO_ORE.get(), 1));
        rewardsG1.add(new ItemStack(ItemInit.RAW_COSMO_ORE.get(), 1));
        rewardsG1.add(new ItemStack(ItemInit.RAW_COSMO_ORE.get(), 1));
        rewardsG1.add(new ItemStack(ItemInit.CLEARANCE_C.get(), 1));
        rewardsG1.add(new ItemStack(ItemInit.CLEARANCE_C.get(), 1));
        rewardsG1.add(new ItemStack(ItemInit.CLEARANCE_C.get(), 1));
        rewardsG1.add(new ItemStack(ItemInit.CLEARANCE_C.get(), 1));

        // Set 1: 覚悟の証明
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g1.s1.q1", new String[]{"quiz.wave_motion_gun_mod.g1.s1.q1.o1", "quiz.wave_motion_gun_mod.g1.s1.q1.o2", "quiz.wave_motion_gun_mod.g1.s1.q1.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g1.s1.q2", new String[]{"quiz.wave_motion_gun_mod.g1.s1.q2.o1", "quiz.wave_motion_gun_mod.g1.s1.q2.o2"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g1.s1.q3", new String[]{"quiz.wave_motion_gun_mod.g1.s1.q3.o1", "quiz.wave_motion_gun_mod.g1.s1.q3.o2"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g1.s1.q4", new String[]{"quiz.wave_motion_gun_mod.g1.s1.q4.o1", "quiz.wave_motion_gun_mod.g1.s1.q4.o2", "quiz.wave_motion_gun_mod.g1.s1.q4.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g1.s1.q5", new String[]{"quiz.wave_motion_gun_mod.g1.s1.q5.o1"}, 0));
            register(1, new QuestionChain(q, rewardsG1));
        }

        // Set 2: エネルギーとコスト
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g1.s2.q1", new String[]{"quiz.wave_motion_gun_mod.g1.s2.q1.o1", "quiz.wave_motion_gun_mod.g1.s2.q1.o2", "quiz.wave_motion_gun_mod.g1.s2.q1.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g1.s2.q2", new String[]{"quiz.wave_motion_gun_mod.g1.s2.q2.o1", "quiz.wave_motion_gun_mod.g1.s2.q2.o2", "quiz.wave_motion_gun_mod.g1.s2.q2.o3"}, 2));
            q.add(new Question("quiz.wave_motion_gun_mod.g1.s2.q3", new String[]{"quiz.wave_motion_gun_mod.g1.s2.q3.o1", "quiz.wave_motion_gun_mod.g1.s2.q3.o2", "quiz.wave_motion_gun_mod.g1.s2.q3.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g1.s2.q4", new String[]{"quiz.wave_motion_gun_mod.g1.s2.q4.o1", "quiz.wave_motion_gun_mod.g1.s2.q4.o2", "quiz.wave_motion_gun_mod.g1.s2.q4.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g1.s2.q5", new String[]{"quiz.wave_motion_gun_mod.g1.s2.q5.o1", "quiz.wave_motion_gun_mod.g1.s2.q5.o2", "quiz.wave_motion_gun_mod.g1.s2.q5.o3"}, 2));
            register(1, new QuestionChain(q, rewardsG1));
        }

        // Set 3: 安全管理
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g1.s3.q1", new String[]{"quiz.wave_motion_gun_mod.g1.s3.q1.o1", "quiz.wave_motion_gun_mod.g1.s3.q1.o2", "quiz.wave_motion_gun_mod.g1.s3.q1.o3"}, 2));
            q.add(new Question("quiz.wave_motion_gun_mod.g1.s3.q2", new String[]{"quiz.wave_motion_gun_mod.g1.s3.q2.o1", "quiz.wave_motion_gun_mod.g1.s3.q2.o2", "quiz.wave_motion_gun_mod.g1.s3.q2.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g1.s3.q3", new String[]{"quiz.wave_motion_gun_mod.g1.s3.q3.o1", "quiz.wave_motion_gun_mod.g1.s3.q3.o2", "quiz.wave_motion_gun_mod.g1.s3.q3.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g1.s3.q4", new String[]{"quiz.wave_motion_gun_mod.g1.s3.q4.o1", "quiz.wave_motion_gun_mod.g1.s3.q4.o2", "quiz.wave_motion_gun_mod.g1.s3.q4.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g1.s3.q5", new String[]{"quiz.wave_motion_gun_mod.g1.s3.q5.o1", "quiz.wave_motion_gun_mod.g1.s3.q5.o2", "quiz.wave_motion_gun_mod.g1.s3.q5.o3"}, 1));
            register(1, new QuestionChain(q, rewardsG1));
        }




        // ==========================================
        // Group 2: Level C (Clearance C -> B)
        // テーマ: 運用手順と基礎科学 (Modチュートリアル・理科)
        // ==========================================
        List<ItemStack> rewardsG2 = new ArrayList<>();
        rewardsG2.add(new ItemStack(ItemInit.RAW_COSMO_ORE.get(), 2));
        rewardsG2.add(new ItemStack(ItemInit.RAW_COSMO_ORE.get(), 1));
        rewardsG2.add(new ItemStack(ItemInit.TACHYON_CRYSTAL.get(), 1));
        rewardsG2.add(new ItemStack(ItemInit.TACHYON_CRYSTAL.get(), 1));
        rewardsG2.add(new ItemStack(ItemInit.TACHYON_CRYSTAL.get(), 1));
        rewardsG2.add(new ItemStack(ItemInit.CLEARANCE_C.get(), 2));
        rewardsG2.add(new ItemStack(ItemInit.CLEARANCE_B.get(), 1));
        rewardsG2.add(new ItemStack(ItemInit.CLEARANCE_B.get(), 1));
        rewardsG2.add(new ItemStack(ItemInit.CLEARANCE_B.get(), 1));

        // Set 1: マルチブロック構造の基礎
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s1.q1", new String[]{"quiz.wave_motion_gun_mod.g2.s1.q1.o1", "quiz.wave_motion_gun_mod.g2.s1.q1.o2", "quiz.wave_motion_gun_mod.g2.s1.q1.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s1.q2", new String[]{"quiz.wave_motion_gun_mod.g2.s1.q2.o1", "quiz.wave_motion_gun_mod.g2.s1.q2.o2", "quiz.wave_motion_gun_mod.g2.s1.q2.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s1.q3", new String[]{"quiz.wave_motion_gun_mod.g2.s1.q3.o1", "quiz.wave_motion_gun_mod.g2.s1.q3.o2", "quiz.wave_motion_gun_mod.g2.s1.q3.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s1.q4", new String[]{"quiz.wave_motion_gun_mod.g2.s1.q4.o1", "quiz.wave_motion_gun_mod.g2.s1.q4.o2", "quiz.wave_motion_gun_mod.g2.s1.q4.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s1.q5", new String[]{"quiz.wave_motion_gun_mod.g2.s1.q5.o1", "quiz.wave_motion_gun_mod.g2.s1.q5.o2", "quiz.wave_motion_gun_mod.g2.s1.q5.o3"}, 0));
            register(2, new QuestionChain(q, rewardsG2));
        }


        // Set 2: 発射シークエンス (応用・安全)
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s2.q1", new String[]{"quiz.wave_motion_gun_mod.g2.s2.q1.o1", "quiz.wave_motion_gun_mod.g2.s2.q1.o2", "quiz.wave_motion_gun_mod.g2.s2.q1.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s2.q2", new String[]{"quiz.wave_motion_gun_mod.g2.s2.q2.o1", "quiz.wave_motion_gun_mod.g2.s2.q2.o2", "quiz.wave_motion_gun_mod.g2.s2.q2.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s2.q3", new String[]{"quiz.wave_motion_gun_mod.g2.s2.q3.o1", "quiz.wave_motion_gun_mod.g2.s2.q3.o2", "quiz.wave_motion_gun_mod.g2.s2.q3.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s2.q4", new String[]{"quiz.wave_motion_gun_mod.g2.s2.q4.o1", "quiz.wave_motion_gun_mod.g2.s2.q4.o2", "quiz.wave_motion_gun_mod.g2.s2.q4.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s2.q5", new String[]{"quiz.wave_motion_gun_mod.g2.s2.q5.o1", "quiz.wave_motion_gun_mod.g2.s2.q5.o2", "quiz.wave_motion_gun_mod.g2.s2.q5.o3"}, 2));
            register(2, new QuestionChain(q, rewardsG2));
        }

        // Set 3: 力と道具 (理科)
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s3.q1", new String[]{"quiz.wave_motion_gun_mod.g2.s3.q1.o1", "quiz.wave_motion_gun_mod.g2.s3.q1.o2", "quiz.wave_motion_gun_mod.g2.s3.q1.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s3.q2", new String[]{"quiz.wave_motion_gun_mod.g2.s3.q2.o1", "quiz.wave_motion_gun_mod.g2.s3.q2.o2", "quiz.wave_motion_gun_mod.g2.s3.q2.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s3.q3", new String[]{"quiz.wave_motion_gun_mod.g2.s3.q3.o1", "quiz.wave_motion_gun_mod.g2.s3.q3.o2", "quiz.wave_motion_gun_mod.g2.s3.q3.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s3.q4", new String[]{"quiz.wave_motion_gun_mod.g2.s3.q4.o1", "quiz.wave_motion_gun_mod.g2.s3.q4.o2", "quiz.wave_motion_gun_mod.g2.s3.q4.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s3.q5", new String[]{"quiz.wave_motion_gun_mod.g2.s3.q5.o1", "quiz.wave_motion_gun_mod.g2.s3.q5.o2", "quiz.wave_motion_gun_mod.g2.s3.q5.o3"}, 1));
            register(2, new QuestionChain(q, rewardsG2));
        }

        // Set 4: 電気の不思議 (理科)
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s4.q1", new String[]{"quiz.wave_motion_gun_mod.g2.s4.q1.o1", "quiz.wave_motion_gun_mod.g2.s4.q1.o2", "quiz.wave_motion_gun_mod.g2.s4.q1.o3"}, 2));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s4.q2", new String[]{"quiz.wave_motion_gun_mod.g2.s4.q2.o1", "quiz.wave_motion_gun_mod.g2.s4.q2.o2", "quiz.wave_motion_gun_mod.g2.s4.q2.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s4.q3", new String[]{"quiz.wave_motion_gun_mod.g2.s4.q3.o1", "quiz.wave_motion_gun_mod.g2.s4.q3.o2", "quiz.wave_motion_gun_mod.g2.s4.q3.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s4.q4", new String[]{"quiz.wave_motion_gun_mod.g2.s4.q4.o1", "quiz.wave_motion_gun_mod.g2.s4.q4.o2", "quiz.wave_motion_gun_mod.g2.s4.q4.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s4.q5", new String[]{"quiz.wave_motion_gun_mod.g2.s4.q5.o1", "quiz.wave_motion_gun_mod.g2.s4.q5.o2", "quiz.wave_motion_gun_mod.g2.s4.q5.o3"}, 0));
            register(2, new QuestionChain(q, rewardsG2));
        }

        // Set 5: 光と音 (理科)
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s5.q1", new String[]{"quiz.wave_motion_gun_mod.g2.s5.q1.o1", "quiz.wave_motion_gun_mod.g2.s5.q1.o2", "quiz.wave_motion_gun_mod.g2.s5.q1.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s5.q2", new String[]{"quiz.wave_motion_gun_mod.g2.s5.q2.o1", "quiz.wave_motion_gun_mod.g2.s5.q2.o2", "quiz.wave_motion_gun_mod.g2.s5.q2.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s5.q3", new String[]{"quiz.wave_motion_gun_mod.g2.s5.q3.o1", "quiz.wave_motion_gun_mod.g2.s5.q3.o2", "quiz.wave_motion_gun_mod.g2.s5.q3.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s5.q4", new String[]{"quiz.wave_motion_gun_mod.g2.s5.q4.o1", "quiz.wave_motion_gun_mod.g2.s5.q4.o2", "quiz.wave_motion_gun_mod.g2.s5.q4.o3"}, 2));
            q.add(new Question("quiz.wave_motion_gun_mod.g2.s5.q5", new String[]{"quiz.wave_motion_gun_mod.g2.s5.q5.o1", "quiz.wave_motion_gun_mod.g2.s5.q5.o2", "quiz.wave_motion_gun_mod.g2.s5.q5.o3"}, 1));
            register(2, new QuestionChain(q, rewardsG2));
        }


        // ==========================================
        // Group 3: Level B (Clearance B -> A)
        // テーマ: 宇宙の旅 (理科・中級)
        // ==========================================
        List<ItemStack> rewardsG3 = new ArrayList<>();
        rewardsG3.add(new ItemStack(ItemInit.RAW_COSMO_ORE.get(), 2));
        rewardsG3.add(new ItemStack(ItemInit.RAW_COSMO_ORE.get(), 2));
        rewardsG3.add(new ItemStack(ItemInit.RAW_COSMO_ORE.get(), 2));
        rewardsG3.add(new ItemStack(ItemInit.TACHYON_CRYSTAL.get(), 2));
        rewardsG3.add(new ItemStack(ItemInit.TACHYON_CRYSTAL.get(), 1));
        rewardsG3.add(new ItemStack(ItemInit.GRAVITY_LENS.get(), 1));
        rewardsG3.add(new ItemStack(ItemInit.CLEARANCE_B.get(), 3));
        rewardsG3.add(new ItemStack(ItemInit.CLEARANCE_A.get(), 1));
        rewardsG3.add(new ItemStack(ItemInit.CLEARANCE_A.get(), 1));
        rewardsG3.add(new ItemStack(ItemInit.CLEARANCE_A.get(), 1));

        // Set 1: オプションユニットの機能
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s1.q1", new String[]{"quiz.wave_motion_gun_mod.g3.s1.q1.o1", "quiz.wave_motion_gun_mod.g3.s1.q1.o2", "quiz.wave_motion_gun_mod.g3.s1.q1.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s1.q2", new String[]{"quiz.wave_motion_gun_mod.g3.s1.q2.o1", "quiz.wave_motion_gun_mod.g3.s1.q2.o2", "quiz.wave_motion_gun_mod.g3.s1.q2.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s1.q3", new String[]{"quiz.wave_motion_gun_mod.g3.s1.q3.o1", "quiz.wave_motion_gun_mod.g3.s1.q3.o2", "quiz.wave_motion_gun_mod.g3.s1.q3.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s1.q4", new String[]{"quiz.wave_motion_gun_mod.g3.s1.q4.o1", "quiz.wave_motion_gun_mod.g3.s1.q4.o2", "quiz.wave_motion_gun_mod.g3.s1.q4.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s1.q5", new String[]{"quiz.wave_motion_gun_mod.g3.s1.q5.o1", "quiz.wave_motion_gun_mod.g3.s1.q5.o2", "quiz.wave_motion_gun_mod.g3.s1.q5.o3"}, 1));
            register(3, new QuestionChain(q, rewardsG3));
        }

        // Set 2: 太陽系の仲間
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s2.q1", new String[]{"quiz.wave_motion_gun_mod.g3.s2.q1.o1", "quiz.wave_motion_gun_mod.g3.s2.q1.o2", "quiz.wave_motion_gun_mod.g3.s2.q1.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s2.q2", new String[]{"quiz.wave_motion_gun_mod.g3.s2.q2.o1", "quiz.wave_motion_gun_mod.g3.s2.q2.o2", "quiz.wave_motion_gun_mod.g3.s2.q2.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s2.q3", new String[]{"quiz.wave_motion_gun_mod.g3.s2.q3.o1", "quiz.wave_motion_gun_mod.g3.s2.q3.o2", "quiz.wave_motion_gun_mod.g3.s2.q3.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s2.q4", new String[]{"quiz.wave_motion_gun_mod.g3.s2.q4.o1", "quiz.wave_motion_gun_mod.g3.s2.q4.o2", "quiz.wave_motion_gun_mod.g3.s2.q4.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s2.q5", new String[]{"quiz.wave_motion_gun_mod.g3.s2.q5.o1", "quiz.wave_motion_gun_mod.g3.s2.q5.o2", "quiz.wave_motion_gun_mod.g3.s2.q5.o3"}, 0));
            register(3, new QuestionChain(q, rewardsG3));
        }

        // Set 3: 月と地球
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s3.q1", new String[]{"quiz.wave_motion_gun_mod.g3.s3.q1.o1", "quiz.wave_motion_gun_mod.g3.s3.q1.o2", "quiz.wave_motion_gun_mod.g3.s3.q1.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s3.q2", new String[]{"quiz.wave_motion_gun_mod.g3.s3.q2.o1", "quiz.wave_motion_gun_mod.g3.s3.q2.o2", "quiz.wave_motion_gun_mod.g3.s3.q2.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s3.q3", new String[]{"quiz.wave_motion_gun_mod.g3.s3.q3.o1", "quiz.wave_motion_gun_mod.g3.s3.q3.o2", "quiz.wave_motion_gun_mod.g3.s3.q3.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s3.q4", new String[]{"quiz.wave_motion_gun_mod.g3.s3.q4.o1", "quiz.wave_motion_gun_mod.g3.s3.q4.o2", "quiz.wave_motion_gun_mod.g3.s3.q4.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s3.q5", new String[]{"quiz.wave_motion_gun_mod.g3.s3.q5.o1", "quiz.wave_motion_gun_mod.g3.s3.q5.o2", "quiz.wave_motion_gun_mod.g3.s3.q5.o3"}, 1));
            register(3, new QuestionChain(q, rewardsG3));
        }

        // Set 4: 宇宙環境
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s4.q1", new String[]{"quiz.wave_motion_gun_mod.g3.s4.q1.o1", "quiz.wave_motion_gun_mod.g3.s4.q1.o2", "quiz.wave_motion_gun_mod.g3.s4.q1.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s4.q2", new String[]{"quiz.wave_motion_gun_mod.g3.s4.q2.o1", "quiz.wave_motion_gun_mod.g3.s4.q2.o2", "quiz.wave_motion_gun_mod.g3.s4.q2.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s4.q3", new String[]{"quiz.wave_motion_gun_mod.g3.s4.q3.o1", "quiz.wave_motion_gun_mod.g3.s4.q3.o2", "quiz.wave_motion_gun_mod.g3.s4.q3.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s4.q4", new String[]{"quiz.wave_motion_gun_mod.g3.s4.q4.o1", "quiz.wave_motion_gun_mod.g3.s4.q4.o2", "quiz.wave_motion_gun_mod.g3.s4.q4.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s4.q5", new String[]{"quiz.wave_motion_gun_mod.g3.s4.q5.o1", "quiz.wave_motion_gun_mod.g3.s4.q5.o2", "quiz.wave_motion_gun_mod.g3.s4.q5.o3"}, 1));
            register(3, new QuestionChain(q, rewardsG3));
        }

        // Set 5: ロケット工学入門
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s5.q1", new String[]{"quiz.wave_motion_gun_mod.g3.s5.q1.o1", "quiz.wave_motion_gun_mod.g3.s5.q1.o2", "quiz.wave_motion_gun_mod.g3.s5.q1.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s5.q2", new String[]{"quiz.wave_motion_gun_mod.g3.s5.q2.o1", "quiz.wave_motion_gun_mod.g3.s5.q2.o2", "quiz.wave_motion_gun_mod.g3.s5.q2.o3"}, 2));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s5.q3", new String[]{"quiz.wave_motion_gun_mod.g3.s5.q3.o1", "quiz.wave_motion_gun_mod.g3.s5.q3.o2", "quiz.wave_motion_gun_mod.g3.s5.q3.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s5.q4", new String[]{"quiz.wave_motion_gun_mod.g3.s5.q4.o1", "quiz.wave_motion_gun_mod.g3.s5.q4.o2", "quiz.wave_motion_gun_mod.g3.s5.q4.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s5.q5", new String[]{"quiz.wave_motion_gun_mod.g3.s5.q5.o1", "quiz.wave_motion_gun_mod.g3.s5.q5.o2", "quiz.wave_motion_gun_mod.g3.s5.q5.o3"}, 1));
            register(3, new QuestionChain(q, rewardsG3));
        }

        // Set 6: 星座と動き
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s6.q1", new String[]{"quiz.wave_motion_gun_mod.g3.s6.q1.o1", "quiz.wave_motion_gun_mod.g3.s6.q1.o2", "quiz.wave_motion_gun_mod.g3.s6.q1.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s6.q2", new String[]{"quiz.wave_motion_gun_mod.g3.s6.q2.o1", "quiz.wave_motion_gun_mod.g3.s6.q2.o2", "quiz.wave_motion_gun_mod.g3.s6.q2.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s6.q3", new String[]{"quiz.wave_motion_gun_mod.g3.s6.q3.o1", "quiz.wave_motion_gun_mod.g3.s6.q3.o2", "quiz.wave_motion_gun_mod.g3.s6.q3.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s6.q4", new String[]{"quiz.wave_motion_gun_mod.g3.s6.q4.o1", "quiz.wave_motion_gun_mod.g3.s6.q4.o2", "quiz.wave_motion_gun_mod.g3.s6.q4.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g3.s6.q5", new String[]{"quiz.wave_motion_gun_mod.g3.s6.q5.o1", "quiz.wave_motion_gun_mod.g3.s6.q5.o2", "quiz.wave_motion_gun_mod.g3.s6.q5.o3"}, 0));
            register(3, new QuestionChain(q, rewardsG3));
        }


        // ==========================================
        // Group 4: Level A (Clearance A -> S)
        // テーマ: 星々の極限 (理科・上級)
        // ==========================================
        List<ItemStack> rewardsG4 = new ArrayList<>();
        rewardsG4.add(new ItemStack(ItemInit.RAW_COSMO_ORE.get(), 3));
        rewardsG4.add(new ItemStack(ItemInit.RAW_COSMO_ORE.get(), 3));
        rewardsG4.add(new ItemStack(ItemInit.TACHYON_CRYSTAL.get(), 3));
        rewardsG4.add(new ItemStack(ItemInit.GRAVITY_LENS.get(), 1));
        rewardsG4.add(new ItemStack(ItemInit.QUANTUM_UNIT.get(), 1));
        rewardsG4.add(new ItemStack(ItemInit.QUANTUM_UNIT.get(), 1));
        rewardsG4.add(new ItemStack(ItemInit.CLEARANCE_A.get(), 2));
        rewardsG4.add(new ItemStack(ItemInit.CLEARANCE_A.get(), 2));
        rewardsG4.add(new ItemStack(ItemInit.CLEARANCE_S.get(), 1));
        rewardsG4.add(new ItemStack(ItemInit.CLEARANCE_S.get(), 1));


        // Set 1: 発射シークエンス (基礎)
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s1.q1", new String[]{"quiz.wave_motion_gun_mod.g4.s1.q1.o1", "quiz.wave_motion_gun_mod.g4.s1.q1.o2", "quiz.wave_motion_gun_mod.g4.s1.q1.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s1.q2", new String[]{"quiz.wave_motion_gun_mod.g4.s1.q2.o1", "quiz.wave_motion_gun_mod.g4.s1.q2.o2", "quiz.wave_motion_gun_mod.g4.s1.q2.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s1.q3", new String[]{"quiz.wave_motion_gun_mod.g4.s1.q3.o1", "quiz.wave_motion_gun_mod.g4.s1.q3.o2", "quiz.wave_motion_gun_mod.g4.s1.q3.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s1.q4", new String[]{"quiz.wave_motion_gun_mod.g4.s1.q4.o1", "quiz.wave_motion_gun_mod.g4.s1.q4.o2", "quiz.wave_motion_gun_mod.g4.s1.q4.o3"}, 2));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s1.q5", new String[]{"quiz.wave_motion_gun_mod.g4.s1.q5.o1", "quiz.wave_motion_gun_mod.g4.s1.q5.o2", "quiz.wave_motion_gun_mod.g4.s1.q5.o3"}, 2));
            register(4, new QuestionChain(q, rewardsG4));
        }

        // Set 2: 星の一生
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s2.q1", new String[]{"quiz.wave_motion_gun_mod.g4.s2.q1.o1", "quiz.wave_motion_gun_mod.g4.s2.q1.o2", "quiz.wave_motion_gun_mod.g4.s2.q1.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s2.q2", new String[]{"quiz.wave_motion_gun_mod.g4.s2.q2.o1", "quiz.wave_motion_gun_mod.g4.s2.q2.o2", "quiz.wave_motion_gun_mod.g4.s2.q2.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s2.q3", new String[]{"quiz.wave_motion_gun_mod.g4.s2.q3.o1", "quiz.wave_motion_gun_mod.g4.s2.q3.o2", "quiz.wave_motion_gun_mod.g4.s2.q3.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s2.q4", new String[]{"quiz.wave_motion_gun_mod.g4.s2.q4.o1", "quiz.wave_motion_gun_mod.g4.s2.q4.o2", "quiz.wave_motion_gun_mod.g4.s2.q4.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s2.q5", new String[]{"quiz.wave_motion_gun_mod.g4.s2.q5.o1", "quiz.wave_motion_gun_mod.g4.s2.q5.o2", "quiz.wave_motion_gun_mod.g4.s2.q5.o3"}, 1));
            register(4, new QuestionChain(q, rewardsG4));
        }

        // Set 3: ブラックホールの謎
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s3.q1", new String[]{"quiz.wave_motion_gun_mod.g4.s3.q1.o1", "quiz.wave_motion_gun_mod.g4.s3.q1.o2", "quiz.wave_motion_gun_mod.g4.s3.q1.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s3.q2", new String[]{"quiz.wave_motion_gun_mod.g4.s3.q2.o1", "quiz.wave_motion_gun_mod.g4.s3.q2.o2", "quiz.wave_motion_gun_mod.g4.s3.q2.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s3.q3", new String[]{"quiz.wave_motion_gun_mod.g4.s3.q3.o1", "quiz.wave_motion_gun_mod.g4.s3.q3.o2", "quiz.wave_motion_gun_mod.g4.s3.q3.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s3.q4", new String[]{"quiz.wave_motion_gun_mod.g4.s3.q4.o1", "quiz.wave_motion_gun_mod.g4.s3.q4.o2", "quiz.wave_motion_gun_mod.g4.s3.q4.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s3.q5", new String[]{"quiz.wave_motion_gun_mod.g4.s3.q5.o1", "quiz.wave_motion_gun_mod.g4.s3.q5.o2", "quiz.wave_motion_gun_mod.g4.s3.q5.o3"}, 2));
            register(4, new QuestionChain(q, rewardsG4));
        }

        // Set 4: 宇宙論
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s4.q1", new String[]{"quiz.wave_motion_gun_mod.g4.s4.q1.o1", "quiz.wave_motion_gun_mod.g4.s4.q1.o2", "quiz.wave_motion_gun_mod.g4.s4.q1.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s4.q2", new String[]{"quiz.wave_motion_gun_mod.g4.s4.q2.o1", "quiz.wave_motion_gun_mod.g4.s4.q2.o2", "quiz.wave_motion_gun_mod.g4.s4.q2.o3"}, 2));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s4.q3", new String[]{"quiz.wave_motion_gun_mod.g4.s4.q3.o1", "quiz.wave_motion_gun_mod.g4.s4.q3.o2", "quiz.wave_motion_gun_mod.g4.s4.q3.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s4.q4", new String[]{"quiz.wave_motion_gun_mod.g4.s4.q4.o1", "quiz.wave_motion_gun_mod.g4.s4.q4.o2", "quiz.wave_motion_gun_mod.g4.s4.q4.o3"}, 2));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s4.q5", new String[]{"quiz.wave_motion_gun_mod.g4.s4.q5.o1", "quiz.wave_motion_gun_mod.g4.s4.q5.o2", "quiz.wave_motion_gun_mod.g4.s4.q5.o3"}, 0));
            register(4, new QuestionChain(q, rewardsG4));
        }

        // Set 5: 光の物理
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s5.q1", new String[]{"quiz.wave_motion_gun_mod.g4.s5.q1.o1", "quiz.wave_motion_gun_mod.g4.s5.q1.o2", "quiz.wave_motion_gun_mod.g4.s5.q1.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s5.q2", new String[]{"quiz.wave_motion_gun_mod.g4.s5.q2.o1", "quiz.wave_motion_gun_mod.g4.s5.q2.o2", "quiz.wave_motion_gun_mod.g4.s5.q2.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s5.q3", new String[]{"quiz.wave_motion_gun_mod.g4.s5.q3.o1", "quiz.wave_motion_gun_mod.g4.s5.q3.o2", "quiz.wave_motion_gun_mod.g4.s5.q3.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s5.q4", new String[]{"quiz.wave_motion_gun_mod.g4.s5.q4.o1", "quiz.wave_motion_gun_mod.g4.s5.q4.o2", "quiz.wave_motion_gun_mod.g4.s5.q4.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s5.q5", new String[]{"quiz.wave_motion_gun_mod.g4.s5.q5.o1", "quiz.wave_motion_gun_mod.g4.s5.q5.o2", "quiz.wave_motion_gun_mod.g4.s5.q5.o3"}, 2));
            register(4, new QuestionChain(q, rewardsG4));
        }

        // Set 6: 極限物質
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s6.q1", new String[]{"quiz.wave_motion_gun_mod.g4.s6.q1.o1", "quiz.wave_motion_gun_mod.g4.s6.q1.o2", "quiz.wave_motion_gun_mod.g4.s6.q1.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s6.q2", new String[]{"quiz.wave_motion_gun_mod.g4.s6.q2.o1", "quiz.wave_motion_gun_mod.g4.s6.q2.o2", "quiz.wave_motion_gun_mod.g4.s6.q2.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s6.q3", new String[]{"quiz.wave_motion_gun_mod.g4.s6.q3.o1", "quiz.wave_motion_gun_mod.g4.s6.q3.o2", "quiz.wave_motion_gun_mod.g4.s6.q3.o3"}, 2));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s6.q4", new String[]{"quiz.wave_motion_gun_mod.g4.s6.q4.o1", "quiz.wave_motion_gun_mod.g4.s6.q4.o2", "quiz.wave_motion_gun_mod.g4.s6.q4.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g4.s6.q5", new String[]{"quiz.wave_motion_gun_mod.g4.s6.q5.o1", "quiz.wave_motion_gun_mod.g4.s6.q5.o2", "quiz.wave_motion_gun_mod.g4.s6.q5.o3"}, 1));
            register(4, new QuestionChain(q, rewardsG4));
        }


        // ==========================================
        // Group 5: Level S (Clearance S)
        // テーマ: 波動とロマン (SF・Mod設定)
        // ==========================================
        List<ItemStack> rewardsG5 = new ArrayList<>();
        rewardsG5.add(new ItemStack(ItemInit.RAW_COSMO_ORE.get(), 4));
        rewardsG5.add(new ItemStack(ItemInit.TACHYON_CRYSTAL.get(), 4));
        rewardsG5.add(new ItemStack(ItemInit.CLEARANCE_S.get(), 2));
        rewardsG5.add(new ItemStack(ItemInit.CLEARANCE_S.get(), 1));
        rewardsG5.add(new ItemStack(ItemInit.GRAVITY_LENS.get(), 1));
        rewardsG5.add(new ItemStack(ItemInit.GRAVITY_LENS.get(), 1));
        rewardsG5.add(new ItemStack(ItemInit.QUANTUM_UNIT.get(), 1));
        rewardsG5.add(new ItemStack(ItemInit.QUANTUM_UNIT.get(), 1));
        rewardsG5.add(new ItemStack(ItemInit.WAVE_CORE.get(), 1));

        // Set 1: ロマン
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s1.q1", new String[]{"quiz.wave_motion_gun_mod.g5.s1.q1.o1", "quiz.wave_motion_gun_mod.g5.s1.q1.o2", "quiz.wave_motion_gun_mod.g5.s1.q1.o3"}, 2));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s1.q2", new String[]{"quiz.wave_motion_gun_mod.g5.s1.q2.o1", "quiz.wave_motion_gun_mod.g5.s1.q2.o2", "quiz.wave_motion_gun_mod.g5.s1.q2.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s1.q3", new String[]{"quiz.wave_motion_gun_mod.g5.s1.q3.o1", "quiz.wave_motion_gun_mod.g5.s1.q3.o2", "quiz.wave_motion_gun_mod.g5.s1.q3.o3"}, 2));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s1.q4", new String[]{"quiz.wave_motion_gun_mod.g5.s1.q4.o1", "quiz.wave_motion_gun_mod.g5.s1.q4.o2", "quiz.wave_motion_gun_mod.g5.s1.q4.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s1.q5", new String[]{"quiz.wave_motion_gun_mod.g5.s1.q5.o1", "quiz.wave_motion_gun_mod.g5.s1.q5.o2", "quiz.wave_motion_gun_mod.g5.s1.q5.o3"}, 1));
            register(5, new QuestionChain(q, rewardsG5));
        }

        // Set 2: 相対論
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s2.q1", new String[]{"quiz.wave_motion_gun_mod.g5.s2.q1.o1", "quiz.wave_motion_gun_mod.g5.s2.q1.o2", "quiz.wave_motion_gun_mod.g5.s2.q1.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s2.q2", new String[]{"quiz.wave_motion_gun_mod.g5.s2.q2.o1", "quiz.wave_motion_gun_mod.g5.s2.q2.o2", "quiz.wave_motion_gun_mod.g5.s2.q2.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s2.q3", new String[]{"quiz.wave_motion_gun_mod.g5.s2.q3.o1", "quiz.wave_motion_gun_mod.g5.s2.q3.o2", "quiz.wave_motion_gun_mod.g5.s2.q3.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s2.q4", new String[]{"quiz.wave_motion_gun_mod.g5.s2.q4.o1", "quiz.wave_motion_gun_mod.g5.s2.q4.o2", "quiz.wave_motion_gun_mod.g5.s2.q4.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s2.q5", new String[]{"quiz.wave_motion_gun_mod.g5.s2.q5.o1", "quiz.wave_motion_gun_mod.g5.s2.q5.o2", "quiz.wave_motion_gun_mod.g5.s2.q5.o3"}, 1));
            register(5, new QuestionChain(q, rewardsG5));
        }

        // Set 3: 量子論
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s3.q1", new String[]{"quiz.wave_motion_gun_mod.g5.s3.q1.o1", "quiz.wave_motion_gun_mod.g5.s3.q1.o2", "quiz.wave_motion_gun_mod.g5.s3.q1.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s3.q2", new String[]{"quiz.wave_motion_gun_mod.g5.s3.q2.o1", "quiz.wave_motion_gun_mod.g5.s3.q2.o2", "quiz.wave_motion_gun_mod.g5.s3.q2.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s3.q3", new String[]{"quiz.wave_motion_gun_mod.g5.s3.q3.o1", "quiz.wave_motion_gun_mod.g5.s3.q3.o2", "quiz.wave_motion_gun_mod.g5.s3.q3.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s3.q4", new String[]{"quiz.wave_motion_gun_mod.g5.s3.q4.o1", "quiz.wave_motion_gun_mod.g5.s3.q4.o2", "quiz.wave_motion_gun_mod.g5.s3.q4.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s3.q5", new String[]{"quiz.wave_motion_gun_mod.g5.s3.q5.o1", "quiz.wave_motion_gun_mod.g5.s3.q5.o2", "quiz.wave_motion_gun_mod.g5.s3.q5.o3"}, 1));
            register(5, new QuestionChain(q, rewardsG5));
        }

        // Set 4: SFガジェット
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s4.q1", new String[]{"quiz.wave_motion_gun_mod.g5.s4.q1.o1", "quiz.wave_motion_gun_mod.g5.s4.q1.o2", "quiz.wave_motion_gun_mod.g5.s4.q1.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s4.q2", new String[]{"quiz.wave_motion_gun_mod.g5.s4.q2.o1", "quiz.wave_motion_gun_mod.g5.s4.q2.o2", "quiz.wave_motion_gun_mod.g5.s4.q2.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s4.q3", new String[]{"quiz.wave_motion_gun_mod.g5.s4.q3.o1", "quiz.wave_motion_gun_mod.g5.s4.q3.o2", "quiz.wave_motion_gun_mod.g5.s4.q3.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s4.q4", new String[]{"quiz.wave_motion_gun_mod.g5.s4.q4.o1", "quiz.wave_motion_gun_mod.g5.s4.q4.o2", "quiz.wave_motion_gun_mod.g5.s4.q4.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s4.q5", new String[]{"quiz.wave_motion_gun_mod.g5.s4.q5.o1", "quiz.wave_motion_gun_mod.g5.s4.q5.o2", "quiz.wave_motion_gun_mod.g5.s4.q5.o3"}, 0));
            register(5, new QuestionChain(q, rewardsG5));
        }

        // Set 5: Mod Lore (Mod設定)
        {
            List<Question> q = new ArrayList<>();
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s5.q1", new String[]{"quiz.wave_motion_gun_mod.g5.s5.q1.o1", "quiz.wave_motion_gun_mod.g5.s5.q1.o2", "quiz.wave_motion_gun_mod.g5.s5.q1.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s5.q2", new String[]{"quiz.wave_motion_gun_mod.g5.s5.q2.o1", "quiz.wave_motion_gun_mod.g5.s5.q2.o2", "quiz.wave_motion_gun_mod.g5.s5.q2.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s5.q3", new String[]{"quiz.wave_motion_gun_mod.g5.s5.q3.o1", "quiz.wave_motion_gun_mod.g5.s5.q3.o2", "quiz.wave_motion_gun_mod.g5.s5.q3.o3"}, 1));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s5.q4", new String[]{"quiz.wave_motion_gun_mod.g5.s5.q4.o1", "quiz.wave_motion_gun_mod.g5.s5.q4.o2", "quiz.wave_motion_gun_mod.g5.s5.q4.o3"}, 0));
            q.add(new Question("quiz.wave_motion_gun_mod.g5.s5.q5", new String[]{"quiz.wave_motion_gun_mod.g5.s5.q5.o1", "quiz.wave_motion_gun_mod.g5.s5.q5.o2", "quiz.wave_motion_gun_mod.g5.s5.q5.o3"}, 0));
            register(5, new QuestionChain(q, rewardsG5));
        }

    }

    // ★修正: 指定されたグループから、シーケンス番号に基づいてチェーンIDを取得
    public static int getChainIndex(int groupId, int sequence) {
        List<Integer> ids = GROUP_MAP.get(groupId);

        // 指定グループが存在しない、または空の場合はデフォルト(1)へフォールバック
        if (ids == null || ids.isEmpty()) {
            ids = GROUP_MAP.get(1);
        }

        // デフォルトも空なら安全策として0を返す
        if (ids == null || ids.isEmpty()) return 0;

        // シーケンスをリストサイズで割った余りを使うことでローテーションさせる
        // 【修正】sequenceが負でもクラッシュしないよう floorMod を使用
        return ids.get(Math.floorMod(sequence, ids.size()));
    }

    // ランダムにチェーンを選ぶ (SupplyCrateBlockEntity のルート生成で使用)
    public static int getRandomChainIndex(int groupId) {
        return getChainIndex(groupId, RANDOM.nextInt(100));
    }

    public static QuestionChain getChain(int index) {
        if (index >= 0 && index < CHAINS.size()) return CHAINS.get(index);
        return CHAINS.get(0);
    }

    public static class QuestionChain {
        public final List<Question> questions;
        public final List<ItemStack> rewards;
        public QuestionChain(List<Question> questions, List<ItemStack> rewards) {
            this.questions = questions;
            this.rewards = rewards;
        }
        public ItemStack getRandomReward() {
            if (rewards.isEmpty()) return ItemStack.EMPTY;
            return rewards.get(RANDOM.nextInt(rewards.size())).copy();
        }
    }
    public static class Question {
        public final String text;
        public final String[] options;
        public final int correctIndex;
        public Question(String text, String[] options, int correctIndex) {
            this.text = text;
            this.options = options;
            this.correctIndex = correctIndex;
        }
    }
}