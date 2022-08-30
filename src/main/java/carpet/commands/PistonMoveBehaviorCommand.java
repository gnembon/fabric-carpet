package carpet.commands;

import java.util.Collection;
import java.util.LinkedList;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import carpet.CarpetSettings;
import carpet.helpers.PistonMoveBehaviorManager;
import carpet.helpers.PistonMoveBehaviorManager.PistonMoveBehavior;
import carpet.utils.BlockUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;

public class PistonMoveBehaviorCommand {

    private static final String[] BEHAVIOR_NAMES;

    static {

        PistonMoveBehavior[] behaviors = PistonMoveBehavior.ALL;
        BEHAVIOR_NAMES = new String[behaviors.length];

        for (PistonMoveBehavior behavior : behaviors) {
            BEHAVIOR_NAMES[behavior.getIndex()] = behavior.getName();
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.
            literal("pistonmovebehavior").
            requires(source -> CarpetSettings.commandPistonMoveBehavior).
            then(Commands.
                literal("get").
                then(Commands.
                    argument("block", BlockStateArgument.block(buildContext)).
                    executes(context -> query(context.getSource(), BlockStateArgument.getBlock(context, "block"))))).
            then(Commands.
                literal("override").
                then(Commands.
                    argument("block", BlockStateArgument.block(buildContext)).
                    executes(context -> queryOverride(context.getSource(), false, BlockStateArgument.getBlock(context, "block"))).
                    then(Commands.
                        argument("piston move behavior", StringArgumentType.word()).
                        suggests((context, suggestionsBuilder) -> SharedSuggestionProvider.suggest(BEHAVIOR_NAMES, suggestionsBuilder)).
                        executes(context -> setOverride(context.getSource(), false, BlockStateArgument.getBlock(context, "block"), parsePistonMoveBehavior(StringArgumentType.getString(context, "piston move behavior"))))))).
            then(Commands.
                literal("defaultOverride").
                then(Commands.
                    argument("block", BlockStateArgument.block(buildContext)).
                    executes(context -> queryOverride(context.getSource(), true, BlockStateArgument.getBlock(context, "block"))).
                    then(Commands.
                        argument("piston move behavior", StringArgumentType.word()).
                        suggests((context, suggestionsBuilder) -> SharedSuggestionProvider.suggest(BEHAVIOR_NAMES, suggestionsBuilder)).
                        executes(context -> setOverride(context.getSource(), true, BlockStateArgument.getBlock(context, "block"), parsePistonMoveBehavior(StringArgumentType.getString(context, "piston move behavior")))))));

        dispatcher.register(builder);
    }

    private static PistonMoveBehavior parsePistonMoveBehavior(String name) throws CommandSyntaxException {
        PistonMoveBehavior behavior = PistonMoveBehavior.fromName(name);

        if (behavior == null) {
            throw new SimpleCommandExceptionType(Component.literal("Uknown piston move behavior: " + name)).create();
        }

        return behavior;
    }

    private static int query(CommandSourceStack source, BlockInput input) {
        BlockState state = input.getState();
        PushReaction pushReaction = state.getPistonPushReaction();
        PistonMoveBehavior behavior = PistonMoveBehavior.fromPushReaction(pushReaction);
        PistonMoveBehavior override = PistonMoveBehaviorManager.getOverride(state);

        MutableComponent message = Component.
            literal("block state ").
            append(Component.
                literal(BlockUtils.blockStateAsString(state)).
                withStyle(ChatFormatting.YELLOW)).
            append(" has piston move behavior ").
            append(Component.
                literal(behavior.getName()).
                append(" (").
                append(override.isPresent() ? "modified" : "vanilla").
                append(")").
                withStyle(override.isPresent() ? ChatFormatting.GOLD : ChatFormatting.GREEN, ChatFormatting.BOLD));
        source.sendSuccess(message, false);

        return Command.SINGLE_SUCCESS;
    }

    private static int queryOverride(CommandSourceStack source, boolean defaults, BlockInput input) {
        BlockState state = input.getState();
        PistonMoveBehavior override = PistonMoveBehaviorManager.getOverride(state);
        PistonMoveBehavior defaultOverride = PistonMoveBehaviorManager.getDefaultOverride(state);
        PistonMoveBehavior queriedOverride = defaults ? defaultOverride : override;

        MutableComponent message = Component.
            literal("block state ").
            append(Component.
                literal(BlockUtils.blockStateAsString(state)).
                withStyle(ChatFormatting.YELLOW)).
            append(" has the ").
            append(defaults ? "default " : "").
            append("piston move behavior override ").
            append(Component.
                literal(queriedOverride.getName()).
                withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

        if (!defaults) {
            boolean modified = (override != defaultOverride);
            message.
                append(Component.
                    literal(" (").
                    append(modified ? "modified" : "default").
                    append(")").
                    withStyle(modified ? ChatFormatting.GOLD : ChatFormatting.GREEN, ChatFormatting.BOLD));
        }

        source.sendSuccess(message, false);

        return Command.SINGLE_SUCCESS;
    }

    private static int setOverride(CommandSourceStack source, boolean defaults, BlockInput input, PistonMoveBehavior override) throws CommandSyntaxException {
        BlockState state = input.getState();
        Collection<Property<?>> properties = input.getDefinedProperties();
        Collection<BlockState> states = collectMatchingBlockStates(state, properties);
        PistonMoveBehavior defaultOverride = PistonMoveBehaviorManager.getDefaultOverride(state);

        for (BlockState blockState : states) {
            if (defaults) {
                PistonMoveBehaviorManager.setDefaultOverride(blockState, override);
            }

            PistonMoveBehaviorManager.setOverride(blockState, override);
        }

        String stateString = BlockUtils.blockStateAsString(state, properties);

        MutableComponent message = Component.
            literal("set the ").
            append(defaults ? "default " : "").
            append("piston move behavior override of all block states matching ").
            append(Component.
                literal(stateString).
                withStyle(ChatFormatting.YELLOW)).
            append(" to ").
            append(Component.
                literal(override.getName()).
                withStyle((defaults || override == defaultOverride) ? ChatFormatting.GREEN : ChatFormatting.GOLD, ChatFormatting.BOLD));

        if (!defaults) {
            message.
                append(Component.
                    literal(" [change permanently?]").
                    withStyle(style -> {
                        return style.
                            applyFormat(ChatFormatting.AQUA).
                            withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Click to keep the overrides in " + PistonMoveBehaviorManager.Config.FILE_NAME + " to save across restarts"))).
                            withClickEvent(new ClickEvent(
                                ClickEvent.Action.SUGGEST_COMMAND,
                                String.format("/pistonmovebehavior defaultOverride %s %s", stateString, override.getName())));
                    }));
        }

        source.sendSuccess(message, true);

        return Command.SINGLE_SUCCESS;
    }

    private static Collection<BlockState> collectMatchingBlockStates(BlockState state, Collection<Property<?>> properties) throws CommandSyntaxException {
        Collection<BlockState> states = new LinkedList<>();

        for (BlockState blockState : state.getBlock().getStateDefinition().getPossibleStates()) {
            if (blockStatesMatchProperties(state, blockState, properties)) {
                if (!PistonMoveBehaviorManager.canChangeOverride(blockState)) {
                    throw new SimpleCommandExceptionType(Component.literal("Cannot change the piston move behavior of " + BlockUtils.blockStateAsString(blockState))).create();
                }

                states.add(blockState);
            }
        }

        return states;
    }

    private static boolean blockStatesMatchProperties(BlockState state1, BlockState state2, Collection<Property<?>> properties) {
        for (Property<?> property : properties) {
            if (state1.getValue(property) != state2.getValue(property)) {
                return false;
            }
        }

        return true;
    }
}
