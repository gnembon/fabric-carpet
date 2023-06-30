package carpet.script;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import carpet.script.value.Value;

// WIP
public abstract class ScriptServer
{
    public final Map<Value, Value> systemGlobals = new ConcurrentHashMap<>();

    public abstract Path resolveResource(String suffix);
}
