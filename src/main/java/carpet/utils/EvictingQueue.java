package carpet.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class EvictingQueue<K,V> extends LinkedHashMap<K,V>
{
     @Override
     protected boolean removeEldestEntry(Map.Entry<K, V> eldest)
     {
        return this.size() > 10; 
     }
}