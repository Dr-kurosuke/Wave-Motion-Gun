package com.example.wave_motion_gun.blockentity;

import com.example.wave_motion_gun.init.BlockEntityInit;
import com.example.wave_motion_gun.init.BlockInit;
import com.example.wave_motion_gun.init.SoundInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.example.wave_motion_gun.blockentity.MonitoringUnitBlockEntity;

public class WaveEnergyStorageBlockEntity extends BlockEntity {

    public enum ExecutionMode {
        EXHAUST,
        CHARGE,
        STAY
    }

    private ExecutionMode currentMode = ExecutionMode.EXHAUST;

    private boolean circuitToWMG = false;
    private boolean emergencyValves = false;
    private boolean inletSafety = false;
    private boolean outletSafety = false;

    private static final long MAX_CAPACITY = 150_000_000L;
    private static final long MAX_RECEIVE = 150_000L;
    private static final long MAX_EXTRACT = 150_000_000L;

    /** 蓄電ユニットの最大容量 (150M)。外部クラスからの参照用アクセサ */
    public static long getCapacity() {
        return MAX_CAPACITY;
    }

    private static final int AUTO_EXHAUST_LIMIT = 1200;

    private final CustomEnergy energy = new CustomEnergy(this, MAX_CAPACITY, MAX_RECEIVE, MAX_EXTRACT);
    private final LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(() -> energy);

    public int overheatTimer = 0;
    private int autoExhaustTimer = 0;

    // ★追加: チャージ開始遅延用タイマー
    private int chargeDelayTimer = 0;

    // サーバー側でのループ再生制御用
    private int soundLoopTimer = 0;

