package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.structure.processor.GravityStructureProcessor;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ViewableWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GravityStructureProcessor.class)
public class GravityStructureProcessorMixin
{
    @Redirect(method = "process", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/ViewableWorld;getTop(Lnet/minecraft/world/Heightmap$Type;II)I"
    ))
    private int getTop(ViewableWorld viewableWorld, Heightmap.Type var1, int var2, int var3)
    {
        return viewableWorld.getTop(CarpetSettings.skipGenerationChecks?Heightmap.Type.WORLD_SURFACE:var1, var2, var3);
    }
}
