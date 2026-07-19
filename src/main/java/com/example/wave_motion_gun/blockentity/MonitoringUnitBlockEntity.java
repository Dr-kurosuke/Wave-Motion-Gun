package com.example.wave_motion_gun.blockentity;

import com.example.wave_motion_gun.init.BlockEntityInit;
import com.example.wave_motion_gun.init.BlockInit;
import com.example.wave_motion_gun.menu.MonitoringUnitMenu;
import com.example.wave_motion_gun.utils.FrequencyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class MonitoringUnitBlockEntity extends BlockEntity implements MenuProvider {

    public int frequency = 0;

    // パラメータ設定
    public int damageValue = 100;
    public boolean isDestructive = false;
    public int innerColor = 0xFFFFFF; // 白
    public int outerColor = 0x3399FF; // 水色
    // --- 【追加】セーフティ解除設定 ---
    public boolean isSafetyDisabled = false;

    // 構造スキャン結果キャッシュ
    private int cachedBarrelCount = 0;
    private int cachedTachyonCount = 0;
    private boolean cachedHasAdjuster = false;
    private boolean cachedHasChamber = false;

    protected final ContainerData data;

    public MonitoringUnitBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.MONITORING_UNIT_BE.get(), pos, state);

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> MonitoringUnitBlockEntity.this.frequency;
                    case 1 -> MonitoringUnitBlockEntity.this.damageValue;
                    case 2 -> MonitoringUnitBlockEntity.this.isDestructive ? 1 : 0;
                    case 3 -> MonitoringUnitBlockEntity.this.innerColor;
                    case 4 -> MonitoringUnitBlockEntity.this.outerColor;
                    // --- GUI同期用構造データ ---
                    case 5 -> MonitoringUnitBlockEntity.this.cachedBarrelCount;
                    case 6 -> MonitoringUnitBlockEntity.this.cachedTachyonCount;
                    case 7 -> MonitoringUnitBlockEntity.this.cachedHasAdjuster ? 1 : 0;
                    case 8 -> MonitoringUnitBlockEntity.this.cachedHasChamber ? 1 : 0;
                    // --- 【追加】セーフティ ---
                    case 9 -> MonitoringUnitBlockEntity.this.isSafetyDisabled ? 1 : 0;
                    default -> 0;
                };
            }
            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> MonitoringUnitBlockEntity.this.setFrequency(value);
                    case 1 -> MonitoringUnitBlockEntity.this.damageValue = value;
                    case 2 -> MonitoringUnitBlockEntity.this.isDestructive = (value != 0);
                    case 3 -> MonitoringUnitBlockEntity.this.innerColor = value;
                    case 4 -> MonitoringUnitBlockEntity.this.outerColor = value;
                    // 5-8 Read Only
                    case 9 -> MonitoringUnitBlockEntity.this.isSafetyDisabled = (value != 0);
                }
                if (index != 0) {
                    setChanged();
                }
            }
            @Override
            public int getCount() {
                // Freq(1) + Params(4) + Struct(4) + Safety(1) = 10
                return 10;
            }
        };
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            FrequencyManager.register(this.frequency, this);
        }
    }

    // 【追加】チャンクアンロード時に確実に登録解除する
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && !level.isClientSide) {
            FrequencyManager.unregister(this.frequency, this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            FrequencyManager.unregister(this.frequency, this);
        }
    }

    public void setFrequency(int newFreq) {
        if (this.frequency != newFreq) {
            if (level != null && !level.isClientSide) {
                FrequencyManager.updateFrequency(this.frequency, newFreq, this);
            }
            this.frequency = newFreq;
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    public void activate(int signalType) {
        activate(signalType, null);
    }

    /** 発射者(実績付与用)を指定して信号を処理する。firerはnull可 */
    public void activate(int signalType, @Nullable ServerPlayer firer) {
        // 【負荷対策】重い構造スキャン(scanStructure/getNearbyCannon)は発射信号(signalType==3)の時のみ実行する。
        // モード信号(0-2)は周期的に届くため、毎回スキャンするとサーバー負荷が大きい。
        if (signalType == 3) {
            if (level != null && !level.isClientSide) scanStructure();

            WaveEnergyStorageBlockEntity storage = getNearbyStorage();
            WaveCannonBlockEntity cannon = getNearbyCannon();

            if (cannon != null && cannon.isStructureValid()) {
                if (storage != null && storage.getEnergyStored() > 0) {
                    storage.setExecutionMode(WaveEnergyStorageBlockEntity.ExecutionMode.EXHAUST);
                }
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    cannon.attemptFire(serverLevel, cannon.getBlockPos(), cannon.getBlockState(), this, firer);
                }
            }
        } else {
            // モード伝搬は隣接6方向のストレージ探索のみで完結する（軽量）
            WaveEnergyStorageBlockEntity storage = getNearbyStorage();
            if (storage != null) {
                int modeIndex = Math.max(0, Math.min(signalType, 2));
                WaveEnergyStorageBlockEntity.ExecutionMode mode = WaveEnergyStorageBlockEntity.ExecutionMode.values()[modeIndex];
                if (storage.getExecutionMode() != mode) {
                    storage.setExecutionMode(mode);
                }
            }
        }
    }

    public void onStorageOverheat() {
        FrequencyManager.feedbackOverheat(this.frequency);
    }

    private boolean isBlock(BlockState state, net.minecraftforge.registries.RegistryObject<net.minecraft.world.level.block.Block> blockReg) {
        return blockReg != null && blockReg.isPresent() && state.is(blockReg.get());
    }

    private Direction getFacing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return null;
    }

    private boolean checkAxis(BlockState state, Direction.Axis axis) {
        Direction dir = getFacing(state);
        if (dir != null) {
            return dir.getAxis() == axis;
        }
        return false;
    }

    public void scanStructure() {
        if (level == null) return;

        cachedBarrelCount = 0;
        cachedTachyonCount = 0;
        cachedHasAdjuster = false;
        cachedHasChamber = false;

        WaveEnergyStorageBlockEntity storage = getNearbyStorage();
        if (storage == null) return;

        BlockState storageState = storage.getBlockState();
        Direction facing = getFacing(storageState);
        if (facing == null) return;

        Direction.Axis axis = facing.getAxis();
        BlockPos currentPos = storage.getBlockPos().relative(facing);

        int safety = 0;
        while (safety < 10) {
            BlockState s = level.getBlockState(currentPos);

            if (isBlock(s, BlockInit.BARREL_UNIT) || isBlock(s, BlockInit.WAVE_CANNON)) {
                break;
            }

            boolean isOption = false;
            if (isBlock(s, BlockInit.TACHYON_PARTICLE_COMPRESSOR)) {
                if (checkAxis(s, axis)) cachedTachyonCount++;
                isOption = true;
            } else if (isBlock(s, BlockInit.WAVELENGTH_ADJUSTER)) {
                if (checkAxis(s, axis)) cachedHasAdjuster = true;
                isOption = true;
            } else if (isBlock(s, BlockInit.WAVE_CHAMBER)) {
                if (checkAxis(s, axis)) cachedHasChamber = true;
                isOption = true;
            }

            if (isOption) {
                currentPos = currentPos.relative(facing);
                safety++;
            } else {
                return;
            }
        }
        if (cachedTachyonCount > 2) cachedTachyonCount = 2;

        int barrels = 0;
        while (barrels < 6) {
            BlockState s = level.getBlockState(currentPos);
            if (isBlock(s, BlockInit.BARREL_UNIT)) {
                if (!checkAxis(s, axis)) return;
                barrels++;
                currentPos = currentPos.relative(facing);
            } else {
                break;
            }
        }
        cachedBarrelCount = barrels;
    }

    public int getRangeLimit() {
        if (cachedBarrelCount == 0) return 10;
        return cachedBarrelCount * 50;
    }

    /** タキオン増幅機の数に応じたダメージ上限(サーバー権威。GUIスライダー上限と同じ式) */
    public int getMaxDamageLimit() {
        if (cachedTachyonCount == 0) return 10;
        if (cachedTachyonCount == 1) return 50;
        return 100;
    }

    public int getRadiusLimit() {
        return cachedBarrelCount * 3 + 2;
    }

    public void tryOpenGui(Player player) {
        scanStructure();
        WaveCannonBlockEntity cannon = getNearbyCannon();
        if (cannon != null && cannon.isStructureValid()) {
            NetworkHooks.openGui((ServerPlayer) player, this, worldPosition);
        } else {
            player.displayClientMessage(new TranslatableComponent("message.wave_motion_gun_mod.error.invalid_structure_gui"), true);
        }
    }

    public WaveCannonBlockEntity getNearbyCannon() {
        if (this.level == null) return null;
        WaveEnergyStorageBlockEntity storage = getNearbyStorage();
        if (storage == null) return null;

        BlockState storageState = storage.getBlockState();
        Direction facing = getFacing(storageState);
        if (facing == null) return null;

        BlockPos scanPos = storage.getBlockPos().relative(facing);

        int safety = 0;
        while (safety < 20) {
            BlockEntity be = level.getBlockEntity(scanPos);
            if (be instanceof WaveCannonBlockEntity cannon) {
                return cannon;
            }
            BlockState s = level.getBlockState(scanPos);
            if (isBlock(s, BlockInit.TACHYON_PARTICLE_COMPRESSOR) ||
                    isBlock(s, BlockInit.WAVELENGTH_ADJUSTER) ||
                    isBlock(s, BlockInit.WAVE_CHAMBER) ||
                    isBlock(s, BlockInit.BARREL_UNIT)) {
                scanPos = scanPos.relative(facing);
            } else {
                break;
            }
            safety++;
        }
        return null;
    }

    public long getNearbyEnergy() {
        WaveEnergyStorageBlockEntity storage = getNearbyStorage();
        return (storage != null) ? storage.getEnergyStored() : 0;
    }

    public long getNearbyMaxEnergy() {
        return WaveEnergyStorageBlockEntity.getCapacity();
    }

    public WaveEnergyStorageBlockEntity getNearbyStorage() {
        if (this.level == null) return null;
        for (Direction dir : Direction.values()) {
            BlockEntity be = level.getBlockEntity(worldPosition.relative(dir));
            if (be instanceof WaveEnergyStorageBlockEntity storage) return storage;
        }
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Frequency", frequency);
        tag.putInt("DamageValue", damageValue);
        tag.putBoolean("IsDestructive", isDestructive);
        tag.putInt("InnerColor", innerColor);
        tag.putInt("OuterColor", outerColor);
        tag.putBoolean("IsSafetyDisabled", isSafetyDisabled);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.frequency = tag.getInt("Frequency");
        if (tag.contains("DamageValue")) this.damageValue = tag.getInt("DamageValue");
        if (tag.contains("IsDestructive")) this.isDestructive = tag.getBoolean("IsDestructive");
        if (tag.contains("InnerColor")) this.innerColor = tag.getInt("InnerColor");
        if (tag.contains("OuterColor")) this.outerColor = tag.getInt("OuterColor");
        if (tag.contains("IsSafetyDisabled")) this.isSafetyDisabled = tag.getBoolean("IsSafetyDisabled");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return new TranslatableComponent("container.wave_motion_gun_mod.monitoring_unit");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MonitoringUnitMenu(id, inv, this, this.data);
    }
}