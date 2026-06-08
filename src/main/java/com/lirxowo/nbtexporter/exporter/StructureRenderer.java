package com.lirxowo.nbtexporter.exporter;

import com.lirxowo.nbtexporter.NBTExporter;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.data.AtlasIds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class StructureRenderer implements AutoCloseable {

    private static final float ORTHO_NEAR = 0.01f;
    private static final float ORTHO_FAR = 400f;
    private static final float CAMERA_DISTANCE = 200f;
    private static final float ORTHO_FACTOR = 0.7f;

    private final StructureScene scene;
    private final VirtualBlockLevel virtualLevel;
    private final RenderBlockGetter blockGetter;

    private final Map<ChunkSectionLayer, LayerMesh> blockMeshes = new EnumMap<>(ChunkSectionLayer.class);
    private boolean meshesBuilt;

    private final ProjectionMatrixBuffer projectionBuffer = new ProjectionMatrixBuffer("nbtexporter");
    private final GpuBuffer lightsBuffer;

    private TextureTarget previewTarget;
    private int previewTexWidth;
    private int previewTexHeight;

    private int camBlockX;
    private int camBlockY;
    private int camBlockZ;
    private float camOffX;
    private float camOffY;
    private float camOffZ;

    public StructureRenderer(StructureScene scene, Level level) {
        this.scene = scene;
        this.virtualLevel = new VirtualBlockLevel(level, scene.getBlocks(), scene.getBlockEntityNbt());
        this.virtualLevel.initAllBlockEntities();
        this.virtualLevel.initEntities(scene.getEntities());
        this.virtualLevel.refreshTransmitterConnections();
        this.virtualLevel.updateNeighborStates();
        this.blockGetter = new RenderBlockGetter(this.virtualLevel);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer lightData = Std140Builder.onStack(stack, Lighting.UBO_SIZE)
                    .putVec3(new Vector3f(2f, 2f, 2f))
                    .putVec3(new Vector3f(-2f, -2f, -2f))
                    .get();
            this.lightsBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "nbtexporter-lights", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, lightData);
        }
    }

    public void renderPreview(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, float rotX, float rotY, float zoom, float panX, float panY) {
        Minecraft mc = Minecraft.getInstance();
        int rectWidth = Math.max(1, x1 - x0);
        int rectHeight = Math.max(1, y1 - y0);
        int guiScale = Math.max(1, (int) mc.getWindow().getGuiScale());
        int texWidth = rectWidth * guiScale;
        int texHeight = rectHeight * guiScale;
        ensurePreviewTarget(texWidth, texHeight);
        float aspect = (float) texWidth / texHeight;

        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                previewTarget.getColorTexture(), 0, previewTarget.getDepthTexture(), 1.0);

        RenderSystem.backupProjectionMatrix();
        setupCamera(rotX, rotY, zoom, panX, panY, aspect);

        renderScene(mc, previewTarget);

        teardownCamera();
        RenderSystem.restoreProjectionMatrix();

        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        graphics.blit(previewTarget.getColorTextureView(), sampler, x0, y0, x1, y1, 0f, 1f, 1f, 0f);
    }

    private void ensurePreviewTarget(int width, int height) {
        if (previewTarget == null || previewTexWidth != width || previewTexHeight != height) {
            if (previewTarget != null) {
                previewTarget.destroyBuffers();
            }
            previewTarget = new TextureTarget("nbtexporter_preview", width, height, true);
            previewTexWidth = width;
            previewTexHeight = height;
        }
    }

    public void exportToPng(Path outputPath, int resolution, float rotX, float rotY, float zoom, float panX, float panY, Consumer<Path> onSuccess, Consumer<Exception> onError) {
        Minecraft mc = Minecraft.getInstance();
        float aspect = (float) mc.getWindow().getGuiScaledWidth() / mc.getWindow().getGuiScaledHeight();

        int maxSize = Math.min(resolution, RenderSystem.getDevice().getMaxTextureSize());
        int exportWidth;
        int exportHeight;
        if (aspect >= 1.0f) {
            exportWidth = maxSize;
            exportHeight = Math.max(1, Math.round(maxSize / aspect));
        } else {
            exportWidth = Math.max(1, Math.round(maxSize * aspect));
            exportHeight = maxSize;
        }

        TextureTarget fbo;
        try {
            fbo = new TextureTarget("nbtexporter_export", exportWidth, exportHeight, true);
        } catch (Exception | OutOfMemoryError error) {
            NBTExporter.LOGGER.error("Export failed: resolution {}x{} is too large", exportWidth, exportHeight, error);
            onError.accept(new RuntimeException("Resolution too large. Try a smaller value."));
            return;
        }

        GpuTexture colorTexture = fbo.getColorTexture();
        GpuTexture depthTexture = fbo.getDepthTexture();
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(colorTexture, 0, depthTexture, 1.0);

        RenderSystem.backupProjectionMatrix();
        setupCamera(rotX, rotY, zoom, panX, panY, aspect);

        renderScene(mc, fbo);

        teardownCamera();
        RenderSystem.restoreProjectionMatrix();

        captureTransparent(fbo, exportWidth, exportHeight, outputPath, mc, onSuccess, onError);
    }

    private void setupCamera(float rotX, float rotY, float zoom, float panX, float panY, float aspect) {
        float dim = Math.max(scene.getMaxDimension(), 1f);
        float orthoRange = dim / zoom * ORTHO_FACTOR;

        Matrix4f ortho = new Matrix4f().ortho(-orthoRange * aspect, orthoRange * aspect, -orthoRange, orthoRange, ORTHO_NEAR, ORTHO_FAR);
        GpuBufferSlice slice = projectionBuffer.getBuffer(ortho);
        RenderSystem.setProjectionMatrix(slice, ProjectionType.ORTHOGRAPHIC);

        Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        camBlockX = Mth.floor(camPos.x);
        camBlockY = Mth.floor(camPos.y);
        camBlockZ = Mth.floor(camPos.z);
        camOffX = (float) (camBlockX - camPos.x);
        camOffY = (float) (camBlockY - camPos.y);
        camOffZ = (float) (camBlockZ - camPos.z);

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        modelView.translate(panX, panY, -CAMERA_DISTANCE);
        modelView.rotate(Axis.XP.rotationDegrees(rotX));
        modelView.rotate(Axis.YP.rotationDegrees(rotY));
        modelView.translate(-(scene.getCenterX() + camOffX), -(scene.getCenterY() + camOffY), -(scene.getCenterZ() + camOffZ));
    }

    private static void teardownCamera() {
        RenderSystem.getModelViewStack().popMatrix();
    }

    private void renderScene(Minecraft mc, RenderTarget target) {
        if (!meshesBuilt) {
            buildBlockMeshes(mc);
            meshesBuilt = true;
        }

        TextureAtlas atlas = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
        GpuTextureView atlasView = atlas.getTextureView();
        GpuSampler atlasSampler = RenderSystem.getSamplerCache().getSampler(
                AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, true);
        int atlasWidth = atlas.getTexture().getWidth(0);
        int atlasHeight = atlas.getTexture().getHeight(0);

        GpuTextureView prevColorOverride = RenderSystem.outputColorTextureOverride;
        GpuTextureView prevDepthOverride = RenderSystem.outputDepthTextureOverride;
        RenderSystem.outputColorTextureOverride = target.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();
        try {
            for (Map.Entry<ChunkSectionLayer, LayerMesh> entry : blockMeshes.entrySet()) {
                drawLayer(mc, target, entry.getKey(), entry.getValue(), atlasView, atlasSampler, atlasWidth, atlasHeight);
            }

            renderFeatures(mc);
        } finally {
            RenderSystem.outputColorTextureOverride = prevColorOverride;
            RenderSystem.outputDepthTextureOverride = prevDepthOverride;
        }
    }

    private void buildBlockMeshes(Minecraft mc) {
        ModelBlockRenderer modelRenderer = new ModelBlockRenderer(true, true, mc.getBlockColors());
        FluidRenderer fluidRenderer = new FluidRenderer(mc.getModelManager().getFluidStateModelSet());

        Map<ChunkSectionLayer, ByteBufferBuilder> byteBuffers = new EnumMap<>(ChunkSectionLayer.class);
        Map<ChunkSectionLayer, BufferBuilder> builders = new EnumMap<>(ChunkSectionLayer.class);

        BlockQuadOutput quadOutput = (x, y, z, quad, instance) -> {
            BufferBuilder builder = getOrBeginLayer(byteBuffers, builders, quad.materialInfo().layer());
            builder.putBlockBakedQuad(x, y, z, quad, instance);
        };
        FluidRenderer.Output fluidOutput = layer -> getOrBeginLayer(byteBuffers, builders, layer);

        BlockModelLighter.enableCaching();
        try {
            for (Map.Entry<BlockPos, BlockState> entry : scene.getBlocks().entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState state = entry.getValue();

                FluidState fluidState = state.getFluidState();
                if (!fluidState.isEmpty()) {
                    fluidRenderer.tesselate(blockGetter, pos, fluidOutput, state, fluidState);
                }

                if (state.getRenderShape() == RenderShape.MODEL) {
                    BlockStateModel model = mc.getModelManager().getBlockStateModelSet().get(state);
                    modelRenderer.tesselateBlock(quadOutput, pos.getX(), pos.getY(), pos.getZ(), blockGetter, pos, state, model, state.getSeed(pos));
                }
            }
        } finally {
            BlockModelLighter.clearCache();
        }

        GpuDevice device = RenderSystem.getDevice();
        for (Map.Entry<ChunkSectionLayer, BufferBuilder> entry : builders.entrySet()) {
            try (MeshData mesh = entry.getValue().build()) {
                if (mesh != null) {
                    GpuBuffer vertices = device.createBuffer(() -> "nbtexporter-vbo",
                            GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, mesh.vertexBuffer());
                    MeshData.SortState sortState = null;
                    if (entry.getKey() == ChunkSectionLayer.TRANSLUCENT) {
                        try (ByteBufferBuilder sortScratch = ByteBufferBuilder.exactlySized(mesh.drawState().indexCount() * Integer.BYTES)) {
                            sortState = mesh.sortQuads(sortScratch, VertexSorting.DISTANCE_TO_ORIGIN);
                        }
                    }
                    blockMeshes.put(entry.getKey(), new LayerMesh(vertices, mesh.drawState().indexCount(), mesh.drawState().mode(), sortState));
                }
            }
        }

        byteBuffers.values().forEach(ByteBufferBuilder::close);
    }

    private BufferBuilder getOrBeginLayer(Map<ChunkSectionLayer, ByteBufferBuilder> byteBuffers, Map<ChunkSectionLayer, BufferBuilder> builders, ChunkSectionLayer layer) {
        BufferBuilder builder = builders.get(layer);
        if (builder == null) {
            ByteBufferBuilder byteBuffer = new ByteBufferBuilder(layer.bufferSize());
            builder = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, layer.vertexFormat());
            byteBuffers.put(layer, byteBuffer);
            builders.put(layer, builder);
        }
        return builder;
    }

    private void drawLayer(Minecraft mc, RenderTarget target, ChunkSectionLayer layer, LayerMesh mesh, GpuTextureView atlasView, GpuSampler atlasSampler, int atlasWidth, int atlasHeight) {
        RenderPipeline pipeline = layer.pipeline();

        GpuBuffer indices;
        VertexFormat.IndexType indexType;
        ByteBufferBuilder sortScratch = null;
        ByteBufferBuilder.Result sortedIndices = null;
        if (mesh.sortState() != null) {
            Vector3f eye = new Matrix4f(RenderSystem.getModelViewMatrix()).invert().transformPosition(new Vector3f());
            sortScratch = ByteBufferBuilder.exactlySized(mesh.indexCount() * Integer.BYTES);
            sortedIndices = mesh.sortState().buildSortedIndexBuffer(sortScratch, VertexSorting.byDistance(eye));
            indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(sortedIndices.byteBuffer());
            indexType = mesh.sortState().indexType();
        } else {
            RenderSystem.AutoStorageIndexBuffer auto = RenderSystem.getSequentialBuffer(mesh.mode());
            indices = auto.getBuffer(mesh.indexCount());
            indexType = auto.type();
        }

        GpuBufferSlice chunkSection = RenderSystem.getDynamicUniforms().writeChunkSections(
                new DynamicUniforms.ChunkSectionInfo(RenderSystem.getModelViewMatrix(), camBlockX, camBlockY, camBlockZ, 1.0f, atlasWidth, atlasHeight))[0];

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "nbtexporter",
                target.getColorTextureView(), OptionalInt.empty(),
                target.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("ChunkSection", chunkSection);
            pass.bindTexture("Sampler0", atlasView, atlasSampler);
            pass.bindTexture("Sampler2", mc.gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setVertexBuffer(0, mesh.vertices());
            pass.setIndexBuffer(indices, indexType);
            pass.drawIndexed(0, 0, mesh.indexCount(), 1);
        } finally {
            if (sortedIndices != null) {
                sortedIndices.close();
            }
            if (sortScratch != null) {
                sortScratch.close();
            }
        }
    }

    private void renderFeatures(Minecraft mc) {
        RenderSystem.setShaderLights(lightsBuffer.slice(0, Lighting.UBO_SIZE));
        FeatureRenderDispatcher featureDispatcher = mc.gameRenderer.getFeatureRenderDispatcher();
        CameraRenderState cameraState = buildCameraState();

        BlockEntityRenderDispatcher beDispatcher = mc.getBlockEntityRenderDispatcher();
        EntityRenderDispatcher entityDispatcher = mc.getEntityRenderDispatcher();
        beDispatcher.prepare(Vec3.ZERO);
        entityDispatcher.prepare(mc.gameRenderer.getMainCamera(), null);

        for (BlockEntity blockEntity : virtualLevel.getRenderedBlockEntities()) {
            try {
                BlockEntityRenderState state = beDispatcher.tryExtractRenderState(blockEntity, 0f, null);
                if (state == null) {
                    continue;
                }
                state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
                BlockPos pos = blockEntity.getBlockPos();
                PoseStack pose = new PoseStack();
                pose.translate(pos.getX() + camOffX, pos.getY() + camOffY, pos.getZ() + camOffZ);
                beDispatcher.submit(state, pose, featureDispatcher.getSubmitNodeStorage(), cameraState);
            } catch (Exception ignored) {
            }
        }

        for (Entity entity : virtualLevel.getRenderedEntities()) {
            try {
                EntityRenderState state = entityDispatcher.extractEntity(entity, 0f);
                state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
                PoseStack pose = new PoseStack();
                entityDispatcher.submit(state, cameraState, entity.getX() + camOffX, entity.getY() + camOffY, entity.getZ() + camOffZ, pose, featureDispatcher.getSubmitNodeStorage());
            } catch (Exception ignored) {
            }
        }

        try {
            featureDispatcher.renderAllFeatures();
            mc.renderBuffers().bufferSource().endBatch();
        } catch (Exception exception) {
            NBTExporter.LOGGER.warn("BlockEntity/Entity feature rendering failed", exception);
        }
    }

    private CameraRenderState buildCameraState() {
        CameraRenderState state = new CameraRenderState();
        state.pos = Vec3.ZERO;
        state.blockPos = BlockPos.ZERO;
        return state;
    }

    private void captureTransparent(TextureTarget fbo, int width, int height, Path outputPath, Minecraft mc, Consumer<Path> onSuccess, Consumer<Exception> onError) {
        GpuTexture colorTexture = fbo.getColorTexture();
        if (colorTexture == null) {
            fbo.destroyBuffers();
            onError.accept(new IllegalStateException("Export framebuffer has no color texture"));
            return;
        }

        try {
            int pixelSize = colorTexture.getFormat().pixelSize();
            GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "nbtexporter screenshot", 9, (long) width * height * pixelSize);
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.copyTextureToBuffer(colorTexture, buffer, 0L, () -> {
                NativeImage image = new NativeImage(width, height, false);
                try (GpuBuffer.MappedView read = encoder.mapBuffer(buffer, true, false)) {
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int argb = read.data().getInt((x + y * width) * pixelSize);
                            image.setPixelABGR(x, height - y - 1, argb);
                        }
                    }
                }
                buffer.close();
                fbo.destroyBuffers();
                saveImage(image, outputPath, mc, onSuccess, onError);
            }, 0);
        } catch (Exception | OutOfMemoryError error) {
            NBTExporter.LOGGER.error("Export readback failed", error);
            fbo.destroyBuffers();
            onError.accept(new RuntimeException("Export failed: " + error.getMessage()));
        }
    }

    private void saveImage(NativeImage image, Path outputPath, Minecraft mc, Consumer<Path> onSuccess, Consumer<Exception> onError) {
        CompletableFuture.runAsync(() -> {
            try {
                if (outputPath.getParent() != null) {
                    outputPath.getParent().toFile().mkdirs();
                }
                image.writeToFile(outputPath);
                image.close();
                mc.execute(() -> onSuccess.accept(outputPath));
            } catch (Exception exception) {
                NBTExporter.LOGGER.error("Export failed", exception);
                image.close();
                mc.execute(() -> onError.accept(exception));
            }
        });
    }

    private void releaseMeshes() {
        blockMeshes.values().forEach(LayerMesh::close);
        blockMeshes.clear();
        meshesBuilt = false;
    }

    @Override
    public void close() {
        releaseMeshes();
        if (previewTarget != null) {
            previewTarget.destroyBuffers();
            previewTarget = null;
        }
        projectionBuffer.close();
        lightsBuffer.close();
    }

    record LayerMesh(GpuBuffer vertices, int indexCount, VertexFormat.Mode mode, MeshData.SortState sortState) {
        void close() {
            vertices.close();
        }
    }
}
