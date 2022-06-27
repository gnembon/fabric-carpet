package carpet.script;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import carpet.script.value.Value;

// WIP
public class ScriptServer {
    public final Map<Value, Value> systemGlobals = new ConcurrentHashMap<>();
}
