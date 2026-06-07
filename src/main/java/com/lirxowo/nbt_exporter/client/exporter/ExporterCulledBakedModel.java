package com.lirxowo.nbt_exporter.client.exporter;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

final class ExporterCulledBakedModel extends BakedModelWrapper<BakedModel> {
    private static final float FACE_EPSILON = 1.0e-4f;

    private final VirtualBlockLevel level;
    private final BlockPos pos;
    private final BlockState state;

    ExporterCulledBakedModel(BakedModel originalModel, VirtualBlockLevel level, BlockPos pos, BlockState state) {
        super(originalModel);
        this.level = level;
        this.pos = pos;
        this.state = state;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, extraData, renderType);
        if (side != null || quads.isEmpty()) {
            return quads;
        }

        List<BakedQuad> filtered = null;
        for (BakedQuad quad : quads) {
            if (shouldCullUnassignedQuad(quad)) {
                if (filtered == null) {
                    filtered = new ArrayList<>(quads.size());
                    for (BakedQuad existing : quads) {
                        if (existing == quad) {
                            break;
                        }
                        filtered.add(existing);
                    }
                }
                continue;
            }

            if (filtered != null) {
                filtered.add(quad);
            }
        }

        return filtered == null ? quads : filtered;
    }

    private boolean shouldCullUnassignedQuad(BakedQuad quad) {
        Direction face = quad.getDirection();
        if (!isQuadOnFace(quad, face)) {
            return false;
        }

        return !Block.shouldRenderFace(state, level, pos, face, pos.relative(face));
    }

    private static boolean isQuadOnFace(BakedQuad quad, Direction face) {
        float expected = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0f : 0.0f;
        int coordIndex = switch (face.getAxis()) {
            case X -> 0;
            case Y -> 1;
            case Z -> 2;
        };
        int[] vertices = quad.getVertices();
        for (int i = 0; i < 4; i++) {
            float coord = Float.intBitsToFloat(vertices[i * 8 + coordIndex]);
            if (Math.abs(coord - expected) > FACE_EPSILON) {
                return false;
            }
        }
        return true;
    }
}
