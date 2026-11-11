package carpet.mixins;

import carpet.helpers.CommandSourceRequirement;
import com.mojang.brigadier.tree.CommandNode;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBox;
import net.minecraft.server.permissions.PermissionSetSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CommandBox.class)
public class CommandBox_customRequirementsMixin
{
    @Inject(method = "convertNodePermissionCheck", at = @At("HEAD"), cancellable = true)
    private static <T extends PermissionSetSupplier> void convertCustomRequirement(
            Predicate<? extends PermissionSetSupplier> requirement, CallbackInfoReturnable<Predicate<T>> cir)
    {
        if (requirement instanceof CommandSourceRequirement<?> custom)
        {
            cir.setReturnValue(custom.cast());
        }
    }

    @Inject(method = "canNodeRun", at = @At("HEAD"), cancellable = true)
    private static void checkCustomRequirement(CommandNode<? extends PermissionSetSupplier> node,
                                              PermissionSetSupplier source, CallbackInfoReturnable<Boolean> cir)
    {
        if (node.getRequirement() instanceof CommandSourceRequirement<?> custom)
        {
            cir.setReturnValue(custom.cast().test(source));
        }
    }
}
