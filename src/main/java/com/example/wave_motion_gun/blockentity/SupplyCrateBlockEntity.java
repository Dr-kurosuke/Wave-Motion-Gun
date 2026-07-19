package com.example.wave_motion_gun.blockentity;

import com.example.wave_motion_gun.init.BlockEntityInit;
import com.example.wave_motion_gun.menu.SupplyCrateMenu;
import com.example.wave_motion_gun.utils.QuestionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SupplyCrateBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> itemHandler);

    private int rewardRank = 0;
    private boolean lootGenerated = false;
    private boolean isUnlocked = false;

    // ★追加: 落下中フラグ (NBT保存不要、一時的な状態管理用)
    public boolean isFalling = false;

    public SupplyCrateBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.SUPPLY_CRATE_BE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.wave_motion_gun_mod.supply_crate");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SupplyCrateMenu(containerId, playerInventory, this);
    }

    public void setRewardRank(int rank) {
        this.rewardRank = rank;
        if (this.level != null && !this.level.isClientSide && !lootGenerated) {
            generateLoot();
        }
        this.setChanged();
    }

    public boolean isUnlocked() {
        return this.isUnlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.isUnlocked = unlocked;
        this.setChanged();
    }

    private void generateLoot() {
        lootGenerated = true;
        int groupId = rewardRank + 1;
        if (groupId < 1) groupId = 1;
        if (groupId > 5) groupId = 5;

        int chainIndex = QuestionManager.getRandomChainIndex(groupId);
        QuestionManager.QuestionChain chain = QuestionManager.getChain(chainIndex);

        if (chain == null || chain.rewards == null || chain.rewards.isEmpty()) return;

        List<ItemStack> sourceRewards = new ArrayList<>(chain.rewards);
        Collections.shuffle(sourceRewards);

        Random rand = new Random();
        int countToGenerate = 3 + rand.nextInt(7);

        List<ItemStack> itemsToAdd = new ArrayList<>();

        for (ItemStack stack : sourceRewards) {
            if (itemsToAdd.size() >= countToGenerate) break;
            itemsToAdd.add(stack.copy());
        }

        while (itemsToAdd.size() < countToGenerate) {
            ItemStack randomReward = sourceRewards.get(rand.nextInt(sourceRewards.size()));
            itemsToAdd.add(randomReward.copy());
        }

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 9; i++) slots.add(i);
        Collections.shuffle(slots);

        for (int i = 0; i < itemsToAdd.size(); i++) {
            if (i < slots.size()) {
                this.itemHandler.setStackInSlot(slots.get(i), itemsToAdd.get(i));
            }
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("RewardRank", rewardRank);
        tag.putBoolean("LootGenerated", lootGenerated);
        tag.putBoolean("IsUnlocked", isUnlocked);
        tag.put("Inventory", itemHandler.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.rewardRank = tag.getInt("RewardRank");
        this.lootGenerated = tag.getBoolean("LootGenerated");
        if (tag.contains("IsUnlocked")) {
            this.isUnlocked = tag.getBoolean("IsUnlocked");
        }
        if (tag.contains("Inventory")) {
            itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        }
    }
}