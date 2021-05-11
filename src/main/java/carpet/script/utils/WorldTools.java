package carpet.script.utils;

import carpet.fakes.MinecraftServerInterface;
import com.google.common.collect.ImmutableList;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.border.WorldBorderListener;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.Spawner;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.UnmodifiableLevelProperties;
import net.minecraft.world.storage.RegionFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class WorldTools
{

    public static boolean canHasChunk(ServerWorld world, ChunkPos chpos, Map<String, RegionFile> regionCache, boolean deepcheck)
    {
        if (world.getChunk(chpos.x, chpos.z, ChunkStatus.STRUCTURE_STARTS, false) != null)
            return true;
        String currentRegionName = "r." + chpos.getRegionX() + "." + chpos.getRegionZ() + ".mca";
        if (regionCache != null && regionCache.containsKey(currentRegionName))
        {
            RegionFile region = regionCache.get(currentRegionName);
            if (region == null) return false;
            return region.hasChunk(chpos);
        }
        Path regionPath = new File(((MinecraftServerInterface )world.getServer()).getCMSession().getWorldDirectory(world.getRegistryKey()), "region").toPath();
        Path regionFilePath = regionPath.resolve(currentRegionName);
        File regionFile = regionFilePath.toFile();
        if (!regionFile.exists())
        {
            if (regionCache != null) regionCache.put(currentRegionName, null);
            return false;
        }
        if (!deepcheck) return true; // not using cache in this case.
        try
        {
            RegionFile region = new RegionFile(regionFile, regionPath.toFile(), true);
            if (regionCache != null) regionCache.put(currentRegionName, region);
            return region.hasChunk(chpos);
        }
        catch (IOException ignored) { }
        return true;
    }

    public static boolean createWorld(MinecraftServer server, String worldKey, Long seed)
    {
        Identifier worldId = new Identifier(worldKey);
        ServerWorld overWorld = server.getOverworld();

        Set<RegistryKey<World>> worldKeys = server.getWorldRegistryKeys();
        for (RegistryKey<World> worldRegistryKey : worldKeys)
        {
            if (worldRegistryKey.getValue().equals(worldId))
            {
                // world with this id already exists
                return false;
            }
        }
        ServerWorldProperties serverWorldProperties = server.getSaveProperties().getMainWorldProperties();
        GeneratorOptions generatorOptions = server.getSaveProperties().getGeneratorOptions();
        boolean bl = generatorOptions.isDebugWorld();
        long l = generatorOptions.getSeed();
        long m = BiomeAccess.hashSeed(l);
        List<Spawner> list = ImmutableList.of();
        SimpleRegistry<DimensionOptions> simpleRegistry = generatorOptions.getDimensions();
        DimensionOptions dimensionOptions = simpleRegistry.get(DimensionOptions.OVERWORLD);
        ChunkGenerator chunkGenerator2;
        DimensionType dimensionType2;
        if (dimensionOptions == null) {
            dimensionType2 = (DimensionType)server.getRegistryManager().getDimensionTypes().getOrThrow(DimensionType.OVERWORLD_REGISTRY_KEY);
            chunkGenerator2 = GeneratorOptions.createOverworldGenerator(server.getRegistryManager().get(Registry.BIOME_KEY), server.getRegistryManager().get(Registry.NOISE_SETTINGS_WORLDGEN), (new Random()).nextLong());
        } else {
            dimensionType2 = dimensionOptions.getDimensionType();
            chunkGenerator2 = dimensionOptions.getChunkGenerator();
        }

        RegistryKey<World> customWorld = RegistryKey.of(Registry.DIMENSION, worldId);

        //chunkGenerator2 = GeneratorOptions.createOverworldGenerator(server.getRegistryManager().get(Registry.BIOME_KEY), server.getRegistryManager().get(Registry.NOISE_SETTINGS_WORLDGEN), (seed==null)?l:seed);

        chunkGenerator2 = new NoiseChunkGenerator(
                new VanillaLayeredBiomeSource((seed==null)?l:seed, false, false, server.getRegistryManager().get(Registry.BIOME_KEY)),
                (seed==null)?l:seed,
                () -> server.getRegistryManager().get(Registry.NOISE_SETTINGS_WORLDGEN).getOrThrow(ChunkGeneratorSettings.OVERWORLD)
        );

        ServerWorld serverWorld = new ServerWorld(
                server,
                Util.getMainWorkerExecutor(),
                ((MinecraftServerInterface) server).getCMSession(),
                new UnmodifiableLevelProperties(server.getSaveProperties(), serverWorldProperties),
                customWorld,
                dimensionType2,
                NOOP_LISTENER,
                chunkGenerator2,
                bl,
                (seed==null)?l:seed,
                list,
                false);
        overWorld.getWorldBorder().addListener(new WorldBorderListener.WorldBorderSyncer(serverWorld.getWorldBorder()));
        ((MinecraftServerInterface) server).getCMWorlds().put(customWorld, serverWorld);
        return true;
    }

    public static void forceChunkUpdate(BlockPos pos, ServerWorld world)
    {
        WorldChunk worldChunk = world.getChunkManager().getWorldChunk(pos.getX()>>4, pos.getZ()>>4, false);
        if (worldChunk != null)
        {
            int vd = world.getServer().getPlayerManager().getViewDistance() * 16;
            int vvd = vd * vd;
            List<ServerPlayerEntity> nearbyPlayers = world.getPlayers(p -> pos.getSquaredDistance(p.getX(), pos.getY(), p.getZ(), true) < vvd);
            if (!nearbyPlayers.isEmpty())
            {
                ChunkDataS2CPacket packet = new ChunkDataS2CPacket(worldChunk, 65535);
                ChunkPos chpos = new ChunkPos(pos);
                nearbyPlayers.forEach(p -> p.networkHandler.sendPacket(packet));
            }
        }
    }


    private static class NoopWorldGenerationProgressListener implements WorldGenerationProgressListener
    {
        @Override public void start(ChunkPos spawnPos) { }
        @Override public void setChunkStatus(ChunkPos pos, ChunkStatus status) { }
        @Override public void stop() { }
    }

    public static final WorldGenerationProgressListener NOOP_LISTENER = new NoopWorldGenerationProgressListener();
}
