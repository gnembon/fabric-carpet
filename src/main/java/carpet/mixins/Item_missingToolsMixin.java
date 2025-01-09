package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class Item_missingToolsMixin
{
    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    public void getDestroySpeed(ItemStack itemStack, BlockState blockState, CallbackInfoReturnable<Float> cir) {
        if (CarpetSettings.missingTools && blockState.getSoundType() == SoundType.GLASS && itemStack.is(ItemTags.PICKAXES))
        {
            Tool tool = itemStack.get(DataComponents.TOOL);
            if (tool != null) {
                cir.setReturnValue(tool.getMiningSpeed(Blocks.STONE.defaultBlockState()));
            }
        }
    }
}
