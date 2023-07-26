package carpet.commands;

import carpet.CarpetSettings;
import carpet.utils.BlockInfo;
import carpet.utils.CommandHelper;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class InfoCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext)
    {
        LiteralArgumentBuilder<CommandSourceStack> command = literal("info").
                requires((player) -> CommandHelper.canUseCommand(player, CarpetSettings.commandInfo)).
                then(literal("block").
                        then(argument("block position", BlockPosArgument.blockPos()).
                                executes( (c) -> infoBlock(
                                        c.getSource(),
                                        BlockPosArgument.getSpawnablePos(c, "block position"), null)).
                                then(literal("grep").
                                        then(argument("regexp",greedyString()).
                                                executes( (c) -> infoBlock(
                                                        c.getSource(),
                                                        BlockPosArgument.getSpawnablePos(c, "block position"),
                                                        getString(c, "regexp")))))));

        dispatcher.register(command);
    }

    public static void printBlock(List<Component> messages, CommandSourceStack source, String grep)
    {
        Messenger.m(source, "");
        if (grep != null)
        {
            Pattern p = Pattern.compile(grep);
            Messenger.m(source, messages.get(0));
            for (int i = 1; i<messages.size(); i++)
            {
                Component line = messages.get(i);
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

    private static int infoBlock(CommandSourceStack source, BlockPos pos, String grep)
    {
        if (!source.hasPermission(Commands.LEVEL_GAMEMASTERS)) {
            //check id pos is loaded
            if (!source.getLevel().hasChunkAt(pos)) {
                Messenger.m(source, "r Chunk is not loaded");
                return 0;
            }
            // verify it is in world bounds
            if (!source.getLevel().isInWorldBounds(pos)) {
                Messenger.m(source, "r Position is outside of world bounds");
                return 0;
            }
        }
        printBlock(BlockInfo.blockInfo(pos, source.getLevel()),source, grep);
        return 1;
    }

}
