package com.lirxowo.nbtexporter.exporter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nullable;

public class RenderBlockGetter implements BlockAndTintGetter {

    private static final CardinalLighting UNIFORM_LIGHTING = new CardinalLighting(1f, 1f, 1f, 1f, 1f, 1f);

    private final VirtualBlockLevel level;
    @Nullable
    private final ClientLevel clientLevel;

    public RenderBlockGetter(VirtualBlockLevel level) {
        this.level = level;
        this.clientLevel = Minecraft.getInstance().level;
    }

    @Override
    public @NotNull BlockState getBlockState(@NotNull BlockPos pos) {
        return level.getBlockState(pos);
    }

    @Override
    public @NotNull FluidState getFluidState(@NotNull BlockPos pos) {
        return level.getFluidState(pos);
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(@NotNull BlockPos pos) {
        return level.getBlockEntity(pos);
    }

    @Override
    public @NonNull LevelLightEngine getLightEngine() {
        return level.getLightEngine();
    }

    @Override
    public int getBrightness(@NotNull LightLayer lightType, @NotNull BlockPos pos) {
        return 15;
    }

    @Override
    public int getRawBrightness(@NotNull BlockPos pos, int amount) {
        return 15;
    }

    @Override
    public boolean canSeeSky(@NotNull BlockPos pos) {
        return true;
    }

    @Override
    public @NonNull CardinalLighting    cardinalLighting() {
        return UNIFORM_LIGHTING;
    }

    @Override
    public int getBlockTint(@NotNull BlockPos pos, @NotNull ColorResolver resolver) {
        return clientLevel != null ? clientLevel.getBlockTint(pos, resolver) : -1;
    }

    @Override
    public int getHeight() {
        return level.getHeight();
    }

    @Override
    public int getMinY() {
        return level.getMinY();
    }
}
