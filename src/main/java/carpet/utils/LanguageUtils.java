package carpet.utils;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LanguageUtils
{
    private static Map<String, String> translationMap;

    public static String currentLanguage;

    public static String translate(String translationKey)
    {
        if (translationMap == null)
        {
            return translationKey;
        }
        return translationMap.getOrDefault(translationKey, translationKey);
    }

    public static String translateOrDefault(String translationKey, String defaultValue)
    {
        if (translationMap == null)
        {
            return defaultValue;
        }
        return translationMap.getOrDefault(translationKey, defaultValue);
    }

    public static String getRuleNameKey(String rule)
    {
        return String.format("carpet.rule.%s.name", rule);
    }

    public static String getRuleDescKey(String rule)
    {
        return String.format("carpet.rule.%s.desc", rule);
    }

    public static ImmutableList<String> getRuleExtras(String rule, ImmutableList<String> defaultList)
    {
        if (translationMap == null) return defaultList;
        String keyBase = String.format("carpet.rule.%s.extra.", rule);
        List<String> extras = new ArrayList<>();
        int i = 0;
        do {
            String keyExtra = keyBase + i;
            String value = translationMap.get(keyExtra);
            System.out.println(keyExtra);
            System.out.println(value);
            if (value != null && !value.equals(""))
            {
                extras.add(value);
            }
            i++;
        }
        while (translationMap.containsKey(keyBase + (i - 1)));
        return (extras.isEmpty()) ? defaultList : ImmutableList.copyOf(extras);
    }

    public static boolean updateLanguage(ServerCommandSource source, String language)
    {
        if (language.equals("none"))
        {
            currentLanguage = language;
            translationMap = null;
            source.sendFeedback(new LiteralText("§aSucceeded: Switched to default"), false);
            return true;
        }

        String langJs;
        try{
            langJs = IOUtils.toString(
                    Objects.requireNonNull(LanguageUtils.class.getClassLoader().getResourceAsStream(String.format("assets/carpet/lang/%s.json", language))),
                    StandardCharsets.UTF_8);
        } catch (NullPointerException | IOException e) {
            source.sendFeedback(new LiteralText("§4No such language file."), false);
            return false;
        }
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> map1 = gson.fromJson(langJs, type);
        if(map1.isEmpty()){
            source.sendFeedback(new LiteralText("§6The current file has no contents"), false);
        }
        currentLanguage = language;
        translationMap = map1;
        source.sendFeedback(new LiteralText("§aSucceeded: Switched to " + language), false);
        return true;
    }


}
