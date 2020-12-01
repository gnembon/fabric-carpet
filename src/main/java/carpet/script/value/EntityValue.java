package carpet.script.value;

import carpet.fakes.BrainInterface;
import carpet.fakes.EntityInterface;
import carpet.fakes.ItemEntityInterface;
import carpet.fakes.LivingEntityInterface;
import carpet.fakes.MemoryInterface;
import carpet.fakes.MobEntityInterface;
import carpet.fakes.HungerManagerInterface;
import carpet.fakes.ServerPlayerInteractionManagerInterface;
import carpet.helpers.Tracer;
import carpet.patches.EntityPlayerMPFake;
import carpet.script.CarpetContext;
import carpet.script.EntityEventsGroup;
import carpet.script.argument.Vector3Argument;
import carpet.script.exception.InternalExpressionException;
import com.google.common.collect.Sets;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.Memory;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.GoToWalkTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static carpet.script.value.NBTSerializableValue.nameFromRegistryId;
import static carpet.utils.MobAI.genericJump;

// TODO: decide whether copy(entity) should duplicate entity in the world.
public class EntityValue extends Value
{
    private final Entity entity;

    public EntityValue(Entity e)
    {
        entity = e;
    }

    private static final Map<String, EntitySelector> selectorCache = new HashMap<>();
    public static Collection<? extends Entity > getEntitiesFromSelector(ServerCommandSource source, String selector)
    {
        try
        {
            EntitySelector entitySelector = selectorCache.get(selector);
            if (entitySelector != null)
            {
                return entitySelector.getEntities(source.withMaxLevel(4));
            }
            entitySelector = new EntitySelectorReader(new StringReader(selector), true).read();
            selectorCache.put(selector, entitySelector);
            return entitySelector.getEntities(source.withMaxLevel(4));
        }
        catch (CommandSyntaxException e)
        {
            throw new InternalExpressionException("Cannot select entities from "+selector);
        }
    }

    public Entity getEntity()
    {
        return entity;
    }

    public static ServerPlayerEntity getPlayerByValue(MinecraftServer server, Value value)
    {
        ServerPlayerEntity player = null;
        if (value instanceof EntityValue)
        {
            Entity e = ((EntityValue) value).getEntity();
            if (e instanceof ServerPlayerEntity)
            {
                player = (ServerPlayerEntity) e;
            }
        }
        else if (value.isNull())
        {
            return null;
        }
        else
        {
            String playerName = value.getString();
            player = server.getPlayerManager().getPlayer(playerName);
        }
        return player;
    }

    public static String getPlayerNameByValue(Value value)
    {
        String playerName = null;
        if (value instanceof EntityValue)
        {
            Entity e = ((EntityValue) value).getEntity();
            if (e instanceof ServerPlayerEntity)
            {
                playerName = e.getEntityName();
            }
        }
        else if (value.isNull())
        {
            return null;
        }
        else
        {
            playerName = value.getString();
        }
        return playerName;
    }

    @Override
    public String getString()
    {
        return entity.getName().getString();
    }

    @Override
    public boolean getBoolean()
    {
        return true;
    }

    @Override
    public boolean equals(Object v)
    {
        if (v instanceof EntityValue)
        {
            return entity.getEntityId()==((EntityValue) v).entity.getEntityId();
        }
        return super.equals((Value)v);
    }

    @Override
    public Value in(Value v)
    {
        if (v instanceof ListValue)
        {
            List<Value> values = ((ListValue) v).getItems();
            String what = values.get(0).getString();
            Value arg = null;
            if (values.size() == 2)
            {
                arg = values.get(1);
            }
            else if (values.size() > 2)
            {
                arg = ListValue.wrap(values.subList(1,values.size()));
            }
            return this.get(what, arg);
        }
        String what = v.getString();
        return this.get(what, null);
    }

    @Override
    public String getTypeString()
    {
        return "entity";
    }

    @Override
    public int hashCode()
    {
        return entity.hashCode();
    }

    public static EntityClassDescriptor getEntityDescriptor(String who)
    {
        EntityClassDescriptor eDesc = EntityClassDescriptor.byName.get(who);
        if (eDesc == null) throw new InternalExpressionException(who+" is not a valid entity descriptor");
        return eDesc;
        //TODO add more here like search by tags, or type
        //if (who.startsWith('tag:'))
    }

    public static class EntityClassDescriptor
    {
        public EntityType<? extends Entity> directType;
        public Predicate<? super Entity> filteringPredicate;
        public List<EntityType<? extends  Entity>> typeList;
        public Value listValue;
        EntityClassDescriptor(EntityType<?> type, Predicate<? super Entity> predicate, List<EntityType<?>> types)
        {
            directType = type;
            filteringPredicate = predicate;
            typeList = types;
            listValue = (types==null)?Value.NULL:ListValue.wrap(types.stream().map(et -> StringValue.of(nameFromRegistryId(Registry.ENTITY_TYPE.getId(et)))).collect(Collectors.toList()));
        }
        EntityClassDescriptor(EntityType<?> type, Predicate<? super Entity> predicate, Stream<EntityType<?>> types)
        {
            this(type, predicate, types.collect(Collectors.toList()));
        }

