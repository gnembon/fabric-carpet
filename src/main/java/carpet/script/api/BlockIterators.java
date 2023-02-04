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
    public static void apply(final Expression expression)
    {
        // lazy cause of lazy expression
        expression.addLazyFunction("scan", (c, t, llv) ->
        {
            if (llv.size() < 3)
            {
                throw new InternalExpressionException("'scan' needs many more arguments");
            }
            final List<Value> lv = Fluff.AbstractFunction.unpackLazy(llv.subList(0, llv.size() - 1), c, Context.NONE);
            final CarpetContext cc = (CarpetContext) c;
            final BlockArgument centerLocator = BlockArgument.findIn(cc, lv, 0);
            Vector3Argument rangeLocator = Vector3Argument.findIn(lv, centerLocator.offset);
            final BlockPos center = centerLocator.block.getPos();
            final Vec3i range;

            if (rangeLocator.fromBlock)
            {
                range = new Vec3i(
                        abs(rangeLocator.vec.x - center.getX()),
                        abs(rangeLocator.vec.y - center.getY()),
                        abs(rangeLocator.vec.z - center.getZ())
                );
            }
            else
            {
                range = new Vec3i(abs(rangeLocator.vec.x), abs(rangeLocator.vec.y), abs(rangeLocator.vec.z));
            }
            Vec3i upperRange = range;
            if (lv.size() > rangeLocator.offset + 1) // +1 cause we still need the expression
            {
                rangeLocator = Vector3Argument.findIn(lv, rangeLocator.offset);
                if (rangeLocator.fromBlock)
                {
                    upperRange = new Vec3i(
                            abs(rangeLocator.vec.x - center.getX()),
                            abs(rangeLocator.vec.y - center.getY()),
                            abs(rangeLocator.vec.z - center.getZ())
                    );
                }
                else
                {
                    upperRange = new Vec3i(abs(rangeLocator.vec.x), abs(rangeLocator.vec.y), abs(rangeLocator.vec.z));
                }
            }
            if (llv.size() != rangeLocator.offset + 1)
            {
                throw new InternalExpressionException("'scan' takes two, or three block positions, and an expression: " + lv.size() + " " + rangeLocator.offset);
            }
            final LazyValue expr = llv.get(rangeLocator.offset);

            final int cx = center.getX();
            final int cy = center.getY();
            final int cz = center.getZ();
            final int xrange = range.getX();
            final int yrange = range.getY();
            final int zrange = range.getZ();
            final int xprange = upperRange.getX();
            final int yprange = upperRange.getY();
            final int zprange = upperRange.getZ();

            //saving outer scope
            final LazyValue _x = c.getVariable("_x");
            final LazyValue _y = c.getVariable("_y");
            final LazyValue _z = c.getVariable("_z");
            final LazyValue __ = c.getVariable("_");
            int sCount = 0;
            outer:
            for (int y = cy - yrange; y <= cy + yprange; y++)
            {
                final int yFinal = y;
                c.setVariable("_y", (c_, t_) -> new NumericValue(yFinal).bindTo("_y"));
                for (int x = cx - xrange; x <= cx + xprange; x++)
                {
                    final int xFinal = x;
                    c.setVariable("_x", (c_, t_) -> new NumericValue(xFinal).bindTo("_x"));
                    for (int z = cz - zrange; z <= cz + zprange; z++)
                    {
                        final int zFinal = z;

                        c.setVariable("_z", (c_, t_) -> new NumericValue(zFinal).bindTo("_z"));
                        final Value blockValue = BlockValue.fromCoords(((CarpetContext) c), xFinal, yFinal, zFinal).bindTo("_");
                        c.setVariable("_", (cc_, t_c) -> blockValue);
                        Value result;
                        try
                        {
                            result = expr.evalValue(c, t);
                        }
                        catch (final ContinueStatement notIgnored)
                        {
                            result = notIgnored.retval;
                        }
                        catch (final BreakStatement notIgnored)
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
            c.setVariable("_x", _x);
            c.setVariable("_y", _y);
            c.setVariable("_z", _z);
            c.setVariable("_", __);
            final int finalSCount = sCount;
            return (c_, t_) -> new NumericValue(finalSCount);
        });

        // must be lazy
        expression.addLazyFunction("volume", (c, t, llv) ->
        {
            final CarpetContext cc = (CarpetContext) c;
            if (llv.size() < 3)
            {
                throw new InternalExpressionException("'volume' needs many more arguments");
            }
            final List<Value> lv = Fluff.AbstractFunction.unpackLazy(llv.subList(0, llv.size() - 1), c, Context.NONE);

            final BlockArgument pos1Locator = BlockArgument.findIn(cc, lv, 0);
            final BlockArgument pos2Locator = BlockArgument.findIn(cc, lv, pos1Locator.offset);
            final BlockPos pos1 = pos1Locator.block.getPos();
            final BlockPos pos2 = pos2Locator.block.getPos();

            final int x1 = pos1.getX();
            final int y1 = pos1.getY();
            final int z1 = pos1.getZ();
            final int x2 = pos2.getX();
            final int y2 = pos2.getY();
            final int z2 = pos2.getZ();
            final int minx = min(x1, x2);
            final int miny = min(y1, y2);
            final int minz = min(z1, z2);
            final int maxx = max(x1, x2);
            final int maxy = max(y1, y2);
            final int maxz = max(z1, z2);
            final LazyValue expr = llv.get(pos2Locator.offset);

            //saving outer scope
            final LazyValue _x = c.getVariable("_x");
            final LazyValue _y = c.getVariable("_y");
            final LazyValue _z = c.getVariable("_z");
            final LazyValue __ = c.getVariable("_");
            int sCount = 0;
            outer:
            for (int y = miny; y <= maxy; y++)
            {
                final int yFinal = y;
                c.setVariable("_y", (c_, t_) -> new NumericValue(yFinal).bindTo("_y"));
                for (int x = minx; x <= maxx; x++)
                {
                    final int xFinal = x;
                    c.setVariable("_x", (c_, t_) -> new NumericValue(xFinal).bindTo("_x"));
                    for (int z = minz; z <= maxz; z++)
                    {
                        final int zFinal = z;
                        c.setVariable("_z", (c_, t_) -> new NumericValue(zFinal).bindTo("_z"));
                        final Value blockValue = BlockValue.fromCoords(((CarpetContext) c), xFinal, yFinal, zFinal).bindTo("_");
                        c.setVariable("_", (cc_, t_c) -> blockValue);
                        Value result;
                        try
                        {
                            result = expr.evalValue(c, t);
                        }
                        catch (final ContinueStatement notIgnored)
                        {
                            result = notIgnored.retval;
                        }
                        catch (final BreakStatement notIgnored)
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
            c.setVariable("_x", _x);
            c.setVariable("_y", _y);
            c.setVariable("_z", _z);
            c.setVariable("_", __);
            final int finalSCount = sCount;
            return (c_, t_) -> new NumericValue(finalSCount);
        });

        expression.addContextFunction("neighbours", -1, (c, t, lv) ->
        {
            final BlockPos center = BlockArgument.findIn((CarpetContext) c, lv, 0).block.getPos();
            final ServerLevel world = ((CarpetContext) c).level();

            final List<Value> neighbours = new ArrayList<>();
            neighbours.add(new BlockValue(null, world, center.above()));
            neighbours.add(new BlockValue(null, world, center.below()));
            neighbours.add(new BlockValue(null, world, center.north()));
            neighbours.add(new BlockValue(null, world, center.south()));
            neighbours.add(new BlockValue(null, world, center.east()));
            neighbours.add(new BlockValue(null, world, center.west()));
            return ListValue.wrap(neighbours);
        });

        expression.addContextFunction("rect", -1, (c, t, lv) ->
        {
            final CarpetContext cc = (CarpetContext) c;
            final int cx;
            final int cy;
            final int cz;
            final int sminx;
            final int sminy;
            final int sminz;
            final int smaxx;
            final int smaxy;
            final int smaxz;
            final BlockArgument cposLocator = BlockArgument.findIn(cc, lv, 0);
            final BlockPos cpos = cposLocator.block.getPos();
            cx = cpos.getX();
            cy = cpos.getY();
            cz = cpos.getZ();
            if (lv.size() > cposLocator.offset)
            {
                final Vector3Argument diffLocator = Vector3Argument.findIn(lv, cposLocator.offset);
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
                    final Vector3Argument posDiff = Vector3Argument.findIn(lv, diffLocator.offset);
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
                    final Value r = BlockValue.fromCoords(cc, x, y, z);
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
            final CarpetContext cc = (CarpetContext) c;

            final BlockArgument cposLocator = BlockArgument.findIn((CarpetContext) c, lv, 0);
            final BlockPos cpos = cposLocator.block.getPos();

            final int cx;
            final int cy;
            final int cz;
            final int width;
            final int height;
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
            catch (final ClassCastException ignored)
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
                        final Value block = BlockValue.fromCoords(cc, cx + (curradius - abs(curpos - 2 * curradius)), cy, cz - curradius + abs(abs(curpos - curradius) % (4 * curradius) - 2 * curradius));
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

                        final Value block = BlockValue.fromCoords(cc, cx + (curradius - abs(curpos - 2 * curradius)), cy + curheight, cz - curradius + abs(abs(curpos - curradius) % (4 * curradius) - 2 * curradius));
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
