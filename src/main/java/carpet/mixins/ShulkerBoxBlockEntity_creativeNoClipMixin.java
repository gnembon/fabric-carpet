package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ShulkerBoxBlockEntity.class)
public class ShulkerBoxBlockEntity_creativeNoClipMixin
{
    @Redirect(method = "pushEntities", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getPistonBehavior()Lnet/minecraft/block/piston/PistonBehavior;"
    ))
    private PistonBehavior getPistonBehaviourOfNoClipPlayers(Entity entity)
    {
        if (CarpetSettings.creativeNoClip && entity instanceof PlayerEntity && (((PlayerEntity) entity).isCreative()) && ((PlayerEntity) entity).abilities.flying)
            return PistonBehavior.IGNORE;
        return entity.getPistonBehavior();
    }
}
