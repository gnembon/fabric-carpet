package carpet.commands;

import carpet.helpers.EntityPlayerActionPack;
import carpet.CarpetSettings;
import carpet.fakes.ServerPlayerInterface;
import carpet.patches.EntityPlayerMPFake;
import carpet.utils.CommandHelper;
import carpet.utils.Messenger;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

public class PlayerCommand
{

    // TODO: allow any order like execute
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext)
    {
        final String[] gamemodeStrings = Arrays.stream(GameType.values())
                .map(GameType::getName)
                .toArray(String[]::new);
        LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = literal("player")
                .requires((player) -> CommandHelper.canUseCommand(player, CarpetSettings.commandPlayer))
                .then(argument("player", StringArgumentType.word())
                        .suggests( (c, b) -> suggest(getPlayers(c.getSource()), b))
                        .then(literal("stop").executes(PlayerCommand::stop))
                        .then(makeActionCommand("use", EntityPlayerActionPack.ActionType.USE))
                        .then(makeActionCommand("jump", EntityPlayerActionPack.ActionType.JUMP))
                        .then(makeActionCommand("attack", EntityPlayerActionPack.ActionType.ATTACK))
                        .then(makeActionCommand("drop", EntityPlayerActionPack.ActionType.DROP_ITEM))
                        .then(makeDropCommand("drop", false))
                        .then(makeActionCommand("dropStack", EntityPlayerActionPack.ActionType.DROP_STACK))
                        .then(makeDropCommand("dropStack", true))
                        .then(makeActionCommand("swapHands", EntityPlayerActionPack.ActionType.SWAP_HANDS))
                        .then(literal("hotbar")
                                .then(argument("slot", IntegerArgumentType.integer(1, 9))
                                        .executes(c -> manipulate(c, ap -> ap.setSlot(IntegerArgumentType.getInteger(c, "slot"))))))
                        .then(literal("kill").executes(PlayerCommand::kill))
                        .then(literal("shadow"). executes(PlayerCommand::shadow))
                        .then(literal("mount").executes(manipulation(ap -> ap.mount(true)))
                                .then(literal("anything").executes(manipulation(ap -> ap.mount(false)))))
                        .then(literal("dismount").executes(manipulation(EntityPlayerActionPack::dismount)))
                        .then(literal("sneak").executes(manipulation(ap -> ap.setSneaking(true))))
                        .then(literal("unsneak").executes(manipulation(ap -> ap.setSneaking(false))))
                        .then(literal("sprint").executes(manipulation(ap -> ap.setSprinting(true))))
                        .then(literal("unsprint").executes(manipulation(ap -> ap.setSprinting(false))))
                        .then(literal("look")
                                .then(literal("north").executes(manipulation(ap -> ap.look(Direction.NORTH))))
                                .then(literal("south").executes(manipulation(ap -> ap.look(Direction.SOUTH))))
                                .then(literal("east").executes(manipulation(ap -> ap.look(Direction.EAST))))
                                .then(literal("west").executes(manipulation(ap -> ap.look(Direction.WEST))))
                                .then(literal("up").executes(manipulation(ap -> ap.look(Direction.UP))))
                                .then(literal("down").executes(manipulation(ap -> ap.look(Direction.DOWN))))
                                .then(literal("at").then(argument("position", Vec3Argument.vec3()).executes(PlayerCommand::lookAt)))
                                .then(argument("direction", RotationArgument.rotation())
                                        .executes(c -> manipulate(c, ap -> ap.look(RotationArgument.getRotation(c, "direction").getRotation(c.getSource())))))
                        ).then(literal("turn")
                                .then(literal("left").executes(c -> manipulate(c, ap -> ap.turn(-90, 0))))
                                .then(literal("right").executes(c -> manipulate(c, ap -> ap.turn(90, 0))))
                                .then(literal("back").executes(c -> manipulate(c, ap -> ap.turn(180, 0))))
                                .then(argument("rotation", RotationArgument.rotation())
                                        .executes(c -> manipulate(c, ap -> ap.turn(RotationArgument.getRotation(c, "rotation").getRotation(c.getSource())))))
                        ).then(literal("move").executes(c -> manipulate(c, EntityPlayerActionPack::stopMovement))
                                .then(literal("forward").executes(c -> manipulate(c, ap -> ap.setForward(1))))
                                .then(literal("backward").executes(c -> manipulate(c, ap -> ap.setForward(-1))))
                                .then(literal("left").executes(c -> manipulate(c, ap -> ap.setStrafing(1))))
                                .then(literal("right").executes(c -> manipulate(c, ap -> ap.setStrafing(-1))))
                        ).then(literal("spawn").executes(PlayerCommand::spawn)
                                .then(literal("in").requires((player) -> player.hasPermission(2))
                                        .then(argument("gamemode", StringArgumentType.word())
                                                .suggests( (c, b) -> suggest(gamemodeStrings, b))
                                        .executes(PlayerCommand::spawn)))
                                .then(literal("at").then(argument("position", Vec3Argument.vec3()).executes(PlayerCommand::spawn)
                                        .then(literal("facing").then(argument("direction", RotationArgument.rotation()).executes(PlayerCommand::spawn)
                                                .then(literal("in").then(argument("dimension", DimensionArgument.dimension()).executes(PlayerCommand::spawn)
                                                        .then(literal("in").requires((player) -> player.hasPermission(2))
                                                                .then(argument("gamemode", StringArgumentType.word()).suggests( (c, b) -> suggest(gamemodeStrings, b))
                                                                .executes(PlayerCommand::spawn)
                                                        )))
                                        )))
                                ))
                        )
                );
        dispatcher.register(literalargumentbuilder);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> makeActionCommand(String actionName, EntityPlayerActionPack.ActionType type)
    {
        return literal(actionName)
                .executes(c -> action(c, type, EntityPlayerActionPack.Action.once()))
                .then(literal("once").executes(c -> action(c, type, EntityPlayerActionPack.Action.once())))
                .then(literal("continuous").executes(c -> action(c, type, EntityPlayerActionPack.Action.continuous())))
                .then(literal("interval").then(argument("ticks", IntegerArgumentType.integer(1))
                        .executes(c -> action(c, type, EntityPlayerActionPack.Action.interval(IntegerArgumentType.getInteger(c, "ticks"))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> makeDropCommand(String actionName, boolean dropAll)
    {
        return literal(actionName)
                .then(literal("all").executes(c ->manipulate(c, ap -> ap.drop(-2,dropAll))))
                .then(literal("mainhand").executes(c ->manipulate(c, ap -> ap.drop(-1,dropAll))))
                .then(literal("offhand").executes(c ->manipulate(c, ap -> ap.drop(40,dropAll))))
                .then(argument("slot", IntegerArgumentType.integer(0, 40)).
                        executes(c ->manipulate(c, ap -> ap.drop(
                                IntegerArgumentType.getInteger(c,"slot"),
                                dropAll
                        ))));
    }

    private static Collection<String> getPlayers(CommandSourceStack source)
    {
        Set<String> players = new LinkedHashSet<>(List.of("Steve", "Alex"));
        players.addAll(source.getOnlinePlayerNames());
        return players;
    }

    private static ServerPlayer getPlayer(CommandContext<CommandSourceStack> context)
    {
        String playerName = StringArgumentType.getString(context, "player");
        MinecraftServer server = context.getSource().getServer();
        return server.getPlayerList().getPlayerByName(playerName);
    }

    private static boolean cantManipulate(CommandContext<CommandSourceStack> context)
    {
        Player player = getPlayer(context);
        if (player == null)
        {
            Messenger.m(context.getSource(), "r Can only manipulate existing players");
            return true;
        }
        Player sendingPlayer;
        try
        {
            sendingPlayer = context.getSource().getPlayerOrException();
        }
        catch (CommandSyntaxException e)
        {
            return false;
        }

        if (!context.getSource().getServer().getPlayerList().isOp(sendingPlayer.getGameProfile()))
        {
            if (sendingPlayer != player && !(player instanceof EntityPlayerMPFake))
            {
                Messenger.m(context.getSource(), "r Non OP players can't control other real players");
                return true;
            }
        }
        return false;
    }

    private static boolean cantReMove(CommandContext<CommandSourceStack> context)
    {
        if (cantManipulate(context)) return true;
        Player player = getPlayer(context);
        if (player instanceof EntityPlayerMPFake) return false;
        Messenger.m(context.getSource(), "r Only fake players can be moved or killed");
        return true;
    }

    private static boolean cantSpawn(CommandContext<CommandSourceStack> context)
    {
        String playerName = StringArgumentType.getString(context, "player");
        MinecraftServer server = context.getSource().getServer();
        PlayerList manager = server.getPlayerList();
        Player player = manager.getPlayerByName(playerName);
        if (player != null)
        {
            Messenger.m(context.getSource(), "r Player ", "rb " + playerName, "r  is already logged on");
            return true;
        }
        GameProfile profile = server.getProfileCache().get(playerName).orElse(null);
        if (profile == null)
        {
            if (!CarpetSettings.allowSpawningOfflinePlayers)
            {
                Messenger.m(context.getSource(), "r Player "+playerName+" is either banned by Mojang, or auth servers are down. " +
                        "Banned players can only be summoned in Singleplayer and in servers in off-line mode.");
                return true;
            } else {
                profile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(playerName), playerName);
            }
        }
        if (manager.getBans().isBanned(profile))
        {
            Messenger.m(context.getSource(), "r Player ", "rb " + playerName, "r  is banned on this server");
            return true;
        }
        if (manager.isUsingWhitelist() && manager.isWhiteListed(profile) && !context.getSource().hasPermission(2))
        {
            Messenger.m(context.getSource(), "r Whitelisted players can only be spawned by operators");
            return true;
        }
        return false;
    }

    private static int kill(CommandContext<CommandSourceStack> context)
    {
        if (cantReMove(context)) return 0;
        getPlayer(context).kill();
        return 1;
    }

    private static int lookAt(CommandContext<CommandSourceStack> context)
    {
        return manipulate(context, ap -> {
            //try {
                ap.lookAt(Vec3Argument.getVec3(context, "position"));
            //} catch (CommandSyntaxException ignored) {}
        });
    }

    @FunctionalInterface
    interface SupplierWithCommandSyntaxException<T>
    {
        T get() throws CommandSyntaxException;
    }

    private static <T> T tryGetArg(SupplierWithCommandSyntaxException<T> a, SupplierWithCommandSyntaxException<T> b) throws CommandSyntaxException
    {
        try
        {
            return a.get();
        }
        catch (IllegalArgumentException e)
        {
            return b.get();
        }
    }

    private static int spawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        if (cantSpawn(context)) return 0;
        CommandSourceStack source = context.getSource();
        Vec3 pos = tryGetArg(
                () -> Vec3Argument.getVec3(context, "position"),
                source::getPosition
        );
        Vec2 facing = tryGetArg(
                () -> RotationArgument.getRotation(context, "direction").getRotation(context.getSource()),
                source::getRotation
        );
        ResourceKey<Level> dimType = tryGetArg(
                () -> DimensionArgument.getDimension(context, "dimension").dimension(),
                () -> source.getLevel().dimension() // dimension.getType()
        );
        GameType mode = GameType.CREATIVE;
        boolean flying = false;
        try
        {
            ServerPlayer player = context.getSource().getPlayerOrException();
            mode = player.gameMode.getGameModeForPlayer();
            flying = player.getAbilities().flying;
        }
        catch (CommandSyntaxException ignored) {}
        try {
            String opGameMode = StringArgumentType.getString(context, "gamemode");
            mode = GameType.byName(opGameMode, null);
            if(mode == null)
            {
                Messenger.m(context.getSource(), "rb Invalid game mode: "+opGameMode+".");
                return 0;
            }
        } catch (IllegalArgumentException ignored) {}
        if(mode == GameType.SPECTATOR)
        {
            // Force override flying to true for spectator players, or they will fell out of the world.
            flying = true;
        } else if(mode.isSurvival()){
            // Force override flying to false for survival-like players, or they will fly too
            flying = false;
        }
        String playerName = StringArgumentType.getString(context, "player");
        if (playerName.length()>maxPlayerLength(source.getServer()))
        {
            Messenger.m(context.getSource(), "rb Player name: "+playerName+" is too long");
            return 0;
        }

        MinecraftServer server = source.getServer();
        if (!Level.isInSpawnableBounds(BlockPos.containing(pos.x, pos.y, pos.z)))
        {
            Messenger.m(context.getSource(), "rb Player "+playerName+" cannot be placed outside of the world");
            return 0;
        }
        Player player = EntityPlayerMPFake.createFake(playerName, server, pos.x, pos.y, pos.z, facing.y, facing.x, dimType, mode, flying);
        if (player == null)
        {
            Messenger.m(context.getSource(), "rb Player " + StringArgumentType.getString(context, "player") + " doesn't exist " +
                    "and cannot spawn in online mode. Turn the server offline to spawn non-existing players");
            return 0;
        }
        return 1;
    }

    private static int maxPlayerLength(MinecraftServer server)
    {
        return server.getPort() >= 0 ? 16 : 40;
    }

    private static int stop(CommandContext<CommandSourceStack> context)
    {
        if (cantManipulate(context)) return 0;
        ServerPlayer player = getPlayer(context);
        ((ServerPlayerInterface) player).getActionPack().stopAll();
        return 1;
    }

    private static int manipulate(CommandContext<CommandSourceStack> context, Consumer<EntityPlayerActionPack> action)
    {
        if (cantManipulate(context)) return 0;
        ServerPlayer player = getPlayer(context);
        action.accept(((ServerPlayerInterface) player).getActionPack());
        return 1;
    }

    private static Command<CommandSourceStack> manipulation(Consumer<EntityPlayerActionPack> action)
    {
        return c -> manipulate(c, action);
    }

    private static int action(CommandContext<CommandSourceStack> context, EntityPlayerActionPack.ActionType type, EntityPlayerActionPack.Action action)
    {
        return manipulate(context, ap -> ap.start(type, action));
    }

    private static int shadow(CommandContext<CommandSourceStack> context)
    {
        ServerPlayer player = getPlayer(context);
        if (player instanceof EntityPlayerMPFake)
        {
            Messenger.m(context.getSource(), "r Cannot shadow fake players");
            return 0;
        }
        ServerPlayer sendingPlayer = null;
        try
        {
            sendingPlayer = context.getSource().getPlayerOrException();
        }
        catch (CommandSyntaxException ignored) { }

        if (sendingPlayer!=player && cantManipulate(context)) return 0;
        EntityPlayerMPFake.createShadow(player.server, player);
        return 1;
    }
}
