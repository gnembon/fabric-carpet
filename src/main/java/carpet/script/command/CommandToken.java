package carpet.script.command;

import carpet.script.CarpetScriptHost;
import carpet.script.value.FunctionValue;
import com.google.common.collect.Lists;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.literal;

public class CommandToken implements Comparable<CommandToken>
{
    public String surface;
    public boolean isArgument;
    public CommandArgument type;

    private CommandToken(final String surface, final CommandArgument type)
    {
        this.surface = surface;
        this.type = type;
        isArgument = type != null;
    }

    public static CommandToken getToken(String source, final CarpetScriptHost host)
    {
        // todo add more type checking and return null
        if (!source.startsWith("<"))
        {
            return source.matches("[_a-zA-Z]+") ? new CommandToken(source, null) : null;
        }
        source = source.substring(1, source.length() - 1);
        return source.matches("[_a-zA-Z]+") ? new CommandToken(source, CommandArgument.getTypeForArgument(source, host)) : null;
    }

    public static List<CommandToken> parseSpec(String spec, final CarpetScriptHost host) throws CommandSyntaxException
    {
        spec = spec.trim();
        if (spec.isEmpty())
        {
            return Collections.emptyList();
        }
        final List<CommandToken> elements = new ArrayList<>();
        final HashSet<String> seenArgs = new HashSet<>();
        for (final String el : spec.split("\\s+"))
        {
            final CommandToken tok = CommandToken.getToken(el, host);
            if (tok == null)
            {
                throw CommandArgument.error("Unrecognized command token: " + el);
            }
            if (tok.isArgument)
            {
                if (seenArgs.contains(tok.surface))
                {
                    throw CommandArgument.error("Repeated command argument: " + tok.surface + ", for '" + spec + "'. Argument names have to be unique");
                }
                seenArgs.add(tok.surface);
            }
            elements.add(tok);
        }
        return elements;
    }

    public static String specFromSignature(final FunctionValue function)
    {
        final List<String> tokens = Lists.newArrayList(function.getString());
        for (final String arg : function.getArguments())
        {
            tokens.add("<" + arg + ">");
        }
        return String.join(" ", tokens);
    }

    public ArgumentBuilder<CommandSourceStack, ?> getCommandNode(final CarpetScriptHost host) throws CommandSyntaxException
    {
        return isArgument ? CommandArgument.argumentNode(surface, host) : literal(surface);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        final CommandToken that = (CommandToken) o;
        return surface.equals(that.surface) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(surface, type);
    }

    @Override
    public int compareTo(final CommandToken o)
    {
        if (isArgument && !o.isArgument)
        {
            return 1;
        }
        if (!isArgument && o.isArgument)
        {
            return -1;
        }
        return surface.compareTo(o.surface);
    }
}
