package carpet.utils;

import carpet.CarpetServer;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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

    public static void clearTracking(EntityType<? extends Entity> etype)
    {
        aiTrackers.remove(etype);
        for(ServerLevel world : CarpetServer.minecraft_server.getAllLevels() )
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

    public static List<String> availbleTypes()
    {
        Set<EntityType<?>> types = new HashSet<>();
        for (TrackingType type: TrackingType.values())
        {
            types.addAll(type.types);
        }
        return types.stream().map(t -> BuiltInRegistries.ENTITY_TYPE.getKey(t).getPath()).collect(Collectors.toList());
    }

    public static List<String> availableFor(EntityType<?> entityType)
    {
        Set<TrackingType> availableOptions = new HashSet<>();
        for (TrackingType type: TrackingType.values())
            if (type.types.contains(entityType))
                availableOptions.add(type);
        return availableOptions.stream().map(t -> t.name().toLowerCase()).collect(Collectors.toList());
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

    /**
     * Not a replacement for living entity jump() - this barely is to allow other entities that can't jump in vanilla to 'jump'
     * @param e
     */
    public static void genericJump(Entity e)
    {
        if (!e.isOnGround() && !e.isInWaterOrBubble() && !e.isInLava()) return;
        float m = e.level.getBlockState(e.blockPosition()).getBlock().getJumpFactor();
        float g = e.level.getBlockState(new BlockPos(e.getX(), e.getBoundingBox().minY - 0.5000001D, e.getZ())).getBlock().getJumpFactor();
        float jumpVelocityMultiplier = (double) m == 1.0D ? g : m;
        float jumpStrength = (0.42F * jumpVelocityMultiplier);
        Vec3 vec3d = e.getDeltaMovement();
        e.setDeltaMovement(vec3d.x, jumpStrength, vec3d.z);
        if (e.isSprinting())
        {
            float u = e.getYRot() * 0.017453292F; // yaw
            e.setDeltaMovement(e.getDeltaMovement().add((-Mth.sin(g) * 0.2F), 0.0D, (Mth.cos(u) * 0.2F)));
        }
        e.hasImpulse = true;
    }

}
