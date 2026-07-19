package com.example.wave_motion_gun.menu;

import com.example.wave_motion_gun.blockentity.MonitoringUnitBlockEntity;
import com.example.wave_motion_gun.blockentity.TriggerUnitBlockEntity;
import com.example.wave_motion_gun.blockentity.WaveCannonBlockEntity;
import com.example.wave_motion_gun.blockentity.WaveEnergyStorageBlockEntity;
import com.example.wave_motion_gun.init.BlockInit;
import com.example.wave_motion_gun.init.MenuInit;
import com.example.wave_motion_gun.utils.FrequencyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Set;

public class TriggerUnitMenu extends AbstractContainerMenu {
    private final TriggerUnitBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final ContainerData data;

    // リモートデータ
    private final DataSlot remoteRange = DataSlot.standalone();
    private final DataSlot remoteRadius = DataSlot.standalone();

    // 【修正】16bit切り捨て対策のため、64bit値を4つの16bitスロットに分割
    private final DataSlot remoteEnergy0 = DataSlot.standalone();
    private final DataSlot remoteEnergy1 = DataSlot.standalone();
    private final DataSlot remoteEnergy2 = DataSlot.standalone();
    private final DataSlot remoteEnergy3 = DataSlot.standalone();

    private final DataSlot remoteMaxEnergy0 = DataSlot.standalone();
    private final DataSlot remoteMaxEnergy1 = DataSlot.standalone();
    private final DataSlot remoteMaxEnergy2 = DataSlot.standalone();
    private final DataSlot remoteMaxEnergy3 = DataSlot.standalone();

    private final DataSlot targetX = DataSlot.standalone();
    private final DataSlot targetY = DataSlot.standalone();
    private final DataSlot targetZ = DataSlot.standalone();
    private final DataSlot hasTarget = DataSlot.standalone();
    private final DataSlot remoteOverheat = DataSlot.standalone();

    // 安全装置ステート (0 or 1)
    private final DataSlot remoteCircuit = DataSlot.standalone();
    private final DataSlot remoteValves = DataSlot.standalone();
    private final DataSlot remoteInlet = DataSlot.standalone();
    private final DataSlot remoteOutlet = DataSlot.standalone();

