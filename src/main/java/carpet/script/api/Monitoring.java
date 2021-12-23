package carpet.script.api;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.annotation.ScarpetFunction;
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
import net.minecraft.world.SpawnHelper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class Monitoring {
    @ScarpetFunction
    public static Value system_info(Context c, String property) {
        Value res = SystemInfo.get(property, (CarpetContext) c);
        if (res == null) throw new InternalExpressionException("Unknown option for 'system_info': '" + property + "'");
        return res;
    }

    @ScarpetFunction(maxParams = 1)
    public static Value get_mob_counts(Context c, Optional<String> category) {
        SpawnHelper.Info info = ((CarpetContext)c).s.getWorld().getChunkManager().getSpawnInfo();
        if (info == null) return null;
        Object2IntMap<SpawnGroup> mobcounts = info.getGroupToCount();
        int chunks = info.getSpawningChunkCount();
        if (category.isPresent())
        {
            SpawnGroup group = SpawnGroup.byName(category.get().toLowerCase(Locale.ROOT));
            if (group == null) throw new InternalExpressionException("Unreconized mob category: " + category.get());
            return ListValue.of(
                    new NumericValue(mobcounts.getInt(group)),
                    new NumericValue((int)(group.getCapacity() * chunks / SpawnReporter.currentMagicNumber()))
            );
        }
        else
        {
            Map<Value, Value> retDict = new HashMap<>();
            for (SpawnGroup group: mobcounts.keySet())
            {
                int currentCap = (int)(group.getCapacity() * chunks / SpawnReporter.currentMagicNumber());
                retDict.put(
                        new StringValue(group.asString().toLowerCase(Locale.ROOT)),
                        ListValue.of(
                                new NumericValue(mobcounts.getInt(group)),
                                new NumericValue(currentCap))
                        );
            }
            return MapValue.wrap(retDict);
        }
    }
}
