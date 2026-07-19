package com.example.wave_motion_gun.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class BarrelUnitBlock extends Block {
    // 1. 向き(Facing)プロパティの定義
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public BarrelUnitBlock(Properties properties) {
        super(properties);
        // 2. デフォルトの向きを北に設定
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // 3. ブロックにFACINGプロパティを登録
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // 4. 設置時にプレイヤーの向きに合わせてFACINGを決定
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // プレイヤーが見ている方向（上下含む）を向くように設置
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }
}