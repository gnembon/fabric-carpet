package carpet.mixins;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.model.ShulkerModel;

@Mixin(net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer.class)
public interface ShulkerBoxAccessMixin {
    @Accessor("model")
    ShulkerModel getModel();
}