        public final static Map<String, EntityClassDescriptor> byName = new HashMap<String, EntityClassDescriptor>() {{
            List<EntityType<?>> allTypes = Registry.ENTITY_TYPE.stream().collect(Collectors.toList());
            // nonliving types
            Set<EntityType<?>> projectiles = Sets.newHashSet(
                    EntityType.ARROW, EntityType.DRAGON_FIREBALL, EntityType.FIREWORK_ROCKET,
                    EntityType.FIREBALL, EntityType.LLAMA_SPIT, EntityType.SMALL_FIREBALL,
                    EntityType.SNOWBALL, EntityType.SPECTRAL_ARROW, EntityType.EGG,
                    EntityType.ENDER_PEARL, EntityType.EXPERIENCE_BOTTLE, EntityType.POTION,
                    EntityType.TRIDENT, EntityType.WITHER_SKULL, EntityType.FISHING_BOBBER
            );
            Set<EntityType<?>> deads = Sets.newHashSet(
                    EntityType.AREA_EFFECT_CLOUD, EntityType.BOAT, EntityType.END_CRYSTAL,
                    EntityType.EVOKER_FANGS, EntityType.EXPERIENCE_ORB, EntityType.EYE_OF_ENDER,
                    EntityType.FALLING_BLOCK, EntityType.ITEM, EntityType.ITEM_FRAME,
                    EntityType.LEASH_KNOT, EntityType.LIGHTNING_BOLT, EntityType.PAINTING,
                    EntityType.TNT

            );
            Set<EntityType<?>> minecarts = Sets.newHashSet(
                   EntityType.MINECART,  EntityType.CHEST_MINECART, EntityType.COMMAND_BLOCK_MINECART,
                    EntityType.FURNACE_MINECART, EntityType.HOPPER_MINECART,
                    EntityType.SPAWNER_MINECART, EntityType.TNT_MINECART
            );
            // living mob groups - non-defeault
            Set<EntityType<?>> undeads = Sets.newHashSet(
                    EntityType.STRAY, EntityType.SKELETON, EntityType.WITHER_SKELETON,
                    EntityType.ZOMBIE, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER,
                    EntityType.ZOMBIE_HORSE, EntityType.SKELETON_HORSE, EntityType.PHANTOM,
                    EntityType.WITHER, EntityType.ZOGLIN, EntityType.HUSK, EntityType.ZOMBIFIED_PIGLIN

            );
            Set<EntityType<?>> arthropods = Sets.newHashSet(
                    EntityType.BEE, EntityType.ENDERMITE, EntityType.SILVERFISH, EntityType.SPIDER,
                    EntityType.CAVE_SPIDER
            );
            Set<EntityType<?>> aquatique = Sets.newHashSet(
                    EntityType.GUARDIAN, EntityType.TURTLE, EntityType.COD, EntityType.DOLPHIN, EntityType.PUFFERFISH,
                    EntityType.SALMON, EntityType.SQUID, EntityType.TROPICAL_FISH
            );
            Set<EntityType<?>> illagers = Sets.newHashSet(
                    EntityType.PILLAGER, EntityType.ILLUSIONER, EntityType.VINDICATOR, EntityType.EVOKER,
                    EntityType.RAVAGER, EntityType.WITCH
            );

            Set<EntityType<?>> living = allTypes.stream().filter(et ->
                    !deads.contains(et) && !projectiles.contains(et) && !minecarts.contains(et)
            ).collect(Collectors.toSet());

            Set<EntityType<?>> regular = allTypes.stream().filter(et ->
                    living.contains(et) && !undeads.contains(et) && !arthropods.contains(et) && !aquatique.contains(et) && !illagers.contains(et)
            ).collect(Collectors.toSet());


            put("*", new EntityClassDescriptor(null, e -> true, allTypes) );
            put("valid", new EntityClassDescriptor(null, EntityPredicates.VALID_ENTITY, allTypes));
            put("!valid", new EntityClassDescriptor(null, e -> !e.isAlive(), allTypes));

            put("living",  new EntityClassDescriptor(null, (e) -> (e instanceof LivingEntity && e.isAlive()), allTypes.stream().filter(living::contains)));
            put("!living",  new EntityClassDescriptor(null, (e) -> (!(e instanceof LivingEntity) && e.isAlive()), allTypes.stream().filter(et -> !living.contains(et))));

            put("projectile", new EntityClassDescriptor(null, (e) -> (e instanceof ProjectileEntity && e.isAlive()), allTypes.stream().filter(projectiles::contains)));
            put("!projectile", new EntityClassDescriptor(null, (e) -> (!(e instanceof ProjectileEntity) && e.isAlive()), allTypes.stream().filter(et -> !projectiles.contains(et) && !living.contains(et))));

            put("minecarts", new EntityClassDescriptor(null, (e) -> (e instanceof AbstractMinecartEntity && e.isAlive()), allTypes.stream().filter(minecarts::contains)));
            put("!minecarts", new EntityClassDescriptor(null, (e) -> (!(e instanceof AbstractMinecartEntity) && e.isAlive()), allTypes.stream().filter(et -> !minecarts.contains(et) && !living.contains(et))));


            // combat groups

            put("arthropod", new EntityClassDescriptor(null, e -> ((e instanceof LivingEntity) && ((LivingEntity) e).getGroup() == EntityGroup.ARTHROPOD && e.isAlive()), allTypes.stream().filter(arthropods::contains)));
            put("!arthropod", new EntityClassDescriptor(null, e -> ((e instanceof LivingEntity) && ((LivingEntity) e).getGroup() != EntityGroup.ARTHROPOD && e.isAlive()), allTypes.stream().filter(et -> !arthropods.contains(et) && living.contains(et))));

            put("undead", new EntityClassDescriptor(null, e -> ((e instanceof LivingEntity) && ((LivingEntity) e).getGroup() == EntityGroup.UNDEAD && e.isAlive()), allTypes.stream().filter(undeads::contains)));
            put("!undead", new EntityClassDescriptor(null, e -> ((e instanceof LivingEntity) && ((LivingEntity) e).getGroup() != EntityGroup.UNDEAD && e.isAlive()), allTypes.stream().filter(et -> !undeads.contains(et) && living.contains(et))));

            put("aquatic", new EntityClassDescriptor(null, e -> ((e instanceof LivingEntity) && ((LivingEntity) e).getGroup() == EntityGroup.AQUATIC && e.isAlive()), allTypes.stream().filter(aquatique::contains)));
            put("!aquatic", new EntityClassDescriptor(null, e -> ((e instanceof LivingEntity) && ((LivingEntity) e).getGroup() != EntityGroup.AQUATIC && e.isAlive()), allTypes.stream().filter(et -> !aquatique.contains(et) && living.contains(et))));

            put("illager", new EntityClassDescriptor(null, e -> ((e instanceof LivingEntity) && ((LivingEntity) e).getGroup() == EntityGroup.ILLAGER && e.isAlive()), allTypes.stream().filter(illagers::contains)));
            put("!illager", new EntityClassDescriptor(null, e -> ((e instanceof LivingEntity) && ((LivingEntity) e).getGroup() != EntityGroup.ILLAGER && e.isAlive()), allTypes.stream().filter(et -> !illagers.contains(et) && living.contains(et))));

            put("regular", new EntityClassDescriptor(null, e -> ((e instanceof LivingEntity) && ((LivingEntity) e).getGroup() == EntityGroup.DEFAULT && e.isAlive()), allTypes.stream().filter(regular::contains)));
            put("!regular", new EntityClassDescriptor(null, e -> ((e instanceof LivingEntity) && ((LivingEntity) e).getGroup() != EntityGroup.DEFAULT && e.isAlive()), allTypes.stream().filter(et -> !regular.contains(et) && living.contains(et))));

            for (Identifier typeId : Registry.ENTITY_TYPE.getIds())
            {
                EntityType<?> type  = Registry.ENTITY_TYPE.get(typeId);
                String mobType = ValueConversions.simplify(typeId);
                put(    mobType, new EntityClassDescriptor(type, EntityPredicates.VALID_ENTITY, Stream.of(type)));
                put("!"+mobType, new EntityClassDescriptor(null, (e) -> e.getType() != type  && e.isAlive(), allTypes.stream().filter(et -> et != type)));
            }
            for (SpawnGroup catId : SpawnGroup.values())
            {
                String catStr = catId.getName();
                put(    catStr, new EntityClassDescriptor(null, e -> ((e.getType().getSpawnGroup() == catId) && e.isAlive()), allTypes.stream().filter(et -> et.getSpawnGroup() == catId)));
                put("!"+catStr, new EntityClassDescriptor(null, e -> ((e.getType().getSpawnGroup() != catId) && e.isAlive()), allTypes.stream().filter(et -> et.getSpawnGroup() != catId)));
            }
        }};
    }

