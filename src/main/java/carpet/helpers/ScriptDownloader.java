package carpet.helpers;

import carpet.utils.Messenger;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Pair;
import net.minecraft.util.WorldSavePath;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    public static int downloadScript(CommandContext<ServerCommandSource> cc, String path, boolean global){
        String code = getScriptCode(path);
        return saveScriptToFile(path, code, cc.getSource().getMinecraftServer(),global);
    }

    public static String getScriptCode(String path){
        try {
            String link = "https://raw.githubusercontent.com/gnembon/scarpet/master/programs/"+ path;
            URL appURL = new URL(link);
            HttpURLConnection http = (HttpURLConnection) appURL.openConnection();
            return getStringFromStream((InputStream) http.getContent());
        } catch (IOException e){//todo add checks to distinguish between incorrect file path or change in scarpet repo structure
            throw new CommandException(new LiteralText("'"+ path + "' is not a valid path to a scarpet app"));
        }
    }

    public static int saveScriptToFile(String name, String code, MinecraftServer server,boolean globalSavePath){
        Path scriptPath;
        String location;
        if(globalSavePath){
            scriptPath = FabricLoader.getInstance().getConfigDir().resolve("carpet/scripts");
            location = "global script config folder";
        } else {
            scriptPath = server.getSavePath(WorldSavePath.ROOT).resolve("scripts");
            location = "world script folder";
        }
        Messenger.m(server.getCommandSource(), "gi Path to place file: '"+ location + "'");
        Messenger.m(server.getCommandSource(), "gi Path to place file: '"+ scriptPath + "'");

        FileWriter fileWriter;
        File file = new File(scriptPath.toFile(), name);
        try {
            if(file.createNewFile())
                throw new CommandException(new LiteralText(String.format("%s already exists in %s, will not overwrite", name, location)));
            Runtime.getRuntime().exec("explorer.exe /select, " + file.getAbsolutePath());//todo remove after debugging and finishing saving to disk
            fileWriter = new FileWriter(file);
            fileWriter.write(code);
            fileWriter.close();
            Messenger.m(server.getCommandSource(), file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
        Messenger.m(server.getCommandSource(), "gi Successfully created "+ name + " in " + location);
        return 1;
    }

    // converting stream to string
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

    /** Updates local copy of scarpet repo, giving an error otherwise which ought to be with immediacy reported to fabric-carpet
     * devs, in particular Ghoulboy if possible as he wrote this code.
     *
     * @author Ghoulboy
     */

    public static void updateLocalRepoStructure(){
        try{
            localScarpetRepoStructure = updateLocalRepoStructure("");
        } catch (IOException | IllegalStateException exc){//should not happen as long as repo name stays the same
            System.out.println("ERROR: ScriptDownloader#scarpetRepoLink variable is out of date, please update fabric-carpet and contact its devs!");
            throw new CommandException(new LiteralText("Internal scarpet app store repo structure changed, please contact fabric-carpet developers immediately, enclosing a copy of the server log!"));
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

    public static Set<String> getFileFolderNames(String path) throws IOException{
        Set<String> directoryNames = new HashSet<>();

        String link = scarpetRepoLink + path;

        URL appURL = new URL(link);

        String response = ScriptDownloader.getStringFromStream(appURL.openStream());

        JsonArray files = new JsonParser().parse(response).getAsJsonArray();

        for(JsonElement je : files){
            JsonObject jo = je.getAsJsonObject();
            directoryNames.add((jo.get("name").getAsString() + jo.get("type").getAsString()).equals("dir") ? "/":"");//if directory name then we wanna add '/' automatically
        }

        return directoryNames;
    }

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
