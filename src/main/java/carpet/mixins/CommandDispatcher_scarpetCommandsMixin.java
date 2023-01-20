package carpet.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.RootCommandNode;

import carpet.fakes.CommandDispatcherInterface;
import carpet.fakes.CommandNodeInterface;

@Mixin(value = CommandDispatcher.class, remap = false)
public class CommandDispatcher_scarpetCommandsMixin<S> implements CommandDispatcherInterface {
    @Shadow @Final
    private RootCommandNode<S> root;

    @Override
    public void carpet$unregister(String node) {
        ((CommandNodeInterface)this.root).carpet$removeChild(node);
    }
}
