package carpet.helpers;

import carpet.utils.Messenger;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Pair;
import net.minecraft.util.WorldSavePath;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A class used to save scarpet app store scripts to disk
 */

public class ScriptDownloader {

    /** A local copy of the scarpet repo's file structure, to avoid multiple queries to github.com while typing out the
     * {@code /script download} command and getting the suggestions. Therefore, we save the file structure at the beginning
     * of the server's starting, and update it with {@link ScriptDownloader#updateLocalRepoStructure()} if we get an error
     * in the query, thereby allowing us to re-enter the command and get the correct app (unless of course the inputted
     * path is itself wrong)
     */
    public static Map<String, Map> localScarpetRepoStructure = new HashMap<>();

    /** This is the link to the scarpet app repo from the github api, so if that ever changes then this variable would
     * have to change as well to match.
     */
    private static final String scarpetRepoLink = "https://api.github.com/repos/gnembon/scarpet/contents/programs/";

    /** A simple shorthand for calling the {@link ScriptDownloader#getScriptCode} and {@link ScriptDownloader#saveScriptToFile}
     * methods to avoid repeating code and so it makes more sense what it's exactly doing.
     *
     * @param path The user-inputted path to the script
     * @param global Whether or not we wanna save it to the local scripts folder or in the global config folder
     * @return {@code 1} if we succesfully saved the script, {@code 0} otherwise
     */

    public static int downloadScript(CommandContext<ServerCommandSource> cc, String path, boolean global){
        String code = getScriptCode(path);
        return saveScriptToFile(path, code, cc, global);
    }

    /** Gets the code once the user inputs the command. The code isn't saved in {@link ScriptDownloader#localScarpetRepoStructure}
     * as the scarpet repo is very large and may get much larger in the future, and that may cause RAM issues if we have
     * the entire thing saved in memory.
     *
     * @param path The user inputted path to the scarpet script
     * @return the HTML request from the path program using {@link ScriptDownloader#getStringFromStream} method to convert
     * HTML response into a string
     */
    public static String getScriptCode(String path){
        try {
            String link = "https://raw.githubusercontent.com/gnembon/scarpet/master/programs/"+ path.replace(" ", "%20");
            URL appURL = new URL(link);
            HttpURLConnection http = (HttpURLConnection) appURL.openConnection();
            return getStringFromStream((InputStream) http.getContent());
        } catch (IOException e){//todo add checks to distinguish between incorrect file path or change in scarpet repo structure
            throw new CommandException(new LiteralText("'"+ path + "' is not a valid path to a scarpet app"));
        }
    }

    public static int saveScriptToFile(String name, String code, CommandContext<ServerCommandSource> cc, boolean globalSavePath){
        Path scriptLocation;
        String scriptPath;
        String location;
        if(globalSavePath){
            scriptLocation = FabricLoader.getInstance().getConfigDir().resolve("carpet/scripts/appstore");
            location = "global script config folder";
        } else {
            scriptLocation = cc.getSource().getMinecraftServer().getSavePath(WorldSavePath.ROOT).resolve("scripts/appstore");
            location = "world scripts folder";
        }
        try {
            scriptPath = scriptLocation.toAbsolutePath() + "/" + name;
            scriptLocation = Paths.get(scriptPath);
            String scriptFolderPath = scriptPath.substring(0, scriptPath.lastIndexOf('/'));//folder location without file name
            Messenger.m(cc.getSource(), "gi Script folder path: "+scriptFolderPath);
            Path scriptFolderLocation = Paths.get(scriptFolderPath);
            if (!Files.exists(scriptFolderLocation)) {
                Files.createDirectories(scriptFolderLocation);
            }

            if(Files.exists(scriptLocation)){
                Messenger.m(cc.getSource(), String.format("gi Note: overwriting existing file '%s'", name));
            }

            Messenger.m(cc.getSource(), "gi Placing script in " + location);

            FileWriter fileWriter = new FileWriter(scriptPath);
            fileWriter.write(code);
            fileWriter.close();
            Messenger.m(cc.getSource(), "gi "+ scriptFolderPath);
        } catch (IOException e) {
            Messenger.m(cc.getSource(), "r Error in downloading script");
            return 0;
        }
        Messenger.m(cc.getSource(), "gi Successfully created "+ name + " in " + location);
        return 1;
    }

