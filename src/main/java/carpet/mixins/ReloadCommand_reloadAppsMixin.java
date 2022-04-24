package carpet.mixins;

import carpet.CarpetServer;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.ReloadCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ReloadCommand.class)
public class ReloadCommand_reloadAppsMixin {
    //method_13530(Lcom/mojang/brigadier/context/CommandContext;)I
    // internal of register.
    @Inject(method = "method_13530", at = @At("TAIL"), remap = false)
    private static void onReload(CommandContext<CommandSourceStack> context, CallbackInfoReturnable<Integer> cir)
    {
        // can't fetch here the reference to the server
        CarpetServer.onReload(context.getSource().getServer());
    }
}
