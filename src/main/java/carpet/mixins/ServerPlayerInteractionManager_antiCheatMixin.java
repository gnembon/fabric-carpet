package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManager_antiCheatMixin
{
    @Shadow public ServerPlayerEntity player;

    @ModifyConstant(method = "processBlockBreakingAction",
            constant = @Constant(doubleValue = 36D))
    private double addDistance(double original) {
        if (CarpetSettings.antiCheatDisabled)
            return 1024D; // blocks 32 distance
        return original;
    }
}
