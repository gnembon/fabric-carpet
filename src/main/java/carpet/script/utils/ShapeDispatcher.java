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
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.Math.sqrt;

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
                ServerNetworkHandler.sendCustomCommand(player,"renderShape", tag);
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
        Map<String, Value> validatedParams = new HashMap<>();
        userParams.forEach((key, value) ->
        {
            Param param = Param.coders.get(key);
            if (param==null) throw new InternalExpressionException("Unknown feature for shape: "+key);
            validatedParams.put(key, param.validate(cc, value));
        });
        Function<Map<String, Value>,ExpiringShape> factory = ExpiringShape.shapeProviders.get(shapeType);
        if (factory == null) throw new InternalExpressionException("Unknown shape: "+shapeType);
        return factory.apply(validatedParams);
    }

    // client
    public static ExpiringShape fromTag(CompoundTag tag)
    {
        Map<String, Value> options = new HashMap<>();
        for (String key : tag.getKeys())
        {
            Function<Tag, Value> decoder = Param.decoders.get(key);
            if (decoder==null)
            {
                CarpetSettings.LOG.error("Unknown parameter for shape: "+key);
                return null;
            }
            Value decodedValue = decoder.apply(tag.get(key));
            options.put(key, decodedValue);
        }
        Value shapeValue = options.get("shape");
        if (shapeValue == null)
        {
            CarpetSettings.LOG.error("Shape id missing in "+ String.join(", ", tag.getKeys()));
            return null;
        }
        Function<Map<String, Value>,ExpiringShape> factory = ExpiringShape.shapeProviders.get(shapeValue.getString());
        if (factory == null)
        {
            CarpetSettings.LOG.error("Unknown shape: "+shapeValue.getString());
            return null;
        }
        try
        {
            return factory.apply(options);
        }
        catch (InternalExpressionException exc)
        {
            CarpetSettings.LOG.error(exc.getMessage());
        }
        return null;
    }

    public abstract static class ExpiringShape
    {
        public static final Map<String, Function<Map<String, Value>,ExpiringShape>> shapeProviders = new HashMap<String, Function<Map<String, Value>, ExpiringShape>>(){{
            put("line", creator(Line::new));
            put("box", creator(Box::new));
        }};
        private static Function<Map<String, Value>,ExpiringShape> creator(Supplier<ExpiringShape> shapeFactory)
        {
            return o -> {
                ExpiringShape shape = shapeFactory.get();
                shape.fromOptions(o);
                return shape;
            };
        }

        protected float r, g, b, a;
        protected int color;
        protected int duration = 0;
        private int key;

        protected ExpiringShape() { }

        public static CompoundTag toTag(Map<String, Value> params)
        {
            CompoundTag tag = new CompoundTag();
            params.forEach((k, v) -> {
                Tag valTag = Param.coders.get(k).toTag(v);
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
                if (!Param.coders.get(k).appliesTo(this))
                    throw new InternalExpressionException("Parameter "+k+" doesn't apply for shape "+options.get("shape").getString());
            });
            init(options);
        }

        protected void init(Map<String, Value> options)
        {
            duration = NumericValue.asNumber(options.get("duration")).getInt();
            color = NumericValue.asNumber(options.getOrDefault("color", optional.get("color"))).getInt();
            this.r = (float)(color >> 24 & 0xFF) / 255.0F;
            this.g = (float)(color >> 16 & 0xFF) / 255.0F;
            this.b = (float)(color >>  8 & 0xFF) / 255.0F;
            this.a = (float)(color & 0xFF) / 255.0F;
            key = 0;
        }
        public int getExpiry() { return duration; }
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
            return hash;
        }
        // list of params that need to be there
        private final Set<String> required = ImmutableSet.of("duration", "shape", "dim");
        private final Map<String, Value> optional = ImmutableMap.of("color", new NumericValue(-1));
        protected Set<String> requiredParams() {return required;}
        // list of params that can be there, with defaults
        protected Set<String> optionalParams() {return optional.keySet();}
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

        float x1, y1, z1;
        float x2, y2, z2;

        @Override
        protected void init(Map<String, Value> options)
        {
            super.init(options);
            List<Value> from = ((ListValue)options.get("from")).getItems();
            x1 = NumericValue.asNumber(from.get(0)).getFloat();
            y1 = NumericValue.asNumber(from.get(1)).getFloat();
            z1 = NumericValue.asNumber(from.get(2)).getFloat();
            List<Value> to = ((ListValue)options.get("to")).getItems();
            x2 = NumericValue.asNumber(to.get(0)).getFloat();
            y2 = NumericValue.asNumber(to.get(1)).getFloat();
            z2 = NumericValue.asNumber(to.get(2)).getFloat();
        }

        @Override
        public Consumer<ServerPlayerEntity> alternative()
        {
            String particleName = String.format(Locale.ROOT , "dust %.1f %.1f %.1f 1.0", r, g, b);
            ParticleEffect particle = getParticleData(particleName);
            double dx = x1-x2;
            double dy = y1-y2;
            double dz = z1-z2;
            double density = Math.max(2.0, Math.sqrt(dx*dx+dy*dy+dz*dz) /50) / (a+0.1);
            return p -> mesh(Collections.singletonList(p), particle, density, x1, y1, z1, x2, y2, z2);
        }

        @Override
        public int calcKey()
        {
            int hash = super.calcKey();
            hash = 31*hash + 1;
            hash = 31*hash + Float.hashCode(x1);
            hash = 31*hash + Float.hashCode(y1);
            hash = 31*hash + Float.hashCode(z1);
            hash = 31*hash + Float.hashCode(x2);
            hash = 31*hash + Float.hashCode(y2);
            hash = 31*hash + Float.hashCode(z2);
            return hash;
        }

        public static int mesh(List<ServerPlayerEntity> playerList, ParticleEffect particle, double density,
                               double x1, double y1, double z1, double x2, double y2, double z2 )
        {
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

        float x1, y1, z1;
        float x2, y2, z2;

        @Override
        protected void init(Map<String, Value> options)
        {
            super.init(options);
            List<Value> from = ((ListValue)options.get("from")).getItems();
            x1 = NumericValue.asNumber(from.get(0)).getFloat();
            y1 = NumericValue.asNumber(from.get(1)).getFloat();
            z1 = NumericValue.asNumber(from.get(2)).getFloat();
            List<Value> to = ((ListValue)options.get("to")).getItems();
            x2 = NumericValue.asNumber(to.get(0)).getFloat();
            y2 = NumericValue.asNumber(to.get(1)).getFloat();
            z2 = NumericValue.asNumber(to.get(2)).getFloat();
        }

        @Override
        public Consumer<ServerPlayerEntity> alternative()
        {
            String particleName = String.format(Locale.ROOT , "dust %.1f %.1f %.1f 1.0", r, g, b);
            ParticleEffect particle = getParticleData(particleName);
            Vec3d pos1 = new Vec3d(x1, y1, z1);
            Vec3d pos2 = new Vec3d(x2, y2, z2);
            double dx = x1-x2;
            double dy = y1-y2;
            double dz = z1-z2;
            double density = Math.max(2.0, Math.sqrt(dx*dx+dy*dy+dz*dz) /50) / (a+0.1);
            return p -> drawParticleLine(Collections.singletonList(p), particle, pos1, pos2, density);
        }

        @Override
        public int calcKey()
        {
            int hash = super.calcKey();
            hash = 31*hash + 2;
            hash = 31*hash + Float.hashCode(x1);
            hash = 31*hash + Float.hashCode(y1);
            hash = 31*hash + Float.hashCode(z1);
            hash = 31*hash + Float.hashCode(x2);
            hash = 31*hash + Float.hashCode(y2);
            hash = 31*hash + Float.hashCode(z2);
            return hash;
        }
    }

    public interface Param
    {
        Map<String, Function<Tag, Value>> decoders = new HashMap<String, Function<Tag, Value>>(){{
            put("shape", StringParam::decode);
            put("dim", StringParam::decode);
            put("duration", ShapeDispatcher::decodeInt);
            put("color", ShapeDispatcher::decodeInt);
            put("from", Vec3Param::decode);
            put("to", Vec3Param::decode);

        }};
        Map<String, Param> coders = new HashMap<String, Param>(){{
            put("shape", new ShapeParam());
            put("player", new PlayerParam());
            put("dim", new DimensionParam());
            put("duration", new DurationParam());
            put("color", new ColorParam());
            put("from", new FromParam());
            put("to", new ToParam());
        }};
        Tag toTag(Value value); //validates value, returning null if not necessary to keep it and serialize
        Value validate(CarpetContext cc, Value value); // makes sure the value is proper
        boolean appliesTo(ExpiringShape shape); // check if applies to the shape
        String identify();
    }
    public static class PlayerParam implements Param
    {
        @Override // doesn't need to be converted
        public Tag toTag(Value value) { return null; }

        @Override
        public Value validate(CarpetContext cc, Value value)
        {
            ServerPlayerEntity player = EntityValue.getPlayerByValue(cc.s.getMinecraftServer(), value);
            if (player == null)
                throw new InternalExpressionException("'player' parameter needs to represent an existing player");
            return new EntityValue(player);
        }

        @Override
        public boolean appliesTo(ExpiringShape shape) { return true; }
        @Override
        public String identify() { return "player"; }

        public static Value decode(Tag tag) { throw new RuntimeException("player tag should not be passed to the client"); }
    }
    public abstract static class StringParam implements Param
    {
        @Override
        public Tag toTag(Value value) { return StringTag.of(value.getString()); }
        @Override
        public Value validate(CarpetContext cc, Value value) { return value; }
        public static Value decode(Tag tag) { return new StringValue(tag.asString()); }
    }

    public static class DimensionParam extends StringParam
    {
        @Override
        public boolean appliesTo(ExpiringShape shape) { return true; }
        @Override
        public String identify() { return "dim"; }
    }
    public static class ShapeParam extends StringParam
    {
        @Override
        public Value validate(CarpetContext cc, Value value)
        {
            String shape = value.getString();
            if (!ExpiringShape.shapeProviders.containsKey(shape))
                throw new InternalExpressionException("Unknown shape: "+shape);
            return value;
        }
        @Override
        public boolean appliesTo(ExpiringShape shape) { return true; }
        @Override
        public String identify() { return "shape"; }
    }
    public static abstract class NumericParam implements Param
    {
        @Override
        public Tag toTag(Value value) { return IntTag.of(NumericValue.asNumber(value).getInt()); }
        @Override
        public Value validate(CarpetContext cc, Value value)
        {
            if (!(value instanceof NumericValue))
                throw new InternalExpressionException("'" + identify() + "' needs to be a number");
            return value;
        }
    }
    public static abstract class PositiveParam extends NumericParam
    {
        @Override public Value validate(CarpetContext cc, Value value)
        {
            Value ret = super.validate(cc, value);
            if (((NumericValue)ret).getDouble()<=0) throw new InternalExpressionException("'"+identify()+"' should be positive");
            return ret;
        }
    }

    public static Value decodeInt(Tag tag) { return new NumericValue(((IntTag)tag).getInt()); }
    public static class DurationParam extends PositiveParam
    {
        @Override
        public boolean appliesTo(ExpiringShape shape) { return true; }
        @Override
        public String identify() { return "duration"; }
    }

    public static abstract class Vec3Param implements Param
    {

        public abstract boolean roundsUpForBlocks();
        public abstract String identify();

        @Override
        public Value validate(CarpetContext cc, Value value)
        {
            if (value instanceof BlockValue)
            {
                BlockPos pos = ((BlockValue) value).getPos();
                int offset = roundsUpForBlocks()?1:0;
                return ListValue.of(
                        new NumericValue(pos.getX()+offset),
                        new NumericValue(pos.getY()+offset),
                        new NumericValue(pos.getZ()+offset)
                );
            }
            if (value instanceof ListValue)
            {
                List<Value> values = ((ListValue) value).getItems();
                if (values.size()!=3) throw new InternalExpressionException("'"+identify()+"' requires 3 numerical values");
                for (Value component : values)
                {
                    if (!(component instanceof NumericValue))
                        throw new InternalExpressionException("'"+identify()+"' requires 3 numerical values");
                }
                return value;
            }
            if (value instanceof EntityValue)
            {
                Entity e = ((EntityValue) value).getEntity();
                return ListValue.of(
                        new NumericValue(e.getX()),
                        new NumericValue(e.getY()),
                        new NumericValue(e.getZ())
                );
            }
            CarpetSettings.LOG.error("Value: "+value.getString());
            throw new InternalExpressionException("'"+identify()+"' requires a triple, block or entity to indicate position");
        }

        public static Value decode(Tag tag)
        {
            CompoundTag ctag = (CompoundTag)tag;
            return ListValue.of(
                    new NumericValue(ctag.getFloat("x")),
                    new NumericValue(ctag.getFloat("y")),
                    new NumericValue(ctag.getFloat("z"))
            );
        }
        @Override
        public Tag toTag(Value value)
        {
            List<Value> lv = ((ListValue)value).getItems();
            CompoundTag tag = new CompoundTag();
            tag.putFloat("x", NumericValue.asNumber(lv.get(0)).getFloat());
            tag.putFloat("y", NumericValue.asNumber(lv.get(1)).getFloat());
            tag.putFloat("z", NumericValue.asNumber(lv.get(2)).getFloat());
            return tag;
        }
    }
    public static class ColorParam extends NumericParam
    {
        @Override
        public boolean appliesTo(ExpiringShape shape) { return true; }
        @Override
        public String identify() { return "color"; }
    }

    public static class FromParam extends Vec3Param
    {
        @Override
        public boolean appliesTo(ExpiringShape shape) { return (shape instanceof Line) || (shape instanceof Box); }
        @Override
        public boolean roundsUpForBlocks() { return false; }
        @Override
        public String identify() { return "from"; }
    }

    public static class ToParam extends Vec3Param
    {
        @Override
        public boolean appliesTo(ExpiringShape shape) { return (shape instanceof Line) || (shape instanceof Box); }
        @Override
        public boolean roundsUpForBlocks() { return true; }
        @Override
        public String identify() { return "to"; }
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
        Vec3d incvec = to.subtract(from).multiply(2*density/sqrt(lineLengthSq));
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
