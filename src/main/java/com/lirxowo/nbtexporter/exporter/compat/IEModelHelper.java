package com.lirxowo.nbtexporter.exporter.compat;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings({"unused", "CommentedOutCode"})
public class IEModelHelper {

    // Immersive Engineering 尚未更新到 26.1.2,以下逻辑暂时注释。
    public static Object getModelOffset(BlockEntity blockEntity, BlockState state) {
        // if (blockEntity instanceof IModelOffsetProvider offsetProvider) {
        //     try {
        //         BlockPos offset = offsetProvider.getModelOffset(state, Vec3i.ZERO);
        //         if (offset != null) {
        //             return ModelData.builder().with(IEProperties.Model.SUBMODEL_OFFSET, offset).build();
        //         }
        //     } catch (Exception ignored) {
        //     }
        // }
        return null;
    }
}
