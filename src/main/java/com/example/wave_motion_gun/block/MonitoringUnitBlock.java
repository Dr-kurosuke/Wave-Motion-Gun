package com.example.wave_motion_gun.block;

import com.example.wave_motion_gun.blockentity.MonitoringUnitBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

// RotatedPillarBlock（3軸）から Block（汎用）に変更
public class MonitoringUnitBlock extends Block implements EntityBlock {
    // 6方向を管理するためのプロパティ
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public MonitoringUnitBlock(Properties properties) {
        super(properties);
        // デフォルトを北向きに設定
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // 1. ブロックにFACINGプロパティを登録する
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // 2. 設置時にプレイヤーの向きに合わせて「正面」を決める
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // プレイヤーの方を向くように設置（ピストンのような挙動）
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation") // 【追加】Mojangの仕様上、オーバーライド必須だが非推奨扱いのため警告を抑制
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MonitoringUnitBlockEntity monitorBE) {
                monitorBE.tryOpenGui(player);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MonitoringUnitBlockEntity(pos, state);
    }
}