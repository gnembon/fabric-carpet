package carpet.helpers;

import carpet.utils.Messenger;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class ScriptDownloader {

    public static String getScriptCode(String fullPath){//todo check if it splits properly
        return getScriptCode(fullPath.substring(0, fullPath.lastIndexOf('/')),fullPath.substring(fullPath.lastIndexOf('/')));
    }

    public static String getScriptCode(String path, String file){
        String link = "https://github.com/gnembon/scarpet/tree/master/programs/"+ path + file;
        URL appURL;
        HttpURLConnection http;
        try {
            appURL = new URL(link);
            http = (HttpURLConnection) appURL.openConnection();
        }
        catch (MalformedURLException e){
            throw new CommandException(new LiteralText("'"+ path + file + "' is not a valid path to a scarpet app"));
        }
        catch (IOException e){
            throw new CommandException(new LiteralText(e.toString()));
        }
        InputStream inputStream;
        try {
            inputStream = http.getInputStream();
            return getStringFromStream(inputStream);
        } catch (IOException e) {
            throw new CommandException(new LiteralText(e.toString()));
        }
    }

    public static int saveScriptToFile(String name, String code, MinecraftServer server, boolean globalSavePath){

        Path scriptPath;
        String location;

        if(globalSavePath){
            scriptPath = FabricLoader.getInstance().getConfigDir().resolve("carpet/scripts");
            location = "global script config folder";
        } else {
            scriptPath = server.getSavePath(WorldSavePath.ROOT).resolve("scripts");
            location = "world script folder";
        }

        Messenger.m(server.getCommandSource(),"gi Path to place file: '"+scriptPath + "/" + name + ".sc'");

        FileWriter fileWriter;
        try {
            if((new File(scriptPath + "/" + name + ".sc")).exists())
                throw new CommandException(new LiteralText(String.format("%s.sc already exists in %s folder, will not overwrite", name, location)));
            fileWriter = new FileWriter(scriptPath + "/" + name + ".sc");
            fileWriter.write(code);
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
        Messenger.m(server.getCommandSource(), "gi Successfuly created "+ name+ ".sc in " + location);
        return 1;
    }

    // ConvertStreamToString() Utility - we name it as getStringFromStream()
    private static String getStringFromStream(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            Writer writer = new StringWriter();

            char[] charBuffer = new char[2048];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                int counter;
                while ((counter = reader.read(charBuffer)) != -1) {
                    writer.write(charBuffer, 0, counter);
                }
            } finally {
                inputStream.close();
            }
            return writer.toString();
        } else {
            return "No Contents";
        }
    }
}
