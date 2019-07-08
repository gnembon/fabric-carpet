package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.item.PickaxeItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PickaxeItem.class)
public class PickaxeItem_missingToolsMixin
{
    @Redirect(method = "getMiningSpeed", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/BlockState;getMaterial()Lnet/minecraft/block/Material;"
    ))
    private Material getCustomMaterial(BlockState blockState)
    {
        Material material = blockState.getMaterial();
        if (CarpetSettings.missingTools && (material == Material.PISTON || material == Material.GLASS))
            material = Material.STONE;
        return material;
    }
}
