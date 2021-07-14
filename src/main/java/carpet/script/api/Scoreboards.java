package carpet.script.api;

import carpet.mixins.ScoreboardObjective_scarpetMixin;
import carpet.mixins.Scoreboard_scarpetMixin;
import carpet.mixins.Team_scarpetMixin;
import carpet.script.CarpetContext;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.utils.InputValidator;
import carpet.script.value.BooleanValue;
import carpet.script.value.EntityValue;
import carpet.script.value.ListValue;
import carpet.script.value.NullValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.Value;
import com.google.common.collect.Lists;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.BossBarManager;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Scoreboards {
    private static String getScoreboardKeyFromValue(Value keyValue)
    {
        if (keyValue instanceof EntityValue)
        {
            Entity e = ((EntityValue) keyValue).getEntity();
            if (e instanceof PlayerEntity)
            {
                return e.getName().getString();
            }
            else
            {
                return e.getUuidAsString();
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
            ScoreboardObjective objective = scoreboard.getObjective(objectiveName);
            if (objective == null) return Value.NULL;
            if (lv.size()==1)
                return ListValue.wrap(scoreboard.getAllPlayerScores(objective).stream().map(s -> new StringValue(s.getPlayerName())).collect(Collectors.toList()));
            String key = getScoreboardKeyFromValue(lv.get(1));
            if(lv.size()==2)
            {
                if(!scoreboard.playerHasObjective(key, objective)) return Value.NULL;
                return NumericValue.of(scoreboard.getPlayerScore(key,objective).getScore());
            }

            Value value = lv.get(2);
            if(value.isNull()) {
                ScoreboardPlayerScore score = scoreboard.getPlayerScore(key, objective);
                scoreboard.resetPlayerScore(key,objective);
                return NumericValue.of(score.getScore());
            }
            if (value instanceof NumericValue) {
                ScoreboardPlayerScore score = scoreboard.getPlayerScore(key, objective);
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
            ScoreboardObjective objective = scoreboard.getObjective(objectiveName);
            if (objective == null) return Value.FALSE;
            if (lv.size() == 1)
            {
                scoreboard.removeObjective(objective);
                return Value.TRUE;
            }
            String key = getScoreboardKeyFromValue(lv.get(1));
            if (!scoreboard.playerHasObjective(key, objective)) return Value.NULL;
            ScoreboardPlayerScore scoreboardPlayerScore = scoreboard.getPlayerScore(key, objective);
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
            ScoreboardCriterion criterion;
            if (lv.size() == 1 )
            {
                criterion = ScoreboardCriterion.DUMMY;
            }
            else
            {
                String critetionName = lv.get(1).getString();
                criterion = ScoreboardCriterion.getOrCreateStatCriterion(critetionName).orElse(null);
                if (criterion==null)
                {
                    throw new ThrowStatement(critetionName, Throwables.UNKNOWN_CRITERION);
                }
            }

            ScoreboardObjective objective = scoreboard.getObjective(objectiveName);
            if (objective != null) {
                c.host.issueDeprecation("reading or modifying an objective's criterion with scoreboard_add");
                if(lv.size() == 1) return StringValue.of(objective.getCriterion().getName());
                if(objective.getCriterion().equals(criterion) || lv.size() == 1) return Value.NULL;
                ((Scoreboard_scarpetMixin)scoreboard).getObjectivesByCriterion().get(objective.getCriterion()).remove(objective);
                ((ScoreboardObjective_scarpetMixin) objective).setCriterion(criterion);
                (((Scoreboard_scarpetMixin)scoreboard).getObjectivesByCriterion().computeIfAbsent(criterion, (criterion1) -> Lists.newArrayList())).add(objective);
                scoreboard.updateObjective(objective);
                return Value.FALSE;
            }

            scoreboard.addObjective(objectiveName, criterion, new LiteralText(objectiveName), criterion.getDefaultRenderType());
            return Value.TRUE;
        });

        expression.addContextFunction("scoreboard_property", -1, (c, t, lv) ->
        {
            if(lv.size() < 2) throw new InternalExpressionException("'scoreboard_property' requires at least two parameters");
            CarpetContext cc = (CarpetContext)c;
            Scoreboard scoreboard =  cc.s.getServer().getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjective(lv.get(0).getString());
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
                        ScoreboardCriterion criterion = ScoreboardCriterion.getOrCreateStatCriterion(setValue.getString()).orElse(null);
                        if (criterion==null) throw new InternalExpressionException("Unknown scoreboard criterion: "+ setValue.getString());
                        if(objective.getCriterion().equals(criterion) || lv.size() == 1) return Value.FALSE;
                        ((Scoreboard_scarpetMixin)scoreboard).getObjectivesByCriterion().get(objective.getCriterion()).remove(objective);
                        ((ScoreboardObjective_scarpetMixin) objective).setCriterion(criterion);
                        (((Scoreboard_scarpetMixin)scoreboard).getObjectivesByCriterion().computeIfAbsent(criterion, (criterion1) -> Lists.newArrayList())).add(objective);
                        scoreboard.updateObjective(objective);
                        return Value.TRUE;
                    }
                    return StringValue.of(objective.getCriterion().getName());
                case "display_name":
                    if(modify) {
                        Text text = FormattedTextValue.getTextByValue(setValue);
                        objective.setDisplayName(text);
                        return Value.TRUE;
                    }
                    return new FormattedTextValue(objective.getDisplayName());
                case "display_slot":
                    if(modify) {
                        int slotId = Scoreboard.getDisplaySlotId(setValue.getString());
                        if(slotId == -1) throw new InternalExpressionException("Unknown scoreboard display slot: " + setValue.getString());
                        if(objective.equals(scoreboard.getObjectiveForSlot(slotId))) {
                            return Value.FALSE;
                        }
                        scoreboard.setObjectiveSlot(slotId,objective);
                        return Value.TRUE;
                    }

                    List<Value> slots = new ArrayList<>();
                    for(int i = 0; i < 19; i++) {
                        if (scoreboard.getObjectiveForSlot(i) == objective) {
                            String slotName = Scoreboard.getDisplaySlotName(i);
                            slots.add(StringValue.of(slotName));
                        }
                    }
                    return ListValue.wrap(slots);
                case "render_type":
                    if(modify) {
                        ScoreboardCriterion.RenderType renderType = ScoreboardCriterion.RenderType.getType(setValue.getString().toLowerCase());
                        if(objective.getRenderType().equals(renderType)) return Value.FALSE;
                        objective.setRenderType(renderType);
                        return Value.TRUE;
                    }
                    return StringValue.of(objective.getRenderType().getName());
                default:
                    throw new InternalExpressionException("scoreboard property '" + property + "' is not a valid property");
            }
        });

        expression.addContextFunction("scoreboard_display", 2, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            Scoreboard scoreboard =  cc.s.getServer().getScoreboard();
            String location = lv.get(0).getString();
            int slot = Scoreboard.getDisplaySlotId(location);
            if (slot < 0) throw new InternalExpressionException("Invalid objective slot: "+location);
            Value target = lv.get(1);
            if (target instanceof NullValue)
            {
                scoreboard.setObjectiveSlot(slot, null);
                return new NumericValue(slot);
            }
            String objectiveString = target.getString();
            ScoreboardObjective objective = scoreboard.getObjective(objectiveString);
            if (objective == null) return Value.NULL;
            scoreboard.setObjectiveSlot(slot, objective);
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
            Team team = scoreboard.getTeam(lv.get(0).getString());
            if(team == null) return Value.NULL;
            return ListValue.wrap(team.getPlayerList().stream().map(StringValue::of).collect(Collectors.toList()));
        });


        expression.addContextFunction("team_add", -1, (c, t, lv) ->
        {
            if(!(lv.size() < 3 && lv.size() > 0)) throw new InternalExpressionException("'team_add' requires one or two parameters");

            CarpetContext cc = (CarpetContext)c;
            ServerScoreboard scoreboard = cc.s.getServer().getScoreboard();
            String teamName = lv.get(0).getString();

            if(lv.size() == 1)
            {
                if (scoreboard.getTeam(teamName) != null) return Value.NULL;
                scoreboard.addTeam(teamName);
                return new StringValue(teamName);
            }
            if(lv.size() != 2) return Value.NULL;
            Value playerVal = lv.get(1);
            String player = EntityValue.getPlayerNameByValue(playerVal);
            if(player == null) return Value.NULL;
            Team team = scoreboard.getTeam(teamName);
            if(team == null) return Value.NULL;
            if(team.isEqual(scoreboard.getPlayerTeam(player))) return Value.FALSE;
            scoreboard.addPlayerToTeam(player,scoreboard.getTeam(teamName));
            return Value.TRUE;
        });

        expression.addContextFunction("team_remove", 1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerScoreboard scoreboard = cc.s.getServer().getScoreboard();
            Value teamVal = lv.get(0);
            String team = teamVal.getString();
            if(scoreboard.getTeam(team) == null) return Value.NULL;
            scoreboard.removeTeam(scoreboard.getTeam(team));
            return Value.TRUE;
        });


        expression.addContextFunction("team_leave", 1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerScoreboard scoreboard = cc.s.getServer().getScoreboard();
            Value playerVal = lv.get(0);
            String player = EntityValue.getPlayerNameByValue(playerVal);
            if(player == null) return Value.NULL;
            return BooleanValue.of(scoreboard.clearPlayerTeam(player));
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

            Team team = scoreboard.getTeam(teamVal.getString());
            if(team == null) return Value.NULL;

            if(!(propertyVal instanceof StringValue)) throw new InternalExpressionException("'team_property' requires a string as the second argument");

            switch (propertyVal.getString()) {
                case "collisionRule":
                    if(!modifying) return new StringValue(team.getCollisionRule().name);
                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_property' requires a string as the third argument for the property " + propertyVal.getString());
                    AbstractTeam.CollisionRule collisionRule = AbstractTeam.CollisionRule.getRule(settingVal.getString());
                    if(collisionRule == null) throw new InternalExpressionException("Unknown value for property " + propertyVal.getString() + ": " + settingVal.getString());
                    team.setCollisionRule(collisionRule);
                    break;
                case "color":
                    if(!modifying) return new StringValue(((Team_scarpetMixin) team).getColor().getName());
                    if(!(settingVal instanceof StringValue)) throw new InternalExpressionException("'team_property' requires a string as the third argument for the property " + propertyVal.getString());
                    Formatting color = Formatting.byName(settingVal.getString().toUpperCase());
                    if(color == null || !color.isColor()) throw new InternalExpressionException("Unknown value for property " + propertyVal.getString() + ": " + settingVal.getString());
                    team.setColor(color);
                    break;
                case "deathMessageVisibility":
                    if(!modifying) return new StringValue(team.getDeathMessageVisibilityRule().name);
                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_property' requires a string as the third argument for the property " + propertyVal.getString());
                    AbstractTeam.VisibilityRule deathMessageVisibility = AbstractTeam.VisibilityRule.getRule(settingVal.getString());
                    if(deathMessageVisibility == null) throw new InternalExpressionException("Unknown value for property " + propertyVal.getString() + ": " + settingVal.getString());
                    team.setDeathMessageVisibilityRule(deathMessageVisibility);
                    break;
                case "displayName":
                    if(!modifying) return new FormattedTextValue(team.getDisplayName());
                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_property' requires a string or formatted text as the third argument for the property " + propertyVal.getString());
                    Text displayName;
                    displayName = FormattedTextValue.getTextByValue(settingVal);
                    team.setDisplayName(displayName);
                    break;
                case "friendlyFire":
                    if(!modifying) return BooleanValue.of(team.isFriendlyFireAllowed());
                    if(!(settingVal instanceof NumericValue)) throw  new InternalExpressionException("'team_property' requires a boolean as the third argument for the property " + propertyVal.getString());
                    boolean friendlyFire = settingVal.getBoolean();
                    team.setFriendlyFireAllowed(friendlyFire);
                    break;
                case "nametagVisibility":
                    if(!modifying) return new StringValue(team.getNameTagVisibilityRule().name);
                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_property' requires a string as the third argument for the property " + propertyVal.getString());
                    AbstractTeam.VisibilityRule nametagVisibility = AbstractTeam.VisibilityRule.getRule(settingVal.getString());
                    if(nametagVisibility == null) throw new InternalExpressionException("Unknown value for property " + propertyVal.getString() + ": " + settingVal.getString());
                    team.setNameTagVisibilityRule(nametagVisibility);

                    break;
                case "prefix":
                    if(!modifying) return new FormattedTextValue(team.getPrefix());
                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_property ' requires a string or formatted text as the third argument for the property " + propertyVal.getString());
                    Text prefix;
                    prefix = FormattedTextValue.getTextByValue(settingVal);
                    team.setPrefix(prefix);
                    break;
                case "seeFriendlyInvisibles":
                    if(!modifying) return BooleanValue.of(team.shouldShowFriendlyInvisibles());
                    if(!(settingVal instanceof NumericValue)) throw  new InternalExpressionException("'team_property' requires a boolean as the third argument for the property " + propertyVal.getString());
                    boolean seeFriendlyInvisibles = settingVal.getBoolean();
                    team.setShowFriendlyInvisibles(seeFriendlyInvisibles);
                    break;
                case "suffix":
                    if(!modifying) return new FormattedTextValue(team.getSuffix());
                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_property' requires a string or formatted text as the third argument for the property " + propertyVal.getString());
                    Text suffix;
                    suffix = FormattedTextValue.getTextByValue(settingVal);
                    team.setSuffix(suffix);
                    break;
                default:
                    throw new InternalExpressionException("team property '" + propertyVal.getString() + "' is not a valid property");
            }
            return Value.TRUE;
        });

        expression.addContextFunction("bossbar", -1, (c, t, lv) ->
        {
            BossBarManager bossBarManager = ((CarpetContext)c).s.getServer().getBossBarManager();
            if(lv.size() > 3) throw new InternalExpressionException("'bossbar' accepts max three arguments");

            if(lv.size() == 0) return ListValue.wrap(bossBarManager.getAll().stream().map(CommandBossBar::getId).map(Identifier::toString).map(StringValue::of).collect(Collectors.toList()));

            String id = lv.get(0).getString();
            Identifier identifier;
            identifier = InputValidator.identifierOf(id);

            if(lv.size() == 1)
            {
                if(bossBarManager.get(identifier) != null) return Value.FALSE;
                return StringValue.of(bossBarManager.add(identifier,new LiteralText(id)).getId().toString());
            }

            String property = lv.get(1).getString();

            CommandBossBar bossBar = bossBarManager.get(identifier);
            if(bossBar == null) return Value.NULL;

            Value propertyValue = (lv.size() == 3)?lv.get(2):null;

            switch (property) {
                case "color":
                    if(propertyValue == null) {
                        BossBar.Color color = (bossBar).getColor();
                        if(color == null) return Value.NULL;
                        return StringValue.of(color.getName());
                    }

                    BossBar.Color color = BossBar.Color.byName(propertyValue.getString());
                    if(color == null) return Value.NULL;
                    bossBar.setColor(BossBar.Color.byName(propertyValue.getString()));
                    return Value.TRUE;
                case "max":
                    if(propertyValue == null) return NumericValue.of(bossBar.getMaxValue());

                    if(!(propertyValue instanceof NumericValue)) throw new InternalExpressionException("'bossbar' requires a number as the value for the property " + property);
                    bossBar.setMaxValue(((NumericValue) propertyValue).getInt());
                    return Value.TRUE;
                case "name":
                    if(propertyValue == null) return new FormattedTextValue(bossBar.getName());

                    bossBar.setName(FormattedTextValue.getTextByValue(propertyValue));
                    return Value.TRUE;
                case "add_player":
                    if(propertyValue == null) throw new InternalExpressionException("Bossbar property " + property + " can't be queried, add a third parameter");

                    if(propertyValue instanceof ListValue) {
                        ((ListValue) propertyValue).getItems().forEach((v)->{
                            ServerPlayerEntity player = EntityValue.getPlayerByValue(((CarpetContext)c).s.getServer(),propertyValue);
                            if(player != null) bossBar.addPlayer(player);
                        });
                        return Value.TRUE;
                    }

                    ServerPlayerEntity player = EntityValue.getPlayerByValue(((CarpetContext) c).s.getServer(), propertyValue);
                    if(player != null) {
                        bossBar.addPlayer(player);
                        return Value.TRUE;
                    }
                    return Value.FALSE;
                case "players":
                    if (propertyValue == null) return ListValue.wrap(bossBar.getPlayers().stream().map(EntityValue::new).collect(Collectors.toList()));
                    if(propertyValue instanceof ListValue)
                    {
                        bossBar.clearPlayers();
                        ((ListValue) propertyValue).getItems().forEach((v) -> {
                            ServerPlayerEntity p = EntityValue.getPlayerByValue(((CarpetContext) c).s.getServer(), v);
                            if (p != null) bossBar.addPlayer(p);
                        });
                        return Value.TRUE;
                    }


                    ServerPlayerEntity p = EntityValue.getPlayerByValue(((CarpetContext) c).s.getServer(), propertyValue);
                    bossBar.clearPlayers();
                    if (p != null) {
                        bossBar.addPlayer(p);
                        return Value.TRUE;
                    }
                    return Value.FALSE;
                case "style":
                    if(propertyValue == null) return StringValue.of(bossBar.getStyle().getName());
                    BossBar.Style style = BossBar.Style.byName(propertyValue.getString());
                    if(style == null) throw new InternalExpressionException("'" + propertyValue.getString() + "' is not a valid value for property " + property);
                    bossBar.setStyle(style);
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

