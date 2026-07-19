package com.example.wave_motion_gun.client.ai;

import com.example.wave_motion_gun.utils.QuestionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * クライアント側で生成したAI問題の一時保管庫。
 * 問題文はサーバーへ送信済みだが、描画は生成元クライアントのローカルコピーで行う。
 * キー: MessengerEntityのエンティティID
 */
public class AiQuestionCache {
    private static final Map<Integer, List<QuestionManager.Question>> CACHE = new HashMap<>();

    public static void put(int entityId, List<QuestionManager.Question> questions) {
        CACHE.put(entityId, questions);
    }

    public static List<QuestionManager.Question> get(int entityId) {
        return CACHE.get(entityId);
    }

    public static void remove(int entityId) {
        CACHE.remove(entityId);
    }

    public static void clear() {
        CACHE.clear();
    }
}
