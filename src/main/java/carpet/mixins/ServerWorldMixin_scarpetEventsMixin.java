package carpet.mixins;

import carpet.fakes.ServerWorldInterface;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.level.ServerWorldProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;


@Mixin(ServerWorld.class)
public class ServerWorldMixin_scarpetEventsMixin implements ServerWorldInterface {
    @Shadow private ServerWorldProperties worldProperties;
    public ServerWorldProperties getWorldPropertiesCM(){
        return worldProperties;
    }
}
