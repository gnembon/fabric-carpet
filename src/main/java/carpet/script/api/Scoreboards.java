package carpet.script.api;

import carpet.mixins.Objective_scarpetMixin;
import carpet.mixins.Scoreboard_scarpetMixin;
import carpet.mixins.PlayerTeam_scarpetMixin;
import carpet.script.CarpetContext;
import carpet.script.Expression;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.utils.InputValidator;
import carpet.script.value.BooleanValue;
import carpet.script.value.EntityValue;
import carpet.script.value.ListValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.Value;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class Scoreboards {
    private static String getScoreboardKeyFromValue(Value keyValue)
    {
        if (keyValue instanceof EntityValue)
        {
            Entity e = ((EntityValue) keyValue).getEntity();
            if (e instanceof Player)
            {
                return e.getName().getString();
            }
            else
            {
                return e.getStringUUID();
            }
        }
        else
        {
            return keyValue.getString();
        }
    }

    public static void apply(Expression expression)
    {
        // scoreboard(player,'objective')
        // scoreboard(player, objective, newValue)
        expression.addContextFunction("scoreboard", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            Scoreboard scoreboard =  cc.s.getServer().getScoreboard();
            if (lv.size()==0)
                return ListValue.wrap(scoreboard.getObjectiveNames().stream().map(StringValue::new).collect(Collectors.toList()));
            String objectiveName = lv.get(0).getString();
            Objective objective = scoreboard.getOrCreateObjective(objectiveName);
            if (objective == null) return Value.NULL;
            if (lv.size()==1)
                return ListValue.wrap(scoreboard.getPlayerScores(objective).stream().map(s -> new StringValue(s.getOwner())).collect(Collectors.toList()));
            String key = getScoreboardKeyFromValue(lv.get(1));
            if(lv.size()==2)
            {
                if(!scoreboard.hasPlayerScore(key, objective)) return Value.NULL;
                return NumericValue.of(scoreboard.getOrCreatePlayerScore(key,objective).getScore());
            }

            Value value = lv.get(2);
            if(value.isNull()) {
                Score score = scoreboard.getOrCreatePlayerScore(key, objective);
                scoreboard.resetPlayerScore(key,objective);
                return NumericValue.of(score.getScore());
            }
            if (value instanceof NumericValue) {
                Score score = scoreboard.getOrCreatePlayerScore(key, objective);
                score.setScore(NumericValue.asNumber(value).getInt());
                return NumericValue.of(score.getScore());
            }
            throw new InternalExpressionException("'scoreboard' requires a number or null as the third parameter");
        });

        expression.addContextFunction("scoreboard_remove", -1, (c, t, lv)->
        {
            if (lv.size()==0) throw new InternalExpressionException("'scoreboard_remove' requires at least one parameter");
            CarpetContext cc = (CarpetContext)c;
            Scoreboard scoreboard =  cc.s.getServer().getScoreboard();
            String objectiveName = lv.get(0).getString();
            Objective objective = scoreboard.getOrCreateObjective(objectiveName);
            if (objective == null) return Value.FALSE;
            if (lv.size() == 1)
            {
                scoreboard.removeObjective(objective);
                return Value.TRUE;
            }
            String key = getScoreboardKeyFromValue(lv.get(1));
            if (!scoreboard.hasPlayerScore(key, objective)) return Value.NULL;
            Score scoreboardPlayerScore = scoreboard.getOrCreatePlayerScore(key, objective);
            Value previous = new NumericValue(scoreboardPlayerScore.getScore());
            scoreboard.resetPlayerScore(key, objective);
            return previous;
        });

        // objective_add('lvl','level')
        // objective_add('counter')

        expression.addContextFunction("scoreboard_add", -1, (c, t, lv)->
        {
            CarpetContext cc = (CarpetContext)c;
            Scoreboard scoreboard =  cc.s.getServer().getScoreboard();
            if (lv.size() == 0 || lv.size()>2) throw new InternalExpressionException("'scoreboard_add' should have one or two parameters");
            String objectiveName = lv.get(0).getString();
            ObjectiveCriteria criterion;
            if (lv.size() == 1 )
            {
                criterion = ObjectiveCriteria.DUMMY;
            }
            else
            {
                String critetionName = lv.get(1).getString();
                criterion = ObjectiveCriteria.byName(critetionName).orElse(null);
                if (criterion==null)
                {
                    throw new ThrowStatement(critetionName, Throwables.UNKNOWN_CRITERION);
                }
            }

            Objective objective = scoreboard.getOrCreateObjective(objectiveName);
            if (objective != null) {
                c.host.issueDeprecation("reading or modifying an objective's criterion with scoreboard_add");
                if(lv.size() == 1) return StringValue.of(objective.getCriteria().getName());
                if(objective.getCriteria().equals(criterion) || lv.size() == 1) return Value.NULL;
                ((Scoreboard_scarpetMixin)scoreboard).getObjectivesByCriterion().get(objective.getCriteria()).remove(objective);
                ((Objective_scarpetMixin) objective).setCriterion(criterion);
                (((Scoreboard_scarpetMixin)scoreboard).getObjectivesByCriterion().computeIfAbsent(criterion, (criterion1) -> Lists.newArrayList())).add(objective);
                scoreboard.onObjectiveAdded(objective);
                return Value.FALSE;
            }

            scoreboard.addObjective(objectiveName, criterion, new TextComponent(objectiveName), criterion.getDefaultRenderType());
            return Value.TRUE;
        });

        expression.addContextFunction("scoreboard_property", -1, (c, t, lv) ->
        {
            if(lv.size() < 2) throw new InternalExpressionException("'scoreboard_property' requires at least two parameters");
            CarpetContext cc = (CarpetContext)c;
            Scoreboard scoreboard =  cc.s.getServer().getScoreboard();
            Objective objective = scoreboard.getOrCreateObjective(lv.get(0).getString());
            if(objective == null) return Value.NULL;

            boolean modify = lv.size() > 2;
            Value setValue = null;
            if(modify) {
                setValue = lv.get(2);
            }
            String property = lv.get(1).getString();
            switch (property) {
                case "criterion":
                    if(modify) {
                        ObjectiveCriteria criterion = ObjectiveCriteria.byName(setValue.getString()).orElse(null);
                        if (criterion==null) throw new InternalExpressionException("Unknown scoreboard criterion: "+ setValue.getString());
                        if(objective.getCriteria().equals(criterion) || lv.size() == 1) return Value.FALSE;
                        ((Scoreboard_scarpetMixin)scoreboard).getObjectivesByCriterion().get(objective.getCriteria()).remove(objective);
                        ((Objective_scarpetMixin) objective).setCriterion(criterion);
                        (((Scoreboard_scarpetMixin)scoreboard).getObjectivesByCriterion().computeIfAbsent(criterion, (criterion1) -> Lists.newArrayList())).add(objective);
                        scoreboard.onObjectiveAdded(objective);
                        return Value.TRUE;
                    }
                    return StringValue.of(objective.getCriteria().getName());
                case "display_name":
                    if(modify) {
                        Component text = FormattedTextValue.getTextByValue(setValue);
                        objective.setDisplayName(text);
                        return Value.TRUE;
                    }
                    return new FormattedTextValue(objective.getDisplayName());
                case "display_slot":
                    if(modify) {
                        int slotId = Scoreboard.getDisplaySlotByName(setValue.getString());
                        if(slotId == -1) throw new InternalExpressionException("Unknown scoreboard display slot: " + setValue.getString());
                        if(objective.equals(scoreboard.getDisplayObjective(slotId))) {
                            return Value.FALSE;
                        }
                        scoreboard.setDisplayObjective(slotId,objective);
                        return Value.TRUE;
                    }

                    List<Value> slots = new ArrayList<>();
                    for(int i = 0; i < 19; i++) {
                        if (scoreboard.getDisplayObjective(i) == objective) {
                            String slotName = Scoreboard.getDisplaySlotName(i);
                            slots.add(StringValue.of(slotName));
                        }
                    }
                    return ListValue.wrap(slots);
                case "render_type":
                    if(modify) {
                        ObjectiveCriteria.RenderType renderType = ObjectiveCriteria.RenderType.byId(setValue.getString().toLowerCase());
                        if(objective.getRenderType().equals(renderType)) return Value.FALSE;
                        objective.setRenderType(renderType);
                        return Value.TRUE;
                    }
                    return StringValue.of(objective.getRenderType().getId());
                default:
                    throw new InternalExpressionException("scoreboard property '" + property + "' is not a valid property");
            }
        });

        expression.addContextFunction("scoreboard_display", 2, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            Scoreboard scoreboard =  cc.s.getServer().getScoreboard();
            String location = lv.get(0).getString();
            int slot = Scoreboard.getDisplaySlotByName(location);
            if (slot < 0) throw new InternalExpressionException("Invalid objective slot: "+location);
            Value target = lv.get(1);
            if (target.isNull())
            {
                scoreboard.setDisplayObjective(slot, null);
                return new NumericValue(slot);
            }
            String objectiveString = target.getString();
            Objective objective = scoreboard.getOrCreateObjective(objectiveString);
            if (objective == null) return Value.NULL;
            scoreboard.setDisplayObjective(slot, objective);
            return new NumericValue(slot);
        });

        expression.addContextFunction("team_list", -1, (c, t, lv) ->
        {
            if(lv.size() > 1) throw new InternalExpressionException("'team_list' requires zero or one parameters");
            CarpetContext cc = (CarpetContext)c;
            ServerScoreboard scoreboard = cc.s.getServer().getScoreboard();
            if(lv.size() == 0)
                return ListValue.wrap(scoreboard.getTeamNames().stream().map(StringValue::of).collect(Collectors.toList()));
            if (lv.size() != 1) return Value.NULL;
            PlayerTeam team = scoreboard.getPlayerTeam(lv.get(0).getString());
            if(team == null) return Value.NULL;
            return ListValue.wrap(team.getPlayers().stream().map(StringValue::of).collect(Collectors.toList()));
        });


        expression.addContextFunction("team_add", -1, (c, t, lv) ->
        {
            if(!(lv.size() < 3 && lv.size() > 0)) throw new InternalExpressionException("'team_add' requires one or two parameters");

            CarpetContext cc = (CarpetContext)c;
            ServerScoreboard scoreboard = cc.s.getServer().getScoreboard();
            String teamName = lv.get(0).getString();

            if(lv.size() == 1)
            {
                if (scoreboard.getPlayerTeam(teamName) != null) return Value.NULL;
                scoreboard.addPlayerTeam(teamName);
                return new StringValue(teamName);
            }
            if(lv.size() != 2) return Value.NULL;
            Value playerVal = lv.get(1);
            String player = EntityValue.getPlayerNameByValue(playerVal);
            if(player == null) return Value.NULL;
            PlayerTeam team = scoreboard.getPlayerTeam(teamName);
            if(team == null) return Value.NULL;
            if(team.isAlliedTo(scoreboard.getPlayersTeam(player))) return Value.FALSE;
            scoreboard.addPlayerToTeam(player,scoreboard.getPlayerTeam(teamName));
            return Value.TRUE;
        });

        expression.addContextFunction("team_remove", 1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerScoreboard scoreboard = cc.s.getServer().getScoreboard();
            Value teamVal = lv.get(0);
            String team = teamVal.getString();
            if(scoreboard.getPlayerTeam(team) == null) return Value.NULL;
            scoreboard.removePlayerTeam(scoreboard.getPlayerTeam(team));
            return Value.TRUE;
        });


        expression.addContextFunction("team_leave", 1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerScoreboard scoreboard = cc.s.getServer().getScoreboard();
            Value playerVal = lv.get(0);
            String player = EntityValue.getPlayerNameByValue(playerVal);
            if(player == null) return Value.NULL;
            return BooleanValue.of(scoreboard.removePlayerFromTeam(player));
        });

        expression.addContextFunction("team_property", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerScoreboard scoreboard = cc.s.getServer().getScoreboard();

            if(lv.size() < 2 || lv.size() > 3) throw new InternalExpressionException("'team_property' requires two or three arguments");

            Value teamVal = lv.get(0);
            Value propertyVal = lv.get(1);

            Value settingVal = null;
            boolean modifying = false;
            if(lv.size() == 3)
            {
                modifying = true;
                settingVal = lv.get(2);
            }

            PlayerTeam team = scoreboard.getPlayerTeam(teamVal.getString());
            if(team == null) return Value.NULL;

            if(!(propertyVal instanceof StringValue)) throw new InternalExpressionException("'team_property' requires a string as the second argument");

            switch (propertyVal.getString()) {
                case "collisionRule":
                    if(!modifying) return new StringValue(team.getCollisionRule().name);
                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_property' requires a string as the third argument for the property " + propertyVal.getString());
                    Team.CollisionRule collisionRule = Team.CollisionRule.byName(settingVal.getString());
                    if(collisionRule == null) throw new InternalExpressionException("Unknown value for property " + propertyVal.getString() + ": " + settingVal.getString());
                    team.setCollisionRule(collisionRule);
                    break;
                case "color":
                    if(!modifying) return new StringValue(((PlayerTeam_scarpetMixin) team).getColor().getName());
                    if(!(settingVal instanceof StringValue)) throw new InternalExpressionException("'team_property' requires a string as the third argument for the property " + propertyVal.getString());
                    ChatFormatting color = ChatFormatting.getByName(settingVal.getString().toUpperCase());
                    if(color == null || !color.isColor()) throw new InternalExpressionException("Unknown value for property " + propertyVal.getString() + ": " + settingVal.getString());
                    team.setColor(color);
                    break;
                case "deathMessageVisibility":
                    if(!modifying) return new StringValue(team.getDeathMessageVisibility().name);
                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_property' requires a string as the third argument for the property " + propertyVal.getString());
                    Team.Visibility deathMessageVisibility = Team.Visibility.byName(settingVal.getString());
                    if(deathMessageVisibility == null) throw new InternalExpressionException("Unknown value for property " + propertyVal.getString() + ": " + settingVal.getString());
                    team.setDeathMessageVisibility(deathMessageVisibility);
                    break;
                case "displayName":
                    if(!modifying) return new FormattedTextValue(team.getDisplayName());
                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_property' requires a string or formatted text as the third argument for the property " + propertyVal.getString());
                    Component displayName;
                    displayName = FormattedTextValue.getTextByValue(settingVal);
                    team.setDisplayName(displayName);
                    break;
                case "friendlyFire":
                    if(!modifying) return BooleanValue.of(team.isAllowFriendlyFire());
                    if(!(settingVal instanceof NumericValue)) throw  new InternalExpressionException("'team_property' requires a boolean as the third argument for the property " + propertyVal.getString());
                    boolean friendlyFire = settingVal.getBoolean();
                    team.setAllowFriendlyFire(friendlyFire);
                    break;
                case "nametagVisibility":
                    if(!modifying) return new StringValue(team.getNameTagVisibility().name);
                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_property' requires a string as the third argument for the property " + propertyVal.getString());
                    Team.Visibility nametagVisibility = Team.Visibility.byName(settingVal.getString());
                    if(nametagVisibility == null) throw new InternalExpressionException("Unknown value for property " + propertyVal.getString() + ": " + settingVal.getString());
                    team.setNameTagVisibility(nametagVisibility);

                    break;
                case "prefix":
                    if(!modifying) return new FormattedTextValue(team.getPlayerPrefix());
                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_property ' requires a string or formatted text as the third argument for the property " + propertyVal.getString());
                    Component prefix;
                    prefix = FormattedTextValue.getTextByValue(settingVal);
                    team.setPlayerPrefix(prefix);
                    break;
                case "seeFriendlyInvisibles":
                    if(!modifying) return BooleanValue.of(team.canSeeFriendlyInvisibles());
                    if(!(settingVal instanceof NumericValue)) throw  new InternalExpressionException("'team_property' requires a boolean as the third argument for the property " + propertyVal.getString());
                    boolean seeFriendlyInvisibles = settingVal.getBoolean();
                    team.setSeeFriendlyInvisibles(seeFriendlyInvisibles);
                    break;
                case "suffix":
                    if(!modifying) return new FormattedTextValue(team.getPlayerSuffix());
                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_property' requires a string or formatted text as the third argument for the property " + propertyVal.getString());
                    Component suffix;
                    suffix = FormattedTextValue.getTextByValue(settingVal);
                    team.setPlayerSuffix(suffix);
                    break;
                default:
                    throw new InternalExpressionException("team property '" + propertyVal.getString() + "' is not a valid property");
            }
            return Value.TRUE;
        });

        expression.addContextFunction("bossbar", -1, (c, t, lv) ->
        {
            CustomBossEvents bossBarManager = ((CarpetContext)c).s.getServer().getCustomBossEvents();
            if(lv.size() > 3) throw new InternalExpressionException("'bossbar' accepts max three arguments");

            if(lv.size() == 0) return ListValue.wrap(bossBarManager.getEvents().stream().map(CustomBossEvent::getTextId).map(ResourceLocation::toString).map(StringValue::of).collect(Collectors.toList()));

            String id = lv.get(0).getString();
            ResourceLocation identifier;
            identifier = InputValidator.identifierOf(id);

            if(lv.size() == 1)
            {
                if(bossBarManager.get(identifier) != null) return Value.FALSE;
                return StringValue.of(bossBarManager.create(identifier,new TextComponent(id)).getTextId().toString());
            }

            String property = lv.get(1).getString();

            CustomBossEvent bossBar = bossBarManager.get(identifier);
            if(bossBar == null) return Value.NULL;

            Value propertyValue = (lv.size() == 3)?lv.get(2):null;

            switch (property) {
                case "color":
                    if(propertyValue == null) {
                        BossEvent.BossBarColor color = (bossBar).getColor();
                        if(color == null) return Value.NULL;
                        return StringValue.of(color.getName());
                    }

                    BossEvent.BossBarColor color = BossEvent.BossBarColor.byName(propertyValue.getString());
                    if(color == null) return Value.NULL;
                    bossBar.setColor(BossEvent.BossBarColor.byName(propertyValue.getString()));
                    return Value.TRUE;
                case "max":
                    if(propertyValue == null) return NumericValue.of(bossBar.getMax());

                    if(!(propertyValue instanceof NumericValue)) throw new InternalExpressionException("'bossbar' requires a number as the value for the property " + property);
                    bossBar.setMax(((NumericValue) propertyValue).getInt());
                    return Value.TRUE;
                case "name":
                    if(propertyValue == null) return new FormattedTextValue(bossBar.getName());

                    bossBar.setName(FormattedTextValue.getTextByValue(propertyValue));
                    return Value.TRUE;
                case "add_player":
                    if(propertyValue == null) throw new InternalExpressionException("Bossbar property " + property + " can't be queried, add a third parameter");

                    if(propertyValue instanceof ListValue) {
                        ((ListValue) propertyValue).getItems().forEach((v)->{
                            ServerPlayer player = EntityValue.getPlayerByValue(((CarpetContext)c).s.getServer(),propertyValue);
                            if(player != null) bossBar.addPlayer(player);
                        });
                        return Value.TRUE;
                    }

                    ServerPlayer player = EntityValue.getPlayerByValue(((CarpetContext) c).s.getServer(), propertyValue);
                    if(player != null) {
                        bossBar.addPlayer(player);
                        return Value.TRUE;
                    }
                    return Value.FALSE;
                case "players":
                    if (propertyValue == null) return ListValue.wrap(bossBar.getPlayers().stream().map(EntityValue::new).collect(Collectors.toList()));
                    if(propertyValue instanceof ListValue)
                    {
                        bossBar.removeAllPlayers();
                        ((ListValue) propertyValue).getItems().forEach((v) -> {
                            ServerPlayer p = EntityValue.getPlayerByValue(((CarpetContext) c).s.getServer(), v);
                            if (p != null) bossBar.addPlayer(p);
                        });
                        return Value.TRUE;
                    }


                    ServerPlayer p = EntityValue.getPlayerByValue(((CarpetContext) c).s.getServer(), propertyValue);
                    bossBar.removeAllPlayers();
                    if (p != null) {
                        bossBar.addPlayer(p);
                        return Value.TRUE;
                    }
                    return Value.FALSE;
                case "style":
                    if(propertyValue == null) return StringValue.of(bossBar.getOverlay().getName());
                    BossEvent.BossBarOverlay style = BossEvent.BossBarOverlay.byName(propertyValue.getString());
                    if(style == null) throw new InternalExpressionException("'" + propertyValue.getString() + "' is not a valid value for property " + property);
                    bossBar.setOverlay(style);
                    return Value.TRUE;
                case "value":
                    if(propertyValue == null) return NumericValue.of(bossBar.getValue());

                    if(!(propertyValue instanceof NumericValue)) throw new InternalExpressionException("'bossbar' requires a number as the value for the property " + property);
                    bossBar.setValue(((NumericValue) propertyValue).getInt());
                    return Value.TRUE;
                case "visible":
                    if(propertyValue == null) return BooleanValue.of(bossBar.isVisible());

                    bossBar.setVisible(propertyValue.getBoolean());
                    return Value.TRUE;
                case "remove":
                    bossBarManager.remove(bossBar);
                    return Value.TRUE;
                default:
                    throw new InternalExpressionException("Unknown bossbar property " + property);
            }
        });
    }
}

