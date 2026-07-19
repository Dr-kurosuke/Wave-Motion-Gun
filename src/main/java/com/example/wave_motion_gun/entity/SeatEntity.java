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

    // 登録用コンストラクタ (EntityInitで使用)
    public SeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true; // 物理演算無効
    }

    // 実際にスポーンさせる時のコンストラクタ
    public SeatEntity(Level level, BlockPos pos) {
        this(EntityInit.SEAT.get(), level);
        // 位置調整 (0.001はずり落ち防止)
        this.setPos(pos.getX() + 0.5, pos.getY() + 0.001, pos.getZ() + 0.5);
    }

    @Override
    public void tick() {
        if (this.level.isClientSide) return;

        // シートブロックがなくなったり、別のブロックになったら消滅
        BlockState state = this.level.getBlockState(this.blockPosition());
        if (!(state.getBlock() instanceof SeatBlock)) {
            this.discard();
            return;
        }

        // 誰も乗っていなければ消滅
        if (this.getPassengers().isEmpty()) {
            this.discard();
        }
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.20D; // 座る高さの調整
    }

    // プレイヤーが乗った瞬間に向きと視点を固定する処理
    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);

        BlockPos pos = this.blockPosition();
        BlockState state = this.level.getBlockState(pos);

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

    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag compound) {}
    @Override protected void addAdditionalSaveData(CompoundTag compound) {}
    @Override public Packet<?> getAddEntityPacket() { return new ClientboundAddEntityPacket(this); }
}