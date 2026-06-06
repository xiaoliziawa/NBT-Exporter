package com.lirxowo.nbtexporter.exporter.compat;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.client.IModelOffsetProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class IEModelHelper {
    public static ModelData getModelOffset(BlockEntity blockEntity, BlockState state) {
        if (blockEntity instanceof IModelOffsetProvider offsetProvider) {
            try {
                BlockPos offset = offsetProvider.getModelOffset(state, Vec3i.ZERO);

                if (offset != null) {
                    return ModelData.builder().with(IEProperties.Model.SUBMODEL_OFFSET, offset).build();
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}
