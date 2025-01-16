package carpet.script.value;

import carpet.fakes.FoodDataInterface;
import carpet.script.external.Vanilla;
import carpet.script.utils.Tracer;
import carpet.script.CarpetContext;
import carpet.script.CarpetScriptServer;
import carpet.script.EntityEventsGroup;
import carpet.script.argument.Vector3Argument;
import carpet.script.exception.InternalExpressionException;
import carpet.script.external.Carpet;
import carpet.script.utils.EntityTools;
import carpet.script.utils.InputValidator;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
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

// TODO: decide whether copy(entity) should duplicate entity in the world.
public class EntityValue extends Value
{
    private Entity entity;

    public EntityValue(Entity e)
    {
        entity = e;
    }

    public static Value of(@Nullable Entity e)
    {
        return e == null ? Value.NULL : new EntityValue(e);
    }

    private static final Map<String, EntitySelector> selectorCache = new HashMap<>();

    public static Collection<? extends Entity> getEntitiesFromSelector(CommandSourceStack source, String selector)
    {
        try
        {
            EntitySelector entitySelector = selectorCache.get(selector);
            if (entitySelector != null)
            {
                return entitySelector.findEntities(source.withMaximumPermission(4));
            }
            entitySelector = new EntitySelectorParser(new StringReader(selector), true).parse();
            selectorCache.put(selector, entitySelector);
            return entitySelector.findEntities(source.withMaximumPermission(4));
        }
        catch (CommandSyntaxException e)
        {
            throw new InternalExpressionException("Cannot select entities from " + selector);
        }
    }

    public Entity getEntity()
    {
        if (entity instanceof ServerPlayer serverPlayer && Vanilla.ServerPlayer_isInvalidEntityObject(serverPlayer))
        {
            ServerPlayer newPlayer = entity.getServer().getPlayerList().getPlayer(entity.getUUID());
            if (newPlayer != null)
            {
                entity = newPlayer;
            }
        }
        return entity;
    }

    public static ServerPlayer getPlayerByValue(MinecraftServer server, Value value)
    {
        if (value instanceof EntityValue ev && ev.getEntity() instanceof ServerPlayer sp)
        {
            return sp;
        }
        if (value.isNull())
        {
            return null;
        }
        String playerName = value.getString();
        return server.getPlayerList().getPlayerByName(playerName);
    }

    public static String getPlayerNameByValue(Value value)
    {
        if (value instanceof EntityValue ev && ev.getEntity() instanceof ServerPlayer sp)
        {
            return sp.getScoreboardName();
        }
        if (value.isNull())
        {
            return null;
        }
        return value.getString();
    }

    @Override
    public String getString()
    {
        return getEntity().getName().getString();
    }

    @Override
    public boolean getBoolean()
    {
        return true;
    }

    @Override
    public boolean equals(Object v)
    {
        if (v instanceof EntityValue ev)
        {
            return getEntity().getId() == ev.getEntity().getId();
        }
        return super.equals(v);
    }

    @Override
    public Value in(Value v)
    {
        if (v instanceof ListValue lv)
        {
            List<Value> values = lv.getItems();
            String what = values.get(0).getString();
            Value arg = null;
            if (values.size() == 2)
            {
                arg = values.get(1);
            }
            else if (values.size() > 2)
            {
                arg = ListValue.wrap(values.subList(1, values.size()));
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
        return getEntity().hashCode();
    }

    public static final EntityTypeTest<Entity, ?> ANY = EntityTypeTest.forClass(Entity.class);

    public static EntityClassDescriptor getEntityDescriptor(String who, MinecraftServer server)
    {
        EntityClassDescriptor eDesc = EntityClassDescriptor.byName.get(who);
        if (eDesc == null)
        {
            boolean positive = true;
            if (who.startsWith("!"))
            {
                positive = false;
                who = who.substring(1);
            }
            String booWho = who;
            HolderSet.Named<EntityType<?>> eTagValue = server.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE)
                    .get(TagKey.create(Registries.ENTITY_TYPE, InputValidator.identifierOf(who)))
                    .orElseThrow(() -> new InternalExpressionException(booWho + " is not a valid entity descriptor"));
            Set<EntityType<?>> eTag = eTagValue.stream().map(Holder::value).collect(Collectors.toUnmodifiableSet());
            if (positive)
            {
                if (eTag.size() == 1)
                {
                    EntityType<?> type = eTag.iterator().next();
                    return new EntityClassDescriptor(type, Entity::isAlive, eTag.stream());
                }
                else
                {
                    return new EntityClassDescriptor(ANY, e -> eTag.contains(e.getType()) && e.isAlive(), eTag.stream());
                }
            }
            else
            {
                return new EntityClassDescriptor(ANY, e -> !eTag.contains(e.getType()) && e.isAlive(), server.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE).stream().filter(et -> !eTag.contains(et)));
            }
        }
        return eDesc;
        //TODO add more here like search by tags, or type
        //if (who.startsWith('tag:'))
    }

    public static class EntityClassDescriptor
    {
        public final EntityTypeTest<Entity, ? extends Entity> directType; // interface of EntityType
        public final Predicate<? super Entity> filteringPredicate;
        public final List<EntityType<? extends Entity>> types;

        EntityClassDescriptor(EntityTypeTest<Entity, ?> type, Predicate<? super Entity> predicate, List<EntityType<?>> types)
        {
            this.directType = type;
            this.filteringPredicate = predicate;
            this.types = types;
        }

        EntityClassDescriptor(EntityTypeTest<Entity, ?> type, Predicate<? super Entity> predicate, Stream<EntityType<?>> types)
        {
            this(type, predicate, types.toList());
        }

        public Value listValue(RegistryAccess regs)
        {
            Registry<EntityType<?>> entityRegs = regs.lookupOrThrow(Registries.ENTITY_TYPE);
            return ListValue.wrap(types.stream().map(et -> nameFromRegistryId(entityRegs.getKey(et))));
        }

