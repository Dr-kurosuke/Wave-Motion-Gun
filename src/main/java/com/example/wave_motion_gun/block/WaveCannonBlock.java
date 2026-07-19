package com.example.wave_motion_gun.block;

import com.example.wave_motion_gun.blockentity.WaveCannonBlockEntity;
import com.example.wave_motion_gun.init.BlockEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker; // 【追加】Tickerインターフェース
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable; // 【追加】Null許容アノテーション

public class WaveCannonBlock extends DirectionalBlock implements EntityBlock {

    public WaveCannonBlock(Properties properties) {
        super(properties);
        // TRIGGEREDプロパティを削除し、FACINGのみ設定
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // TRIGGEREDを削除
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    // BEとの紐付け
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaveCannonBlockEntity(pos, state);
    }

    // --- 【追加】クライアント側での演出用Ticker登録 ---
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // クライアント側（描画側）でのみ動作させる
        if (level.isClientSide) {
            return createTickerHelper(type, BlockEntityInit.WAVE_CANNON_BE.get(), WaveCannonBlockEntity::clientTick);
        }
        return null;
    }

    // --- 【追加】Ticker型チェック用ヘルパー ---
    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> expectedType, BlockEntityType<E> targetType, BlockEntityTicker<E> ticker) {
        return targetType == expectedType ? (BlockEntityTicker<A>) ticker : null;
    }

    // neighborChanged, tick メソッドを削除し、レッドストーン入力を無効化
}