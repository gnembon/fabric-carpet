package carpet.script.utils;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.storage.RegionFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

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
        Path regionPath = world.getDimension().getType().getSaveDirectory(
                world.getSaveHandler().getWorldDir()
        ).toPath().resolve("region");
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
            RegionFile region = new RegionFile(regionFile, regionPath.toFile());
            if (regionCache != null) regionCache.put(currentRegionName, region);
            return region.hasChunk(chpos);
        }
        catch (IOException ignored) { }
        return true;
    }
}
