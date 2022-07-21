package carpet.helpers;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import carpet.fakes.BlockStateBaseInterface;
import carpet.utils.BlockUtils;

import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.LevelResource;

public class PistonMoveBehaviorManager {

    private static final Logger LOGGER = LogManager.getLogger("Carpet Piston Move Behavior Manager");

    private static boolean dirty;

    public static boolean canChangeOverride(BlockState state) {
        return ((BlockStateBaseInterface)state).canChangePistonMoveBehaviorOverride();
    }

    public static PistonMoveBehavior getOverride(BlockState state) {
        return ((BlockStateBaseInterface)state).getPistonMoveBehaviorOverride();
    }

    public static void setOverride(BlockState state, PistonMoveBehavior override) {
        ((BlockStateBaseInterface)state).setPistonMoveBehaviorOverride(override);
    }

    public static void resetOverride(BlockState state) {
        setOverride(state, getDefaultOverride(state));
    }

    public static PistonMoveBehavior getDefaultOverride(BlockState state) {
        return ((BlockStateBaseInterface)state).getDefaultPistonMoveBehaviorOverride();
    }

    public static void setDefaultOverride(BlockState state, PistonMoveBehavior override) {
        ((BlockStateBaseInterface)state).setDefaultPistonMoveBehaviorOverride(override);
        dirty = true;
    }

    private static void setDefaultOverrides(Block block, PistonMoveBehavior override) {
        setDefaultOverrides(block, override, state -> true);
    }

