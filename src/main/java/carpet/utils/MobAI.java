package carpet.utils;

import carpet.CarpetServer;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

public class MobAI
{
    private static Map<EntityType<?>, Set<TrackingType>> aiTrackers = new HashMap<>();

    public static void resetTrackers()
    {
        aiTrackers.clear();
    }

    public static boolean isTracking(Entity e, TrackingType type)
    {
        if (e.getCommandSenderWorld().isClientSide())
            return false;
        Set<TrackingType> currentTrackers = aiTrackers.get(e.getType());
        if (currentTrackers == null)
            return false;
        return currentTrackers.contains(type);
    }

    public static void clearTracking(final MinecraftServer server, EntityType<? extends Entity> etype)
    {
        aiTrackers.remove(etype);
        for(ServerLevel world : server.getAllLevels() )
        {
            for (Entity e: world.getEntities(etype, Entity::hasCustomName))
            {
                e.setCustomNameVisible(false);
                e.setCustomName(null);
            }
        }
    }

    public static void startTracking(EntityType<?> e, TrackingType type)
    {
        aiTrackers.putIfAbsent(e,Sets.newHashSet());
        aiTrackers.get(e).add(type);
    }

    public static Stream<String> availbleTypes(CommandSourceStack source)
    {
        Set<EntityType<?>> types = new HashSet<>();
        for (TrackingType type: TrackingType.values())
        {
            types.addAll(type.types);
        }
        return types.stream().map(t -> source.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE).getKey(t).getPath());
    }

    public static Stream<String> availableFor(EntityType<?> entityType)
    {
        Set<TrackingType> availableOptions = new HashSet<>();
        for (TrackingType type: TrackingType.values())
            if (type.types.contains(entityType))
                availableOptions.add(type);
        return availableOptions.stream().map(t -> t.name().toLowerCase());
    }

    public enum TrackingType
    {
        IRON_GOLEM_SPAWNING(Set.of(EntityType.VILLAGER)),
        BREEDING(Set.of(EntityType.VILLAGER));
        public final Set<EntityType<?>> types;
        TrackingType(Set<EntityType<?>> applicableTypes)
        {
            types = applicableTypes;
        }
    }
}
