package carpet.script.api;

import carpet.script.CarpetContext;
import carpet.script.Expression;
import carpet.script.exception.InternalExpressionException;
import carpet.script.external.Vanilla;
import carpet.script.utils.SystemInfo;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;

public class Monitoring
{
    private static final Map<String, MobCategory> MOB_CATEGORY_MAP = Arrays.stream(MobCategory.values()).collect(Collectors.toMap(MobCategory::getName, Function.identity()));

    public static void apply(final Expression expression)
    {
        expression.addContextFunction("system_info", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
            {
                return SystemInfo.getAll();
            }
            if (lv.size() == 1)
            {
                final String what = lv.get(0).getString();
                final Value res = SystemInfo.get(what, (CarpetContext) c);
                if (res == null)
                {
                    throw new InternalExpressionException("Unknown option for 'system_info': " + what);
                }
                return res;
            }
            throw new InternalExpressionException("'system_info' requires one or no parameters");
        });
        // game processed snooper functions
        expression.addContextFunction("get_mob_counts", -1, (c, t, lv) ->
        {
            final CarpetContext cc = (CarpetContext) c;
            final ServerLevel world = cc.level();
            final NaturalSpawner.SpawnState info = world.getChunkSource().getLastSpawnState();
            if (info == null)
            {
                return Value.NULL;
            }
            final Object2IntMap<MobCategory> mobcounts = info.getMobCategoryCounts();
            final int chunks = info.getSpawnableChunkCount();
            if (lv.size() == 0)
            {
                final Map<Value, Value> retDict = new HashMap<>();
                for (final MobCategory category : mobcounts.keySet())
                {
                    final int currentCap = category.getMaxInstancesPerChunk() * chunks / Vanilla.NaturalSpawner_MAGIC_NUMBER();
                    retDict.put(
                            new StringValue(category.getSerializedName().toLowerCase(Locale.ROOT)),
                            ListValue.of(
                                    new NumericValue(mobcounts.getInt(category)),
                                    new NumericValue(currentCap))
                    );
                }
                return MapValue.wrap(retDict);
            }
            final String catString = lv.get(0).getString();
            final MobCategory cat = MOB_CATEGORY_MAP.get(catString.toLowerCase(Locale.ROOT));
            if (cat == null)
            {
                throw new InternalExpressionException("Unreconized mob category: " + catString);
            }
            return ListValue.of(
                    new NumericValue(mobcounts.getInt(cat)),
                    new NumericValue((int) (cat.getMaxInstancesPerChunk() * chunks / Vanilla.NaturalSpawner_MAGIC_NUMBER()))
            );
        });
    }
}
