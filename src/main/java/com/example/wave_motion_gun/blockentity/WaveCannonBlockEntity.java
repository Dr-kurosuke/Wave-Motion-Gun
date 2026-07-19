package com.example.wave_motion_gun.blockentity;

import com.example.wave_motion_gun.entity.WaveEnergyBall;
import com.example.wave_motion_gun.init.BlockEntityInit;
import com.example.wave_motion_gun.init.BlockInit;
import com.example.wave_motion_gun.init.SoundInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level; // 【追加】Levelクラス
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import com.mojang.math.Vector3f; // 【追加】パーティクルの色指定用

import java.util.Random; // 【追加】ランダム座標計算用

public class WaveCannonBlockEntity extends BlockEntity {
    public static final int FIRE_COST_BASE = 107_000_000;
    public static final int FIRE_COST_PER_RANGE_AND_RADIUS_SQUARED = 150;

    /** 発射コストに対する過充填(オーバーフィル)上限倍率。1.2 = 120% (double精度の計算用) */
    public static final double OVERFILL_FACTOR = 1.2;
    /** float比較用の過充填倍率 ((float) 1.2 == 1.2f なので値は同一) */
    public static final float OVERFILL_RATIO = (float) OVERFILL_FACTOR;

    /** 発射に必要な基本エネルギーコスト: range * radius^2 * 150 + 107,000,000 */
    public static long calcFireCost(int range, int radius) {
        return (long) range * (long) (radius * radius) * FIRE_COST_PER_RANGE_AND_RADIUS_SQUARED + FIRE_COST_BASE;
    }

    public int currentRange = 150;
    public int currentRadius = 4;

    // --- 【追加】クライアント演出用のフィールド ---
    public WaveEnergyStorageBlockEntity cachedStorage = null; // 接続先ストレージのキャッシュ
    private int checkStorageTimer = 0; // スキャン頻度制御用タイマー

