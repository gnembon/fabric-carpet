package carpet.script.command;

import carpet.CarpetServer;
import carpet.fakes.BlockStateArgumentInterface;
import carpet.script.CarpetScriptHost;
import carpet.script.argument.FunctionArgument;
import carpet.script.value.BlockValue;
import carpet.script.value.BooleanValue;
import carpet.script.value.EntityValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import carpet.utils.CarpetProfiler;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.AngleArgument;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.ObjectiveCriteriaArgument;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.ScoreboardSlotArgument;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.SwizzleArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.BossBarCommands;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.scores.Scoreboard;

import static net.minecraft.commands.Commands.argument;

public abstract class CommandArgument
{
    public static CommandSyntaxException error(String text)
    {
        return new SimpleCommandExceptionType(Component.literal(text)).create();
    }

    private static final DynamicCommandExceptionType ERROR_BIOME_INVALID = new DynamicCommandExceptionType(v -> Component.translatable("commands.locate.biome.invalid", v));


    private static final List<? extends CommandArgument> baseTypes = Lists.newArrayList(
            // default
            new StringArgument(),
            // vanilla arguments as per https://minecraft.gamepedia.com/Argument_types
            new VanillaUnconfigurableArgument( "bool", BoolArgumentType::bool,
                    (c, p) -> BooleanValue.of(BoolArgumentType.getBool(c, p)), false
            ),
            new FloatArgument(),
            new IntArgument(),
            new WordArgument(), new GreedyStringArgument(),
            new VanillaUnconfigurableArgument( "yaw", AngleArgument::angle,  // angle
                    (c, p) -> new NumericValue(AngleArgument.getAngle(c, p)), true
            ),
            new BlockPosArgument(),
            new VanillaUnconfigurableArgument( "block", BlockStateArgument::block,
                    (c, p) -> {
                        BlockInput result = BlockStateArgument.getBlock(c, p);
                        return new BlockValue(result.getState(), null, null, ((BlockStateArgumentInterface)result).getCMTag() );
                    },
                    param -> (ctx, builder) -> ctx.getArgument(param, BlockStateArgument.class).listSuggestions(ctx, builder)
            ),
            new VanillaUnconfigurableArgument( "blockpredicate", BlockPredicateArgument::blockPredicate,
                    (c, p) -> ValueConversions.ofBlockPredicate(c.getSource().getServer().registryAccess(), BlockPredicateArgument.getBlockPredicate(c, p)),
                    param -> (ctx, builder) -> ctx.getArgument(param, BlockPredicateArgument.class).listSuggestions(ctx, builder)
            ),
            new VanillaUnconfigurableArgument("teamcolor", ColorArgument::color,
                    (c, p) -> {
                        ChatFormatting format = ColorArgument.getColor(c, p);
                        return ListValue.of(StringValue.of(format.getName()), ValueConversions.ofRGB(format.getColor()));
                    },
                    false
            ),
            new VanillaUnconfigurableArgument("columnpos", ColumnPosArgument::columnPos,
                    (c, p) -> ValueConversions.of(ColumnPosArgument.getColumnPos(c, p)), false
            ),
            // component  // raw json
            new VanillaUnconfigurableArgument("dimension", DimensionArgument::dimension,
                    (c, p) -> ValueConversions.of(DimensionArgument.getDimension(c, p)), false
            ),
            new EntityArgument(),
            new VanillaUnconfigurableArgument("anchor", EntityAnchorArgument::anchor,
                    (c, p) -> StringValue.of(EntityAnchorArgument.getAnchor(c, p).name()), false
            ),
            new VanillaUnconfigurableArgument("entitytype", c -> ResourceArgument.resource(c, Registries.ENTITY_TYPE),
                    (c, p) -> ValueConversions.of(ResourceArgument.getSummonableEntityType(c, p).key()), SuggestionProviders.SUMMONABLE_ENTITIES
            ),
            new VanillaUnconfigurableArgument("floatrange", RangeArgument::floatRange,
                    (c, p) -> ValueConversions.of(c.getArgument(p, MinMaxBounds.Doubles.class)), true
            ),
            // function??

            new PlayerProfileArgument(),
            new VanillaUnconfigurableArgument("intrange", RangeArgument::intRange,
                    (c, p) -> ValueConversions.of(RangeArgument.Ints.getRange(c, p)), true
            ),
            new VanillaUnconfigurableArgument("enchantment", Registries.ENCHANTMENT),

            // item_predicate  ?? //same as item but accepts tags, not sure right now
            new SlotArgument(),
            new VanillaUnconfigurableArgument("item", ItemArgument::item,
                    (c, p) -> ValueConversions.of(ItemArgument.getItem(c, p).createItemStack(1, false)),
                    param -> (ctx, builder) -> ctx.getArgument(param, ItemArgument.class).listSuggestions(ctx, builder)
            ),
            new VanillaUnconfigurableArgument("message", MessageArgument::message,
                    (c, p) -> new FormattedTextValue(MessageArgument.getMessage(c, p)), true
            ),
            new VanillaUnconfigurableArgument("effect", Registries.MOB_EFFECT),

            new TagArgument(), // for nbt_compound_tag and nbt_tag
            new VanillaUnconfigurableArgument("path", NbtPathArgument::nbtPath,
                    (c, p) -> StringValue.of(NbtPathArgument.getPath(c, p).toString()), true
            ),
            new VanillaUnconfigurableArgument("objective", ObjectiveArgument::objective,
                    (c, p) -> ValueConversions.of(ObjectiveArgument.getObjective(c, p)), false
            ),
            new VanillaUnconfigurableArgument("criterion", ObjectiveCriteriaArgument::criteria,
                    (c, p) -> StringValue.of(ObjectiveCriteriaArgument.getCriteria(c, p).getName()), false
            ),
            // operation // not sure if we need it, you have scarpet for that
            new VanillaUnconfigurableArgument("particle", ParticleArgument::particle,
                    (c, p) -> ValueConversions.of(ParticleArgument.getParticle(c, p)), (c, b) -> SharedSuggestionProvider.suggestResource(c.getSource().getServer().registryAccess().registryOrThrow(Registries.PARTICLE_TYPE).keySet(), b)
            ),

            // resource / identifier section

            new VanillaUnconfigurableArgument("recipe", ResourceLocationArgument::id,
                    (c, p) -> ValueConversions.of( ResourceLocationArgument.getRecipe(c, p).getId()), SuggestionProviders.ALL_RECIPES
            ),
            new VanillaUnconfigurableArgument("advancement", ResourceLocationArgument::id,
                    (c, p) -> ValueConversions.of( ResourceLocationArgument.getAdvancement(c, p).getId()), (ctx, builder) -> SharedSuggestionProvider.suggestResource(ctx.getSource().getServer().getAdvancements().getAllAdvancements().stream().map(Advancement::getId), builder)
            ),
            new VanillaUnconfigurableArgument("lootcondition", ResourceLocationArgument::id,
                    (c, p) -> ValueConversions.of( BuiltInRegistries.LOOT_CONDITION_TYPE.getKey(ResourceLocationArgument.getPredicate(c, p).getType())), (ctx, builder) -> SharedSuggestionProvider.suggestResource(ctx.getSource().getServer().getPredicateManager().getKeys(), builder)
            ),
            new VanillaUnconfigurableArgument("loottable", ResourceLocationArgument::id,
                    (c, p) -> ValueConversions.of( ResourceLocationArgument.getId(c, p)), (ctx, builder) -> SharedSuggestionProvider.suggestResource(ctx.getSource().getServer().getLootTables().getIds(), builder)
            ),
            new VanillaUnconfigurableArgument("attribute", Registries.ATTRIBUTE),

            new VanillaUnconfigurableArgument("boss", ResourceLocationArgument::id,
                    (c, p) -> ValueConversions.of( ResourceLocationArgument.getId(c, p)), BossBarCommands.SUGGEST_BOSS_BAR
            ),

            new VanillaUnconfigurableArgument("biome", (c) -> ResourceOrTagArgument.resourceOrTag(c, Registries.BIOME),
                    (c, p) -> {
                        ResourceOrTagArgument.Result<Biome> result = ResourceOrTagArgument.getResourceOrTag(c, "biome", Registries.BIOME);
                        Either<Holder.Reference<Biome>, HolderSet.Named<Biome>> res = result.unwrap();
                        if (res.left().isPresent())
                        {
                            return ValueConversions.of(res.left().get().key());
                        }
                        if (res.right().isPresent())
                        {
                            return ValueConversions.of(res.right().get().key());
                        }
                        return Value.NULL;
                    }, (ctx, builder) -> SharedSuggestionProvider.suggestResource(ctx.getSource().getServer().registryAccess().registryOrThrow(Registries.BIOME).keySet(), builder)
            ),
            new VanillaUnconfigurableArgument("sound", ResourceLocationArgument::id,
                    (c, p) -> ValueConversions.of( ResourceLocationArgument.getId(c, p)), SuggestionProviders.AVAILABLE_SOUNDS
            ),
            new VanillaUnconfigurableArgument("storekey", ResourceLocationArgument::id,
                    (c, p) -> ValueConversions.of( ResourceLocationArgument.getId(c, p)), (ctx, builder) -> SharedSuggestionProvider.suggestResource(ctx.getSource().getServer().getCommandStorage().keys(), builder)
            ),

            // default
            new CustomIdentifierArgument(),

            // end resource / identifier // I would be great if you guys have suggestions for that.

            new VanillaUnconfigurableArgument("rotation",
                    RotationArgument::rotation,
                    (c, p) -> {
                        Vec2 rot = RotationArgument.getRotation(c, p).getRotation(c.getSource());
                        return ListValue.of(new NumericValue(rot.x), new NumericValue(rot.y));
                    },
                    true
            ),
            new ScoreholderArgument(),
            new VanillaUnconfigurableArgument("scoreboardslot", ScoreboardSlotArgument::displaySlot,
                    (c, p) -> StringValue.of(Scoreboard.getDisplaySlotName(ScoreboardSlotArgument.getDisplaySlot(c, p))), false
            ),
            new VanillaUnconfigurableArgument("swizzle", SwizzleArgument::swizzle,
                    (c, p) -> StringValue.of(SwizzleArgument.getSwizzle(c, p).stream().map(Direction.Axis::getSerializedName).collect(Collectors.joining())), true
            ),
            new VanillaUnconfigurableArgument("team", TeamArgument::team,
                    (c, p) -> StringValue.of(TeamArgument.getTeam(c, p).getName()), false
            ),
            new VanillaUnconfigurableArgument("time", TimeArgument::time,
                    (c, p) -> new NumericValue(IntegerArgumentType.getInteger(c, p)), false
            ),
            new VanillaUnconfigurableArgument("uuid", UuidArgument::uuid,
                    (c, p) -> StringValue.of(UuidArgument.getUuid(c, p).toString()), false
            ),
            new VanillaUnconfigurableArgument("surfacelocation", () -> Vec2Argument.vec2(), // vec2
                    (c, p) -> {
                        Vec2 res = Vec2Argument.getVec2(c, p);
                        return ListValue.of(NumericValue.of(res.x), NumericValue.of(res.y));
                    },
                    false
            ),
            new LocationArgument()
    );

