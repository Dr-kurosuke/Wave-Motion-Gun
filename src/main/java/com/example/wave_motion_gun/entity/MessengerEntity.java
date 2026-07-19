package com.example.wave_motion_gun.entity;

import com.example.wave_motion_gun.init.ItemInit;
import com.example.wave_motion_gun.utils.QuestionManager;
import com.example.wave_motion_gun.world.data.SupplyDropManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class MessengerEntity extends Mob {
    private static final EntityDataAccessor<Integer> CHAIN_INDEX = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> QUESTION_INDEX = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_ACTIVE = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> REWARD_RANK = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.INT);
    // AI生成問題セッションが受理済みかどうか(クライアントはこれを見て表示を切り替える)
    private static final EntityDataAccessor<Boolean> AI_ACTIVE = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.BOOLEAN);

    // サーバー側のみ: 受理したAI問題(正解インデックスを含む)。判定はサーバー権威。
    private java.util.List<QuestionManager.Question> aiQuestions = null;

    private int lifeTime = 0;
    private static final int MAX_LIFE = 2400;

    public MessengerEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CHAIN_INDEX, 0);
        this.entityData.define(QUESTION_INDEX, 0);
        this.entityData.define(IS_ACTIVE, false);
        this.entityData.define(REWARD_RANK, 0);
        this.entityData.define(AI_ACTIVE, false);
    }

    public boolean isAiActive() { return this.entityData.get(AI_ACTIVE); }

    /**
     * クライアント生成のAI問題セットを受理する(サーバー側)。
     * 1セッション1回のみ。受理すると進行を先頭に戻し、AI_ACTIVEを同期する。
     */
    public boolean acceptAiQuestions(java.util.List<QuestionManager.Question> questions) {
        if (this.level.isClientSide || this.aiQuestions != null || !isActive()) return false;
        this.aiQuestions = questions;
        this.entityData.set(QUESTION_INDEX, 0);
        this.entityData.set(AI_ACTIVE, true);
        return true;
    }

    public void assignChainBySequence(int groupId, int sequence) {
        if (!this.level.isClientSide) {
            int index = QuestionManager.getChainIndex(groupId, sequence);
            this.entityData.set(CHAIN_INDEX, index);
            this.entityData.set(QUESTION_INDEX, 0);
            this.entityData.set(IS_ACTIVE, true);
        }
    }

    public int getChainIndex() { return this.entityData.get(CHAIN_INDEX); }
    public int getQuestionIndex() { return this.entityData.get(QUESTION_INDEX); }
    public boolean isActive() { return this.entityData.get(IS_ACTIVE); }
    public int getRewardRank() { return this.entityData.get(REWARD_RANK); }

    public void setQuestionIndex(int index) {
        this.entityData.set(QUESTION_INDEX, index);
    }

    public void advanceQuestion() {
        this.entityData.set(QUESTION_INDEX, getQuestionIndex() + 1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, 20.0F, 1.0F));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) {
            lifeTime++;
            if (lifeTime > MAX_LIFE) this.discard();
            // プレイヤーへの注視は registerGoals の LookAtPlayerGoal が担当する
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            if (isActive()) {
                if (this.level.isClientSide) {
                    openGui();
                }
                return InteractionResult.SUCCESS;
            } else if (!this.level.isClientSide) {
                return tryActivate(player, hand);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    private InteractionResult tryActivate(Player player, InteractionHand hand) {
        // 実績: 初めての交信 (サーバー側でのみ呼ばれる)
        if (player instanceof ServerPlayer serverPlayer) {
            com.example.wave_motion_gun.utils.AdvancementHelper.grant(serverPlayer, "first_contact");
        }

        CompoundTag data = player.getPersistentData();
        String waitingKey = "WaveMotionGun_IsWaitingDrop";
        String timeKey = "WaveMotionGun_DropStartTime";
        long currentTime = this.level.getGameTime();

        if (data.getBoolean(waitingKey)) {
            if (data.contains(timeKey)) {
                long startTime = data.getLong(timeKey);
                if (currentTime - startTime > 3600) {
                    data.putBoolean(waitingKey, false);
                } else {
                    player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent("message.wave_motion_gun_mod.lady.wait"), true);
                    return InteractionResult.FAIL;
                }
            } else {
                data.putBoolean(waitingKey, false);
            }
        }

        ItemStack stack = player.getItemInHand(hand);
        int selectedGroup = 1;
        boolean consumeItem = false;
        int consumeAmount = 1;
        String rankName = "D";
        int rankValue = 0;

        if (stack.is(ItemInit.CLEARANCE_S.get())) {
            selectedGroup = 5;
            consumeItem = true;
            rankName = "S";
            rankValue = 4;
        } else if (stack.is(ItemInit.CLEARANCE_A.get())) {
            selectedGroup = 4;
            consumeItem = true;
            rankName = "A";
            rankValue = 3;
        } else if (stack.is(ItemInit.CLEARANCE_B.get())) {
            selectedGroup = 3;
            consumeItem = true;
            rankName = "B";
            rankValue = 2;
        } else if (stack.is(ItemInit.CLEARANCE_C.get())) {
            selectedGroup = 2;
            consumeItem = true;
            rankName = "C";
            rankValue = 1;
        } else {
            rankValue = 0;
        }

        if (consumeItem) {
            stack.shrink(consumeAmount);
            this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
        }

        String sequenceKey = "WaveMotionGun_QuestSequence_G" + selectedGroup;
        int sequence = player.getPersistentData().getInt(sequenceKey);

        assignChainBySequence(selectedGroup, sequence);
        this.entityData.set(REWARD_RANK, rankValue);

        player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent("message.wave_motion_gun_mod.lady.auth", rankName), true);

        return InteractionResult.SUCCESS;
    }

    @OnlyIn(Dist.CLIENT)
    private void openGui() {
        Minecraft.getInstance().setScreen(new com.example.wave_motion_gun.client.MessengerScreen(this));
    }

    public void processAnswer(ServerPlayer player, int answerIndex) {
        // AI問題セッションが受理済みならAIセット、それ以外は静的問題集で判定(どちらもサーバー権威)
        java.util.List<QuestionManager.Question> questions;
        if (this.aiQuestions != null) {
            questions = this.aiQuestions;
        } else {
            if (isAiActive()) {
                // AI_ACTIVEなのにサーバーがセットを持っていない(再起動等)場合は静的問題集へ戻す
                this.entityData.set(AI_ACTIVE, false);
                this.entityData.set(QUESTION_INDEX, 0);
                return;
            }
            questions = QuestionManager.getChain(getChainIndex()).questions;
        }

        int currentQIdx = getQuestionIndex();

        if (currentQIdx >= questions.size()) return;

        QuestionManager.Question currentQuestion = questions.get(currentQIdx);

        if (answerIndex == currentQuestion.correctIndex) {
            advanceQuestion();
            if (currentQIdx + 1 >= questions.size()) {
                triggerSupplyDrop(player);
            }
        } else {
            player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent("message.wave_motion_gun_mod.lady.disappointed"), true);
            this.entityData.set(QUESTION_INDEX, -1);
            this.discard();
        }
    }

    private void triggerSupplyDrop(ServerPlayer player) {
        ServerLevel serverLevel = (ServerLevel) this.level;

        // 実績: クイズ全問正解。AI生成問題を突破した場合は隠し実績も付与
        com.example.wave_motion_gun.utils.AdvancementHelper.grant(player, "first_quiz");
        if (this.aiQuestions != null) {
            com.example.wave_motion_gun.utils.AdvancementHelper.grant(player, "ai_quiz");
        }

        int rankValue = getRewardRank();
        int groupId = rankValue + 1;
        String sequenceKey = "WaveMotionGun_QuestSequence_G" + groupId;

        String waitingKey = "WaveMotionGun_IsWaitingDrop";
        String timeKey = "WaveMotionGun_DropStartTime";
        CompoundTag data = player.getPersistentData();

        data.putBoolean(waitingKey, true);
        data.putLong(timeKey, serverLevel.getGameTime());

        int currentSeq = data.getInt(sequenceKey);
        data.putInt(sequenceKey, currentSeq + 1);

        double angle = this.random.nextDouble() * 2 * Math.PI;
        double distance = 200 + this.random.nextDouble() * 100;
        int targetX = (int) (this.getX() + Math.cos(angle) * distance);
        int targetZ = (int) (this.getZ() + Math.sin(angle) * distance);

        // ★修正: ターゲット決定ロジックのみ (土台生成はマネージャーに移譲)
        // 水面も含めて最も高いブロックのY座標を取得
        int targetY = serverLevel.getHeight(Heightmap.Types.WORLD_SURFACE, targetX, targetZ);
        BlockPos targetPos = new BlockPos(targetX, targetY, targetZ);

        double offsetAngle = this.random.nextDouble() * 2 * Math.PI;
        double offsetDist = this.random.nextDouble() * 30.0;
        int displayX = (int) (targetX + Math.cos(offsetAngle) * offsetDist);
        int displayZ = (int) (targetZ + Math.sin(offsetAngle) * offsetDist);

        SupplyDropManager.get(serverLevel).triggerDrop(serverLevel, targetPos, rankValue, player.getUUID());

        ItemStack dataPad = new ItemStack(ItemInit.COORDINATE_DATAPAD.get());
        CompoundTag tag = dataPad.getOrCreateTag();
        tag.putInt("TargetX", displayX);
        tag.putInt("TargetZ", displayZ);
        tag.putLong("Timestamp", serverLevel.getGameTime());

        if (!player.getInventory().add(dataPad)) {
            player.drop(dataPad, false);
        }

        player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent("message.wave_motion_gun_mod.lady.transfer"), false);
        this.playSound(SoundEvents.BEACON_DEACTIVATE, 1.0F, 1.0F);

        this.discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ChainIndex", getChainIndex());
        tag.putInt("QuestionIndex", getQuestionIndex());
        tag.putBoolean("IsActive", isActive());
        tag.putInt("RewardRank", getRewardRank());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ChainIndex")) this.entityData.set(CHAIN_INDEX, tag.getInt("ChainIndex"));
        if (tag.contains("QuestionIndex")) this.entityData.set(QUESTION_INDEX, tag.getInt("QuestionIndex"));
        if (tag.contains("IsActive")) this.entityData.set(IS_ACTIVE, tag.getBoolean("IsActive"));
        if (tag.contains("RewardRank")) this.entityData.set(REWARD_RANK, tag.getInt("RewardRank"));
    }

    @Override public void push(Entity entity) {}
    @Override protected void doPush(Entity entity) {}
    @Override public boolean isPushable() { return false; }

    @Override
    public Packet<?> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    // 攻撃のインタラクション自体をスキップさせる（trueを返すと攻撃処理が行われない）
    @Override
    public boolean skipAttackInteraction(Entity entity) {
        return true;
    }

    // このエンティティが攻撃対象として有効かどうか（falseで敵対Mob等のターゲットからも外れる）
    @Override
    public boolean isAttackable() {
        return false;
    }

    // ダメージ処理を完全に無効化（念の為の保険）
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }
}