package carpet.script.utils;

import carpet.script.exception.InternalExpressionException;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import org.lwjgl.system.CallbackI;

import java.util.Locale;

public class InputValidator {
    public static String validateSimpleString(String input, boolean strict)
    {
        String simplified = input.toLowerCase(Locale.ROOT).replaceAll("[^A-Za-z0-9+_]", "");
        if (simplified.isEmpty() || (strict && !simplified.equals(input)))
            throw new InternalExpressionException("simple name can only contain numbers, letter and _");
        return simplified;
    }

    public static Identifier identifierOf(String string)
    {
        try
        {
            return new Identifier(string);
        }
        catch (InvalidIdentifierException iie)
        {
            throw new InternalExpressionException("Incorrect identifier format '"+string+"': "+iie.getMessage());
        }
    }

}
