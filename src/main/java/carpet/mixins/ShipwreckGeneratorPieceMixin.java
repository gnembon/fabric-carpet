package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.structure.ShipwreckGenerator;
import net.minecraft.world.Heightmap;
import net.minecraft.world.IWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ShipwreckGenerator.Piece.class)
public class ShipwreckGeneratorPieceMixin
{
    @Shadow @Final private boolean grounded;

    @Redirect(method = "generate", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/IWorld;getTop(Lnet/minecraft/world/Heightmap$Type;II)I"
    ))
    private int getTop(IWorld iWorld, Heightmap.Type var1, int var2, int var3)
    {
        if (grounded)
            return iWorld.getTop(CarpetSettings.skipGenerationChecks?Heightmap.Type.WORLD_SURFACE:Heightmap.Type.WORLD_SURFACE_WG, var2, var3);
        else
            return iWorld.getTop(CarpetSettings.skipGenerationChecks?Heightmap.Type.OCEAN_FLOOR:Heightmap.Type.OCEAN_FLOOR_WG, var2, var3);
    }
}
