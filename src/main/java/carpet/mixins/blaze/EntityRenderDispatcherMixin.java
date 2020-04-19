package carpet.mixins.blaze;

import carpet.patches.BlazeRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin
{
    @Shadow protected abstract <T extends Entity> void register(EntityType<T> entityType, EntityRenderer<? super T> entityRenderer);

    @Shadow @Final private Map<EntityType<?>, EntityRenderer<?>> renderers;

    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private <T extends Entity> void register(EntityType<T> entityType, EntityRenderer<? super T> entityRenderer, CallbackInfo ci)
    {
        if (entityType == EntityType.BLAZE)
        {
            renderers.put(entityType, new BlazeRenderer((EntityRenderDispatcher) (Object) this));
            ci.cancel();
        }
    }
}