    public WaveEnergyStorageBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.WAVE_ENERGY_STORAGE_BE.get(), pos, state);
    }

    // 既存のSE再生（ピッチ・音量固定）
    public void playSound(SoundEvent sound) {
        this.playSound(sound, 1.0F, 1.0F);
    }

    // 【追加】ピッチ・音量指定可能なサーバー側SE再生
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (this.level != null && !this.level.isClientSide) {
            this.level.playSound(null, this.worldPosition, sound, SoundSource.BLOCKS, volume, pitch);
        }
    }

    public void setExecutionMode(ExecutionMode mode) {
        if (this.overheatTimer > 0 && mode != ExecutionMode.EXHAUST) {
            return;
        }

        if (this.currentMode != mode) {
            if (this.currentMode == ExecutionMode.EXHAUST && mode == ExecutionMode.CHARGE) {
                playSound(SoundInit.WAVE_MOTION_GUN_FORCED_INJECTOR.get());
                this.chargeDelayTimer = 30; // ★追加: 音が終わるまで約1.5秒 (30tick) 待機設定
            } else if ((this.currentMode == ExecutionMode.CHARGE || this.currentMode == ExecutionMode.STAY)
                    && mode == ExecutionMode.EXHAUST) {
                playSound(SoundInit.WAVE_MOTION_GUN_FORCED_INJECTOR_OFF.get());
            }

            this.currentMode = mode;
            this.setChanged();
            this.updateInClient();
        }
    }

    public ExecutionMode getExecutionMode() { return this.currentMode; }
    public boolean isCircuitToWMG() { return this.circuitToWMG; }
    public boolean isEmergencyValves() { return this.emergencyValves; }
    public boolean isInletSafety() { return this.inletSafety; }
    public boolean isOutletSafety() { return this.outletSafety; }

    public void setCircuitToWMG(boolean active) {
        if (this.circuitToWMG != active) {
            this.circuitToWMG = active;
            this.setChanged();
            this.updateInClient();
        }
    }

    public void setEmergencyValves(boolean active) {
        if (this.emergencyValves != active) {
            this.emergencyValves = active;
            if (active) {
                playSound(SoundInit.WAVE_MOTION_GUN_VALVE_CLOSE.get());
            } else {
                playSound(SoundInit.WAVE_MOTION_GUN_VALVE_OPEN.get());
            }
            this.setChanged();
            this.updateInClient();
        }
    }

    public void setInletSafety(boolean active) {
        if (this.inletSafety != active) {
            this.inletSafety = active;
            playSound(SoundInit.WAVE_MOTION_GUN_SAFETY.get());
            this.setChanged();
            this.updateInClient();
        }
    }

    public void setOutletSafety(boolean active) {
        if (this.outletSafety != active) {
            this.outletSafety = active;
            playSound(SoundInit.WAVE_MOTION_GUN_SAFETY.get());
            this.setChanged();
            this.updateInClient();
        }
    }

    public void setOverheat(int ticks) {
        this.overheatTimer = ticks;
        this.setChanged();
        this.updateInClient();

        if (this.level != null && !this.level.isClientSide) {
            try {
                for (Direction dir : Direction.values()) {
                    BlockPos neighborPos = this.worldPosition.relative(dir);
                    if (this.level.isLoaded(neighborPos)) {
                        BlockEntity neighbor = this.level.getBlockEntity(neighborPos);
                        if (neighbor instanceof MonitoringUnitBlockEntity monitor) {
                            monitor.onStorageOverheat();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WaveEnergyStorageBlockEntity be) {
        // クライアント側の処理は現状なし (旧パーティクル演出は廃止済み)
        if (!level.isClientSide) {
            handleServerTick(level, pos, be);
        }
    }

    private static void handleServerTick(Level level, BlockPos pos, WaveEnergyStorageBlockEntity be) {
        try {

            // ★追加: チャージ遅延タイマーの減算処理
            // 【負荷対策】毎tickのsetChangedはやめ、タイマー完了時のみ保存マークする
            // (開始時はsetExecutionMode内でsetChanged済み)
            if (be.chargeDelayTimer > 0) {
                be.chargeDelayTimer--;
                if (be.chargeDelayTimer == 0) {
                    be.setChanged();
                }
            }

            // 1. ループ音再生管理
            handleSoundLoopServer(be);

            // 2. オーバーヒートタイマー
            // 【負荷対策】毎tickのsetChangedはやめ、タイマー完了時のみ保存マークする
            // (開始時はsetOverheat内でsetChanged済み)
            if (be.overheatTimer > 0) {
                be.overheatTimer--;
                if (be.currentMode != ExecutionMode.EXHAUST) {
                    be.setExecutionMode(ExecutionMode.EXHAUST);
                }
                if (be.overheatTimer == 0) {
                    be.setChanged();
                }
            }

            // 3. 自動排出機能
            if (be.energy.getEnergyStored() > 0) {
                // 【修正】STAYモード中はタイマーをリセットし、意図しない排出を防ぐ
                if (be.currentMode == ExecutionMode.STAY) {
                    be.autoExhaustTimer = 0;
                } else {
                    be.autoExhaustTimer++;
                    if (be.autoExhaustTimer >= AUTO_EXHAUST_LIMIT) {
                        be.setExecutionMode(ExecutionMode.EXHAUST);
                        // setOverheat が隣接モニターへの通知まで行う
                        be.setOverheat(600);
                        // 実績「機関室、応答せよ!」: 排出忘れによる自然オーバーヒート時のみ付与
                        // (発射後の冷却setOverheat(1400)では付与しない)
                        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                            com.example.wave_motion_gun.utils.AdvancementHelper.grantNearby(serverLevel, pos, 16, "overheat");
                        }
                    }
                }
            } else {
                be.autoExhaustTimer = 0;
            }

            // 4. EXHAUSTモード処理
            if (be.currentMode == ExecutionMode.EXHAUST && be.energy.getEnergyStored() > 0) {
                be.energy.extractEnergy((int) 1_000_000, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleSoundLoopServer(WaveEnergyStorageBlockEntity be) {
        long maxEnergy = be.getMaxEnergyStored();
        if (maxEnergy <= 0) return;

        long currentEnergy = be.getEnergyStored();
        double ratio = (double) currentEnergy / (double) maxEnergy;

        if (ratio >= 0.01) {
            if (be.soundLoopTimer > 0) {
                be.soundLoopTimer--;
            } else {
                float finalPitch = 0.5F + (float) (ratio * 1.0);
                float volume = 0.4F + (float) (ratio * 1.0F);

                SoundEvent soundToPlay;
                if (be.currentMode == ExecutionMode.EXHAUST) {
                    soundToPlay = SoundInit.WAVE_MOTION_GUN_EXHAUSTING.get();
                } else {
                    if (ratio >= 0.6) {
                        finalPitch = 0.5F + (float) (0.6 * 1.0);
                        soundToPlay = SoundInit.WAVE_MOTION_GUN_CRITICAL.get();
                    } else {
                        soundToPlay = SoundInit.WAVE_MOTION_GUN_CHARGING.get();
                    }
                }

                if (soundToPlay != null) {
                    be.playSound(soundToPlay, volume, finalPitch);
                }

                be.soundLoopTimer = (int) (40 / finalPitch);
                if (be.soundLoopTimer > 100) be.soundLoopTimer = 100;
            }
        } else {
            be.soundLoopTimer = 0;
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.energy.setEnergy(tag.getLong("EnergyStored"));
        if (tag.contains("ExecutionMode")) {
            this.currentMode = ExecutionMode.values()[tag.getInt("ExecutionMode")];
        }
        this.overheatTimer = tag.getInt("OverheatTimer");
        this.autoExhaustTimer = tag.getInt("AutoExhaustTimer");
        this.chargeDelayTimer = tag.getInt("ChargeDelayTimer"); // ★追加: 読み込み

        this.circuitToWMG = tag.getBoolean("CircuitToWMG");
        this.emergencyValves = tag.getBoolean("EmergencyValves");
        this.inletSafety = tag.getBoolean("InletSafety");
        this.outletSafety = tag.getBoolean("OutletSafety");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("EnergyStored", this.energy.getEnergyStoredLong());
        tag.putInt("ExecutionMode", this.currentMode.ordinal());
        tag.putInt("OverheatTimer", this.overheatTimer);
        tag.putInt("AutoExhaustTimer", this.autoExhaustTimer);
        tag.putInt("ChargeDelayTimer", this.chargeDelayTimer); // ★追加: 保存

        tag.putBoolean("CircuitToWMG", this.circuitToWMG);
        tag.putBoolean("EmergencyValves", this.emergencyValves);
        tag.putBoolean("InletSafety", this.inletSafety);
        tag.putBoolean("OutletSafety", this.outletSafety);
    }

    public void consumeEnergy(int amount) {
        this.energy.extractEnergy(amount, false);
        updateInClient();
    }

    public boolean canExtract(int amount) {
        return this.energy.getEnergyStored() >= amount;
    }

    public long getEnergyStored() { return this.energy.getEnergyStoredLong(); }
    public long getMaxEnergyStored() { return this.energy.getMaxEnergyStoredLong(); }

    public void updateInClient() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            BlockState currentState = this.level.getBlockState(this.worldPosition);
            if (currentState != null) {
                this.level.sendBlockUpdated(this.worldPosition, currentState, currentState, 3);
            }
        }
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
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyOptional.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyOptional.invalidate();
    }

    private static class CustomEnergy implements IEnergyStorage {
        private final WaveEnergyStorageBlockEntity parent;
        private long energy;
        private final long capacity;
        private final long maxReceive;
        private final long maxExtract;

        // 【負荷対策】クライアント同期の間引き用（前回同期時のエネルギー割合と時刻）
        private int lastSyncedPercent = -1;
        private long lastSyncGameTime = Long.MIN_VALUE;

        public CustomEnergy(WaveEnergyStorageBlockEntity parent, long capacity, long maxReceive, long maxExtract) {
            this.parent = parent;
            this.capacity = capacity;
            this.maxReceive = maxReceive;
            this.maxExtract = (int) maxExtract;
            this.energy = 0;
        }

        /**
         * 【負荷対策】毎tickのフルNBT同期パケット送信を抑制する。
         * 割合(%)が1以上変化した時、または前回同期から10tick以上経過した時のみ同期。
         * それ以外は保存用のダーティマーク(setChanged)のみ行う。
         */
        private void syncThrottled() {
            int percent = (capacity > 0) ? (int) (energy * 100L / capacity) : 0;
            long now = (parent.level != null) ? parent.level.getGameTime() : 0L;
            if (percent != lastSyncedPercent || now - lastSyncGameTime >= 10) {
                lastSyncedPercent = percent;
                lastSyncGameTime = now;
                parent.updateInClient();
            } else {
                parent.setChanged();
            }
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            // ★修正: 遅延タイマーが残っている場合も受け入れを拒否する
            if (parent.currentMode != ExecutionMode.CHARGE || parent.chargeDelayTimer > 0) return 0;

            long canReceive = capacity - energy;
            long maxLimited = Math.min(this.maxReceive, maxReceive);
            int energyReceived = (int) Math.min(canReceive, maxLimited);
            if (!simulate) {
                energy += energyReceived;
                if (energyReceived != 0) syncThrottled();
            }
            return energyReceived;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int energyExtracted = (int) Math.min(energy, Math.min(this.maxExtract, maxExtract));
            if (!simulate) {
                energy -= energyExtracted;
                if (energyExtracted != 0) syncThrottled();
            }
            return energyExtracted;
        }

        @Override
        public int getEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, this.energy); }
        @Override
        public int getMaxEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, this.capacity); }
        @Override
        public boolean canExtract() { return this.maxExtract > 0; }
        @Override
        public boolean canReceive() { return this.maxReceive > 0; }

        public void setEnergy(long energy) { this.energy = energy; }
        public long getEnergyStoredLong() { return this.energy; }
        public long getMaxEnergyStoredLong() { return this.capacity; }
    }
}