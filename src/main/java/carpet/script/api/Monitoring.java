package carpet.script.api;

import carpet.fakes.SpawnHelperInnerInterface;
import carpet.script.CarpetContext;
import carpet.script.Expression;
import carpet.script.exception.InternalExpressionException;
import carpet.script.utils.SystemInfo;
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
        expression.addContextFunction("system_info", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
            {
                return SystemInfo.getAll((CarpetContext) c);
            }
            if (lv.size() == 1) {
                String what = lv.get(0).getString();
                Value res = SystemInfo.get(what, (CarpetContext) c);
                if (res == null) throw new InternalExpressionException("Unknown option for 'system_info': " + what);
                return res;
            }
            throw new InternalExpressionException("'system_info' requires one or no parameters");
        });
        // game processed snooper functions
        expression.addContextFunction("get_mob_counts", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            SpawnHelper.Info info = world.getChunkManager().getSpawnInfo();
            if (info == null) return Value.NULL;
            Object2IntMap<SpawnGroup> mobcounts = info.getGroupToCount();
            int chunks = ((SpawnHelperInnerInterface)info).cmGetChunkCount();
            if (lv.size() == 0)
            {
                Map<Value, Value> retDict = new HashMap<>();
                for (SpawnGroup category: mobcounts.keySet())
                {
                    int currentCap = (int)(category.getCapacity() * chunks / SpawnReporter.MAGIC_NUMBER);
                    retDict.put(
                            new StringValue(category.asString().toLowerCase(Locale.ROOT)),
                            ListValue.of(
                                    new NumericValue(mobcounts.getInt(category)),
                                    new NumericValue(currentCap))
                    );
                }
                return MapValue.wrap(retDict);
            }
            String catString = lv.get(0).getString();
            SpawnGroup cat = SpawnGroup.byName(catString.toLowerCase(Locale.ROOT));
            if (cat == null) throw new InternalExpressionException("Unreconized mob category: "+catString);
            return ListValue.of(
                    new NumericValue(mobcounts.getInt(cat)),
                    new NumericValue((int)(cat.getCapacity() * chunks / SpawnReporter.MAGIC_NUMBER))
            );
        });
    }
}
