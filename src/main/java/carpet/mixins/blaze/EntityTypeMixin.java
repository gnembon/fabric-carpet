package carpet.mixins.blaze;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public class EntityTypeMixin<T extends Entity>
{
    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private static <T extends Entity> void registerBlaze(String id, EntityType.Builder<T> type, CallbackInfoReturnable<EntityType<T>> cir)
    {
        if ("blaze".equals(id))
        {
            cir.setReturnValue((EntityType) Registry.register(Registry.ENTITY_TYPE, (String)id, EntityType.Builder.create(BlazeEntity::new, EntityCategory.MONSTER).makeFireImmune().setDimensions(0.35F, 0.99F).build(id)));
        }
    }
}
