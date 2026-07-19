package com.example.wave_motion_gun.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class WaveMachineBlock extends Block {
    // 向きプロパティ
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public WaveMachineBlock(Properties properties) {
        super(properties);
        // デフォルトの向き設定
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // バレルと同じく、プレイヤーの視線方向（設置した面や向き）に合わせて向きを決定
        // context.getNearestLookingDirection() はプレイヤーが見ている方向（例：南を向いていたら南）を返します
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }
}