    public WaveCannonBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.WAVE_CANNON_BE.get(), pos, state);
    }

    private static class StructureResult {
        boolean valid = false;
        int barrelCount = 0;
        int tachyonCount = 0;
        boolean hasAdjuster = false;
        boolean hasChamber = false;
    }

    // 【追加】安全なブロック判定メソッド
    private boolean isBlock(BlockState state, net.minecraftforge.registries.RegistryObject<net.minecraft.world.level.block.Block> blockReg) {
        return blockReg != null && blockReg.isPresent() && state.is(blockReg.get());
    }

    private StructureResult scanStructure() {
        StructureResult result = new StructureResult();
        if (this.level == null) return result;

        BlockState myState = level.getBlockState(worldPosition);
        if (!myState.hasProperty(BlockStateProperties.FACING)) return result;

        Direction facing = myState.getValue(BlockStateProperties.FACING);
        Direction.Axis cannonAxis = facing.getAxis();
        Direction behind = facing.getOpposite();

        BlockPos currentPos = worldPosition.relative(behind);

        // 1. バレルのスキャン
        int barrels = 0;
        while (barrels < 6) {
            BlockState state = level.getBlockState(currentPos);
            // 【修正】isBlockヘルパーを使用
            if (isBlock(state, BlockInit.BARREL_UNIT)) {
                if (!isValidAxis(state, cannonAxis)) return result;
                currentPos = currentPos.relative(behind);
                barrels++;
            } else {
                break;
            }
        }
        result.barrelCount = barrels;

        // 2. オプションブロック群のスキャン
        int safetyCounter = 0;
        while (safetyCounter < 10) {
            BlockState state = level.getBlockState(currentPos);

            // 【修正】isBlockヘルパーを使用
            if (isBlock(state, BlockInit.WAVE_ENERGY_STORAGE)) {
                break;
            }

            boolean isOption = false;
            // 【修正】isBlockヘルパーを使用
            if (isBlock(state, BlockInit.TACHYON_PARTICLE_COMPRESSOR)) {
                if (!isValidAxis(state, cannonAxis)) return result;
                result.tachyonCount++;
                isOption = true;
            } else if (isBlock(state, BlockInit.WAVELENGTH_ADJUSTER)) {
                if (!isValidAxis(state, cannonAxis)) return result;
                result.hasAdjuster = true;
                isOption = true;
            } else if (isBlock(state, BlockInit.WAVE_CHAMBER)) {
                if (!isValidAxis(state, cannonAxis)) return result;
                result.hasChamber = true;
                isOption = true;
            }

            if (isOption) {
                currentPos = currentPos.relative(behind);
                safetyCounter++;
            } else {
                return result;
            }
        }

        // 【修正】増幅機3基以上は「構造エラー」にはしない(マニュアル準拠)。
        // 構成制約は変調/増幅ブロック合計10基(上のsafetyCounter)のみで、
        // 増幅効果は発射時のlimitDamage計算で2基分に頭打ちされる

        // 3. 蓄電ユニットの確認
        BlockState storageState = level.getBlockState(currentPos);
        // 【修正】isBlockヘルパーを使用
        if (!isBlock(storageState, BlockInit.WAVE_ENERGY_STORAGE)) return result;
        if (!isValidAxis(storageState, cannonAxis)) return result;

        currentPos = currentPos.relative(behind);

        // 4. モニタリングユニットの確認
        BlockState monitorState = level.getBlockState(currentPos);
        // 【修正】isBlockヘルパーを使用
        if (!isBlock(monitorState, BlockInit.MONITORING_UNIT)) return result;

        result.valid = true;
        return result;
    }

    // --- 【修正】FACINGとHORIZONTAL_FACINGの両対応 ---
    private boolean isValidAxis(BlockState state, Direction.Axis targetAxis) {
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING).getAxis() == targetAxis;
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis() == targetAxis;
        }
        // プロパティを持たないブロック(全方向対応とみなすか、あるいは軸なしブロックならtrue)
        // ここでは軸を持つべきブロックのみ通すため、プロパティなしはfalseとするのが安全だが、
        // 万が一プロパティ未設定のブロックを通したい場合はtrueにする
        return false;
    }

    public boolean isStructureValid() {
        return scanStructure().valid;
    }

    public void attemptFire(ServerLevel level, BlockPos pos, BlockState state, MonitoringUnitBlockEntity monitor) {
        attemptFire(level, pos, state, monitor, null);
    }

    // --- 【追加】発射者(実績付与用)を受け取るオーバーロード ---
    public void attemptFire(ServerLevel level, BlockPos pos, BlockState state, MonitoringUnitBlockEntity monitor,
                            @org.jetbrains.annotations.Nullable net.minecraft.server.level.ServerPlayer firer) {
        StructureResult structure = scanStructure();

        if (!structure.valid) {
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.5F);
            level.players().forEach(player -> player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent("message.wave_motion_gun_mod.fire.invalid_structure"), false));
            return;
        }

        int limitRange = (structure.barrelCount == 0) ? 10 : structure.barrelCount * 50;
        int limitRadius = structure.barrelCount * 3 + 2;
        int limitDamage = (structure.tachyonCount == 0) ? 10 : (structure.tachyonCount == 1 ? 50 : 100);

        int finalRange = Math.min(this.currentRange, limitRange);
        int finalRadius = Math.min(this.currentRadius, limitRadius);
        int finalDamage = Math.min(monitor.damageValue, limitDamage);

        boolean finalDestructive = structure.hasChamber && monitor.isDestructive;
        int finalInner = structure.hasAdjuster ? monitor.innerColor : 0xFFFFFF;
        int finalOuter = structure.hasAdjuster ? monitor.outerColor : 0x3399FF;

        // --- 【追加】セーフティ解除フラグの取得 ---
        boolean finalSafetyDisabled = monitor.isSafetyDisabled;

        BlockPos storagePos = calculateStoragePos(state.getValue(BlockStateProperties.FACING));
        BlockEntity be = level.getBlockEntity(storagePos);

        if (be instanceof WaveEnergyStorageBlockEntity storageBE) {
            long baseCost = calcFireCost(finalRange, finalRadius);
            long storedEnergy = storageBE.getEnergyStored();

            if (storedEnergy >= baseCost) {
                long maxConsumable = (long) (baseCost * OVERFILL_FACTOR);
                long actualConsumption = Math.min(storedEnergy, maxConsumable);
                double magnification = (double) actualConsumption / baseCost;

                int boostedRange = (int) (finalRange * magnification);
                int boostedRadius = (int) (finalRadius * magnification);

                storageBE.consumeEnergy((int) actualConsumption);
                storageBE.setOverheat(1400);
                storageBE.setExecutionMode(WaveEnergyStorageBlockEntity.ExecutionMode.EXHAUST);

                // --- 【修正】performFireへsafetyDisabledを渡す ---
                performFire(level, pos, state.getValue(BlockStateProperties.FACING),
                        boostedRange, boostedRadius, magnification,
                        finalDamage, finalDestructive, finalInner, finalOuter, finalSafetyDisabled);

                // 実績: 波動砲発射成功。地形破壊モードでの発射は隠し実績も付与
                if (firer != null) {
                    com.example.wave_motion_gun.utils.AdvancementHelper.grant(firer, "fire_wave_cannon");
                    if (finalDestructive) {
                        com.example.wave_motion_gun.utils.AdvancementHelper.grant(firer, "destructive_fire");
                    }
                }

            } else {
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.2F);
                net.minecraft.network.chat.TranslatableComponent msg = new net.minecraft.network.chat.TranslatableComponent(
                        "message.wave_motion_gun_mod.fire.low_energy", String.valueOf(storedEnergy), String.valueOf(baseCost));
                level.players().forEach(player -> player.displayClientMessage(msg, false));
            }
        } else {
            level.players().forEach(player -> player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent("message.wave_motion_gun_mod.fire.storage_error"), false));
        }
    }

    private BlockPos calculateStoragePos(Direction facing) {
        Direction behind = facing.getOpposite();
        BlockPos p = worldPosition.relative(behind);

        // 【修正】scanStructureと同じくバレルは最大6本まで (無限ループ防止)
        int barrels = 0;
        while (barrels < 6 && isBlock(level.getBlockState(p), BlockInit.BARREL_UNIT)) {
            p = p.relative(behind);
            barrels++;
        }
        int safety = 0;
        while (safety < 10) {
            BlockState s = level.getBlockState(p);
            if (isBlock(s, BlockInit.WAVE_ENERGY_STORAGE)) return p;
            if (s.isAir()) break;
            p = p.relative(behind);
            safety++;
        }
        return p;
    }

    // --- 【修正】safetyDisabled引数を追加 ---
    private void performFire(ServerLevel level, BlockPos pos, Direction direction, int range, int radius, double mag,
                             int damage, boolean destructive, int innerColor, int outerColor, boolean safetyDisabled) {
        net.minecraft.network.chat.TranslatableComponent debugMsg = new net.minecraft.network.chat.TranslatableComponent(
                "message.wave_motion_gun_mod.fire.success",
                String.format("%.0f", mag * 100), String.valueOf(range), String.valueOf(radius), String.valueOf(damage));
        level.players().forEach(player -> player.displayClientMessage(debugMsg, false));

        level.playSound(null, pos, SoundInit.WAVE_MOTION_GUN_LIGHTNING.get(), net.minecraft.sounds.SoundSource.BLOCKS, 5.0F, 1.0F);
        level.playSound(null, pos, SoundInit.WAVE_MOTION_GUN_FIRE.get(), net.minecraft.sounds.SoundSource.BLOCKS, 10.0F, 1.0F);

        WaveEnergyBall fireball = new WaveEnergyBall(EntityType.FIREBALL, level);
        double x = pos.getX() + 0.5D + direction.getStepX() * 0.7D;
        double y = pos.getY() + 0.5D + direction.getStepY() * 0.7D;
        double z = pos.getZ() + 0.5D + direction.getStepZ() * 0.7D;
        fireball.setPos(x, y, z);

        fireball.setCustomParams(range, (float) radius);
        // --- 【修正】セーフティ設定を渡す ---
        fireball.setAdditionalParams(damage, destructive, innerColor, outerColor, safetyDisabled);

        double speed = 1.2D;
        fireball.xPower = direction.getStepX() * speed;
        fireball.yPower = direction.getStepY() * speed;
        fireball.zPower = direction.getStepZ() * speed;

        level.addFreshEntity(fireball);
    }

    // --- クライアントTick処理 ---
    // パーティクル生成は削除し、接続先Storageのキャッシュ更新のみ行う
    public static void clientTick(Level level, BlockPos pos, BlockState state, WaveCannonBlockEntity be) {
        if (be.checkStorageTimer++ >= 20) {
            be.checkStorageTimer = 0;
            be.findStorageForClient(level, pos, state);
        }
    }

    private void findStorageForClient(Level level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(BlockStateProperties.FACING)) return;
        Direction facing = state.getValue(BlockStateProperties.FACING);
        Direction behind = facing.getOpposite();

        BlockPos p = pos.relative(behind);

        int safety = 0;
        while (safety < 20) {
            BlockState s = level.getBlockState(p);

            if (isBlock(s, BlockInit.WAVE_ENERGY_STORAGE)) {
                BlockEntity target = level.getBlockEntity(p);
                if (target instanceof WaveEnergyStorageBlockEntity storage) {
                    this.cachedStorage = storage;
                }
                return;
            }

            if (!isBlock(s, BlockInit.BARREL_UNIT) && !isBlock(s, BlockInit.TACHYON_PARTICLE_COMPRESSOR)
                    && !isBlock(s, BlockInit.WAVELENGTH_ADJUSTER) && !isBlock(s, BlockInit.WAVE_CHAMBER)) {
                break;
            }
            p = p.relative(behind);
            safety++;
        }
        // 見つからなければキャッシュクリア
        this.cachedStorage = null;
    }

    // Getter追加（レンダラー用）
    public WaveEnergyStorageBlockEntity getCachedStorage() {
        return this.cachedStorage;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("CurrentRange", this.currentRange);
        tag.putInt("CurrentRadius", this.currentRadius);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // 【修正】キーが無い旧データでデフォルト値が0に潰されないようにガードする (MonitoringUnitと同様)
        if (tag.contains("CurrentRange")) this.currentRange = tag.getInt("CurrentRange");
        if (tag.contains("CurrentRadius")) this.currentRadius = tag.getInt("CurrentRadius");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.load(tag);
    }
}