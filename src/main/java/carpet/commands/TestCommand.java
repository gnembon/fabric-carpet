package carpet.commands;

import carpet.CarpetServer;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class TestCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(literal("testcarpet").
                then(literal("dump").
                        executes((c) -> CarpetServer.settingsManager.printAllRulesToLog(null)).
                        then(argument("category", word()).
                                executes( (c) -> CarpetServer.settingsManager.printAllRulesToLog(getString(c, "category"))))).
                then(argument("first",word()).
                        executes( (c)-> test(c, getString(c, "first")+" 1"))).
                then(argument("second", word()).
                        executes( (c)-> test(c, getString(c, "second")+" 2"))));
    }

    private static int test(CommandContext<CommandSourceStack> c, String term)
    {
        Messenger.m(c.getSource(),term.substring(0,1)+" "+term+": how did you get here?");
        return 1;
    }
}
