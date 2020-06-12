package carpet.mixins;

import carpet.fakes.HungerManagerInterface;
import net.minecraft.entity.player.HungerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(HungerManager.class)
public class HungerManagerMixin_scarpetEventsMixin implements HungerManagerInterface {
    @Shadow private float exhaustion;
    public float getExhaustionCM(){
        return exhaustion;
    }

    @Override
    public void setExhaustionCM(float exhaust)
    {
        exhaustion = exhaust;
    }
}
