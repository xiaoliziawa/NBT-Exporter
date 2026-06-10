package com.lirxowo.nbtexporter.exporter.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;

public class MekanismTransmitterHelper {

    // Mekanism 尚未更新到 26.1.2,以下逻辑暂时注释。
    @SuppressWarnings({"unused", "CommentedOutCode"})
    public static void refreshConnections(Map<BlockPos, BlockEntity> blockEntities) {
        // for (Map.Entry<BlockPos, BlockEntity> entry : blockEntities.entrySet()) {
        //     BlockEntity blockEntity = entry.getValue();
        //     if (!(blockEntity instanceof TileEntityTransmitter transmitterTile)) {
        //         continue;
        //     }
        //     Transmitter<?, ?, ?> transmitter = transmitterTile.getTransmitter();
        //     byte connections = 0x00;
        //     for (Direction side : Direction.values()) {
        //         BlockPos neighborPos = entry.getKey().relative(side);
        //         BlockEntity neighborBlockEntity = blockEntities.get(neighborPos);
        //         if (neighborBlockEntity instanceof TileEntityTransmitter neighborTile) {
        //             if (transmitter.supportsTransmissionType(neighborTile) && transmitter.canConnect(side) && neighborTile.getTransmitter().canConnect(side.getOpposite())) {
        //                 connections |= (byte) (1 << side.ordinal());
        //             }
        //         }
        //     }
        //     transmitter.currentTransmitterConnections = connections;
        // }
    }
}
