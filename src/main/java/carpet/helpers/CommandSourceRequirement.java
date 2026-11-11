package carpet.helpers;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.Predicate;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.ParseContext;
import net.minecraft.commands.arguments.ValidationWrapper;
import net.minecraft.commands.suggestions.SuggestionContext;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.PermissionSetSupplier;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Opt-in compatibility for requirements that need an entity or world, not just permissions.
 * Use vanilla PermissionProviderCheck for permission-only requirements.
 */
public record CommandSourceRequirement<T extends PermissionSetSupplier>(Predicate<CommandSourceStack> requirement) implements Predicate<T>
{
    @Override
    public boolean test(T context)
    {
        CommandSourceStack source;
        if (context instanceof CommandSourceStack commandSource)
        {
            source = commandSource;
        }
        else if (context instanceof SourceParseContext parseContext)
        {
            source = parseContext.source;
        }
        else if (context instanceof SuggestionContext suggestions && suggestions.entity() instanceof ServerPlayer player)
        {
            source = player.createCommandSourceStack();
        }
        else if (context instanceof ParseContext)
        {
            // Function compilation has permissions, but no executing entity or world.
            source = new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, context.permissions(), Component.empty(), null);
        }
        else
        {
            // Permission-only packet probes cannot establish source-dependent access.
            return false;
        }
        return requirement.test(source.withPermission(context.permissions()));
    }

    public <S extends PermissionSetSupplier> CommandSourceRequirement<S> cast()
    {
        return new CommandSourceRequirement<>(requirement);
    }

    public static ParseContext withSource(ParseContext context, CommandSourceStack source)
    {
        return new SourceParseContext(context, source);
    }

    private record SourceParseContext(ParseContext context, CommandSourceStack source) implements ParseContext
    {
        @Override
        public PermissionSet permissions()
        {
            return context.permissions();
        }

        @Override
        public HolderGetter.Provider holderGetters()
        {
            return context.holderGetters();
        }

        @Override
        public FeatureFlagSet enabledFeatures()
        {
            return context.enabledFeatures();
        }

        @Override
        public boolean allowsSelectors()
        {
            return context.allowsSelectors();
        }

        @Override
        public <V extends Validatable> ValidationWrapper<V> getOrCreateValidationWrapper(Holder<V> value, ContextKeySet parameters) throws CommandSyntaxException
        {
            return context.getOrCreateValidationWrapper(value, parameters);
        }
    }
}