        public static final Map<String, EntityClassDescriptor> byName = new HashMap<>()
        {{
            List<EntityType<?>> allTypes = BuiltInRegistries.ENTITY_TYPE.stream().toList();
            // nonliving types
            Set<EntityType<?>> projectiles = Set.of(
                    EntityType.ARROW, EntityType.DRAGON_FIREBALL, EntityType.FIREWORK_ROCKET,
                    EntityType.FIREBALL, EntityType.LLAMA_SPIT, EntityType.SMALL_FIREBALL,
                    EntityType.SNOWBALL, EntityType.SPECTRAL_ARROW, EntityType.EGG,
                    EntityType.ENDER_PEARL, EntityType.EXPERIENCE_BOTTLE, EntityType.POTION,
                    EntityType.TRIDENT, EntityType.WITHER_SKULL, EntityType.FISHING_BOBBER, EntityType.SHULKER_BULLET
            );
            Set<EntityType<?>> deads = Set.of(
                    EntityType.AREA_EFFECT_CLOUD, EntityType.MARKER, EntityType.END_CRYSTAL,
                    EntityType.EVOKER_FANGS, EntityType.EXPERIENCE_ORB, EntityType.EYE_OF_ENDER,
                    EntityType.FALLING_BLOCK, EntityType.ITEM, EntityType.ITEM_FRAME, EntityType.GLOW_ITEM_FRAME,
                    EntityType.LEASH_KNOT, EntityType.LIGHTNING_BOLT, EntityType.PAINTING,
                    EntityType.TNT, EntityType.ARMOR_STAND

            );
            Set<EntityType<?>> minecarts = Set.of(
                    EntityType.MINECART, EntityType.CHEST_MINECART, EntityType.COMMAND_BLOCK_MINECART,
                    EntityType.FURNACE_MINECART, EntityType.HOPPER_MINECART,
                    EntityType.SPAWNER_MINECART, EntityType.TNT_MINECART
            );
            // living mob groups - non-defeault
            Set<EntityType<?>> undeads = Set.of(
                    EntityType.STRAY, EntityType.SKELETON, EntityType.WITHER_SKELETON,
                    EntityType.ZOMBIE, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER,
                    EntityType.ZOMBIE_HORSE, EntityType.SKELETON_HORSE, EntityType.PHANTOM,
                    EntityType.WITHER, EntityType.ZOGLIN, EntityType.HUSK, EntityType.ZOMBIFIED_PIGLIN

            );
            Set<EntityType<?>> arthropods = Set.of(
                    EntityType.BEE, EntityType.ENDERMITE, EntityType.SILVERFISH, EntityType.SPIDER,
                    EntityType.CAVE_SPIDER
            );
            Set<EntityType<?>> aquatique = Set.of(
                    EntityType.GUARDIAN, EntityType.TURTLE, EntityType.COD, EntityType.DOLPHIN, EntityType.PUFFERFISH,
                    EntityType.SALMON, EntityType.SQUID, EntityType.TROPICAL_FISH
            );
            Set<EntityType<?>> illagers = Set.of(
                    EntityType.PILLAGER, EntityType.ILLUSIONER, EntityType.VINDICATOR, EntityType.EVOKER,
                    EntityType.RAVAGER, EntityType.WITCH
            );

            Set<EntityType<?>> living = allTypes.stream().filter(et ->
                    !deads.contains(et) && !projectiles.contains(et) && !minecarts.contains(et) && !et.is(EntityTypeTags.BOAT)
            ).collect(Collectors.toSet());

            Set<EntityType<?>> regular = allTypes.stream().filter(et ->
                    living.contains(et) && !undeads.contains(et) && !arthropods.contains(et) && !aquatique.contains(et) && !illagers.contains(et)
            ).collect(Collectors.toSet());


            put("*", new EntityClassDescriptor(ANY, e -> true, allTypes));
            put("valid", new EntityClassDescriptor(ANY, net.minecraft.world.entity.EntitySelector.ENTITY_STILL_ALIVE, allTypes));
            put("!valid", new EntityClassDescriptor(ANY, e -> !e.isAlive(), allTypes));

            put("living", new EntityClassDescriptor(EntityTypeTest.forClass(LivingEntity.class), net.minecraft.world.entity.EntitySelector.ENTITY_STILL_ALIVE, allTypes.stream().filter(living::contains)));
            put("!living", new EntityClassDescriptor(ANY, (e) -> (!(e instanceof LivingEntity) && e.isAlive()), allTypes.stream().filter(et -> !living.contains(et))));

            put("projectile", new EntityClassDescriptor(EntityTypeTest.forClass(Projectile.class), net.minecraft.world.entity.EntitySelector.ENTITY_STILL_ALIVE, allTypes.stream().filter(projectiles::contains)));
            put("!projectile", new EntityClassDescriptor(ANY, (e) -> (!(e instanceof Projectile) && e.isAlive()), allTypes.stream().filter(et -> !projectiles.contains(et) && !living.contains(et))));

            put("minecarts", new EntityClassDescriptor(EntityTypeTest.forClass(AbstractMinecart.class), net.minecraft.world.entity.EntitySelector.ENTITY_STILL_ALIVE, allTypes.stream().filter(minecarts::contains)));
            put("!minecarts", new EntityClassDescriptor(ANY, (e) -> (!(e instanceof AbstractMinecart) && e.isAlive()), allTypes.stream().filter(et -> !minecarts.contains(et) && !living.contains(et))));


            // combat groups

            put("arthropod", new EntityClassDescriptor(EntityTypeTest.forClass(LivingEntity.class), e -> (e.getType().is(EntityTypeTags.ARTHROPOD) && e.isAlive()), allTypes.stream().filter(arthropods::contains)));
            put("!arthropod", new EntityClassDescriptor(EntityTypeTest.forClass(LivingEntity.class), e -> (!e.getType().is(EntityTypeTags.ARTHROPOD) && e.isAlive()), allTypes.stream().filter(et -> !arthropods.contains(et) && living.contains(et))));

            put("undead", new EntityClassDescriptor(EntityTypeTest.forClass(LivingEntity.class), e -> (e.getType().is(EntityTypeTags.UNDEAD) && e.isAlive()), allTypes.stream().filter(undeads::contains)));
            put("!undead", new EntityClassDescriptor(EntityTypeTest.forClass(LivingEntity.class), e -> (!e.getType().is(EntityTypeTags.UNDEAD) && e.isAlive()), allTypes.stream().filter(et -> !undeads.contains(et) && living.contains(et))));

            put("aquatic", new EntityClassDescriptor(EntityTypeTest.forClass(LivingEntity.class), e -> (e.getType().is(EntityTypeTags.AQUATIC) && e.isAlive()), allTypes.stream().filter(aquatique::contains)));
            put("!aquatic", new EntityClassDescriptor(EntityTypeTest.forClass(LivingEntity.class), e -> (!e.getType().is(EntityTypeTags.AQUATIC) && e.isAlive()), allTypes.stream().filter(et -> !aquatique.contains(et) && living.contains(et))));

            put("illager", new EntityClassDescriptor(EntityTypeTest.forClass(LivingEntity.class), e -> (e.getType().is(EntityTypeTags.ILLAGER) && e.isAlive()), allTypes.stream().filter(illagers::contains)));
            put("!illager", new EntityClassDescriptor(EntityTypeTest.forClass(LivingEntity.class), e -> (!e.getType().is(EntityTypeTags.ILLAGER) && e.isAlive()), allTypes.stream().filter(et -> !illagers.contains(et) && living.contains(et))));

            put("regular", new EntityClassDescriptor(EntityTypeTest.forClass(LivingEntity.class), e -> {
                EntityType<?> type = e.getType();
                return !illagers.contains(type) && !arthropods.contains(type) && !undeads.contains(type) && !aquatique.contains(type) && living.contains(type) && e.isAlive();
            }, allTypes.stream().filter(regular::contains)));
            put("!regular", new EntityClassDescriptor(EntityTypeTest.forClass(LivingEntity.class), e -> {
                EntityType<?> type = e.getType();
                return (illagers.contains(type) || arthropods.contains(type) || undeads.contains(type) || aquatique.contains(type)) && e.isAlive();
            }, allTypes.stream().filter(et -> !regular.contains(et) && living.contains(et))));

            for (ResourceLocation typeId : BuiltInRegistries.ENTITY_TYPE.keySet())
            {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(typeId);
                String mobType = ValueConversions.simplify(typeId);
                put(mobType, new EntityClassDescriptor(type, net.minecraft.world.entity.EntitySelector.ENTITY_STILL_ALIVE, Stream.of(type)));
                put("!" + mobType, new EntityClassDescriptor(ANY, (e) -> e.getType() != type && e.isAlive(), allTypes.stream().filter(et -> et != type)));
            }
            for (MobCategory catId : MobCategory.values())
            {
                String catStr = catId.getName();
                put(catStr, new EntityClassDescriptor(ANY, e -> ((e.getType().getCategory() == catId) && e.isAlive()), allTypes.stream().filter(et -> et.getCategory() == catId)));
                put("!" + catStr, new EntityClassDescriptor(ANY, e -> ((e.getType().getCategory() != catId) && e.isAlive()), allTypes.stream().filter(et -> et.getCategory() != catId)));
            }
        }};
    }

    public Value get(String what, @Nullable Value arg)
    {
        if (!(featureAccessors.containsKey(what)))
        {
            throw new InternalExpressionException("Unknown entity feature: " + what);
        }
        try
        {
            return featureAccessors.get(what).apply(getEntity(), arg);
        }
        catch (NullPointerException npe)
        {
            throw new InternalExpressionException("Cannot fetch '" + what + "' with these arguments");
        }
    }

    private static final Map<String, EquipmentSlot> inventorySlots = Map.of(
            "mainhand", EquipmentSlot.MAINHAND,
            "offhand", EquipmentSlot.OFFHAND,
            "head", EquipmentSlot.HEAD,
            "chest", EquipmentSlot.CHEST,
            "legs", EquipmentSlot.LEGS,
            "feet", EquipmentSlot.FEET
    );