    public Value get(String what, Value arg)
    {
        if (!(featureAccessors.containsKey(what)))
            throw new InternalExpressionException("Unknown entity feature: "+what);
        try
        {
            return featureAccessors.get(what).apply(entity, arg);
        }
        catch (NullPointerException npe)
        {
            throw new InternalExpressionException("Cannot fetch '"+what+"' with these arguments");
        }
    }
    private static final Map<String, EquipmentSlot> inventorySlots = new HashMap<String, EquipmentSlot>(){{
        put("mainhand", EquipmentSlot.MAINHAND);
        put("offhand", EquipmentSlot.OFFHAND);
        put("head", EquipmentSlot.HEAD);
        put("chest", EquipmentSlot.CHEST);
        put("legs", EquipmentSlot.LEGS);
        put("feet", EquipmentSlot.FEET);
    }};

    private static final Map<String, BiFunction<Entity, Value, Value>> featureAccessors = new HashMap<String, BiFunction<Entity, Value, Value>>() {{
        //put("test", (e, a) -> a == null ? Value.NULL : new StringValue(a.getString()));
        put("removed", (entity, arg) -> new NumericValue(entity.removed));
        put("uuid",(e, a) -> new StringValue(e.getUuidAsString()));
        put("id",(e, a) -> new NumericValue(e.getEntityId()));
        put("pos", (e, a) -> ListValue.of(new NumericValue(e.getX()), new NumericValue(e.getY()), new NumericValue(e.getZ())));
        put("location", (e, a) -> ListValue.of(new NumericValue(e.getX()), new NumericValue(e.getY()), new NumericValue(e.getZ()), new NumericValue(e.yaw), new NumericValue(e.pitch)));
        put("x", (e, a) -> new NumericValue(e.getX()));
        put("y", (e, a) -> new NumericValue(e.getY()));
        put("z", (e, a) -> new NumericValue(e.getZ()));
        put("motion", (e, a) ->
        {
            Vec3d velocity = e.getVelocity();
            return ListValue.of(new NumericValue(velocity.x), new NumericValue(velocity.y), new NumericValue(velocity.z));
        });
        put("motion_x", (e, a) -> new NumericValue(e.getVelocity().x));
        put("motion_y", (e, a) -> new NumericValue(e.getVelocity().y));
        put("motion_z", (e, a) -> new NumericValue(e.getVelocity().z));
        put("on_ground", (e, a) -> new NumericValue(e.isOnGround()));
        put("name", (e, a) -> new StringValue(e.getName().getString()));
        put("display_name", (e, a) -> new StringValue(e.getDisplayName().getString()));
        put("command_name", (e, a) -> new StringValue(e.getEntityName()));
        put("custom_name", (e, a) -> e.hasCustomName()?new StringValue(e.getCustomName().getString()):Value.NULL);
        put("type", (e, a) -> new StringValue(nameFromRegistryId(Registry.ENTITY_TYPE.getId(e.getType()))));
        put("is_riding", (e, a) -> new NumericValue(e.hasVehicle()));
        put("is_ridden", (e, a) -> new NumericValue(e.hasPassengers()));
        put("passengers", (e, a) -> ListValue.wrap(e.getPassengerList().stream().map(EntityValue::new).collect(Collectors.toList())));
        put("mount", (e, a) -> (e.getVehicle()!=null)?new EntityValue(e.getVehicle()):Value.NULL);
        put("unmountable", (e, a) -> new NumericValue(((EntityInterface)e).isPermanentVehicle()));
        put("tags", (e, a) -> ListValue.wrap(e.getScoreboardTags().stream().map(StringValue::new).collect(Collectors.toList())));
        put("has_tag", (e, a) -> new NumericValue(e.getScoreboardTags().contains(a.getString())));
        put("yaw", (e, a)-> new NumericValue(e.yaw));
        put("head_yaw", (e, a)-> {
            if (e instanceof LivingEntity)
            {
                return  new NumericValue(e.getHeadYaw());
            }
            return Value.NULL;
        });
        put("body_yaw", (e, a)-> {
            if (e instanceof LivingEntity)
            {
                return  new NumericValue(((LivingEntity) e).bodyYaw);
            }
            return Value.NULL;
        });

        put("pitch", (e, a)-> new NumericValue(e.pitch));
        put("look", (e, a) -> {
            Vec3d look = e.getRotationVector();
            return ListValue.of(new NumericValue(look.x),new NumericValue(look.y),new NumericValue(look.z));
        });
        put("is_burning", (e, a) -> new NumericValue(e.isOnFire()));
        //put("fire", (e, a) -> new NumericValue(e.getFire())); needs mixing
        put("silent", (e, a)-> new NumericValue(e.isSilent()));
        put("gravity", (e, a) -> new NumericValue(!e.hasNoGravity()));
        put("immune_to_fire", (e, a) -> new NumericValue(e.isFireImmune()));

        put("invulnerable", (e, a) -> new NumericValue(e.isInvulnerable()));
        put("dimension", (e, a) -> new StringValue(nameFromRegistryId(e.world.getRegistryKey().getValue()))); // getDimId
        put("height", (e, a) -> new NumericValue(e.getDimensions(EntityPose.STANDING).height));
        put("width", (e, a) -> new NumericValue(e.getDimensions(EntityPose.STANDING).width));
        put("eye_height", (e, a) -> new NumericValue(e.getStandingEyeHeight()));
        put("age", (e, a) -> new NumericValue(e.age));
        put("breeding_age", (e, a) -> e instanceof PassiveEntity?new NumericValue(((PassiveEntity) e).getBreedingAge()):Value.NULL);
        put("despawn_timer", (e, a) -> e instanceof LivingEntity?new NumericValue(((LivingEntity) e).getDespawnCounter()):Value.NULL);
        put("item", (e, a) -> (e instanceof ItemEntity)?ListValue.fromItemStack(((ItemEntity) e).getStack()):Value.NULL);
        put("count", (e, a) -> (e instanceof ItemEntity)?new NumericValue(((ItemEntity) e).getStack().getCount()):Value.NULL);
        put("pickup_delay", (e, a) -> (e instanceof ItemEntity)?new NumericValue(((ItemEntityInterface) e).getPickupDelayCM()):Value.NULL);
        put("portal_cooldown", (e , a) ->new NumericValue(((EntityInterface)e).getPortalTimer()));
        put("portal_timer", (e , a) ->new NumericValue(((EntityInterface)e).getPublicNetherPortalCooldown()));
        // ItemEntity -> despawn timer via ssGetAge
        put("is_baby", (e, a) -> (e instanceof LivingEntity)?new NumericValue(((LivingEntity) e).isBaby()):Value.NULL);
        put("target", (e, a) -> {
            if (e instanceof MobEntity)
            {
                LivingEntity target = ((MobEntity) e).getTarget(); // there is also getAttacking in living....
                if (target != null)
                {
                    return new EntityValue(target);
                }
            }
            return Value.NULL;
        });
        put("home", (e, a) -> {
            if (e instanceof MobEntity)
            {
                return (((MobEntity) e).getPositionTargetRange () > 0)?new BlockValue(null, (ServerWorld) e.getEntityWorld(), ((PathAwareEntity) e).getPositionTarget()):Value.FALSE;
            }
            return Value.NULL;
        });
        put("spawn_point", (e, a) -> {
            if (e instanceof ServerPlayerEntity)
            {
                ServerPlayerEntity spe = (ServerPlayerEntity)e;
                if (spe.getSpawnPointPosition() == null) return Value.FALSE;
                return ListValue.of(
                        ValueConversions.of(spe.getSpawnPointPosition()),
                        ValueConversions.of(spe.getSpawnPointDimension()),
                        new NumericValue(spe.getSpawnAngle()),
                        new NumericValue(spe.isSpawnPointSet()) // true if forced spawn point
                        );
            }
            return Value.NULL;
        });
        put("pose", (e, a) -> new StringValue(e.getPose().name().toLowerCase(Locale.ROOT)));
        put("sneaking", (e, a) -> e.isSneaking()?Value.TRUE:Value.FALSE);
        put("sprinting", (e, a) -> e.isSprinting()?Value.TRUE:Value.FALSE);
        put("swimming", (e, a) -> e.isSwimming()?Value.TRUE:Value.FALSE);
        put("air", (e, a) -> new NumericValue(e.getAir()));
        put("persistence", (e, a) -> {
            if (e instanceof MobEntity) return new NumericValue(((MobEntity) e).isPersistent());
            return Value.NULL;
        });
        put("hunger", (e, a) -> {
            if(e instanceof PlayerEntity) return new NumericValue(((PlayerEntity) e).getHungerManager().getFoodLevel());
            return Value.NULL;
        });
        put("saturation", (e, a) -> {
            if(e instanceof PlayerEntity) return new NumericValue(((PlayerEntity) e).getHungerManager().getSaturationLevel());
            return Value.NULL;
        });

        put("exhaustion",(e, a)->{
            if(e instanceof PlayerEntity) return new NumericValue(((HungerManagerInterface)((PlayerEntity) e).getHungerManager()).getExhaustionCM());
            return Value.NULL;
        });

        put("absorption",(e, a)->{
            if(e instanceof PlayerEntity) return new NumericValue(((PlayerEntity) e).getAbsorptionAmount());
            return Value.NULL;
        });

        put("xp",(e, a)->{
            if(e instanceof PlayerEntity) return new NumericValue(((PlayerEntity) e).totalExperience);
            return Value.NULL;
        });

        put("xp_level", (e, a)->{
            if(e instanceof PlayerEntity) return new NumericValue(((PlayerEntity) e).experienceLevel);
            return Value.NULL;
        });

        put("xp_progress", (e, a)->{
            if(e instanceof PlayerEntity) return new NumericValue(((PlayerEntity) e).experienceProgress);
            return Value.NULL;
        });

        put("score", (e, a)->{
            if(e instanceof PlayerEntity) return new NumericValue(((PlayerEntity) e).getScore());
            return Value.NULL;
        });

        put("jumping", (e, a) -> {
            if (e instanceof LivingEntity)
            {
                return  ((LivingEntityInterface) e).isJumpingCM()?Value.TRUE:Value.FALSE;
            }
            return Value.NULL;
        });
        put("gamemode", (e, a) -> {
            if (e instanceof  ServerPlayerEntity)
            {
                return new StringValue(((ServerPlayerEntity) e).interactionManager.getGameMode().getName());
            }
            return Value.NULL;
        });

        put("path", (e, a) -> {
            if (e instanceof MobEntity)
            {
                Path path = ((MobEntity)e).getNavigation().getCurrentPath();
                if (path == null) return Value.NULL;
                return ValueConversions.fromPath((ServerWorld)e.getEntityWorld(), path);
            }
            return Value.NULL;
        });

        put("brain", (e, a) -> {
            String module = a.getString();
            MemoryModuleType<?> moduleType = Registry.MEMORY_MODULE_TYPE.get(new Identifier(module));
            if (moduleType == MemoryModuleType.DUMMY) return Value.NULL;
            if (e instanceof LivingEntity)
            {
                LivingEntity livingEntity = (LivingEntity)e;
                Brain<?> brain = livingEntity.getBrain();
                Map<MemoryModuleType<?>, Optional<? extends Memory<?>>> memories = ((BrainInterface)brain).getMobMemories();
                Optional<? extends Memory<?>> optmemory = memories.get(moduleType);
                if (optmemory==null || !optmemory.isPresent()) return Value.NULL;
                Memory<?> memory = optmemory.get();
                return ValueConversions.fromTimedMemory(e, ((MemoryInterface)memory).getScarpetExpiry(), memory.getValue());
            }
            return Value.NULL;
        });
        put("gamemode_id", (e, a) -> {
            if (e instanceof  ServerPlayerEntity)
            {
                return new NumericValue(((ServerPlayerEntity) e).interactionManager.getGameMode().getId());
            }
            return Value.NULL;
        });

        put("permission_level", (e, a) -> {
            if (e instanceof  ServerPlayerEntity)
            {
                ServerPlayerEntity spe = (ServerPlayerEntity) e;
                for (int i=4; i>=0; i--)
                {
                    if (spe.hasPermissionLevel(i))
                        return new NumericValue(i);

                }
                return new NumericValue(0);
            }
            return Value.NULL;
        });

        put("player_type", (e, a) -> {
            if (e instanceof PlayerEntity)
            {
                if (e instanceof EntityPlayerMPFake) return new StringValue(((EntityPlayerMPFake) e).isAShadow?"shadow":"fake");
                PlayerEntity p = (PlayerEntity)e;
                MinecraftServer server = p.getEntityWorld().getServer();
                if (server.isDedicated()) return new StringValue("multiplayer");
                boolean runningLan = server.isRemote();
                if (!runningLan) return new StringValue("singleplayer");
                boolean isowner = server.isHost(p.getGameProfile());
                if (isowner) return new StringValue("lan_host");
                return new StringValue("lan player");
                // realms?
            }
            return Value.NULL;
        });

        put("team", (e, a) -> e.getScoreboardTeam()==null?Value.NULL:new StringValue(e.getScoreboardTeam().getName()));

        put("ping", (e, a) -> {
            if (e instanceof  ServerPlayerEntity)
            {
                ServerPlayerEntity spe = (ServerPlayerEntity) e;
                return new NumericValue(spe.pingMilliseconds);
            }
            return Value.NULL;
        });

        //spectating_entity
        // isGlowing
        put("effect", (e, a) ->
        {
            if (!(e instanceof LivingEntity))
            {
                return Value.NULL;
            }
            if (a == null)
            {
                List<Value> effects = new ArrayList<>();
                for (StatusEffectInstance p : ((LivingEntity) e).getStatusEffects())
                {
                    effects.add(ListValue.of(
                        new StringValue(p.getTranslationKey().replaceFirst("^effect\\.minecraft\\.", "")),
                        new NumericValue(p.getAmplifier()),
                        new NumericValue(p.getDuration())
                    ));
                }
                return ListValue.wrap(effects);
            }
            String effectName = a.getString();
            StatusEffect potion = Registry.STATUS_EFFECT.get(new Identifier(effectName));
            if (potion == null)
                throw new InternalExpressionException("No such an effect: "+effectName);
            if (!((LivingEntity) e).hasStatusEffect(potion))
                return Value.NULL;
            StatusEffectInstance pe = ((LivingEntity) e).getStatusEffect(potion);
            return ListValue.of( new NumericValue(pe.getAmplifier()), new NumericValue(pe.getDuration()) );
        });

        put("health", (e, a) ->
        {
            if (e instanceof LivingEntity)
            {
                return new NumericValue(((LivingEntity) e).getHealth());
            }
            //if (e instanceof ItemEntity)
            //{
            //    e.h consider making item health public
            //}
            return Value.NULL;
        });
        put("holds", (e, a) -> {
            EquipmentSlot where = EquipmentSlot.MAINHAND;
            if (a != null)
                where = inventorySlots.get(a.getString());
            if (where == null)
                throw new InternalExpressionException("Unknown inventory slot: "+a.getString());
            if (e instanceof LivingEntity)
                return ListValue.fromItemStack(((LivingEntity)e).getEquippedStack(where));
            return Value.NULL;
        });

        put("selected_slot", (e, a) -> {
           if (e instanceof PlayerEntity)
               return new NumericValue(((PlayerEntity) e).inventory.selectedSlot);
           return Value.NULL;
        });

        put("active_block", (e, a) -> {
            if (e instanceof ServerPlayerEntity)
            {
                ServerPlayerInteractionManagerInterface manager = (ServerPlayerInteractionManagerInterface) (((ServerPlayerEntity) e).interactionManager);
                BlockPos pos = manager.getCurrentBreakingBlock();
                if (pos == null) return Value.NULL;
                return new BlockValue(null, ((ServerPlayerEntity) e).getServerWorld(), pos);
            }
            return Value.NULL;
        });

        put("breaking_progress", (e, a) -> {
            if (e instanceof ServerPlayerEntity)
            {
                ServerPlayerInteractionManagerInterface manager = (ServerPlayerInteractionManagerInterface) (((ServerPlayerEntity) e).interactionManager);
                int progress = manager.getCurrentBlockBreakingProgress();
                if (progress < 0) return Value.NULL;
                return new NumericValue(progress);
            }
            return Value.NULL;
        });


        put("facing", (e, a) -> {
            int index = 0;
            if (a != null)
                index = (6+(int)NumericValue.asNumber(a).getLong())%6;
            if (index < 0 || index > 5)
                throw new InternalExpressionException("Facing order should be between -6 and 5");

            return new StringValue(Direction.getEntityFacingOrder(e)[index].asString());
        });

        put("trace", (e, a) ->
        {
            float reach = 4.5f;
            boolean entities = true;
            boolean liquids = false;
            boolean blocks = true;
            boolean exact = false;

            if (a!=null)
            {
                if (!(a instanceof ListValue))
                {
                    reach = (float) NumericValue.asNumber(a).getDouble();
                }
                else
                {
                    List<Value> args = ((ListValue) a).getItems();
                    if (args.size()==0)
                        throw new InternalExpressionException("'trace' needs more arguments");
                    reach = (float) NumericValue.asNumber(args.get(0)).getDouble();
                    if (args.size() > 1)
                    {
                        entities = false;
                        blocks = false;
                        for (int i = 1; i < args.size(); i++)
                        {
                            String what = args.get(i).getString();
                            if (what.equalsIgnoreCase("entities"))
                                entities = true;
                            else if (what.equalsIgnoreCase("blocks"))
                                blocks = true;
                            else if (what.equalsIgnoreCase("liquids"))
                                liquids = true;
                            else if (what.equalsIgnoreCase("exact"))
                                exact = true;

                            else throw new InternalExpressionException("Incorrect tracing: "+what);
                        }
                    }
                }
            }
            else if (e instanceof ServerPlayerEntity && ((ServerPlayerEntity) e).interactionManager.isCreative())
            {
                reach = 5.0f;
            }

            HitResult hitres;
            if (entities && !blocks)
                hitres = Tracer.rayTraceEntities(e, 1, reach, reach*reach);
            else if (entities)
                hitres = Tracer.rayTrace(e, 1, reach, liquids);
            else
                hitres = Tracer.rayTraceBlocks(e, 1, reach, liquids);

            if (hitres == null) return Value.NULL;
            if (exact && hitres.getType() != HitResult.Type.MISS) return ValueConversions.of(hitres.getPos());
            switch (hitres.getType())
            {
                case MISS: return Value.NULL;
                case BLOCK: return new BlockValue(null, (ServerWorld) e.getEntityWorld(), ((BlockHitResult)hitres).getBlockPos() );
                case ENTITY: return new EntityValue(((EntityHitResult)hitres).getEntity());
            }
            return Value.NULL;
        });

        put("nbt",(e, a) -> {
            CompoundTag nbttagcompound = e.toTag((new CompoundTag()));
            if (a==null)
                return new NBTSerializableValue(nbttagcompound);
            return new NBTSerializableValue(nbttagcompound).get(a);
        });

        put("category",(e,a)->{return new StringValue(e.getType().getSpawnGroup().toString().toLowerCase(Locale.ROOT));});
    }};

