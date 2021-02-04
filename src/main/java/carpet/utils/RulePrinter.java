package carpet.utils;

import carpet.CarpetServer;
import net.fabricmc.api.DedicatedServerModInitializer;
import java.lang.System;
/**
 * Runs only from GitHub Actions to generate the wiki
 * page with all Carpet rules on it.
 * It is here so it can be managed by the IDE
 *
 */
public class RulePrinter implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        CarpetServer.onGameStarted();
        CarpetServer.settingsManager.printAllRulesToLog(null);
        System.exit(0);
    }
}