    private static final Map<String, BiFunction<Entity, Value, Value>> featureAccessors = new HashMap<String, BiFunction<Entity, Value, Value>>()
    {{
        //put("test", (e, a) -> a == null ? Value.NULL : new StringValue(a.getString()));
        put("removed", (entity, arg) -> BooleanValue.of(entity.isRemoved()));
        put("uuid", (e, a) -> new StringValue(e.getStringUUID()));
        put("id", (e, a) -> new NumericValue(e.getId()));
        put("pos", (e, a) -> ListValue.of(new NumericValue(e.getX()), new NumericValue(e.getY()), new NumericValue(e.getZ())));
        put("location", (e, a) -> ListValue.of(new NumericValue(e.getX()), new NumericValue(e.getY()), new NumericValue(e.getZ()), new NumericValue(e.getYRot()), new NumericValue(e.getXRot())));
        put("x", (e, a) -> new NumericValue(e.getX()));
        put("y", (e, a) -> new NumericValue(e.getY()));
        put("z", (e, a) -> new NumericValue(e.getZ()));
        put("motion", (e, a) ->
        {
            Vec3 velocity = e.getDeltaMovement();
            return ListValue.of(new NumericValue(velocity.x), new NumericValue(velocity.y), new NumericValue(velocity.z));
        });
        put("motion_x", (e, a) -> new NumericValue(e.getDeltaMovement().x));
        put("motion_y", (e, a) -> new NumericValue(e.getDeltaMovement().y));
        put("motion_z", (e, a) -> new NumericValue(e.getDeltaMovement().z));
        put("on_ground", (e, a) -> BooleanValue.of(e.onGround()));
        put("name", (e, a) -> new StringValue(e.getName().getString()));
        put("display_name", (e, a) -> new FormattedTextValue(e.getDisplayName()));
        put("command_name", (e, a) -> new StringValue(e.getScoreboardName()));
        put("custom_name", (e, a) -> e.hasCustomName() ? new StringValue(e.getCustomName().getString()) : Value.NULL);
        put("type", (e, a) -> nameFromRegistryId(e.level().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE).getKey(e.getType())));
        put("is_riding", (e, a) -> BooleanValue.of(e.isPassenger()));
        put("is_ridden", (e, a) -> BooleanValue.of(e.isVehicle()));
        put("passengers", (e, a) -> ListValue.wrap(e.getPassengers().stream().map(EntityValue::new)));
        put("mount", (e, a) -> (e.getVehicle() != null) ? new EntityValue(e.getVehicle()) : Value.NULL);
        put("unmountable", (e, a) -> BooleanValue.of(Vanilla.Entity_isPermanentVehicle(e)));
        // deprecated
        put("tags", (e, a) -> ListValue.wrap(e.getTags().stream().map(StringValue::new)));

        put("scoreboard_tags", (e, a) -> ListValue.wrap(e.getTags().stream().map(StringValue::new)));
        put("entity_tags", (e, a) -> {
            EntityType<?> type = e.getType();
            return ListValue.wrap(e.getServer().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE).getTags().filter(entry -> entry.stream().anyMatch(h -> h.value() == type)).map(entry -> ValueConversions.of(entry)));
        });
        // deprecated
        put("has_tag", (e, a) -> BooleanValue.of(e.getTags().contains(a.getString())));

