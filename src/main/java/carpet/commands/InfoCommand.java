package carpet.commands;

import carpet.CarpetSettings;
import carpet.settings.SettingsManager;
import carpet.utils.BlockInfo;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.text.BaseText;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class InfoCommand
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        LiteralArgumentBuilder<ServerCommandSource> command = literal("info").
                requires((player) -> SettingsManager.canUseCommand(player, CarpetSettings.commandInfo)).
                then(literal("block").
                        then(argument("block position", BlockPosArgumentType.blockPos()).
                                executes( (c) -> infoBlock(
                                        c.getSource(),
                                        BlockPosArgumentType.getBlockPos(c, "block position"), null)).
                                then(literal("grep").
                                        then(argument("regexp",greedyString()).
                                                executes( (c) -> infoBlock(
                                                        c.getSource(),
                                                        BlockPosArgumentType.getBlockPos(c, "block position"),
                                                        getString(c, "regexp")))))));

        dispatcher.register(command);
    }

    public static void printBlock(List<BaseText> messages, ServerCommandSource source, String grep)
    {
        Messenger.m(source, "");
        if (grep != null)
        {
            Pattern p = Pattern.compile(grep);
            Messenger.m(source, messages.get(0));
            for (int i = 1; i<messages.size(); i++)
            {
                BaseText line = messages.get(i);
                Matcher m = p.matcher(line.getString());
                if (m.find())
                {
                    Messenger.m(source, line);
                }
            }
        }
        else
        {
            Messenger.send(source, messages);
        }
    }

    private static int infoBlock(ServerCommandSource source, BlockPos pos, String grep)
    {
        printBlock(BlockInfo.blockInfo(pos, source.getWorld()),source, grep);
        return 1;
    }

}
