package carpet.script.api;

import carpet.fakes.SpawnHelperInnerInterface;
import carpet.script.CarpetContext;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.utils.SpawnReporter;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.SpawnHelper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Monitoring {
    public static void apply(Expression expression)
    {
        // game processed snooper functions
        expression.addLazyFunction("get_mob_counts", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            SpawnHelper.Info info = world.getChunkManager().getSpawnInfo();
            if (info == null) return LazyValue.NULL;
            Object2IntMap<SpawnGroup> mobcounts = info.getGroupToCount();
            int chunks = ((SpawnHelperInnerInterface)info).cmGetChunkCount();
            Value retVal;
            if (lv.size() == 0)
            {
                Map<Value, Value> retDict = new HashMap<>();
                for (SpawnGroup category: mobcounts.keySet())
                {
                    int currentCap = (int)(category.getCapacity() * chunks / SpawnReporter.currentMagicNumber()); // MAGIC_NUMBER
                    retDict.put(
                            new StringValue(category.asString().toLowerCase(Locale.ROOT)),
                            ListValue.of(
                                    new NumericValue(mobcounts.getInt(category)),
                                    new NumericValue(currentCap))
                    );
                }
                retVal = MapValue.wrap(retDict);
            }
            else
            {
                String catString = lv.get(0).evalValue(c).getString();
                SpawnGroup cat = SpawnGroup.byName(catString.toLowerCase(Locale.ROOT));
                if (cat == null) throw new InternalExpressionException("Unreconized mob category: "+catString);
                retVal = ListValue.of(
                        new NumericValue(mobcounts.getInt(cat)),
                        new NumericValue((int)(cat.getCapacity() * chunks / SpawnReporter.currentMagicNumber()))
                );
            }
            return (_c, _t) -> retVal;
        });
    }
}