    public static final Map<String, CommandArgument> builtIns = baseTypes.stream().collect(Collectors.toMap(CommandArgument::getTypeSuffix, a -> a));

    public static final CommandArgument DEFAULT = baseTypes.get(0);

    public static CommandArgument getTypeForArgument(String argument, CarpetScriptHost host)
    {
        String[] components = argument.split("_");
        CommandArgument arg;
        for (int i = 0; i < components.length; i++)
        {
            String candidate = String.join("_", Arrays.asList(components).subList(i, components.length));
            arg = host.appArgTypes.get(candidate);
            if (arg != null) return arg;
            arg = builtIns.get(candidate);
            if (arg != null) return arg;
        }
        return DEFAULT;
    }

    public static RequiredArgumentBuilder<CommandSourceStack, ?> argumentNode(String param, CarpetScriptHost host) throws CommandSyntaxException
    {
        CommandArgument arg = getTypeForArgument(param, host);
        if (arg.suggestionProvider != null) return argument(param, arg.getArgumentType(host)).suggests(arg.suggestionProvider.apply(param));
        if (!arg.needsMatching) return argument(param, arg.getArgumentType(host));
        String hostName = host.getName();
        return argument(param, arg.getArgumentType(host)).suggests((ctx, b) -> {
            CarpetScriptHost cHost = CarpetServer.scriptServer.modules.get(hostName).retrieveOwnForExecution(ctx.getSource());
            return arg.suggest(ctx, b, cHost);
        });
    }

