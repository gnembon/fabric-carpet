package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.PressurePlateBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PressurePlateBlock.class)
public class PressurePlateBlock_creativeNoClipMixin {
    @Redirect(method="getSignalStrength", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;isIgnoringBlockTriggers()Z"))
    private boolean ignoreNoClip(Entity instance) {
        if (instance instanceof Player player) {
            if (CarpetSettings.creativeNoClip && player.isCreative() && player.getAbilities().flying) {
                return true;
            }
        }
        return instance.isIgnoringBlockTriggers();
    }
}
