package carpet.mixins;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import carpet.fakes.CommandNodeInterface;

@Mixin(value = CommandNode.class, remap = false)
public class CommandNode_scarpetCommandsMixin<S> implements CommandNodeInterface {
    @Shadow @Final
    private Map<String, CommandNode<S>> children;
    @Shadow @Final
    private Map<String, LiteralCommandNode<S>> literals;
    @Shadow @Final
    private Map<String, ArgumentCommandNode<S, ?>> arguments;
    
    @Override
    public void carpet$removeChild(String name) {
        this.children.remove(name);
        this.literals.remove(name);
        this.arguments.remove(name);
    }
}