    protected String suffix;
    protected Collection<String> examples;
    protected boolean needsMatching;
    protected boolean caseSensitive = true;
    protected Function<String, SuggestionProvider<CommandSourceStack>> suggestionProvider;
    protected FunctionArgument customSuggester;


    protected CommandArgument(
            String suffix,
            Collection<String> examples,
            boolean suggestFromExamples)
    {
        this.suffix = suffix;
        this.examples = examples;
        this.needsMatching = suggestFromExamples;
    }

    protected abstract ArgumentType<?> getArgumentType(CarpetScriptHost host) throws CommandSyntaxException;


    public static Value getValue(CommandContext<CommandSourceStack> context, String param, CarpetScriptHost host) throws CommandSyntaxException
    {
        return getTypeForArgument(param, host).getValueFromContext(context, param);
    }

    protected abstract Value getValueFromContext(CommandContext<CommandSourceStack> context, String param) throws CommandSyntaxException;

    public String getTypeSuffix()
    {
        return suffix;
    }

    public static CommandArgument buildFromConfig(String suffix, Map<String, Value> config, CarpetScriptHost host) throws CommandSyntaxException
    {
        if (!config.containsKey("type"))
            throw CommandArgument.error("Custom type "+suffix+" should at least specify the type");
        String baseType = config.get("type").getString();
        if (!builtIns.containsKey(baseType))
            throw CommandArgument.error("Unknown base type "+baseType+" for custom type "+suffix);
        CommandArgument variant = builtIns.get(baseType).factory(host.scriptServer().server).get();
        variant.suffix = suffix;
        variant.configure(config, host);
        return variant;
    }

    protected void configure(Map<String, Value> config, CarpetScriptHost host) throws CommandSyntaxException
    {
        caseSensitive = config.getOrDefault("case_sensitive", Value.TRUE).getBoolean();
        if (config.containsKey("suggester"))
        {
            customSuggester = FunctionArgument.fromCommandSpec(host, config.get("suggester"));
        }
        if (config.containsKey("suggest"))
        {
            if (config.containsKey("suggester")) throw error("Attempted to provide 'suggest' list while 'suggester' is present"+" for custom type "+suffix);
            Value suggestionValue = config.get("suggest");
            if (!(suggestionValue instanceof ListValue)) throw error("Argument suggestions needs to be a list"+" for custom type "+suffix);
            examples = ((ListValue) suggestionValue).getItems().stream()
                    .map(Value::getString)
                    .collect(Collectors.toSet());
            if (!examples.isEmpty()) needsMatching = true;
        }
    }

