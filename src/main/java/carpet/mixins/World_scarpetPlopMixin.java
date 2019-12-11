package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(World.class)
public class World_scarpetPlopMixin
{

    @Redirect(method = "getTopY", at = @At(
            value = "INVOKE",
            target = "net/minecraft/world/chunk/WorldChunk.sampleHeightmap(Lnet/minecraft/world/Heightmap$Type;II)I"
    ))
    private int fixSampleHeightmap(WorldChunk chunk, Heightmap.Type type, int x, int z)
    {
        if (CarpetSettings.skipGenerationChecks)
        {
            Heightmap.Type newType = type;
            if (type == Heightmap.Type.OCEAN_FLOOR_WG) newType = Heightmap.Type.OCEAN_FLOOR;
            else if (type == Heightmap.Type.WORLD_SURFACE_WG) newType = Heightmap.Type.WORLD_SURFACE;
            return chunk.sampleHeightmap(newType, x, z);
        }
        return chunk.sampleHeightmap(type, x, z);
    }
}

