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
        if (this.level().isClientSide) return;

        // シートブロックがなくなったり、別のブロックになったら消滅
        BlockState state = this.level().getBlockState(this.blockPosition());
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

    /**
     * ブロックの向き(facing)を、船の回転を考慮したワールド座標系のyawに変換する。
     *
     * facing はシップヤード座標系の向きなので、facing.toYRot() をそのまま使うと
     * 「ワールドの絶対方位」になり、船が回頭している分だけ着席時の向きがずれる。
     * 方向ベクトルに直して VSCompat.toWorldDirection を通してから yaw に戻す。
     *
     * 船に載っていない(またはVS2未導入の)場合、toWorldDirection は入力をそのまま返すため
     * 結果は facing.toYRot() と数値的に一致する(SOUTH=0/WEST=90/NORTH=180/EAST=270)。
     */
    private float worldYawOf(Direction facing, BlockPos pos) {
        net.minecraft.world.phys.Vec3 dir =
                new net.minecraft.world.phys.Vec3(facing.getStepX(), 0, facing.getStepZ());
        net.minecraft.world.phys.Vec3 worldDir = com.example.wave_motion_gun.compat.VSCompat
                .toWorldDirection(this.level(), dir, net.minecraft.world.phys.Vec3.atCenterOf(pos));
        // 水平成分が消える(船が真上を向く等)場合は従来の値にフォールバック
        if (worldDir.x * worldDir.x + worldDir.z * worldDir.z < 1.0e-6) return facing.toYRot();
        // yaw の定義: 視線ベクトル = (-sin(yaw), *, cos(yaw))
        return (float) Math.toDegrees(net.minecraft.util.Mth.atan2(-worldDir.x, worldDir.z));
    }

    // プレイヤーが乗った瞬間に向きと視点を固定する処理
    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);

        BlockPos pos = this.blockPosition();
        BlockState state = this.level().getBlockState(pos);

        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
            float yaw = worldYawOf(facing, pos);

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
    @Override public Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() { return new ClientboundAddEntityPacket(this); }
}