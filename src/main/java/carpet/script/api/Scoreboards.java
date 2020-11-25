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
import carpet.script.value.Value;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.text.LiteralText;

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
                criterion = ScoreboardCriterion.getOrCreateStatCriterion(critetionName).orElse(null);
                if (criterion==null)
                {
                    throw new InternalExpressionException("Unknown scoreboard criterion: "+critetionName);
                }
            }

            ScoreboardObjective objective = scoreboard.getObjective(objectiveName);
            if (objective != null)
                return LazyValue.FALSE;

            scoreboard.addObjective(objectiveName, criterion, new LiteralText(objectiveName), criterion.getDefaultRenderType());
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
            if (objective == null) return LazyValue.NULL;
            scoreboard.setObjectiveSlot(slot, objective);
            return (_c, _t) -> new NumericValue(slot);
        });
    }
}
