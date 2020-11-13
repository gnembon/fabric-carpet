package carpet.script.command;

import carpet.script.CarpetScriptHost;
import carpet.script.value.FunctionValue;
import com.google.common.collect.Lists;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static net.minecraft.server.command.CommandManager.literal;

public class CommandToken implements Comparable<CommandToken>
{
    public String surface;
    public boolean isArgument;
    public CommandArgument type;

    private CommandToken(String surface, CommandArgument type )
    {
        this.surface = surface;
        this.type = type;
        isArgument = type != null;
    }

    public static CommandToken getToken(String source, CarpetScriptHost host)
    {
        // todo add more type checking and return null
        if (!source.startsWith("<"))
        {
            return new CommandToken(source, null);
        }
        source = source.substring(1, source.length()-1);
        CommandArgument arg = CommandArgument.getTypeForArgument(source, host);
        return new CommandToken(source, arg);
    }

    public static List<CommandToken> parseSpec(String spec, CarpetScriptHost host) throws CommandSyntaxException
    {
        spec = spec.trim();
        if (spec.isEmpty()) return Collections.emptyList();
        List<CommandToken> elements = new ArrayList<>();
        for (String el: spec.split("\\s+"))
        {
            CommandToken tok = CommandToken.getToken(el, host);
            if (tok == null) throw CommandArgument.error("Unrecognized command token: "+ el);
            elements.add(tok);
        }
        return elements;
    }

    public static String specFromSignature(FunctionValue function)
    {
        List<String> tokens = Lists.newArrayList(function.getString());
        for (String arg : function.getArguments()) tokens.add("<"+arg+">");
        return String.join(" ", tokens);
    }


    public ArgumentBuilder<ServerCommandSource, ?> getCommandNode(CarpetScriptHost host) throws CommandSyntaxException
    {
        if (isArgument)
            return CommandArgument.argumentNode(surface, host);
        return literal(surface);

    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandToken that = (CommandToken) o;
        return surface.equals(that.surface) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(surface, type);
    }

    @Override
    public int compareTo(CommandToken o)
    {
        if (isArgument && !o.isArgument) return 1;
        if (!isArgument && o.isArgument) return -1;
        return surface.compareTo(o.surface);
    }
}
