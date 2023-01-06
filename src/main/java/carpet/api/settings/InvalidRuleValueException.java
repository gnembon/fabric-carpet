package carpet.api.settings;

import carpet.utils.Messenger;
import net.minecraft.commands.CommandSourceStack;

/**
 * <p>An {@link Exception} thrown when the value given for a {@link CarpetRule} is invalid.</p>
 * 
 * <p>It can hold a message to be sent to the executing source.</p>
 */
public class InvalidRuleValueException extends Exception {

    /**
     * <p>Constructs a new {@link InvalidRuleValueException} with a message that will be passed to the executing source</p>
     * @param cause The cause of the exception
     */
    public InvalidRuleValueException(String cause) {
        super(cause);
    }
    
    /**
     * <p>Constructs a new {@link InvalidRuleValueException} with no detail message, that therefore should not notify the source</p>
     */
    public InvalidRuleValueException() {
        super();
    }
    
    /**
     * <p>Notifies the given source with the exception's message if it exists, does nothing if it doesn't exist or it is {@code null}</p>
     * @param source The source to notify
     */
    public void notifySource(String ruleName, CommandSourceStack source) {
        if (getMessage() != null)
            Messenger.m(source, "r Couldn't set value for rule " + ruleName + ": "+ getMessage());
    }
}