    public void set(String what, Value toWhat)
    {
        if (!(featureModifiers.containsKey(what)))
            throw new InternalExpressionException("Unknown entity action: " + what);
        try
        {
            featureModifiers.get(what).accept(entity, toWhat);
        }
        catch (NullPointerException npe)
        {
            throw new InternalExpressionException("'modify' for '"+what+"' expects a value");
        }
        catch (IndexOutOfBoundsException ind)
        {
            throw new InternalExpressionException("Wrong number of arguments for `modify` option: "+what);
        }
    }

    private static void updatePosition(Entity e, double x, double y, double z, float yaw, float pitch)
    {
        if (
                !Double.isFinite(x) || Double.isNaN(x) ||
                !Double.isFinite(y) || Double.isNaN(y) ||
                !Double.isFinite(z) || Double.isNaN(z) ||
                !Float.isFinite(yaw) || Float.isNaN(yaw) ||
                !Float.isFinite(pitch) || Float.isNaN(pitch)
        )
            return;
        if (e instanceof ServerPlayerEntity)
        {
            // this forces position but doesn't angles for some reason. Need both in the API in the future.
            EnumSet<PlayerPositionLookS2CPacket.Flag> set  = EnumSet.noneOf(PlayerPositionLookS2CPacket.Flag.class);
            set.add(PlayerPositionLookS2CPacket.Flag.X_ROT);
            set.add(PlayerPositionLookS2CPacket.Flag.Y_ROT);
            ((ServerPlayerEntity)e).networkHandler.teleportRequest(x, y, z, yaw, pitch, set );
        }
        else
        {
            e.refreshPositionAndAngles(x, y, z, yaw, pitch);
            // we were sending to players for not-living entites, that were untracked. Living entities should be tracked.
            //((ServerWorld) e.getEntityWorld()).getChunkManager().sendToNearbyPlayers(e, new EntityS2CPacket.(e));
            if (e instanceof LivingEntity)
            {
                LivingEntity le = (LivingEntity)e;
                le.prevBodyYaw = le.prevYaw = yaw;
                le.prevHeadYaw = le.headYaw = yaw;
                // seems universal for:
                //e.setHeadYaw(yaw);
                //e.setYaw(yaw);
            }
            else
            {
                ((ServerWorld) e.getEntityWorld()).getChunkManager().sendToNearbyPlayers(e, new EntityPositionS2CPacket(e));
            }
        }
    }

