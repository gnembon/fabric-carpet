package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.structure.IglooGenerator;
import net.minecraft.world.Heightmap;
import net.minecraft.world.IWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(IglooGenerator.Piece.class)
public class IglooGeneratorPieceMixin
{
    @Redirect(method = "generate", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/IWorld;getTop(Lnet/minecraft/world/Heightmap$Type;II)I"
    ))
    private int getTop(IWorld iWorld, Heightmap.Type var1, int var2, int var3)
    {
        return iWorld.getTop(CarpetSettings.skipGenerationChecks?Heightmap.Type.WORLD_SURFACE:Heightmap.Type.WORLD_SURFACE_WG, var2, var3);
    }
}
