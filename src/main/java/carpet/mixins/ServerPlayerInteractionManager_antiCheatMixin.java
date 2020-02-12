package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = ServerPlayerInteractionManager.class, priority = 69420) // not that important for carpet
public class ServerPlayerInteractionManager_antiCheatMixin
{
    @Shadow public ServerPlayerEntity player;

    @ModifyConstant(method = "processBlockBreakingAction", require = 0,
            constant = @Constant(doubleValue = 36D))
    private double addDistance(double original) {
        if (CarpetSettings.antiCheatDisabled)
            return 1024D; // blocks 32 distance
        return original;
    }
}
