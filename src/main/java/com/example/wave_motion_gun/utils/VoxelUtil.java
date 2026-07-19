package com.example.wave_motion_gun.utils;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VoxelUtil {

    /**
     * VoxelShape を指定した方向に回転させます。
     * @param sourceDirection モデルの基準方向（通常は NORTH）
     * @param targetDirection 回転させたい方向
     * @param shape 回転させる形状
     * @return 回転後の VoxelShape
     */
    public static VoxelShape rotateShape(Direction sourceDirection, Direction targetDirection, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{ shape, Shapes.empty() };

        // 北(NORTH)を基準としたときの回転回数を計算
        int times = (targetDirection.get2DDataValue() - sourceDirection.get2DDataValue() + 4) % 4;

        for (int i = 0; i < times; i++) {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = Shapes.or(buffer[1],
                    Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)
            ));
            buffer[0] = buffer[1];
            buffer[1] = Shapes.empty();
        }
        return buffer[0];
    }
}