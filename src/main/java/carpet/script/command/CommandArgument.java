package carpet.script.command;

import carpet.fakes.BlockStateArgumentInterface;
import carpet.script.CarpetScriptHost;
import carpet.script.argument.FunctionArgument;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BlockValue;
import carpet.script.value.EntityValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
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
import net.minecraft.advancement.Advancement;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.AngleArgumentType;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.BlockPredicateArgumentType;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.command.argument.ColorArgumentType;
import net.minecraft.command.argument.ColumnPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.EntitySummonArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.ItemEnchantmentArgumentType;
import net.minecraft.command.argument.ItemSlotArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.command.argument.MobEffectArgumentType;
import net.minecraft.command.argument.NbtCompoundTagArgumentType;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.command.argument.NbtTagArgumentType;
import net.minecraft.command.argument.NumberRangeArgumentType;
import net.minecraft.command.argument.ObjectiveArgumentType;
import net.minecraft.command.argument.ObjectiveCriteriaArgumentType;
import net.minecraft.command.argument.ParticleArgumentType;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.command.argument.ScoreHolderArgumentType;
import net.minecraft.command.argument.ScoreboardSlotArgumentType;
import net.minecraft.command.argument.SwizzleArgumentType;
import net.minecraft.command.argument.TeamArgumentType;
import net.minecraft.command.argument.TimeArgumentType;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.command.argument.Vec2ArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.entity.Entity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.command.BossBarCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.tuple.Pair;

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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static net.minecraft.server.command.CommandManager.argument;


public abstract class CommandArgument
{
    public static CommandSyntaxException error(String text)
    {
        return new SimpleCommandExceptionType(new LiteralText(text)).create();
    }

