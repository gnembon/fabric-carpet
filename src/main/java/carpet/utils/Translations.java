package carpet.utils;

import carpet.CarpetSettings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

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

    public static void updateLanguage(ServerCommandSource source)
    {
        if (CarpetSettings.language.equalsIgnoreCase("none"))
        {
            translationMap = null;
            return;
        }
        String langJs;
        try
        {
            langJs = IOUtils.toString(
                    Objects.requireNonNull(Translations.class.getClassLoader().getResourceAsStream(String.format("assets/carpet/lang/%s.json", CarpetSettings.language))),
                    StandardCharsets.UTF_8);
        } catch (NullPointerException | IOException e) {
            Messenger.m(source, "r Failed to update language");
            return;
        }
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        translationMap = gson.fromJson(langJs, type);
        translationMap.entrySet().removeIf(e -> e.getKey().startsWith("//"));
    }


    public static boolean isValidLanguage(String newValue)
    {
        // will put some validations for availble languages at some point
        return true;
    }
}
