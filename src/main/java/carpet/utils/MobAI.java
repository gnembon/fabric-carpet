package carpet.utils;

import carpet.CarpetServer;
import com.google.common.collect.Sets;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MobAI
{
    private static Map<EntityType,Set<TrackingType>> aiTrackers = new HashMap<>();

    public static void resetTrackers()
    {
        aiTrackers.clear();
    }

    public static boolean isTracking(Entity e, TrackingType type)
    {
        if (e.getEntityWorld().isClient())
            return false;
        Set<TrackingType> currentTrackers = aiTrackers.get(e.getType());
        if (currentTrackers == null)
            return false;
        return currentTrackers.contains(type);
    }

    public static void clearTracking(EntityType etype)
    {
        aiTrackers.remove(etype);
        for(ServerWorld world : CarpetServer.minecraft_server.getWorlds() )
        {
            for (Entity e: world.getEntities(etype, Entity::hasCustomName))
            {
                e.setCustomNameVisible(false);
                e.setCustomName(null);
            }
        }
    }

    public static void startTracking(EntityType e, TrackingType type)
    {
        aiTrackers.putIfAbsent(e,Sets.newHashSet());
        aiTrackers.get(e).add(type);
    }

    public static List<String> availbleTypes()
    {
        Set<EntityType> types = new HashSet<>();
        for (TrackingType type: TrackingType.values())
        {
            types.addAll(type.types);
        }
        return types.stream().map(t -> Registry.ENTITY_TYPE.getId(t).getPath()).collect(Collectors.toList());
    }

    public static List<String> availableFor(EntityType<?> entityType)
    {
        Set<TrackingType> availableOptions = new HashSet<>();
        for (TrackingType type: TrackingType.values())
            for (EntityType etype: type.types)
                if (etype == entityType)
                    availableOptions.add(type);
        return availableOptions.stream().map(t -> t.name).collect(Collectors.toList());
    }

    public enum TrackingType
    {
        IRON_GOLEM_SPAWNING("iron_golem_spawning", Sets.newHashSet(EntityType.VILLAGER)),
        VILLAGER_BREEDING("breeding", Sets.newHashSet(EntityType.VILLAGER));
        public Set<EntityType> types;
        public String name;
        TrackingType(String name, Set<EntityType> applicableTypes)
        {
            this.name = name;
            types = applicableTypes;
        }

        public static TrackingType byName(String aspect)
        {
            for (TrackingType type: values())
                if (type.name.equalsIgnoreCase(aspect))
                    return type;
            return null;
        }
    }

    /**
     * Not a replacement for living entity jump() - this barely is to allow other entities that can't jump in vanilla to 'jump'
     * @param e
     */
    public static void genericJump(Entity e)
    {
        if (!e.onGround && !e.isInFluid(FluidTags.WATER) && !e.isInLava()) return;
        float m = e.world.getBlockState(new BlockPos(e)).getBlock().getJumpVelocityMultiplier();
        float g = e.world.getBlockState(new BlockPos(e.getX(), e.getBoundingBox().y1 - 0.5000001D, e.getZ())).getBlock().getJumpVelocityMultiplier();
        float jumpVelocityMultiplier = (double) m == 1.0D ? g : m;
        float jumpStrength = (0.42F * jumpVelocityMultiplier);
        Vec3d vec3d = e.getVelocity();
        e.setVelocity(vec3d.x, jumpStrength, vec3d.z);
        if (e.isSprinting())
        {
            float u = e.yaw * 0.017453292F;
            e.setVelocity(e.getVelocity().add((-MathHelper.sin(g) * 0.2F), 0.0D, (MathHelper.cos(u) * 0.2F)));
        }
        e.velocityDirty = true;
    }

}
