package com.example.wave_motion_gun.menu;

import com.example.wave_motion_gun.blockentity.MonitoringUnitBlockEntity;
import com.example.wave_motion_gun.blockentity.WaveCannonBlockEntity;
import com.example.wave_motion_gun.init.BlockInit;
import com.example.wave_motion_gun.init.MenuInit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class MonitoringUnitMenu extends AbstractContainerMenu {
    public final MonitoringUnitBlockEntity blockEntity;
    private final ContainerData data;
    private final List<DataSlot> trackedSlots = new ArrayList<>();
    // 【追加】実績付与用: メニューを開いたプレイヤー (サーバー側でのみServerPlayer)
    private final Player player;
    // 【追加】構造完成実績の重複スキャン防止フラグ
    private boolean structureAdvGranted = false;

    public MonitoringUnitMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level.getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(10));
    }

    public MonitoringUnitMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(MenuInit.MONITORING_UNIT_MENU.get(), id);
        // 【修正】クライアント側でBEが未同期の場合にClassCastExceptionにならないようガードする
        this.blockEntity = (entity instanceof MonitoringUnitBlockEntity monitor) ? monitor : null;
        this.data = data;
        this.player = inv.player;

        addDataSlots(data);

        // 【修正】12個のスロットを確保 (Energy: 4, MaxEnergy: 4, Range/Rad: 4)
        // 16bit(short)同期のパケット仕様に対応するため、64bit値を4分割して送受信する
        for (int i = 0; i < 12; i++) {
            trackedSlots.add(this.addDataSlot(DataSlot.standalone()));
        }

        if (!inv.player.level.isClientSide && this.blockEntity != null) {
            refreshTrackedData();
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        return false;
    }

    private void refreshTrackedData() {
        long currentEnergy = blockEntity.getNearbyEnergy();
        long maxEnergy = blockEntity.getNearbyMaxEnergy();

        // 【修正】64bitを16bitずつ4つのスロットに分割してセット
        trackedSlots.get(0).set((int) (currentEnergy & 0xFFFF));
        trackedSlots.get(1).set((int) ((currentEnergy >> 16) & 0xFFFF));
        trackedSlots.get(2).set((int) ((currentEnergy >> 32) & 0xFFFF));
        trackedSlots.get(3).set((int) ((currentEnergy >> 48) & 0xFFFF));

        trackedSlots.get(4).set((int) (maxEnergy & 0xFFFF));
        trackedSlots.get(5).set((int) ((maxEnergy >> 16) & 0xFFFF));
        trackedSlots.get(6).set((int) ((maxEnergy >> 32) & 0xFFFF));
        trackedSlots.get(7).set((int) ((maxEnergy >> 48) & 0xFFFF));

        WaveCannonBlockEntity cannon = blockEntity.getNearbyCannon();
        if (cannon != null) {
            trackedSlots.get(8).set(cannon.currentRange);
            trackedSlots.get(10).set(cannon.currentRadius);
            trackedSlots.get(9).set(blockEntity.getRangeLimit());
            trackedSlots.get(11).set(blockEntity.getRadiusLimit());
        } else {
            trackedSlots.get(8).set(150);
            trackedSlots.get(10).set(4);
            trackedSlots.get(9).set(100);
            trackedSlots.get(11).set(5);
        }
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity != null) {
            refreshTrackedData();

            // 実績: 波動砲の構造完成 (サーバー側のみ。付与後は再スキャンしない)
            if (!structureAdvGranted && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                WaveCannonBlockEntity cannon = blockEntity.getNearbyCannon();
                if (cannon != null && cannon.isStructureValid()) {
                    com.example.wave_motion_gun.utils.AdvancementHelper.grant(serverPlayer, "structure_complete");
                    structureAdvGranted = true;
                }
            }
        }
        super.broadcastChanges();
    }

    public int getFrequency() { return this.data.get(0); }
    public int getBarrelCount() { return this.data.get(5); }
    public int getTachyonCount() { return this.data.get(6); }
    public boolean hasAdjuster() { return this.data.get(7) == 1; }
    public boolean hasChamber() { return this.data.get(8) == 1; }
    public boolean isSafetyDisabled() { return this.data.get(9) == 1; }

    public int getMaxDamageLimit() {
        int t = getTachyonCount();
        if (t == 0) return 10;
        if (t == 1) return 50;
        return 100;
    }

    // 【修正】16bit×4スロットから64bit値を復元
    public long getStoredEnergy() {
        return ((long) (trackedSlots.get(3).get() & 0xFFFF) << 48) |
                ((long) (trackedSlots.get(2).get() & 0xFFFF) << 32) |
                ((long) (trackedSlots.get(1).get() & 0xFFFF) << 16) |
                ((long) (trackedSlots.get(0).get() & 0xFFFF));
    }

    public long getMaxEnergy() {
        return ((long) (trackedSlots.get(7).get() & 0xFFFF) << 48) |
                ((long) (trackedSlots.get(6).get() & 0xFFFF) << 32) |
                ((long) (trackedSlots.get(5).get() & 0xFFFF) << 16) |
                ((long) (trackedSlots.get(4).get() & 0xFFFF));
    }

    public int getCurrentRange() { return trackedSlots.get(8).get(); }
    public int getMaxRangeLimit() { return Math.max(1, trackedSlots.get(9).get()); }
    public int getCurrentRadius() { return trackedSlots.get(10).get(); }
    public int getMaxRadiusLimit() { return Math.max(1, trackedSlots.get(11).get()); }

    // 【修正】他メニューと同様に距離・ブロック存在チェックを行う
    @Override
    public boolean stillValid(Player p) {
        if (this.blockEntity == null || this.blockEntity.getLevel() == null) return false;
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), p, BlockInit.MONITORING_UNIT.get());
    }

    @Override public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }
}