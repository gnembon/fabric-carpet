package carpet.script.api;

import carpet.script.CarpetContext;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.EntityValue;
import carpet.script.value.ListValue;
import carpet.script.value.NullValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.Value;
import net.minecraft.entity.Entity;
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
            if (objective == null) throw new InternalExpressionException("Unknown objective: "+objectiveName);
            if (lv.size()==1)
            {
                Value ret = ListValue.wrap(scoreboard.getAllPlayerScores(objective).stream().map(s -> new StringValue(s.getPlayerName())).collect(Collectors.toList()));
                return (_c, _t) -> ret;
            }
            String key = getScoreboardKeyFromValue(lv.get(1).evalValue(c));
            if (!scoreboard.playerHasObjective(key, objective) && lv.size()==2)
                return LazyValue.NULL;
            ScoreboardPlayerScore scoreboardPlayerScore = scoreboard.getPlayerScore(key, objective);
            Value retval = new NumericValue(scoreboardPlayerScore.getScore());
            if (lv.size() > 2)
            {
                scoreboardPlayerScore.setScore(NumericValue.asNumber(lv.get(2).evalValue(c)).getInt());
            }
            return (_c, _t) -> retval;
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
                    throw new InternalExpressionException("Unknown scoreboard criterion: "+critetionName);
                }
            }

            ScoreboardObjective objective = scoreboard.getObjective(objectiveName);
            if (objective != null)
                return LazyValue.FALSE;

            scoreboard.addObjective(objectiveName, criterion, new LiteralText(objectiveName), criterion.getCriterionType());
            return LazyValue.TRUE;
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
            if (objective == null) throw new InternalExpressionException("Objective doesn't exist: "+objectiveString);
            scoreboard.setObjectiveSlot(slot, objective);
            return (_c, _t) -> new NumericValue(slot);
        });

        expression.addLazyFunction("team_list", -1, (c, t, lv) ->
        {
            if(lv.size() > 1) throw new InternalExpressionException("'team_list' requires zero or one parameters");
            CarpetContext cc = (CarpetContext)c;
            ServerScoreboard scoreboard = cc.s.getMinecraftServer().getScoreboard();
            if(lv.size() == 0) {
                Value ret = ListValue.wrap(scoreboard.getTeamNames().stream().map(StringValue::new).collect(Collectors.toList()));
                return (_c, _t) -> ret;
            } else if(lv.size() == 1) {
                Value teamVal = lv.get(0).evalValue(c);
                if(teamVal instanceof StringValue) {
                    Team team = scoreboard.getTeam(lv.get(0).evalValue(c).getString());
                    if(team == null) return LazyValue.NULL;
                    Value ret = ListValue.wrap(team.getPlayerList().stream().map(StringValue::new).collect(Collectors.toList()));
                    return (_c, _t) -> ret;
                } else {
                    throw new InternalExpressionException("'team_list' requires a string as the first argument");
                }
            } else {
                return LazyValue.NULL;
            }
        });


        expression.addLazyFunction("team_add", -1, (c, t, lv) ->
        {
            if(!(lv.size() < 3 && lv.size() > 0)) throw new InternalExpressionException("'team_add' requires one or two parameters");

            CarpetContext cc = (CarpetContext)c;
            ServerScoreboard scoreboard = cc.s.getMinecraftServer().getScoreboard();
            Value teamVal = lv.get(0).evalValue(c);
            if(!(teamVal instanceof StringValue)) throw new InternalExpressionException("'team_add' requires a string as the first argument");

            String teamName = teamVal.getString();

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
                String player;
                if(playerVal instanceof StringValue) {
                    player = playerVal.getString();
                } else if(playerVal instanceof EntityValue && ((EntityValue) playerVal).getEntity() instanceof ServerPlayerEntity) {
                    player = ((EntityValue) playerVal).getEntity().getEntityName();
                } else {
                    throw new InternalExpressionException("'team_add' requires a string or a player as the second argument");
                }

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
            if(!(teamVal instanceof StringValue)) throw new InternalExpressionException("'team_remove' requires a string as the first argument");

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
            String player;
            if(playerVal instanceof StringValue) {
                player = playerVal.getString();
            } else if(playerVal instanceof EntityValue && ((EntityValue) playerVal).getEntity() instanceof ServerPlayerEntity) {
                player = ((EntityValue) playerVal).getEntity().getEntityName();
            } else {
                throw new InternalExpressionException("'team_leave' requires a string or a player as the first argument");
            }
            Value ret = new NumericValue(scoreboard.clearPlayerTeam(player));
            return (_c, _t) -> ret;
        });

        expression.addLazyFunction("team_empty", 1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerScoreboard scoreboard = cc.s.getMinecraftServer().getScoreboard();
            Value teamVal = lv.get(0).evalValue(c);

            if(!(teamVal instanceof StringValue)) throw new InternalExpressionException("'team_add' requires a string as the first argument");

            Team team = scoreboard.getTeam(teamVal.getString());

            if(team == null) return LazyValue.NULL;

            Value ret = new NumericValue(team.getPlayerList().size());
            if(team.getPlayerList().size() != 0) {
                team.getPlayerList().forEach((player) -> scoreboard.removePlayerFromTeam(player, team));
            }
            return (_c, _t) -> ret;
        });

        expression.addLazyFunction("team_modify", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerScoreboard scoreboard = cc.s.getMinecraftServer().getScoreboard();

            if(lv.size() < 2 || lv.size() > 3) throw new InternalExpressionException("'team_modify' requires two or three arguments");

            Value teamVal = lv.get(0).evalValue(c);
            Value propertyVal = lv.get(1).evalValue(c);

            Value settingVal = null;
            boolean modifying = false;
            if(lv.size() == 3) {
                modifying = true;
                settingVal = lv.get(2).evalValue(c);
            }

            if(!(teamVal instanceof StringValue)) throw new InternalExpressionException("'team_modify' requires a string as the first argument");

            Team team = scoreboard.getTeam(teamVal.getString());
            if(team == null) return LazyValue.NULL;

            if(!(propertyVal instanceof StringValue)) throw new InternalExpressionException("'team_modify' requires a string as the second argument");

            switch (propertyVal.getString()) {
                case "collisionRule":
                    if(!modifying) {
                        Value ret = new StringValue(team.getCollisionRule().name);
                        return (_c, _t) -> ret;
                    }

                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_modify' requires a string as the third argument for the property " + propertyVal.getString());

                    AbstractTeam.CollisionRule collisionRule = AbstractTeam.CollisionRule.getRule(settingVal.getString());
                    if(collisionRule == null) throw new InternalExpressionException("Unknown value for " + propertyVal.getString() + ": " + settingVal.getString());
                    team.setCollisionRule(collisionRule);
                    break;
                case "color":
                    if(!modifying) {
                        Value ret = new StringValue(team.getColor().getName());
                        return (_c, _t) -> ret;
                    }

                    if(!(settingVal instanceof StringValue)) throw new InternalExpressionException("'team_modify' requires a string as the third argument for the property " + propertyVal.getString());

                    Formatting color = Formatting.byName(settingVal.getString().toUpperCase());
                    if(color == null || !color.isColor()) throw new InternalExpressionException("Unknown value for " + propertyVal.getString() + ": " + settingVal.getString());
                    team.setColor(color);

                    break;
                case "deathMessageVisibility":
                    if(!modifying) {
                        Value ret = new StringValue(team.getDeathMessageVisibilityRule().name);
                        return (_c, _t) -> ret;
                    }

                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_modify' requires a string as the third argument for the property " + propertyVal.getString());

                    AbstractTeam.VisibilityRule deathMessageVisibility = AbstractTeam.VisibilityRule.getRule(settingVal.getString());
                    if(deathMessageVisibility == null) throw new InternalExpressionException("Unknown value for " + propertyVal.getString() + ": " + settingVal.getString());
                    team.setDeathMessageVisibilityRule(deathMessageVisibility);

                    break;
                case "displayName":
                    if(!modifying) {
                        Value ret = new FormattedTextValue(team.getDisplayName());
                        return (_c, _t) -> ret;
                    }

                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_modify' requires a string or formatted text as the third argument for the property " + propertyVal.getString());

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

                    if(!(settingVal instanceof NumericValue)) throw  new InternalExpressionException("'team_modify' requires a boolean as the third argument for the property " + propertyVal.getString());

                    boolean friendlyFire = settingVal.getBoolean();
                    team.setFriendlyFireAllowed(friendlyFire);
                    break;
                case "nametagVisibility":
                    if(!modifying) {
                        Value ret = new StringValue(team.getNameTagVisibilityRule().name);
                        return (_c, _t) -> ret;
                    }

                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_modify' requires a string as the third argument for the property " + propertyVal.getString());

                    AbstractTeam.VisibilityRule nametagVisibility = AbstractTeam.VisibilityRule.getRule(settingVal.getString());
                    if(nametagVisibility == null) throw new InternalExpressionException("Unknown value for " + propertyVal.getString() + ": " + settingVal.getString());
                    team.setNameTagVisibilityRule(nametagVisibility);
                    break;
                case "prefix":
                    if(!modifying) {
                        Value ret = new FormattedTextValue(team.getPrefix());
                        return (_c, _t) -> ret;
                    }

                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_modify' requires a string or formatted text as the third argument for the property " + propertyVal.getString());

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

                    if(!(settingVal instanceof NumericValue)) throw  new InternalExpressionException("'team_modify' requires a boolean as the third argument for the property " + propertyVal.getString());

                    boolean seeFriendlyInvisibles = settingVal.getBoolean();
                    team.setShowFriendlyInvisibles(seeFriendlyInvisibles);

                    break;
                case "suffix":
                    if(!modifying) {
                        Value ret = new FormattedTextValue(team.getSuffix());
                        return (_c, _t) -> ret;
                    }

                    if(!(settingVal instanceof StringValue)) throw  new InternalExpressionException("'team_modify' requires a string or formatted text as the third argument for the property " + propertyVal.getString());

                    Text suffix;

                    if(settingVal instanceof FormattedTextValue) {
                        suffix = ((FormattedTextValue) settingVal).getText();
                    } else {
                        suffix = new LiteralText(settingVal.getString());
                    }

                    team.setSuffix(suffix);
                    break;
                default:
                    throw new InternalExpressionException("team property " + propertyVal.getString() + " is not a valid property");
            }
            return LazyValue.TRUE;
        });
    }
}
