package carpet.script.utils;

import carpet.CarpetSettings;
import carpet.network.ServerNetworkHandler;
import carpet.script.CarpetContext;
import carpet.script.Expression;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BlockValue;
import carpet.script.value.EntityValue;
import carpet.script.value.ListValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.arguments.ParticleArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ShapeDispatcher
{
    private static final Map<String, ParticleEffect> particleCache = new HashMap<>();

    public static void sendShape(List<ServerPlayerEntity> players, ExpiringShape shape, Map<String, Value> params)
    {
        Consumer<ServerPlayerEntity> alternative = null;
        CompoundTag tag = null;
        for (ServerPlayerEntity player : players)
        {
            if (ServerNetworkHandler.validCarpetPlayers.contains(player))
            {
                if (tag == null) tag = ExpiringShape.toTag(params);
                ServerNetworkHandler.sendCustomCommand(player,"scShape", tag);
            }
            else
            {
                if (alternative == null) alternative = shape.alternative();
                alternative.accept(player);
            }
        }
    }

    public static ParticleEffect getParticleData(String name)
    {
        ParticleEffect particle = particleCache.get(name);
        if (particle != null)
            return particle;
        try
        {
            particle = ParticleArgumentType.readParameters(new StringReader(name));
        }
        catch (CommandSyntaxException e)
        {
            throw new InternalExpressionException("No such particle: "+name);
        }
        particleCache.put(name, particle);
        return particle;
    }

    public static Map<String, Value> parseParams(List<Value> items)
    {
        // parses params from API function
        if (items.size()%2 == 1) throw new InternalExpressionException("Shape parameters list needs to be of even size");
        Map<String, Value> param = new HashMap<>();
        int i = 0;
        while(i < items.size())
        {
            String name = items.get(i).getString();
            Value val = items.get(i+1);
            param.put(name, val);
            i += 2;
        }
        return param;
    }

    public static ExpiringShape create(CarpetContext cc, String shapeType, Map<String, Value> userParams)
    {
        userParams.put("shape", new StringValue(shapeType));
        userParams.keySet().forEach(key -> {
            Param param = Param.of.get(key);
            if (param==null) throw new InternalExpressionException("Unknown feature for shape: "+key);
            userParams.put(key, param.validate(userParams, cc, userParams.get(key)));
        });
        Function<Map<String, Value>,ExpiringShape> factory = ExpiringShape.shapeProviders.get(shapeType);
        if (factory == null) throw new InternalExpressionException("Unknown shape: "+shapeType);
        return factory.apply(userParams);
    }

    // client
    public static ExpiringShape fromTag(CompoundTag tag)
    {
        Map<String, Value> options = new HashMap<>();
        for (String key : tag.getKeys())
        {
            Param decoder = Param.of.get(key);
            if (decoder==null)
            {
                CarpetSettings.LOG.info("Unknown parameter for shape: "+key);
                return null;
            }
            Value decodedValue = decoder.decode(tag.get(key));
            options.put(key, decodedValue);
        }
        Value shapeValue = options.get("shape");
        if (shapeValue == null)
        {
            CarpetSettings.LOG.info("Shape id missing in "+ String.join(", ", tag.getKeys()));
            return null;
        }
        Function<Map<String, Value>,ExpiringShape> factory = ExpiringShape.shapeProviders.get(shapeValue.getString());
        if (factory == null)
        {
            CarpetSettings.LOG.info("Unknown shape: "+shapeValue.getString());
            return null;
        }
        try
        {
            return factory.apply(options);
        }
        catch (InternalExpressionException exc)
        {
            CarpetSettings.LOG.info(exc.getMessage());
        }
        return null;
    }

    public abstract static class ExpiringShape
    {
        public static final Map<String, Function<Map<String, Value>,ExpiringShape>> shapeProviders = new HashMap<String, Function<Map<String, Value>, ExpiringShape>>(){{
            put("line", creator(Line::new));
            put("box", creator(Box::new));
            put("sphere", creator(Sphere::new));
        }};
        private static Function<Map<String, Value>,ExpiringShape> creator(Supplier<ExpiringShape> shapeFactory)
        {
            return o -> {
                ExpiringShape shape = shapeFactory.get();
                shape.fromOptions(o);
                return shape;
            };
        }

        float lineWidth;
        protected float r, g, b, a;
        protected int color;
        protected float fr, fg, fb, fa;
        protected int fillColor;
        protected int duration = 0;
        private int key;
        protected int followEntity;
        protected DimensionType entityDimension;


        protected ExpiringShape() { }

        public static CompoundTag toTag(Map<String, Value> params)
        {
            CompoundTag tag = new CompoundTag();
            params.forEach((k, v) -> {
                Tag valTag = Param.of.get(k).toTag(v);
                if (valTag != null) tag.put(k, valTag);
            });
            return tag;
        }

        private void fromOptions(Map<String, Value> options)
        {
            Set<String> optional = optionalParams();
            Set<String> required = requiredParams();
            Set<String> all = Sets.union(optional, required);
            if (!all.containsAll(options.keySet()))
                throw new InternalExpressionException("Received unexpected parameters for shape: "+Sets.difference(options.keySet(), all));
            if (!options.keySet().containsAll(required))
                throw new InternalExpressionException("Missing required parameters for shape: "+Sets.difference(required, options.keySet()));
            options.keySet().forEach(k ->{
                if (!this.canTake(k))
                    throw new InternalExpressionException("Parameter "+k+" doesn't apply for shape "+options.get("shape").getString());
            });
            init(options);
        }



        protected void init(Map<String, Value> options)
        {

            duration = NumericValue.asNumber(options.get("duration")).getInt();

            lineWidth = NumericValue.asNumber(options.getOrDefault("width", optional.get("width"))).getFloat();

            fillColor = NumericValue.asNumber(options.getOrDefault("fill", optional.get("fill"))).getInt();
            this.fr = (float)(fillColor >> 24 & 0xFF) / 255.0F;
            this.fg = (float)(fillColor >> 16 & 0xFF) / 255.0F;
            this.fb = (float)(fillColor >>  8 & 0xFF) / 255.0F;
            this.fa = (float)(fillColor & 0xFF) / 255.0F;

            color = NumericValue.asNumber(options.getOrDefault("color", optional.get("color"))).getInt();
            this.r = (float)(color >> 24 & 0xFF) / 255.0F;
            this.g = (float)(color >> 16 & 0xFF) / 255.0F;
            this.b = (float)(color >>  8 & 0xFF) / 255.0F;
            this.a = (float)(color & 0xFF) / 255.0F;

            key = 0;
            followEntity = -1;
            entityDimension = null;
            if (options.containsKey("follow"))
            {
                followEntity = NumericValue.asNumber(options.getOrDefault("follow", optional.get("follow"))).getInt();
                entityDimension = Registry.DIMENSION_TYPE.get(new Identifier(options.get("dim").getString()));
            }
        }
        public int getExpiry() { return duration; }
        public Vec3d toAbsolute(Entity e, Vec3d vec)
        {
            return e.getPos().add(vec);
        }
        public Vec3d vecFromValue(Value value)
        {
            if (!(value instanceof ListValue)) throw new InternalExpressionException("decoded value of "+value.getPrettyString()+" is not a triple");
            List<Value> elements = ((ListValue) value).getItems();
            return new Vec3d(
                    NumericValue.asNumber( elements.get(0)).getDouble(),
                    NumericValue.asNumber( elements.get(1)).getDouble(),
                    NumericValue.asNumber( elements.get(2)).getDouble()
            );
        }

        public abstract Consumer<ServerPlayerEntity> alternative();
        public int key()
        {
            if (key!=0) return key;
            key = calcKey();
            return key;
        }
        protected int calcKey()
        {
            int hash = 17;
            hash = 31*hash + color;
            hash = 31*hash + followEntity;
            hash = 31*hash + Float.hashCode(lineWidth);
            if (fa != 0.0) hash = 31*hash + fillColor;
            return hash;
        }
        // list of params that need to be there
        private final Set<String> required = ImmutableSet.of("duration", "shape", "dim");
        private final Map<String, Value> optional = ImmutableMap.of(
                "color", new NumericValue(-1),
                "follow", new NumericValue(-1),
                "width", new NumericValue(2.0),
                "fill", new NumericValue(0xffffff00)
        );
        protected Set<String> requiredParams() {return required;}
        // list of params that can be there, with defaults
        protected Set<String> optionalParams() {return optional.keySet();}

        private boolean canTake(String param)
        {
            return requiredParams().contains(param) || optionalParams().contains(param);
        }
    }

    public static class Box extends ExpiringShape
    {
        private final Set<String> required = ImmutableSet.of("from", "to");
        private final Map<String, Value> optional = ImmutableMap.of();
        @Override
        protected Set<String> requiredParams() { return Sets.union(super.requiredParams(), required); }
        @Override
        protected Set<String> optionalParams() { return Sets.union(super.optionalParams(), optional.keySet()); }
        public Box() { }

        Vec3d from;
        Vec3d to;

        @Override
        protected void init(Map<String, Value> options)
        {
            super.init(options);
            from = vecFromValue(options.get("from"));
            to = vecFromValue(options.get("to"));
        }

        @Override
        public Consumer<ServerPlayerEntity> alternative()
        {
            String particleName = String.format(Locale.ROOT , "dust %.1f %.1f %.1f 1.0", r, g, b);
            ParticleEffect particle = getParticleData(particleName);
            double density = Math.max(2.0, from.distanceTo(to) /50) / (a+0.1);
            return p ->
            {
                if (followEntity == -1)
                {
                    particleMesh(Collections.singletonList(p), particle, density, from, to);
                    return;
                }
                Entity e = p.getServer().getWorld(entityDimension).getEntityById(followEntity);
                if (e == null) return;
                Vec3d rel1 = toAbsolute(e, from);
                Vec3d rel2 = toAbsolute(e, to);
                particleMesh(Collections.singletonList(p), particle, density, rel1, rel2);
            };
        }

        @Override
        public int calcKey()
        {
            int hash = super.calcKey();
            hash = 31*hash + 1;
            hash = 31*hash + from.hashCode();
            hash = 31*hash + to.hashCode();
            return hash;
        }

        public static int particleMesh(List<ServerPlayerEntity> playerList, ParticleEffect particle, double density,
                                       Vec3d from, Vec3d to)
        {
            double x1 = from.x;
            double y1 = from.y;
            double z1 = from.z;
            double x2 = to.z;
            double y2 = to.y;
            double z2 = to.z;
            return
            drawParticleLine(playerList, particle, new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), density)+
            drawParticleLine(playerList, particle, new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), density)+
            drawParticleLine(playerList, particle, new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), density)+
            drawParticleLine(playerList, particle, new Vec3d(x2, y1, z1), new Vec3d(x1, y1, z1), density)+

            drawParticleLine(playerList, particle, new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), density)+
            drawParticleLine(playerList, particle, new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), density)+
            drawParticleLine(playerList, particle, new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), density)+
            drawParticleLine(playerList, particle, new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), density)+

            drawParticleLine(playerList, particle, new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), density)+
            drawParticleLine(playerList, particle, new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), density)+
            drawParticleLine(playerList, particle, new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), density)+
            drawParticleLine(playerList, particle, new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), density);
        }
    }

    public static class Line extends ExpiringShape
    {
        private final Set<String> required = ImmutableSet.of("from", "to");
        private final Map<String, Value> optional = ImmutableMap.of();
        @Override
        protected Set<String> requiredParams() { return Sets.union(super.requiredParams(), required); }
        @Override
        protected Set<String> optionalParams() { return Sets.union(super.optionalParams(), optional.keySet()); }

        private Line()
        {
            super();
        }

        Vec3d from;
        Vec3d to;

        @Override
        protected void init(Map<String, Value> options)
        {
            super.init(options);
            from = vecFromValue(options.get("from"));
            to = vecFromValue(options.get("to"));
        }

        @Override
        public Consumer<ServerPlayerEntity> alternative()
        {
            String particleName = String.format(Locale.ROOT , "dust %.1f %.1f %.1f 1.0", r, g, b);
            ParticleEffect particle = getParticleData(particleName);
            double density = Math.max(2.0, from.distanceTo(to) /50) / (a+0.1);
            return p ->
            {
                if (followEntity == -1)
                {
                    drawParticleLine(Collections.singletonList(p), particle, from, to, density);
                    return;
                }

                Entity e = p.getServer().getWorld(entityDimension).getEntityById(followEntity);
                if (e == null) return;
                Vec3d rel1 = toAbsolute(e, from);
                Vec3d rel2 = toAbsolute(e, to);
                drawParticleLine(Collections.singletonList(p), particle, rel1, rel2, density);
            };
        }

        @Override
        public int calcKey()
        {
            int hash = super.calcKey();
            hash = 31*hash + 2;
            hash = 31*hash + from.hashCode();
            hash = 31*hash + to.hashCode();
            return hash;
        }
    }

    public static class Sphere extends ExpiringShape
    {
        private final Set<String> required = ImmutableSet.of("center", "radius");
        private final Map<String, Value> optional = ImmutableMap.of("level", Value.ZERO);
        @Override
        protected Set<String> requiredParams() { return Sets.union(super.requiredParams(), required); }
        @Override
        protected Set<String> optionalParams() { return Sets.union(super.optionalParams(), optional.keySet()); }

        private Sphere()
        {
            super();
        }

        Vec3d center;
        float radius;
        int level;
        int subdivisions;

        @Override
        protected void init(Map<String, Value> options)
        {
            super.init(options);
            center = vecFromValue(options.get("center"));
            radius = NumericValue.asNumber(options.get("radius")).getFloat();
            level = NumericValue.asNumber(options.getOrDefault("level", optional.get("level"))).getInt();
            subdivisions = level;
            if (subdivisions <= 0)
            {
                subdivisions = Math.max(10, (int)(10*Math.sqrt(radius)));
            }
        }

        @Override
        public Consumer<ServerPlayerEntity> alternative() { return p ->
        {
            String particleName = String.format(Locale.ROOT , "dust %.1f %.1f %.1f 1.0", r, g, b);
            ParticleEffect particle = getParticleData(particleName);
            int partno = Math.min(1000,20*subdivisions);
            Random rand = p.world.getRandom();
            ServerWorld world = p.getServerWorld();

            double ccx, ccy, ccz;
            if (followEntity == -1)
            {
                ccx = center.x; ccy = center.y; ccz = center.z;
            }
            else
            {
                Entity e = p.getServer().getWorld(entityDimension).getEntityById(followEntity);
                if (e == null) return;
                Vec3d rel1 = toAbsolute(e, center);
                ccx = (float)rel1.x; ccy = (float)rel1.y; ccz = (float)rel1.z;
            }

            for (int i=0; i<partno; i++)
            {
                float theta = (float)Math.asin(rand.nextDouble()*2.0-1.0);
                float phi = (float)(2*Math.PI*rand.nextDouble());

                double x = radius * MathHelper.cos(theta) * MathHelper.cos(phi);
                double y = radius * MathHelper.cos(theta) * MathHelper.sin(phi);
                double z = radius * MathHelper.sin(theta);
                world.spawnParticles(p, particle, true,
                        x+ccx, y+ccy, z+ccz, 1,
                        0.0, 0.0, 0.0, 0.0);
            }
        };}

        @Override
        public int calcKey()
        {
            int hash = super.calcKey();
            hash = 31*hash + 3;
            hash = 31*hash + center.hashCode();
            hash = 31*hash + Double.hashCode(radius);
            hash = 31*hash + level;
            return hash;
        }
    }


    public static abstract class Param
    {
        public static Map<String, Param> of = new HashMap<String, Param>(){{
            put("shape", new ShapeParam());
            put("dim", new DimensionParam());
            put("duration", new PositiveIntParam("duration"));
            put("color", new ColorParam("color"));
            put("follow", new EntityParam("follow"));
            put("width", new PositiveFloatParam("width"));
            put("fill", new ColorParam("fill"));

            put("from", new Vec3Param("from", false));
            put("to", new Vec3Param("to", true));
            put("center", new Vec3Param("center", false));
            put("radius", new PositiveFloatParam("radius"));
            put("level", new PositiveIntParam("level"));
        }};
        protected String id;
        protected Param(String id)
        {
            this.id = id;
        }

        public abstract Tag toTag(Value value); //validates value, returning null if not necessary to keep it and serialize
        public abstract Value validate(Map<String, Value> options, CarpetContext cc, Value value); // makes sure the value is proper
        public abstract Value decode(Tag tag);
    }

    public abstract static class StringParam extends Param
    {
        protected StringParam(String id) { super(id); }

        @Override
        public Tag toTag(Value value) { return StringTag.of(value.getString()); }
        public Value decode(Tag tag) { return new StringValue(tag.asString()); }
    }

    public static class DimensionParam extends StringParam
    {
        protected DimensionParam() { super("dim"); }

        @Override
        public Value validate(Map<String, Value> options, CarpetContext cc, Value value)
        {
            String dimStr = value.getString();
            Optional<DimensionType> dimOp = Registry.DIMENSION_TYPE.getOrEmpty(new Identifier(dimStr));
            if (!dimOp.isPresent()) throw new InternalExpressionException("Unknown dimension "+dimStr);
            return value;
        }
    }
    public static class ShapeParam extends StringParam
    {
        protected ShapeParam() { super("shape"); }

        @Override
        public Value validate(Map<String, Value> options, CarpetContext cc, Value value)
        {
            String shape = value.getString();
            if (!ExpiringShape.shapeProviders.containsKey(shape))
                throw new InternalExpressionException("Unknown shape: "+shape);
            return value;
        }
    }
    public static abstract class NumericParam extends Param
    {
        protected NumericParam(String id) { super(id); }

        @Override
        public Value validate(Map<String, Value> options, CarpetContext cc, Value value)
        {
            if (!(value instanceof NumericValue))
                throw new InternalExpressionException("'" + id + "' needs to be a number");
            return value;
        }
    }
    public static abstract class PositiveParam extends NumericParam
    {
        protected PositiveParam(String id) { super(id); }
        @Override public Value validate(Map<String, Value> options, CarpetContext cc, Value value)
        {
            Value ret = super.validate(options, cc, value);
            if (((NumericValue)ret).getDouble()<=0) throw new InternalExpressionException("'"+id+"' should be positive");
            return ret;
        }
    }
    public static class PositiveFloatParam extends PositiveParam
    {
        protected PositiveFloatParam(String id) { super(id); }
        @Override
        public Value decode(Tag tag) { return new NumericValue(((FloatTag)tag).getFloat()); }
        @Override
        public Tag toTag(Value value) { return FloatTag.of(NumericValue.asNumber(value).getFloat()); }

    }
    public static class PositiveIntParam extends PositiveParam
    {
        protected PositiveIntParam(String id) { super(id); }
        @Override
        public Value decode(Tag tag) { return new NumericValue(((IntTag)tag).getInt()); }
        @Override
        public Tag toTag(Value value) { return IntTag.of(NumericValue.asNumber(value).getInt()); }

    }

    public static class Vec3Param extends Param
    {
        private boolean roundsUpForBlocks;
        protected Vec3Param(String id, boolean doesRoundUpForBlocks)
        {
            super(id);
            roundsUpForBlocks = doesRoundUpForBlocks;
        }
        @Override
        public Value validate(Map<String, Value> options, CarpetContext cc, Value value)
        {
            if (value instanceof BlockValue)
            {
                if (options.containsKey("follow"))
                    throw new InternalExpressionException(id+" parameter cannot use blocks as positions for relative positioning due to 'follow' attribute being present");
                BlockPos pos = ((BlockValue) value).getPos();
                int offset = roundsUpForBlocks?1:0;
                return ListValue.of(
                        new NumericValue(pos.getX()+offset),
                        new NumericValue(pos.getY()+offset),
                        new NumericValue(pos.getZ()+offset)
                );
            }
            if (value instanceof ListValue)
            {
                List<Value> values = ((ListValue) value).getItems();
                if (values.size()!=3) throw new InternalExpressionException("'"+id+"' requires 3 numerical values");
                for (Value component : values)
                {
                    if (!(component instanceof NumericValue))
                        throw new InternalExpressionException("'"+id+"' requires 3 numerical values");
                }
                return value;
            }
            if (value instanceof EntityValue)
            {
                if (options.containsKey("follow"))
                    throw new InternalExpressionException(id+" parameter cannot use entity as positions for relative positioning due to 'follow' attribute being present");
                Entity e = ((EntityValue) value).getEntity();
                return ListValue.of(
                        new NumericValue(e.getX()),
                        new NumericValue(e.getY()),
                        new NumericValue(e.getZ())
                );
            }
            CarpetSettings.LOG.error("Value: "+value.getString());
            throw new InternalExpressionException("'"+id+"' requires a triple, block or entity to indicate position");
        }

        public Value decode(Tag tag)
        {
            CompoundTag ctag = (CompoundTag)tag;
            return ListValue.of(
                    new NumericValue(ctag.getDouble("x")),
                    new NumericValue(ctag.getDouble("y")),
                    new NumericValue(ctag.getDouble("z"))
            );
        }
        @Override
        public Tag toTag(Value value)
        {
            List<Value> lv = ((ListValue)value).getItems();
            CompoundTag tag = new CompoundTag();
            tag.putDouble("x", NumericValue.asNumber(lv.get(0)).getDouble());
            tag.putDouble("y", NumericValue.asNumber(lv.get(1)).getDouble());
            tag.putDouble("z", NumericValue.asNumber(lv.get(2)).getDouble());
            return tag;
        }
    }
    public static class ColorParam extends NumericParam
    {
        protected ColorParam(String id)
        {
            super(id);
        }

        public Value decode(Tag tag) { return new NumericValue(((IntTag)tag).getInt()); }
        @Override
        public Tag toTag(Value value) { return IntTag.of(NumericValue.asNumber(value).getInt()); }
    }

    public static class EntityParam extends Param
    {

        protected EntityParam(String id) { super(id); }

        @Override
        public Tag toTag(Value value)
        {
            return IntTag.of(NumericValue.asNumber(value).getInt());
        }

        @Override
        public Value validate(Map<String, Value> options, CarpetContext cc, Value value)
        {
            if (value instanceof EntityValue) return new NumericValue(((EntityValue) value).getEntity().getEntityId());
            ServerPlayerEntity player = EntityValue.getPlayerByValue(cc.s.getMinecraftServer(), value);
            if (player == null)
                throw new InternalExpressionException(id+" parameter needs to represent an entity or player");
            return new NumericValue(player.getEntityId());
        }

        public Value decode(Tag tag) { return new NumericValue(((IntTag)tag).getInt()); }
    }

    private static boolean isStraight(Vec3d from, Vec3d to, double density)
    {
        if ( (from.x == to.x && from.y == to.y) || (from.x == to.x && from.z == to.z) || (from.y == to.y && from.z == to.z))
            return from.distanceTo(to) / density > 20;
        return false;
    }

    private static int drawOptimizedParticleLine(List<ServerPlayerEntity> playerList, ParticleEffect particle, Vec3d from, Vec3d to, double density)
    {
        double distance = from.distanceTo(to);
        int particles = (int)(distance/density);
        Vec3d towards = to.subtract(from);
        int parts = 0;
        for (ServerPlayerEntity player : playerList)
        {
            ServerWorld world = player.getServerWorld();
            world.spawnParticles(player, particle, true,
                    (towards.x)/2+from.x, (towards.y)/2+from.y, (towards.z)/2+from.z, particles/3,
                    towards.x/6, towards.y/6, towards.z/6, 0.0);
            world.spawnParticles(player, particle, true,
                    from.x, from.y, from.z,1,0.0,0.0,0.0,0.0);
            world.spawnParticles(player, particle, true,
                    to.x, to.y, to.z,1,0.0,0.0,0.0,0.0);
            parts += particles/3+2;
        }
        int divider = 6;
        while (particles/divider > 1)
        {
            int center = (divider*2)/3;
            int dev = 2*divider;
            for (ServerPlayerEntity player : playerList)
            {
                ServerWorld world = player.getServerWorld();
                world.spawnParticles(player, particle, true,
                        (towards.x)/center+from.x, (towards.y)/center+from.y, (towards.z)/center+from.z, particles/divider,
                        towards.x/dev, towards.y/dev, towards.z/dev, 0.0);
                world.spawnParticles(player, particle, true,
                        (towards.x)*(1.0-1.0/center)+from.x, (towards.y)*(1.0-1.0/center)+from.y, (towards.z)*(1.0-1.0/center)+from.z, particles/divider,
                        towards.x/dev, towards.y/dev, towards.z/dev, 0.0);
            }
            parts += 2*particles/divider;
            divider = 2*divider;
        }
        return parts;
    }

    public static int drawParticleLine(List<ServerPlayerEntity> players, ParticleEffect particle, Vec3d from, Vec3d to, double density)
    {
        if (isStraight(from, to, density)) return drawOptimizedParticleLine(players, particle, from, to, density);
        double lineLengthSq = from.squaredDistanceTo(to);
        if (lineLengthSq == 0) return 0;
        Vec3d incvec = to.subtract(from).multiply(2*density/MathHelper.sqrt(lineLengthSq));
        int pcount = 0;
        for (Vec3d delta = new Vec3d(0.0,0.0,0.0);
             delta.lengthSquared()<lineLengthSq;
             delta = delta.add(incvec.multiply(Expression.randomizer.nextFloat())))
        {
            for (ServerPlayerEntity player : players)
            {
                player.getServerWorld().spawnParticles(player, particle, true,
                        delta.x+from.x, delta.y+from.y, delta.z+from.z, 1,
                        0.0, 0.0, 0.0, 0.0);
                pcount ++;
            }
        }
        return pcount;
    }
}
