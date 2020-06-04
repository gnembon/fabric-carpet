package carpet.mixins;

import carpet.fakes.EntityInterface;
import carpet.fakes.HungerManagerInterface;
import carpet.script.EntityEventsGroup;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.HungerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HungerManager.class)
public class HungerManagerMixin_scarpetEventsMixin implements HungerManagerInterface {
    @Shadow private float exhaustion;
    public float getExhaustion(){
        return exhaustion;
    }
}