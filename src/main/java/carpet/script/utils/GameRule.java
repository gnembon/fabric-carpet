package carpet.script.utils;

import carpet.script.CarpetContext;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GameRule {
    public static final Map<String, GameRules.Key> gamerules = new HashMap<String, GameRules.Key>(){{
        put("announce_advancements",
            GameRules.ANNOUNCE_ADVANCEMENTS
        );
        put("command_block_output",
            GameRules.COMMAND_BLOCK_OUTPUT
        );
        put("disable_elytra_movement_check",
            GameRules.DISABLE_ELYTRA_MOVEMENT_CHECK
        );
        put("disable_raids",
           GameRules.DISABLE_RAIDS
        );
        put("do_daylight_cycle",
           GameRules.DO_DAYLIGHT_CYCLE
        );
        put("do_entity_drops",
           GameRules.DO_ENTITY_DROPS
        );
        put("do_fire_tick",
           GameRules.DO_FIRE_TICK
        );
        put("do_immediate_respawn",
           GameRules.DO_IMMEDIATE_RESPAWN
        );
        put("do_insomnia",
           GameRules.DO_INSOMNIA
        );
        put("do_limited_crafting",
           GameRules.DO_LIMITED_CRAFTING
        );
        put("do_mob_loot",
           GameRules.DO_MOB_LOOT
        );
        put("do_mob_spawning",
           GameRules.DO_MOB_SPAWNING
        );
        put("do_patrol_spawning",
           GameRules.DISABLE_ELYTRA_MOVEMENT_CHECK
        );
        put("do_tile_drops",
            GameRules.DO_TILE_DROPS
        );
        put("do_trader_spawning",
            GameRules.DO_TRADER_SPAWNING
        );
        put("do_weather_cycle",
            GameRules.DO_WEATHER_CYCLE
        );
        put("drowning_damage",
            GameRules.DROWNING_DAMAGE
        );
        put("fall_damage",
            GameRules.FALL_DAMAGE
        );
        put("fire_damage",
            GameRules.FIRE_DAMAGE
        );
        put("forgive_dead_players",
            GameRules.FORGIVE_DEAD_PLAYERS
        );
        //put("freeze_damage", for 1.17+
        //    GameRules.DO_WEATHER_CYCLE
        //);
        put("keep_inventory",
            GameRules.KEEP_INVENTORY
        );
        put("log_admin_commands",
            GameRules.LOG_ADMIN_COMMANDS
        );
        put("max_command_chain_length",
            GameRules.MAX_COMMAND_CHAIN_LENGTH
        );
        put("max_entity_cramming",
            GameRules.MAX_ENTITY_CRAMMING
        );
        put("mob_griefing",
            GameRules.DO_MOB_GRIEFING
        );
        put("natural_regeneration",
            GameRules.NATURAL_REGENERATION
        );
        put("random_tick_speed",
            GameRules.RANDOM_TICK_SPEED
        );
        put("reduced_debug_info",
            GameRules.REDUCED_DEBUG_INFO
        );
        put("send_command_feedback",
            GameRules.SEND_COMMAND_FEEDBACK
        );
        put("spawn_radius",
            GameRules.SPAWN_RADIUS
        );
        put("spectators_generate_chunks",
            GameRules.SPECTATORS_GENERATE_CHUNKS
        );
        put("universal_anger",
            GameRules.UNIVERSAL_ANGER
        );
    }};

    public static Value getAll(CarpetContext cc)
    {
        return MapValue.wrap(gamerules.entrySet().stream().collect(Collectors.toMap(e -> new StringValue(e.getValue().getName()), e -> {
            GameRules.Key rule = e.getValue();

            return getRuleValue(rule,cc.s.getWorld());
        })));
    }

    public static Value getRuleValue(GameRules.Key rule, World world){

        try {//cos cant check for instanceof with gamerules for some reason
            return new NumericValue(world.getGameRules().getBoolean(rule));
        } catch(Exception exc){
            return new NumericValue(world.getGameRules().getInt(rule));
        }
    }
}
