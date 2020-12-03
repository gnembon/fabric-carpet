package carpet.mixins;

import carpet.fakes.GameRulesInterface;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(GameRules.class)
public class GameRules_scarpetMixin implements GameRulesInterface {
    @Shadow
    private static Map<GameRules.Key<?>, GameRules.Type<?>> RULE_TYPES;
    public Map<GameRules.Key<?>, GameRules.Type<?>> getRuleTypesCM(){
        return RULE_TYPES;
    }
}
