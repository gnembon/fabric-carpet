package carpet.script.utils;

import carpet.CarpetSettings;
import carpet.network.ServerNetworkHandler;
import carpet.script.CarpetContext;
import carpet.script.Expression;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BlockValue;
import carpet.script.value.EntityValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
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
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
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

    public static Pair<ExpiringShape,Map<String, Value>> fromFunctionArgs(
            CarpetContext cc,
            List<Value> lv,
            ServerPlayerEntity[] playerRef
    )
    {
        if (lv.size() < 3) throw new InternalExpressionException("'draw_shape' takes at least three parameters, shape name, duration, and its params");
        String shapeType = lv.get(0).getString();
        Value duration = NumericValue.asNumber(lv.get(1), "duration");
        Map<String, Value> params;
        if (lv.size() == 3)
        {
            Value paramValue = lv.get(2);
            if (paramValue instanceof MapValue)
            {
                params = new HashMap<>();
                ((MapValue) paramValue).getMap().entrySet().forEach(e -> params.put(e.getKey().getString(),e.getValue()));
            }
            else if (paramValue instanceof ListValue)
            {
                params = parseParams(((ListValue) paramValue).getItems());
            }
            else throw new InternalExpressionException("Parameters for 'draw_shape' need to be defined either in a list or a map");
        }
        else
        {
            List<Value> paramList = new ArrayList<>();
            for (int i=2; i < lv.size(); i++) paramList.add(lv.get(i));
            params = ShapeDispatcher.parseParams(paramList);
        }
        params.putIfAbsent("dim", new StringValue(cc.s.getWorld().getDimension().getType().toString()));
        params.putIfAbsent("duration", duration);

        if (params.containsKey("player"))
        {
            ServerPlayerEntity player = EntityValue.getPlayerByValue(cc.s.getMinecraftServer(), params.get("player"));
            if (player == null)
                throw new InternalExpressionException("'player' parameter needs to represent an existing player");
            params.remove("player");
            playerRef[0] = player;
        }
        return Pair.of(ShapeDispatcher.create(cc, shapeType, params), params);
    }

    public static void sendShape(List<ServerPlayerEntity> players, List<Pair<ExpiringShape,Map<String, Value>>> shapes)
    {
        List<ServerPlayerEntity> clientPlayers = new ArrayList<>();
        List<ServerPlayerEntity> alternativePlayers = new ArrayList<>();
        for (ServerPlayerEntity player : players)
        {
            if (ServerNetworkHandler.isValidCarpetPlayer(player))
            {
                clientPlayers.add(player);
            }
            else
            {
                alternativePlayers.add(player);
            }
        }
        if (!clientPlayers.isEmpty())
        {

            ListTag tag = new ListTag();
            int tagcount = 0;
            for (Pair<ExpiringShape, Map<String, Value>> s : shapes)
            {
                tag.add(ExpiringShape.toTag(s.getRight()));  // 4000 shapes limit boxes
                if (tagcount++>1000)
                {
                    tagcount = 0;
                    Tag finalTag = tag;
                    clientPlayers.forEach( p -> ServerNetworkHandler.sendCustomCommand(p, "scShapes", finalTag));
                    tag = new ListTag();
                }
            }
            Tag finalTag = tag;
            if (tag.size() > 0) clientPlayers.forEach( p -> ServerNetworkHandler.sendCustomCommand(p, "scShapes", finalTag));
        }
        if (!alternativePlayers.isEmpty())
        {
            List<Consumer<ServerPlayerEntity>> alternatives = new ArrayList<>();
            shapes.forEach(s -> alternatives.add(s.getLeft().alternative()));
            alternativePlayers.forEach(p -> alternatives.forEach( a -> a.accept(p)));
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
            put("cylinder", creator(Cylinder::new));
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
        private long key;
        protected int followEntity;
        protected String snapTo;
        protected boolean snapX, snapY, snapZ;
        public DimensionType shapeDimension;


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

            lineWidth = NumericValue.asNumber(options.getOrDefault("line", optional.get("line"))).getFloat();

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
            shapeDimension = Registry.DIMENSION_TYPE.get(new Identifier(options.get("dim").getString()));
            if (options.containsKey("follow"))
            {
                followEntity = NumericValue.asNumber(options.getOrDefault("follow", optional.get("follow"))).getInt();
                snapTo = options.getOrDefault("snap", optional.get("snap")).getString().toLowerCase(Locale.ROOT);
                snapX = snapTo.contains("x");
                snapY = snapTo.contains("y");
                snapZ = snapTo.contains("z");
            }
        }
        public int getExpiry() { return duration; }
        public Vec3d toAbsolute(Entity e, Vec3d vec, float partialTick)
        {
            return vec.add(
                    snapX?MathHelper.lerp(partialTick, e.prevX, e.getX()):0.0,
                    snapY?MathHelper.lerp(partialTick, e.prevY, e.getY()):0.0,
                    snapZ?MathHelper.lerp(partialTick, e.prevZ, e.getZ()):0.0
            );
        }
        public Vec3d relativiseRender(World world, Vec3d vec, float partialTick)
        {
            if (followEntity < 0) return vec;
            Entity e = world.getEntityById(followEntity);
            if (e == null) return vec;
            return toAbsolute(e, vec, partialTick);
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
        protected ParticleEffect replacementParticle()
        {
            String particleName = fa ==0 ?
                    String.format(Locale.ROOT , "dust %.1f %.1f %.1f 1.0", r, g, b):
                    String.format(Locale.ROOT , "dust %.1f %.1f %.1f 1.0", fr, fg, fb);
            return getParticleData(particleName);
        }


        public abstract Consumer<ServerPlayerEntity> alternative();
        public long key()
        {
            if (key!=0) return key;
            key = calcKey();
            return key;
        }
        protected long calcKey()
        { // using FNV-1a algorithm
            long hash = -3750763034362895579L;
            hash ^= shapeDimension.hashCode(); hash *= 1099511628211L;
            hash ^= color;                     hash *= 1099511628211L;
            hash ^= followEntity;              hash *= 1099511628211L;
            hash ^= Float.hashCode(lineWidth); hash *= 1099511628211L;
            if (fa != 0.0) { hash = 31*hash + fillColor; hash *= 1099511628211L; }
            return hash;
        }
        // list of params that need to be there
        private final Set<String> required = ImmutableSet.of("duration", "shape", "dim");
        private final Map<String, Value> optional = ImmutableMap.of(
                "color", new NumericValue(-1),
                "follow", new NumericValue(-1),
                "line", new NumericValue(2.0),
                "fill", new NumericValue(0xffffff00),
                "snap", new StringValue("xyz")
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
            if (from.equals(to)) throw new InternalExpressionException("Box dimensions are invalid - cannot draw a zero-size boxes");
        }

        @Override
        public Consumer<ServerPlayerEntity> alternative()
        {
            ParticleEffect particle = replacementParticle();
            double density = Math.max(2.0, from.distanceTo(to) /50) / (a+0.1);
            return p ->
            {
                if (p.dimension == shapeDimension)
                {
                    particleMesh(
                            Collections.singletonList(p),
                            particle,
                            density,
                            relativiseRender(p.getServerWorld(), from, 0),
                            relativiseRender(p.getServerWorld(), to, 0)
                    );
                }
            };
        }

        @Override
        public long calcKey()
        {
            long hash = super.calcKey();
            hash ^= 1;                     hash *= 1099511628211L;
            hash ^= from.hashCode();       hash *= 1099511628211L;
            hash ^= to.hashCode();         hash *= 1099511628211L;
            return hash;
        }

        public static int particleMesh(List<ServerPlayerEntity> playerList, ParticleEffect particle, double density,
                                       Vec3d from, Vec3d to)
        {
            double x1 = from.x;
            double y1 = from.y;
            double z1 = from.z;
            double x2 = to.x;
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
            if (from.equals(to)) throw new InternalExpressionException("Line dimensions are invalid - cannot draw a zero-length lines");
        }

        @Override
        public Consumer<ServerPlayerEntity> alternative()
        {
            ParticleEffect particle = replacementParticle();
            double density = Math.max(2.0, from.distanceTo(to) /50) / (a+0.1);
            return p ->
            {
                if (p.dimension == shapeDimension) drawParticleLine(
                        Collections.singletonList(p),
                        particle,
                        relativiseRender(p.getServerWorld(), from, 0),
                        relativiseRender(p.getServerWorld(), to, 0),
                        density
                );
            };
        }

        @Override
        public long calcKey()
        {
            long hash = super.calcKey();
            hash ^= 2;                     hash *= 1099511628211L;
            hash ^= from.hashCode();       hash *= 1099511628211L;
            hash ^= to.hashCode();         hash *= 1099511628211L;
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
            ParticleEffect particle = replacementParticle();
            int partno = Math.min(1000,20*subdivisions);
            Random rand = p.world.getRandom();
            ServerWorld world = p.getServerWorld();

            Vec3d ccenter = relativiseRender(world, center, 0 );

            double ccx = ccenter.x;
            double ccy = ccenter.y;
            double ccz = ccenter.z;

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
        public long calcKey()
        {
            long hash = super.calcKey();
            hash ^= 3;                        hash *= 1099511628211L;
            hash ^= center.hashCode();        hash *= 1099511628211L;
            hash ^= Double.hashCode(radius);  hash *= 1099511628211L;
            hash ^= level;                    hash *= 1099511628211L;
            return hash;
        }
    }
    public static class Cylinder extends ExpiringShape
    {
        private final Set<String> required = ImmutableSet.of("center", "radius");
        private final Map<String, Value> optional = ImmutableMap.of(
                "level", Value.ZERO,
                "height", Value.ZERO,
                "axis", new StringValue("y")
        );
        @Override
        protected Set<String> requiredParams() { return Sets.union(super.requiredParams(), required); }
        @Override
        protected Set<String> optionalParams() { return Sets.union(super.optionalParams(), optional.keySet()); }

        Vec3d center;
        float height;
        float radius;
        int level;
        int subdivisions;
        Direction.Axis axis;

        private Cylinder() { super(); }

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
            height = NumericValue.asNumber(options.getOrDefault("height", optional.get("height"))).getFloat();
            axis = Direction.Axis.fromName(options.getOrDefault("axis", optional.get("axis")).getString());
        }


        @Override
        public Consumer<ServerPlayerEntity> alternative() { return p ->
        {
            ParticleEffect particle = replacementParticle();
            int partno = (int)Math.min(1000,Math.sqrt(20*subdivisions*(1+height)));
            Random rand = p.world.getRandom();
            ServerWorld world = p.getServerWorld();

            Vec3d ccenter = relativiseRender(world, center, 0 );

            double ccx = ccenter.x;
            double ccy = ccenter.y;
            double ccz = ccenter.z;

            if (axis == Direction.Axis.Y)
            {
                for (int i=0; i<partno; i++)
                {
                    float d = rand.nextFloat()*height;
                    float phi = (float)(2*Math.PI*rand.nextDouble());
                    double x = radius * MathHelper.cos(phi);
                    double y = d;
                    double z = radius * MathHelper.sin(phi);
                    world.spawnParticles(p, particle, true, x+ccx, y+ccy, z+ccz, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
            else if (axis== Direction.Axis.X)
            {
                for (int i=0; i<partno; i++)
                {
                    float d = rand.nextFloat()*height;
                    float phi = (float)(2*Math.PI*rand.nextDouble());
                    double x = d;
                    double y = radius * MathHelper.cos(phi);
                    double z = radius * MathHelper.sin(phi);
                    world.spawnParticles(p, particle, true, x+ccx, y+ccy, z+ccz, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
            else  // Z
            {
                for (int i=0; i<partno; i++)
                {
                    float d = rand.nextFloat()*height;
                    float phi = (float)(2*Math.PI*rand.nextDouble());
                    double x = radius * MathHelper.sin(phi);
                    double y = radius * MathHelper.cos(phi);
                    double z = d;
                    world.spawnParticles(p, particle, true, x+ccx, y+ccy, z+ccz, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        };}

        @Override
        public long calcKey()
        {
            long hash = super.calcKey();
            hash ^= 3;                        hash *= 1099511628211L;
            hash ^= center.hashCode();        hash *= 1099511628211L;
            hash ^= Double.hashCode(radius);  hash *= 1099511628211L;
            hash ^= Double.hashCode(height);  hash *= 1099511628211L;
            hash ^= level;                    hash *= 1099511628211L;
            return hash;
        }
    }


    public static abstract class Param
    {
        public static Map<String, Param> of = new HashMap<String, Param>(){{
            put("shape", new ShapeParam());
            put("dim", new DimensionParam());
            put("duration", new NonNegativeIntParam("duration"));
            put("color", new ColorParam("color"));
            put("follow", new EntityParam("follow"));
            put("snap", new StringChoiceParam("snap", "xyz", "xz", "yz", "xy", "x", "y", "z"));
            put("line", new PositiveFloatParam("line"));
            put("fill", new ColorParam("fill"));

            put("from", new Vec3Param("from", false));
            put("to", new Vec3Param("to", true));
            put("center", new Vec3Param("center", false));
            put("radius", new PositiveFloatParam("radius"));
            put("level", new PositiveIntParam("level"));
            put("height", new FloatParam("height"));
            put("axis", new StringChoiceParam("axis", "x", "y", "z"));
            put("points", new PointsParam("points"));
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

    public static class StringChoiceParam extends StringParam
    {
        private Set<String> options;
        public StringChoiceParam(String id, String ... options)
        {
            super(id);
            this.options = Sets.newHashSet(options);
        }


        @Override
        public Value validate(Map<String, Value> options, CarpetContext cc, Value value)
        {
            if (this.options.contains(value.getString())) return value;
            return null;
        }
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
    public static class FloatParam extends NumericParam
    {
        protected FloatParam(String id) { super(id); }
        @Override
        public Value decode(Tag tag) { return new NumericValue(((FloatTag)tag).getFloat()); }
        @Override
        public Tag toTag(Value value) { return FloatTag.of(NumericValue.asNumber(value, id).getFloat()); }
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
        public Tag toTag(Value value) { return FloatTag.of(NumericValue.asNumber(value, id).getFloat()); }

    }
    public static class PositiveIntParam extends PositiveParam
    {
        protected PositiveIntParam(String id) { super(id); }
        @Override
        public Value decode(Tag tag) { return new NumericValue(((IntTag)tag).getInt()); }
        @Override
        public Tag toTag(Value value) { return IntTag.of(NumericValue.asNumber(value, id).getInt()); }

    }
    public static class NonNegativeIntParam extends NumericParam
    {
        protected NonNegativeIntParam(String id) { super(id); }
        @Override
        public Value decode(Tag tag) { return new NumericValue(((IntTag)tag).getInt()); }
        @Override
        public Tag toTag(Value value) { return IntTag.of(NumericValue.asNumber(value, id).getInt()); }
        @Override public Value validate(Map<String, Value> options, CarpetContext cc, Value value)
        {
            Value ret = super.validate(options, cc, value);
            if (((NumericValue)ret).getDouble()<0) throw new InternalExpressionException("'"+id+"' should be non-negative");
            return ret;
        }
    }
    public static class NonNegativeFloatParam extends NumericParam
    {
        protected NonNegativeFloatParam(String id) { super(id); }
        @Override
        public Value decode(Tag tag) { return new NumericValue(((FloatTag)tag).getFloat()); }
        @Override
        public Tag toTag(Value value) { return FloatTag.of(NumericValue.asNumber(value, id).getFloat()); }
        @Override public Value validate(Map<String, Value> options, CarpetContext cc, Value value)
        {
            Value ret = super.validate(options, cc, value);
            if (((NumericValue)ret).getDouble()<0) throw new InternalExpressionException("'"+id+"' should be non-negative");
            return ret;
        }
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
            return validate(this, options, cc, value, roundsUpForBlocks);
        }
        public static Value validate(Param p, Map<String, Value> options, CarpetContext cc, Value value, boolean roundsUp)
        {
            if (value instanceof BlockValue)
            {
                if (options.containsKey("follow"))
                    throw new InternalExpressionException(p.id+" parameter cannot use blocks as positions for relative positioning due to 'follow' attribute being present");
                BlockPos pos = ((BlockValue) value).getPos();
                int offset = roundsUp?1:0;
                return ListValue.of(
                        new NumericValue(pos.getX()+offset),
                        new NumericValue(pos.getY()+offset),
                        new NumericValue(pos.getZ()+offset)
                );
            }
            if (value instanceof ListValue)
            {
                List<Value> values = ((ListValue) value).getItems();
                if (values.size()!=3) throw new InternalExpressionException("'"+p.id+"' requires 3 numerical values");
                for (Value component : values)
                {
                    if (!(component instanceof NumericValue))
                        throw new InternalExpressionException("'"+p.id+"' requires 3 numerical values");
                }
                return value;
            }
            if (value instanceof EntityValue)
            {
                if (options.containsKey("follow"))
                    throw new InternalExpressionException(p.id+" parameter cannot use entity as positions for relative positioning due to 'follow' attribute being present");
                Entity e = ((EntityValue) value).getEntity();
                return ListValue.of(
                        new NumericValue(e.getX()),
                        new NumericValue(e.getY()),
                        new NumericValue(e.getZ())
                );
            }
            CarpetSettings.LOG.error("Value: "+value.getString());
            throw new InternalExpressionException("'"+p.id+"' requires a triple, block or entity to indicate position");
        }

        public Value decode(Tag tag)
        {
            ListTag ctag = (ListTag)tag;
            return ListValue.of(
                    new NumericValue(ctag.getDouble(0)),
                    new NumericValue(ctag.getDouble(1)),
                    new NumericValue(ctag.getDouble(2))
            );
        }
        @Override
        public Tag toTag(Value value)
        {
            List<Value> lv = ((ListValue)value).getItems();
            ListTag tag = new ListTag();
            tag.add(DoubleTag.of(NumericValue.asNumber(lv.get(0), "x").getDouble()));
            tag.add(DoubleTag.of(NumericValue.asNumber(lv.get(1), "y").getDouble()));
            tag.add(DoubleTag.of(NumericValue.asNumber(lv.get(2), "z").getDouble()));
            return tag;
        }
    }

    public static class PointsParam extends Param
    {
        public PointsParam(String id)
        {
            super(id);
        }
        @Override
        public Value validate(Map<String, Value> options, CarpetContext cc, Value value)
        {
            if (!(value instanceof ListValue))
                throw new InternalExpressionException(id+ " parameter should be a list");
            List<Value> points = new ArrayList<>();
            for (Value point: ((ListValue) value).getItems())
                points.add(Vec3Param.validate(this, options, cc, point, false));
            return ListValue.wrap(points);
        }

        public Value decode(Tag tag)
        {
            ListTag ltag = (ListTag)tag;
            List<Value> points = new ArrayList<>();
            for (int i=0, ll = ltag.size(); i<ll; i++)
            {
                ListTag ptag = ltag.getList(i);
                points.add(ListValue.of(
                        new NumericValue(ptag.getDouble(0)),
                        new NumericValue(ptag.getDouble(1)),
                        new NumericValue(ptag.getDouble(2))
                ));
            }
            return ListValue.wrap(points);
        }
        @Override
        public Tag toTag(Value pointsValue)
        {
            List<Value> lv = ((ListValue)pointsValue).getItems();
            ListTag ltag = new ListTag();
            for (Value value : lv)
            {
                List<Value> coords = ((ListValue)value).getItems();
                ListTag tag = new ListTag();
                tag.add(DoubleTag.of(NumericValue.asNumber(lv.get(0), "x").getDouble()));
                tag.add(DoubleTag.of(NumericValue.asNumber(lv.get(1), "y").getDouble()));
                tag.add(DoubleTag.of(NumericValue.asNumber(lv.get(2), "z").getDouble()));
                ltag.add(tag);
            }
            return ltag;
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
        public Tag toTag(Value value) { return IntTag.of(NumericValue.asNumber(value, id).getInt()); }
    }

    public static class EntityParam extends Param
    {

        protected EntityParam(String id) { super(id); }

        @Override
        public Tag toTag(Value value)
        {
            return IntTag.of(NumericValue.asNumber(value, id).getInt());
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
