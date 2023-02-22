package carpet.script.api;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Fluff;
import carpet.script.LazyValue;
import carpet.script.argument.BlockArgument;
import carpet.script.argument.Vector3Argument;
import carpet.script.exception.BreakStatement;
import carpet.script.exception.ContinueStatement;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BlockValue;
import carpet.script.value.LazyListValue;
import carpet.script.value.ListValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class BlockIterators
{
    public static void apply(Expression expression)
    {
        // lazy cause of lazy expression
        expression.addLazyFunction("scan", (c, t, llv) ->
        {
            if (llv.size() < 3)
            {
                throw new InternalExpressionException("'scan' needs many more arguments");
            }
            List<Value> lv = Fluff.AbstractFunction.unpackLazy(llv.subList(0, llv.size() - 1), c, Context.NONE);
            CarpetContext cc = (CarpetContext) c;
            BlockArgument centerLocator = BlockArgument.findIn(cc, lv, 0);
            Vector3Argument rangeLocator = Vector3Argument.findIn(lv, centerLocator.offset);
            BlockPos center = centerLocator.block.getPos();
            Vec3i range;

            if (rangeLocator.fromBlock)
            {
                range = new Vec3i(
                        Mth.floor(abs(rangeLocator.vec.x - center.getX())),
                        Mth.floor(abs(rangeLocator.vec.y - center.getY())),
                        Mth.floor(abs(rangeLocator.vec.z - center.getZ()))
                );
            }
            else
            {
                range = new Vec3i(
                        Mth.floor(abs(rangeLocator.vec.x)),
                        Mth.floor(abs(rangeLocator.vec.y)),
                        Mth.floor(abs(rangeLocator.vec.z))
                );
            }
            Vec3i upperRange = range;
            if (lv.size() > rangeLocator.offset + 1) // +1 cause we still need the expression
            {
                rangeLocator = Vector3Argument.findIn(lv, rangeLocator.offset);
                if (rangeLocator.fromBlock)
                {
                    upperRange = new Vec3i(
                            Mth.floor(abs(rangeLocator.vec.x - center.getX())),
                            Mth.floor(abs(rangeLocator.vec.y - center.getY())),
                            Mth.floor(abs(rangeLocator.vec.z - center.getZ()))
                    );
                }
                else
                {
                    upperRange = new Vec3i(
                            Mth.floor(abs(rangeLocator.vec.x)),
                            Mth.floor(abs(rangeLocator.vec.y)),
                            Mth.floor(abs(rangeLocator.vec.z)));
                }
            }
            if (llv.size() != rangeLocator.offset + 1)
            {
                throw new InternalExpressionException("'scan' takes two, or three block positions, and an expression: " + lv.size() + " " + rangeLocator.offset);
            }
            LazyValue expr = llv.get(rangeLocator.offset);

            int cx = center.getX();
            int cy = center.getY();
            int cz = center.getZ();
            int xrange = range.getX();
            int yrange = range.getY();
            int zrange = range.getZ();
            int xprange = upperRange.getX();
            int yprange = upperRange.getY();
            int zprange = upperRange.getZ();

            //saving outer scope
            LazyValue xVal = c.getVariable("_x");
            LazyValue yVal = c.getVariable("_y");
            LazyValue zVal = c.getVariable("_z");
            LazyValue defaultVal = c.getVariable("_");
            int sCount = 0;
            outer:
            for (int y = cy - yrange; y <= cy + yprange; y++)
            {
                int yFinal = y;
                c.setVariable("_y", (ct, tt) -> new NumericValue(yFinal).bindTo("_y"));
                for (int x = cx - xrange; x <= cx + xprange; x++)
                {
                    int xFinal = x;
                    c.setVariable("_x", (ct, tt) -> new NumericValue(xFinal).bindTo("_x"));
                    for (int z = cz - zrange; z <= cz + zprange; z++)
                    {
                        int zFinal = z;

                        c.setVariable("_z", (ct, tt) -> new NumericValue(zFinal).bindTo("_z"));
                        Value blockValue = BlockValue.fromCoords(((CarpetContext) c), xFinal, yFinal, zFinal).bindTo("_");
                        c.setVariable("_", (ct, tt) -> blockValue);
                        Value result;
                        try
                        {
                            result = expr.evalValue(c, t);
                        }
                        catch (ContinueStatement notIgnored)
                        {
                            result = notIgnored.retval;
                        }
                        catch (BreakStatement notIgnored)
                        {
                            break outer;
                        }
                        if (t != Context.VOID && result.getBoolean())
                        {
                            sCount += 1;
                        }
                    }
                }
            }
            //restoring outer scope
            c.setVariable("_x", xVal);
            c.setVariable("_y", yVal);
            c.setVariable("_z", zVal);
            c.setVariable("_", defaultVal);
            int finalSCount = sCount;
            return (ct, tt) -> new NumericValue(finalSCount);
        });

        // must be lazy
        expression.addLazyFunction("volume", (c, t, llv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            if (llv.size() < 3)
            {
                throw new InternalExpressionException("'volume' needs many more arguments");
            }
            List<Value> lv = Fluff.AbstractFunction.unpackLazy(llv.subList(0, llv.size() - 1), c, Context.NONE);

            BlockArgument pos1Locator = BlockArgument.findIn(cc, lv, 0);
            BlockArgument pos2Locator = BlockArgument.findIn(cc, lv, pos1Locator.offset);
            BlockPos pos1 = pos1Locator.block.getPos();
            BlockPos pos2 = pos2Locator.block.getPos();

            int x1 = pos1.getX();
            int y1 = pos1.getY();
            int z1 = pos1.getZ();
            int x2 = pos2.getX();
            int y2 = pos2.getY();
            int z2 = pos2.getZ();
            int minx = min(x1, x2);
            int miny = min(y1, y2);
            int minz = min(z1, z2);
            int maxx = max(x1, x2);
            int maxy = max(y1, y2);
            int maxz = max(z1, z2);
            LazyValue expr = llv.get(pos2Locator.offset);

            //saving outer scope
            LazyValue xVal = c.getVariable("_x");
            LazyValue yVal = c.getVariable("_y");
            LazyValue zVal = c.getVariable("_z");
            LazyValue defaultVal = c.getVariable("_");
            int sCount = 0;
            outer:
            for (int y = miny; y <= maxy; y++)
            {
                int yFinal = y;
                c.setVariable("_y", (ct, tt) -> new NumericValue(yFinal).bindTo("_y"));
                for (int x = minx; x <= maxx; x++)
                {
                    int xFinal = x;
                    c.setVariable("_x", (ct, tt) -> new NumericValue(xFinal).bindTo("_x"));
                    for (int z = minz; z <= maxz; z++)
                    {
                        int zFinal = z;
                        c.setVariable("_z", (ct, tt) -> new NumericValue(zFinal).bindTo("_z"));
                        Value blockValue = BlockValue.fromCoords(((CarpetContext) c), xFinal, yFinal, zFinal).bindTo("_");
                        c.setVariable("_", (ct, tt) -> blockValue);
                        Value result;
                        try
                        {
                            result = expr.evalValue(c, t);
                        }
                        catch (ContinueStatement notIgnored)
                        {
                            result = notIgnored.retval;
                        }
                        catch (BreakStatement notIgnored)
                        {
                            break outer;
                        }
                        if (t != Context.VOID && result.getBoolean())
                        {
                            sCount += 1;
                        }
                    }
                }
            }
            //restoring outer scope
            c.setVariable("_x", xVal);
            c.setVariable("_y", yVal);
            c.setVariable("_z", zVal);
            c.setVariable("_", defaultVal);
            int finalSCount = sCount;
            return (ct, tt) -> new NumericValue(finalSCount);
        });

        expression.addContextFunction("neighbours", -1, (c, t, lv) ->
        {
            BlockPos center = BlockArgument.findIn((CarpetContext) c, lv, 0).block.getPos();
            ServerLevel world = ((CarpetContext) c).level();

            List<Value> neighbours = new ArrayList<>();
            neighbours.add(new BlockValue(world, center.above()));
            neighbours.add(new BlockValue(world, center.below()));
            neighbours.add(new BlockValue(world, center.north()));
            neighbours.add(new BlockValue(world, center.south()));
            neighbours.add(new BlockValue(world, center.east()));
            neighbours.add(new BlockValue(world, center.west()));
            return ListValue.wrap(neighbours);
        });

        expression.addContextFunction("rect", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            int cx;
            int cy;
            int cz;
            int sminx;
            int sminy;
            int sminz;
            int smaxx;
            int smaxy;
            int smaxz;
            BlockArgument cposLocator = BlockArgument.findIn(cc, lv, 0);
            BlockPos cpos = cposLocator.block.getPos();
            cx = cpos.getX();
            cy = cpos.getY();
            cz = cpos.getZ();
            if (lv.size() > cposLocator.offset)
            {
                Vector3Argument diffLocator = Vector3Argument.findIn(lv, cposLocator.offset);
                if (diffLocator.fromBlock)
                {
                    sminx = Mth.floor(abs(diffLocator.vec.x - cx));
                    sminy = Mth.floor(abs(diffLocator.vec.y - cx));
                    sminz = Mth.floor(abs(diffLocator.vec.z - cx));
                }
                else
                {
                    sminx = Mth.floor(abs(diffLocator.vec.x));
                    sminy = Mth.floor(abs(diffLocator.vec.y));
                    sminz = Mth.floor(abs(diffLocator.vec.z));
                }
                if (lv.size() > diffLocator.offset)
                {
                    Vector3Argument posDiff = Vector3Argument.findIn(lv, diffLocator.offset);
                    if (posDiff.fromBlock)
                    {
                        smaxx = Mth.floor(abs(posDiff.vec.x - cx));
                        smaxy = Mth.floor(abs(posDiff.vec.y - cx));
                        smaxz = Mth.floor(abs(posDiff.vec.z - cx));
                    }
                    else
                    {
                        smaxx = Mth.floor(abs(posDiff.vec.x));
                        smaxy = Mth.floor(abs(posDiff.vec.y));
                        smaxz = Mth.floor(abs(posDiff.vec.z));
                    }
                }
                else
                {
                    smaxx = sminx;
                    smaxy = sminy;
                    smaxz = sminz;
                }
            }
            else
            {
                sminx = 1;
                sminy = 1;
                sminz = 1;
                smaxx = 1;
                smaxy = 1;
                smaxz = 1;
            }

            return new LazyListValue()
            {
                final int minx = cx - sminx;
                final int miny = cy - sminy;
                final int minz = cz - sminz;
                final int maxx = cx + smaxx;
                final int maxy = cy + smaxy;
                final int maxz = cz + smaxz;

                int x;
                int y;
                int z;

                {
                    reset();
                }

                @Override
                public boolean hasNext()
                {
                    return y <= maxy;
                }

                @Override
                public Value next()
                {
                    Value r = BlockValue.fromCoords(cc, x, y, z);
                    //possibly reroll context
                    x++;
                    if (x > maxx)
                    {
                        x = minx;
                        z++;
                        if (z > maxz)
                        {
                            z = minz;
                            y++;
                            // hasNext should fail if we went over
                        }
                    }

                    return r;
                }

                @Override
                public void fatality()
                {
                    // possibly return original x, y, z
                    super.fatality();
                }

                @Override
                public void reset()
                {
                    x = minx;
                    y = miny;
                    z = minz;
                }

                @Override
                public String getString()
                {
                    return String.format(Locale.ROOT, "rect[(%d,%d,%d),..,(%d,%d,%d)]", minx, miny, minz, maxx, maxy, maxz);
                }
            };
        });

        expression.addContextFunction("diamond", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;

            BlockArgument cposLocator = BlockArgument.findIn((CarpetContext) c, lv, 0);
            BlockPos cpos = cposLocator.block.getPos();

            int cx;
            int cy;
            int cz;
            int width;
            int height;
            try
            {
                cx = cpos.getX();
                cy = cpos.getY();
                cz = cpos.getZ();

                if (lv.size() == cposLocator.offset)
                {
                    return ListValue.of(
                            BlockValue.fromCoords(cc, cx, cy - 1, cz),
                            BlockValue.fromCoords(cc, cx, cy, cz),
                            BlockValue.fromCoords(cc, cx - 1, cy, cz),
                            BlockValue.fromCoords(cc, cx, cy, cz - 1),
                            BlockValue.fromCoords(cc, cx + 1, cy, cz),
                            BlockValue.fromCoords(cc, cx, cy, cz + 1),
                            BlockValue.fromCoords(cc, cx, cy + 1, cz)
                    );
                }
                else if (lv.size() == 1 + cposLocator.offset)
                {
                    width = (int) ((NumericValue) lv.get(cposLocator.offset)).getLong();
                    height = 0;
                }
                else if (lv.size() == 2 + cposLocator.offset)
                {
                    width = (int) ((NumericValue) lv.get(cposLocator.offset)).getLong();
                    height = (int) ((NumericValue) lv.get(cposLocator.offset + 1)).getLong();
                }
                else
                {
                    throw new InternalExpressionException("Incorrect number of arguments for 'diamond'");
                }
            }
            catch (ClassCastException ignored)
            {
                throw new InternalExpressionException("Attempted to pass a non-number to 'diamond'");
            }
            if (height == 0)
            {
                return new LazyListValue()
                {
                    int curradius;
                    int curpos;

                    {
                        reset();
                    }

                    @Override
                    public boolean hasNext()
                    {
                        return curradius <= width;
                    }

                    @Override
                    public Value next()
                    {
                        if (curradius == 0)
                        {
                            curradius = 1;
                            return BlockValue.fromCoords(cc, cx, cy, cz);
                        }
                        // x = 3-|i-6|
                        // z = |( (i-3)%12-6|-3
                        Value block = BlockValue.fromCoords(cc, cx + (curradius - abs(curpos - 2 * curradius)), cy, cz - curradius + abs(abs(curpos - curradius) % (4 * curradius) - 2 * curradius));
                        curpos++;
                        if (curpos >= curradius * 4)
                        {
                            curradius++;
                            curpos = 0;
                        }
                        return block;

                    }

                    @Override
                    public void reset()
                    {
                        curradius = 0;
                        curpos = 0;
                    }

                    @Override
                    public String getString()
                    {
                        return String.format(Locale.ROOT, "diamond[(%d,%d,%d),%d,0]", cx, cy, cz, width);
                    }
                };
            }
            else
            {
                return new LazyListValue()
                {
                    int curradius;
                    int curpos;
                    int curheight;

                    {
                        reset();
                    }

                    @Override
                    public boolean hasNext()
                    {
                        return curheight <= height;
                    }

                    @Override
                    public Value next()
                    {
                        if (curheight == -height || curheight == height)
                        {
                            return BlockValue.fromCoords(cc, cx, cy + curheight++, cz);
                        }
                        if (curradius == 0)
                        {
                            curradius++;
                            return BlockValue.fromCoords(cc, cx, cy + curheight, cz);
                        }
                        // x = 3-|i-6|
                        // z = |( (i-3)%12-6|-3

                        Value block = BlockValue.fromCoords(cc, cx + (curradius - abs(curpos - 2 * curradius)), cy + curheight, cz - curradius + abs(abs(curpos - curradius) % (4 * curradius) - 2 * curradius));
                        curpos++;
                        if (curpos >= curradius * 4)
                        {
                            curradius++;
                            curpos = 0;
                            if (curradius > width - abs(width * curheight / height))
                            {
                                curheight++;
                                curradius = 0;
                            }
                        }
                        return block;
                    }

                    @Override
                    public void reset()
                    {
                        curradius = 0;
                        curpos = 0;
                        curheight = -height;
                    }

                    @Override
                    public String getString()
                    {
                        return String.format(Locale.ROOT, "diamond[(%d,%d,%d),%d,%d]", cx, cy, cz, width, height);
                    }
                };
            }
        });
    }
}