    public TriggerUnitMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level.getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
    }

    public TriggerUnitMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(MenuInit.TRIGGER_UNIT_MENU.get(), id);
        // 【修正】クライアント側でBEが未同期の場合にNPE/ClassCastExceptionにならないようガードする
        if (entity instanceof TriggerUnitBlockEntity be && be.getLevel() != null) {
            this.blockEntity = be;
            this.levelAccess = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        } else {
            this.blockEntity = null;
            this.levelAccess = ContainerLevelAccess.NULL;
        }
        this.data = data;

        checkContainerDataCount(data, 4);
        this.addDataSlots(data);

        this.addDataSlot(targetX);
        this.addDataSlot(targetY);
        this.addDataSlot(targetZ);
        this.addDataSlot(hasTarget);
        this.addDataSlot(remoteRange);
        this.addDataSlot(remoteRadius);

        // 分割したエネルギースロットを登録
        this.addDataSlot(remoteEnergy0);
        this.addDataSlot(remoteEnergy1);
        this.addDataSlot(remoteEnergy2);
        this.addDataSlot(remoteEnergy3);

        this.addDataSlot(remoteMaxEnergy0);
        this.addDataSlot(remoteMaxEnergy1);
        this.addDataSlot(remoteMaxEnergy2);
        this.addDataSlot(remoteMaxEnergy3);

        this.addDataSlot(remoteOverheat);

        this.addDataSlot(remoteCircuit);
        this.addDataSlot(remoteValves);
        this.addDataSlot(remoteInlet);
        this.addDataSlot(remoteOutlet);

        if (!inv.player.level.isClientSide && this.blockEntity != null) {
            updateRemoteData();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, BlockInit.TRIGGER_UNIT.get());
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
            updateRemoteData();
        }
        super.broadcastChanges();
    }

    private void updateRemoteData() {
        int range = 0;
        int radius = 0;
        int tX = 0, tY = 0, tZ = 0;
        int found = 0;

        long energy = 0;
        long maxEnergy = 1;
        int overheat = 0;

        int rCircuit = 0;
        int rValves = 0;
        int rInlet = 0;
        int rOutlet = 0;

        // 統合された targetFrequency を使用して MonitoringUnit を取得
        Set<MonitoringUnitBlockEntity> monitors = FrequencyManager.getReceivers(blockEntity.getLevel(), blockEntity.targetFrequency);

        if (monitors != null) {
            for (MonitoringUnitBlockEntity monitor : monitors) {
                if (monitor == null || monitor.isRemoved() || monitor.getLevel() == null) continue;
                if (!monitor.getLevel().isLoaded(monitor.getBlockPos())) continue;

                // --- キャノン情報の取得 ---
                WaveCannonBlockEntity cannon = monitor.getNearbyCannon();
                if (cannon != null) {
                    BlockPos cPos = cannon.getBlockPos();
                    tX = cPos.getX();
                    tY = cPos.getY();
                    tZ = cPos.getZ();
                    found = 1;
                    range = cannon.currentRange;
                    radius = cannon.currentRadius;
                }

                // --- ストレージ情報の取得 ---
                WaveEnergyStorageBlockEntity storage = monitor.getNearbyStorage();
                if (storage != null) {
                    energy = storage.getEnergyStored();
                    maxEnergy = storage.getMaxEnergyStored();
                    overheat = Math.max(overheat, storage.overheatTimer);

                    rCircuit = storage.isCircuitToWMG() ? 1 : 0;
                    rValves = storage.isEmergencyValves() ? 1 : 0;
                    rInlet = storage.isInletSafety() ? 1 : 0;
                    rOutlet = storage.isOutletSafety() ? 1 : 0;
                }

                // 【修正】キャノンとストレージの両方が見つかった時のみループ終了
                // (ORだと片方しか見つかっていない時点で打ち切られ、もう一方の情報が取れない)
                if (found == 1 && maxEnergy > 1) break;
            }
        }

        remoteRange.set(range);
        remoteRadius.set(radius);
        targetX.set(tX);
        targetY.set(tY);
        targetZ.set(tZ);
        hasTarget.set(found);

        // 【修正】64bit値を16bitごとに分割してセット (パケットでの桁あふれ防止)
        remoteEnergy0.set((int) (energy & 0xFFFF));
        remoteEnergy1.set((int) ((energy >> 16) & 0xFFFF));
        remoteEnergy2.set((int) ((energy >> 32) & 0xFFFF));
        remoteEnergy3.set((int) ((energy >> 48) & 0xFFFF));

        remoteMaxEnergy0.set((int) (maxEnergy & 0xFFFF));
        remoteMaxEnergy1.set((int) ((maxEnergy >> 16) & 0xFFFF));
        remoteMaxEnergy2.set((int) ((maxEnergy >> 32) & 0xFFFF));
        remoteMaxEnergy3.set((int) ((maxEnergy >> 48) & 0xFFFF));

        remoteOverheat.set(overheat);

        remoteCircuit.set(rCircuit);
        remoteValves.set(rValves);
        remoteInlet.set(rInlet);
        remoteOutlet.set(rOutlet);
    }

    // --- Getters ---
    // 【修正】ContainerDataの欠番(旧index1)を詰めたため、インデックスを0..3に変更
    public int getTargetFreq() { return this.data.get(0); }
    public int getStorageMode() { return this.data.get(1); }
    public int getLocalOverheat() { return this.data.get(2); }
    public int getLightLevel() { return this.data.get(3); }

    public int getRemoteRange() { return remoteRange.get(); }
    public int getRemoteRadius() { return remoteRadius.get(); }

    // 【修正】4つのスロットから64bit値を復元
    public long getRemoteEnergy() {
        return ((long) (remoteEnergy3.get() & 0xFFFF) << 48) |
                ((long) (remoteEnergy2.get() & 0xFFFF) << 32) |
                ((long) (remoteEnergy1.get() & 0xFFFF) << 16) |
                ((long) (remoteEnergy0.get() & 0xFFFF));
    }

    public long getRemoteMaxEnergy() {
        return ((long) (remoteMaxEnergy3.get() & 0xFFFF) << 48) |
                ((long) (remoteMaxEnergy2.get() & 0xFFFF) << 32) |
                ((long) (remoteMaxEnergy1.get() & 0xFFFF) << 16) |
                ((long) (remoteMaxEnergy0.get() & 0xFFFF));
    }

    public int getRemoteOverheat() { return remoteOverheat.get(); }

    public boolean getRemoteCircuit() { return remoteCircuit.get() == 1; }
    public boolean getRemoteValves() { return remoteValves.get() == 1; }
    public boolean getRemoteInlet() { return remoteInlet.get() == 1; }
    public boolean getRemoteOutlet() { return remoteOutlet.get() == 1; }

    public boolean hasRemoteTarget() { return hasTarget.get() == 1; }
    public BlockPos getTargetPos() { return new BlockPos(targetX.get(), targetY.get(), targetZ.get()); }
    public TriggerUnitBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }
}