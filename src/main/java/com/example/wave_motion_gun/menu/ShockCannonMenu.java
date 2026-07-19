package com.example.wave_motion_gun.menu;

import com.example.wave_motion_gun.block.ShockCannonBlock;
import com.example.wave_motion_gun.init.BlockInit;
import com.example.wave_motion_gun.blockentity.ShockCannonBlockEntity;
import com.example.wave_motion_gun.init.MenuInit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class ShockCannonMenu extends AbstractContainerMenu {
    public final ShockCannonBlockEntity blockEntity;
    private final ContainerData data;

    public ShockCannonMenu(int windowId, Inventory inv, FriendlyByteBuf extraData) {
        // コンテナデータサイズを 2 に変更
        this(windowId, inv, inv.player.level.getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(2));
    }

    public ShockCannonMenu(int windowId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(MenuInit.SHOCK_CANNON_MENU.get(), windowId);
        // 【修正】クライアント側でBEが未同期の場合にClassCastExceptionにならないようガードする
        this.blockEntity = (entity instanceof ShockCannonBlockEntity cannon) ? cannon : null;
        this.data = data;

        checkContainerSize(inv, 1);

        if (this.blockEntity != null) {
            this.blockEntity.getCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {
                // スロットを左側に移動 (x=25, y=35)
                this.addSlot(new SlotItemHandler(handler, 0, 25, 35));
            });
        }

        addDataSlots(data);
        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    public int getColorMode() {
        return this.data.get(0);
    }

    // ■ 追加: ディレイ値の取得
    public int getFireDelay() {
        return this.data.get(1);
    }

    public ItemStack getAmmo() {
        if (this.slots.size() > 0) {
            return this.slots.get(0).getItem();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.blockEntity == null || this.blockEntity.getLevel() == null) return false;
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, BlockInit.SHOCK_CANNON.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 1) {
                if (!this.moveItemStackTo(itemstack1, 1, 37, true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return itemstack;
    }
}