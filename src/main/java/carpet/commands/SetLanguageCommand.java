package carpet.commands;

import carpet.CarpetSettings;
import carpet.settings.SettingsManager;
import carpet.utils.LanguageUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SetLanguageCommand
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        LiteralArgumentBuilder<ServerCommandSource> zh_cn = literal("set-carpet-language").
                then(argument("language", StringArgumentType.word()).
                executes((c) -> tryUpdateLanguage(c.getSource(), StringArgumentType.getString(c, "language"))));
        dispatcher.register(zh_cn);
    }

    private static int tryUpdateLanguage(ServerCommandSource source, String lang){
        boolean success = LanguageUtils.updateLanguage(source, lang);
        return success ? 1 : 0;
    }

}
