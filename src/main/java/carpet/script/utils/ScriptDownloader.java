package carpet.script.utils;

import carpet.script.CarpetScriptServer;
import carpet.settings.ParsedRule;
import carpet.settings.Validator;
import carpet.utils.Messenger;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.WorldSavePath;
import org.apache.commons.lang3.tuple.Pair;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * A class used to save scarpet app store scripts to disk
 */
public class ScriptDownloader
{
    /** A local copy of the scarpet repo's file structure, to avoid multiple queries to github.com while typing out the
     * {@code /script download} command and getting the suggestions.
     */
    public static final PathNode appStoreRoot = PathNode.folder(null, "");

    /** This is the base link to the scarpet app repo from the github api.
     */
    private static String scarpetRepoLink = "https://api.github.com/repos/gnembon/scarpet/contents/programs/";

    public static class ScarpetAppStoreValidator extends Validator<String>
    {
        @Override public String validate(ServerCommandSource source, ParsedRule<String> currentRule, String newValue, String string)
        {
            appStoreRoot.sealed = false;
            appStoreRoot.children = new HashMap<>();
            if (newValue.equalsIgnoreCase("none"))
            {
                scarpetRepoLink = null;
                return newValue;
            }
            if (newValue.endsWith("/")) newValue = newValue.replaceAll("/$", "");
            scarpetRepoLink = "https://api.github.com/repos/"+newValue+"/";
            return newValue;
        }
        @Override
        public String description() { return "Appstore link should point to a valid github repository";}
    }

    public static class PathNode
    {
        public String name;
        public PathNode parent;
        public Map<String, PathNode> children;
        public boolean sealed;
        public String value;
        public static PathNode folder(PathNode parent, String name)
        {
            PathNode node = new PathNode(parent, name);
            node.children = new HashMap<>();
            node.value = null;
            node.sealed = false;
            return node;
        }

        public static PathNode scriptFile(PathNode parent, String name, String value)
        {
            PathNode node = new PathNode(parent, name);
            node.children = null;
            node.value = value;
            node.sealed = true;
            return node;
        }

        public boolean isLeaf()
        {
            return value != null;
        }
        public String pathElement()
        {
            return name+(isLeaf()?"":"/");
        }
        public String getPath()
        {
            return createPrePath().toString();
        }
        private StringBuilder createPrePath()
        {
            return this == appStoreRoot ? new StringBuilder() : parent.createPrePath().append(pathElement());
        }
        private PathNode(PathNode parent, String name)
        {
            this.parent = parent;
            this.name = name;
            this.sealed = false;
        }

        public void fillChildren() throws IOException
        {
            if (sealed) return;
            if (scarpetRepoLink == null) throw new IOException("Accessing scarpet app repo is disabled");

            String queryPath = scarpetRepoLink + getPath();
            CarpetScriptServer.LOG.error("Fetching: "+queryPath);
            String response;
            try
            {
                URL appURL = new URL(queryPath);
                response = ScriptDownloader.getStringFromStream(appURL.openStream());
            }
            catch (IOException e)
            {
                CarpetScriptServer.LOG.error("Problems fetching: "+e);
                sealed = true;
                return;
            }
            JsonArray files = new JsonParser().parse(response).getAsJsonArray();
            for(JsonElement je : files)
            {
                JsonObject jo = je.getAsJsonObject();
                String name = jo.get("name").getAsString();
                if (jo.get("type").getAsString().equals("dir"))
                {
                    children.put(name, folder(this, name));
                }
                else if (name.matches("(\\w+\\.scl?)"))
                {
                    String value = jo.get("download_url").getAsString();
                    children.put(name, scriptFile(this, name, value));
                }
            }
            CarpetScriptServer.LOG.error("nodes for "+ getPath()+" are "+children.values().stream().map(PathNode::pathElement).collect(Collectors.joining(",")));
            sealed = true;
        }

        /**
         * Returns true if doing down the directory structire cannot continue since the matching element is either a leaf or
         * a string not matching of any node.
         * @param pathElement
         * @return
         */
        public boolean cannotContinueFor(String pathElement) throws IOException
        {
            if (isLeaf()) return true;
            fillChildren();
            return !children.containsKey(pathElement);
        }

        public List<String> createPathSuggestions() throws IOException
        {
            if (isLeaf())
            {
                CarpetScriptServer.LOG.error("suggestions for leaf :"+name);
                return Collections.singletonList(getPath());
            }
            fillChildren();
            String prefix = getPath();
            CarpetScriptServer.LOG.error("suggestions for node :"+name);
            return children.values().stream().map(s -> prefix+s.pathElement().replaceAll("/$", "")).collect(Collectors.toList());
        }

        public PathNode drillDown(String pathElement) throws IOException
        {
            if (isLeaf()) throw new IOException(pathElement+" is not a folder");
            fillChildren();
            if (!children.containsKey(pathElement)) throw new IOException("Folder "+pathElement+" is not present");
            return children.get(pathElement);
        }

