package carpet.script.api;

import carpet.mixins.ScoreboardObjective_scarpetMixin;
import carpet.mixins.Scoreboard_scarpetMixin;
import carpet.script.CarpetContext;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
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
        expression.addLazyFunction("scoreboard", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            Scoreboard scoreboard =  cc.s.getMinecraftServer().getScoreboard();
            if (lv.size()==0)
            {
                Value ret = ListValue.wrap(scoreboard.getObjectiveNames().stream().map(StringValue::new).collect(Collectors.toList()));
                return (_c, _t) -> ret;
            }
            String objectiveName = lv.get(0).evalValue(c).getString();
            ScoreboardObjective objective = scoreboard.getObjective(objectiveName);
            if (objective == null) return LazyValue.NULL;
            if (lv.size()==1)
            {
                Value ret = ListValue.wrap(scoreboard.getAllPlayerScores(objective).stream().map(s -> new StringValue(s.getPlayerName())).collect(Collectors.toList()));
                return (_c, _t) -> ret;
            }

            String key = getScoreboardKeyFromValue(lv.get(1).evalValue(c));
            if(lv.size()==2) {
                if(scoreboard.playerHasObjective(key, objective)) {
                    return (_c,_t) -> NumericValue.of(scoreboard.getPlayerScore(key,objective).getScore());
                }
                return LazyValue.NULL;
            }

            Value value = lv.get(2).evalValue(c);
            if(value.isNull()) {
                ScoreboardPlayerScore score = scoreboard.getPlayerScore(key, objective);
                scoreboard.resetPlayerScore(key,objective);
                return (_c,_t) -> NumericValue.of(score.getScore());
            }
            if (value instanceof NumericValue) {
                ScoreboardPlayerScore score = scoreboard.getPlayerScore(key, objective);
                score.setScore(NumericValue.asNumber(value).getInt());
                return (_c,_t) -> NumericValue.of(score.getScore());
            }
            throw new InternalExpressionException("'scoreboard' requires a number or null as the third parameter");
        });

        expression.addLazyFunction("scoreboard_remove", -1, (c, t, lv)->
        {
            if (lv.size()==0) throw new InternalExpressionException("'scoreboard_remove' requires at least one parameter");
            CarpetContext cc = (CarpetContext)c;
            Scoreboard scoreboard =  cc.s.getMinecraftServer().getScoreboard();
            String objectiveName = lv.get(0).evalValue(c).getString();
            ScoreboardObjective objective = scoreboard.getObjective(objectiveName);
            if (objective == null)
                return LazyValue.FALSE;
            if (lv.size() == 1)
            {
                scoreboard.removeObjective(objective);
                return LazyValue.TRUE;
            }
            String key = getScoreboardKeyFromValue(lv.get(1).evalValue(c));
            if (!scoreboard.playerHasObjective(key, objective)) return LazyValue.NULL;
            ScoreboardPlayerScore scoreboardPlayerScore = scoreboard.getPlayerScore(key, objective);
            Value previous = new NumericValue(scoreboardPlayerScore.getScore());
            scoreboard.resetPlayerScore(key, objective);
            return (c_, t_) -> previous;
        });

        // objective_add('lvl','level')
        // objective_add('counter')

        expression.addLazyFunction("scoreboard_add", -1, (c, t, lv)->
        {
            CarpetContext cc = (CarpetContext)c;
            Scoreboard scoreboard =  cc.s.getMinecraftServer().getScoreboard();
            if (lv.size() == 0 || lv.size()>2) throw new InternalExpressionException("'scoreboard_add' should have one or two parameters");
            String objectiveName = lv.get(0).evalValue(c).getString();
            ScoreboardCriterion criterion;
            if (lv.size() == 1 )
            {
                criterion = ScoreboardCriterion.DUMMY;
            }
            else
            {
                String critetionName = lv.get(1).evalValue(c).getString();
                criterion = ScoreboardCriterion.createStatCriterion(critetionName).orElse(null);
                if (criterion==null)
                {
                    throw new ThrowStatement(critetionName, Throwables.UNKNOWN_CRITERION);
                }
            }

            ScoreboardObjective objective = scoreboard.getObjective(objectiveName);
            if (objective != null) {
                c.host.issueDeprecation("reading or modifying an objective's criterion with scoreboard_add");
                if(lv.size() == 1) return (_c, _t) -> StringValue.of(objective.getCriterion().getName());
                if(objective.getCriterion().equals(criterion) || lv.size() == 1) return LazyValue.NULL;
                ((Scoreboard_scarpetMixin)scoreboard).getObjectivesByCriterion().get(objective.getCriterion()).remove(objective);
                ((ScoreboardObjective_scarpetMixin) objective).setCriterion(criterion);
                (((Scoreboard_scarpetMixin)scoreboard).getObjectivesByCriterion().computeIfAbsent(criterion, (criterion1) -> Lists.newArrayList())).add(objective);
                scoreboard.updateObjective(objective);
                return LazyValue.FALSE;
            }

            scoreboard.addObjective(objectiveName, criterion, new LiteralText(objectiveName), criterion.getCriterionType());
            return LazyValue.TRUE;
        });

        expression.addLazyFunction("scoreboard_property", -1, (c, t, lv) ->
        {
            if(lv.size() < 2) throw new InternalExpressionException("'scoreboard_property' requires at least two parameters");
            CarpetContext cc = (CarpetContext)c;
            Scoreboard scoreboard =  cc.s.getMinecraftServer().getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjective(lv.get(0).evalValue(c).getString());
            if(objective == null) return LazyValue.NULL;

            boolean modify = lv.size() > 2;
            Value setValue = null;
            if(modify) {
                setValue = lv.get(2).evalValue(c);
            }
            String property = lv.get(1).evalValue(c).getString();
            switch (property) {
                case "criterion":
                    if(modify) {
                        ScoreboardCriterion criterion = ScoreboardCriterion.createStatCriterion(setValue.getString()).orElse(null);
                        if (criterion==null) throw new InternalExpressionException("Unknown scoreboard criterion: "+ setValue.getString());
                        if(objective.getCriterion().equals(criterion) || lv.size() == 1) return LazyValue.FALSE;
                        ((Scoreboard_scarpetMixin)scoreboard).getObjectivesByCriterion().get(objective.getCriterion()).remove(objective);
                        ((ScoreboardObjective_scarpetMixin) objective).setCriterion(criterion);
                        (((Scoreboard_scarpetMixin)scoreboard).getObjectivesByCriterion().computeIfAbsent(criterion, (criterion1) -> Lists.newArrayList())).add(objective);
                        scoreboard.updateObjective(objective);
                        return LazyValue.TRUE;
                    }
                    return (_c, _t) -> StringValue.of(objective.getCriterion().getName());
                case "display_name":
                    if(modify) {
                        Text text = (setValue instanceof FormattedTextValue)?((FormattedTextValue) setValue).getText():new LiteralText(setValue.getString());
                        objective.setDisplayName(text);
                        return LazyValue.TRUE;
                    }
                    return (_c, _t) -> new FormattedTextValue(objective.getDisplayName());
                case "display_slot":
                    if(modify) {
                        int slotId = Scoreboard.getDisplaySlotId(setValue.getString());
                        if(slotId == -1) throw new InternalExpressionException("Unknown scoreboard display slot: " + setValue.getString());
                        if(objective.equals(scoreboard.getObjectiveForSlot(slotId))) {
                            return LazyValue.FALSE;
                        }
                        scoreboard.setObjectiveSlot(slotId,objective);
                        return LazyValue.TRUE;
                    }

                    List<Value> slots = new ArrayList<>();
                    for(int i = 0; i < 19; i++) {
                        if (scoreboard.getObjectiveForSlot(i) == objective) {
                            String slotName = Scoreboard.getDisplaySlotName(i);
                            slots.add(StringValue.of(slotName));
                        }
                    }
                    return (_c, _t) -> ListValue.wrap(slots);
                case "render_type":
                    if(modify) {
                        ScoreboardCriterion.RenderType renderType = ScoreboardCriterion.RenderType.getType(setValue.getString().toLowerCase());
                        if(objective.getRenderType().equals(renderType)) return LazyValue.FALSE;
                        objective.setRenderType(renderType);
                        return LazyValue.TRUE;
                    }
                    return (_c, _t) -> StringValue.of(objective.getRenderType().getName());
                default:
                    throw new InternalExpressionException("scoreboard property '" + property + "' is not a valid property");
            }
        });

        expression.addLazyFunction("scoreboard_display", 2, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            Scoreboard scoreboard =  cc.s.getMinecraftServer().getScoreboard();
            String location = lv.get(0).evalValue(c).getString();
            int slot = Scoreboard.getDisplaySlotId(location);
            if (slot < 0) throw new InternalExpressionException("Invalid objective slot: "+location);
            Value target = lv.get(1).evalValue(c);
            if (target instanceof NullValue)
            {
                scoreboard.setObjectiveSlot(slot, null);
                return (_c, _t) -> new NumericValue(slot);
            }
            String objectiveString = target.getString();
            ScoreboardObjective objective = scoreboard.getObjective(objectiveString);
            if (objective == null) return LazyValue.NULL;
            scoreboard.setObjectiveSlot(slot, objective);
            return (_c, _t) -> new NumericValue(slot);
        });

        expression.addLazyFunction("team_list", -1, (c, t, lv) ->
        {
            if(lv.size() > 1) throw new InternalExpressionException("'team_list' requires zero or one parameters");
            CarpetContext cc = (CarpetContext)c;
            ServerScoreboard scoreboard = cc.s.getMinecraftServer().getScoreboard();
            if(lv.size() == 0) {
                Value ret = ListValue.wrap(scoreboard.getTeamNames().stream().map(StringValue::of).collect(Collectors.toList()));
                return (_c, _t) -> ret;
            } else if(lv.size() == 1) {
                Team team = scoreboard.getTeam(lv.get(0).evalValue(c).getString());
                if(team == null) return LazyValue.NULL;
                Value ret = ListValue.wrap(team.getPlayerList().stream().map(StringValue::of).collect(Collectors.toList()));
                return (_c, _t) -> ret;
            } else {
                return LazyValue.NULL;
            }
        });


        expression.addLazyFunction("team_add", -1, (c, t, lv) ->
        {
            if(!(lv.size() < 3 && lv.size() > 0)) throw new InternalExpressionException("'team_add' requires one or two parameters");

            CarpetContext cc = (CarpetContext)c;
            ServerScoreboard scoreboard = cc.s.getMinecraftServer().getScoreboard();
            String teamName = lv.get(0).evalValue(c).getString();

            if(lv.size() == 1) {
                if(scoreboard.getTeam(teamName) == null) {
                    scoreboard.addTeam(teamName);
                    Value ret = new StringValue(teamName);
                    return (_c, _t) -> ret;
                } else {
                    return LazyValue.NULL;
                }
            } else if(lv.size() == 2) {
                Value playerVal = lv.get(1).evalValue(c);

                String player = EntityValue.getPlayerNameByValue(playerVal);
                if(player == null) return LazyValue.NULL;

                Team team = scoreboard.getTeam(teamName);
                if(team == null) return LazyValue.NULL;

                if(team.isEqual(scoreboard.getPlayerTeam(player))) {
                    return LazyValue.FALSE;
                } else {
                    scoreboard.addPlayerToTeam(player,scoreboard.getTeam(teamName));
                    return LazyValue.TRUE;
                }
            } else {
                return LazyValue.NULL;
            }
        });

        expression.addLazyFunction("team_remove", 1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerScoreboard scoreboard = cc.s.getMinecraftServer().getScoreboard();
            Value teamVal = lv.get(0).evalValue(c);

            String team = teamVal.getString();

            if(scoreboard.getTeam(team) != null) {
                scoreboard.removeTeam(scoreboard.getTeam(team));
                return LazyValue.TRUE;
            } else {
                return LazyValue.NULL;
            }
        });


        expression.addLazyFunction("team_leave", 1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerScoreboard scoreboard = cc.s.getMinecraftServer().getScoreboard();
            Value playerVal = lv.get(0).evalValue(c);

            String player = EntityValue.getPlayerNameByValue(playerVal);
            if(player == null) return LazyValue.NULL;

            Value ret = new NumericValue(scoreboard.clearPlayerTeam(player));
            return (_c, _t) -> ret;
        });

        expression.addLazyFunction("team_property", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerScoreboard scoreboard = cc.s.getMinecraftServer().getScoreboard();

            if(lv.size() < 2 || lv.size() > 3) throw new InternalExpressionException("'team_property' requires two or three arguments");

            Value teamVal = lv.get(0).evalValue(c);
            Value propertyVal = lv.get(1).evalValue(c);

            Value settingVal = null;
            boolean modifying = false;
            if(lv.size() == 3) {
                modifying = true;
                settingVal = lv.get(2).evalValue(c);
            }

            Team team = scoreboard.getTeam(teamVal.getString());
            if(team == null) return LazyValue.NULL;

            if(!(propertyVal instanceof StringValue)) throw new InternalExpressionException("'team_property' requires a string as the second argument");

            switch (propertyVal.getString()) {
                case "collisionRule":
                    if(!modifying) {
                        Value ret = new StringValue(team.getCollisionRule().name);
                        return (_c, _t) -> ret;
                    }

                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_property' requires a string as the third argument for the property " + propertyVal.getString());

                    AbstractTeam.CollisionRule collisionRule = AbstractTeam.CollisionRule.getRule(settingVal.getString());
                    if(collisionRule == null) throw new InternalExpressionException("Unknown value for property " + propertyVal.getString() + ": " + settingVal.getString());
                    team.setCollisionRule(collisionRule);
                    break;
                case "color":
                    if(!modifying) {
                        Value ret = new StringValue(team.getColor().getName());
                        return (_c, _t) -> ret;
                    }

                    if(!(settingVal instanceof StringValue)) throw new InternalExpressionException("'team_property' requires a string as the third argument for the property " + propertyVal.getString());

                    Formatting color = Formatting.byName(settingVal.getString().toUpperCase());
                    if(color == null || !color.isColor()) throw new InternalExpressionException("Unknown value for property " + propertyVal.getString() + ": " + settingVal.getString());
                    team.setColor(color);

                    break;
                case "deathMessageVisibility":
                    if(!modifying) {
                        Value ret = new StringValue(team.getDeathMessageVisibilityRule().name);
                        return (_c, _t) -> ret;
                    }

                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_property' requires a string as the third argument for the property " + propertyVal.getString());

                    AbstractTeam.VisibilityRule deathMessageVisibility = AbstractTeam.VisibilityRule.getRule(settingVal.getString());
                    if(deathMessageVisibility == null) throw new InternalExpressionException("Unknown value for property " + propertyVal.getString() + ": " + settingVal.getString());
                    team.setDeathMessageVisibilityRule(deathMessageVisibility);

                    break;
                case "displayName":
                    if(!modifying) {
                        Value ret = new FormattedTextValue(team.getDisplayName());
                        return (_c, _t) -> ret;
                    }

                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_property' requires a string or formatted text as the third argument for the property " + propertyVal.getString());

                    Text displayName;

                    if(settingVal instanceof FormattedTextValue) {
                        displayName = ((FormattedTextValue) settingVal).getText();
                    } else {
                        displayName = new LiteralText(settingVal.getString());
                    }

                    team.setDisplayName(displayName);

                    break;
                case "friendlyFire":
                    if(!modifying) {
                        Value ret = new NumericValue(team.isFriendlyFireAllowed());
                        return (_c, _t) -> ret;
                    }

                    if(!(settingVal instanceof NumericValue)) throw  new InternalExpressionException("'team_property' requires a boolean as the third argument for the property " + propertyVal.getString());

                    boolean friendlyFire = settingVal.getBoolean();
                    team.setFriendlyFireAllowed(friendlyFire);
                    break;
                case "nametagVisibility":
                    if(!modifying) {
                        Value ret = new StringValue(team.getNameTagVisibilityRule().name);
                        return (_c, _t) -> ret;
                    }

                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_property' requires a string as the third argument for the property " + propertyVal.getString());

                    AbstractTeam.VisibilityRule nametagVisibility = AbstractTeam.VisibilityRule.getRule(settingVal.getString());
                    if(nametagVisibility == null) throw new InternalExpressionException("Unknown value for property " + propertyVal.getString() + ": " + settingVal.getString());
                    team.setNameTagVisibilityRule(nametagVisibility);

                    break;
                case "prefix":
                    if(!modifying) {
                        Value ret = new FormattedTextValue(team.getPrefix());
                        return (_c, _t) -> ret;
                    }

                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_property ' requires a string or formatted text as the third argument for the property " + propertyVal.getString());

                    Text prefix;

                    if(settingVal instanceof FormattedTextValue) {
                        prefix = ((FormattedTextValue) settingVal).getText();
                    } else {
                        prefix = new LiteralText(settingVal.getString());
                    }

                    team.setPrefix(prefix);
                    break;
                case "seeFriendlyInvisibles":
                    if(!modifying) {
                        Value ret = new NumericValue(team.shouldShowFriendlyInvisibles());
                        return (_c, _t) -> ret;
                    }

                    if(!(settingVal instanceof NumericValue)) throw  new InternalExpressionException("'team_property' requires a boolean as the third argument for the property " + propertyVal.getString());

                    boolean seeFriendlyInvisibles = settingVal.getBoolean();
                    team.setShowFriendlyInvisibles(seeFriendlyInvisibles);

                    break;
                case "suffix":
                    if(!modifying) {
                        Value ret = new FormattedTextValue(team.getSuffix());
                        return (_c, _t) -> ret;
                    }

                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_property' requires a string or formatted text as the third argument for the property " + propertyVal.getString());

                    Text suffix;

                    if(settingVal instanceof FormattedTextValue) {
                        suffix = ((FormattedTextValue) settingVal).getText();
                    } else {
                        suffix = new LiteralText(settingVal.getString());
                    }

                    team.setSuffix(suffix);
                    break;
                default:
                    throw new InternalExpressionException("team property '" + propertyVal.getString() + "' is not a valid property");
            }
            return LazyValue.TRUE;
        });

        expression.addLazyFunction("bossbar", -1, (c, t, lv) ->
        {
            BossBarManager bossBarManager = ((CarpetContext)c).s.getMinecraftServer().getBossBarManager();
            if(lv.size() > 3) throw new InternalExpressionException("'bossbar' accepts max three arguments");

            if(lv.size() == 0) {
                Value ret = ListValue.wrap(bossBarManager.getAll().stream().map(CommandBossBar::getId).map(Identifier::toString).map(StringValue::of).collect(Collectors.toList()));
                return (_c, _t) -> ret;
            }

            String id = lv.get(0).evalValue(c).getString();
            Identifier identifier;
            try {
                identifier = new Identifier(id);
            } catch (InvalidIdentifierException invalidIdentifierException) {
                return LazyValue.NULL;
            }

            if(lv.size() == 1) {
                if(bossBarManager.get(identifier) != null) return LazyValue.FALSE;
                Value ret = StringValue.of(bossBarManager.add(identifier,new LiteralText(id)).getId().toString());
                return (_c, _t) -> ret;
            }

            String property = lv.get(1).evalValue(c).getString();

            CommandBossBar bossBar = bossBarManager.get(identifier);
            if(bossBar == null) return LazyValue.NULL;

            Value propertyValue = (lv.size() == 3)?lv.get(2).evalValue(c):null;

            switch (property) {
                case "color":
                    if(propertyValue == null) {
                        BossBar.Color color = (bossBar).getColor();
                        if(color == null) return LazyValue.NULL;
                        return (_c, _t) -> StringValue.of(color.getName());
                    }

                    BossBar.Color color = BossBar.Color.byName(propertyValue.getString());
                    if(color == null) return LazyValue.NULL;
                    bossBar.setColor(BossBar.Color.byName(propertyValue.getString()));
                    return LazyValue.TRUE;
                case "max":
                    if(propertyValue == null) return (_c, _t) -> NumericValue.of(bossBar.getMaxValue());

                    if(!(propertyValue instanceof NumericValue)) throw new InternalExpressionException("'bossbar' requires a number as the value for the property " + property);
                    bossBar.setMaxValue(((NumericValue) propertyValue).getInt());
                    return LazyValue.TRUE;
                case "name":
                    if(propertyValue == null) return (_c, _t) -> new FormattedTextValue(bossBar.getName());

                    if(propertyValue instanceof FormattedTextValue) {
                        bossBar.setName(((FormattedTextValue) propertyValue).getText());
                    } else {
                        bossBar.setName(new LiteralText(propertyValue.getString()));
                    }
                    return LazyValue.TRUE;
                case "add_player":
                    if(propertyValue == null) throw new InternalExpressionException("Bossbar property " + property + " can't be queried, add a third parameter");

                    if(propertyValue instanceof ListValue) {
                        ((ListValue) propertyValue).getItems().forEach((v)->{
                            ServerPlayerEntity player = EntityValue.getPlayerByValue(((CarpetContext)c).s.getMinecraftServer(),propertyValue);
                            if(player != null) bossBar.addPlayer(player);
                        });
                        return LazyValue.TRUE;
                    }

                    ServerPlayerEntity player = EntityValue.getPlayerByValue(((CarpetContext) c).s.getMinecraftServer(), propertyValue);
                    if(player != null) {
                        bossBar.addPlayer(player);
                        return LazyValue.TRUE;
                    }
                    return LazyValue.FALSE;
                case "players":
                    if(propertyValue == null) {
                        return (_c, _t) -> ListValue.wrap(bossBar.getPlayers().stream().map(EntityValue::new).collect(Collectors.toList()));
                    }

                    if(propertyValue instanceof ListValue) {
                        bossBar.clearPlayers();
                        ((ListValue) propertyValue).getItems().forEach((v) -> {
                            ServerPlayerEntity p = EntityValue.getPlayerByValue(((CarpetContext) c).s.getMinecraftServer(), v);
                            if (p != null) bossBar.addPlayer(p);
                        });
                        return LazyValue.TRUE;
                    }


                    ServerPlayerEntity p = EntityValue.getPlayerByValue(((CarpetContext) c).s.getMinecraftServer(), propertyValue);
                    bossBar.clearPlayers();
                    if (p != null) {
                        bossBar.addPlayer(p);
                        return LazyValue.TRUE;
                    }
                    return LazyValue.FALSE;
                case "style":
                    if(propertyValue == null) {
                        return (_c, _t) -> StringValue.of(bossBar.getOverlay().getName());
                    }

                    BossBar.Style style = BossBar.Style.byName(propertyValue.getString());
                    if(style == null) throw new InternalExpressionException("'" + propertyValue.getString() + "' is not a valid value for property " + property);
                    bossBar.setOverlay(style);
                    return LazyValue.TRUE;
                case "value":
                    if(propertyValue == null) return (_c, _t) -> NumericValue.of(bossBar.getValue());

                    if(!(propertyValue instanceof NumericValue)) throw new InternalExpressionException("'bossbar' requires a number as the value for the property " + property);
                    bossBar.setValue(((NumericValue) propertyValue).getInt());
                    return LazyValue.TRUE;
                case "visible":
                    if(propertyValue == null) return (_c, _t) -> new NumericValue(bossBar.isVisible());

                    bossBar.setVisible(propertyValue.getBoolean());
                    return LazyValue.TRUE;
                case "remove":
                    bossBarManager.remove(bossBar);
                    return LazyValue.TRUE;
                default:
                    throw new InternalExpressionException("Unknown bossbar property " + property);
            }
        });
    }
}

