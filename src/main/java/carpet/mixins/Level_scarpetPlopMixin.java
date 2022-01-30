package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Level.class)
public class Level_scarpetPlopMixin
{

    @Redirect(method = "getHeight", at = @At(
            value = "INVOKE",
            target = "net/minecraft/world/level/chunk/LevelChunk.getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"
    ))
    private int fixSampleHeightmap(LevelChunk chunk, Heightmap.Types type, int x, int z)
    {
        if (CarpetSettings.skipGenerationChecks.get())
        {
            Heightmap.Types newType = type;
            if (type == Heightmap.Types.OCEAN_FLOOR_WG) newType = Heightmap.Types.OCEAN_FLOOR;
            else if (type == Heightmap.Types.WORLD_SURFACE_WG) newType = Heightmap.Types.WORLD_SURFACE;
            return chunk.getHeight(newType, x, z);
        }
        return chunk.getHeight(type, x, z);
    }
}

