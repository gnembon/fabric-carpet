package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.structure.processor.GravityStructureProcessor;
import net.minecraft.world.CollisionView;
import net.minecraft.world.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GravityStructureProcessor.class)
public class GravityStructureProcessorMixin
{
    @Redirect(method = "process", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/CollisionView;getTop(Lnet/minecraft/world/Heightmap$Type;II)I"
    ))
    private int getTop(CollisionView world, Heightmap.Type type, int x, int z)
    {
        return world.getTop(CarpetSettings.skipGenerationChecks?Heightmap.Type.WORLD_SURFACE:type, x, z);
    }
}