    private static void updateVelocity(Entity e)
    {
        e.velocityModified = true;
        //((ServerWorld)e.getEntityWorld()).method_14178().sendToNearbyPlayers(e, new EntityVelocityUpdateS2CPacket(e));
    }

    private static final Map<String, BiConsumer<Entity, Value>> featureModifiers = new HashMap<String, BiConsumer<Entity, Value>>() {{
        put("remove", (entity, value) -> entity.remove());
        put("age", (e, v) -> e.age = Math.abs((int)NumericValue.asNumber(v).getLong()) );
        put("health", (e, v) -> {
            float health = (float) NumericValue.asNumber(v).getDouble();
            if (health <= 0f && e instanceof ServerPlayerEntity)
            {
                ServerPlayerEntity player = (ServerPlayerEntity) e;
                if (player.currentScreenHandler != null)
                {
                    // if player dies with open container, then that causes NPE on the client side
                    // its a client side bug that may never surface unless vanilla gets into scripting at some point
                    // bug: #228
                    player.closeHandledScreen();
                }
                ((LivingEntity) e).setHealth(health);
            }
            if (e instanceof LivingEntity) ((LivingEntity) e).setHealth(health);
        });
        // todo add handling of the source for extra effects
        /*put("damage", (e, v) -> {
            float dmgPoints;
            DamageSource source;
            if (v instanceof ListValue && ((ListValue) v).getItems().size() > 1)
            {
                   List<Value> vals = ((ListValue) v).getItems();
                   dmgPoints = (float) NumericValue.asNumber(v).getDouble();
                   source = DamageSource ... yeah...
            }
            else
            {

            }
        });*/
        put("kill", (e, v) -> e.kill());
        put("location", (e, v) ->
        {
            if (!(v instanceof ListValue))
            {
                throw new InternalExpressionException("Expected a list of 5 parameters as a second argument");
            }
            List<Value> coords = ((ListValue) v).getItems();
            updatePosition(e,
                    NumericValue.asNumber(coords.get(0)).getDouble(),
                    NumericValue.asNumber(coords.get(1)).getDouble(),
                    NumericValue.asNumber(coords.get(2)).getDouble(),
                    (float) NumericValue.asNumber(coords.get(3)).getDouble(),
                    (float) NumericValue.asNumber(coords.get(4)).getDouble()
            );
        });
        put("pos", (e, v) ->
        {
            if (!(v instanceof ListValue))
            {
                throw new InternalExpressionException("Expected a list of 3 parameters as a second argument");
            }
            List<Value> coords = ((ListValue) v).getItems();
            updatePosition(e,
                    NumericValue.asNumber(coords.get(0)).getDouble(),
                    NumericValue.asNumber(coords.get(1)).getDouble(),
                    NumericValue.asNumber(coords.get(2)).getDouble(),
                    e.yaw,
                    e.pitch
            );
        });
        put("x", (e, v) ->
        {
            updatePosition(e, NumericValue.asNumber(v).getDouble(), e.getY(), e.getZ(), e.yaw, e.pitch);
        });
        put("y", (e, v) ->
        {
            updatePosition(e, e.getX(), NumericValue.asNumber(v).getDouble(), e.getZ(), e.yaw, e.pitch);
        });
        put("z", (e, v) ->
        {
            updatePosition(e, e.getX(), e.getY(), NumericValue.asNumber(v).getDouble(), e.yaw, e.pitch);
        });
        put("yaw", (e, v) ->
        {
            updatePosition(e, e.getX(), e.getY(), e.getZ(), ((float)NumericValue.asNumber(v).getDouble()) % 360, e.pitch);
        });
        put("head_yaw", (e, v) ->
        {
            if (e instanceof LivingEntity)
            {
                e.setHeadYaw((float)NumericValue.asNumber(v).getDouble() % 360);
            }
        });
        put("body_yaw", (e, v) ->
        {
            if (e instanceof LivingEntity)
            {
                e.setYaw((float)NumericValue.asNumber(v).getDouble() % 360);
            }
        });

        put("pitch", (e, v) ->
        {
            updatePosition(e, e.getX(), e.getY(), e.getZ(), e.yaw, MathHelper.clamp((float)NumericValue.asNumber(v).getDouble(), -90, 90));
        });

        //"look"
        //"turn"
        //"nod"

        put("move", (e, v) ->
        {
            if (!(v instanceof ListValue))
            {
                throw new InternalExpressionException("Expected a list of 3 parameters as a second argument");
            }
            List<Value> coords = ((ListValue) v).getItems();
            updatePosition(e,
                    e.getX() + NumericValue.asNumber(coords.get(0)).getDouble(),
                    e.getY() + NumericValue.asNumber(coords.get(1)).getDouble(),
                    e.getZ() + NumericValue.asNumber(coords.get(2)).getDouble(),
                    e.yaw,
                    e.pitch
            );
        });

        put("motion", (e, v) ->
        {
            if (!(v instanceof ListValue))
            {
                throw new InternalExpressionException("Expected a list of 3 parameters as a second argument");
            }
            List<Value> coords = ((ListValue) v).getItems();
            e.setVelocity(
                    NumericValue.asNumber(coords.get(0)).getDouble(),
                    NumericValue.asNumber(coords.get(1)).getDouble(),
                    NumericValue.asNumber(coords.get(2)).getDouble()
            );
            updateVelocity(e);
        });
        put("motion_x", (e, v) ->
        {
            Vec3d velocity = e.getVelocity();
            e.setVelocity(NumericValue.asNumber(v).getDouble(), velocity.y, velocity.z);
            updateVelocity(e);
        });
        put("motion_y", (e, v) ->
        {
            Vec3d velocity = e.getVelocity();
            e.setVelocity(velocity.x, NumericValue.asNumber(v).getDouble(), velocity.z);
            updateVelocity(e);
        });
        put("motion_z", (e, v) ->
        {
            Vec3d velocity = e.getVelocity();
            e.setVelocity(velocity.x, velocity.y, NumericValue.asNumber(v).getDouble());
            updateVelocity(e);
        });

        put("accelerate", (e, v) ->
        {
            if (!(v instanceof ListValue))
            {
                throw new InternalExpressionException("Expected a list of 3 parameters as a second argument");
            }
            List<Value> coords = ((ListValue) v).getItems();
            e.addVelocity(
                    NumericValue.asNumber(coords.get(0)).getDouble(),
                    NumericValue.asNumber(coords.get(1)).getDouble(),
                    NumericValue.asNumber(coords.get(2)).getDouble()
            );
            updateVelocity(e);

        });
        put("custom_name", (e, v) -> {
            if (v instanceof NullValue)
            {
                e.setCustomNameVisible(false);
                e.setCustomName(null);
                return;
            }
            boolean showName = false;
            if (v instanceof ListValue)
            {
                showName = ((ListValue) v).getItems().get(1).getBoolean();
                v = ((ListValue) v).getItems().get(0);
            }
            e.setCustomNameVisible(showName);
            e.setCustomName(new LiteralText(v.getString()));
        });

        put("persistence", (e, v) ->
        {
            if (!(e instanceof MobEntity)) return;
            if (v == null) v = Value.TRUE;
            ((MobEntityInterface)e).setPersistence(v.getBoolean());
        });

        put("dismount", (e, v) -> e.stopRiding() );
        put("mount", (e, v) -> {
            if (v instanceof EntityValue)
            {
                e.startRiding(((EntityValue) v).getEntity(),true);
            }
            if (e instanceof ServerPlayerEntity)
            {
                ((ServerPlayerEntity)e).networkHandler.sendPacket(new EntityPassengersSetS2CPacket(e));
                //...
            }
        });
        put("unmountable", (e, v) ->{
            if (v == null)
                v = Value.TRUE;
            ((EntityInterface)e).setPermanentVehicle(v.getBoolean());
        });
        put("drop_passengers", (e, v) -> e.removeAllPassengers());
        put("mount_passengers", (e, v) -> {
            if (v==null)
                throw new InternalExpressionException("'mount_passengers' needs entities to ride");
            if (v instanceof EntityValue)
                ((EntityValue) v).getEntity().startRiding(e);
            else if (v instanceof ListValue)
                for (Value element : ((ListValue) v).getItems())
                    if (element instanceof EntityValue)
                        ((EntityValue) element).getEntity().startRiding(e);
        });
        put("tag", (e, v) -> {
            if (v==null)
                throw new InternalExpressionException("'tag' requires parameters");
            if (v instanceof ListValue)
                for (Value element : ((ListValue) v).getItems()) e.addScoreboardTag(element.getString());
            else
                e.addScoreboardTag(v.getString());
        });
        put("clear_tag", (e, v) -> {
            if (v==null)
                throw new InternalExpressionException("'clear_tag' requires parameters");
            if (v instanceof ListValue)
                for (Value element : ((ListValue) v).getItems()) e.removeScoreboardTag(element.getString());
            else
                e.removeScoreboardTag(v.getString());
        });
        //put("target", (e, v) -> {
        //    // attacks indefinitely - might need to do it through tasks
        //    if (e instanceof MobEntity)
        //    {
        //        LivingEntity elb = assertEntityArgType(LivingEntity.class, v);
        //        ((MobEntity) e).setTarget(elb);
        //    }
        //});
        put("breeding_age", (e, v) ->
        {
            if (e instanceof PassiveEntity)
            {
                ((PassiveEntity) e).setBreedingAge((int)NumericValue.asNumber(v).getLong());
            }
        });
        put("talk", (e, v) -> {
            // attacks indefinitely
            if (e instanceof MobEntity)
            {
                ((MobEntity) e).playAmbientSound();
            }
        });
        put("home", (e, v) -> {
            if (!(e instanceof PathAwareEntity))
                return;
            PathAwareEntity ec = (PathAwareEntity)e;
            if (v == null)
                throw new InternalExpressionException("'home' requires at least one position argument, and optional distance, or null to cancel");
            if (v instanceof NullValue)
            {
                ec.setPositionTarget(BlockPos.ORIGIN, -1);
                Map<String,Goal> tasks = ((MobEntityInterface)ec).getTemporaryTasks();
                ((MobEntityInterface)ec).getAI(false).remove(tasks.get("home"));
                tasks.remove("home");
                return;
            }

            BlockPos pos;
            int distance = 16;

            if (v instanceof BlockValue)
            {
                pos = ((BlockValue) v).getPos();
                if (pos == null) throw new InternalExpressionException("Block is not positioned in the world");
            }
            else if (v instanceof ListValue)
            {
                List<Value> lv = ((ListValue) v).getItems();
                Vector3Argument locator = Vector3Argument.findIn(lv, 0, false);
                pos = new BlockPos(locator.vec.x, locator.vec.y, locator.vec.z);
                if (lv.size() > locator.offset)
                {
                    distance = (int) NumericValue.asNumber(lv.get(locator.offset)).getLong();
                }
            }
            else throw new InternalExpressionException("'home' requires at least one position argument, and optional distance");

            ec.setPositionTarget(pos, distance);
            Map<String,Goal> tasks = ((MobEntityInterface)ec).getTemporaryTasks();
            if (!tasks.containsKey("home"))
            {
                Goal task = new GoToWalkTargetGoal(ec, 1.0D);
                tasks.put("home", task);
                ((MobEntityInterface)ec).getAI(false).add(10, task);
            }
        }); //requires mixing

        put("spawn_point", (e, a) -> {
            if (!(e instanceof ServerPlayerEntity)) return;
            ServerPlayerEntity spe = (ServerPlayerEntity)e;
            if (a == null)
            {
                spe.setSpawnPoint(null, null, 0, false, false);
            }
            else if (a instanceof ListValue)
            {
                List<Value> params= ((ListValue) a).getItems();
                Vector3Argument blockLocator = Vector3Argument.findIn(params, 0, false);
                BlockPos pos = new BlockPos(blockLocator.vec);
                RegistryKey<World> world = spe.getEntityWorld().getRegistryKey();
                float angle = spe.getHeadYaw();
                boolean forced = false;
                if (params.size() > blockLocator.offset)
                {
                    Value worldValue = params.get(blockLocator.offset+0);
                    world = ValueConversions.dimFromValue(worldValue, spe.getServer()).getRegistryKey();
                    if (params.size() > blockLocator.offset+1)
                    {
                        angle = NumericValue.asNumber(params.get(blockLocator.offset+1), "angle").getFloat();
                        if (params.size() > blockLocator.offset+2)
                        {
                            forced = params.get(blockLocator.offset+2).getBoolean();
                        }
                    }
                }
                spe.setSpawnPoint(world, pos, angle, forced, false);
            }
            else if (a instanceof BlockValue)
            {
                BlockValue bv= (BlockValue)a;
                if (bv.getPos()==null || bv.getWorld() == null)
                    throw new InternalExpressionException("block for spawn modification should be localised in the world");
                spe.setSpawnPoint(bv.getWorld().getRegistryKey(), bv.getPos(), e.yaw, true, false);
            }
            else if (a.isNull())
            {
                spe.setSpawnPoint(null, null, 0, false, false);
            }
            else
            {
                throw new InternalExpressionException("modifying player respawn point requires a block position, optional world, optional angle, and optional force");

            }
        });

        put("pickup_delay", (e, v) ->
        {
            if (e instanceof ItemEntity)
            {
                ((ItemEntity) e).setPickupDelay((int)NumericValue.asNumber(v).getLong());
            }
        });

        put("despawn_timer", (e, v) ->
        {
            if (e instanceof LivingEntity)
            {
                ((LivingEntity) e).setDespawnCounter((int)NumericValue.asNumber(v).getLong());
            }
        });

        put("portal_cooldown", (e , v) ->
        {
            if (v==null)
                throw new InternalExpressionException("'portal_cooldown' requires a value to set");
            ((EntityInterface)e).setPublicNetherPortalCooldown(NumericValue.asNumber(v).getInt());
        });

        put("portal_timer", (e , v) ->
        {
            if (v==null)
                throw new InternalExpressionException("'portal_timer' requires a value to set");
            ((EntityInterface) e).setPortalTimer(NumericValue.asNumber(v).getInt());
        });

        put("ai", (e, v) ->
        {
            if (e instanceof MobEntity)
            {
                ((MobEntity) e).setAiDisabled(!v.getBoolean());
            }
        });

        put("no_clip", (e, v) ->
        {
            if (v == null)
                e.noClip = true;
            else
                e.noClip = v.getBoolean();
        });
        put("effect", (e, v) ->
        {
            if (!(e instanceof LivingEntity)) return;
            LivingEntity le = (LivingEntity)e;
            if (v == null)
            {
                le.clearStatusEffects();
                return;
            }
            else if (v instanceof ListValue)
            {
                List<Value> lv = ((ListValue) v).getItems();
                if (lv.size() >= 1 && lv.size() <= 5)
                {
                    String effectName = lv.get(0).getString();
                    StatusEffect effect = Registry.STATUS_EFFECT.get(new Identifier(effectName));
                    if (effect == null)
                        throw new InternalExpressionException("Wrong effect name: "+effectName);
                    if (lv.size() == 1)
                    {
                        le.removeStatusEffect(effect);
                        return;
                    }
                    int duration = (int)NumericValue.asNumber(lv.get(1)).getLong();
                    if (duration <= 0)
                    {
                        le.removeStatusEffect(effect);
                        return;
                    }
                    int amplifier = 0;
                    if (lv.size() > 2)
                        amplifier = (int)NumericValue.asNumber(lv.get(2)).getLong();
                    boolean showParticles = true;
                    if (lv.size() > 3)
                        showParticles = lv.get(3).getBoolean();
                    boolean showIcon = true;
                    if (lv.size() > 4)
                        showIcon = lv.get(4).getBoolean();
                    boolean ambient = false;
                    if (lv.size() > 5)
                        showIcon = lv.get(5).getBoolean();
                    le.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier, ambient, showParticles, showIcon));
                    return;
                }
            }
            else
            {
                String effectName = v.getString();
                StatusEffect effect = Registry.STATUS_EFFECT.get(new Identifier(effectName));
                if (effect == null)
                    throw new InternalExpressionException("Wrong effect name: "+effectName);
                le.removeStatusEffect(effect);
                return;
            }
            throw new InternalExpressionException("'effect' needs either no arguments (clear) or effect name, duration, and optional amplifier, show particles, show icon and ambient");
        });