        put("has_scoreboard_tag", (e, a) -> BooleanValue.of(e.getTags().contains(a.getString())));
        put("has_entity_tag", (e, a) -> {
            Optional<HolderSet.Named<EntityType<?>>> tag = e.getServer().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE).get(TagKey.create(Registries.ENTITY_TYPE, InputValidator.identifierOf(a.getString())));
            if (tag.isEmpty())
            {
                return Value.NULL;
            }
            //Tag<EntityType<?>> tag = e.getServer().getTags().getOrEmpty(Registry.ENTITY_TYPE_REGISTRY).getTag(InputValidator.identifierOf(a.getString()));
            //if (tag == null) return Value.NULL;
            //return BooleanValue.of(e.getType().is(tag));
            EntityType<?> type = e.getType();
            return BooleanValue.of(tag.get().stream().anyMatch(h -> h.value() == type));
        });

        put("yaw", (e, a) -> new NumericValue(e.getYRot()));
        put("head_yaw", (e, a) -> e instanceof LivingEntity ? new NumericValue(e.getYHeadRot()) : Value.NULL);
        put("body_yaw", (e, a) -> e instanceof LivingEntity le ? new NumericValue(le.yBodyRot) : Value.NULL);

        put("pitch", (e, a) -> new NumericValue(e.getXRot()));
        put("look", (e, a) -> {
            Vec3 look = e.getLookAngle();
            return ListValue.of(new NumericValue(look.x), new NumericValue(look.y), new NumericValue(look.z));
        });
        put("is_burning", (e, a) -> BooleanValue.of(e.isOnFire()));
        put("fire", (e, a) -> new NumericValue(e.getRemainingFireTicks()));
        put("is_freezing", (e, a) -> BooleanValue.of(e.isFullyFrozen()));
        put("frost", (e, a) -> new NumericValue(e.getTicksFrozen()));
        put("silent", (e, a) -> BooleanValue.of(e.isSilent()));
        put("gravity", (e, a) -> BooleanValue.of(!e.isNoGravity()));
        put("immune_to_fire", (e, a) -> BooleanValue.of(e.fireImmune()));
        put("immune_to_frost", (e, a) -> BooleanValue.of(!e.canFreeze()));

        put("invulnerable", (e, a) -> BooleanValue.of(e.isInvulnerable()));
        put("dimension", (e, a) -> nameFromRegistryId(e.level().dimension().location())); // getDimId
        put("height", (e, a) -> new NumericValue(e.getDimensions(Pose.STANDING).height()));
        put("width", (e, a) -> new NumericValue(e.getDimensions(Pose.STANDING).width()));
        put("eye_height", (e, a) -> new NumericValue(e.getEyeHeight()));
        put("age", (e, a) -> new NumericValue(e.tickCount));
        put("breeding_age", (e, a) -> e instanceof AgeableMob am ? new NumericValue(am.getAge()) : Value.NULL);
        put("despawn_timer", (e, a) -> e instanceof LivingEntity le ? new NumericValue(le.getNoActionTime()) : Value.NULL);
        put("blue_skull", (e, a) -> e instanceof WitherSkull w ? BooleanValue.of(w.isDangerous()) : Value.NULL);
        put("offering_flower", (e, a) -> e instanceof IronGolem ig ? BooleanValue.of(ig.getOfferFlowerTick() > 0) : Value.NULL);
        put("item", (e, a) -> e instanceof ItemEntity ie ? ValueConversions.of(ie.getItem(), e.getServer().registryAccess()) : e instanceof ItemFrame frame ? ValueConversions.of(frame.getItem(), e.getServer().registryAccess()) : Value.NULL);
        put("count", (e, a) -> (e instanceof ItemEntity ie) ? new NumericValue(ie.getItem().getCount()) : Value.NULL);
        put("pickup_delay", (e, a) -> (e instanceof ItemEntity ie) ? new NumericValue(Vanilla.ItemEntity_getPickupDelay(ie)) : Value.NULL);
        put("portal_cooldown", (e, a) -> new NumericValue(Vanilla.Entity_getPublicNetherPortalCooldown(e)));
        put("portal_timer", (e, a) -> new NumericValue(Vanilla.Entity_getPortalTimer(e)));
        // ItemEntity -> despawn timer via ssGetAge
        put("is_baby", (e, a) -> (e instanceof LivingEntity le) ? BooleanValue.of(le.isBaby()) : Value.NULL);
        put("target", (e, a) -> {
            if (e instanceof Mob mob)
            {
                LivingEntity target = mob.getTarget(); // there is also getAttacking in living....
                if (target != null)
                {
                    return new EntityValue(target);
                }
            }
            return Value.NULL;
        });
        put("home", (e, a) -> e instanceof Mob mob ? (mob.getRestrictRadius() > 0) ? new BlockValue(null, (ServerLevel) e.level(), mob.getRestrictCenter()) : Value.FALSE : Value.NULL);
        put("spawn_point", (e, a) -> {
            if (e instanceof ServerPlayer spe)
            {
                if (spe.getRespawnPosition() == null)
                {
                    return Value.FALSE;
                }
                return ListValue.of(
                        ValueConversions.of(spe.getRespawnPosition()),
                        ValueConversions.of(spe.getRespawnDimension()),
                        new NumericValue(spe.getRespawnAngle()),
                        BooleanValue.of(spe.isRespawnForced())
                );
            }
            return Value.NULL;
        });
        put("pose", (e, a) -> new StringValue(e.getPose().name().toLowerCase(Locale.ROOT)));
        put("sneaking", (e, a) -> e.isShiftKeyDown() ? Value.TRUE : Value.FALSE);
        put("sprinting", (e, a) -> e.isSprinting() ? Value.TRUE : Value.FALSE);
        put("swimming", (e, a) -> e.isSwimming() ? Value.TRUE : Value.FALSE);
        put("swinging", (e, a) -> e instanceof LivingEntity le ? BooleanValue.of(le.swinging) : Value.NULL);
        put("air", (e, a) -> new NumericValue(e.getAirSupply()));
        put("language", (e, a) -> !(e instanceof ServerPlayer p) ? NULL : StringValue.of(Vanilla.ServerPlayer_getLanguage(p)));
        put("persistence", (e, a) -> e instanceof Mob mob ? BooleanValue.of(mob.isPersistenceRequired()) : Value.NULL);
        put("hunger", (e, a) -> e instanceof Player player ? new NumericValue(player.getFoodData().getFoodLevel()) : Value.NULL);
        put("saturation", (e, a) -> e instanceof Player player ? new NumericValue(player.getFoodData().getSaturationLevel()) : Value.NULL);
        put("exhaustion", (e, a) -> e instanceof Player player ? new NumericValue(((FoodDataInterface)player.getFoodData()).getCMExhaustionLevel()) : Value.NULL);
        put("absorption", (e, a) -> e instanceof Player player ? new NumericValue(player.getAbsorptionAmount()) : Value.NULL);
        put("xp", (e, a) -> e instanceof Player player ? new NumericValue(player.totalExperience) : Value.NULL);
        put("xp_level", (e, a) -> e instanceof Player player ? new NumericValue(player.experienceLevel) : Value.NULL);
        put("xp_progress", (e, a) -> e instanceof Player player ? new NumericValue(player.experienceProgress) : Value.NULL);
        put("score", (e, a) -> e instanceof Player player ? new NumericValue(player.getScore()) : Value.NULL);
        put("jumping", (e, a) -> e instanceof LivingEntity le ? Vanilla.LivingEntity_isJumping(le) ? Value.TRUE : Value.FALSE : Value.NULL);
        put("gamemode", (e, a) -> e instanceof ServerPlayer sp ? new StringValue(sp.gameMode.getGameModeForPlayer().getName()) : Value.NULL);
        put("path", (e, a) -> {
            if (e instanceof Mob mob)
            {
                Path path = mob.getNavigation().getPath();
                if (path == null)
                {
                    return Value.NULL;
                }
                return ValueConversions.fromPath((ServerLevel) e.getCommandSenderWorld(), path);
            }
            return Value.NULL;
        });

        put("brain", (e, a) -> {
            String module = a.getString();
            MemoryModuleType<?> moduleType = e.level().registryAccess().lookupOrThrow(Registries.MEMORY_MODULE_TYPE).getValue(InputValidator.identifierOf(module));
            if (moduleType == MemoryModuleType.DUMMY)
            {
                return Value.NULL;
            }
            if (e instanceof LivingEntity livingEntity)
            {
                Brain<?> brain = livingEntity.getBrain();
                Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = brain.getMemories();
                Optional<? extends ExpirableValue<?>> optmemory = memories.get(moduleType);
                if (optmemory == null || !optmemory.isPresent())
                {
                    return Value.NULL;
                }
                ExpirableValue<?> memory = optmemory.get();
                return ValueConversions.fromTimedMemory(e, memory.getTimeToLive(), memory.getValue());
            }
            return Value.NULL;
        });
        put("gamemode_id", (e, a) -> e instanceof ServerPlayer sp ? new NumericValue(sp.gameMode.getGameModeForPlayer().getId()) : Value.NULL);
        put("permission_level", (e, a) -> {
            if (e instanceof ServerPlayer spe)
            {
                for (int i = 4; i >= 0; i--)
                {
                    if (spe.hasPermissions(i))
                    {
                        return new NumericValue(i);
                    }

                }
                return new NumericValue(0);
            }
            return Value.NULL;
        });

        put("player_type", (e, a) -> {
            if (e instanceof Player p)
            {
                String moddedType = Carpet.isModdedPlayer(p);
                if (moddedType != null)
                {
                    return StringValue.of(moddedType);
                }
                MinecraftServer server = p.getCommandSenderWorld().getServer();
                if (server.isDedicatedServer())
                {
                    return new StringValue("multiplayer");
                }
                boolean runningLan = server.isPublished();
                if (!runningLan)
                {
                    return new StringValue("singleplayer");
                }
                boolean isowner = server.isSingleplayerOwner(p.getGameProfile());
                if (isowner)
                {
                    return new StringValue("lan_host");
                }
                return new StringValue("lan player");
                // realms?
            }
            return Value.NULL;
        });

        put("client_brand", (e, a) -> e instanceof ServerPlayer sp ? StringValue.of(Carpet.getPlayerStatus(sp)) : Value.NULL);
        put("team", (e, a) -> e.getTeam() == null ? Value.NULL : new StringValue(e.getTeam().getName()));
        put("ping", (e, a) -> e instanceof ServerPlayer sp ? new NumericValue(sp.connection.latency()) : Value.NULL);

        //spectating_entity
        // isGlowing
        put("effect", (e, a) ->
        {
            if (!(e instanceof LivingEntity le))
            {
                return Value.NULL;
            }
            if (a == null)
            {
                List<Value> effects = new ArrayList<>();
                for (MobEffectInstance p : le.getActiveEffects())
                {
                    effects.add(ListValue.of(
                            new StringValue(p.getDescriptionId().replaceFirst("^effect\\.minecraft\\.", "")),
                            new NumericValue(p.getAmplifier()),
                            new NumericValue(p.getDuration())
                    ));
                }
                return ListValue.wrap(effects);
            }
            String effectName = a.getString();
            Holder<MobEffect> potion = BuiltInRegistries.MOB_EFFECT.get(ResourceKey.create(Registries.MOB_EFFECT, InputValidator.identifierOf(effectName))).orElseThrow(() -> new InternalExpressionException("No such an effect: " + effectName));
            if (!le.hasEffect(potion))
            {
                return Value.NULL;
            }
            MobEffectInstance pe = le.getEffect(potion);
            return ListValue.of(new NumericValue(pe.getAmplifier()), new NumericValue(pe.getDuration()));
        });

        put("health", (e, a) -> e instanceof LivingEntity le ? new NumericValue(le.getHealth()) : Value.NULL);
        put("may_fly", (e, a) -> e instanceof ServerPlayer player ? BooleanValue.of(player.getAbilities().mayfly) : Value.NULL);
        put("flying", (e, v) -> e instanceof ServerPlayer player ? BooleanValue.of(player.getAbilities().flying) : Value.NULL);
        put("may_build", (e, v) -> e instanceof ServerPlayer player ? BooleanValue.of(player.getAbilities().mayBuild) : Value.NULL);
        put("insta_build", (e, v) -> e instanceof ServerPlayer player ? BooleanValue.of(player.getAbilities().instabuild) : Value.NULL);
        put("fly_speed", (e, v) -> e instanceof ServerPlayer player ? NumericValue.of(player.getAbilities().getFlyingSpeed()) : Value.NULL);
        put("walk_speed", (e, v) -> e instanceof ServerPlayer player ? NumericValue.of(player.getAbilities().getWalkingSpeed()) : Value.NULL);
        put("holds", (e, a) -> {
            EquipmentSlot where = EquipmentSlot.MAINHAND;
            if (a != null)
            {
                where = inventorySlots.get(a.getString());
            }
            if (where == null)
            {
                throw new InternalExpressionException("Unknown inventory slot: " + a.getString());
            }
            if (e instanceof LivingEntity le)
            {
                return ValueConversions.of(le.getItemBySlot(where), e.getServer().registryAccess());
            }
            return Value.NULL;
        });

        put("selected_slot", (e, a) -> e instanceof Player p ? new NumericValue(p.getInventory().selected) : Value.NULL);

        put("active_block", (e, a) -> {
            if (e instanceof ServerPlayer sp)
            {
                BlockPos pos = Vanilla.ServerPlayerGameMode_getCurrentBlockPosition(sp.gameMode);
                if (pos == null)
                {
                    return Value.NULL;
                }
                return new BlockValue(null, sp.serverLevel(), pos);
            }
            return Value.NULL;
        });

        put("breaking_progress", (e, a) -> {
            if (e instanceof ServerPlayer sp)
            {
                int progress = Vanilla.ServerPlayerGameMode_getCurrentBlockBreakingProgress(sp.gameMode);
                return progress < 0 ? Value.NULL : new NumericValue(progress);
            }
            return Value.NULL;
        });


        put("facing", (e, a) -> {
            int index = 0;
            if (a != null)
            {
                index = (6 + (int) NumericValue.asNumber(a).getLong()) % 6;
            }
            if (index < 0 || index > 5)
            {
                throw new InternalExpressionException("Facing order should be between -6 and 5");
            }

            return new StringValue(Direction.orderedByNearest(e)[index].getSerializedName());
        });

        put("trace", (e, a) ->
        {
            float reach = 4.5f;
            boolean entities = true;
            boolean liquids = false;
            boolean blocks = true;
            boolean exact = false;

            if (a != null)
            {
                if (!(a instanceof ListValue lv))
                {
                    reach = (float) NumericValue.asNumber(a).getDouble();
                }
                else
                {
                    List<Value> args = lv.getItems();
                    if (args.size() == 0)
                    {
                        throw new InternalExpressionException("'trace' needs more arguments");
                    }
                    reach = (float) NumericValue.asNumber(args.get(0)).getDouble();
                    if (args.size() > 1)
                    {
                        entities = false;
                        blocks = false;
                        for (int i = 1; i < args.size(); i++)
                        {
                            String what = args.get(i).getString();
                            if (what.equalsIgnoreCase("entities"))
                            {
                                entities = true;
                            }
                            else if (what.equalsIgnoreCase("blocks"))
                            {
                                blocks = true;
                            }
                            else if (what.equalsIgnoreCase("liquids"))
                            {
                                liquids = true;
                            }
                            else if (what.equalsIgnoreCase("exact"))
                            {
                                exact = true;
                            }

                            else
                            {
                                throw new InternalExpressionException("Incorrect tracing: " + what);
                            }
                        }
                    }
                }
            }
            else if (e instanceof ServerPlayer sp && sp.gameMode.isCreative())
            {
                reach = 5.0f;
            }

            HitResult hitres;
            if (entities && !blocks)
            {
                hitres = Tracer.rayTraceEntities(e, 1, reach, reach * reach);
            }
            else if (entities)
            {
                hitres = Tracer.rayTrace(e, 1, reach, liquids);
            }
            else
            {
                hitres = Tracer.rayTraceBlocks(e, 1, reach, liquids);
            }

            if (hitres == null)
            {
                return Value.NULL;
            }
            if (exact && hitres.getType() != HitResult.Type.MISS)
            {
                return ValueConversions.of(hitres.getLocation());
            }
            switch (hitres.getType())
            {
                case MISS:
                    return Value.NULL;
                case BLOCK:
                    return new BlockValue((ServerLevel) e.getCommandSenderWorld(), ((BlockHitResult) hitres).getBlockPos());
                case ENTITY:
                    return new EntityValue(((EntityHitResult) hitres).getEntity());
            }
            return Value.NULL;
        });

        put("attribute", (e, a) -> {
            if (!(e instanceof LivingEntity el))
            {
                return Value.NULL;
            }
            Registry<Attribute> attributes = e.level().registryAccess().lookupOrThrow(Registries.ATTRIBUTE);
            if (a == null)
            {
                AttributeMap container = el.getAttributes();
                return MapValue.wrap(attributes.listElements().filter(container::hasAttribute).collect(Collectors.toMap(aa -> ValueConversions.of(aa.key()), aa -> NumericValue.of(container.getValue(aa)))));
            }
            ResourceLocation id = InputValidator.identifierOf(a.getString());
            Holder<Attribute> attrib = attributes.get(id).orElseThrow(
                    () -> new InternalExpressionException("Unknown attribute: " + a.getString())
            );
            if (!el.getAttributes().hasAttribute(attrib))
            {
                return Value.NULL;
            }
            return NumericValue.of(el.getAttributeValue(attrib));
        });

        put("nbt", (e, a) -> {
            CompoundTag nbttagcompound = e.saveWithoutId((new CompoundTag()));
            if (a == null)
            {
                return new NBTSerializableValue(nbttagcompound);
            }
            return new NBTSerializableValue(nbttagcompound).get(a);
        });

        put("category", (e, a) -> {
            return new StringValue(e.getType().getCategory().toString().toLowerCase(Locale.ROOT));
        });
    }};

    public void set(String what, @Nullable Value toWhat)
    {
        if (!(featureModifiers.containsKey(what)))
        {
            throw new InternalExpressionException("Unknown entity action: " + what);
        }
        try
        {
            featureModifiers.get(what).accept(getEntity(), toWhat);
        }
        catch (NullPointerException npe)
        {
            throw new InternalExpressionException("'modify' for '" + what + "' expects a value");
        }
        catch (IndexOutOfBoundsException ind)
        {
            throw new InternalExpressionException("Wrong number of arguments for `modify` option: " + what);
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
        {
            return;
        }
        if (e instanceof ServerPlayer sp)
        {
            // this forces position but doesn't angles for some reason. Need both in the API in the future.
            EnumSet<Relative> set = EnumSet.noneOf(Relative.class);
            set.add(Relative.X_ROT);
            set.add(Relative.Y_ROT);

            sp.connection.teleport(new PositionMoveRotation(new Vec3(x, y, z), Vec3.ZERO, yaw, pitch), set);
        }
        else
        {
            e.snapTo(x, y, z, yaw, pitch);
            // we were sending to players for not-living entites, that were untracked. Living entities should be tracked.
            //((ServerWorld) e.getEntityWorld()).getChunkManager().sendToNearbyPlayers(e, new EntityS2CPacket.(e));
            if (e instanceof LivingEntity le)
            {
                le.yBodyRotO = le.yRotO = yaw;
                le.yHeadRotO = le.yHeadRot = yaw;
                // seems universal for:
                //e.setHeadYaw(yaw);
                //e.setYaw(yaw);
            }
            else
            {
                ((ServerLevel) e.getCommandSenderWorld()).getChunkSource().broadcastAndSend(e, ClientboundEntityPositionSyncPacket.of(e));
            }
        }
    }

    private static void updateVelocity(Entity e, double scale)
    {
        e.hurtMarked = true;
        if (Math.abs(scale) > 10000)
        {
            CarpetScriptServer.LOG.warn("Moved entity " + e.getScoreboardName() + " " + e.getName() + " at " + e.position() + " extremely fast: " + e.getDeltaMovement());
        }
    }

    private static final Map<String, BiConsumer<Entity, Value>> featureModifiers = new HashMap<String, BiConsumer<Entity, Value>>()
    {{
        put("remove", (entity, value) -> entity.discard()); // using discard here - will see other options if valid
        put("age", (e, v) -> e.tickCount = Math.abs((int) NumericValue.asNumber(v).getLong()));
        put("health", (e, v) -> {
            float health = (float) NumericValue.asNumber(v).getDouble();
            if (health <= 0f && e instanceof ServerPlayer player)
            {
                if (player.containerMenu != null)
                {
                    // if player dies with open container, then that causes NPE on the client side
                    // its a client side bug that may never surface unless vanilla gets into scripting at some point
                    // bug: #228
                    player.closeContainer();
                }
                ((LivingEntity) e).setHealth(health);
            }
            if (e instanceof LivingEntity le)
            {
                le.setHealth(health);
            }
        });

        put("may_fly", (e, v) -> {
            boolean mayFly = v.getBoolean();
            if (e instanceof ServerPlayer player)
            {
                player.getAbilities().mayfly = mayFly;
                if (!mayFly && player.getAbilities().flying)
                {
                    player.getAbilities().flying = false;
                }
                player.onUpdateAbilities();
            }
        });

        put("flying", (e, v) -> {
            boolean flying = v.getBoolean();
            if (e instanceof ServerPlayer player)
            {
                player.getAbilities().flying = flying;
                player.onUpdateAbilities();
            }
        });

        put("may_build", (e, v) -> {
            boolean mayBuild = v.getBoolean();
            if (e instanceof ServerPlayer player)
            {
                player.getAbilities().mayBuild = mayBuild;
                player.onUpdateAbilities();
            }
        });

        put("insta_build", (e, v) -> {
            boolean instaBuild = v.getBoolean();
            if (e instanceof ServerPlayer player)
            {
                player.getAbilities().instabuild = instaBuild;
                player.onUpdateAbilities();
            }
        });

        put("fly_speed", (e, v) -> {
            float flySpeed = NumericValue.asNumber(v).getFloat();
            if (e instanceof ServerPlayer player)
            {
                player.getAbilities().setFlyingSpeed(flySpeed);
                player.onUpdateAbilities();
            }
        });

        put("walk_speed", (e, v) -> {
            float walkSpeed = NumericValue.asNumber(v).getFloat();
            if (e instanceof ServerPlayer player)
            {
                player.getAbilities().setWalkingSpeed(walkSpeed);
                player.onUpdateAbilities();
            }
        });

        put("selected_slot", (e, v) ->
        {
            if (e instanceof ServerPlayer player)
            {
                int slot = NumericValue.asNumber(v).getInt();
                player.connection.send(new ClientboundSetHeldSlotPacket(slot));
            }
        });

        // todo add handling of the source for extra effects
        /*put("damage", (e, v) -> {
            float dmgPoints;
            DamageSource source;
            if (v instanceof final ListValue lv && lv.getItems().size() > 1)
            {
                   List<Value> vals = lv.getItems();
                   dmgPoints = (float) NumericValue.asNumber(v).getDouble();
                   source = DamageSource ... yeah...
            }
            else
            {

            }
        });*/
        put("kill", (e, v) -> e.kill((ServerLevel) e.level()));
        put("location", (e, v) ->
        {
            if (!(v instanceof ListValue lv))
            {
                throw new InternalExpressionException("Expected a list of 5 parameters as a second argument");
            }
            List<Value> coords = lv.getItems();
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
            if (!(v instanceof ListValue lv))
            {
                throw new InternalExpressionException("Expected a list of 3 parameters as a second argument");
            }
            List<Value> coords = lv.getItems();
            updatePosition(e,
                    NumericValue.asNumber(coords.get(0)).getDouble(),
                    NumericValue.asNumber(coords.get(1)).getDouble(),
                    NumericValue.asNumber(coords.get(2)).getDouble(),
                    e.getYRot(),
                    e.getXRot()
            );
        });
        put("x", (e, v) -> updatePosition(e, NumericValue.asNumber(v).getDouble(), e.getY(), e.getZ(), e.getYRot(), e.getXRot()));
        put("y", (e, v) -> updatePosition(e, e.getX(), NumericValue.asNumber(v).getDouble(), e.getZ(), e.getYRot(), e.getXRot()));
        put("z", (e, v) -> updatePosition(e, e.getX(), e.getY(), NumericValue.asNumber(v).getDouble(), e.getYRot(), e.getXRot()));
        put("yaw", (e, v) -> updatePosition(e, e.getX(), e.getY(), e.getZ(), ((float) NumericValue.asNumber(v).getDouble()) % 360, e.getXRot()));
        put("head_yaw", (e, v) ->
        {
            if (e instanceof LivingEntity)
            {
                e.setYHeadRot((float) NumericValue.asNumber(v).getDouble() % 360);
            }
        });
        put("body_yaw", (e, v) ->
        {
            if (e instanceof LivingEntity)
            {
                e.setYRot((float) NumericValue.asNumber(v).getDouble() % 360);
            }
        });

        put("pitch", (e, v) -> updatePosition(e, e.getX(), e.getY(), e.getZ(), e.getYRot(), Mth.clamp((float) NumericValue.asNumber(v).getDouble(), -90, 90)));

        put("look", (e, v) -> {
            if (!(v instanceof ListValue lv))
            {
                throw new InternalExpressionException("Expected a list of 3 parameters as a second argument");
            }
            List<Value> vec = lv.getItems();
            float x = NumericValue.asNumber(vec.get(0)).getFloat();
            float y = NumericValue.asNumber(vec.get(1)).getFloat();
            float z = NumericValue.asNumber(vec.get(2)).getFloat();
            float l = Mth.sqrt(x * x + y * y + z * z);
            if (l == 0)
            {
                return;
            }
            x /= l;
            y /= l;
            z /= l;
            float pitch = (float) -Math.asin(y) / 0.017453292F;
            float yaw = (float) (x == 0 && z == 0 ? e.getYRot() : Mth.atan2(-x, z) / 0.017453292F);
            updatePosition(e, e.getX(), e.getY(), e.getZ(), yaw, pitch);
        });

        //"turn"
        //"nod"

        put("move", (e, v) ->
        {
            if (!(v instanceof ListValue lv))
            {
                throw new InternalExpressionException("Expected a list of 3 parameters as a second argument");
            }
            List<Value> coords = lv.getItems();
            updatePosition(e,
                    e.getX() + NumericValue.asNumber(coords.get(0)).getDouble(),
                    e.getY() + NumericValue.asNumber(coords.get(1)).getDouble(),
                    e.getZ() + NumericValue.asNumber(coords.get(2)).getDouble(),
                    e.getYRot(),
                    e.getXRot()
            );
        });

        put("motion", (e, v) ->
        {
            if (!(v instanceof ListValue lv))
            {
                throw new InternalExpressionException("Expected a list of 3 parameters as a second argument");
            }
            List<Value> coords = lv.getItems();
            double dx = NumericValue.asNumber(coords.get(0)).getDouble();
            double dy = NumericValue.asNumber(coords.get(1)).getDouble();
            double dz = NumericValue.asNumber(coords.get(2)).getDouble();
            e.setDeltaMovement(dx, dy, dz);
            updateVelocity(e, Mth.absMax(Mth.absMax(dx, dy), dz));
        });
        put("motion_x", (e, v) ->
        {
            Vec3 velocity = e.getDeltaMovement();
            double dv = NumericValue.asNumber(v).getDouble();
            e.setDeltaMovement(dv, velocity.y, velocity.z);
            updateVelocity(e, dv);
        });
        put("motion_y", (e, v) ->
        {
            Vec3 velocity = e.getDeltaMovement();
            double dv = NumericValue.asNumber(v).getDouble();
            e.setDeltaMovement(velocity.x, dv, velocity.z);
            updateVelocity(e, dv);
        });
        put("motion_z", (e, v) ->
        {
            Vec3 velocity = e.getDeltaMovement();
            double dv = NumericValue.asNumber(v).getDouble();
            e.setDeltaMovement(velocity.x, velocity.y, dv);
            updateVelocity(e, dv);
        });

        put("accelerate", (e, v) ->
        {
            if (!(v instanceof ListValue lv))
            {
                throw new InternalExpressionException("Expected a list of 3 parameters as a second argument");
            }
            List<Value> coords = lv.getItems();
            e.push(
                    NumericValue.asNumber(coords.get(0)).getDouble(),
                    NumericValue.asNumber(coords.get(1)).getDouble(),
                    NumericValue.asNumber(coords.get(2)).getDouble()
            );
            updateVelocity(e, e.getDeltaMovement().length());

        });
        put("custom_name", (e, v) -> {
            if (v.isNull())
            {
                e.setCustomNameVisible(false);
                e.setCustomName(null);
                return;
            }
            boolean showName = false;
            if (v instanceof ListValue lv)
            {
                showName = lv.getItems().get(1).getBoolean();
                v = lv.getItems().get(0);
            }
            e.setCustomNameVisible(showName);
            e.setCustomName(FormattedTextValue.getTextByValue(v));
        });

        put("persistence", (e, v) ->
        {
            if (!(e instanceof Mob mob))
            {
                return;
            }
            if (v == null)
            {
                v = Value.TRUE;
            }
            Vanilla.Mob_setPersistence(mob, v.getBoolean());
        });

        put("dismount", (e, v) -> e.stopRiding());
        put("mount", (e, v) -> {
            if (v instanceof EntityValue ev)
            {
                e.startRiding(ev.getEntity(), true);
            }
            if (e instanceof ServerPlayer sp)
            {
                sp.connection.send(new ClientboundSetPassengersPacket(e));
            }
        });
        put("unmountable", (e, v) -> Vanilla.Entity_setPermanentVehicle(e, v == null || v.getBoolean()));
        put("drop_passengers", (e, v) -> e.ejectPassengers());
        put("mount_passengers", (e, v) -> {
            if (v == null)
            {
                throw new InternalExpressionException("'mount_passengers' needs entities to ride");
            }
            if (v instanceof EntityValue ev)
            {
                ev.getEntity().startRiding(e);
            }
            else if (v instanceof ListValue lv)
            {
                for (Value element : lv.getItems())
                {
                    if (element instanceof EntityValue ev)
                    {
                        ev.getEntity().startRiding(e);
                    }
                }
            }
        });
        put("tag", (e, v) -> {
            if (v == null)
            {
                throw new InternalExpressionException("'tag' requires parameters");
            }
            if (v instanceof ListValue lv)
            {
                for (Value element : lv.getItems())
                {
                    e.addTag(element.getString());
                }
            }
            else
            {
                e.addTag(v.getString());
            }
        });
        put("clear_tag", (e, v) -> {
            if (v == null)
            {
                throw new InternalExpressionException("'clear_tag' requires parameters");
            }
            if (v instanceof ListValue lv)
            {
                for (Value element : lv.getItems())
                {
                    e.removeTag(element.getString());
                }
            }
            else
            {
                e.removeTag(v.getString());
            }
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
            if (e instanceof AgeableMob am)
            {
                am.setAge((int) NumericValue.asNumber(v).getLong());
            }
        });
        put("talk", (e, v) -> {
            // attacks indefinitely
            if (e instanceof Mob mob)
            {
                mob.playAmbientSound();
            }
        });
        put("home", (e, v) -> {
            if (!(e instanceof PathfinderMob ec))
            {
                return;
            }
            if (v == null)
            {
                throw new InternalExpressionException("'home' requires at least one position argument, and optional distance, or null to cancel");
            }
            if (v.isNull())
            {
                ec.restrictTo(BlockPos.ZERO, -1);
                Map<String, Goal> tasks = Vanilla.Mob_getTemporaryTasks(ec);
                Vanilla.Mob_getAI(ec, false).removeGoal(tasks.get("home"));
                tasks.remove("home");
                return;
            }

            BlockPos pos;
            int distance = 16;

            if (v instanceof BlockValue bv)
            {
                pos = bv.getPos();
                if (pos == null)
                {
                    throw new InternalExpressionException("Block is not positioned in the world");
                }
            }
            else if (v instanceof ListValue lv)
            {
                List<Value> list = lv.getItems();
                Vector3Argument locator = Vector3Argument.findIn(list, 0, false, false);
                pos = BlockPos.containing(locator.vec.x, locator.vec.y, locator.vec.z);
                if (list.size() > locator.offset)
                {
                    distance = (int) NumericValue.asNumber(list.get(locator.offset)).getLong();
                }
            }
            else
            {
                throw new InternalExpressionException("'home' requires at least one position argument, and optional distance");
            }

            ec.restrictTo(pos, distance);
            Map<String, Goal> tasks = Vanilla.Mob_getTemporaryTasks(ec);
            if (!tasks.containsKey("home"))
            {
                Goal task = new MoveTowardsRestrictionGoal(ec, 1.0D);
                tasks.put("home", task);
                Vanilla.Mob_getAI(ec, false).addGoal(10, task);
            }
        }); //requires mixing

        put("spawn_point", (e, a) -> {
            if (!(e instanceof ServerPlayer spe))
            {
                return;
            }
            if (a == null)
            {
                spe.setRespawnPosition(null, null, 0, false, false);
            }
            else if (a instanceof ListValue lv)
            {
                List<Value> params = lv.getItems();
                Vector3Argument blockLocator = Vector3Argument.findIn(params, 0, false, false);
                BlockPos pos = BlockPos.containing(blockLocator.vec);
                ResourceKey<Level> world = spe.getCommandSenderWorld().dimension();
                float angle = spe.getYHeadRot();
                boolean forced = false;
                if (params.size() > blockLocator.offset)
                {
                    Value worldValue = params.get(blockLocator.offset);
                    world = ValueConversions.dimFromValue(worldValue, spe.getServer()).dimension();
                    if (params.size() > blockLocator.offset + 1)
                    {
                        angle = NumericValue.asNumber(params.get(blockLocator.offset + 1), "angle").getFloat();
                        if (params.size() > blockLocator.offset + 2)
                        {
                            forced = params.get(blockLocator.offset + 2).getBoolean();
                        }
                    }
                }
                spe.setRespawnPosition(world, pos, angle, forced, false);
            }
            else if (a instanceof BlockValue bv)
            {
                if (bv.getPos() == null || bv.getWorld() == null)
                {
                    throw new InternalExpressionException("block for spawn modification should be localised in the world");
                }
                spe.setRespawnPosition(bv.getWorld().dimension(), bv.getPos(), e.getYRot(), true, false); // yaw
            }
            else if (a.isNull())
            {
                spe.setRespawnPosition(null, null, 0, false, false);
            }
            else
            {
                throw new InternalExpressionException("modifying player respawn point requires a block position, optional world, optional angle, and optional force");
            }
        });

        put("pickup_delay", (e, v) ->
        {
            if (e instanceof ItemEntity ie)
            {
                ie.setPickUpDelay((int) NumericValue.asNumber(v).getLong());
            }
        });

        put("despawn_timer", (e, v) ->
        {
            if (e instanceof LivingEntity le)
            {
                le.setNoActionTime((int) NumericValue.asNumber(v).getLong());
            }
        });

        put("portal_cooldown", (e, v) ->
        {
            if (v == null)
            {
                throw new InternalExpressionException("'portal_cooldown' requires a value to set");
            }
            Vanilla.Entity_setPublicNetherPortalCooldown(e, NumericValue.asNumber(v).getInt());
        });

        put("portal_timer", (e, v) ->
        {
            if (v == null)
            {
                throw new InternalExpressionException("'portal_timer' requires a value to set");
            }
            Vanilla.Entity_setPortalTimer(e, NumericValue.asNumber(v).getInt());
        });

        put("ai", (e, v) ->
        {
            if (e instanceof Mob mob)
            {
                mob.setNoAi(!v.getBoolean());
            }
        });

        put("no_clip", (e, v) ->
        {
            if (v == null)
            {
                e.noPhysics = true;
            }
            else
            {
                e.noPhysics = v.getBoolean();
            }
        });
        put("effect", (e, v) ->
        {
            if (!(e instanceof LivingEntity le))
            {
                return;
            }
            if (v == null)
            {
                le.removeAllEffects();
                return;
            }
            else if (v instanceof ListValue lv)
            {
                List<Value> list = lv.getItems();
                if (list.size() >= 1 && list.size() <= 6)
                {
                    String effectName = list.get(0).getString();
                    Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.get(InputValidator.identifierOf(effectName)).orElseThrow(() -> new InternalExpressionException("No such an effect: " + effectName));
                    if (list.size() == 1)
                    {
                        le.removeEffect(effect);
                        return;
                    }
                    int duration = (int) NumericValue.asNumber(list.get(1)).getLong();
                    if (duration == 0)
                    {
                        le.removeEffect(effect);
                        return;
                    }
                    if (duration < 0)
                    {
                        duration = -1;
                    }
                    int amplifier = 0;
                    if (list.size() > 2)
                    {
                        amplifier = (int) NumericValue.asNumber(list.get(2)).getLong();
                    }
                    boolean showParticles = true;
                    if (list.size() > 3)
                    {
                        showParticles = list.get(3).getBoolean();
                    }
                    boolean showIcon = true;
                    if (list.size() > 4)
                    {
                        showIcon = list.get(4).getBoolean();
                    }
                    boolean ambient = false;
                    if (list.size() > 5)
                    {
                        ambient = list.get(5).getBoolean();
                    }
                    le.addEffect(new MobEffectInstance(effect, duration, amplifier, ambient, showParticles, showIcon));
                    return;
                }
            }
            else
            {
                String effectName = v.getString();
                Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.get(InputValidator.identifierOf(effectName)).orElseThrow(() -> new InternalExpressionException("No such an effect: " + effectName));
                le.removeEffect(effect);
                return;
            }
            throw new InternalExpressionException("'effect' needs either no arguments (clear) or effect name, duration, and optional amplifier, show particles, show icon and ambient");
        });

        put("gamemode", (e, v) -> {
            if (!(e instanceof ServerPlayer sp))
            {
                return;
            }
            GameType toSet = v instanceof NumericValue ?
                    GameType.byId(((NumericValue) v).getInt()) :
                    GameType.byName(v.getString().toLowerCase(Locale.ROOT), null);
            if (toSet != null)
            {
                sp.setGameMode(toSet);
            }
        });

        put("jumping", (e, v) -> {
            if (!(e instanceof LivingEntity le))
            {
                return;
            }
            le.setJumping(v.getBoolean());
        });

        put("jump", (e, v) -> {
            if (e instanceof LivingEntity le)
            {
                Vanilla.LivingEntity_setJumping(le);
            }
            else
            {
                EntityTools.genericJump(e);
            }
        });

        put("swing", (e, v) -> {
            if (e instanceof LivingEntity le)
            {
                InteractionHand hand = InteractionHand.MAIN_HAND;
                if (v != null)
                {
                    String handString = v.getString().toLowerCase(Locale.ROOT);
                    if (handString.equals("offhand") || handString.equals("off_hand"))
                    {
                        hand = InteractionHand.OFF_HAND;
                    }
                }
                le.swing(hand, true);
            }
        });

        put("silent", (e, v) -> e.setSilent(v.getBoolean()));

        put("gravity", (e, v) -> e.setNoGravity(!v.getBoolean()));

        put("invulnerable", (e, v) -> {
            boolean invulnerable = v.getBoolean();
            if (e instanceof ServerPlayer player)
            {
                player.getAbilities().invulnerable = invulnerable;
                player.onUpdateAbilities();
            }
            else
            {
                e.setInvulnerable(invulnerable);
            }
        });

        put("fire", (e, v) -> e.setRemainingFireTicks((int) NumericValue.asNumber(v).getLong()));
        put("frost", (e, v) -> e.setTicksFrozen((int) NumericValue.asNumber(v).getLong()));

        put("hunger", (e, v) -> {
            if (e instanceof Player p)
            {
                p.getFoodData().setFoodLevel((int) NumericValue.asNumber(v).getLong());
            }
        });

        put("exhaustion", (e, v) -> {
            if (e instanceof Player p)
            {
                ((FoodDataInterface)p.getFoodData()).setExhaustion(NumericValue.asNumber(v).getFloat());
            }
        });

        put("add_exhaustion", (e, v) -> {
            if (e instanceof Player p)
            {
                p.getFoodData().addExhaustion(NumericValue.asNumber(v).getFloat());
            }
        });

        put("absorption", (e, v) -> {
            if (e instanceof Player p)
            {
                p.setAbsorptionAmount(NumericValue.asNumber(v, "absorbtion").getFloat());
            }
        });

        put("add_xp", (e, v) -> {
            if (e instanceof Player p)
            {
                p.giveExperiencePoints(NumericValue.asNumber(v, "add_xp").getInt());
            }
        });

        put("xp_level", (e, v) -> {
            if (e instanceof Player p)
            {
                p.giveExperienceLevels(NumericValue.asNumber(v, "xp_level").getInt() - p.experienceLevel);
            }
        });

        put("xp_progress", (e, v) -> {
            if (e instanceof ServerPlayer p)
            {
                p.experienceProgress = NumericValue.asNumber(v, "xp_progress").getFloat();
                p.connection.send(new ClientboundSetExperiencePacket(p.experienceProgress, p.totalExperience, p.experienceLevel));
            }
        });

        put("xp_score", (e, v) -> {
            if (e instanceof Player p)
            {
                p.setScore(NumericValue.asNumber(v, "xp_score").getInt());
            }
        });

        put("saturation", (e, v) -> {
            if (e instanceof Player p)
            {
                p.getFoodData().setSaturation(NumericValue.asNumber(v, "saturation").getFloat());
            }
        });

        put("air", (e, v) -> e.setAirSupply(NumericValue.asNumber(v, "air").getInt()));

        put("breaking_progress", (e, a) -> {
            if (e instanceof ServerPlayer sp)
            {
                int progress = (a == null || a.isNull()) ? -1 : NumericValue.asNumber(a).getInt();
                Vanilla.ServerPlayerGameMode_setBlockBreakingProgress(sp.gameMode, progress);
            }
        });

        put("nbt", (e, v) -> {
            if (!(e instanceof Player))
            {
                UUID uUID = e.getUUID();
                Value tagValue = NBTSerializableValue.fromValue(v);
                if (tagValue instanceof NBTSerializableValue nbtsv)
                {
                    e.load(nbtsv.getCompoundTag());
                    e.setUUID(uUID);
                }
            }
        });
        put("nbt_merge", (e, v) -> {
            if (!(e instanceof Player))
            {
                UUID uUID = e.getUUID();
                Value tagValue = NBTSerializableValue.fromValue(v);
                if (tagValue instanceof NBTSerializableValue nbtsv)
                {
                    CompoundTag nbttagcompound = e.saveWithoutId((new CompoundTag()));
                    nbttagcompound.merge(nbtsv.getCompoundTag());
                    e.load(nbttagcompound);
                    e.setUUID(uUID);
                }
            }
        });

        put("blue_skull", (e, v) -> {
            if (e instanceof WitherSkull w)
            {
                w.setDangerous(v.getBoolean());
            }

        });
        put("offering_flower", (e, v) -> {
            if (e instanceof IronGolem ig)
            {
                ig.offerFlower(v.getBoolean());
            }

        });
        put("item", (e, v) -> {
            ItemStack item = ValueConversions.getItemStackFromValue(v, true, e.level().registryAccess());
            if (e instanceof ItemEntity itementity)
            {
                itementity.setItem(item);
            }
            if (e instanceof ItemFrame itemframe)
            {
                itemframe.setItem(item);
            }
        });
        // "dimension"      []
        // "count",         []
        // "effect_"name    []
    }};

    public void setEvent(CarpetContext cc, String eventName, FunctionValue fun, List<Value> args)
    {
        EntityEventsGroup.Event event = EntityEventsGroup.Event.byName.get(eventName);
        if (event == null)
        {
            throw new InternalExpressionException("Unknown entity event: " + eventName);
        }
        Vanilla.Entity_getEventContainer(getEntity()).addEvent(event, cc.host, fun, args);
    }

    @Override
    public net.minecraft.nbt.Tag toTag(boolean force, RegistryAccess regs)
    {
        if (!force)
        {
            throw new NBTSerializableValue.IncompatibleTypeException(this);
        }
        CompoundTag tag = new CompoundTag();
        tag.put("Data", getEntity().saveWithoutId(new CompoundTag()));
        Registry<EntityType<?>> reg = getEntity().level().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
        tag.put("Name", StringTag.valueOf(reg.getKey(getEntity().getType()).toString()));
        return tag;
    }
}
