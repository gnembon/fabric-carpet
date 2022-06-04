package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(Level.class)
public class Level_creativeNoClipMixin {
    @Inject(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At(value = "TAIL"), cancellable = true)
    private void whatever(@Nullable Entity entity, AABB aABB, Predicate<? super Entity> predicate, CallbackInfoReturnable<List<Entity>> cir) {
        if (predicate == EntitySelector.NO_SPECTATORS) {
            cir.setReturnValue(cir.getReturnValue().stream().filter((item) -> {
                if (item instanceof Player player) {
                    return !(CarpetSettings.creativeNoClip && player.isCreative() && player.getAbilities().flying);
                } else {
                    return true;
                }
            }).toList());
        }
    }

    @Inject(method = "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At("TAIL"), cancellable = true)
    private void whatever2(EntityTypeTest<Entity, Entity> entityTypeTest, AABB aABB, Predicate<? super Entity> predicate, CallbackInfoReturnable<List<Entity>> cir) {
        if (predicate == EntitySelector.NO_SPECTATORS) {
            cir.setReturnValue(cir.getReturnValue().stream().filter((item) -> {
                if (item instanceof Player player) {
                    return !(CarpetSettings.creativeNoClip && player.isCreative() && player.getAbilities().flying);
                } else {
                    return true;
                }
            }).toList());
        }
    }

}
