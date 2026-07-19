package com.example.wave_motion_gun.entity; // パッケージを blockentity から entity に変更

import com.example.wave_motion_gun.block.SeatBlock;
import com.example.wave_motion_gun.init.EntityInit; // 自作のEntityInitをインポート
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SeatEntity extends Entity {

    /**
     * 対応するシートブロックの座標。
     *
     * VS2の船上ではブロックはシップヤード座標にあり、エンティティはワールド座標に置く必要がある。
     * つまり座席エンティティの位置とブロックの座標は一致しなくなるため、
     * 「どのブロックの座席か」を blockPosition() から復元できない。そこで明示的に保持する。
     * ブロック参照(getBlockState)には必ずこちらを使うこと。
     *
     * クライアント側でも向き固定に使うため SynchedEntityData で同期する。
     */
    private static final net.minecraft.network.syncher.EntityDataAccessor<BlockPos> SEAT_BLOCK_POS =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    SeatEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BLOCK_POS);

    // 登録用コンストラクタ (EntityInitで使用)
    public SeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true; // 物理演算無効
    }

    // 実際にスポーンさせる時のコンストラクタ
    public SeatEntity(Level level, BlockPos pos) {
        this(EntityInit.SEAT.get(), level);
        this.entityData.set(SEAT_BLOCK_POS, pos.immutable());
        this.moveToSeatBlock();
    }

    /** 参照すべきシートブロックの座標(シップヤード座標系)。 */
    public BlockPos getSeatBlockPos() {
        BlockPos stored = this.entityData.get(SEAT_BLOCK_POS);
        // 旧セーブ由来などで未設定の場合は、船に載っていない前提で自身の座標にフォールバック
        return BlockPos.ZERO.equals(stored) ? this.blockPosition() : stored;
    }

    /**
     * シートブロックの現在のワールド座標へ自分を移動する。
     *
     * 船は動くので毎tick追従させる必要がある。これをしないと、船が進んでも
     * 座席(と乗っているプレイヤー)がその場に取り残される。
     */
    private void moveToSeatBlock() {
        BlockPos pos = this.getSeatBlockPos();
        // 0.001はずり落ち防止。ブロック下端を基準にするため中心から-0.499する。
        // オフセットは変換前に足す(船が傾いていても座面が正しい向きに来るように)。
        net.minecraft.world.phys.Vec3 p = com.example.wave_motion_gun.compat.VSCompat.toWorldPos(
                this.level(), net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0, -0.499, 0));
        this.setPos(p.x, p.y, p.z);
    }

    @Override
    public void tick() {
        if (this.level().isClientSide) return;

        // シートブロックがなくなったり、別のブロックになったら消滅
        // (ブロック参照はシップヤード座標のまま行うのが正しい)
        BlockState state = this.level().getBlockState(this.getSeatBlockPos());
        if (!(state.getBlock() instanceof SeatBlock)) {
            this.discard();
            return;
        }

        // 誰も乗っていなければ消滅
        if (this.getPassengers().isEmpty()) {
            this.discard();
            return;
        }

        // 船が動いた場合に追従する
        this.moveToSeatBlock();
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.20D; // 座る高さの調整
    }

    // プレイヤーが乗った瞬間に向きと視点を固定する処理
    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);

        // ブロック参照はシップヤード座標のまま行う (自身の座標はワールド座標なので使えない)
        BlockPos pos = this.getSeatBlockPos();
        BlockState state = this.level().getBlockState(pos);

        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
            float yaw = facing.toYRot();

            // シートの向きを設定
            this.setYRot(yaw);

            // プレイヤーの体の向き
            passenger.setYRot(yaw);

            // プレイヤーの頭(カメラ)の向き
            passenger.setYHeadRot(yaw);

            // 上下の視点をリセット
            passenger.setXRot(0);

            // 描画補間のズレを防ぐために前回の値も更新
            passenger.yRotO = yaw;
            passenger.xRotO = 0;
            if (passenger instanceof LivingEntity living) {
                living.yHeadRotO = yaw;
                living.yBodyRot = yaw;
                living.yBodyRotO = yaw;
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SEAT_BLOCK_POS, BlockPos.ZERO);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("SeatBlockX")) {
            this.entityData.set(SEAT_BLOCK_POS, new BlockPos(
                    compound.getInt("SeatBlockX"), compound.getInt("SeatBlockY"), compound.getInt("SeatBlockZ")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        BlockPos pos = this.entityData.get(SEAT_BLOCK_POS);
        compound.putInt("SeatBlockX", pos.getX());
        compound.putInt("SeatBlockY", pos.getY());
        compound.putInt("SeatBlockZ", pos.getZ());
    }
    @Override public Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() { return new ClientboundAddEntityPacket(this); }
}