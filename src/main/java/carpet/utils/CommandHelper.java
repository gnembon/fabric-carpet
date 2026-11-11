package carpet.utils;

import carpet.CarpetSettings;
import carpet.helpers.CommandSourceRequirement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.commands.CommandPermissionLevels;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionProviderCheck;
import net.minecraft.server.permissions.PermissionSet;

/**
 * A few helpful methods to work with settings and commands.
 * 
 * This is not any kind of API, but it's unlikely to change
 *
 */
public final class CommandHelper {
    private CommandHelper() {}

    /**
     * A native command requirement that reads the current rule value on every permission check.
     * Serialization captures its current value without changing the live server-side check.
     */
    public static PermissionProviderCheck<CommandSourceStack> commandPermission(Supplier<?> commandLevel)
    {
        return Commands.hasPermission(new RulePermissionCheck(commandLevel));
    }

    /**
     * A native requirement for live conditions such as a settings manager's lock state.
     */
    public static PermissionProviderCheck<CommandSourceStack> requireCondition(BooleanSupplier condition)
    {
        return commandPermission(condition::getAsBoolean);
    }

    /**
     * Combines requirements without turning native permission checks back into source-dependent lambdas.
     */
    public static Predicate<CommandSourceStack> andRequirements(Predicate<CommandSourceStack> first, Predicate<CommandSourceStack> second)
    {
        if (first instanceof PermissionProviderCheck<CommandSourceStack> firstCheck
                && second instanceof PermissionProviderCheck<CommandSourceStack> secondCheck)
        {
            if (firstCheck.test() == CommandPermissionLevels.LEVEL_ALL)
                return secondCheck;
            if (secondCheck.test() == CommandPermissionLevels.LEVEL_ALL)
                return firstCheck;
            return Commands.hasPermission(new AllOfPermissionCheck(firstCheck.test(), secondCheck.test()));
        }
        return new CommandSourceRequirement<>(source -> first.test(source) && second.test(source));
    }

    public static void registerPermissionCheckTypes(WritableRegistry<MapCodec<? extends PermissionCheck>> registry)
    {
        Registry.register(registry, Identifier.fromNamespaceAndPath("carpet", "command_permission"), RulePermissionCheck.CODEC);
        Registry.register(registry, Identifier.fromNamespaceAndPath("carpet", "all_of"), AllOfPermissionCheck.CODEC);
    }

    private record RulePermissionCheck(Supplier<?> commandLevel) implements PermissionCheck
    {
        private static final Set<String> LEVELS = Set.of("true", "false", "ops", "0", "1", "2", "3", "4");
        private static final MapCodec<RulePermissionCheck> CODEC = Codec.STRING
                .validate(level -> LEVELS.contains(level)
                        ? DataResult.success(level)
                        : DataResult.error(() -> "Invalid command permission level: " + level))
                .fieldOf("level")
                .xmap(level -> new RulePermissionCheck(() -> level), check -> check.commandLevel.get().toString());

        @Override
        public boolean check(PermissionSet permissions)
        {
            return canUseCommand(permissions, commandLevel.get());
        }

        @Override
        public MapCodec<RulePermissionCheck> codec()
        {
            return CODEC;
        }
    }

    private record AllOfPermissionCheck(PermissionCheck first, PermissionCheck second) implements PermissionCheck
    {
        private static final MapCodec<AllOfPermissionCheck> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                PermissionCheck.CODEC.fieldOf("first").forGetter(AllOfPermissionCheck::first),
                PermissionCheck.CODEC.fieldOf("second").forGetter(AllOfPermissionCheck::second)
        ).apply(instance, AllOfPermissionCheck::new));

        @Override
        public boolean check(PermissionSet permissions)
        {
            return first.check(permissions) && second.check(permissions);
        }

        @Override
        public MapCodec<AllOfPermissionCheck> codec()
        {
            return CODEC;
        }
    }

    /**
     * Notifies all players that the commands changed by resending the command tree.
     */
    public static void notifyPlayersCommandsChanged(MinecraftServer server)
    {
        if (server == null || server.getPlayerList() == null)
        {
            return;
        }
        server.schedule(new TickTask(server.getTickCount(), () ->
        {
            try {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    server.getCommands().sendCommands(player);
                }
            }
            catch (NullPointerException e)
            {
                CarpetSettings.LOG.warn("Exception while refreshing commands, please report this to Carpet", e);
            }
        }));
    }
    
    /**
     * Whether the given source has enough permission level to run a command that requires the given commandLevel
     */
    public static boolean canUseCommand(CommandSourceStack source, Object commandLevel)
    {
        return canUseCommand(source.permissions(), commandLevel);
    }

    private static boolean canUseCommand(PermissionSet permissions, Object commandLevel)
    {
        if (commandLevel instanceof Boolean) return (Boolean) commandLevel;
        String commandLevelString = commandLevel.toString();
        return switch (commandLevelString)
        {
            case "true"  -> true;
            case "false" -> false;
            case "ops"   -> CommandPermissionLevels.LEVEL_GAMEMASTERS.check(permissions); // typical for other cheaty commands
            case "0" ->  CommandPermissionLevels.LEVEL_ALL.check(permissions);
            case "1" -> CommandPermissionLevels.LEVEL_MODERATORS.check(permissions);
            case "2" -> CommandPermissionLevels.LEVEL_GAMEMASTERS.check(permissions);
            case "3" -> CommandPermissionLevels.LEVEL_ADMINS.check(permissions);
            case "4" -> CommandPermissionLevels.LEVEL_OWNERS.check(permissions);
            default -> false;
        };
    }
}