        put("gamemode", (e,v)->{
            if(!(e instanceof ServerPlayerEntity)) return;
            GameMode toSet = v instanceof NumericValue ?
                    GameMode.byId(((NumericValue) v).getInt(), null) :
                    GameMode.byName(v.getString().toLowerCase(Locale.ROOT), null);
            if (toSet != null) ((ServerPlayerEntity) e).setGameMode(toSet);
        });

        put("jumping",(e,v)->{
            if(!(e instanceof LivingEntity)) return;
            ((LivingEntity) e).setJumping(v.getBoolean());
        });

        put("jump",(e,v)->{
            if (e instanceof LivingEntity)
            {
                ((LivingEntityInterface)e).doJumpCM();
            }
            else
            {
                genericJump(e);
            }
        });

        put("silent",(e,v)-> e.setSilent(v.getBoolean()));

        put("gravity",(e,v)-> e.setNoGravity(!v.getBoolean()));

        put("invulnerable",(e,v)-> e.setInvulnerable(v.getBoolean()));

        put("fire",(e,v)-> e.setFireTicks((int)NumericValue.asNumber(v).getLong()));

        put("hunger", (e, v)-> {
            if(e instanceof PlayerEntity) ((PlayerEntity) e).getHungerManager().setFoodLevel((int) NumericValue.asNumber(v).getLong());
        });

