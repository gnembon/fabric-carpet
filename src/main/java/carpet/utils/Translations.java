package carpet.utils;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.CarpetSettings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.commands.CommandSourceStack;

public class Translations
{
    private static Map<String, String> translationMap;

    public static String tr(String key)
    {
        return translationMap == null ? key : translationMap.getOrDefault(key, key);
    }

    public static String tr(String key, String str)
    {
        return translationMap == null ? str : translationMap.getOrDefault(key, str);
    }

    public static boolean hasTranslations()
    {
        return translationMap != null;
    }

    public static boolean hasTranslation(String key)
    {
        return translationMap != null && translationMap.containsKey(key);
    }

    public static Map<String, String> getTranslationFromResourcePath(String path)
    {
        String dataJSON;
        try
        {
            dataJSON = IOUtils.toString(
                    Objects.requireNonNull(Translations.class.getClassLoader().getResourceAsStream(String.format("assets/carpet/lang/%s.json", CarpetSettings.language))),
                    StandardCharsets.UTF_8);
        } catch (NullPointerException | IOException e) {
            return null;
        }
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
        return gson.fromJson(dataJSON, new TypeToken<Map<String, String>>() {}.getType());
    }


    public static void updateLanguage(CommandSourceStack source)
    {
        if (CarpetSettings.language.equalsIgnoreCase("none"))
        {
            translationMap = null;
            return;
        }
        Map<String, String> translations = new HashMap<>();
        Map<String, String> trans = getTranslationFromResourcePath(String.format("assets/carpet/lang/%s.json", CarpetSettings.language));
        if (trans != null) trans.forEach(translations::put);

        for (CarpetExtension ext : CarpetServer.extensions)
        {
            Map<String, String> extMappings = ext.canHasTranslations(CarpetSettings.language);
            if (extMappings != null)
            {
                extMappings.forEach((key, value) ->
                {
                    if (!translations.containsKey(key)) translations.put(key, value);
                });
            }
        }
        translations.entrySet().removeIf(e -> e.getKey().startsWith("//"));
        if (translations.isEmpty())
        {
            translationMap = null;
            return;
        }
        translationMap = translations;
    }

    public static boolean isValidLanguage(String newValue)
    {
        // will put some validations for availble languages at some point
        return true;
    }
}
