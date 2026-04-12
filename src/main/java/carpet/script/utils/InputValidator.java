package carpet.script.utils;

import carpet.script.exception.InternalExpressionException;

import java.util.Locale;

import net.minecraft.IdentifierException;
import net.minecraft.resources.Identifier;

public class InputValidator
{
    public static String validateSimpleString(String input, boolean strict)
    {
        String simplified = input.toLowerCase(Locale.ROOT).replaceAll("[^A-Za-z0-9+_]", "");
        if (simplified.isEmpty() || (strict && !simplified.equals(input)))
        {
            throw new InternalExpressionException("simple name can only contain numbers, letter and _");
        }
        return simplified;
    }

    public static Identifier identifierOf(String string)
    {
        try
        {
            return Identifier.parse(string);
        }
        catch (IdentifierException iie)
        {
            throw new InternalExpressionException("Incorrect identifier format '" + string + "': " + iie.getMessage());
        }
    }

}
