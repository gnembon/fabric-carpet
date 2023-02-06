package carpet.script.external;

import carpet.CarpetSettings;
import carpet.fakes.BiomeInterface;
import carpet.fakes.BlockPredicateInterface;
import carpet.fakes.BlockStateArgumentInterface;
import carpet.fakes.ChunkTicketManagerInterface;
import carpet.fakes.CommandDispatcherInterface;
import carpet.fakes.EntityInterface;
import carpet.fakes.IngredientInterface;
import carpet.fakes.InventoryBearerInterface;
import carpet.fakes.ItemEntityInterface;
import carpet.fakes.LivingEntityInterface;
import carpet.fakes.MinecraftServerInterface;
import carpet.fakes.MobEntityInterface;
import carpet.fakes.RandomStateVisitorAccessor;
import carpet.fakes.RecipeManagerInterface;
import carpet.fakes.AbstractContainerMenuInterface;
import carpet.fakes.ServerChunkManagerInterface;
import carpet.fakes.ServerPlayerInterface;
import carpet.fakes.ServerPlayerInteractionManagerInterface;
import carpet.fakes.ServerWorldInterface;
import carpet.fakes.SpawnHelperInnerInterface;
import carpet.fakes.ThreadedAnvilChunkStorageInterface;
import carpet.mixins.Objective_scarpetMixin;
import carpet.mixins.Scoreboard_scarpetMixin;
import carpet.network.ServerNetworkHandler;
import carpet.script.CarpetScriptServer;
import carpet.script.EntityEventsGroup;
import carpet.script.value.MapValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.utils.CommandHelper;
import carpet.utils.SpawnReporter;
import com.mojang.brigadier.CommandDispatcher;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.level.Ticket;
import net.minecraft.tags.TagKey;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.PotentialCalculator;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class Vanilla
{
    public static void MinecraftServer_forceTick(final MinecraftServer server, final BooleanSupplier sup)
    {
        ((MinecraftServerInterface) server).forceTick(sup);
    }

    public static void ChunkMap_relightChunk(final ChunkMap chunkMap, final ChunkPos pos)
    {
        ((ThreadedAnvilChunkStorageInterface) chunkMap).relightChunk(pos);
    }

    public static Map<String, Integer> ChunkMap_regenerateChunkRegion(final ChunkMap chunkMap, final List<ChunkPos> requestedChunks)
    {
        return ((ThreadedAnvilChunkStorageInterface) chunkMap).regenerateChunkRegion(requestedChunks);
    }

    public static List<Collection<ItemStack>> Ingredient_getRecipeStacks(final Ingredient ingredient)
    {
        return ((IngredientInterface) (Object) ingredient).getRecipeStacks();
    }

    public static List<Recipe<?>> RecipeManager_getAllMatching(final RecipeManager recipeManager, final RecipeType<?> type, final ResourceLocation output, final RegistryAccess registryAccess)
    {
        return ((RecipeManagerInterface) recipeManager).getAllMatching(type, output, registryAccess);
    }

    public static int NaturalSpawner_MAGIC_NUMBER()
    {
        return SpawnReporter.MAGIC_NUMBER;
    }

    public static PotentialCalculator SpawnState_getPotentialCalculator(final NaturalSpawner.SpawnState spawnState)
    {
        return ((SpawnHelperInnerInterface) spawnState).getPotentialCalculator();
    }

    public static void Objective_setCriterion(final Objective objective, final ObjectiveCriteria criterion)
    {
        ((Objective_scarpetMixin) objective).setCriterion(criterion);
    }

    public static Map<ObjectiveCriteria, List<Objective>> Scoreboard_getObjectivesByCriterion(final Scoreboard scoreboard)
    {
        return ((Scoreboard_scarpetMixin) scoreboard).getObjectivesByCriterion();
    }

    public static ServerLevelData ServerLevel_getWorldProperties(final ServerLevel world)
    {
        return ((ServerWorldInterface) world).getWorldPropertiesCM();
    }

    public static DistanceManager ServerChunkCache_getCMTicketManager(final ServerChunkCache chunkCache)
    {
        return ((ServerChunkManagerInterface) chunkCache).getCMTicketManager();
    }

    public static Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> ChunkTicketManager_getTicketsByPosition(final DistanceManager ticketManager)
    {
        return ((ChunkTicketManagerInterface) ticketManager).getTicketsByPosition();
    }

    public static DensityFunction.Visitor RandomState_getVisitor(final RandomState randomState)
    {
        return ((RandomStateVisitorAccessor) (Object) randomState).getVisitor();
    }

    public static CompoundTag BlockInput_getTag(final BlockInput blockInput)
    {
        return ((BlockStateArgumentInterface) blockInput).getCMTag();
    }

    public static CarpetScriptServer MinecraftServer_getScriptServer(final MinecraftServer server)
    {
        return ((MinecraftServerInterface) server).getScriptServer();
    }

    public static Biome.ClimateSettings Biome_getClimateSettings(final Biome biome)
    {
        return ((BiomeInterface) (Object) biome).getClimateSettings();
    }

    public static ThreadLocal<Boolean> skipGenerationChecks(final ServerLevel level)
    { // not sure does vanilla care at all - needs checking
        return CarpetSettings.skipGenerationChecks;
    }

    public static void sendScarpetShapesDataToPlayer(final ServerPlayer player, final Tag data)
    { // dont forget to add the packet to vanilla packed handler and call ShapesRenderer.addShape to handle on client
        ServerNetworkHandler.sendCustomCommand(player, "scShapes", data);
    }

    public static int MinecraftServer_getRunPermissionLevel(final MinecraftServer server)
    {
        return CarpetSettings.runPermissionLevel;
    }

    public static String MinecraftServer_getReleaseTarget(final MinecraftServer server)
    {
        return CarpetSettings.releaseTarget;
    }

    public static boolean isDevelopmentEnvironment()
    {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    public static MapValue getServerMods(final MinecraftServer server)
    {
        final Map<Value, Value> ret = new HashMap<>();
        for (final ModContainer mod : FabricLoader.getInstance().getAllMods())
        {
            ret.put(new StringValue(mod.getMetadata().getId()), new StringValue(mod.getMetadata().getVersion().getFriendlyString()));
        }
        return MapValue.wrap(ret);
    }

    public static LevelStorageSource.LevelStorageAccess MinecraftServer_storageSource(final MinecraftServer server)
    {
        return ((MinecraftServerInterface) server).getCMSession();
    }

    public static BlockPos ServerPlayerGameMode_getCurrentBlockPosition(final ServerPlayerGameMode gameMode)
    {
        return ((ServerPlayerInteractionManagerInterface) gameMode).getCurrentBreakingBlock();
    }

    public static int ServerPlayerGameMode_getCurrentBlockBreakingProgress(final ServerPlayerGameMode gameMode)
    {
        return ((ServerPlayerInteractionManagerInterface) gameMode).getCurrentBlockBreakingProgress();
    }

    public static void ServerPlayerGameMode_setBlockBreakingProgress(final ServerPlayerGameMode gameMode, final int progress)
    {
        ((ServerPlayerInteractionManagerInterface) gameMode).setBlockBreakingProgress(progress);
    }

    public static boolean ServerPlayer_isInvalidEntityObject(final ServerPlayer player)
    {
        return ((ServerPlayerInterface) player).isInvalidEntityObject();
    }

    public static String ServerPlayer_getLanguage(final ServerPlayer player)
    {
        return ((ServerPlayerInterface) player).getLanguage();
    }

    public static GoalSelector Mob_getAI(final Mob mob, final boolean target)
    {
        return ((MobEntityInterface) mob).getAI(target);
    }

    public static Map<String, Goal> Mob_getTemporaryTasks(final Mob mob)
    {
        return ((MobEntityInterface) mob).getTemporaryTasks();
    }

    public static void Mob_setPersistence(final Mob mob, final boolean what)
    {
        ((MobEntityInterface) mob).setPersistence(what);
    }

    public static EntityEventsGroup Entity_getEventContainer(final Entity entity)
    {
        return ((EntityInterface) entity).getEventContainer();
    }

    public static boolean Entity_isPermanentVehicle(final Entity entity)
    {
        return ((EntityInterface) entity).isPermanentVehicle();
    }

    public static void Entity_setPermanentVehicle(final Entity entity, final boolean permanent)
    {
        ((EntityInterface) entity).setPermanentVehicle(permanent);
    }

    public static int Entity_getPortalTimer(final Entity entity)
    {
        return ((EntityInterface) entity).getPortalTimer();
    }

    public static void Entity_setPortalTimer(final Entity entity, final int amount)
    {
        ((EntityInterface) entity).setPortalTimer(amount);
    }

    public static int Entity_getPublicNetherPortalCooldown(final Entity entity)
    {
        return ((EntityInterface) entity).getPublicNetherPortalCooldown();
    }

    public static void Entity_setPublicNetherPortalCooldown(final Entity entity, final int what)
    {
        ((EntityInterface) entity).setPublicNetherPortalCooldown(what);
    }

    public static int ItemEntity_getPickupDelay(final ItemEntity entity)
    {
        return ((ItemEntityInterface) entity).getPickupDelayCM();
    }

    public static boolean LivingEntity_isJumping(final LivingEntity entity)
    {
        return ((LivingEntityInterface) entity).isJumpingCM();
    }

    public static void LivingEntity_setJumping(final LivingEntity entity)
    {
        ((LivingEntityInterface) entity).doJumpCM();
    }

    public static Container AbstractHorse_getInventory(final AbstractHorse horse)
    {
        return ((InventoryBearerInterface) horse).getCMInventory();
    }

    public static DataSlot AbstractContainerMenu_getDataSlot(final AbstractContainerMenu handler, final int index)
    {
        return ((AbstractContainerMenuInterface) handler).getDataSlot(index);
    }

    public static void CommandDispatcher_unregisterCommand(final CommandDispatcher<CommandSourceStack> dispatcher, final String name)
    {
        ((CommandDispatcherInterface) dispatcher).carpet$unregister(name);
    }

    public static boolean MinecraftServer_doScriptsAutoload(final MinecraftServer server)
    {
        return CarpetSettings.scriptsAutoload;
    }

    public static void MinecraftServer_notifyPlayersCommandsChanged(final MinecraftServer server)
    {
        CommandHelper.notifyPlayersCommandsChanged(server);
    }

    public static boolean ScriptServer_scriptOptimizations(final MinecraftServer scriptServer)
    {
        return CarpetSettings.scriptsOptimization;
    }

    public static boolean ScriptServer_scriptDebugging(final MinecraftServer server)
    {
        return CarpetSettings.scriptsDebugging;
    }

    public static boolean ServerPlayer_canScriptACE(final CommandSourceStack player)
    {
        return CommandHelper.canUseCommand(player, CarpetSettings.commandScriptACE);
    }

    public static boolean ServerPlayer_canScriptGeneral(final CommandSourceStack player)
    {
        return CommandHelper.canUseCommand(player, CarpetSettings.commandScript);
    }

    public static int MinecraftServer_getFillLimit(final MinecraftServer server)
    {
        return Math.max(server.getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT), CarpetSettings.fillLimit);
    }

    public record BlockPredicatePayload(BlockState state, TagKey<Block> tagKey, Map<Value, Value> properties, CompoundTag tag) {
        public static BlockPredicatePayload of(final Predicate<BlockInWorld> blockPredicate)
        {
            final BlockPredicateInterface predicateData = (BlockPredicateInterface) blockPredicate;
            return new BlockPredicatePayload(predicateData.getCMBlockState(), predicateData.getCMBlockTagKey(), predicateData.getCMProperties(), predicateData.getCMDataTag());
        }
    }

}
