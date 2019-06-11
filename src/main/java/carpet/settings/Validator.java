package carpet.settings;

import carpet.CarpetServer;
import carpet.utils.Messenger;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Locale;

public abstract class Validator<T> {
    /**
     * Validate the new value of a rule
     * @return true if valid, false if new rule invalid.
     */
    abstract boolean validate(ServerCommandSource source, ParsedRule<T> currentRule, T newValue, String string);


    public static class POSITIVE_NUMBER<T extends Number> extends Validator<T> {
        @Override
        public boolean validate(ServerCommandSource source, ParsedRule<T> currentRule, T newValue, String string) {
            return newValue.doubleValue() > 0;
        }
    }

    public static class _COMMAND<T> extends Validator<T> {
        @Override
        public boolean validate(ServerCommandSource source, ParsedRule<T> currentRule, T newValue, String string) {
            if (CarpetServer.settingsManager != null)
                CarpetServer.settingsManager.notifyPlayersCommandsChanged();
            return true;
        }
    }

    public static class WIP<T> extends Validator<T> {
        @Override
        public boolean validate(ServerCommandSource source, ParsedRule<T> currentRule, T newValue, String string) {
            if (source != null)
                try
                {
                    Messenger.m(source, "r Work in progress - limited or no functionality available for "+currentRule.name);
                }
                catch (NullPointerException ignored) { }
            return true;
        }
    }
    public static class _STRICT<T> extends Validator<T> {
        @Override
        public boolean validate(ServerCommandSource source, ParsedRule<T> currentRule, T newValue, String string) {
            if (!currentRule.options.contains(string.toLowerCase(Locale.ROOT)))
            {
                if (source != null)
                    try
                    {
                        Messenger.m(source, "r Valid options: " + currentRule.options.toString());
                    }
                    catch (NullPointerException ignored)
                    {
                    }
                return false;
            }
            return true;
        }
    }
}
