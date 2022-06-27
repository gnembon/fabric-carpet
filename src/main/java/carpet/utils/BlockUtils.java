package carpet.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import net.minecraft.ResourceLocationException;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockUtils {

    public static String blockAsString(Block block) {
        return Registry.BLOCK.getKey(block).toString();
    }

    public static String blockStateAsString(BlockState state) {
        return blockStateAsString(state, state.getProperties());
    }

    public static String blockStateAsString(BlockState state, Collection<Property<?>> properties) {
        return blockAsString(state.getBlock()) + propertiesAsString(state, properties);
    }
    
    public static String propertiesAsString(BlockState state) {
        return propertiesAsString(state, state.getProperties());
    }

    public static String propertiesAsString(BlockState state, Collection<Property<?>> properties) {
         return "[" + String.join(",", valuesAsStrings(state, properties)) + "]";
    }

    public static Collection<String> valuesAsStrings(BlockState state, Collection<Property<?>> properties) {
        Collection<String> strings = new LinkedList<>();

        for (Property<?> property : properties) {
            if (state.hasProperty(property)) {
                strings.add(property.getName() + "=" + valueAsString(state, property));
            }
        }

        return strings;
    }

    public static <T extends Comparable<T>> String valueAsString(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    public static Block blockFromString(String string) {
        try {
            return Registry.BLOCK.get(new ResourceLocation(string));
        } catch (ResourceLocationException e) {

        }

        return null;
    }

    public static BlockState blockStateFromString(String string) {
        int i = string.indexOf('[');

        if (i < 0) {
            return null;
        }

        String blockString = string.substring(0, i);
        Block block = blockFromString(blockString);

        if (block == null) {
            return null;
        }

        return blockStateFromString(block, string.substring(i));
    }

    public static BlockState blockStateFromString(Block block, String string) {
        Map<Property<?>, String> properties = propertiesFromString(block, string);

        if (properties == null) {
            return null;
        }

        int propertyCount = properties.size();
        int expectedPropertyCount = block.getStateDefinition().getProperties().size();

        if (propertyCount != expectedPropertyCount) {
            return null;
        }

        BlockState state = block.defaultBlockState();

        for (Entry<Property<?>, String> entry : properties.entrySet()) {
            Property<?> property = entry.getKey();
            String valueString = entry.getValue();
            Comparable<?> value = valueFromString(property, valueString);

            if (value == null) {
                return null;
            }

            state = setValue(state, property, value);
        }

        return state;
    }

    public static Map<Property<?>, String> propertiesFromString(Block block, String string) {
        if (!string.startsWith("[") || !string.endsWith("]")) {
            return null;
        }

        string = string.substring(1, string.length() - 1);

        if (string.isEmpty()) {
            return Collections.emptyMap();
        }

        String[] entries = string.split("[,]");
        Map<Property<?>, String> properties = new HashMap<>();
        StateDefinition<Block, BlockState> stateDefinition = block.getStateDefinition();

        for (String entry : entries) {
            String[] args = entry.split("[=]");

            if (args.length != 2) {
                return null;
            }

            String propertyName = args[0];
            String valueName = args[1];

            Property<?> property = stateDefinition.getProperty(propertyName);

            if (property == null) {
                return null;
            }

            properties.put(property, valueName);
        }

        return properties;
    }

    public static <T extends Comparable<T>> T valueFromString(Property<T> property, String string) {
        Optional<T> o = property.getValue(string);
        return o == null ? null : o.get();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState setValue(BlockState state, Property<T> property, Comparable<?> value) {
        return state.setValue(property, (T)value);
    }
}
