package carpet.fakes;

import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSigningContext;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.util.TaskChainer;

public interface CommandSourceStackInterface {
    void setupPrivates(boolean bl, CommandResultCallback commandResultCallback, EntityAnchorArgument.Anchor anchor, CommandSigningContext commandSigningContext, TaskChainer taskChainer);
}
