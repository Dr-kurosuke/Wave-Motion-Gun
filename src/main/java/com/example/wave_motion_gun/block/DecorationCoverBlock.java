package com.example.wave_motion_gun.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class DecorationCoverBlock extends Block {
    // 6方向（上下含む）に向きを変えられるようにする
    public static final DirectionProperty FACING = DirectionalBlock.FACING;

    // 各方向ごとの「額縁型」の形状をキャッシュするマップ
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        // --- 形状の定義 (厚さ2ピクセル, 縁の幅2ピクセル) ---
        // 基本となる北向き(NORTH)の形状定義: 背面(Z=14~16)に張り付くプレート
        // box(x1, y1, z1, x2, y2, z2)
        VoxelShape northShape = Shapes.or(
                Block.box(0, 0, 14, 16, 2, 16),   // 下の縁
                Block.box(0, 14, 14, 16, 16, 16), // 上の縁
                Block.box(0, 2, 14, 2, 14, 16),   // 左の縁
                Block.box(14, 2, 14, 16, 14, 16)  // 右の縁
        );
        // 中央(2, 2, 14) ~ (14, 14, 16) は空洞になります

        SHAPES.put(Direction.NORTH, northShape);
        SHAPES.put(Direction.SOUTH, calculateShapes(Direction.SOUTH, northShape));
        SHAPES.put(Direction.EAST, calculateShapes(Direction.EAST, northShape));
        SHAPES.put(Direction.WEST, calculateShapes(Direction.WEST, northShape));
        SHAPES.put(Direction.UP, calculateShapes(Direction.UP, northShape));
        SHAPES.put(Direction.DOWN, calculateShapes(Direction.DOWN, northShape));
    }

    public DecorationCoverBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // --- 形状判定 ---

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // 向いている方向に応じた「額縁型」の形状を返す
        // 中央が空洞なので、そこへのクリックは突き抜けて奥のブロックに届きます
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // 衝突判定も見た目通りにする
        return SHAPES.get(state.getValue(FACING));
    }

    // --- 設置・プロパティ関連 ---

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // クリックした面に対して貼り付くように向きを決定
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    // --- 形状計算用ヘルパー ---
    // (手動で座標計算するのは大変なので、回転処理で生成します)
    private static VoxelShape calculateShapes(Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{shape, Shapes.empty()};

        int times = 0;
        if (to == Direction.SOUTH) times = 2;
        else if (to == Direction.WEST) times = 1;
        else if (to == Direction.EAST) times = 3;

        // Y軸回転
        for (int i = 0; i < times; i++) {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = Shapes.or(buffer[1], Shapes.box(1-maxZ, minY, minX, 1-minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = Shapes.empty();
        }

        // 上下向きの調整
        if (to == Direction.UP) {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = Shapes.or(buffer[1], Shapes.box(minX, 1-maxZ, minY, maxX, 1-minZ, maxY)));
            buffer[0] = buffer[1];
        } else if (to == Direction.DOWN) {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = Shapes.or(buffer[1], Shapes.box(minX, minZ, 1-maxY, maxX, maxZ, 1-minY)));
            buffer[0] = buffer[1];
        }

        return buffer[0];
    }
}