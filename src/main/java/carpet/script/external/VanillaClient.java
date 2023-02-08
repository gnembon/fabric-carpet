package carpet.script.external;

import carpet.mixins.ShulkerBoxAccessMixin;
import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

public class VanillaClient
{
    public static ShulkerModel<?> ShulkerBoxRenderer_model(BlockEntityRenderer<ShulkerBoxBlockEntity> shulkerBoxRenderer) {
        return ((ShulkerBoxAccessMixin)shulkerBoxRenderer).getModel();
    }
}
