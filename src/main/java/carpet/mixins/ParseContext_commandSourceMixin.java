package carpet.mixins;

import carpet.helpers.CommandSourceRequirement;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.ParseContext;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParseContext.class)
public interface ParseContext_commandSourceMixin
{
    /***
     * It preserves the original command source for source-dependent Scarpet permission checks.
     * Vanilla’s ParseContext.of(...) retains permissions and registry information, but not the executing player/world. The mixin wraps that context with the original CommandSourceStack, which CommandSourceRequirement.test uses for "players", "server", and custom permission functions.
     * Without it, the current adapter falls back to an entity-less source:
     * •
     * Player-only commands reject players.
     * •
     * Server-only commands can incorrectly allow players.
     * •
     * Custom predicates lose their player/world context.
     * Numeric permission levels, ops, all, and particle parsing don’t need it. It is also separate from argumentNode’s context-copy helper: suggestion contexts already expose their entity.
     * Removing this mixin safely would require redesigning those source-dependent permission checks.
     * @param source
     * @param cir
     */

    @Inject(method = "of(Lnet/minecraft/commands/CommandSourceStack;)Lnet/minecraft/commands/ParseContext;",
            at = @At("RETURN"), cancellable = true)
    private static void retainCommandSource(CommandSourceStack source, CallbackInfoReturnable<ParseContext> cir)
    {
        cir.setReturnValue(CommandSourceRequirement.withSource(cir.getReturnValue(), source));
    }

    @Inject(method = "of(Lnet/minecraft/server/level/ServerPlayer;)Lnet/minecraft/commands/ParseContext;",
            at = @At("RETURN"), cancellable = true)
    private static void retainPlayerSource(ServerPlayer player, CallbackInfoReturnable<ParseContext> cir)
    {
        cir.setReturnValue(CommandSourceRequirement.withSource(cir.getReturnValue(), player.createCommandSourceStack()));
    }
}
