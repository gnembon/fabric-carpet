package carpet.script.utils;

import carpet.CarpetServer;
import carpet.script.CarpetScriptHost;
import carpet.script.CarpetScriptServer;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.MapValue;
import carpet.script.value.Value;
import carpet.settings.ParsedRule;
import carpet.settings.Validator;
import carpet.utils.Messenger;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.WorldSavePath;
import org.apache.commons.lang3.tuple.Triple;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * A class used to save scarpet app store scripts to disk
 */
public class AppStoreManager
{
    /** A local copy of the scarpet repo's file structure, to avoid multiple queries to github.com while typing out the
     * {@code /script download} command and getting the suggestions.
     */
    public static final StoreNode appStoreRoot = StoreNode.folder(null, "");

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

    public static class StoreNode
    {
        public String name;
        public StoreNode parent;
        public Map<String, StoreNode> children;
        public boolean sealed;
        public String value;
        public static StoreNode folder(StoreNode parent, String name)
        {
            StoreNode node = new StoreNode(parent, name);
            node.children = new HashMap<>();
            node.value = null;
            node.sealed = false;
            return node;
        }

        public static StoreNode scriptFile(StoreNode parent, String name, String value)
        {
            StoreNode node = new StoreNode(parent, name);
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
        private StoreNode(StoreNode parent, String name)
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
            String response;
            try
            {
                response = AppStoreManager.getStringFromStream(queryPath);
            }
            catch (IOException e)
            {
                throw new IOException("Problems fetching "+queryPath+": "+e);
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
                else// if (name.matches("(\\w+\\.scl?)"))
                {
                    String value = jo.get("download_url").getAsString();
                    children.put(name, scriptFile(this, name, value));
                }
            }
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
                if (name.endsWith(".sc"))
                    return Collections.singletonList(getPath());
                return Collections.emptyList();
            }
            fillChildren();
            String prefix = getPath();
            return children.values().stream().
                    filter(n -> (!n.isLeaf() || n.name.endsWith(".sc"))).
                    map(s -> prefix+s.pathElement().replaceAll("/$", "")).
                    collect(Collectors.toList());
        }

        public StoreNode drillDown(String pathElement) throws IOException
        {
            if (isLeaf()) throw new IOException(pathElement+" is not a folder");
            fillChildren();
            if (!children.containsKey(pathElement)) throw new IOException("Folder "+pathElement+" is not present");
            return children.get(pathElement);
        }

        public String getValue(String file) throws IOException
        {
            StoreNode leaf = drillDown(file);
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
        StoreNode appKiosk = appStoreRoot;
        for(String pathElement : path)
        {
            if (appKiosk.cannotContinueFor(pathElement)) break;
            appKiosk = appKiosk.children.get(pathElement);
        }
        List<String> filteredSuggestions = appKiosk.createPathSuggestions().stream().filter(s -> s.startsWith(currentPath)).collect(Collectors.toList());
        if (filteredSuggestions.size() == 1) {
            if (!appKiosk.isLeaf())
                return suggestionsFromPath(filteredSuggestions.get(0)); // Start suggesting directory contents
        }
        return filteredSuggestions;
    }



    /** Downloads script and saves it to appropriate place.
     *
     * @param path The user-inputted path to the script
     * @return {@code 1} if we succesfully saved the script, {@code 0} otherwise
     */

    public static int downloadScript(ServerCommandSource source, String path)
    {
        Triple<String, String, StoreNode> nodeInfo = getFileNode(path);
        String code;
        try
        {
            code = getStringFromStream(nodeInfo.getMiddle());
        }
        catch (IOException e)
        {
            throw new CommandException(Messenger.c("rb Failed to obtain app file content: "+e.getMessage()));
        }
        if (!saveScriptToFile(source, path, nodeInfo.getLeft(), code, false)) return 0;
        boolean success = CarpetServer.scriptServer.addScriptHost(source, nodeInfo.getLeft().replaceFirst("\\.sc$", ""), null, true, false, false, nodeInfo.getRight());
        return success?1:0;
    }

    /** Gets the code once the user inputs the command.
     *
     * @param appPath The user inputted path to the scarpet script
     * @return Pair of app file name and content
     */
    public static Triple<String, String, StoreNode> getFileNode(String appPath)
    {
        return getFileNodeFrom(appStoreRoot, appPath);
    }

