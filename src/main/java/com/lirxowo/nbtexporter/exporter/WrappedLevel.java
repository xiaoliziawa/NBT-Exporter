package com.lirxowo.nbtexporter.exporter;

import com.lirxowo.nbtexporter.mixin.EntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;
import net.neoforged.neoforge.entity.PartEntity;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
public class WrappedLevel extends Level {

    protected final Level host;
    private final LevelEntityGetter<Entity> emptyEntityGetter = new EmptyEntityGetter<>();

    public WrappedLevel(Level host) {
        super((WritableLevelData) host.getLevelData(), host.dimension(), host.registryAccess(),
                host.dimensionTypeRegistration(), host.isClientSide(), host.isDebug(), 0, 0);
        this.host = host;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return host.getBlockState(pos);
    }

    @Override
    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> predicate) {
        return host.isStateAtPosition(pos, predicate);
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return host.getBlockEntity(pos);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return host.getLightEngine();
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return host.getBlockTicks();
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return host.getFluidTicks();
    }

    @Override
    public ChunkSource getChunkSource() {
        return host.getChunkSource();
    }

    @Override
    public Scoreboard getScoreboard() {
        return host.getScoreboard();
    }

    @Override
    public RegistryAccess registryAccess() {
        return host.registryAccess();
    }

    @Override
    public PotionBrewing potionBrewing() {
        return host.potionBrewing();
    }

    @Override
    public RecipeAccess recipeAccess() {
        return host.recipeAccess();
    }

    @Override
    public ClockManager clockManager() {
        return host.clockManager();
    }

    @Override
    public EnvironmentAttributeSystem environmentAttributes() {
        return host.environmentAttributes();
    }

    @Override
    public FuelValues fuelValues() {
        return host.fuelValues();
    }

    @Override
    public TickRateManager tickRateManager() {
        return host.tickRateManager();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return host.enabledFeatures();
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
        return host.getUncachedNoiseBiome(x, y, z);
    }

    @Override
    public String gatherChunkSourceStats() {
        return host.gatherChunkSourceStats();
    }

    @Override
    public int getMaxLocalRawBrightness(BlockPos pos) {
        return 15;
    }

    @Override
    public int getSeaLevel() {
        return host.getSeaLevel();
    }

    @Override
    public WorldBorder getWorldBorder() {
        return host.getWorldBorder();
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags) {
        return host.setBlock(pos, state, flags);
    }

    @Override
    public boolean addFreshEntity(Entity entity) {
        ((EntityAccessor) entity).nbtexporter$callSetLevel(host);
        return host.addFreshEntity(entity);
    }

    @Override
    public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
        host.sendBlockUpdated(pos, oldState, newState, flags);
    }

    @Override
    public void updateNeighbourForOutputSignal(BlockPos pos, Block block) {}

    @Override
    public void levelEvent(@Nullable Entity entity, int type, BlockPos pos, int data) {}

    @Override
    public void destroyBlockProgress(int breakerId, BlockPos pos, int progress) {}

    @Override
    public void gameEvent(@Nullable Entity entity, Holder<GameEvent> event, Vec3 pos) {}

    @Override
    public void gameEvent(Holder<GameEvent> event, Vec3 position, GameEvent.Context context) {}

    @Override
    public void playSeededSound(@Nullable Entity except, double x, double y, double z, Holder<SoundEvent> sound,
                                SoundSource source, float volume, float pitch, long seed) {}

    @Override
    public void playSeededSound(@Nullable Entity except, Entity sourceEntity, Holder<SoundEvent> sound,
                                SoundSource source, float volume, float pitch, long seed) {}

    @Override
    public void explode(@Nullable Entity source, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator damageCalculator,
                        double x, double y, double z, float r, boolean fire, Level.ExplosionInteraction interactionType,
                        ParticleOptions smallExplosionParticles, ParticleOptions largeExplosionParticles,
                        WeightedList<ExplosionParticleInfo> blockParticles, Holder<SoundEvent> explosionSound) {}

    @Override
    public void setRespawnData(LevelData.RespawnData respawnData) {}

    @Override
    public LevelData.RespawnData getRespawnData() {
        return host.getRespawnData();
    }

    @Override
    public Collection<? extends PartEntity<?>> dragonParts() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Entity getEntity(int id) {
        return null;
    }

    @Nullable
    @Override
    public MapItemSavedData getMapData(MapId mapId) {
        return null;
    }

    @Override
    public List<? extends Player> players() {
        return Collections.emptyList();
    }

    @Override
    protected LevelEntityGetter<Entity> getEntities() {
        return emptyEntityGetter;
    }

    private static final class EmptyEntityGetter<T extends EntityAccess> implements LevelEntityGetter<T> {

        @Nullable
        @Override
        public T get(int id) {
            return null;
        }

        @Nullable
        @Override
        public T get(UUID uuid) {
            return null;
        }

        @Override
        public Iterable<T> getAll() {
            return Collections.emptyList();
        }

        @Override
        public <U extends T> void get(EntityTypeTest<T, U> test, AbortableIterationConsumer<U> consumer) {}

        @Override
        public void get(AABB box, Consumer<T> consumer) {}

        @Override
        public <U extends T> void get(EntityTypeTest<T, U> test, AABB box, AbortableIterationConsumer<U> consumer) {}
    }
}
