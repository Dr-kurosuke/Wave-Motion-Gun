package com.example.wave_motion_gun.block;

import net.minecraft.network.chat.Component;

import com.example.wave_motion_gun.entity.MessengerEntity;
import com.example.wave_motion_gun.init.EntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public class CommunicationBeaconBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final boolean debug = false;

    public CommunicationBeaconBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            // VS2の船上ではブロックがシップヤード座標にあるため、エンティティを扱う処理は
            // すべてワールド座標へ変換した基準点で行う。素のposを使うと、レディが遠方の
            // シップヤードに湧いてプレイヤーから見えず、以後の32ブロック判定も必ず失敗する。
            net.minecraft.world.phys.Vec3 worldCenter =
                    com.example.wave_motion_gun.compat.VSCompat.worldCenterOf(level, pos);

            // 21 = 従来の new AABB(pos).inflate(10) と同じ一辺 (pos基準で±10 + 自ブロック1)
            if (!level.getEntitiesOfClass(MessengerEntity.class,
                    net.minecraft.world.phys.AABB.ofSize(worldCenter, 21, 21, 21)).isEmpty()) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.wave_motion_gun_mod.beacon.busy"), true);
                return InteractionResult.FAIL;
            }

            if (debug) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.wave_motion_gun_mod.beacon.debug"), true);
            } else {
                long timeOfDay = level.getDayTime() % 24000;
                boolean isStarryTime = timeOfDay >= 13000 && timeOfDay <= 23000;

                if (!isStarryTime) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.wave_motion_gun_mod.beacon.no_stars"), true);
                    return InteractionResult.FAIL;
                }

                // ★修正: 日付制限を廃止し、到着待ちフラグによる制限に変更
                String waitingKey = "WaveMotionGun_IsWaitingDrop";
                String timeKey = "WaveMotionGun_DropStartTime";
                CompoundTag playerData = player.getPersistentData();
                long currentTime = level.getGameTime();

                if (playerData.getBoolean(waitingKey)) {
                    // 安全策: 3分(3600tick)以上経過していたら強制解除して許可 (MessengerEntity側と同じロジック)
                    if (playerData.contains(timeKey)) {
                        long startTime = playerData.getLong(timeKey);
                        if (currentTime - startTime <= 3600) {
                            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.wave_motion_gun_mod.beacon.wait_drop"), true);
                            return InteractionResult.FAIL;
                        }
                        // タイムアウト時はフラグを解除して通す
                        playerData.putBoolean(waitingKey, false);
                    } else {
                        // 時間記録がない場合も許可
                        playerData.putBoolean(waitingKey, false);
                    }
                }

            }

            level.playSound(null, worldCenter.x, worldCenter.y, worldCenter.z,
                    SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);

            MessengerEntity lady = new MessengerEntity(EntityInit.MESSENGER.get(), level);
            // 「ブロックの1.5上」はオフセットを付けてから変換する。worldCenter.y+1.5 だと
            // ワールドの上になり、船が傾いているとビーコンの真上からずれてしまう。
            net.minecraft.world.phys.Vec3 spawnPos = com.example.wave_motion_gun.compat.VSCompat
                    .toWorldPos(level, net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0, 1.5, 0));
            lady.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            lady.lookAt(player, 180, 180);
            level.addFreshEntity(lady);

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (random.nextInt(5) == 0) {
            // パーティクルはワールド座標系に描かれるため、船上ブロックでは変換が必要
            // (素のposだと演出が遠方のシップヤードに出て見えない)。
            // 高さのオフセットも変換前に足す(船が傾いても真上に出すため)。
            net.minecraft.world.phys.Vec3 c = com.example.wave_motion_gun.compat.VSCompat
                    .toWorldPos(level, net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0, 1.0, 0));
            level.addParticle(ParticleTypes.END_ROD, c.x, c.y, c.z, 0, 0.1, 0);
        }
    }
}