package carpet.fakes;

import net.minecraft.world.GameRules;
import java.util.Map;

public interface GameRulesInterface {
    Map<GameRules.Key<?>, GameRules.Type<?>> getRuleTypesCM();
}
