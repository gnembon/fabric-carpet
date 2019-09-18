package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.class_4538;
import net.minecraft.structure.processor.GravityStructureProcessor;
import net.minecraft.world.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GravityStructureProcessor.class)
public class GravityStructureProcessorMixin
{
    @Redirect(method = "process", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_4538;getTopY(Lnet/minecraft/world/Heightmap$Type;II)I"
    ))
    private int getTop(class_4538 class_4538, Heightmap.Type var1, int var2, int var3)
    {
        return class_4538.getTopY(CarpetSettings.skipGenerationChecks?Heightmap.Type.WORLD_SURFACE:var1, var2, var3);
    }
}
