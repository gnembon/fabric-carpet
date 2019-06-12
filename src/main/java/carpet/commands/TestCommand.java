package carpet.commands;

import carpet.CarpetServer;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TestCommand
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        dispatcher.register(literal("test").
                then(literal("dump").executes((c) -> CarpetServer.settingsManager.printAllRulesToLog())).
                then(argument("first",word()).
                        executes( (c)-> test(c, getString(c, "first")+" 1"))).
                then(argument("second", word()).
                        executes( (c)-> test(c, getString(c, "second")+" 2"))));
    }

    private static int test(CommandContext<ServerCommandSource> c, String term)
    {
        Messenger.m(c.getSource(),term.substring(0,1)+" "+term+": how did you get here?");
        return 1;
    }
}
