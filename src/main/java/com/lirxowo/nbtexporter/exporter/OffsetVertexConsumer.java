package com.lirxowo.nbtexporter.exporter;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

public class OffsetVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;

    public OffsetVertexConsumer(VertexConsumer delegate, float offsetX, float offsetY, float offsetZ) {
        this.delegate = delegate;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    @Override
    public @NotNull VertexConsumer addVertex(float x, float y, float z) {
        return delegate.addVertex(x + offsetX, y + offsetY, z + offsetZ);
    }

    @Override
    public @NotNull VertexConsumer setColor(int red, int green, int blue, int alpha) {
        return delegate.setColor(red, green, blue, alpha);
    }

    @Override
    public @NotNull VertexConsumer setUv(float u, float v) {
        return delegate.setUv(u, v);
    }

    @Override
    public @NotNull VertexConsumer setUv1(int u, int v) {
        return delegate.setUv1(u, v);
    }

    @Override
    public @NotNull VertexConsumer setUv2(int u, int v) {
        return delegate.setUv2(u, v);
    }

    @Override
    public @NotNull VertexConsumer setNormal(float x, float y, float z) {
        return delegate.setNormal(x, y, z);
    }
}
