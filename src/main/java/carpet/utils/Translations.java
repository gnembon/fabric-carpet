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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Translations
{
    private static Map<String, String> translationMap = Collections.emptyMap();

    public static String tr(String key)
    {
        return translationMap.getOrDefault(key, key);
    }
    
    public static String trOrNull(String key)
    {
        return translationMap.get(key);
    }

    public static String tr(String key, String str)
    {
        return translationMap.getOrDefault(key, str);
    }

    public static boolean hasTranslations()
    {
        return !translationMap.isEmpty();
    }

    public static boolean hasTranslation(String key)
    {
        return translationMap.containsKey(key);
    }

    public static Map<String, String> getTranslationFromResourcePath(String path)
    {
        String dataJSON;
        try
        {
            dataJSON = IOUtils.toString(
                    Objects.requireNonNull(Translations.class.getClassLoader().getResourceAsStream(path)),
                    StandardCharsets.UTF_8);
        } catch (NullPointerException | IOException e) {
            return Map.of();
        }
        Gson gson = new GsonBuilder().setLenient().create();
        return gson.fromJson(dataJSON, new TypeToken<Map<String, String>>() {}.getType());
    }

    public static void updateLanguage()
    {
        Map<String, String> translations = new HashMap<>();
        translations.putAll(getTranslationFromResourcePath(String.format("assets/carpet/lang/%s.json", CarpetSettings.language)));

        for (CarpetExtension ext : CarpetServer.extensions)
        {
            Map<String, String> extMappings = ext.canHasTranslations(CarpetSettings.language);
            if (extMappings == null) continue; // would be nice to get rid of this, but too many extensions return null where they don't know they do
            boolean warned = false;
            for (var entry : extMappings.entrySet()) {
                var key = entry.getKey();
                // Migrate the old format
                if (!key.startsWith("carpet.")) {
                    if (key.startsWith("rule.")) {
                        // default to carpet's settings manager. Custom managers are really uncommon and the known ones don't provide translations anyway
                        key = TranslationKeys.BASE_RULE_NAMESPACE.formatted("carpet") + key.substring(5);
                    } else if (key.startsWith("category.")) {
                        key = TranslationKeys.CATEGORY_PATTERN.formatted("carpet", key.substring(9));
                    }
                    if (!warned && key != entry.getKey()) {
                        CarpetSettings.LOG.warn("""
                                Found outdated translation keys in extension '%s'!
                                These won't be supported in a later Carpet version!
                                Carpet will now try to map them to the correct keys in a best-effort basis""".formatted(ext.getClass().getName()));
                        warned = true;
                    }
                }
                translations.putIfAbsent(key, entry.getValue());
            }
        }
        translations.keySet().removeIf(e -> {
            if (e.startsWith("//")) {
                CarpetSettings.LOG.warn("""
                        Found translation key starting with // while preparing translations!
                        Doing this is deprecated and may cause issues in later versions! Consider settings GSON to "lenient" mode and
                        using regular comments instead!
                        Translation key is '%s'""".formatted(e));
                return true;
            } else
                return false;
        });
        // Remove after deprecated settings api is removed
        addFallbacksTo(translations);
        translationMap = translations;
    }

    public static boolean isValidLanguage(String newValue)
    {
        // will put some validations for availble languages at some point
        return true;
    }
    
    // fallbacks for old rules that don't define rule descriptions or stuff in language files yet
    // to be removed when old settings system is removed and translation refactor is finished
    
    private static final Map<String, String> FALLBACKS = new HashMap<>();
    /**
     * @deprecated if you compile against this method I'll steal your kneecaps
     */
    @Deprecated(forRemoval = true)
    public static void registerFallbackTranslation(String key, String description) {
        FALLBACKS.put(key, description);
    }
    
    private static void addFallbacksTo(Map<String, String> translationMap) {
        FALLBACKS.forEach(translationMap::putIfAbsent);
    }
}
