package com.example.wave_motion_gun.block;

import com.example.wave_motion_gun.blockentity.TriggerUnitBlockEntity;
import com.example.wave_motion_gun.init.BlockEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class TriggerUnitBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("light_level", 0, 15);

    // 当たり判定用の形状 (高さ2px。プレイヤーはほぼ引っかからない)
    private static final VoxelShape COLLISION_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
    // 視線・選択枠用の形状 (高さ12px)
    private static final VoxelShape OUTLINE_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);

    public TriggerUnitBlock(Properties properties) {
        // 【修正1】noOcclusion() を追加
        // これにより「透けない完全なブロック」ではないことを宣言し、描画バグを防ぎます
        super(properties.lightLevel(state -> state.getValue(LIGHT_LEVEL)).noOcclusion());

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIGHT_LEVEL, 0));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // 【修正2】Shapes.block() ではなく、高さが低い形状を返す
        // これにより、側面が「埋まっていない」と判定され、ガラス板などが接続しなくなります。
        return OUTLINE_SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // 当たり判定はさらに低い形状を使用 (視線判定より低く、乗り越えやすくする)
        return COLLISION_SHAPE;
    }

    // --- 以下変更なし ---

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIGHT_LEVEL);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.isPassenger()) {
            if (!level.isClientSide) {
                player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent("message.wave_motion_gun_mod.trigger.need_seat"), true);
            }
            return InteractionResult.SUCCESS;
        }

        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TriggerUnitBlockEntity) {
                NetworkHooks.openGui((ServerPlayer) player, (TriggerUnitBlockEntity) be, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TriggerUnitBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == BlockEntityInit.TRIGGER_UNIT_BE.get()
                ? (lvl, pos, st, be) -> TriggerUnitBlockEntity.tick(lvl, pos, st, (TriggerUnitBlockEntity) be)
                : null;
    }
}