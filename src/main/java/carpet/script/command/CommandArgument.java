package carpet.script.command;

import carpet.script.exception.InternalExpressionException;
import carpet.script.value.ListValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import com.google.common.collect.Lists;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class CommandArgument
{
    private static final List<? extends CommandArgument> baseTypes = Lists.newArrayList(
            new WordArgument(), new PosArgument(), new FloatArgument()
    );

    public static final Map<String, CommandArgument> builtIns = baseTypes.stream().collect(Collectors.toMap(CommandArgument::getTypeSuffix, a -> a));

    private String suffix;
    private final Collection<String> examples;

    protected CommandArgument(
            String suffix,
            Collection<String> examples)
    {
        this.suffix = suffix;
        this.examples = examples;
    }

    public abstract ArgumentType<?> getArgument();

    public abstract Value getValueFromContext(CommandContext<ServerCommandSource> context, String param) throws CommandSyntaxException;



    public String getTypeSuffix()
    {
        return suffix;
    }

    public Collection<String> getExamples()
    {
        return examples;
    }


    public static CommandArgument buildFromConfig(String suffix, Map<String, Value> config)
    {
        if (!config.containsKey("type"))
            throw new InternalExpressionException("Custom types should at least specify the base type");
        String baseType = config.get("type").getString();
        if (!builtIns.containsKey(baseType))
            throw new InternalExpressionException("Unknown base type: "+baseType);
        CommandArgument variant = builtIns.get(baseType).builder().get();
        variant.configure(config);
        variant.suffix = suffix;
        return variant;
    };

    protected abstract void configure(Map<String, Value> config);


    protected CompletableFuture<Suggestions> suggest(
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
        return Collections.emptyList();
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

    public static class WordArgument extends CommandArgument
    {
        Set<String> validOptions = Collections.emptySet();
        boolean caseSensitive = false;
        private WordArgument()
        {
            super("string", StringArgumentType.StringType.SINGLE_WORD.getExamples());
        }

        @Override
        public ArgumentType<?> getArgument()
        {
            return StringArgumentType.word();
        }

        @Override
        public Value getValueFromContext(CommandContext<ServerCommandSource> context, String param) throws CommandSyntaxException
        {
            String choseValue = StringArgumentType.getString(context, param);
            if (!caseSensitive) choseValue = choseValue.toLowerCase(Locale.ROOT);
            if (!validOptions.isEmpty() && !validOptions.contains(choseValue))
            {
                throw new SimpleCommandExceptionType(new LiteralText("")).create();
            }
            return StringValue.of(choseValue);
        }

        @Override
        protected void configure(Map<String, Value> config)
        {
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
        protected Collection<String> getOptions() { return validOptions; }

        @Override
        protected Supplier<CommandArgument> builder() { return WordArgument::new; }
    }

    public static class PosArgument extends CommandArgument
    {
        private boolean mustBeLoaded = false;

        private PosArgument()
        {
            super("pos", BlockPosArgumentType.blockPos().getExamples());
        }

        @Override
        public ArgumentType<?> getArgument()
        {
            return BlockPosArgumentType.blockPos();
        }

        @Override
        public Value getValueFromContext(CommandContext<ServerCommandSource> context, String param) throws CommandSyntaxException
        {
            BlockPos pos = mustBeLoaded
                    ? BlockPosArgumentType.getLoadedBlockPos(context, param)
                    : BlockPosArgumentType.getBlockPos(context, param);
            return ValueConversions.fromPos(pos);
        }

        @Override
        protected void configure(Map<String, Value> config)
        {
            mustBeLoaded = config.getOrDefault("loaded", Value.FALSE).getBoolean();
        }

        @Override
        protected Supplier<CommandArgument> builder()
        {
            return PosArgument::new;
        }
    }

    public static class FloatArgument extends CommandArgument
    {
        private Double min = null;
        private Double max = null;
        private FloatArgument()
        {
            super("float", DoubleArgumentType.doubleArg().getExamples());
        }

        @Override
        public ArgumentType<?> getArgument()
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
}
