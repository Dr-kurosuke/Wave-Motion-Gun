package com.example.wave_motion_gun.world.data;

import com.example.wave_motion_gun.ExampleMod;
import com.example.wave_motion_gun.entity.SupplyMeteorEntity;
import com.example.wave_motion_gun.init.EntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class SupplyDropManager extends SavedData {
    private static final String DATA_NAME = ExampleMod.MODID + "_supply_drops";
    private final List<DropEvent> activeEvents = new ArrayList<>();
    public static final int EVENT_DURATION = 3600; // 3分

    private static final UUID TICKET_OWNER = UUID.fromString("d8d8d8d8-d8d8-d8d8-d8d8-d8d8d8d8d8d8");

    // メテオの出発地点: 着弾地点から水平に+80ブロックずらし、演出として斜めに落下させる
    private static final double METEOR_START_HORIZONTAL_OFFSET = 80.0;
    // メテオの出発高度: 建築上限から100ブロック下 (雲より上だが遠くから視認できる高さ)
    private static final double METEOR_START_HEIGHT_BELOW_MAX = 100.0;

    public static SupplyDropManager get(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.getDataStorage().computeIfAbsent(SupplyDropManager::load, SupplyDropManager::new, DATA_NAME);
        }
        throw new RuntimeException("SupplyDropManager accessed on client");
    }

    public SupplyDropManager() {}

    public void triggerDrop(ServerLevel level, BlockPos targetPos, int rank, UUID ownerId) {
        DropEvent event = new DropEvent(targetPos, rank, EVENT_DURATION, ownerId);
        activeEvents.add(event);
        forceLoadChunks(level, event, true);
        this.setDirty();
    }

    public void tick(ServerLevel level) {
        if (activeEvents.isEmpty()) return;

        Iterator<DropEvent> iterator = activeEvents.iterator();
        boolean changed = false;

        while (iterator.hasNext()) {
            DropEvent event = iterator.next();
            event.timer--;

            if (event.timer <= 0) {
                spawnMeteor(level, event);
                forceLoadChunks(level, event, false);

                iterator.remove();
                changed = true;
            }
        }

        // タイマーを毎tick減算しているため、イベントが存在する限り常にdirtyにする
        // (これがないとセーブ時にカウントダウンが保存されず、再起動で巻き戻る)
        this.setDirty();
    }

    private void forceLoadChunks(ServerLevel level, DropEvent event, boolean load) {
        BlockPos target = event.targetPos;
        Vec3 start = getStartPos(target, level.getMaxBuildHeight());

        ChunkPos targetChunk = new ChunkPos(target);
        ChunkPos startChunk = new ChunkPos(new BlockPos(start));

        ForgeChunkManager.forceChunk(level, ExampleMod.MODID, TICKET_OWNER, targetChunk.x, targetChunk.z, load, true);
        ForgeChunkManager.forceChunk(level, ExampleMod.MODID, TICKET_OWNER, startChunk.x, startChunk.z, load, true);
    }

    private Vec3 getStartPos(BlockPos target, int maxHeight) {
        double startX = target.getX() + METEOR_START_HORIZONTAL_OFFSET;
        double startZ = target.getZ() + METEOR_START_HORIZONTAL_OFFSET;
        double startY = maxHeight - METEOR_START_HEIGHT_BELOW_MAX;
        return new Vec3(startX, startY, startZ);
    }

    private void spawnMeteor(ServerLevel level, DropEvent event) {
        Vec3 startPos = getStartPos(event.targetPos, level.getMaxBuildHeight());
        SupplyMeteorEntity meteor = new SupplyMeteorEntity(EntityInit.SUPPLY_METEOR.get(), level);
        meteor.initialize(startPos, event.targetPos, event.rank, event.ownerId);
        level.addFreshEntity(meteor);
    }

    public static SupplyDropManager load(CompoundTag tag) {
        SupplyDropManager manager = new SupplyDropManager();
        ListTag list = tag.getList("Events", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag eventTag = (CompoundTag) t;
            BlockPos pos = new BlockPos(eventTag.getInt("X"), eventTag.getInt("Y"), eventTag.getInt("Z"));
            int rank = eventTag.getInt("Rank");
            int timer = eventTag.getInt("Timer");
            UUID ownerId = eventTag.hasUUID("Owner") ? eventTag.getUUID("Owner") : null;
            manager.activeEvents.add(new DropEvent(pos, rank, timer, ownerId));
        }
        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (DropEvent event : activeEvents) {
            CompoundTag eventTag = new CompoundTag();
            eventTag.putInt("X", event.targetPos.getX());
            eventTag.putInt("Y", event.targetPos.getY());
            eventTag.putInt("Z", event.targetPos.getZ());
            eventTag.putInt("Rank", event.rank);
            eventTag.putInt("Timer", event.timer);
            if (event.ownerId != null) {
                eventTag.putUUID("Owner", event.ownerId);
            }
            list.add(eventTag);
        }
        tag.put("Events", list);
        return tag;
    }

    private static class DropEvent {
        final BlockPos targetPos;
        final int rank;
        int timer;
        final UUID ownerId;
        DropEvent(BlockPos targetPos, int rank, int timer, UUID ownerId) {
            this.targetPos = targetPos;
            this.rank = rank;
            this.timer = timer;
            this.ownerId = ownerId;
        }
    }
}