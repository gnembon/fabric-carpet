package carpet.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class EvictingQueue<K> extends LinkedHashMap<K,Integer>
{
    public void put(K key)
    {
        super.put(key, 1);
    }

    @Override
     protected boolean removeEldestEntry(Map.Entry<K, Integer> eldest)
     {
        return this.size() > 10; 
     }


}