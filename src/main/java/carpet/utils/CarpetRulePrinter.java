package carpet.utils;

import carpet.CarpetServer;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import joptsimple.util.PathProperties;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.System;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a command line interface to generate a dump with all rules
 * in a pretty markdown format to a specified file, with an optional 
 * category filter
 *
 */
public class CarpetRulePrinter implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        // When launching, we use the "--" separator to prevent the game rejecting to launch because of unknown options
        // Clear it in case it's present given else our option parser would also ignore them!
        String[] args = Arrays.stream(FabricLoader.getInstance().getLaunchArguments(true)).filter(opt -> !opt.equals("--")).toArray(String[]::new);

        // Prepare an OptionParser for our parameters
        OptionParser parser = new OptionParser();
        OptionSpec<Void> shouldDump = parser.accepts("carpetDumpRules");
        OptionSpec<Path> pathSpec = parser.accepts("dumpPath").withRequiredArg().withValuesConvertedBy(new PathConverter(PathProperties.WRITABLE));
        OptionSpec<String> filterSpec = parser.accepts("dumpFilter").withRequiredArg();
        parser.allowsUnrecognizedOptions(); // minecraft may need more stuff later that we don't want to special-case
        OptionSet options = parser.parse(args);
        // If our flag isn't set, continue regular launch
        if (!options.has(shouldDump)) return;


        Logger logger = LoggerFactory.getLogger("Carpet Rule Printer");
        logger.info("Starting in rule dump mode...");
        // at this point, onGameStarted() already ran given it as an entrypoint runs before
        PrintStream outputStream;
        try {
            Path path = options.valueOf(pathSpec).toAbsolutePath();
            logger.info("Printing rules to: " + path);
            Files.createDirectories(path.getParent());
            outputStream = new PrintStream(Files.newOutputStream(path));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        // Ensure translations fallbacks have been generated given we run before the validator that ensures that has.
        // Remove after removing old setting system, given there'll be no fallbacks
        Translations.updateLanguage();
        String filter = options.valueOf(filterSpec);
        if (filter != null) logger.info("Applying category filter: " + filter);
        CarpetServer.settingsManager.dumpAllRulesToStream(outputStream, filter);
        outputStream.close();
        logger.info("Rules have been printed");
        System.exit(0);
    }
}