    /** Returns the string from the inputstream gotten from the html request
     * Thanks to App Shah in <a href="https://crunchify.com/in-java-how-to-read-github-file-contents-using-httpurlconnection-convert-stream-to-string-utility/">this post</a>
     * for this code.
     *
     * @return the string input from the InputStream
     * @throws IOException if an I/O error occurs
     */
    public static String getStringFromStream(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            Writer stringWriter = new StringWriter();

            char[] charBuffer = new char[2048];
            try{
                Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                int counter;
                while ((counter = reader.read(charBuffer)) != -1) {
                    stringWriter.write(charBuffer, 0, counter);
                }
            } finally {
                inputStream.close();
            }
            return stringWriter.toString();
        } else {
            return "";
        }
    }

    /** Updates local copy of scarpet repo, giving an error otherwise which ought to be reported to fabric-carpet devs,
     * in particular Ghoulboy if possible as he wrote this code.
     *
     * @author Ghoulboy
     */

    public static void updateLocalRepoStructure(){
        try{
            localScarpetRepoStructure = updateLocalRepoStructure("");
        } catch (IOException | IllegalStateException exc){//should not happen as long as repo name stays the same
            System.out.println("ERROR: ScriptDownloader#scarpetRepoLink variable is out of date, please update your carpet version, and if the problem persists please submit a bug report here: https://github.com/gnembon/fabric-carpet/issues/new");
            throw new CommandException(new LiteralText("Internal scarpet app store repo structure changed, please contact fabric-carpet developers, enclosing a copy of the server log!"));
        }
    }

    /** A DFS to save the file structure (not the code) in the scarpet repo, called at server start and whenever running
     * the command {@code /script download} command  causes an oopsie, so the internal scarpet repo structure is changed
     * and we therefore need to update it. We don't save the code here in case it needs updating from the last time, and
     * also not to slow down the search for
     *
     * @param currentPath The current path down which we are looking for code.
     * @return The scarpet repo structure as a map which you navigate down, so you don't need to query to github API
     * multiple times
     * @throws IOException only if {@link ScriptDownloader#scarpetRepoLink} structure is out of date, i.e scarpet internal
     * repo structure has changed
     */

    public static Map<String, Map> updateLocalRepoStructure(String currentPath) throws IOException{

        Map<String, Map> ret = new HashMap<>();

        String queryPath = scarpetRepoLink + currentPath;

        URL appURL = new URL(queryPath);

        String response = ScriptDownloader.getStringFromStream(appURL.openStream());

        JsonArray files = new JsonParser().parse(response).getAsJsonArray();

        for(JsonElement je : files){
            JsonObject jo = je.getAsJsonObject();
            String name = jo.get("name").getAsString();
            String filePath = jo.get("path").getAsString().substring(9).replace(" ", "%20");

            if (jo.get("type").getAsString().equals("dir") && !name.contains("shared")) {//cos that may be a directory for shared data, so we don't wanna search there
                ret.put(name, updateLocalRepoStructure(filePath));
            } else if (name.matches("(\\w+\\.scl?)")){
                ret.put(name, null);
            }
        }

        return ret;
    }

    /** This method searches for valid file names from the user-inputted string, e.g if the user has thus far typed
     * {@code survival/a} then it will return all the files in the {@code survival} directory of the scarpet repo (and
     * will automatically highlight those starting with a), and the string {@code survival/} as the current most valid path.
     *
     * @param currentPath The path down which we want to search for files
     * @return A pair of the current valid path, as well as the set of all the file/directory names at the end of that path
     */
    public static Pair<String, Set<String>> fileNamesFromPath(String currentPath){
        String[] path = currentPath.split("/");

        Map<String, Map> currentLookedAtFiles = localScarpetRepoStructure;
        StringBuilder currentValidPath = new StringBuilder();


        for(String folder : path){
            if(!currentLookedAtFiles.containsKey(folder))
                return new Pair<>(currentValidPath.toString(), currentLookedAtFiles.keySet());

            currentValidPath.append(folder).append("/");


            Map newFolder = currentLookedAtFiles.get(folder);

            if(newFolder == null)
                return new Pair<>(currentValidPath.toString(), currentLookedAtFiles.keySet());

            currentLookedAtFiles = newFolder;
        }

        return new Pair<>(currentValidPath.toString(), currentLookedAtFiles.keySet());
    }
}