    public static Triple<String, String, StoreNode> getFileNodeFrom(StoreNode start, String appPath)
    {
        String[] path = appPath.split("/");
        StoreNode appKiosk = start;
        try
        {
            for(String pathElement : Arrays.copyOfRange(path, 0, path.length-1))
                appKiosk = appKiosk.drillDown(pathElement);
            String appName = path[path.length-1];
            appKiosk.getValue(appName);
            return Triple.of(appName, appKiosk.getValue(appName), appKiosk);
        }
        catch (IOException e)
        {
            throw new CommandException(Messenger.c("rb '"+ appPath + "' is not a valid path to a scarpet app: "+e.getMessage()));
        }
    }


    public static boolean saveScriptToFile(ServerCommandSource source, String path, String appFileName, String code, boolean globalSavePath){
        String  scriptPath;
        String location;
        if(globalSavePath && !source.getMinecraftServer().isDedicated())
        { //cos config folder only is in clients
            scriptPath = FabricLoader.getInstance().getConfigDir().resolve("carpet/scripts/appstore").toAbsolutePath()+"/"+path;
            location = "global script config folder";
        }
        else
        {
            scriptPath = source.getMinecraftServer().getSavePath(WorldSavePath.ROOT).resolve("scripts").toAbsolutePath()+"/"+appFileName;
            location = "world scripts folder";
        }
        try
        {
            Path scriptLocation = Paths.get(scriptPath);
            Files.createDirectories(scriptLocation.getParent());
            if(Files.exists(scriptLocation))
                Messenger.m(source, String.format("gi Note: replaced existing app '%s'", appFileName));
            FileWriter fileWriter = new FileWriter(scriptPath);
            fileWriter.write(code);
            fileWriter.close();
        }
        catch (IOException e)
        {
            Messenger.m(source, "r Error in downloading app: "+e.getMessage());
            return false;
        }
        return true;
    }

    /** Returns the string from the inputstream gotten from the html request.
     * Thanks to App Shah in <a href="https://crunchify.com/in-java-how-to-read-github-file-contents-using-httpurlconnection-convert-stream-to-string-utility/">this post</a>
     * for this code.
     *
     * @return the string input from the InputStream
     * @throws IOException if an I/O error occurs
     */
    public static String getStringFromStream(String url) throws IOException
    {
        InputStream inputStream = new URL(url).openStream();
        if (inputStream == null)
            throw new IOException("No app to be found on the appstore");

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

    public static void writeUrlToFile(String url, Path destination) throws IOException
    {
        try (final InputStream in = new URL(url).openStream())
        {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void addResource(CarpetScriptHost carpetScriptHost, StoreNode storeSource, Value resource)
    {
        if (!(resource instanceof MapValue))
            throw new InternalExpressionException("This is not a valid resource map: "+resource.getString());
        Map<String, Value> resourceMap = ((MapValue) resource).getMap().entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getString(), Map.Entry::getValue));
        for (String key: ImmutableList.of("source", "target", "type"))
            if (!resourceMap.containsKey(key)) throw new InternalExpressionException("Missing '"+key+"' field in resource descriptor: "+resource.getString());
        String source = resourceMap.get("source").getString();
        String target = resourceMap.get("target").getString();
        boolean shared = resourceMap.getOrDefault("shared", Value.FALSE).getBoolean();
        String type = resourceMap.get("type").getString();
        String contentUrl;
        if (type.equalsIgnoreCase("url"))
        {
            contentUrl = source;
        }
        else if (type.equalsIgnoreCase("store"))
        {
            Triple<String, String, StoreNode> nodeInfo = getFileNode(source);
            contentUrl = nodeInfo.getMiddle();
        }
        else if (type.equalsIgnoreCase("app"))
        {
            Triple<String, String, StoreNode> nodeInfo = getFileNodeFrom(storeSource, source);
            contentUrl = nodeInfo.getMiddle();
        }
        else
        {
            throw new InternalExpressionException("Incorrect source type, expecting 'url', 'store' or 'app', got "+type);
        }
        if (!carpetScriptHost.applyActionForResource(target, shared, p -> {
            try {
                writeUrlToFile(contentUrl, p);
            } catch (IOException e) {
                throw new InternalExpressionException("Unable to write resource "+target+": "+e.getMessage());
            }
        }))
        {
            throw new InternalExpressionException("Unable to write resource "+target);
        }
        CarpetScriptServer.LOG.info("Downloaded resource "+target+" from "+contentUrl);
    }
}