    private static final List<? extends CommandArgument> baseTypes = Lists.newArrayList(
            // default
            new StringArgument(),
            // vanilla arguments as per https://minecraft.gamepedia.com/Argument_types
            new VanillaUnconfigurableArgument( "bool", BoolArgumentType::bool,
                    (c, p) -> new NumericValue(BoolArgumentType.getBool(c, p)), false
            ),
            new FloatArgument(),
            new IntArgument(),
            new WordArgument(), new GreedyStringArgument(),
            new VanillaUnconfigurableArgument( "yaw", AngleArgumentType::angle,  // angle
                    (c, p) -> new NumericValue(AngleArgumentType.getAngle(c, p)), true
            ),
            new BlockPosArgument(),
            new VanillaUnconfigurableArgument( "block", BlockStateArgumentType::blockState,
                    (c, p) -> {
                        BlockStateArgument result = BlockStateArgumentType.getBlockState(c, p);
                        return new BlockValue(result.getBlockState(), null, null, ((BlockStateArgumentInterface)result).getCMTag() );
                    },
                    false
            ),
            new VanillaUnconfigurableArgument( "blockpredicate", BlockPredicateArgumentType::blockPredicate,
                    (c, p) -> ValueConversions.ofBlockPredicate(c.getSource().getMinecraftServer().getTagManager(), BlockPredicateArgumentType.getBlockPredicate(c, p)), false
            ),
            new VanillaUnconfigurableArgument("teamcolor", ColorArgumentType::color,
                    (c, p) -> {
                        Formatting format = ColorArgumentType.getColor(c, p);
                        return ListValue.of(StringValue.of(format.getName()), ValueConversions.ofRGB(format.getColorValue()));
                    },
                    false
            ),
            new VanillaUnconfigurableArgument("columnpos", ColumnPosArgumentType::columnPos,
                    (c, p) -> ValueConversions.of(ColumnPosArgumentType.getColumnPos(c, p)), false
            ),
            // component  // raw json
            new VanillaUnconfigurableArgument("dimension", DimensionArgumentType::dimension,
                    (c, p) -> ValueConversions.of(DimensionArgumentType.getDimensionArgument(c, p)), false
            ),
            new EntityArgument(),
            new VanillaUnconfigurableArgument("anchor", EntityAnchorArgumentType::entityAnchor,
                    (c, p) -> StringValue.of(EntityAnchorArgumentType.getEntityAnchor(c, p).name()), false
            ),
            new VanillaUnconfigurableArgument("entitytype", EntitySummonArgumentType::entitySummon,
                    (c, p) -> ValueConversions.of(EntitySummonArgumentType.getEntitySummon(c, p)), SuggestionProviders.SUMMONABLE_ENTITIES
            ),
            new VanillaUnconfigurableArgument("floatrange", NumberRangeArgumentType::method_30918,
                    (c, p) -> ValueConversions.of(c.getArgument(p, NumberRange.FloatRange.class)), true
            ),
            // function??

            new PlayerProfileArgument(),
            new VanillaUnconfigurableArgument("intrange", NumberRangeArgumentType::numberRange,
                    (c, p) -> ValueConversions.of(NumberRangeArgumentType.IntRangeArgumentType.getRangeArgument(c, p)), true
            ),
            new VanillaUnconfigurableArgument("enchantment", ItemEnchantmentArgumentType::itemEnchantment,
                    (c, p) -> ValueConversions.of(Registry.ENCHANTMENT.getId(ItemEnchantmentArgumentType.getEnchantment(c, p))), false
            ),
            // item_predicate  ?? //same as item but accepts tags, not sure right now
            new SlotArgument(),
            new VanillaUnconfigurableArgument("item", ItemStackArgumentType::itemStack,
                    (c, p) -> ValueConversions.of(ItemStackArgumentType.getItemStackArgument(c, p).createStack(1, false)), false
            ),
            new VanillaUnconfigurableArgument("message", MessageArgumentType::message,
                    (c, p) -> new FormattedTextValue(MessageArgumentType.getMessage(c, p)), true
            ),
            new VanillaUnconfigurableArgument("effect", MobEffectArgumentType::mobEffect,
                    (c, p) -> ValueConversions.of(Registry.STATUS_EFFECT.getId(MobEffectArgumentType.getMobEffect(c, p))),false
            ),
            new TagArgument(), // for nbt_compound_tag and nbt_tag
            new VanillaUnconfigurableArgument("path", NbtPathArgumentType::nbtPath,
                    (c, p) -> StringValue.of(NbtPathArgumentType.getNbtPath(c, p).toString()), true
            ),
            new VanillaUnconfigurableArgument("objective", ObjectiveArgumentType::objective,
                    (c, p) -> ValueConversions.of(ObjectiveArgumentType.getObjective(c, p)), false
            ),
            new VanillaUnconfigurableArgument("criterion", ObjectiveCriteriaArgumentType::objectiveCriteria,
                    (c, p) -> StringValue.of(ObjectiveCriteriaArgumentType.getCriteria(c, p).getName()), false
            ),
            // operation // not sure if we need it, you have scarpet for that
            new VanillaUnconfigurableArgument("particle", ParticleArgumentType::particle,
                    (c, p) -> ValueConversions.of(ParticleArgumentType.getParticle(c, p)), false
            ),

            // resource / identifier section

            new VanillaUnconfigurableArgument("recipe", IdentifierArgumentType::identifier,
                    (c, p) -> ValueConversions.of( IdentifierArgumentType.getRecipeArgument(c, p).getId()), SuggestionProviders.ALL_RECIPES
            ),
            new VanillaUnconfigurableArgument("advancement", IdentifierArgumentType::identifier,
                    (c, p) -> ValueConversions.of( IdentifierArgumentType.getAdvancementArgument(c, p).getId()), (ctx, builder) -> CommandSource.suggestIdentifiers(ctx.getSource().getMinecraftServer().getAdvancementLoader().getAdvancements().stream().map(Advancement::getId), builder)
            ),
            new VanillaUnconfigurableArgument("lootcondition", IdentifierArgumentType::identifier,
                    (c, p) -> ValueConversions.of( Registry.LOOT_CONDITION_TYPE.getId(IdentifierArgumentType.method_23727(c, p).getType())), (ctx, builder) -> CommandSource.suggestIdentifiers(ctx.getSource().getMinecraftServer().getPredicateManager().getIds(), builder)
            ),
            new VanillaUnconfigurableArgument("loottable", IdentifierArgumentType::identifier,
                    (c, p) -> ValueConversions.of( IdentifierArgumentType.getIdentifier(c, p)), (ctx, builder) -> CommandSource.suggestIdentifiers(ctx.getSource().getMinecraftServer().getLootManager().getTableIds(), builder)
            ),
            new VanillaUnconfigurableArgument("attribute", IdentifierArgumentType::identifier,
                    (c, p) -> ValueConversions.of( Registry.ATTRIBUTE.getId(IdentifierArgumentType.method_27575(c, p))), (ctx, builder) -> CommandSource.suggestIdentifiers(Registry.ATTRIBUTE.getIds(), builder)
            ),
            new VanillaUnconfigurableArgument("boss", IdentifierArgumentType::identifier,
                    (c, p) -> ValueConversions.of( IdentifierArgumentType.getIdentifier(c, p)), BossBarCommand.SUGGESTION_PROVIDER
            ),
            new VanillaUnconfigurableArgument("biome", IdentifierArgumentType::identifier,
                    (c, p) -> ValueConversions.of( IdentifierArgumentType.getIdentifier(c, p)), SuggestionProviders.ALL_BIOMES
            ),
            new VanillaUnconfigurableArgument("sound", IdentifierArgumentType::identifier,
                    (c, p) -> ValueConversions.of( IdentifierArgumentType.getIdentifier(c, p)), SuggestionProviders.AVAILABLE_SOUNDS
            ),
            new VanillaUnconfigurableArgument("storekey", IdentifierArgumentType::identifier,
                    (c, p) -> ValueConversions.of( IdentifierArgumentType.getIdentifier(c, p)), (ctx, builder) -> CommandSource.suggestIdentifiers(ctx.getSource().getMinecraftServer().getDataCommandStorage().getIds(), builder)
            ),

            // default
            new CustomIdentifierArgument(),

            // end resource / identifier // I would be great if you guys have suggestions for that.

            new VanillaUnconfigurableArgument("rotation",
                    RotationArgumentType::rotation,
                    (c, p) -> {
                        Vec2f rot = RotationArgumentType.getRotation(c, p).toAbsoluteRotation(c.getSource());
                        return ListValue.of(new NumericValue(rot.x), new NumericValue(rot.y));
                    },
                    true
            ),
            new ScoreholderArgument(),
            new VanillaUnconfigurableArgument("scoreboardslot", ScoreboardSlotArgumentType::scoreboardSlot,
                    (c, p) -> StringValue.of(Scoreboard.getDisplaySlotName(ScoreboardSlotArgumentType.getScoreboardSlot(c, p))), false
            ),
            new VanillaUnconfigurableArgument("swizzle", SwizzleArgumentType::swizzle,
                    (c, p) -> StringValue.of(SwizzleArgumentType.getSwizzle(c, p).stream().map(Direction.Axis::asString).collect(Collectors.joining())), true
            ),
            new VanillaUnconfigurableArgument("team", TeamArgumentType::team,
                    (c, p) -> StringValue.of(TeamArgumentType.getTeam(c, p).getName()), false
            ),
            new VanillaUnconfigurableArgument("time", TimeArgumentType::time,
                    (c, p) -> new NumericValue(IntegerArgumentType.getInteger(c, p)), false
            ),
            new VanillaUnconfigurableArgument("uuid", UuidArgumentType::uuid,
                    (c, p) -> StringValue.of(UuidArgumentType.getUuid(c, p).toString()), false
            ),
            new VanillaUnconfigurableArgument("surfacelocation", Vec2ArgumentType::vec2, // vec2
                    (c, p) -> {
                        Vec2f res = Vec2ArgumentType.getVec2(c, p);
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
        String suffix = components[components.length-1];
        CommandArgument arg =  host.appArgTypes.get(suffix);
        if (arg != null) return arg;
        return builtIns.getOrDefault(suffix, DEFAULT);
    }

    public static RequiredArgumentBuilder<ServerCommandSource, ?> argumentNode(String param, CarpetScriptHost host) throws CommandSyntaxException
    {
        CommandArgument arg = getTypeForArgument(param, host);
        if (arg.suggestionProvider != null) return argument(param, arg.getArgumentType()).suggests(arg.suggestionProvider);
        return arg.needsMatching? argument(param, arg.getArgumentType()).suggests((c, b) -> arg.suggest(c, b, host)) : argument(param, arg.getArgumentType());
    }

    protected String suffix;
    protected Collection<String> examples;
    protected boolean needsMatching;
    protected boolean caseSensitive = true;
    protected SuggestionProvider<ServerCommandSource> suggestionProvider;
    protected FunctionArgument<Value> customSuggester;


    protected CommandArgument(
            String suffix,
            Collection<String> examples,
            boolean suggestFromExamples)
    {
        this.suffix = suffix;
        this.examples = examples;
        this.needsMatching = suggestFromExamples;
    }

    protected abstract ArgumentType<?> getArgumentType() throws CommandSyntaxException;


    public static Value getValue(CommandContext<ServerCommandSource> context, String param, CarpetScriptHost host) throws CommandSyntaxException
    {
        return getTypeForArgument(param, host).getValueFromContext(context, param);
    }

    protected abstract Value getValueFromContext(CommandContext<ServerCommandSource> context, String param) throws CommandSyntaxException;

    public String getTypeSuffix()
    {
        return suffix;
    }

    public static CommandArgument buildFromConfig(String suffix, Map<String, Value> config, CarpetScriptHost host)
    {
        if (!config.containsKey("type"))
            throw new InternalExpressionException("Custom type "+suffix+" should at least specify the type");
        String baseType = config.get("type").getString();
        if (!builtIns.containsKey(baseType))
            throw new InternalExpressionException("Unknown base type "+baseType+" for custom type "+suffix);
        CommandArgument variant = builtIns.get(baseType).factory().get();
        variant.suffix = suffix;
        variant.configure(config, host);
        return variant;
    }

    protected void configure(Map<String, Value> config, CarpetScriptHost host)
    {
        caseSensitive = config.getOrDefault("case_sensitive", Value.TRUE).getBoolean();
        if (config.containsKey("suggester"))
        {
            customSuggester = FunctionArgument.fromCommandSpec(host, config.get("suggester"));
        }
        if (config.containsKey("suggest"))
        {
            if (config.containsKey("suggester")) throw new InternalExpressionException("Attempted to provide 'suggest' list while 'suggester' is present"+" for custom type "+suffix);
            Value suggestionValue = config.get("suggest");
            if (!(suggestionValue instanceof ListValue)) throw new InternalExpressionException("Argument suggestions needs to be a list"+" for custom type "+suffix);
            examples = ((ListValue) suggestionValue).getItems().stream()
                    .map(Value::getString)
                    .collect(Collectors.toSet());
            if (!examples.isEmpty()) needsMatching = true;
        }
    }

    public CompletableFuture<Suggestions> suggest(
            CommandContext<ServerCommandSource> context,
            SuggestionsBuilder suggestionsBuilder,
            CarpetScriptHost host
    ) throws CommandSyntaxException
    {
        String prefix = suggestionsBuilder.getRemaining();
        if (!caseSensitive) prefix = prefix.toLowerCase(Locale.ROOT);
        suggestFor(context, prefix, host).forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    protected List<String> suggestFor(CommandContext<ServerCommandSource> context, String prefix, CarpetScriptHost host) throws CommandSyntaxException
    {
        return getOptions(context, host).stream().filter(s -> optionMatchesPrefix(prefix, s)).collect(Collectors.toList());
    }

    protected Collection<String> getOptions(CommandContext<ServerCommandSource> context, CarpetScriptHost host) throws CommandSyntaxException
    {
        if (customSuggester != null)
        {
            Map<Value, Value> params = new HashMap<>();
            for(ParsedCommandNode<ServerCommandSource> pnode : context.getNodes())
            {
                CommandNode<ServerCommandSource> node = pnode.getNode();
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
            return ((ListValue) response).getItems().stream().map(Value::getString).collect(Collectors.toList());
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

    protected abstract Supplier<CommandArgument> factory();

    private static class StringArgument extends CommandArgument
    {
        Set<String> validOptions = Collections.emptySet();
        private StringArgument()
        {
            super("string", StringArgumentType.StringType.QUOTABLE_PHRASE.getExamples(), true);
        }

        @Override
        public ArgumentType<?> getArgumentType()
        {
            return StringArgumentType.string();
        }

        @Override
        public Value getValueFromContext(CommandContext<ServerCommandSource> context, String param) throws CommandSyntaxException
        {
            String choseValue = StringArgumentType.getString(context, param);
            if (!caseSensitive) choseValue = choseValue.toLowerCase(Locale.ROOT);
            if (!validOptions.isEmpty() && !validOptions.contains(choseValue))
            {
                throw new SimpleCommandExceptionType(new LiteralText("Incorrect value for "+param+": "+choseValue+" for custom type "+suffix)).create();
            }
            return StringValue.of(choseValue);
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host)
        {
            super.configure(config, host);
            if (config.containsKey("options"))
            {
                Value optionsValue = config.get("options");
                if (!(optionsValue instanceof ListValue)) throw new InternalExpressionException("Custom string type requires options passed as a list"+" for custom type "+suffix);
                validOptions = ((ListValue) optionsValue).getItems().stream()
                        .map(v -> caseSensitive?v.getString():(v.getString().toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toSet());
            }
        }

        @Override
        protected Collection<String> getOptions(CommandContext<ServerCommandSource> context, CarpetScriptHost host) throws CommandSyntaxException
        {
            return validOptions.isEmpty()?super.getOptions(context, host):validOptions;
        }

        @Override
        protected Supplier<CommandArgument> factory() { return WordArgument::new; }
    }

    private static class WordArgument extends StringArgument
    {
        private WordArgument() { super(); suffix = "term"; examples = StringArgumentType.StringType.SINGLE_WORD.getExamples(); }
        @Override
        public ArgumentType<?> getArgumentType() { return StringArgumentType.word(); }
        @Override
        protected Supplier<CommandArgument> factory() { return WordArgument::new; }
    }

    private static class GreedyStringArgument extends StringArgument
    {
        private GreedyStringArgument() { super();suffix = "text"; examples = StringArgumentType.StringType.GREEDY_PHRASE.getExamples(); }
        @Override
        public ArgumentType<?> getArgumentType() { return StringArgumentType.greedyString(); }
        @Override
        protected Supplier<CommandArgument> factory() { return GreedyStringArgument::new; }
    }

    private static class BlockPosArgument extends CommandArgument
    {
        private boolean mustBeLoaded = false;

        private BlockPosArgument()
        {
            super("pos", BlockPosArgumentType.blockPos().getExamples(), false);
        }

        @Override
        public ArgumentType<?> getArgumentType()
        {
            return BlockPosArgumentType.blockPos();
        }

        @Override
        public Value getValueFromContext(CommandContext<ServerCommandSource> context, String param) throws CommandSyntaxException
        {
            BlockPos pos = mustBeLoaded
                    ? BlockPosArgumentType.getLoadedBlockPos(context, param)
                    : BlockPosArgumentType.getBlockPos(context, param);
            return ValueConversions.of(pos);
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host)
        {
            super.configure(config, host);
            mustBeLoaded = config.getOrDefault("loaded", Value.FALSE).getBoolean();
        }

        @Override
        protected Supplier<CommandArgument> factory()
        {
            return BlockPosArgument::new;
        }
    }

    private static class LocationArgument extends CommandArgument
    {
        boolean blockCentered;

        private LocationArgument()
        {
            super("location", Vec3ArgumentType.vec3().getExamples(), false);
            blockCentered = true;
        }
        @Override
        protected ArgumentType<?> getArgumentType()
        {
            return Vec3ArgumentType.vec3(blockCentered);
        }

        @Override
        protected Value getValueFromContext(CommandContext<ServerCommandSource> context, String param) throws CommandSyntaxException
        {
            return ValueConversions.of(Vec3ArgumentType.getVec3(context, param));
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host)
        {
            super.configure(config, host);
            blockCentered = config.getOrDefault("block_centered", Value.TRUE).getBoolean();
        }

        @Override
        protected Supplier<CommandArgument> factory()
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
            super("entities", EntityArgumentType.entities().getExamples(), false);
            onlyFans = false;
            single = false;
        }
        @Override
        protected ArgumentType<?> getArgumentType()
        {
            if (onlyFans)
            {
                return single?EntityArgumentType.player():EntityArgumentType.players();
            }
            else
            {
                return single?EntityArgumentType.entity():EntityArgumentType.entities();
            }
        }

        @Override
        protected Value getValueFromContext(CommandContext<ServerCommandSource> context, String param) throws CommandSyntaxException
        {
            Collection<? extends Entity> founds = EntityArgumentType.getOptionalEntities(context, param);
            if (!single) return ListValue.wrap(founds.stream().map(EntityValue::new).collect(Collectors.toList()));
            if (founds.size() == 0) return Value.NULL;
            if (founds.size() == 1) return new EntityValue(founds.iterator().next());
            throw new SimpleCommandExceptionType(new LiteralText("Multiple entities returned while only one was requested"+" for custom type "+suffix)).create();
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host)
        {
            super.configure(config, host);
            onlyFans = config.getOrDefault("players", Value.FALSE).getBoolean();
            single = config.getOrDefault("single", Value.FALSE).getBoolean();
        }

        @Override
        protected Supplier<CommandArgument> factory()
        {
            return EntityArgument::new;
        }
    }

    private static class PlayerProfileArgument extends CommandArgument
    {
        boolean single;

        private PlayerProfileArgument()
        {
            super("players", GameProfileArgumentType.gameProfile().getExamples(), false);
            single = false;
        }
        @Override
        protected ArgumentType<?> getArgumentType()
        {
            return GameProfileArgumentType.gameProfile();
        }

        @Override
        protected Value getValueFromContext(CommandContext<ServerCommandSource> context, String param) throws CommandSyntaxException
        {
            Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, param);
            if (!single) return ListValue.wrap(profiles.stream().map(p -> StringValue.of(p.getName())).collect(Collectors.toList()));
            int size = profiles.size();
            if (size == 0) return Value.NULL;
            if (size == 1) return StringValue.of(profiles.iterator().next().getName());
            throw new SimpleCommandExceptionType(new LiteralText("Multiple game profiles returned while only one was requested"+" for custom type "+suffix)).create();
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host)
        {
            super.configure(config, host);
            single = config.getOrDefault("single", Value.FALSE).getBoolean();
        }

        @Override
        protected Supplier<CommandArgument> factory()
        {
            return PlayerProfileArgument::new;
        }
    }

    private static class ScoreholderArgument extends CommandArgument
    {
        boolean single;

        private ScoreholderArgument()
        {
            super("scoreholder", ScoreHolderArgumentType.scoreHolder().getExamples(), false);
            single = false;
            suggestionProvider = ScoreHolderArgumentType.SUGGESTION_PROVIDER;
        }
        @Override
        protected ArgumentType<?> getArgumentType()
        {
            return single?ScoreHolderArgumentType.scoreHolder():ScoreHolderArgumentType.scoreHolders();
        }

        @Override
        protected Value getValueFromContext(CommandContext<ServerCommandSource> context, String param) throws CommandSyntaxException
        {
            Collection<String> holders = ScoreHolderArgumentType.getScoreHolders(context, param);
            if (!single) return ListValue.wrap(holders.stream().map(StringValue::of).collect(Collectors.toList()));
            int size = holders.size();
            if (size == 0) return Value.NULL;
            if (size == 1) return StringValue.of(holders.iterator().next());
            throw new SimpleCommandExceptionType(new LiteralText("Multiple score holders returned while only one was requested"+" for custom type "+suffix)).create();
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host)
        {
            super.configure(config, host);
            single = config.getOrDefault("single", Value.FALSE).getBoolean();
        }

        @Override
        protected Supplier<CommandArgument> factory()
        {
            return PlayerProfileArgument::new;
        }
    }

    private static class TagArgument extends CommandArgument
    {
        boolean mapRequired;
        private TagArgument()
        {
            super("tag", NbtCompoundTagArgumentType.nbtCompound().getExamples(), false);
            mapRequired = true;
        }
        @Override
        protected ArgumentType<?> getArgumentType()
        {
            return mapRequired?NbtCompoundTagArgumentType.nbtCompound(): NbtTagArgumentType.nbtTag();
        }

        @Override
        protected Value getValueFromContext(CommandContext<ServerCommandSource> context, String param) throws CommandSyntaxException
        {
            if (mapRequired)
                return new NBTSerializableValue(NbtCompoundTagArgumentType.getCompoundTag(context, param));
            else
                return new NBTSerializableValue(NbtTagArgumentType.getTag(context, param));
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host)
        {
            super.configure(config, host);
            mapRequired = !config.getOrDefault("allow_element", Value.FALSE).getBoolean();
        }

        @Override
        protected Supplier<CommandArgument> factory()
        {
            return TagArgument::new;
        }
    }

    private static class CustomIdentifierArgument extends CommandArgument
    {
        Set<Identifier> validOptions = Collections.emptySet();

        protected CustomIdentifierArgument()
        {
            super("identifier", Collections.emptyList(), true);
        }

        @Override
        protected ArgumentType<?> getArgumentType()
        {
            return IdentifierArgumentType.identifier();
        }

        @Override
        protected Value getValueFromContext(CommandContext<ServerCommandSource> context, String param) throws CommandSyntaxException
        {
            Identifier choseValue = IdentifierArgumentType.getIdentifier(context, param);
            if (!validOptions.isEmpty() && !validOptions.contains(choseValue))
            {
                throw new SimpleCommandExceptionType(new LiteralText("Incorrect value for "+param+": "+choseValue+" for custom type "+suffix)).create();
            }
            return ValueConversions.of(choseValue);
        }

        @Override
        protected Supplier<CommandArgument> factory()
        {
            return CustomIdentifierArgument::new;
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host)
        {
            super.configure(config, host);
            if (config.containsKey("options"))
            {
                Value optionsValue = config.get("options");
                if (!(optionsValue instanceof ListValue)) throw new InternalExpressionException("Custom sting type requires options passed as a list"+" for custom type "+suffix);
                validOptions = ((ListValue) optionsValue).getItems().stream().map(v -> new Identifier(v.getString())).collect(Collectors.toSet());
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
        public ArgumentType<?> getArgumentType()
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
        public Value getValueFromContext(CommandContext<ServerCommandSource> context, String param) throws CommandSyntaxException
        {
            return new NumericValue(DoubleArgumentType.getDouble(context, param));
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host)
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
            if (max != null && min == null) throw new InternalExpressionException("Double types cannot be only upper-bounded"+" for custom type "+suffix);
        }

        @Override
        protected Supplier<CommandArgument> factory()
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
        public ArgumentType<?> getArgumentType()
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
        public Value getValueFromContext(CommandContext<ServerCommandSource> context, String param) throws CommandSyntaxException
        {
            return new NumericValue(LongArgumentType.getLong(context, param));
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host)
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
            if (max != null && min == null) throw new InternalExpressionException("Double types cannot be only upper-bounded"+" for custom type "+suffix);
        }

        @Override
        protected Supplier<CommandArgument> factory()
        {
            return IntArgument::new;
        }
    }
    private static class SlotArgument extends CommandArgument
    {
        private String restrict;
        private static final Map<String, Pair<Set<Integer>, Set<String>>> RESTRICTED_CONTAINERS = new HashMap<String, Pair<Set<Integer>, Set<String>>>(){{
            int i;
            for (String source : Arrays.asList("player", "enderchest", "equipment", "armor", "weapon", "container", "villager", "horse"))
                put(source, Pair.of(new HashSet<>(), new HashSet<>()));
            for (i = 0; i < 41; i++) get("player").getLeft().add(i);
            for(i = 0; i < 41; i++) get("player").getRight().add("container." + i);
            for(i = 0; i < 9; i++) get("player").getRight().add("hotbar." + i);
            for(i = 0; i < 27; i++) get("player").getRight().add("inventory." + i);
            for (String place : Arrays.asList("weapon", "weapon.mainhand", "weapon.offhand"))
            {
                get("player").getRight().add(place);
                get("equipment").getRight().add(place);
                get("weapon").getRight().add(place);
            }
            for (String place : Arrays.asList("armor.feet","armor.legs", "armor.chest","armor.head"))
            {
                get("player").getRight().add(place);
                get("equipment").getRight().add(place);
                get("armor").getRight().add(place);
            }

            for (i = 0; i < 27; i++) get("enderchest").getLeft().add(200+i);
            for(i = 0; i < 27; i++) get("enderchest").getRight().add("enderchest." + i);

            for (i = 0; i < 6; i++) get("equipment").getLeft().add(98+i);

            for (i = 0; i < 4; i++) get("armor").getLeft().add(100+i);

            for (i = 0; i < 2; i++) get("weapon").getLeft().add(98+i);

            for (i = 0; i < 54; i++) get("container").getLeft().add(i);
            for(i = 0; i < 41; i++) get("container").getRight().add("container." + i);

            for (i = 0; i < 8; i++) get("villager").getLeft().add(i);
            for(i = 0; i < 8; i++) get("villager").getRight().add("villager." + i);

            for (i = 0; i < 15; i++) get("horse").getLeft().add(500+i);
            for(i = 0; i < 15; i++) get("horse").getRight().add("horse." + i);
            get("horse").getLeft().add(400);
            get("horse").getRight().add("horse.saddle");
            get("horse").getLeft().add(401);
            get("horse").getRight().add("horse.armor");
        }};

        protected SlotArgument()
        {
            super("slot", ItemSlotArgumentType.itemSlot().getExamples(), false);
        }

        @Override
        protected ArgumentType<?> getArgumentType() throws CommandSyntaxException
        {
            return ItemSlotArgumentType.itemSlot();
        }

        @Override
        protected Value getValueFromContext(CommandContext<ServerCommandSource> context, String param) throws CommandSyntaxException
        {
            int slot = ItemSlotArgumentType.getItemSlot(context, param);
            if (restrict != null && !RESTRICTED_CONTAINERS.get(restrict).getLeft().contains(slot))
            {
                throw new SimpleCommandExceptionType(new LiteralText("Incorrect slot restricted to "+restrict+" for custom type "+suffix)).create();
            }
            return ValueConversions.ofVanillaSlotResult(slot);
        }

        @Override
        protected void configure(Map<String, Value> config, CarpetScriptHost host)
        {
            super.configure(config, host);
            if (config.containsKey("restrict"))
            {
                restrict = config.get("restrict").getString().toLowerCase(Locale.ROOT);
                needsMatching = true;
                if (!RESTRICTED_CONTAINERS.containsKey(restrict))
                    throw new InternalExpressionException("Incorrect slot restriction "+restrict+" for custom type "+suffix);
            }
        }

        @Override
        protected Collection<String> getOptions(CommandContext<ServerCommandSource> context, CarpetScriptHost host) throws CommandSyntaxException
        {
            return restrict==null?super.getOptions(context, host):RESTRICTED_CONTAINERS.get(restrict).getRight();
        }

        @Override
        protected Supplier<CommandArgument> factory()
        {
            return SlotArgument::new;
        }
    }

    @FunctionalInterface
    private interface ValueExtractor
    {
        Value apply(CommandContext<ServerCommandSource> ctx, String param) throws CommandSyntaxException;
    }

    @FunctionalInterface
    private interface ArgumentProvider
    {
        ArgumentType<?> get() throws CommandSyntaxException;
    }

    public static class VanillaUnconfigurableArgument extends  CommandArgument
    {
        private final ArgumentProvider argumentTypeSupplier;
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
        }
        public VanillaUnconfigurableArgument(
                String suffix,
                ArgumentProvider argumentTypeSupplier,
                ValueExtractor  valueExtractor,
                SuggestionProvider<ServerCommandSource> suggester
        )
        {
            super(suffix, Collections.emptyList(), false);
            this.suggestionProvider = suggester;
            this.providesExamples = false;
            this.argumentTypeSupplier = argumentTypeSupplier;
            this.valueExtractor = valueExtractor;
        }

        @Override
        protected ArgumentType<?> getArgumentType() throws CommandSyntaxException
        {
            return argumentTypeSupplier.get();
        }

        @Override
        protected Value getValueFromContext(CommandContext<ServerCommandSource> context, String param) throws CommandSyntaxException
        {
            return valueExtractor.apply(context, param);
        }

        @Override
        protected Supplier<CommandArgument> factory()
        {
            return () -> new VanillaUnconfigurableArgument(getTypeSuffix(), argumentTypeSupplier, valueExtractor, providesExamples);
        }
    }
}
