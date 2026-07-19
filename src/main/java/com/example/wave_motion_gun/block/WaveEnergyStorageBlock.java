package com.example.wave_motion_gun.block;

import com.example.wave_motion_gun.blockentity.WaveEnergyStorageBlockEntity;
import com.example.wave_motion_gun.init.BlockEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

// RotatedPillarBlock から Block へ変更
public class WaveEnergyStorageBlock extends Block implements EntityBlock {
    // 6方向を管理するプロパティを定義
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public WaveEnergyStorageBlock(Properties properties) {
        super(properties);
        // デフォルトを北向きに設定
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // ブロックにFACINGプロパティを登録
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // 設置時にプレイヤーの向きに合わせてFACINGを決定
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // プレイヤーが見ている方向（上下含む）に真っ直ぐ配置
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaveEnergyStorageBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == BlockEntityInit.WAVE_ENERGY_STORAGE_BE.get()
                ? (level1, pos, state1, be) -> WaveEnergyStorageBlockEntity.tick(level1, pos, state1, (WaveEnergyStorageBlockEntity)be)
                : null;
    }

}