        put("exhaustion", (e, v)-> {
            if(e instanceof PlayerEntity) ((HungerManagerInterface) ((PlayerEntity) e).getHungerManager()).setExhaustionCM(NumericValue.asNumber(v).getFloat());
        });

        put("add_exhaustion", (e, v)-> {
            if(e instanceof PlayerEntity) ((PlayerEntity) e).getHungerManager().addExhaustion((int) NumericValue.asNumber(v).getLong());
        });

        put("absorption", (e, v) -> {
            if (e instanceof PlayerEntity) ((PlayerEntity) e).setAbsorptionAmount((float) NumericValue.asNumber(v).getLong());
        });

        put("add_xp", (e, v) -> {
            if (e instanceof PlayerEntity) ((PlayerEntity) e).addExperience(NumericValue.asNumber(v).getInt());
        });

        put("xp_level", (e, v) -> {
            if (e instanceof PlayerEntity) ((PlayerEntity) e).addExperienceLevels(NumericValue.asNumber(v).getInt()-((PlayerEntity) e).experienceLevel);
        });

        put("xp_score", (e, v) -> {
            if (e instanceof PlayerEntity) ((PlayerEntity) e).setScore(NumericValue.asNumber(v).getInt());
        });

        put("saturation", (e, v)-> {
            if(e instanceof PlayerEntity) ((PlayerEntity) e).getHungerManager().setSaturationLevelClient(NumericValue.asNumber(v).getFloat());
        });

