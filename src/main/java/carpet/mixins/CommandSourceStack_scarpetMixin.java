package carpet.mixins;

import carpet.fakes.CommandSourceStackInterface;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSigningContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.util.TaskChainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CommandSourceStack.class)
public class CommandSourceStack_scarpetMixin implements CommandSourceStackInterface {
    @Mutable @Shadow private boolean silent;
    @Mutable @Shadow private CommandResultCallback resultCallback;
    @Mutable @Shadow private EntityAnchorArgument.Anchor anchor;
    @Mutable @Shadow private CommandSigningContext signingContext;
    @Mutable @Shadow private TaskChainer chatMessageChainer;

    @Override
    public void setupPrivates(boolean bl, CommandResultCallback commandResultCallback, EntityAnchorArgument.Anchor anchor, CommandSigningContext commandSigningContext, TaskChainer taskChainer) {
        this.silent = bl;
        this.resultCallback = commandResultCallback;
        this.anchor = anchor;
        this.signingContext = commandSigningContext;
        this.chatMessageChainer = taskChainer;
    }
}