    public CompletableFuture<Suggestions> suggest(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder suggestionsBuilder,
            CarpetScriptHost host
    ) throws CommandSyntaxException
    {
        String prefix = suggestionsBuilder.getRemaining();
        if (!caseSensitive) prefix = prefix.toLowerCase(Locale.ROOT);
        suggestFor(context, prefix, host).forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    protected List<String> suggestFor(CommandContext<CommandSourceStack> context, String prefix, CarpetScriptHost host) throws CommandSyntaxException
    {
        return getOptions(context, host).stream().filter(s -> optionMatchesPrefix(prefix, s)).collect(Collectors.toList());
    }

    protected Collection<String> getOptions(CommandContext<CommandSourceStack> context, CarpetScriptHost host) throws CommandSyntaxException
    {
        if (customSuggester != null)
        {
            CarpetProfiler.ProfilerToken currentSection = CarpetProfiler.start_section(null, "Scarpet command", CarpetProfiler.TYPE.GENERAL);
            Map<Value, Value> params = new HashMap<>();
            for(ParsedCommandNode<CommandSourceStack> pnode : context.getNodes())
            {
                CommandNode<CommandSourceStack> node = pnode.getNode();
                if (node instanceof ArgumentCommandNode)
                {
                    params.put(StringValue.of(node.getName()), CommandArgument.getValue(context, node.getName(), host));
                }
            }
            List<Value> args = new ArrayList<>(customSuggester.args.size()+1);
            args.add(MapValue.wrap(params));
            args.addAll(customSuggester.args);
            Value response = host.handleCommand(context.getSource(), customSuggester.function, args);
            if (!(response instanceof ListValue)) throw error("Custom suggester should return a list of options"+" for custom type "+suffix);
            Collection<String> res = ((ListValue) response).getItems().stream().map(Value::getString).collect(Collectors.toList());
            CarpetProfiler.end_current_section(currentSection);
            return res;
        }
        if (needsMatching) return examples;
        //return Lists.newArrayList("");
        // better than nothing I guess
        // nothing is such a bad default.
        return Collections.singletonList("... "+getTypeSuffix());
    }

    protected boolean optionMatchesPrefix(String prefix, String option)
    {
        if (!caseSensitive)
        {
            //prefix = prefix.toLowerCase(Locale.ROOT);
            option = option.toLowerCase(Locale.ROOT);
        }
        for(int i = 0; !option.startsWith(prefix, i); ++i)
        {
            i = option.indexOf('_', i);
            if (i < 0) return false;
        }
        return true;
    }

    protected abstract Supplier<CommandArgument> factory(MinecraftServer server);

    private static class StringArgument extends CommandArgument
    {
        Set<String> validOptions = Collections.emptySet();
        private StringArgument()
        {
            super("string", StringArgumentType.StringType.QUOTABLE_PHRASE.getExamples(), true);
        }

        @Override
        public ArgumentType<?> getArgumentType(CarpetScriptHost host)
        {
            return StringArgumentType.string();
        }

        @Override
        public Value getValueFromContext(CommandContext<CommandSourceStack> context, String param) throws CommandSyntaxException
        {
            String choseValue = StringArgumentType.getString(context, param);
            if (!caseSensitive) choseValue = choseValue.toLowerCase(Locale.ROOT);
            if (!validOptions.isEmpty() && !validOptions.contains(choseValue))
            {
                throw new SimpleCommandExceptionType(Component.literal("Incorrect value for "+param+": "+choseValue+" for custom type "+suffix)).create();
            }
            return StringValue.of(choseValue);
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host) throws CommandSyntaxException
        {
            super.configure(config, host);
            if (config.containsKey("options"))
            {
                Value optionsValue = config.get("options");
                if (!(optionsValue instanceof ListValue)) throw error("Custom string type requires options passed as a list"+" for custom type "+suffix);
                validOptions = ((ListValue) optionsValue).getItems().stream()
                        .map(v -> caseSensitive?v.getString():(v.getString().toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toSet());
            }
        }

        @Override
        protected Collection<String> getOptions(CommandContext<CommandSourceStack> context, CarpetScriptHost host) throws CommandSyntaxException
        {
            return validOptions.isEmpty()?super.getOptions(context, host):validOptions;
        }

        @Override
        protected Supplier<CommandArgument> factory(MinecraftServer server) { return StringArgument::new; }
    }

    private static class WordArgument extends StringArgument
    {
        private WordArgument() { super(); suffix = "term"; examples = StringArgumentType.StringType.SINGLE_WORD.getExamples(); }
        @Override
        public ArgumentType<?> getArgumentType(CarpetScriptHost host) { return StringArgumentType.word(); }
        @Override
        protected Supplier<CommandArgument> factory(MinecraftServer server) { return WordArgument::new; }
    }

    private static class GreedyStringArgument extends StringArgument
    {
        private GreedyStringArgument() { super();suffix = "text"; examples = StringArgumentType.StringType.GREEDY_PHRASE.getExamples(); }
        @Override
        public ArgumentType<?> getArgumentType(CarpetScriptHost host) { return StringArgumentType.greedyString(); }
        @Override
        protected Supplier<CommandArgument> factory(MinecraftServer server) { return GreedyStringArgument::new; }
    }

    private static class BlockPosArgument extends CommandArgument
    {
        private boolean mustBeLoaded = false;

        private BlockPosArgument()
        {
            super("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos().getExamples(), false);
        }

        @Override
        public ArgumentType<?> getArgumentType(CarpetScriptHost host)
        {
            return net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos();
        }

        @Override
        public Value getValueFromContext(CommandContext<CommandSourceStack> context, String param) throws CommandSyntaxException
        {
            BlockPos pos = mustBeLoaded
                    ? net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(context, param)
                    : net.minecraft.commands.arguments.coordinates.BlockPosArgument.getSpawnablePos(context, param);
            return ValueConversions.of(pos);
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host) throws CommandSyntaxException
        {
            super.configure(config, host);
            mustBeLoaded = config.getOrDefault("loaded", Value.FALSE).getBoolean();
        }

        @Override
        protected Supplier<CommandArgument> factory(MinecraftServer server)
        {
            return BlockPosArgument::new;
        }
    }

    private static class LocationArgument extends CommandArgument
    {
        boolean blockCentered;

        private LocationArgument()
        {
            super("location", Vec3Argument.vec3().getExamples(), false);
            blockCentered = true;
        }
        @Override
        protected ArgumentType<?> getArgumentType(CarpetScriptHost host)
        {
            return Vec3Argument.vec3(blockCentered);
        }

        @Override
        protected Value getValueFromContext(CommandContext<CommandSourceStack> context, String param) throws CommandSyntaxException
        {
            return ValueConversions.of(Vec3Argument.getVec3(context, param));
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host) throws CommandSyntaxException
        {
            super.configure(config, host);
            blockCentered = config.getOrDefault("block_centered", Value.TRUE).getBoolean();
        }

        @Override
        protected Supplier<CommandArgument> factory(MinecraftServer server)
        {
            return LocationArgument::new;
        }
    }

    private static class EntityArgument extends CommandArgument
    {
        boolean onlyFans;
        boolean single;

        private EntityArgument()
        {
            super("entities", net.minecraft.commands.arguments.EntityArgument.entities().getExamples(), false);
            onlyFans = false;
            single = false;
        }
        @Override
        protected ArgumentType<?> getArgumentType(CarpetScriptHost host)
        {
            if (onlyFans)
            {
                return single?net.minecraft.commands.arguments.EntityArgument.player():net.minecraft.commands.arguments.EntityArgument.players();
            }
            else
            {
                return single?net.minecraft.commands.arguments.EntityArgument.entity():net.minecraft.commands.arguments.EntityArgument.entities();
            }
        }

        @Override
        protected Value getValueFromContext(CommandContext<CommandSourceStack> context, String param) throws CommandSyntaxException
        {
            Collection<? extends Entity> founds = net.minecraft.commands.arguments.EntityArgument.getOptionalEntities(context, param);
            if (!single) return ListValue.wrap(founds.stream().map(EntityValue::new).collect(Collectors.toList()));
            if (founds.size() == 0) return Value.NULL;
            if (founds.size() == 1) return new EntityValue(founds.iterator().next());
            throw new SimpleCommandExceptionType(Component.literal("Multiple entities returned while only one was requested"+" for custom type "+suffix)).create();
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host) throws CommandSyntaxException
        {
            super.configure(config, host);
            onlyFans = config.getOrDefault("players", Value.FALSE).getBoolean();
            single = config.getOrDefault("single", Value.FALSE).getBoolean();
        }

        @Override
        protected Supplier<CommandArgument> factory(MinecraftServer server)
        {
            return EntityArgument::new;
        }
    }

    private static class PlayerProfileArgument extends CommandArgument
    {
        boolean single;

        private PlayerProfileArgument()
        {
            super("players", GameProfileArgument.gameProfile().getExamples(), false);
            single = false;
        }
        @Override
        protected ArgumentType<?> getArgumentType(CarpetScriptHost host)
        {
            return GameProfileArgument.gameProfile();
        }

        @Override
        protected Value getValueFromContext(CommandContext<CommandSourceStack> context, String param) throws CommandSyntaxException
        {
            Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, param);
            if (!single) return ListValue.wrap(profiles.stream().map(p -> StringValue.of(p.getName())).collect(Collectors.toList()));
            int size = profiles.size();
            if (size == 0) return Value.NULL;
            if (size == 1) return StringValue.of(profiles.iterator().next().getName());
            throw new SimpleCommandExceptionType(Component.literal("Multiple game profiles returned while only one was requested"+" for custom type "+suffix)).create();
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host) throws CommandSyntaxException
        {
            super.configure(config, host);
            single = config.getOrDefault("single", Value.FALSE).getBoolean();
        }

        @Override
        protected Supplier<CommandArgument> factory(MinecraftServer server)
        {
            return PlayerProfileArgument::new;
        }
    }

    private static class ScoreholderArgument extends CommandArgument
    {
        boolean single;

        private ScoreholderArgument()
        {
            super("scoreholder", ScoreHolderArgument.scoreHolder().getExamples(), false);
            single = false;
            suggestionProvider = param -> ScoreHolderArgument.SUGGEST_SCORE_HOLDERS;
        }
        @Override
        protected ArgumentType<?> getArgumentType(CarpetScriptHost host)
        {
            return single?ScoreHolderArgument.scoreHolder():ScoreHolderArgument.scoreHolders();
        }

        @Override
        protected Value getValueFromContext(CommandContext<CommandSourceStack> context, String param) throws CommandSyntaxException
        {
            Collection<String> holders = ScoreHolderArgument.getNames(context, param);
            if (!single) return ListValue.wrap(holders.stream().map(StringValue::of).collect(Collectors.toList()));
            int size = holders.size();
            if (size == 0) return Value.NULL;
            if (size == 1) return StringValue.of(holders.iterator().next());
            throw new SimpleCommandExceptionType(Component.literal("Multiple score holders returned while only one was requested"+" for custom type "+suffix)).create();
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host) throws CommandSyntaxException
        {
            super.configure(config, host);
            single = config.getOrDefault("single", Value.FALSE).getBoolean();
        }

        @Override
        protected Supplier<CommandArgument> factory(MinecraftServer server)
        {
            return PlayerProfileArgument::new;
        }
    }

    private static class TagArgument extends CommandArgument
    {
        boolean mapRequired;
        private TagArgument()
        {
            super("tag", CompoundTagArgument.compoundTag().getExamples(), false);
            mapRequired = true;
        }
        @Override
        protected ArgumentType<?> getArgumentType(CarpetScriptHost host)
        {
            return mapRequired?CompoundTagArgument.compoundTag(): NbtTagArgument.nbtTag();
        }

        @Override
        protected Value getValueFromContext(CommandContext<CommandSourceStack> context, String param) throws CommandSyntaxException
        {
            if (mapRequired)
                return new NBTSerializableValue(CompoundTagArgument.getCompoundTag(context, param));
            else
                return new NBTSerializableValue(NbtTagArgument.getNbtTag(context, param));
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host) throws CommandSyntaxException
        {
            super.configure(config, host);
            mapRequired = !config.getOrDefault("allow_element", Value.FALSE).getBoolean();
        }

        @Override
        protected Supplier<CommandArgument> factory(MinecraftServer server)
        {
            return TagArgument::new;
        }
    }

    private static class CustomIdentifierArgument extends CommandArgument
    {
        Set<ResourceLocation> validOptions = Collections.emptySet();

        protected CustomIdentifierArgument()
        {
            super("identifier", Collections.emptyList(), true);
        }

        @Override
        protected ArgumentType<?> getArgumentType(CarpetScriptHost host)
        {
            return ResourceLocationArgument.id();
        }

        @Override
        protected Value getValueFromContext(CommandContext<CommandSourceStack> context, String param) throws CommandSyntaxException
        {
            ResourceLocation choseValue = ResourceLocationArgument.getId(context, param);
            if (!validOptions.isEmpty() && !validOptions.contains(choseValue))
            {
                throw new SimpleCommandExceptionType(Component.literal("Incorrect value for "+param+": "+choseValue+" for custom type "+suffix)).create();
            }
            return ValueConversions.of(choseValue);
        }

        @Override
        protected Supplier<CommandArgument> factory(MinecraftServer server)
        {
            return CustomIdentifierArgument::new;
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host) throws CommandSyntaxException
        {
            super.configure(config, host);
            if (config.containsKey("options"))
            {
                Value optionsValue = config.get("options");
                if (!(optionsValue instanceof ListValue)) throw error("Custom sting type requires options passed as a list"+" for custom type "+suffix);
                validOptions = ((ListValue) optionsValue).getItems().stream().map(v -> new ResourceLocation(v.getString())).collect(Collectors.toSet());
            }
        }
    }

    private static class FloatArgument extends CommandArgument
    {
        private Double min = null;
        private Double max = null;
        private FloatArgument()
        {
            super("float", DoubleArgumentType.doubleArg().getExamples(), true);
        }

        @Override
        public ArgumentType<?> getArgumentType(CarpetScriptHost host)
        {
            if (min != null)
            {
                if (max != null)
                {
                    return DoubleArgumentType.doubleArg(min, max);
                }
                return DoubleArgumentType.doubleArg(min);
            }
            return DoubleArgumentType.doubleArg();
        }

        @Override
        public Value getValueFromContext(CommandContext<CommandSourceStack> context, String param) throws CommandSyntaxException
        {
            return new NumericValue(DoubleArgumentType.getDouble(context, param));
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host) throws CommandSyntaxException
        {
            super.configure(config, host);
            if (config.containsKey("min"))
            {
                min = NumericValue.asNumber(config.get("min"), "min").getDouble();
            }
            if (config.containsKey("max"))
            {
                max = NumericValue.asNumber(config.get("max"), "max").getDouble();
            }
            if (max != null && min == null) throw error("Double types cannot be only upper-bounded"+" for custom type "+suffix);
        }

        @Override
        protected Supplier<CommandArgument> factory(MinecraftServer server)
        {
            return FloatArgument::new;
        }
    }

    private static class IntArgument extends CommandArgument
    {
        private Long min = null;
        private Long max = null;
        private IntArgument()
        {
            super("int", LongArgumentType.longArg().getExamples(), true);
        }

        @Override
        public ArgumentType<?> getArgumentType(CarpetScriptHost host)
        {
            if (min != null)
            {
                if (max != null)
                {
                    return LongArgumentType.longArg(min, max);
                }
                return LongArgumentType.longArg(min);
            }
            return LongArgumentType.longArg();
        }

        @Override
        public Value getValueFromContext(CommandContext<CommandSourceStack> context, String param) throws CommandSyntaxException
        {
            return new NumericValue(LongArgumentType.getLong(context, param));
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host) throws CommandSyntaxException
        {
            super.configure(config, host);
            if (config.containsKey("min"))
            {
                min = NumericValue.asNumber(config.get("min"), "min").getLong();
            }
            if (config.containsKey("max"))
            {
                max = NumericValue.asNumber(config.get("max"), "max").getLong();
            }
            if (max != null && min == null) throw error("Double types cannot be only upper-bounded"+" for custom type "+suffix);
        }

        @Override
        protected Supplier<CommandArgument> factory(MinecraftServer server)
        {
            return IntArgument::new;
        }
    }
    private static class SlotArgument extends CommandArgument
    {
        private static record ContainerIds(IntSet numericalIds, Set<String> commandIds) {}
        private String restrict;
        private static final Map<String, ContainerIds> RESTRICTED_CONTAINERS = new HashMap<String, ContainerIds>(){{
            int i;
            for (String source : Arrays.asList("player", "enderchest", "equipment", "armor", "weapon", "container", "villager", "horse"))
                put(source, new ContainerIds(new IntOpenHashSet(), new HashSet<>()));
            for (i = 0; i < 41; i++) get("player").numericalIds().add(i);
            for(i = 0; i < 41; i++) get("player").commandIds().add("container." + i);
            for(i = 0; i < 9; i++) get("player").commandIds().add("hotbar." + i);
            for(i = 0; i < 27; i++) get("player").commandIds().add("inventory." + i);
            for (String place : Arrays.asList("weapon", "weapon.mainhand", "weapon.offhand"))
            {
                get("player").commandIds().add(place);
                get("equipment").commandIds().add(place);
                get("weapon").commandIds().add(place);
            }
            for (String place : Arrays.asList("armor.feet","armor.legs", "armor.chest","armor.head"))
            {
                get("player").commandIds().add(place);
                get("equipment").commandIds().add(place);
                get("armor").commandIds().add(place);
            }

            for (i = 0; i < 27; i++) get("enderchest").numericalIds().add(200+i);
            for(i = 0; i < 27; i++) get("enderchest").commandIds().add("enderchest." + i);

            for (i = 0; i < 6; i++) get("equipment").numericalIds().add(98+i);

            for (i = 0; i < 4; i++) get("armor").numericalIds().add(100+i);

            for (i = 0; i < 2; i++) get("weapon").numericalIds().add(98+i);

            for (i = 0; i < 54; i++) get("container").numericalIds().add(i);
            for(i = 0; i < 41; i++) get("container").commandIds().add("container." + i);

            for (i = 0; i < 8; i++) get("villager").numericalIds().add(i);
            for(i = 0; i < 8; i++) get("villager").commandIds().add("villager." + i);

            for (i = 0; i < 15; i++) get("horse").numericalIds().add(500+i);
            for(i = 0; i < 15; i++) get("horse").commandIds().add("horse." + i);
            get("horse").numericalIds().add(400);
            get("horse").commandIds().add("horse.saddle");
            get("horse").numericalIds().add(401);
            get("horse").commandIds().add("horse.armor");
        }};

        protected SlotArgument()
        {
            super("slot", net.minecraft.commands.arguments.SlotArgument.slot().getExamples(), false);
        }

        @Override
        protected ArgumentType<?> getArgumentType(CarpetScriptHost host) throws CommandSyntaxException
        {
            return net.minecraft.commands.arguments.SlotArgument.slot();
        }

        @Override
        protected Value getValueFromContext(CommandContext<CommandSourceStack> context, String param) throws CommandSyntaxException
        {
            int slot = net.minecraft.commands.arguments.SlotArgument.getSlot(context, param);
            if (restrict != null && !RESTRICTED_CONTAINERS.get(restrict).numericalIds().contains(slot))
            {
                throw new SimpleCommandExceptionType(Component.literal("Incorrect slot restricted to "+restrict+" for custom type "+suffix)).create();
            }
            return ValueConversions.ofVanillaSlotResult(slot);
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host) throws CommandSyntaxException
        {
            super.configure(config, host);
            if (config.containsKey("restrict"))
            {
                restrict = config.get("restrict").getString().toLowerCase(Locale.ROOT);
                needsMatching = true;
                if (!RESTRICTED_CONTAINERS.containsKey(restrict))
                    throw error("Incorrect slot restriction "+restrict+" for custom type "+suffix);
            }
        }

        @Override
        protected Collection<String> getOptions(CommandContext<CommandSourceStack> context, CarpetScriptHost host) throws CommandSyntaxException
        {
            return restrict==null?super.getOptions(context, host):RESTRICTED_CONTAINERS.get(restrict).commandIds();
        }

        @Override
        protected Supplier<CommandArgument> factory(MinecraftServer server)
        {
            return SlotArgument::new;
        }
    }

    @FunctionalInterface
    private interface ValueExtractor
    {
        Value apply(CommandContext<CommandSourceStack> ctx, String param) throws CommandSyntaxException;
    }

    @FunctionalInterface
    private interface ArgumentProvider
    {
        ArgumentType<?> get() throws CommandSyntaxException;
    }

    @FunctionalInterface
    private interface ArgumentProviderEx
    {
        ArgumentType<?> get(CommandBuildContext regAccess) throws CommandSyntaxException;
    }

    public static class VanillaUnconfigurableArgument extends  CommandArgument
    {
        private final ArgumentProvider argumentTypeSupplier;
        private final ArgumentProviderEx argumentTypeSupplierEx;
        private final ValueExtractor valueExtractor;
        private final boolean providesExamples;

        public VanillaUnconfigurableArgument(
                String suffix,
                ArgumentProvider argumentTypeSupplier,
                ValueExtractor  valueExtractor,
                boolean suggestFromExamples
                )
        {
            super(suffix, null, suggestFromExamples);
            try
            {
                this.examples = argumentTypeSupplier.get().getExamples();
            }
            catch (CommandSyntaxException e)
            {
                this.examples = Collections.emptyList();
            }
            this.providesExamples = suggestFromExamples;
            this.argumentTypeSupplier = argumentTypeSupplier;
            this.valueExtractor = valueExtractor;
            this.argumentTypeSupplierEx = null;
        }
        public VanillaUnconfigurableArgument(
                String suffix,
                ArgumentProvider argumentTypeSupplier,
                ValueExtractor  valueExtractor,
                SuggestionProvider<CommandSourceStack> suggester
        )
        {
            super(suffix, Collections.emptyList(), false);
            this.suggestionProvider = param -> suggester;
            this.providesExamples = false;
            this.argumentTypeSupplier = argumentTypeSupplier;
            this.valueExtractor = valueExtractor;
            this.argumentTypeSupplierEx = null;
        }
        public VanillaUnconfigurableArgument(
                String suffix,
                ArgumentProviderEx argumentTypeSupplier,
                ValueExtractor valueExtractor,
                boolean suggestFromExamples,
                MinecraftServer server)
        {
            super(suffix, null, suggestFromExamples);
            try
            {
                final CommandBuildContext context = CommandBuildContext.simple(server.registryAccess(), server.getWorldData().enabledFeatures());
                this.examples = argumentTypeSupplier.get(context).getExamples();
            }
            catch (CommandSyntaxException e)
            {
                this.examples = Collections.emptyList();
            }
            this.providesExamples = suggestFromExamples;
            this.argumentTypeSupplierEx = argumentTypeSupplier;
            this.valueExtractor = valueExtractor;
            this.argumentTypeSupplier = null;
        }

        public VanillaUnconfigurableArgument(
                String suffix,
                ArgumentProviderEx argumentTypeSupplier,
                ValueExtractor valueExtractor,
                SuggestionProvider<CommandSourceStack> suggester
        )
        {
            super(suffix, Collections.emptyList(), false);
            this.suggestionProvider = param -> suggester;
            this.providesExamples = false;
            this.argumentTypeSupplierEx = argumentTypeSupplier;
            this.valueExtractor = valueExtractor;
            this.argumentTypeSupplier = null;
        }

        public VanillaUnconfigurableArgument(
                String suffix,
                ArgumentProviderEx argumentTypeSupplier,
                ValueExtractor valueExtractor,
                Function<String, SuggestionProvider<CommandSourceStack>> suggesterGen
        )
        {
            super(suffix, Collections.emptyList(), false);
            this.suggestionProvider = suggesterGen;
            this.providesExamples = false;
            this.argumentTypeSupplierEx = argumentTypeSupplier;
            this.valueExtractor = valueExtractor;
            this.argumentTypeSupplier = null;
        }

        public <T> VanillaUnconfigurableArgument(
                String suffix,
                ResourceKey<Registry<T>> registry
        )
        {
            this(
                    suffix,
                    (c) -> ResourceArgument.resource(c, registry),
                    (c, p) -> ValueConversions.of(ResourceArgument.getResource(c, p, registry).key()),
                    (c, b) -> SharedSuggestionProvider.suggestResource(c.getSource().getServer().registryAccess().registryOrThrow(registry).keySet(), b)
            );
        }

        @Override
        protected ArgumentType<?> getArgumentType(CarpetScriptHost host) throws CommandSyntaxException
        {
            if (argumentTypeSupplier != null) return argumentTypeSupplier.get();
            CommandBuildContext registryAccess = CommandBuildContext.simple(host.scriptServer().server.registryAccess(), host.scriptServer().server.getWorldData().enabledFeatures());
            return argumentTypeSupplierEx.get(registryAccess);
        }

        @Override
        protected Value getValueFromContext(CommandContext<CommandSourceStack> context, String param) throws CommandSyntaxException
        {
            return valueExtractor.apply(context, param);
        }

        @Override
        protected Supplier<CommandArgument> factory(MinecraftServer server)
        {
            if (argumentTypeSupplier != null) return () -> new VanillaUnconfigurableArgument(getTypeSuffix(), argumentTypeSupplier, valueExtractor, providesExamples);
            return () -> new VanillaUnconfigurableArgument(getTypeSuffix(), argumentTypeSupplierEx, valueExtractor, providesExamples, server);
        }
    }
}
