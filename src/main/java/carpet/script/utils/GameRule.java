package carpet.script.utils;

import carpet.fakes.GameRulesInterface;
import carpet.script.CarpetContext;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class GameRule {

    //Map of String gamerule name (e.g doMobLoot) to Key<BooleanRule or IntRule>.

    public static Map<String, GameRules.Key<?>> gamerules(CarpetContext cc){
        World world = cc.s.getWorld();

        Map<GameRules.Key<?>, GameRules.Type<?>> rule_types = ((GameRulesInterface)world.getGameRules()).getRuleTypesCM();

        Map<String, GameRules.Key<?>> ret_map = new HashMap<>();

        for(Map.Entry<GameRules.Key<?>, GameRules.Type<?>> e : rule_types.entrySet()){
            String k = e.getKey().getName();
            GameRules.Key<?> v = e.getKey();
            ret_map.put(k, v);
        }

        return ret_map;
    }
    //
    public static MapValue getAll(CarpetContext cc){
        Map<Value, Value> ret_map = new HashMap<>();
        for(Map.Entry<String, GameRules.Key<?>> e : gamerules(cc).entrySet()){
            Value k = StringValue.of(e.getKey());
            Value v = getRuleValue(e.getValue(),cc.s.getWorld());
            ret_map.put(k, v);
        }
        return MapValue.wrap(ret_map);
    }

    public static NumericValue getRuleValue(GameRules.Key rule, World world){

        try {//cos cant check for instanceof with gamerules for some reason
            return new NumericValue(world.getGameRules().getBoolean(rule));
        } catch(Exception exc){
            return new NumericValue(world.getGameRules().getInt(rule));
        }
    }
}