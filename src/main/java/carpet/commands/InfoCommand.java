package carpet.commands;

import carpet.settings.CarpetSettings;
import carpet.utils.BlockInfo;
import carpet.utils.EntityInfo;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.text.BaseText;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.arguments.BlockPosArgumentType;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
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
                requires((player) -> CarpetSettings.commandInfo).
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
                                                        getString(c, "regexp"))))))).
                then(literal("entity").
                        then(argument("entity selector", EntityArgumentType.entities()).
                                executes( (c) -> infoEntities(
                                        c.getSource(), EntityArgumentType.getEntities(c,"entity selector"), null)).
                                then(literal("grep").
                                        then(argument("regexp",greedyString()).
                                                executes( (c) -> infoEntities(
                                                        c.getSource(),
                                                        EntityArgumentType.getEntities(c,"entity selector"),
                                                        getString(c, "regexp")))))));

        dispatcher.register(command);
    }

    public static void printEntity(List<BaseText> messages, ServerCommandSource source, String grep)
    {
        List<BaseText> actual = new ArrayList<>();
        if (grep != null)
        {
            Pattern p = Pattern.compile(grep);
            actual.add(messages.get(0));
            boolean empty = true;
            for (int i = 1; i<messages.size(); i++)
            {
                BaseText line = messages.get(i);
                Matcher m = p.matcher(line.getString());
                if (m.find())
                {
                    empty = false;
                    actual.add(line);
                }
            }
            if (empty)
            {
                return;
            }
        }
        else
        {
            actual = messages;
        }
        Messenger.m(source, "");
        Messenger.send(source, actual);
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



    private static int infoEntities(ServerCommandSource source, Collection<? extends Entity> entities, String grep)
    {
        for (Entity e: entities)
        {
            List<BaseText> report = EntityInfo.entityInfo(e, source.getWorld());
            printEntity(report, source, grep);
        }
        return 1;
    }
    private static int infoBlock(ServerCommandSource source, BlockPos pos, String grep)
    {
        printBlock(BlockInfo.blockInfo(pos, source.getWorld()),source, grep);
        return 1;
    }

}