        public String getValue(String file) throws IOException
        {
            PathNode leaf = drillDown(file);
            if (!leaf.isLeaf()) throw new IOException(file+" is not a file");
            return leaf.value;
        }
    }

    /** This method searches for valid file names from the user-inputted string, e.g if the user has thus far typed
     * {@code survival/a} then it will return all the files in the {@code survival} directory of the scarpet repo (and
     * will automatically highlight those starting with a), and the string {@code survival/} as the current most valid path.
     *
     * @param currentPath The path down which we want to search for files
     * @return A pair of the current valid path, as well as the set of all the file/directory names at the end of that path
     */
    public static List<String> suggestionsFromPath(String currentPath) throws IOException
    {
        String[] path = currentPath.split("/");
        PathNode appKiosk = appStoreRoot;
        for(String pathElement : path)
        {
            if (appKiosk.cannotContinueFor(pathElement)) return appKiosk.createPathSuggestions();
            appKiosk = appKiosk.children.get(pathElement);
        }
        return appKiosk.createPathSuggestions();
    }



    /** A simple shorthand for calling the {@link ScriptDownloader#getScriptCode} and {@link ScriptDownloader#saveScriptToFile}
     * methods to avoid repeating code and so it makes more sense what it's exactly doing.
     *
     * @param path The user-inputted path to the script
     * @param global Whether or not we wanna save it to the local scripts folder or in the global config folder
     * @return {@code 1} if we succesfully saved the script, {@code 0} otherwise
     */

    public static int downloadScript(CommandContext<ServerCommandSource> cc, String path, boolean global)
    {
        Pair<String,String> code = getScriptCode(path);
        return saveScriptToFile(path, code.getLeft(), code.getRight(), cc, global);
    }

    /** Gets the code once the user inputs the command.
     *
     * @param appPath The user inputted path to the scarpet script
     * @return Pair of app file name and content
     */
    public static Pair<String,String> getScriptCode(String appPath)
    {
        String[] path = appPath.split("/");
        PathNode appKiosk = appStoreRoot;
        try
        {
            for(String pathElement : Arrays.copyOfRange(path, 0, path.length-1))
                appKiosk = appKiosk.drillDown(pathElement);
            String appName = path[path.length-1];
            URL appURL = new URL(appKiosk.getValue(appName));
            HttpURLConnection http = (HttpURLConnection) appURL.openConnection();
            return Pair.of(appName, getStringFromStream((InputStream) http.getContent()));
        }
        catch (IOException e)
        {
            throw new CommandException(Messenger.c("rb '"+ appPath + "' is not a valid path to a scarpet app: "+e.getMessage()));
        }
    }

    public static int saveScriptToFile(String path, String appFileName, String code, CommandContext<ServerCommandSource> cc, boolean globalSavePath){
        String  scriptPath;
        String message;
        if(globalSavePath && !cc.getSource().getMinecraftServer().isDedicated())
        { //cos config folder only is in clients
            scriptPath = FabricLoader.getInstance().getConfigDir().resolve("carpet/scripts/appstore").toAbsolutePath()+"/"+path;
            message = "global script config folder";
        }
        else
        {
            scriptPath = cc.getSource().getMinecraftServer().getSavePath(WorldSavePath.ROOT).resolve("scripts").toAbsolutePath()+"/"+appFileName;
            message = "world scripts folder";
        }
        try
        {
            Path scriptLocation = Paths.get(scriptPath);
            Path scriptFolderLocation = Paths.get(scriptPath.substring(0, scriptPath.lastIndexOf('/')));//folder location without file name
            if (!Files.exists(scriptFolderLocation))
                Files.createDirectories(scriptFolderLocation);

            if(Files.exists(scriptLocation))
                Messenger.m(cc.getSource(), String.format("gi Note: overwriting existing file '%s'", appFileName));


            Messenger.m(cc.getSource(), "gi Placing script in " + message);

            FileWriter fileWriter = new FileWriter(scriptPath);
            fileWriter.write(code);
            fileWriter.close();
        }
        catch (IOException e)
        {
            Messenger.m(cc.getSource(), "r Error in downloading script");
            return 0;
        }
        Messenger.m(cc.getSource(), "gi Successfully created "+ appFileName + " in " + message);
        return 1;
    }

    /** Returns the string from the inputstream gotten from the html request.
     * Thanks to App Shah in <a href="https://crunchify.com/in-java-how-to-read-github-file-contents-using-httpurlconnection-convert-stream-to-string-utility/">this post</a>
     * for this code.
     *
     * @return the string input from the InputStream
     * @throws IOException if an I/O error occurs
     */
    public static String getStringFromStream(InputStream inputStream) throws IOException
    {
        if (inputStream != null)
        {
            Writer stringWriter = new StringWriter();

            char[] charBuffer = new char[2048];
            try
            {
                Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                int counter;
                while ((counter = reader.read(charBuffer)) != -1)
                    stringWriter.write(charBuffer, 0, counter);

            }
            finally
            {
                inputStream.close();
            }
            return stringWriter.toString();
        }
        else
        {
            return "";
        }
    }
}
