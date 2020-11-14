package carpet.utils;

import carpet.CarpetServer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import java.lang.System;
/**
 * Runs only from GitHub Actions to generate the wiki
 * page with all Carpet rules on it.
 * It is here so it can be managed by the IDE
 * 
 * @author altrisi
 *
 */
public class RulePrinter implements PreLaunchEntrypoint {

	@Override
	public void onPreLaunch() {
		CarpetServer.onGameStarted();
        CarpetServer.settingsManager.printAllRulesToLog(null);
        System.exit(0);
    }
}