package com.example.wave_motion_gun.block;

import com.example.wave_motion_gun.entity.SeatEntity;
import com.example.wave_motion_gun.utils.VoxelUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;


import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SeatBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // --- 形状の定義 ---
    // 北向き(NORTH)を基準とする基本形状
    // 高さ: 6ピクセル (0.0D ~ 6.0D)
    // 奥行き: 正面(Z=0側)を2ピクセル削る -> 2.0D ~ 16.0D
    // これにより、ブロック手前の2ピクセル分は空洞になり、右クリックが貫通します
    private static final VoxelShape BASE_SHAPE = Block.box(0.0D, 0.0D, 5.0D, 16.0D, 7.0D, 16.0D);

    // 【最適化】getShapeは毎フレーム呼ばれるため、向きごとの回転済み形状をクラス初期化時に一度だけ計算する
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);
    static {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            SHAPES.put(dir, VoxelUtil.rotateShape(Direction.NORTH, dir, BASE_SHAPE));
        }
    }

    public SeatBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // --- 判定関連 ---

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // 向きに合わせた事前計算済みの形状を返す（視線・操作判定）
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // 物理衝突判定も視線判定と同じにする
        return this.getShape(state, level, pos, context);
    }

    // 右クリック時の処理
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // 既に座っているエンティティがいないか確認
        List<SeatEntity> seats = level.getEntitiesOfClass(SeatEntity.class, new AABB(pos));
        if (!seats.isEmpty()) {
            return InteractionResult.PASS; // 既に誰か座っていれば何もしない
        }

        // 座る処理を実行
        SeatEntity seat = new SeatEntity(level, pos);
        level.addFreshEntity(seat); // ワールドにエンティティをスポーン
        player.startRiding(seat);   // プレイヤーを乗せる

        return InteractionResult.SUCCESS;
    }

    // 設置時にプレイヤーの方を向くようにする
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    // プロパティの登録
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}