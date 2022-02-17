package carpet.utils;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.fakes.ServerPlayerEntityInterface;
import carpet.mixins.Style_translationMixin;
import carpet.mixins.TranslatableComponent_translationInterface;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Translations
{
    public static final String DEFAULT_LANGUAGE = "en_us";
    // language -> (translation key -> translated text)
    private static final Map<String, Map<String, String>> translationStorage = new LinkedHashMap<>();

    ///////////////////////////
    //          API          //
    ///////////////////////////

    /**
     * Return the preferred language of a CommandSourceStack if it's a player, otherwise return server's language.
     * See also {@link Translations#getPreferredLanguage(net.minecraft.world.entity.player.Player)}
     */
    public static String getPreferredLanguage(CommandSourceStack source)
    {
        if (source.getEntity() instanceof ServerPlayer serverPlayer)
        {
            return ((ServerPlayerEntityInterface) serverPlayer).getLanguage();
        }
        return getServerLanguage();
    }

    /**
     * Return the preferred language of a player, which is the language set in the player's client setting.
     * If the player is not a ServerPlayer, returns server's language.
     */
    public static String getPreferredLanguage(Player player)
    {
        String lang = null;
        if (player instanceof ServerPlayer serverPlayer)
        {
            // might be null if the client hasn't sent the setting packet
            lang = ((ServerPlayerEntityInterface) serverPlayer).getLanguage();
        }
        if (lang == null)
        {
            lang = getServerLanguage();
        }
        return lang;
    }

    /**
     * Translate the given translation key into a string.
     * Use {@link Messenger#tr} instead is suggested.
     * @param key the translation key
     * @return the translated text. If the translation entry doesn't exist, the key will be returned.
     */
    public static String tr(String key)
    {
        return tr(key, key);
    }

    /**
     * Translate the given translation key into a string.
     * Use {@link Messenger#tr} instead is suggested.
     * @param key the translation key
     * @param fallback the fallback text
     * @return the translated text. If the translation entry doesn't exist, the fallback text will be returned.
     */
    public static String tr(String key, String fallback)
    {
		return key2Translation(getServerLanguage(), key).orElse(fallback);
    }

    /**
     * Translated all {@link TranslatableComponent} recursively in a text component
     * Server's language will be used here
     * Language sent in player's client setting will be used
     * @param text The text to translate. A copy of the text will be made
     * @return A translated text component
     */
    public static BaseComponent tr(BaseComponent text)
    {
        return translate(text, getServerLanguage());
    }

    /**
     * Translated all {@link TranslatableComponent} recursively in a text component
     * The given player's client setting language is used as the target language.
     * Language sent in player's client setting will be used
     * @param text The text to translate. A copy of the text will be made
     * @param player The player that indicates the target language
     * @return A translated text component
     */
    public static BaseComponent tr(BaseComponent text, Player player)
    {
        return translate(text, getPreferredLanguage(player));
    }

    /**
     * Translated all {@link TranslatableComponent} recursively in a text component
     * The given CommandSourceStack's client setting language is used as the target language, if it's a player
     * Language sent in player's client setting will be used
     * @param text The text to translate. A copy of the text will be made
     * @param source The CommandSourceStack that indicates the target language
     * @return A translated text component
     */
    public static BaseComponent tr(BaseComponent text, CommandSourceStack source)
    {
        return translate(text, getPreferredLanguage(source));
    }

    /**
     * Returns if the given translation key exists.
     * Server's language is used as the target language here.
     * @param key The translation key to query
     */
    public static boolean hasTranslation(String key)
    {
		return key2Translation(getServerLanguage(), key).isPresent();
    }

    /**
     * Returns if the given translation key exists.
     * The given player's client setting language is used as the target language.
     * @param key The translation key to query
     * @param player The player that indicates the target language
     */
    public static boolean hasTranslation(String key, Player player)
    {
		return key2Translation(getPreferredLanguage(player), key).isPresent();
    }

    /**
     * Returns if the given translation key exists.
     * The given CommandSourceStack's client setting language is used as the target language, if it's a player
     * @param key The translation key to query
     * @param source The CommandSourceStack that indicates the target language
     */
    public static boolean hasTranslation(String key, CommandSourceStack source)
    {
		return key2Translation(getPreferredLanguage(source), key).isPresent();
    }

    /**
     * An util method for loading a language json file into a map.
     * @param path The path to the language json file in resources. e.g. "assets/carpet/lang/en_us.json"
     * @return A map storing all translation entries. Returns null if file reading or json parsing error.
     */
    @Nullable
    public static Map<String, String> getTranslationFromResourcePath(String path)
    {
        String dataJSON;
        try
        {
            dataJSON = IOUtils.toString(Objects.requireNonNull(Translations.class.getClassLoader().getResourceAsStream(path)), StandardCharsets.UTF_8);
        }
        catch (NullPointerException | IOException e)
        {
            return null;
        }
        try
        {
            Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
            return gson.fromJson(dataJSON, new TypeToken<LinkedHashMap<String, String>>() {}.getType());
        }
        catch (JsonParseException e)
        {
            CarpetSettings.LOG.error("[CM] Failed to load translation from {}", path);
            return null;
        }
    }

    /**
     * Return the language of the server ("en_us" by default or {@link CarpetSettings#language} if set).
     * It's used as the target translation language when no player context is given.
     * Rule value "none" will be mapped to "en_us".
     */
    public static String getServerLanguage()
    {
        return getRealLanguage(CarpetSettings.language);
    }

    /**
     * Returns if a language is valid.
     * A language is valid when it has any translation entry.
     */
    public static boolean isValidLanguage(String newLanguage)
    {
        return !getTranslations(getRealLanguage(newLanguage)).isEmpty();
    }

    /**
     * Manually add a translation entry into the translation storage.
     * @param lang the language the translation belongs to
     * @param translationKey the key of the translation
     * @param text the translated text
     * @param overwrite if sets to true, it will overwrite existed translation entry
     */
    public static void addEntry(String lang, String translationKey, String text, boolean overwrite)
    {
        Map<String, String> translations = getTranslations(lang);
        if (overwrite || !translations.containsKey(translationKey))
        {
            translations.put(translationKey, text);
        }
    }

    ///////////////////////////
    //     Implementation    //
    ///////////////////////////

    /**
     * Return a translation map of the given language
     * If it's the first time to access this language, load it from file and extensions
     */
    private static Map<String, String> getTranslations(String lang)
    {
        return translationStorage.computeIfAbsent(lang.toLowerCase(), l ->
        {
            Map<String, String> translations = new LinkedHashMap<>();
            Map<String, String> carpetTranslations = getTranslationFromResourcePath(String.format("assets/carpet/lang/%s.json", l));
            if (carpetTranslations != null) translations.putAll(carpetTranslations);

            for (CarpetExtension ext : CarpetServer.extensions)
            {
                Map<String, String> extMappings = ext.canHasTranslations(l);
                if (extMappings != null)
                {
                    extMappings.forEach((key, value) ->
                    {
                        if (!translations.containsKey(key)) translations.put(key, value);
                    });
                }
            }

            translations.entrySet().removeIf(e -> e.getKey().startsWith("//"));
			return translations;
        });
    }

    /**
     * Translated all {@link TranslatableComponent} recursively in a text component
     * A copy of the given text will be made to make sure the given text stays untouched
     * @param text The text to translate. A copy of the text will be made
     * @param lang The target language
     * @return A translated text component
     */
    private static BaseComponent translate(BaseComponent text, String lang)
    {
        return translateText((BaseComponent) text.copy(), lang);
    }

    /**
     * Maps "none" to "en_us" (default language)
     * Used as an adaptor of the carpet rule language since its default value is "none" and that means "en_us"
     */
	private static String getRealLanguage(String lang)
	{
		return (lang.equalsIgnoreCase("none") ? DEFAULT_LANGUAGE : lang).toLowerCase();
	}

    /**
     * key -> translated text
     * Translation text might contain arguments to be handled
     */
    public static Optional<String> key2Translation(String lang, String key)
    {
        return Optional.ofNullable(getTranslations(lang.toLowerCase()).get(key));
    }

    /**
     * Core translation applying logic
     * 1. It checks if the given text is a TranslatableComponent. If it is then
     *   1. Try to do translation using the text's key into the given language
     *   2. If the previous translation failed, use the default language ("en_us") to translate again
     *   3. If 1. or 2. succeeded, handle all arguments in the translated text and create a translated text component
     * 2. If the text has a hover text, translate the hover text
     * 3. Translate all siblings of the text
     */
    private static BaseComponent translateText(BaseComponent text, @NotNull String lang)
    {
        if (text instanceof TranslatableComponent translatableComponent)
        {
            Optional<String> optionalString = key2Translation(lang, translatableComponent.getKey());
            if (optionalString.isEmpty() && !lang.equals(DEFAULT_LANGUAGE))
            {
                optionalString = key2Translation(DEFAULT_LANGUAGE, translatableComponent.getKey());
            }
            if (optionalString.isPresent())
            {
                BaseComponent origin = text;
				String translated = optionalString.get();
                TranslatableComponent customTranslatableComponent = new TranslatableComponent(translated, translatableComponent.getArgs());
                try
                {
                    List<FormattedText> elements = new ArrayList<>();
                    ((TranslatableComponent_translationInterface) customTranslatableComponent).invokeDecomposeTemplate(translated, elements::add);
                    text = Messenger.c(elements.stream().map(stringVisitable ->
                    {
                        if (stringVisitable instanceof BaseComponent)
                        {
                            return (BaseComponent) stringVisitable;
                        }
                        return Messenger.s(stringVisitable.getString());
                    }).toArray());
                }
                catch (TranslatableFormatException e)
                {
                    CarpetSettings.LOG.warn("[CM] Invalid translation string: {}", translated);
                    text = Messenger.s(translated);
                }

                // migrating text data
                text.getSiblings().addAll(origin.getSiblings());
                text.setStyle(origin.getStyle());
            }
            else
            {
                CarpetSettings.LOG.warn("[CM] Unknown translation key {}", translatableComponent.getKey());
            }
        }

        // translate hover text
        HoverEvent hoverEvent = ((Style_translationMixin) text.getStyle()).getHoverEventField$CM();
        if (hoverEvent != null)
        {
            Object hoverText = hoverEvent.getValue(hoverEvent.getAction());
            if (hoverEvent.getAction() == HoverEvent.Action.SHOW_TEXT && hoverText instanceof BaseComponent)
            {
                text.setStyle(text.getStyle().withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translateText((BaseComponent) hoverText, lang))));
            }
        }

        // translate sibling texts
        List<Component> siblings = text.getSiblings();
        for (int i = 0; i < siblings.size(); i++)
        {
            siblings.set(i, translateText((BaseComponent) siblings.get(i), lang));
        }
        return text;
    }
}
