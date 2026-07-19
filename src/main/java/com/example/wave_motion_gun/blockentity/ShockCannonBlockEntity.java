package com.example.wave_motion_gun.blockentity;

import com.example.wave_motion_gun.block.ShockCannonBarrelBlock;
import com.example.wave_motion_gun.entity.ShockCannonBeam;
import com.example.wave_motion_gun.entity.Type3ShellEntity;
import com.example.wave_motion_gun.entity.WaveCartridgeEntity;
import com.example.wave_motion_gun.init.BlockEntityInit;
import com.example.wave_motion_gun.init.EntityInit;
import com.example.wave_motion_gun.init.ItemInit;
import com.example.wave_motion_gun.init.SoundInit;
import com.example.wave_motion_gun.menu.ShockCannonMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// 衝突判定用
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

public class ShockCannonBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() == ItemInit.SHOCK_CANNON_SHELL.get() ||
                    stack.getItem() == ItemInit.TYPE_3_SHELL.get() ||
                    stack.getItem() == ItemInit.WAVE_CARTRIDGE.get();
        }
    };

    private final LazyOptional<IItemHandler> handlerOptional = LazyOptional.of(() -> itemHandler);
    private int cooldown = 0;
    private int colorMode = 0; // 0:Green, 1:Red, 2:Blue

    // ■ 追加: ディレイ関連
    private int fireDelay = 0;
    private int fireTimer = 0;

    // ■ 追加: 発射者のUUID (実績付与用。ディレイ発射でも発射者を追跡できるようUUIDで保持。NBT保存はしない)
    @Nullable
    private java.util.UUID pendingFirerId = null;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0: return ShockCannonBlockEntity.this.colorMode;
                case 1: return ShockCannonBlockEntity.this.fireDelay;
                default: return 0;
            }
        }
        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0: ShockCannonBlockEntity.this.colorMode = value; break;
                case 1: ShockCannonBlockEntity.this.fireDelay = value; break;
            }
        }
        @Override
        public int getCount() { return 2; } // サイズを2に変更
    };

    public ShockCannonBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.SHOCK_CANNON_BE.get(), pos, state);
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, ShockCannonBlockEntity be) {
        if (be.cooldown > 0) {
            be.cooldown--;
        }

        // ■ 追加: ディレイタイマー処理
        if (!level.isClientSide && be.fireTimer > 0) {
            be.fireTimer--;
            if (be.fireTimer == 0) {
                be.executeFire();
            }
        }
    }

    // トリガー時の処理（信号受信 or GUIボタン）
    public void attemptFire() {
        attemptFire(null);
    }

    // 発射者(実績付与用)を指定して発射を試みる
    public void attemptFire(@Nullable net.minecraft.server.level.ServerPlayer firer) {
        if (level == null || level.isClientSide) return;
        if (this.cooldown > 0) return;
        if (this.fireTimer > 0) return; // 既にカウントダウン中

        // 発射者を記録 (ディレイ発射時にも参照される。firerがnullなら記録もクリア)
        this.pendingFirerId = (firer != null) ? firer.getUUID() : null;

        // 発射条件チェック (NGならエラー音)
        FireContext ctx = validateFireCondition(true);
        if (ctx == null) return;

        if (this.fireDelay <= 0) {
            // 即時発射
            performFire(ctx);
        } else {
            // タイマー開始
            this.fireTimer = this.fireDelay;
            // カウントダウン開始音などを鳴らしたい場合はここで
        }
    }

    // タイマー0になった時の処理
    private void executeFire() {
        if (level == null || level.isClientSide) return;

        // 再度条件チェック (待機中に状況が変わった可能性があるため)
        // ここでNGだった場合もエラー音を鳴らす
        FireContext ctx = validateFireCondition(true);
        if (ctx != null) {
            performFire(ctx);
        }
    }

    // 発射に必要な情報をまとめたクラス
    private record FireContext(Direction fireDirection, int barrelCount) {}

    // VS2船上ではブロック座標(シップヤード)だと音が届かないため、ワールド座標で鳴らす
    private void playSoundHere(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        net.minecraft.world.phys.Vec3 p = com.example.wave_motion_gun.compat.VSCompat.worldCenterOf(level, worldPosition);
        level.playSound(null, p.x, p.y, p.z, sound, SoundSource.BLOCKS, volume, pitch);
    }

    // 発射条件をチェックし、OKならコンテキストを返す。NGならnull（必要に応じて音再生）
    private FireContext validateFireCondition(boolean playSoundOnFail) {
        ItemStack ammo = itemHandler.getStackInSlot(0);
        if (ammo.isEmpty()) {
            if (playSoundOnFail) playSoundHere(net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, 1.0F, 1.2F);
            return null;
        }

        BlockState myState = this.getBlockState();
        Direction myFacing = myState.getValue(BlockStateProperties.FACING);
        Direction.Axis myAxis = myFacing.getAxis();
        Direction dirBack = myFacing.getOpposite();
        int barrelsBack = countBarrels(dirBack, myAxis);
        Direction dirFront = myFacing;
        int barrelsFront = countBarrels(dirFront, myAxis);

        Direction fireDirection = null;
        int barrelCount = 0;

        if (barrelsBack > 0) {
            fireDirection = dirBack;
            barrelCount = barrelsBack;
        } else if (barrelsFront > 0) {
            fireDirection = dirFront;
            barrelCount = barrelsFront;
        } else {
            if (playSoundOnFail) playSoundHere(net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, 1.0F, 0.5F);
            return null;
        }

        if (barrelCount > 6) {
            if (playSoundOnFail) playSoundHere(net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, 1.0F, 0.5F);
            return null;
        }

        return new FireContext(fireDirection, barrelCount);
    }

    private int countBarrels(Direction direction, Direction.Axis validAxis) {
        int count = 0;
        for (int i = 1; i <= 7; i++) {
            BlockPos checkPos = this.worldPosition.relative(direction, i);
            BlockState checkState = level.getBlockState(checkPos);
            if (isValidBarrel(checkState, validAxis)) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    private boolean isValidBarrel(BlockState state, Direction.Axis baseAxis) {
        if (!(state.getBlock() instanceof ShockCannonBarrelBlock)) return false;
        Direction barrelFacing = state.getValue(BlockStateProperties.FACING);
        return barrelFacing.getAxis() == baseAxis;
    }

    private void performFire(FireContext ctx) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        Direction direction = ctx.fireDirection;
        int offsetBlocks = ctx.barrelCount;

        double offset = 0.6D + (double) offsetBlocks;
        double x = worldPosition.getX() + 0.5D + direction.getStepX() * offset;
        double y = worldPosition.getY() + 0.5D + direction.getStepY() * offset;
        double z = worldPosition.getZ() + 0.5D + direction.getStepZ() * offset;

        BlockPos muzzlePos = BlockPos.containing(x, y, z);
        BlockState stateAtMuzzle = level.getBlockState(muzzlePos);

        // ■ 閉塞チェック
        if (!stateAtMuzzle.isAir()) {
            VoxelShape blockShape = stateAtMuzzle.getCollisionShape(level, muzzlePos, CollisionContext.empty());
            if (!blockShape.isEmpty()) {
                double beamW = 0.5D;
                double beamH = 0.5D;
                AABB beamBox = new AABB(
                        x - beamW / 2.0D, y - beamH / 2.0D, z - beamW / 2.0D,
                        x + beamW / 2.0D, y + beamH / 2.0D, z + beamW / 2.0D
                );

                AABB localBeamBox = beamBox.move(-muzzlePos.getX(), -muzzlePos.getY(), -muzzlePos.getZ());

                if (Shapes.joinIsNotEmpty(blockShape, Shapes.create(localBeamBox), BooleanOp.AND)) {
                    playSoundHere(net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, 1.0F, 0.5F);
                    // VS2船上ではプレイヤーはワールド座標側にいるため、変換後の中心で検索する
                    net.minecraft.world.phys.Vec3 center = com.example.wave_motion_gun.compat.VSCompat.worldCenterOf(level, worldPosition);
                    level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(BlockPos.containing(center)).inflate(10)).forEach(p ->
                            p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.wave_motion_gun_mod.shock.obstructed"), true));
                    return;
                }
            }
        }

        // --- 発射処理 ---
        // 弾薬消費
        ItemStack ammo = itemHandler.extractItem(0, 1, false);
        this.cooldown = 60; // クールダウン設定

        SoundEvent fireSound;
        if (ammo.getItem() == ItemInit.SHOCK_CANNON_SHELL.get()) {
            fireSound = SoundInit.WAVE_MOTION_GUN_SHOCK_CANNON.get();
        } else {
            fireSound = SoundInit.WAVE_MOTION_GUN_SHELL.get();
        }
        playSoundHere(fireSound, 2.0F, 1.0F);

        // --- エンティティ生成 ---
        // VS2船上ではシップヤード座標のままだと見た目の砲口に出現しないため、
        // 生成位置と方向をワールド座標系へ変換してから使う
        net.minecraft.world.phys.Vec3 muzzle = new net.minecraft.world.phys.Vec3(x, y, z);
        net.minecraft.world.phys.Vec3 dirVec = new net.minecraft.world.phys.Vec3(
                direction.getStepX(), direction.getStepY(), direction.getStepZ());
        net.minecraft.world.phys.Vec3 worldMuzzle = com.example.wave_motion_gun.compat.VSCompat.toWorldPos(level, muzzle);
        net.minecraft.world.phys.Vec3 worldDir = com.example.wave_motion_gun.compat.VSCompat.toWorldDirection(level, dirVec, muzzle);

        if (ammo.getItem() == ItemInit.SHOCK_CANNON_SHELL.get()) {
            // 1. エネルギー弾
            ShockCannonBeam beam = new ShockCannonBeam(EntityType.FIREBALL, level);
            beam.setPos(worldMuzzle.x, worldMuzzle.y, worldMuzzle.z);

            double speed = 2.5D;
            beam.xPower = worldDir.x * speed;
            beam.yPower = worldDir.y * speed;
            beam.zPower = worldDir.z * speed;

            int outerColor = 0x00FF00;
            if (this.colorMode == 1) outerColor = 0xFF0000;
            if (this.colorMode == 2) outerColor = 0x00AAFF;

            beam.setCustomParams(150, 5.0F);
            beam.setAdditionalParams(25, 0xFFFFFF, outerColor);

            level.addFreshEntity(beam);

        } else if (ammo.getItem() == ItemInit.TYPE_3_SHELL.get()) {
            // 2. 三式弾
            Type3ShellEntity shell = new Type3ShellEntity(EntityInit.TYPE_3_SHELL_ENTITY.get(), level);
            shell.setPos(worldMuzzle.x, worldMuzzle.y, worldMuzzle.z);

            double speed = 3.0D;
            shell.setDeltaMovement(worldDir.x * speed, worldDir.y * speed, worldDir.z * speed);
            shell.xPower = worldDir.x * 0.1D;
            shell.yPower = worldDir.y * 0.1D;
            shell.zPower = worldDir.z * 0.1D;

            level.addFreshEntity(shell);

        } else if (ammo.getItem() == ItemInit.WAVE_CARTRIDGE.get()) {
            // 3. 波動カートリッジ弾
            WaveCartridgeEntity shell = new WaveCartridgeEntity(EntityInit.WAVE_CARTRIDGE_ENTITY.get(), level);
            shell.setPos(worldMuzzle.x, worldMuzzle.y, worldMuzzle.z);

            double speed = 2.5D;
            shell.setDeltaMovement(worldDir.x * speed, worldDir.y * speed, worldDir.z * speed);
            shell.xPower = worldDir.x * 0.1D;
            shell.yPower = worldDir.y * 0.1D;
            shell.zPower = worldDir.z * 0.1D;

            level.addFreshEntity(shell);
        }

        // --- 実績付与 (発射成功時のみ) ---
        if (this.pendingFirerId != null) {
            net.minecraft.server.level.ServerPlayer firer =
                    serverLevel.getServer().getPlayerList().getPlayer(this.pendingFirerId);
            if (firer != null) {
                com.example.wave_motion_gun.utils.AdvancementHelper.grant(firer, "fire_shock_cannon");
                if (ammo.getItem() == ItemInit.TYPE_3_SHELL.get()) {
                    com.example.wave_motion_gun.utils.AdvancementHelper.grant(firer, "fire_type3");
                } else if (ammo.getItem() == ItemInit.WAVE_CARTRIDGE.get()) {
                    com.example.wave_motion_gun.utils.AdvancementHelper.grant(firer, "fire_cartridge");
                }
            }
            this.pendingFirerId = null;
        }
    }

    public void setColorMode(int mode) {
        this.colorMode = mode;
        this.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setFireDelay(int delay) {
        this.fireDelay = Math.max(0, delay);
        this.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        if (itemHandler.getSlots() != 1) itemHandler.setSize(1);
        cooldown = tag.getInt("Cooldown");
        this.colorMode = tag.getInt("ColorMode");
        this.fireDelay = tag.getInt("FireDelay");
        this.fireTimer = tag.getInt("FireTimer");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putInt("Cooldown", cooldown);
        tag.putInt("ColorMode", this.colorMode);
        tag.putInt("FireDelay", this.fireDelay);
        tag.putInt("FireTimer", this.fireTimer);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.wave_motion_gun_mod.shock_cannon");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ShockCannonMenu(id, inventory, this, this.data);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return handlerOptional.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handlerOptional.invalidate();
    }
}