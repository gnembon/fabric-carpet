package carpet.fakes;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public interface CommandsInterface
{
    /**
     * The full command tree retained before Minecraft converts it into parsing and suggestion trees.
     * After mutating it on the server thread, call {@link #carpet$refreshCommandTrees()} and resend commands to players.
     */
    CommandDispatcher<CommandSourceStack> carpet$getDispatcher();

    /**
     * Rebuilds both derived trees without replacing boxes retained by other command consumers.
     * Already parsed commands and functions are not invalidated.
     */
    void carpet$refreshCommandTrees();
}