    private static void setDefaultOverrides(Block block, PistonMoveBehavior override, Predicate<BlockState> filter) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            if (filter.test(state)) {
                setDefaultOverride(state, override);
            }
        }
    }

    public static void resetOverrides() {
        Iterator<Block> it = Registry.BLOCK.iterator();

        while (it.hasNext()) {
            Block block = it.next();

            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                resetOverride(state);
            }
        }
    }

    private static void initDefaultOverrides() {
        Iterator<Block> it = Registry.BLOCK.iterator();

        while (it.hasNext()) {
            Block block = it.next();

            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                setDefaultOverride(state, PistonMoveBehavior.NONE);
            }
        }

        setDefaultOverrides(Blocks.MOVING_PISTON, PistonMoveBehavior.BLOCK);

        // Blocks that are immovable due to having block entities, but should
        // also be immovble for other reasons (e.g. containing obsidian).
        setDefaultOverrides(Blocks.ENCHANTING_TABLE, PistonMoveBehavior.BLOCK);
        setDefaultOverrides(Blocks.BEACON, PistonMoveBehavior.BLOCK);
        setDefaultOverrides(Blocks.ENDER_CHEST, PistonMoveBehavior.BLOCK);
        setDefaultOverrides(Blocks.SPAWNER, PistonMoveBehavior.BLOCK);
    }

    public static void load(MinecraftServer server) {
        LOGGER.info("loading carpet piston move behavior overrides...");

        initDefaultOverrides();
        Config.load(server);
        resetOverrides();
        dirty = false;
    }

    public static void save(MinecraftServer server, boolean quietly) {
        if (dirty) {
            if (!quietly) {
                LOGGER.info("saving carpet piston move behavior overrides...");
            }

            Config.save(server);
            dirty = false;
        }
    }

    /**
     * A wrapper of {@link net.minecraft.world.level.material.PushReaction PushReaction}
     * that includes {@code none}, to be used in the `/pistonmovebehavior` command.
     */
    public static enum PistonMoveBehavior {

        NONE     (0, "none"     , null),
        NORMAL   (1, "normal"   , PushReaction.NORMAL),
        DESTROY  (2, "destroy"  , PushReaction.DESTROY),
        BLOCK    (3, "block"    , PushReaction.BLOCK),
        IGNORE   (4, "ignore"   , PushReaction.IGNORE),
        PUSH_ONLY(5, "push_only", PushReaction.PUSH_ONLY);

        public static final PistonMoveBehavior[] ALL;
        private static final Map<String, PistonMoveBehavior> BY_NAME;
        private static final Map<PushReaction, PistonMoveBehavior> BY_PUSH_REACTION;

        static {

            PistonMoveBehavior[] values = values();

            ALL = new PistonMoveBehavior[values.length];
            BY_NAME = new HashMap<>();
            BY_PUSH_REACTION = new HashMap<>();

            for (PistonMoveBehavior behavior : values) {
                ALL[behavior.index] = behavior;
                BY_NAME.put(behavior.name, behavior);

                if (behavior.pushReaction != null) {
                    BY_PUSH_REACTION.put(behavior.pushReaction, behavior);
                }
            }
        }

        private final int index;
        private final String name;
        private final PushReaction pushReaction;

        private PistonMoveBehavior(int index, String name, PushReaction pushReaction) {
            this.index = index;
            this.name = name;
            this.pushReaction = pushReaction;
        }

        public int getIndex() {
            return index;
        }

        public static PistonMoveBehavior fromIndex(int index) {
            return (index < 0 || index >= ALL.length) ? null : ALL[index];
        }

        public String getName() {
            return name;
        }

        public static PistonMoveBehavior fromName(String name) {
            return name == null ? null : BY_NAME.get(name);
        }

        public PushReaction getPushReaction() {
            return pushReaction;
        }

        public static PistonMoveBehavior fromPushReaction(PushReaction pushReaction) {
            return BY_PUSH_REACTION.get(pushReaction);
        }

        public boolean isPresent() {
            return pushReaction != null;
        }

        public boolean is(PushReaction pushReaction) {
            return pushReaction != null && this.pushReaction == pushReaction;
        }
    }

    public static class Config {

        public static final String FILE_NAME = "carpet_piston_move_behavior_overrides.json";
        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

        private static Path getFile(MinecraftServer server) {
            return server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        }

        public static void load(MinecraftServer server) {
            Path conf = getFile(server);

            if (!Files.exists(conf)) {
                return;
            }
            if (!Files.isRegularFile(conf) || !Files.isReadable(conf)) {
                LOGGER.warn("piston move behavior overrides config is not readable!");
                return;
            }

            try (FileReader fr = new FileReader(conf.toFile())) {
                JsonElement rawJson = GSON.fromJson(fr, JsonElement.class);

                if (rawJson.isJsonObject()) {
                    JsonObject json = rawJson.getAsJsonObject();

                    for (Entry<String, JsonElement> entry : json.entrySet()) {
                        loadOverrides(entry.getKey(), entry.getValue());
                    }
                }
            } catch (IOException | JsonSyntaxException | JsonIOException e) {

            }
        }

        private static void loadOverrides(String blockString, JsonElement rawJson) {
            Block block = BlockUtils.blockFromString(blockString);

            if (block == null) {
                LOGGER.info("ignoring piston move behavior overrides for unknown block " + blockString);
                return;
            }
            if (!rawJson.isJsonObject()) {
                LOGGER.info("piston move behavior overrides for " + blockString + " provided in an invalid format");
                return;
            }

            JsonObject json = rawJson.getAsJsonObject();

            for (Entry<String, JsonElement> entry : json.entrySet()) {
                loadOverride(block, entry.getKey(), entry.getValue());
            }
        }

        private static void loadOverride(Block block, String blockStateString, JsonElement rawJson) {
            BlockState state = BlockUtils.blockStateFromString(block, blockStateString);

            if (state == null) {
                LOGGER.info("ignoring piston move behavior overrides for unknown block state " + blockStateString + " of block " + block);
                return;
            }
            if (!canChangeOverride(state)) {
                LOGGER.info("ignoring piston move behavior override for block state " + blockStateString + " of block " + block + ": not allowed to change overrides");
                return;
            }
            if (!rawJson.isJsonPrimitive()) {
                LOGGER.info("piston move behavior overrides for block state " + blockStateString + " of block " + block + " provided in an invalid format");
                return;
            }

            String overrideString = rawJson.getAsString();
            PistonMoveBehavior override = PistonMoveBehavior.fromName(overrideString);

            if (override == null) {
                LOGGER.info("unknown piston move behavior " + overrideString + " given for block state " + blockStateString + " of block " + block);
                return;
            }

            setDefaultOverride(state, override);
        }

        public static void save(MinecraftServer server) {
            Path conf = getFile(server);

            if (Files.exists(conf) && !Files.isWritable(conf)) {
                LOGGER.warn("unable to write piston move behavior overrides config!");
                return;
            }

            JsonObject json = new JsonObject();

            Registry.BLOCK.forEach(block -> {
                saveOverrides(block, json);
            });

            if (json.size() == 0) {
                return; // nothing to save
            }

            try (FileWriter fw = new FileWriter(conf.toFile())) {
                fw.write(GSON.toJson(json));
            } catch (IOException e) {

            }
        }

        private static void saveOverrides(Block block, JsonObject json) {
            JsonObject overrides = new JsonObject();

            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                saveOverride(state, overrides);
            }

            if (overrides.size() > 0) {
                json.add(BlockUtils.blockAsString(block), overrides);
            }
        }

        private static void saveOverride(BlockState state, JsonObject json) {
            if (!canChangeOverride(state)) {
                return;
            }

            PistonMoveBehavior override = getDefaultOverride(state);

            if (!override.isPresent()) {
                return;
            }

            String blockStateString = BlockUtils.propertiesAsString(state);
            String overrideString = override.getName();

            json.addProperty(blockStateString, overrideString);
        }
    }
}
