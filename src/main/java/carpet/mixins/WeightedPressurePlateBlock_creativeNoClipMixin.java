package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WeightedPressurePlateBlock;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(WeightedPressurePlateBlock.class)

public class WeightedPressurePlateBlock_creativeNoClipMixin {
    @Redirect(method = "getSignalStrength", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private <T extends Entity> List<T> ignoreNoClip(Level instance, Class<T> aClass, AABB aabb) {
        // filter no-clip players out of the list of entities.
        return instance.getEntitiesOfClass(aClass, aabb).stream().filter((item) -> {
            if (item instanceof Player player) {
                return !(CarpetSettings.creativeNoClip && player.isCreative() && player.getAbilities().flying);
            } else {
                return true;
            }
        }).toList();
    }

}