        put("air", (e, v) -> e.setAir(NumericValue.asNumber(v).getInt()));

        put("breaking_progress", (e, a) -> {
            if (e instanceof ServerPlayerEntity)
            {
                int progress = (a == null || a.isNull())?-1:NumericValue.asNumber(a).getInt();
                ServerPlayerInteractionManagerInterface manager = (ServerPlayerInteractionManagerInterface) (((ServerPlayerEntity) e).interactionManager);
                manager.setBlockBreakingProgress(progress);
            }
        });

        put("nbt", (e, v) -> {
            if (!(e instanceof PlayerEntity))
            {
                UUID uUID = e.getUuid();
                Value tagValue = NBTSerializableValue.fromValue(v);
                if (tagValue instanceof NBTSerializableValue)
                {
                    e.fromTag(((NBTSerializableValue) tagValue).getCompoundTag());
                    e.setUuid(uUID);
                }
            }
        });
        put("nbt_merge", (e, v) -> {
            if (!(e instanceof PlayerEntity))
            {
                UUID uUID = e.getUuid();
                Value tagValue = NBTSerializableValue.fromValue(v);
                if (tagValue instanceof NBTSerializableValue)
                {
                    CompoundTag nbttagcompound = e.toTag((new CompoundTag()));
                    nbttagcompound.copyFrom(((NBTSerializableValue) tagValue).getCompoundTag());
                    e.fromTag(nbttagcompound);
                    e.setUuid(uUID);
                }
            }
        });

        // "dimension"      []
        // "item"           []
        // "count",         []
        // "effect_"name    []
    }};

    public void setEvent(CarpetContext cc, String eventName, FunctionValue fun, List<Value> args)
    {
        EntityEventsGroup.Event event = EntityEventsGroup.Event.byName.get(eventName);
        if (event == null)
            throw new InternalExpressionException("Unknown entity event: " + eventName);
        ((EntityInterface)entity).getEventContainer().addEvent(event, cc.host.getName(), fun, args);
    }

    @Override
    public Tag toTag(boolean force)
    {
        if (!force) throw new NBTSerializableValue.IncompatibleTypeException(this);
        CompoundTag tag = new CompoundTag();
        tag.put("Data", getEntity().toTag( new CompoundTag()));
        tag.put("Name", StringTag.of(Registry.ENTITY_TYPE.getId(entity.getType()).toString()));
        return tag;
    }
}
