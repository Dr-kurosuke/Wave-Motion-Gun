package com.example.wave_motion_gun.blockentity;

import com.example.wave_motion_gun.block.TriggerUnitBlock;
import com.example.wave_motion_gun.init.BlockEntityInit;
import com.example.wave_motion_gun.menu.TriggerUnitMenu;
import com.example.wave_motion_gun.utils.FrequencyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Direction;
import java.util.Set;
import net.minecraft.util.Mth;

import com.example.wave_motion_gun.blockentity.MonitoringUnitBlockEntity;
import com.example.wave_motion_gun.blockentity.WaveEnergyStorageBlockEntity;
import com.example.wave_motion_gun.blockentity.WaveCannonBlockEntity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class TriggerUnitBlockEntity extends BlockEntity implements MenuProvider {

    public int targetFrequency = 0;
    public int storageMode = 0; // 0=EXHAUST, 1=CHARGE, 2=STAY
    public int overheatTimer = 0;
    public int autoShutdownTicks = 0;
    public int lightLevel = 0;

    public float currentChargeRatio = 0.0f;
    public float clientChargeProgress = 0.0f;

    // 【負荷対策】信号の変化検知用。MIN_VALUEは「未送信」を示す番兵値（初回tickで必ず1回送る）
    private int lastSentFreq = Integer.MIN_VALUE;
    private int lastSentMode = Integer.MIN_VALUE;
    // 【負荷対策】重い走査処理(受信機スキャン等)を20tickに1回へ間引くためのカウンタ
    private int serverTickCounter = 0;

    private Object soundInstance = null;

    protected final ContainerData data;

    public TriggerUnitBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.TRIGGER_UNIT_BE.get(), pos, state);

        // 【修正】旧index1が欠番だったため詰めた (Menu側のgetterも0..3に対応済み)
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> TriggerUnitBlockEntity.this.targetFrequency;
                    case 1 -> TriggerUnitBlockEntity.this.storageMode;
                    case 2 -> TriggerUnitBlockEntity.this.overheatTimer;
                    case 3 -> TriggerUnitBlockEntity.this.lightLevel;
                    default -> 0;
                };
            }
            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> TriggerUnitBlockEntity.this.targetFrequency = value;
                    case 1 -> TriggerUnitBlockEntity.this.storageMode = value;
                    case 2 -> TriggerUnitBlockEntity.this.overheatTimer = value;
                    case 3 -> TriggerUnitBlockEntity.this.lightLevel = value;
                }
            }
            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            FrequencyManager.registerTrigger(this.targetFrequency, this);
        }
    }

    // 【追加】チャンクアンロード時に確実に登録解除する（幽霊BEのリーク防止。MonitoringUnitと同様）
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && !level.isClientSide) {
            FrequencyManager.unregisterTrigger(this.targetFrequency, this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            FrequencyManager.unregisterTrigger(this.targetFrequency, this);
        }
        if (level != null && level.isClientSide) {
            stopSound();
        }
    }

    public void setStorageData(int newFreq, int newMode) {
        // ContainerData は 16bit で同期されるため、収まらない値はクライアントで負値に化ける
        newFreq = Mth.clamp(newFreq, 0, FrequencyManager.MAX_FREQUENCY);
        if (this.targetFrequency != newFreq) {
            if (level != null && !level.isClientSide) {
                FrequencyManager.updateTriggerFrequency(this.targetFrequency, newFreq, this);
            }
            this.targetFrequency = newFreq;
        }
        this.storageMode = newMode;
        this.setChanged();
        if (level != null) {
            BlockState currentState = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, currentState, currentState, 3);
        }
    }

    public void setLightLevel(int newLevel) {
        int clampedLevel = Math.max(0, Math.min(15, newLevel));
        if (this.lightLevel != clampedLevel) {
            this.lightLevel = clampedLevel;
            this.setChanged();
            if (level != null && !level.isClientSide) {
                BlockState state = level.getBlockState(worldPosition);
                if (state != null && state.hasProperty(TriggerUnitBlock.LIGHT_LEVEL)) {
                    level.setBlock(worldPosition, state.setValue(TriggerUnitBlock.LIGHT_LEVEL, clampedLevel), 3);
                }
            }
        }
    }

    public void setStorageMode(int mode) {
        if (this.storageMode != mode) {
            this.storageMode = mode;
            this.setChanged();
            if (level != null) {
                BlockState currentState = level.getBlockState(worldPosition);
                level.sendBlockUpdated(worldPosition, currentState, currentState, 3);
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TriggerUnitBlockEntity be) {
        if (!level.isClientSide) {
            try {
                if (be.overheatTimer > 0) {
                    be.overheatTimer--;
                    be.setChanged();
                }

                // 【負荷対策】毎tick送信をやめ、変化検知＋20tick周期の再送に変更。
                // 変化検知だけだと、後から設置/再ロードされた受信機が現在のモードを受け取れないため、
                // 低頻度(1秒毎)の再送でキープアライブを維持する。
                be.serverTickCounter++;
                boolean periodicTick = (be.serverTickCounter % 20 == 0);
                if (be.targetFrequency != be.lastSentFreq || be.storageMode != be.lastSentMode || periodicTick) {
                    FrequencyManager.sendSignal(be.targetFrequency, be.storageMode);
                    be.lastSentFreq = be.targetFrequency;
                    be.lastSentMode = be.storageMode;
                }

                // 【負荷対策】受信機の全走査を伴う処理は20tickに1回に間引く
                // (チャージ率はGUI表示用のため1秒粒度で十分)
                if (periodicTick) {
                    handleAutoShutdown(level, be);
                    be.updateChargeRatio(level);
                }

            } catch (Exception e) {
                System.err.println("[TriggerUnit] Error in tick: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            be.clientTick();
        }
    }

    private void updateChargeRatio(Level level) {
        long totalEnergy = 0;
        long totalCost = 0;
        boolean foundStorage = false;
        boolean foundCannon = false;

        Set<MonitoringUnitBlockEntity> monitors = FrequencyManager.getReceivers(this.targetFrequency);

        if (monitors != null) {
            for (MonitoringUnitBlockEntity monitor : monitors) {
                if (monitor == null || monitor.isRemoved() || monitor.getLevel() == null) continue;
                if (!monitor.getLevel().isLoaded(monitor.getBlockPos())) continue;

                WaveCannonBlockEntity cannon = monitor.getNearbyCannon();
                if (cannon != null && !foundCannon) {
                    totalCost = WaveCannonBlockEntity.calcFireCost(cannon.currentRange, cannon.currentRadius);
                    foundCannon = true;
                }

                WaveEnergyStorageBlockEntity storage = monitor.getNearbyStorage();
                if (storage != null && !foundStorage) {
                    totalEnergy = storage.getEnergyStored();
                    foundStorage = true;
                }

                if (foundCannon && foundStorage) break;
            }
        }

        float rawRatio = 0.0f;
        if (foundCannon && foundStorage && totalCost > 0) {
            rawRatio = (float) ((double) totalEnergy / totalCost);
        }

        // 実績: 充填完了(100%)・過充填(120%)。周囲のプレイヤーへ付与 (grantは冪等なので毎回呼んでも問題ない)
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            if (rawRatio >= 1.0f) {
                com.example.wave_motion_gun.utils.AdvancementHelper.grantNearby(serverLevel, worldPosition, 10, "full_charge");
            }
            if (rawRatio >= WaveCannonBlockEntity.OVERFILL_RATIO) {
                com.example.wave_motion_gun.utils.AdvancementHelper.grantNearby(serverLevel, worldPosition, 10, "overcharge");
            }
        }

        // 120%チャージ監視 (rawRatio >= OVERFILL_RATIO)
        if (this.storageMode == 1 && rawRatio >= WaveCannonBlockEntity.OVERFILL_RATIO) {
            setStorageMode(2); // STAY
        }

        float newRatio = rawRatio;
        if (newRatio > 1.0f) newRatio = 1.0f;

        if (Math.abs(this.currentChargeRatio - newRatio) > 0.001f) {
            this.currentChargeRatio = newRatio;
            this.setChanged();
            BlockState currentState = level.getBlockState(this.worldPosition);
            level.sendBlockUpdated(this.worldPosition, currentState, currentState, 3);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void clientTick() {
        this.clientChargeProgress = Mth.lerp(0.1f, this.clientChargeProgress, this.currentChargeRatio);
        if (this.clientChargeProgress < 0.0f) this.clientChargeProgress = 0.0f;
        manageSound();
    }

    @OnlyIn(Dist.CLIENT)
    private void manageSound() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        boolean shouldPlay = this.clientChargeProgress > 0.001f;

        if (shouldPlay) {
            if (soundInstance == null || ((com.example.wave_motion_gun.client.sound.TriggerUnitSound)soundInstance).isStopped()) {
                com.example.wave_motion_gun.client.sound.TriggerUnitSound newSound =
                        new com.example.wave_motion_gun.client.sound.TriggerUnitSound(this);
                mc.getSoundManager().play(newSound);
                soundInstance = newSound;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void stopSound() {
        if (soundInstance != null) {
            net.minecraft.client.Minecraft.getInstance().getSoundManager().stop((com.example.wave_motion_gun.client.sound.TriggerUnitSound)soundInstance);
            soundInstance = null;
        }
    }

    private static void handleAutoShutdown(Level level, TriggerUnitBlockEntity be) {
        Set<MonitoringUnitBlockEntity> monitors = FrequencyManager.getReceivers(be.targetFrequency);
        if (monitors == null) return;

        boolean foundStorage = false;

        for (MonitoringUnitBlockEntity monitor : monitors) {
            if (monitor == null || monitor.isRemoved() || monitor.getLevel() == null) continue;
            if (!monitor.getLevel().isLoaded(monitor.getBlockPos())) continue;

            WaveEnergyStorageBlockEntity storage = monitor.getNearbyStorage();
            if (storage != null) {
                foundStorage = true;
                // 【修正】Getterを使用
                boolean circuit = storage.isCircuitToWMG();
                boolean valves = storage.isEmergencyValves();

                boolean cond1 = circuit && !valves;
                boolean cond2 = circuit && valves && (be.storageMode == 0);

                if (cond1 || cond2) {
                    // 【負荷対策】本メソッドは20tick毎の呼び出しになったため、tick換算で+20する
                    // (閾値400tick≒20秒のタイミングをほぼ維持)
                    be.autoShutdownTicks += 20;
                    if (be.autoShutdownTicks > 400) {
                        boolean changed = false;
                        // 【修正】Setterを使用 (これによりSEや更新処理も自動で行われる)
                        if (storage.isCircuitToWMG()) { storage.setCircuitToWMG(false); changed = true; }
                        if (storage.isEmergencyValves()) { storage.setEmergencyValves(false); changed = true; }
                        if (storage.isInletSafety()) { storage.setInletSafety(false); changed = true; }
                        if (storage.isOutletSafety()) { storage.setOutletSafety(false); changed = true; }

                        // Setter内でsetChangedなどは呼ばれるため、個別のフラグ管理は不要だが、
                        // 念のためブロック更新通知等はSetter任せにする。

                        if (be.storageMode != 0) {
                            be.storageMode = 0;
                            be.setChanged();
                            BlockState currentState = level.getBlockState(be.getBlockPos());
                            level.sendBlockUpdated(be.getBlockPos(), currentState, currentState, 3);
                        }
                        be.autoShutdownTicks = 0;
                    }
                } else {
                    be.autoShutdownTicks = 0;
                }
                break;
            }
        }

        if (!foundStorage) {
            be.autoShutdownTicks = 0;
        }
    }

    public void fireCannonSignal() {
        fireCannonSignal(null);
    }

    /** 発射者(実績付与用)を指定して発射信号を送る */
    public void fireCannonSignal(@Nullable net.minecraft.server.level.ServerPlayer firer) {
        FrequencyManager.sendSignal(targetFrequency, 3, firer);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("TargetFreq", targetFrequency);
        tag.putInt("StorageMode", storageMode);
        tag.putInt("OverheatTimer", overheatTimer);
        tag.putInt("AutoShutdownTicks", autoShutdownTicks);
        tag.putInt("LightLevel", lightLevel);
        tag.putFloat("ChargeRatio", currentChargeRatio);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("TargetFreq")) {
            this.targetFrequency = tag.getInt("TargetFreq");
        } else if (tag.contains("StorageFreq")) {
            this.targetFrequency = tag.getInt("StorageFreq");
        }
        // 旧セーブに範囲外の値が残っている場合の救済 (16bit同期で負値に化けるのを防ぐ)
        this.targetFrequency = Mth.clamp(this.targetFrequency, 0, FrequencyManager.MAX_FREQUENCY);

        this.storageMode = tag.getInt("StorageMode");
        this.overheatTimer = tag.getInt("OverheatTimer");
        this.autoShutdownTicks = tag.getInt("AutoShutdownTicks");
        this.lightLevel = tag.getInt("LightLevel");
        this.currentChargeRatio = tag.getFloat("ChargeRatio");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return new net.minecraft.network.chat.TranslatableComponent("container.wave_motion_gun_mod.trigger_unit");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new TriggerUnitMenu(id, inv, this, this.data);
    }
}