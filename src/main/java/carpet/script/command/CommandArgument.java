package carpet.script.command;

import carpet.fakes.BlockStateArgumentInterface;
import carpet.script.CarpetScriptHost;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BlockValue;
import carpet.script.value.EntityValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.ListValue;
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
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.advancement.Advancement;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.AngleArgumentType;
import net.minecraft.command.argument.BlockPosArgumentType;
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

import java.util.Collection;
import java.util.Collections;
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
            // block_predicate todo - not sure about the returned format. Needs to match block tags used in the API (future)
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
            // slot // item_slot
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
            // team
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
        String suffix = components[components.length-1].toLowerCase(Locale.ROOT);
        CommandArgument arg =  host.appArgTypes.get(suffix);
        if (arg != null) return arg;
        return builtIns.getOrDefault(suffix, DEFAULT);
    }

    public static RequiredArgumentBuilder<ServerCommandSource, ?> argumentNode(String param, CarpetScriptHost host) throws CommandSyntaxException
    {
        CommandArgument arg = getTypeForArgument(param, host);
        if (arg.suggestionProvider != null) return argument(param, arg.getArgumentType()).suggests(arg.suggestionProvider);
        return arg.needsMatching? argument(param, arg.getArgumentType()).suggests(arg::suggest) : argument(param, arg.getArgumentType());
    }

    protected String suffix;
    protected Collection<String> examples;
    protected boolean needsMatching = false;
    protected SuggestionProvider<ServerCommandSource> suggestionProvider;

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

    public static CommandArgument buildFromConfig(String suffix, Map<String, Value> config)
    {
        if (!config.containsKey("type"))
            throw new InternalExpressionException("Custom types should at least specify the type");
        String baseType = config.get("type").getString();
        if (!builtIns.containsKey(baseType))
            throw new InternalExpressionException("Unknown base type: "+baseType);
        CommandArgument variant = builtIns.get(baseType).builder().get();
        variant.configure(config);
        variant.suffix = suffix;
        return variant;
    }

    protected void configure(Map<String, Value> config)
    {
        if (config.containsKey("suggest"))
        {
            Value suggestionValue = config.get("suggest");
            if (!(suggestionValue instanceof ListValue)) throw new InternalExpressionException("Argument suggestions needs to be a list");
            examples = ((ListValue) suggestionValue).getItems().stream()
                    .map(Value::getString)
                    .collect(Collectors.toSet());
            if (!examples.isEmpty()) needsMatching = true;
        }
    };

    public CompletableFuture<Suggestions> suggest(
            CommandContext<ServerCommandSource> context,
            SuggestionsBuilder suggestionsBuilder
    )
    {
        String prefix = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);
        suggestFor(prefix).forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    protected List<String> suggestFor(String prefix)
    {
        return getOptions().stream().filter(s -> optionMatchesPrefix(prefix, s)).collect(Collectors.toList());
    }

    protected Collection<String> getOptions()
    {
        //return Lists.newArrayList("");
        // better than nothing I guess
        // nothing is such a bad default.
        if (needsMatching) return examples;
        return Collections.singletonList("... "+getTypeSuffix());
    }

    protected boolean optionMatchesPrefix(String prefix, String option)
    {
        for(int i = 0; !option.startsWith(prefix, i); ++i)
        {
            i = option.indexOf('_', i);
            if (i < 0) return false;
        }
        return true;
    }

    protected abstract Supplier<CommandArgument> builder();

    private static class StringArgument extends CommandArgument
    {
        Set<String> validOptions = Collections.emptySet();
        boolean caseSensitive = false;
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
                throw new SimpleCommandExceptionType(new LiteralText("Incorrect value for "+param+": "+choseValue)).create();
            }
            return StringValue.of(choseValue);
        }

        @Override
        protected void configure(Map<String, Value> config)
        {
            super.configure(config);
            caseSensitive = config.getOrDefault("case_sensitive", Value.FALSE).getBoolean();
            if (config.containsKey("options"))
            {
                Value optionsValue = config.get("options");
                if (!(optionsValue instanceof ListValue)) throw new InternalExpressionException("Custom sting type requires options passed as a list");
                validOptions = ((ListValue) optionsValue).getItems().stream()
                        .map(v -> caseSensitive?v.getString():v.getString().toLowerCase(Locale.ROOT))
                        .collect(Collectors.toSet());
            }
        }

        @Override
        protected Collection<String> getOptions() { return validOptions.isEmpty()?super.getOptions():validOptions; }

        @Override
        protected Supplier<CommandArgument> builder() { return WordArgument::new; }
    }

    private static class WordArgument extends StringArgument
    {
        private WordArgument() { super(); suffix = "term"; examples = StringArgumentType.StringType.SINGLE_WORD.getExamples(); }
        @Override
        public ArgumentType<?> getArgumentType() { return StringArgumentType.word(); }
        @Override
        protected Supplier<CommandArgument> builder() { return WordArgument::new; }
    }

    private static class GreedyStringArgument extends StringArgument
    {
        private GreedyStringArgument() { super();suffix = "text"; examples = StringArgumentType.StringType.GREEDY_PHRASE.getExamples(); }
        @Override
        public ArgumentType<?> getArgumentType() { return StringArgumentType.greedyString(); }
        @Override
        protected Supplier<CommandArgument> builder() { return GreedyStringArgument::new; }
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
        protected void configure(Map<String, Value> config)
        {
            super.configure(config);
            mustBeLoaded = config.getOrDefault("loaded", Value.FALSE).getBoolean();
        }

        @Override
        protected Supplier<CommandArgument> builder()
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
        protected void configure(Map<String, Value> config)
        {
            super.configure(config);
            blockCentered = config.getOrDefault("block_centered", Value.TRUE).getBoolean();
        }

        @Override
        protected Supplier<CommandArgument> builder()
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
            super("entity", EntityArgumentType.entities().getExamples(), false);
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
            throw new SimpleCommandExceptionType(new LiteralText("Multiple entities returned while only one was requested")).create();
        }

        @Override
        protected void configure(Map<String, Value> config)
        {
            super.configure(config);
            onlyFans = config.getOrDefault("players", Value.FALSE).getBoolean();
            single = config.getOrDefault("single", Value.FALSE).getBoolean();
        }

        @Override
        protected Supplier<CommandArgument> builder()
        {
            return EntityArgument::new;
        }
    }

    private static class PlayerProfileArgument extends CommandArgument
    {
        boolean single;

        private PlayerProfileArgument()
        {
            super("player", GameProfileArgumentType.gameProfile().getExamples(), false);
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
            throw new SimpleCommandExceptionType(new LiteralText("Multiple game profiles returned while only one was requested")).create();
        }

        @Override
        protected void configure(Map<String, Value> config)
        {
            super.configure(config);
            single = config.getOrDefault("single", Value.FALSE).getBoolean();
        }

        @Override
        protected Supplier<CommandArgument> builder()
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
            throw new SimpleCommandExceptionType(new LiteralText("Multiple score holders returned while only one was requested")).create();
        }

        @Override
        protected void configure(Map<String, Value> config)
        {
            super.configure(config);
            single = config.getOrDefault("single", Value.FALSE).getBoolean();
        }

        @Override
        protected Supplier<CommandArgument> builder()
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
        protected void configure(Map<String, Value> config)
        {
            super.configure(config);
            mapRequired = !config.getOrDefault("allow_element", Value.FALSE).getBoolean();
        }

        @Override
        protected Supplier<CommandArgument> builder()
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
                throw new SimpleCommandExceptionType(new LiteralText("Incorrect value for "+param+": "+choseValue)).create();
            }
            return ValueConversions.of(choseValue);
        }

        @Override
        protected Supplier<CommandArgument> builder()
        {
            return CustomIdentifierArgument::new;
        }

        @Override
        protected void configure(Map<String, Value> config)
        {
            super.configure(config);
            if (config.containsKey("options"))
            {
                Value optionsValue = config.get("options");
                if (!(optionsValue instanceof ListValue)) throw new InternalExpressionException("Custom sting type requires options passed as a list");
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
        protected void configure(Map<String, Value> config)
        {
            super.configure(config);
            if (config.containsKey("min"))
            {
                min = NumericValue.asNumber(config.get("min"), "min").getDouble();
            }
            if (config.containsKey("max"))
            {
                max = NumericValue.asNumber(config.get("max"), "max").getDouble();
            }
            if (max != null && min == null) throw new InternalExpressionException("Double types cannot be only upper-bounded");
        }

        @Override
        protected Supplier<CommandArgument> builder()
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
        protected void configure(Map<String, Value> config)
        {
            super.configure(config);
            if (config.containsKey("min"))
            {
                min = NumericValue.asNumber(config.get("min"), "min").getLong();
            }
            if (config.containsKey("max"))
            {
                max = NumericValue.asNumber(config.get("max"), "max").getLong();
            }
            if (max != null && min == null) throw new InternalExpressionException("Double types cannot be only upper-bounded");
        }

        @Override
        protected Supplier<CommandArgument> builder()
        {
            return IntArgument::new;
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
        protected Supplier<CommandArgument> builder()
        {
            return () -> new VanillaUnconfigurableArgument(getTypeSuffix(), argumentTypeSupplier, valueExtractor, providesExamples);
        }
    }
}
