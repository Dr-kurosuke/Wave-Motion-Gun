package com.example.wave_motion_gun.menu;

import com.example.wave_motion_gun.init.MenuInit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess; // ★追加
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class SupplyCrateMenu extends AbstractContainerMenu {
    private final BlockEntity blockEntity;

    // クライアント側コンストラクタ
    public SupplyCrateMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, playerInv.player.level.getBlockEntity(extraData.readBlockPos()));
    }

    // サーバー側コンストラクタ
    public SupplyCrateMenu(int containerId, Inventory playerInv, BlockEntity entity) {
        super(MenuInit.SUPPLY_CRATE_MENU.get(), containerId);
        // クライアント側でBEが未同期のままGUIが開かれるとentityがnullになりうるためガードする
        this.blockEntity = entity;

        // BlockEntityのインベントリ機能を取得してスロット配置 (3x3)
        if (this.blockEntity != null) {
            this.blockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 3; col++) {
                        // ディスペンサーGUIに合わせた配置 (x:62, y:17 が左上)
                        this.addSlot(new SlotItemHandler(handler, col + row * 3, 62 + col * 18, 17 + row * 18));
                    }
                }
            });
        }

        // プレイヤーのインベントリ (メイン)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // プレイヤーのインベントリ (ホットバー)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.blockEntity == null || this.blockEntity.getLevel() == null) return false;
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, blockEntity.getBlockState().getBlock());
    }

    // Shiftクリック時の挙動
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack sourceStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            sourceStack = slotStack.copy();

            if (index < 9) {
                // コンテナ -> プレイヤーインベントリ
                if (!this.moveItemStackTo(slotStack, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // プレイヤーインベントリ -> コンテナ
                if (!this.moveItemStackTo(slotStack, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == sourceStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
        }
        return sourceStack;
    }
}