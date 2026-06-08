package com.lirxowo.nbtexporter.exporter;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class StructureScene {
    public record EntityInfo(Vec3 pos, CompoundTag nbt) {
    }

    private final Map<BlockPos, BlockState> blocks = new HashMap<>();
    private final Map<BlockPos, CompoundTag> blockEntityNbt = new HashMap<>();
    private final List<EntityInfo> entities = new ArrayList<>();
    private final Set<String> missingBlocks = new LinkedHashSet<>();

    private int sizeX;
    private int sizeY;
    private int sizeZ;

    private float centerX;
    private float centerY;
    private float centerZ;

    private float maxDimension;

    private StructureScene() {
    }

    public static StructureScene loadFromFile(Path path) throws IOException {
        CompoundTag root = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());

        StructureScene scene = new StructureScene();
        scene.parse(root);

        return scene;
    }

    private void parse(CompoundTag root) {
        ListTag sizeTag = root.getListOrEmpty("size");
        sizeX = sizeTag.getIntOr(0, 0);
        sizeY = sizeTag.getIntOr(1, 0);
        sizeZ = sizeTag.getIntOr(2, 0);

        ListTag paletteTag = root.getListOrEmpty("palette");
        BlockState[] palette = new BlockState[paletteTag.size()];

        for (int index = 0; index < paletteTag.size(); index++) {
            CompoundTag paletteEntry = paletteTag.getCompoundOrEmpty(index);
            String blockName = paletteEntry.getStringOr("Name", "");
            Identifier blockId = Identifier.parse(blockName);

            if (!BuiltInRegistries.BLOCK.containsKey(blockId)) {
                palette[index] = Blocks.AIR.defaultBlockState();
                missingBlocks.add(blockName);
            } else {
                palette[index] = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, paletteEntry);
            }
        }

        ListTag blocksList = root.getListOrEmpty("blocks");

        for (int index = 0; index < blocksList.size(); index++) {

            CompoundTag entry = blocksList.getCompoundOrEmpty(index);
            ListTag posTag = entry.getListOrEmpty("pos");

            BlockPos pos = new BlockPos(posTag.getIntOr(0, 0), posTag.getIntOr(1, 0), posTag.getIntOr(2, 0));

            int stateIndex = entry.getIntOr("state", -1);

            BlockState state;
            if (stateIndex >= 0 && stateIndex < palette.length) {
                state = palette[stateIndex];
            } else {
                state = Blocks.AIR.defaultBlockState();
            }

            if (!state.isAir()) {
                blocks.put(pos, state);
            }

            if (entry.contains("nbt")) {
                blockEntityNbt.put(pos, entry.getCompoundOrEmpty("nbt"));
            }
        }

        centerX = sizeX / 2f;
        centerY = sizeY / 2f;
        centerZ = sizeZ / 2f;

        maxDimension = Math.max(sizeX, Math.max(sizeY, sizeZ));

        if (root.contains("entities")) {
            ListTag entitiesList = root.getListOrEmpty("entities");

            for (int index = 0; index < entitiesList.size(); index++) {
                CompoundTag entityEntry = entitiesList.getCompoundOrEmpty(index);

                if (!entityEntry.contains("nbt")) {
                    continue;
                }

                ListTag posTag = entityEntry.getListOrEmpty("pos");

                Vec3 pos = new Vec3(posTag.getDoubleOr(0, 0), posTag.getDoubleOr(1, 0), posTag.getDoubleOr(2, 0));

                CompoundTag nbt = entityEntry.getCompoundOrEmpty("nbt");

                entities.add(new EntityInfo(pos, nbt));
            }
        }
    }
}
