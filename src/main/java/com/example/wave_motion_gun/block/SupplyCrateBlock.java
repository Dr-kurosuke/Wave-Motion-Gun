package com.example.wave_motion_gun.block;

import com.example.wave_motion_gun.blockentity.SupplyCrateBlockEntity;
import com.example.wave_motion_gun.item.CoordinateDatapadItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean; // 追加

public class SupplyCrateBlock extends FallingBlock implements EntityBlock {

    public SupplyCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SupplyCrateBlockEntity(pos, state);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
        if (isFree(level.getBlockState(pos.below())) && pos.getY() >= level.getMinBuildHeight()) {

            // 落下する前に BlockEntity にフラグを立てる
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SupplyCrateBlockEntity crate) {
                crate.isFalling = true; // これにより onRemove でのドロップを回避
            }

            FallingBlockEntity entity = FallingBlockEntity.fall(level, pos, state);

            // データを落下エンティティに引き継ぐ
            if (be instanceof SupplyCrateBlockEntity) {
                entity.blockData = be.saveWithoutMetadata();
            }

            this.falling(entity);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SupplyCrateBlockEntity crate) {
                if (!crate.isUnlocked()) {
                    // 1. クリエイティブモードのチェック
                    if (player.isCreative()) {
                        player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent("message.wave_motion_gun_mod.crate.creative"), true);
                        NetworkHooks.openGui((ServerPlayer) player, crate, pos);
                        return InteractionResult.SUCCESS;
                    }

                    // 2. ★追加: インベントリが空かどうかのチェック
                    // AtomicBooleanを使用してlambda式の中から値を変更できるようにする
                    AtomicBoolean isEmpty = new AtomicBoolean(true);
                    crate.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {
                        for (int i = 0; i < handler.getSlots(); i++) {
                            if (!handler.getStackInSlot(i).isEmpty()) {
                                isEmpty.set(false); // アイテムが見つかったら空ではない
                                break;
                            }
                        }
                    });

                    // 空の場合はロックを解除して開く
                    if (isEmpty.get()) {
                        crate.setUnlocked(true); // 永続的にロック解除
                        // メッセージは出さずに自然に開く、あるいは「空のためロック解除」と出しても良い
                        // player.displayClientMessage(new TextComponent("§a空のコンテナ: ロックが解除されました"), true);
                        NetworkHooks.openGui((ServerPlayer) player, crate, pos);
                        level.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
                        return InteractionResult.SUCCESS;
                    }

                    // 3. データパッドによる解除試行
                    if (tryUnlockWithPad(player.getItemInHand(hand), pos, crate)) {
                        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0f, 1.0f);
                        player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent("message.wave_motion_gun_mod.crate.unlocked"), true);
                        NetworkHooks.openGui((ServerPlayer) player, crate, pos);
                        level.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
                    } else {
                        level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0f, 1.0f);
                        player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent("message.wave_motion_gun_mod.crate.locked"), true);
                        return InteractionResult.FAIL;
                    }
                } else {
                    // 既にロック解除済みの場合
                    NetworkHooks.openGui((ServerPlayer) player, crate, pos);
                    level.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * データパッドを持っていれば、座標に関係なく消費してロックを解除する
     */
    private boolean tryUnlockWithPad(ItemStack stack, BlockPos cratePos, SupplyCrateBlockEntity crate) {
        // アイテムが CoordinateDatapadItem かどうかだけチェック
        if (!stack.isEmpty() && stack.getItem() instanceof CoordinateDatapadItem) {
            // NBTタグ(座標データ)の有無確認は任意ですが、念のためデータが入っているものに限定する場合
            // 単に「アイテムの種類」だけで判定するなら以下の通りです

            stack.shrink(1);         // アイテム消費
            crate.setUnlocked(true); // ロック解除設定
            return true;             // 成功
        }
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SupplyCrateBlockEntity crate) {
                // 落下中（isFalling）の場合は中身をばら撒かない
                if (!crate.isFalling) {
                    crate.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {
                        for (int i = 0; i < handler.getSlots(); i++) {
                            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i));
                        }
                    });
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}