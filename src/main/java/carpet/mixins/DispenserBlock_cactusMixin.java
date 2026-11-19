package carpet.mixins;

import carpet.CarpetSettings;
import carpet.helpers.BlockRotator;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.dispenser.DispenseBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DispenserBlock.class)
public abstract class DispenserBlock_cactusMixin
{
    @Inject(method = "getDispenseMethods", at = @At("RETURN"), cancellable = true)
    private void registerCarpetBehaviors(ServerLevel level, ItemStack stack, CallbackInfoReturnable<List<DispenseBehavior>> cir)
    {
        Item item = stack.getItem();
        if (CarpetSettings.rotatorBlock && item == Items.CACTUS)
        {
            List<DispenseBehavior> behaviors = new ArrayList<>(cir.getReturnValue());
            behaviors.add(0, new BlockRotator.CactusDispenserBehaviour());
            cir.setReturnValue(behaviors);
        }
    }
}
