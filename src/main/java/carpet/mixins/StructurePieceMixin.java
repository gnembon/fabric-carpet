package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.structure.StructurePiece;
import net.minecraft.world.CollisionView;
import net.minecraft.world.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StructurePiece.class)
public class StructurePieceMixin
{
    @Redirect(method = "isUnderSeaLevel", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/CollisionView;getTop(Lnet/minecraft/world/Heightmap$Type;II)I"
    ))
    private int getTop(CollisionView world, Heightmap.Type var1, int var2, int var3)
    {
        return world.getTop(CarpetSettings.skipGenerationChecks?Heightmap.Type.OCEAN_FLOOR:Heightmap.Type.OCEAN_FLOOR_WG, var2, var3);
    }
}
