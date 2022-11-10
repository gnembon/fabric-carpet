package carpet.script.utils;

import carpet.CarpetSettings;
import carpet.network.ServerNetworkHandler;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.language.Sys;
import carpet.script.utils.shapes.ShapeDirection;
import carpet.script.value.AbstractListValue;
import carpet.script.value.BlockValue;
import carpet.script.value.BooleanValue;
import carpet.script.value.EntityValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Map.entry;

public class ShapeDispatcher
{
    private static final Map<String, ParticleOptions> particleCache = new HashMap<>();
    public static record ShapeWithConfig(ExpiringShape shape, Map<String, Value> config) {}

    public static ShapeWithConfig fromFunctionArgs(
            MinecraftServer server, ServerLevel world,
            List<Value> lv,
            Set<ServerPlayer> playerSet
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
        params.putIfAbsent("dim", new StringValue(world.dimension().location().toString()));
        params.putIfAbsent("duration", duration);

        if (params.containsKey("player"))
        {
            Value players = params.get("player");
            List<Value> playerVals;
            if (players instanceof ListValue)
                playerVals = ((ListValue) players).getItems();
            else
                playerVals = Collections.singletonList(players);
            for (Value pVal : playerVals)
            {
                ServerPlayer player = EntityValue.getPlayerByValue(server, pVal);
                if (player == null)
                    throw new InternalExpressionException("'player' parameter needs to represent an existing player, not "+pVal.getString());
                playerSet.add(player);
            }
            params.remove("player");
        }
        return new ShapeWithConfig(ShapeDispatcher.create(server, shapeType, params), params);
    }

    public static void sendShape(Collection<ServerPlayer> players, List<ShapeWithConfig> shapes)
    {
        List<ServerPlayer> clientPlayers = new ArrayList<>();
        List<ServerPlayer> alternativePlayers = new ArrayList<>();
        for (ServerPlayer player : players)
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
            for (ShapeWithConfig s : shapes)
            {
                tag.add(ExpiringShape.toTag(s.config()));  // 4000 shapes limit boxes
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
            List<Consumer<ServerPlayer>> alternatives = new ArrayList<>();
            shapes.forEach(s -> alternatives.add(s.shape().alternative()));
            alternativePlayers.forEach(p -> alternatives.forEach( a -> a.accept(p)));
        }
    }

    public static ParticleOptions getParticleData(String name, RegistryAccess regs)
    {
        ParticleOptions particle = particleCache.get(name); // [SCARY SHIT] cache should be cleared between worlds
        if (particle != null)
            return particle;
        try
        {
            particle = ParticleArgument.readParticle(new StringReader(name), regs.lookupOrThrow(Registries.PARTICLE_TYPE));
        }
        catch (CommandSyntaxException e)
        {
            throw new ThrowStatement(name, Throwables.UNKNOWN_PARTICLE);
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

    public static ExpiringShape create(MinecraftServer server, String shapeType, Map<String, Value> userParams)
    {
        userParams.put("shape", new StringValue(shapeType));
        userParams.keySet().forEach(key -> {
            Param param = Param.of.get(key);
            if (param==null) throw new InternalExpressionException("Unknown feature for shape: "+key);
            userParams.put(key, param.validate(userParams, server, userParams.get(key)));
        });
        Function<Map<String, Value>,ExpiringShape> factory = ExpiringShape.shapeProviders.get(shapeType);
        if (factory == null) throw new InternalExpressionException("Unknown shape: "+shapeType);
        return factory.apply(userParams);
    }

    // client
    public static ExpiringShape fromTag(CompoundTag tag, Level level)
    {
        Map<String, Value> options = new HashMap<>();
        for (String key : tag.getAllKeys())
        {
            Param decoder = Param.of.get(key);
            if (decoder==null)
            {
                CarpetSettings.LOG.info("Unknown parameter for shape: "+key);
                return null;
            }
            Value decodedValue = decoder.decode(tag.get(key), level);
            options.put(key, decodedValue);
        }
        Value shapeValue = options.get("shape");
        if (shapeValue == null)
        {
            CarpetSettings.LOG.info("Shape id missing in "+ String.join(", ", tag.getAllKeys()));
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
            put("label", creator(DisplayedText::new));
            put("polygon",creator(Polyface::new));
            put("block",creator(()->new DisplayedSprite(false)));
            put("item",creator(()->new DisplayedSprite(true)));
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
        protected boolean discreteX, discreteY, discreteZ;
        protected ResourceKey<Level> shapeDimension;
        protected boolean debug;


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

            debug = false;
            if (options.containsKey("debug")) {
                debug = options.get("debug").getBoolean();
            }

            key = 0;
            followEntity = -1;
            shapeDimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(options.get("dim").getString()));
            if (options.containsKey("follow"))
            {
                followEntity = NumericValue.asNumber(options.getOrDefault("follow", optional.get("follow"))).getInt();
                snapTo = options.getOrDefault("snap", optional.get("snap")).getString().toLowerCase(Locale.ROOT);
                snapX = snapTo.contains("x");
                snapY = snapTo.contains("y");
                snapZ = snapTo.contains("z");
                discreteX = snapTo.contains("dx");
                discreteY = snapTo.contains("dy");
                discreteZ = snapTo.contains("dz");
            }
        }
        public int getExpiry() { return duration; }
        public Vec3 toAbsolute(Entity e, Vec3 vec, float partialTick)
        {
            return vec.add(
                    snapX?(discreteX ?Mth.floor(e.getX()):Mth.lerp(partialTick, e.xo, e.getX())):0.0,
                    snapY?(discreteY ?Mth.floor(e.getY()):Mth.lerp(partialTick, e.yo, e.getY())):0.0,
                    snapZ?(discreteZ ?Mth.floor(e.getZ()):Mth.lerp(partialTick, e.zo, e.getZ())):0.0
            );
        }
        public Vec3 relativiseRender(Level world, Vec3 vec, float partialTick)
        {
            if (followEntity < 0) return vec;
            Entity e = world.getEntity(followEntity);
            if (e == null) return vec;
            return toAbsolute(e, vec, partialTick);
        }

        public Vec3 vecFromValue(Value value)
        {
            if (!(value instanceof ListValue)) throw new InternalExpressionException("decoded value of "+value.getPrettyString()+" is not a triple");
            List<Value> elements = ((ListValue) value).getItems();
            return new Vec3(
                    NumericValue.asNumber( elements.get(0)).getDouble(),
                    NumericValue.asNumber( elements.get(1)).getDouble(),
                    NumericValue.asNumber( elements.get(2)).getDouble()
            );
        }
        protected ParticleOptions replacementParticle(RegistryAccess regs)
        {
            String particleName = fa ==0 ?
                    String.format(Locale.ROOT , "dust %.1f %.1f %.1f 1.0", r, g, b):
                    String.format(Locale.ROOT , "dust %.1f %.1f %.1f 1.0", fr, fg, fb);
            return getParticleData(particleName, regs);
        }


        public abstract Consumer<ServerPlayer> alternative();
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
            hash ^= Boolean.hashCode(debug); hash *= 1099511628211L;
            if (followEntity >= 0)
            {
                hash ^= snapTo.hashCode();     hash *= 1099511628211L;
            }
            hash ^= Float.hashCode(lineWidth); hash *= 1099511628211L;
            if (fa != 0.0) { hash = 31*hash + fillColor; hash *= 1099511628211L; }
            return hash;
        }
        private static final double xdif = new Random().nextDouble();
        private static final double ydif = new Random().nextDouble();
        private static final double zdif = new Random().nextDouble();
        int vec3dhash(Vec3 vec)
        {
            return vec.add(xdif, ydif, zdif).hashCode();
        }

        // list of params that need to be there
        private final Set<String> required = Set.of("duration", "shape", "dim");
        private final Map<String, Value> optional = Map.of(
                "color", new NumericValue(-1),
                "follow", new NumericValue(-1),
                "line", new NumericValue(2.0),
                "debug", Value.FALSE,
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

    public static class DisplayedText extends ExpiringShape
    {
        private final Set<String> required = Set.of("pos", "text");
        private final Map<String, Value> optional = Map.ofEntries(
                entry("facing", new StringValue("player")),
                entry("raise", new NumericValue(0)),
                entry("tilt", new NumericValue(0)),
                entry("lean", new NumericValue(0)),
                entry("turn", new NumericValue(0)),
                entry("indent", new NumericValue(0)),
                entry("height", new NumericValue(0)),
                entry("align", new StringValue("center")),
                entry("size", new NumericValue(10)),
                entry("value", Value.NULL),
                entry("doublesided", new NumericValue(0)));
        @Override
        protected Set<String> requiredParams() { return Sets.union(super.requiredParams(), required); }
        @Override
        protected Set<String> optionalParams() { return Sets.union(super.optionalParams(), optional.keySet()); }
        public DisplayedText() { }

        Vec3 pos;
        String text;
        int textcolor;
        int textbck;

        ShapeDirection facing;
        float raise;
        float tilt;
        float lean;
        float turn;
        float size;
        float indent;
        int align;
        float height;
        Component value;
        boolean doublesided;

        @Override
        protected void init(Map<String, Value> options)
        {
            super.init(options);
            pos = vecFromValue(options.get("pos"));
            value = ((FormattedTextValue)options.get("text")).getText();
            text = value.getString();
            if (options.containsKey("value"))
            {
                value = ((FormattedTextValue)options.get("value")).getText();
            }
            textcolor = rgba2argb(color);
            textbck = rgba2argb(fillColor);
            String dir = options.getOrDefault("facing", optional.get("facing")).getString();
            facing = ShapeDirection.fromString(dir);
            align = 0;
            if (options.containsKey("align"))
            {
                String alignStr = options.get("align").getString();
                if ("right".equalsIgnoreCase(alignStr))
                    align = 1;
                else if ("left".equalsIgnoreCase(alignStr))
                    align = -1;
            }
            doublesided = false;
            if (options.containsKey("doublesided"))
            {
                doublesided = options.get("doublesided").getBoolean();
            }

            raise = NumericValue.asNumber(options.getOrDefault("raise", optional.get("raise"))).getFloat();
            tilt = NumericValue.asNumber(options.getOrDefault("tilt", optional.get("tilt"))).getFloat();
            lean = NumericValue.asNumber(options.getOrDefault("lean", optional.get("lean"))).getFloat();
            turn = NumericValue.asNumber(options.getOrDefault("turn", optional.get("turn"))).getFloat();
            indent = NumericValue.asNumber(options.getOrDefault("indent", optional.get("indent"))).getFloat();
            height = NumericValue.asNumber(options.getOrDefault("height", optional.get("height"))).getFloat();

            size = NumericValue.asNumber(options.getOrDefault("size", optional.get("size"))).getFloat();
        }

        private int rgba2argb(int color)
        {
            int r = Math.max(1, color >> 24 & 0xFF);
            int g = Math.max(1, color >> 16 & 0xFF);
            int b = Math.max(1, color >>  8 & 0xFF);
            int a = color & 0xFF;
            return (a << 24) + (r << 16) + (g << 8) + b;
        }


        @Override
        public Consumer<ServerPlayer> alternative()
        {
            return s -> {};
        }

        @Override
        public long calcKey()
        {
            long hash = super.calcKey();
            hash ^= 5;                  hash *= 1099511628211L;
            hash ^= vec3dhash(pos);     hash *= 1099511628211L;
            hash ^= text.hashCode();    hash *= 1099511628211L;
            if (facing!= null) hash ^= facing.hashCode(); hash *= 1099511628211L;
            hash ^= Float.hashCode(raise); hash *= 1099511628211L;
            hash ^= Float.hashCode(tilt); hash *= 1099511628211L;
            hash ^= Float.hashCode(lean); hash *= 1099511628211L;
            hash ^= Float.hashCode(turn); hash *= 1099511628211L;
            hash ^= Float.hashCode(indent); hash *= 1099511628211L;
            hash ^= Float.hashCode(height); hash *= 1099511628211L;
            hash ^= Float.hashCode(size); hash *= 1099511628211L;
            hash ^= Integer.hashCode(align); hash *= 1099511628211L;
            hash ^= Boolean.hashCode(doublesided); hash *= 1099511628211L;

            return hash;
        }
    }

    public static class DisplayedSprite extends ExpiringShape {
        private final Set<String> required = Set.of("pos");
        private final Map<String, Value> optional = Map.ofEntries(
                entry("facing", new StringValue("north")),
                entry("tilt", new NumericValue(0)),
                entry("lean", new NumericValue(0)),
                entry("turn", new NumericValue(0)),
                entry("scale", ListValue.fromTriple(1,1,1)),
                entry("blocklight", new NumericValue(-1)),
                entry("skylight", new NumericValue(-1)));
        private boolean isitem;

        @Override
        protected Set<String> requiredParams() { return Sets.union(Sets.union(super.requiredParams(), required), Set.of(isitem ? "item" : "block"));}

        @Override
        protected Set<String> optionalParams() { return Sets.union(Sets.union(super.optionalParams(), optional.keySet()),isitem ? Set.of("variant") : Set.of()); }

        public DisplayedSprite(boolean i)
        {
            isitem = i;
        }

        Vec3 pos;

        ShapeDirection facing;

        float tilt;
        float lean;
        float turn;

        int blockLight;
        int skyLight;

        float scaleX = 1.0f;
        float scaleY = 1.0f;
        float scaleZ = 1.0f;
        CompoundTag blockEntity;
        BlockState blockState;
        ItemStack item = null;
        String itemTransformType;

        @Override
        protected void init(Map<String, Value> options)
        {
            super.init(options);
            pos = vecFromValue(options.get("pos"));
            if (!this.isitem)
            {
                final BlockValue block = (BlockValue)options.get("block");
                blockState = block.getBlockState();
                blockEntity = block.getData();
            }
            else
            {
                this.item = ItemStack.of(((NBTSerializableValue) options.get("item")).getCompoundTag());
            }
            blockLight = NumericValue.asNumber(options.getOrDefault("blocklight", optional.get("blocklight"))).getInt();
            if (blockLight > 15) blockLight = 15;
            skyLight = NumericValue.asNumber(options.getOrDefault("skylight", optional.get("skylight"))).getInt();
            if (skyLight > 15) skyLight = 15;

            itemTransformType = "none";
            if (options.containsKey("variant"))
            {
                itemTransformType = options.get("variant").getString().toLowerCase(Locale.ROOT);
            }

            String dir = options.getOrDefault("facing", optional.get("facing")).getString();
            facing = ShapeDirection.fromString(dir);

            tilt = NumericValue.asNumber(options.getOrDefault("tilt", optional.get("tilt"))).getFloat();
            lean = NumericValue.asNumber(options.getOrDefault("lean", optional.get("lean"))).getFloat();
            turn = NumericValue.asNumber(options.getOrDefault("turn", optional.get("turn"))).getFloat();
            List<Value> scale = ((ListValue) options.getOrDefault("scale", optional.get("scale"))).unpack();
            scaleY = NumericValue.asNumber(scale.get(1)).getFloat();
            scaleX = NumericValue.asNumber(scale.get(0)).getFloat();
            scaleZ = NumericValue.asNumber(scale.get(2)).getFloat();
        }

        @Override
        public Consumer<ServerPlayer> alternative() {
            return p -> {
                ParticleOptions particle;
                if(this.isitem)
                {
                    if (Block.byItem(this.item.getItem()).defaultBlockState().isAir()) return;
                    particle = getParticleData("block_marker "+ BuiltInRegistries.BLOCK.getKey(Block.byItem(this.item.getItem())), p.level.registryAccess());
                }
                else
                {
                    particle = getParticleData("block_marker "+BuiltInRegistries.BLOCK.getKey(this.blockState.getBlock()), p.level.registryAccess());
                }

                Vec3 v = relativiseRender(p.level, this.pos, 0);
                p.getLevel().sendParticles(p, particle , true, v.x, v.y, v.z, 1,0.0, 0.0, 0.0, 0.0);
            };
        }

        @Override
        public long calcKey() {
            long hash = super.calcKey();
            hash ^= 7;                  hash *= 1099511628211L;
            hash ^= Boolean.hashCode(isitem); hash *= 1099511628211L;
            hash ^= vec3dhash(pos);     hash *= 1099511628211L;
            if (facing!= null) hash ^= facing.hashCode(); hash *= 1099511628211L;
            hash ^= Float.hashCode(tilt); hash *= 1099511628211L;
            hash ^= Float.hashCode(lean); hash *= 1099511628211L;
            hash ^= Float.hashCode(turn); hash *= 1099511628211L;
            hash ^= Float.hashCode(scaleY); hash *= 1099511628211L;
            hash ^= Float.hashCode(scaleZ); hash *= 1099511628211L;
            hash ^= Float.hashCode(scaleX); hash *= 1099511628211L;
            hash ^= Float.hashCode(skyLight); hash *= 1099511628211L;
            hash ^= Float.hashCode(blockLight); hash *= 1099511628211L;
            if (blockEntity!= null) hash ^= blockEntity.toString().hashCode(); hash *= 1099511628211L;
            if (blockState!= null) hash ^= blockState.hashCode(); hash *= 1099511628211L;
            hash ^= ValueConversions.of(item).getString().hashCode(); hash *= 1099511628211L;
            hash ^= itemTransformType.hashCode(); hash *= 1099511628211L;

            return hash;
        }
    }


    public static class Box extends ExpiringShape
    {
        private final Set<String> required = Set.of("from", "to");
        private final Map<String, Value> optional = Map.of();
        @Override
        protected Set<String> requiredParams() { return Sets.union(super.requiredParams(), required); }
        @Override
        protected Set<String> optionalParams() { return Sets.union(super.optionalParams(), optional.keySet()); }
        public Box() { }

        Vec3 from;
        Vec3 to;

        @Override
        protected void init(Map<String, Value> options)
        {
            super.init(options);
            from = vecFromValue(options.get("from"));
            to = vecFromValue(options.get("to"));
        }

        @Override
        public Consumer<ServerPlayer> alternative()
        {
            double density = Math.max(2.0, from.distanceTo(to) /50/ (a+0.1)) ;
            return p ->
            {
                if (p.level.dimension() == shapeDimension)
                {
                    particleMesh(
                            Collections.singletonList(p),
                            replacementParticle(p.level.registryAccess()),
                            density,
                            relativiseRender(p.level, from, 0),
                            relativiseRender(p.level, to, 0)
                    );
                }
            };
        }

        @Override
        public long calcKey()
        {
            long hash = super.calcKey();
            hash ^= 1;                     hash *= 1099511628211L;
            hash ^= vec3dhash(from);       hash *= 1099511628211L;
            hash ^= vec3dhash(to);         hash *= 1099511628211L;
            return hash;
        }

        public static int particleMesh(List<ServerPlayer> playerList, ParticleOptions particle, double density,
                                       Vec3 from, Vec3 to)
        {
            double x1 = from.x;
            double y1 = from.y;
            double z1 = from.z;
            double x2 = to.x;
            double y2 = to.y;
            double z2 = to.z;
            return
            drawParticleLine(playerList, particle, new Vec3(x1, y1, z1), new Vec3(x1, y2, z1), density)+
            drawParticleLine(playerList, particle, new Vec3(x1, y2, z1), new Vec3(x2, y2, z1), density)+
            drawParticleLine(playerList, particle, new Vec3(x2, y2, z1), new Vec3(x2, y1, z1), density)+
            drawParticleLine(playerList, particle, new Vec3(x2, y1, z1), new Vec3(x1, y1, z1), density)+

            drawParticleLine(playerList, particle, new Vec3(x1, y1, z2), new Vec3(x1, y2, z2), density)+
            drawParticleLine(playerList, particle, new Vec3(x1, y2, z2), new Vec3(x2, y2, z2), density)+
            drawParticleLine(playerList, particle, new Vec3(x2, y2, z2), new Vec3(x2, y1, z2), density)+
            drawParticleLine(playerList, particle, new Vec3(x2, y1, z2), new Vec3(x1, y1, z2), density)+

            drawParticleLine(playerList, particle, new Vec3(x1, y1, z1), new Vec3(x1, y1, z2), density)+
            drawParticleLine(playerList, particle, new Vec3(x1, y2, z1), new Vec3(x1, y2, z2), density)+
            drawParticleLine(playerList, particle, new Vec3(x2, y2, z1), new Vec3(x2, y2, z2), density)+
            drawParticleLine(playerList, particle, new Vec3(x2, y1, z1), new Vec3(x2, y1, z2), density);
        }
    }

    public static class Polyface extends ExpiringShape
    {
        @Override
        public long calcKey(){
            long hash = super.calcKey();
            hash ^= 6;                     hash *= 1099511628211L;
            hash ^= mode.hashCode();   hash *= 1099511628211L;
            hash ^= relative.hashCode();      hash *= 1099511628211L;
            for (Vec3 i :vertex_list){
                hash ^= vec3dhash(i);
                hash *= 1099511628211L;
            }
            hash ^= Boolean.hashCode(doublesided);    hash *= 1099511628211L;
            hash ^= Integer.hashCode(vertex_list.size());    hash *= 1099511628211L;
            hash ^= Boolean.hashCode(inneredges);    hash *= 1099511628211L;
            return hash;
        }
        ArrayList<Vec3> alter_point=null;
        final Random random=new Random();
        boolean doublesided;

        ArrayList<Vec3> alter_point(ServerPlayer p){
            if (alter_point!=null){
                return alter_point;
            }
            alter_point=new ArrayList<>();
            switch (mode) {
                case TRIANGLES:
                    for (int i=0;i<vertex_list.size();i++){
                        Vec3 vecA=vertex_list.get(i);
                        if(relative.get(i)){
                            vecA=relativiseRender(p.level, vecA, 0);
                        }
                        i++;
                        Vec3 vecB=vertex_list.get(i);
                        if(relative.get(i)){
                            vecB=relativiseRender(p.level, vecB, 0);
                        }
                        i++;
                        Vec3 vecC=vertex_list.get(i);
                        if(relative.get(i)){
                            vecC=relativiseRender(p.level, vecC, 0);
                        }
                        alter_draw_triangles(vecA, vecB, vecC);
                    }
                    break;
                case TRIANGLE_FAN:
                    Vec3 vec0=vertex_list.get(0);
                    if(relative.get(0)){
                        vec0=relativiseRender(p.level, vec0, 0);
                    }
                    Vec3 vec1=vertex_list.get(1);
                    if(relative.get(1)){
                        vec1=relativiseRender(p.level, vec1, 0);
                    }
                    for(int i=2;i<vertex_list.size();i++){
                        Vec3 vec=vertex_list.get(i);
                        if(relative.get(i)){
                            vec=relativiseRender(p.level, vec, 0);
                        }
                        alter_draw_triangles(vec0,vec1,vec);
                        vec1=vec;
                    }
                    break;
                case TRIANGLE_STRIP:
                    Vec3 vecA=vertex_list.get(0);
                    if(relative.get(0)){
                        vecA=relativiseRender(p.level, vecA, 0);
                    }
                    Vec3 vecB=vertex_list.get(1);
                    if(relative.get(1)){
                        vecB=relativiseRender(p.level, vecB, 0);
                    }
                    for(int i=2;i<vertex_list.size();i++){
                        Vec3 vec=vertex_list.get(i);
                        if(relative.get(i)){
                            vec=relativiseRender(p.level, vec, 0);
                        }
                        alter_draw_triangles(vecA,vecB,vec);
                        vecA=vecB;vecB=vec;
                    }
                    break;
                default:
                    break;
            }
            
            return alter_point;
        }
        void alter_draw_triangles(Vec3 a,Vec3 b,Vec3 c){
            
            
            Vec3 B = b.subtract(a);Vec3 C = c.subtract(a);
            for(int i=0;i/8<B.cross(C).length();i++){
                double x = random.nextDouble();double y = random.nextDouble();
                alter_point.add(a.add(B.scale(x/2)).add(C.scale(y/2)));
                if(x+y<1){
                    alter_point.add(a.add(B.scale((x+1)/2)).add(C.scale(y/2)));
                }else{
                    x=1-x;y=1-y;
                    alter_point.add(a.add(B.scale(x/2)).add(C.scale((y+1)/2)));
                }
            }
            
        }
        @Override
        public Consumer<ServerPlayer> alternative() {
            return p->{
                if(p.level.dimension() != this.shapeDimension){return;}
                if(!(fa>0.0f)){return;}
                final ParticleOptions locparticledata = getParticleData(String.format(Locale.ROOT ,"dust %.1f %.1f %.1f %.1f", fr, fg, fb, fa), p.level.registryAccess());
                for(Vec3 v : alter_point(p)){
                    p.getLevel().sendParticles(p,locparticledata , true,
                    v.x, v.y, v.z, 1,
                    0.0, 0.0, 0.0, 0.0);
                }
            };
        }
        private final Set<String> required = Set.of("points");
        private final Map<String, Value> optional = Map.ofEntries(
            entry("relative",Value.NULL),
            entry("mode", new StringValue("polygon")),
            entry("inner", Value.TRUE),
            entry("doublesided", Value.TRUE)
        );
        @Override
        protected Set<String> requiredParams() { return Sets.union(super.requiredParams(), required); }
        @Override
        protected Set<String> optionalParams() { return Sets.union(super.optionalParams(), optional.keySet()); }
        ArrayList<Vec3> vertex_list=new ArrayList<>();
        Mode mode;
        ArrayList<Boolean> relative=new ArrayList<>();
        boolean inneredges;
        @Override
        protected void init(Map<String, Value> options)
        {
            super.init(options);

            doublesided=options.getOrDefault("doublesided",optional.get("doublesided")).getBoolean();
            
            if (options.get("points") instanceof AbstractListValue abl){
                abl.forEach(x->vertex_list.add(vecFromValue(x)));
            }
            String _mode = options.getOrDefault("mode",optional.get("mode")).getString();
            inneredges = options.getOrDefault("inner",optional.get("inner")).getBoolean();
            if(vertex_list.size()<3){
                throw new IllegalArgumentException("Unexpected vertex list size: " + vertex_list.size());
            }else if(vertex_list.size()<4){
                inneredges=false;
            }
            if("polygon".equals(_mode)){
                mode=VertexFormat.Mode.TRIANGLE_FAN;
            }else if("strip".equals(_mode)){
                mode=VertexFormat.Mode.TRIANGLE_STRIP;
            }else if("triangles".equals(_mode)){
                mode=VertexFormat.Mode.TRIANGLES;
                if(vertex_list.size()%3!=0){
                    throw new IllegalArgumentException("Unexpected vertex list size: " + vertex_list.size());
                }
            }
            if (options.getOrDefault("relative",optional.get("relative")) instanceof AbstractListValue abl){
                Iterator<Value> it = abl.iterator();
                for(long i=0L;i<vertex_list.size();i++){
                    relative.add(it.hasNext()?it.next().getBoolean():false);//if part of it got defined.
                }
            }
            else if(options.getOrDefault("relative",optional.get("relative")) instanceof BooleanValue boolv){
                for(long i=0L;i<vertex_list.size();i++){
                    relative.add(boolv.getBoolean());//if it is a boolean.
                }
            }
            else{
                for(long i=0L;i<vertex_list.size();i++){
                    relative.add(true);//if there is nothing defined at all.
                }
            }
            
        }
    }

    public static class Line extends ExpiringShape
    {
        private final Set<String> required = Set.of("from", "to");
        private final Map<String, Value> optional = Map.of();
        @Override
        protected Set<String> requiredParams() { return Sets.union(super.requiredParams(), required); }
        @Override
        protected Set<String> optionalParams() { return Sets.union(super.optionalParams(), optional.keySet()); }

        private Line()
        {
            super();
        }

        Vec3 from;
        Vec3 to;

        @Override
        protected void init(Map<String, Value> options)
        {
            super.init(options);
            from = vecFromValue(options.get("from"));
            to = vecFromValue(options.get("to"));
        }

        @Override
        public Consumer<ServerPlayer> alternative()
        {
            double density = Math.max(2.0, from.distanceTo(to) /50) / (a+0.1);
            return p ->
            {
                if (p.level.dimension() == shapeDimension) drawParticleLine(
                        Collections.singletonList(p),
                        replacementParticle(p.level.registryAccess()),
                        relativiseRender(p.level, from, 0),
                        relativiseRender(p.level, to, 0),
                        density
                );
            };
        }

        @Override
        public long calcKey()
        {
            long hash = super.calcKey();
            hash ^= 2;                     hash *= 1099511628211L;
            hash ^= vec3dhash(from);       hash *= 1099511628211L;
            hash ^= vec3dhash(to);         hash *= 1099511628211L;
            return hash;
        }
    }

    public static class Sphere extends ExpiringShape
    {
        private final Set<String> required = Set.of("center", "radius");
        private final Map<String, Value> optional = Map.of("level", Value.ZERO);
        @Override
        protected Set<String> requiredParams() { return Sets.union(super.requiredParams(), required); }
        @Override
        protected Set<String> optionalParams() { return Sets.union(super.optionalParams(), optional.keySet()); }

        private Sphere()
        {
            super();
        }

        Vec3 center;
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
        public Consumer<ServerPlayer> alternative() { return p ->
        {

            int partno = Math.min(1000,20*subdivisions);
            RandomSource rand = p.level.getRandom();
            ServerLevel world = p.getLevel();
            ParticleOptions particle = replacementParticle(world.registryAccess());

            Vec3 ccenter = relativiseRender(world, center, 0 );

            double ccx = ccenter.x;
            double ccy = ccenter.y;
            double ccz = ccenter.z;

            for (int i=0; i<partno; i++)
            {
                float theta = (float)Math.asin(rand.nextDouble()*2.0-1.0);
                float phi = (float)(2*Math.PI*rand.nextDouble());

                double x = radius * Mth.cos(theta) * Mth.cos(phi);
                double y = radius * Mth.cos(theta) * Mth.sin(phi);
                double z = radius * Mth.sin(theta);
                world.sendParticles(p, particle, true,
                        x+ccx, y+ccy, z+ccz, 1,
                        0.0, 0.0, 0.0, 0.0);
            }
        };}

        @Override
        public long calcKey()
        {
            long hash = super.calcKey();
            hash ^= 3;                        hash *= 1099511628211L;
            hash ^= vec3dhash(center);        hash *= 1099511628211L;
            hash ^= Double.hashCode(radius);  hash *= 1099511628211L;
            hash ^= level;                    hash *= 1099511628211L;
            return hash;
        }
    }
    public static class Cylinder extends ExpiringShape
    {
        private final Set<String> required = Set.of("center", "radius");
        private final Map<String, Value> optional = Map.of(
                "level", Value.ZERO,
                "height", Value.ZERO,
                "axis", new StringValue("y")
        );
        @Override
        protected Set<String> requiredParams() { return Sets.union(super.requiredParams(), required); }
        @Override
        protected Set<String> optionalParams() { return Sets.union(super.optionalParams(), optional.keySet()); }

        Vec3 center;
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
            axis = Direction.Axis.byName(options.getOrDefault("axis", optional.get("axis")).getString());
        }


        @Override
        public Consumer<ServerPlayer> alternative() { return p ->
        {

            int partno = (int)Math.min(1000,Math.sqrt(20*subdivisions*(1+height)));
            RandomSource rand = p.level.getRandom();
            ServerLevel world = p.getLevel();
            ParticleOptions particle = replacementParticle(world.registryAccess());

            Vec3 ccenter = relativiseRender(world, center, 0 );

            double ccx = ccenter.x;
            double ccy = ccenter.y;
            double ccz = ccenter.z;

            if (axis == Direction.Axis.Y)
            {
                for (int i=0; i<partno; i++)
                {
                    float d = rand.nextFloat()*height;
                    float phi = (float)(2*Math.PI*rand.nextDouble());
                    double x = radius * Mth.cos(phi);
                    double y = d;
                    double z = radius * Mth.sin(phi);
                    world.sendParticles(p, particle, true, x+ccx, y+ccy, z+ccz, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
            else if (axis== Direction.Axis.X)
            {
                for (int i=0; i<partno; i++)
                {
                    float d = rand.nextFloat()*height;
                    float phi = (float)(2*Math.PI*rand.nextDouble());
                    double x = d;
                    double y = radius * Mth.cos(phi);
                    double z = radius * Mth.sin(phi);
                    world.sendParticles(p, particle, true, x+ccx, y+ccy, z+ccz, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
            else  // Z
            {
                for (int i=0; i<partno; i++)
                {
                    float d = rand.nextFloat()*height;
                    float phi = (float)(2*Math.PI*rand.nextDouble());
                    double x = radius * Mth.sin(phi);
                    double y = radius * Mth.cos(phi);
                    double z = d;
                    world.sendParticles(p, particle, true, x+ccx, y+ccy, z+ccz, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        };}

        @Override
        public long calcKey()
        {
            long hash = super.calcKey();
            hash ^= 4;                        hash *= 1099511628211L;
            hash ^= vec3dhash(center);        hash *= 1099511628211L;
            hash ^= Double.hashCode(radius);  hash *= 1099511628211L;
            hash ^= Double.hashCode(height);  hash *= 1099511628211L;
            hash ^= level;                    hash *= 1099511628211L;
            return hash;
        }
    }


    public static abstract class Param
    {
        public static Map<String, Param> of = new HashMap<String, Param>(){{
            put("mode",new StringChoiceParam("mode","polygon","strip","triangles"));
            put("relative",new OptionalBoolListParam("relative"));
            put("inner", new BoolParam("inner"));
            put("shape", new ShapeParam());
            put("dim", new DimensionParam());
            put("duration", new NonNegativeIntParam("duration"));
            put("color", new ColorParam("color"));
            put("follow", new EntityParam("follow"));
            put("variant",new StringChoiceParam("variant",
                    "NONE",
                    "THIRD_PERSON_LEFT_HAND",
                    "THIRD_PERSON_RIGHT_HAND",
                    "FIRST_PERSON_LEFT_HAND",
                    "FIRST_PERSON_RIGHT_HAND",
                    "HEAD",
                    "GUI",
                    "GROUND",
                    "FIXED")
                    {
                        public Value validate(Map o, MinecraftServer s, Value v)
                        {
                            return super.validate(o, s ,new StringValue(v.getString().toUpperCase(Locale.ROOT)));
                        }
                    });
            put("snap", new StringChoiceParam("snap",
                    "xyz", "xz", "yz", "xy", "x", "y", "z",
                    "dxdydz", "dxdz", "dydz", "dxdy", "dx", "dy", "dz",
                    "xdz", "dxz", "ydz", "dyz", "xdy", "dxy",
                    "xydz", "xdyz", "xdydz", "dxyz", "dxydz", "dxdyz"
                    ));
            put("line", new PositiveFloatParam("line"));
            put("fill", new ColorParam("fill"));

            put("from", new Vec3Param("from", false));
            put("to", new Vec3Param("to", true));
            put("center", new Vec3Param("center", false));
            put("pos", new Vec3Param("pos", false));
            put("radius", new PositiveFloatParam("radius"));
            put("level", new PositiveIntParam("level"));
            put("height", new FloatParam("height"));
            put("width", new FloatParam("width"));
            put("scale", new Vec3Param("scale", false){
                public Value validate(java.util.Map<String,Value> options, MinecraftServer server, Value value) {
                    if (value instanceof NumericValue vn){
                        value = ListValue.of(vn,vn,vn);
                    }
                    return super.validate(options, server, value);
                };
            });
            put("axis", new StringChoiceParam("axis", "x", "y", "z"));
            put("points", new PointsParam("points"));
            put("text", new FormattedTextParam("text"));
            put("value", new FormattedTextParam("value"));
            put("size", new PositiveIntParam("size"));
            put("align", new StringChoiceParam("align", "center", "left", "right"));

            put("block", new BlockParam("block"));
            put("item", new ItemParam("item"));
            put("blocklight", new NonNegativeIntParam("blocklight"));
            put("skylight", new NonNegativeIntParam("skylight"));
            put("indent", new FloatParam("indent"));
            put("raise", new FloatParam("raise"));
            put("tilt", new FloatParam("tilt"));
            put("lean", new FloatParam("lean"));
            put("turn", new FloatParam("turn"));
            put("facing", new StringChoiceParam("facing", "player", "camera", "north", "south", "east", "west", "up", "down"));
            put("doublesided", new BoolParam("doublesided"));
            put("debug", new BoolParam("debug"));

        }};
        protected String id;
        protected Param(String id)
        {
            this.id = id;
        }

        public abstract Tag toTag(Value value); //validates value, returning null if not necessary to keep it and serialize
        public abstract Value validate(Map<String, Value> options, MinecraftServer server, Value value); // makes sure the value is proper
        public abstract Value decode(Tag tag, Level level);
    }
    public static class OptionalBoolListParam extends Param
    {
        public OptionalBoolListParam(String id) { super(id); }

        @Override
        public Tag toTag(Value value) { return value.toTag(true);}
        @Override
        public Value decode(Tag tag, Level level) {
            if(tag instanceof ListTag){
                return ListValue.wrap(((ListTag)tag).stream().map(x->BooleanValue.of(((NumericTag)x).getAsNumber().doubleValue()!=0)));
            }
            if(tag instanceof ByteTag booltag){
                return BooleanValue.of(booltag.getAsByte()!=0);
            }
            return Value.NULL;
        }

        @Override
        public Value validate(Map<String, Value> options, MinecraftServer server, Value value) {
            if(value instanceof AbstractListValue lv){
                return ListValue.wrap(lv.unpack().stream().map(Value::getBoolean).map(BooleanValue::of));
            }
            if(value instanceof BooleanValue || value.isNull()){
                return value;
            }
            return BooleanValue.of(value.getBoolean());
        }
    }
    public abstract static class StringParam extends Param
    {
        protected StringParam(String id) { super(id); }

        @Override
        public Tag toTag(Value value) { return StringTag.valueOf(value.getString()); }
        @Override
        public Value decode(Tag tag, Level level) { return new StringValue(tag.getAsString()); }
    }
    public static class BlockParam extends Param
    {
        
        protected BlockParam(String id) {
            super(id);
        }
        @Override
        public Value validate(Map<String, Value> options, MinecraftServer server, Value value)
        {
            if(value instanceof BlockValue blv){
                return value;
            }
            return BlockValue.fromString(value.getString(), server.registryAccess());
        }
        @Override
        public Tag toTag(Value value) {
            if(value instanceof BlockValue blv){
                CompoundTag com = NbtUtils.writeBlockState(blv.getBlockState());
                CompoundTag dataTag = blv.getData();
                if (dataTag != null)
                {
                    com.put("TileEntityData", dataTag);
                }
                return com;
            }
            return null;
        }

        @Override
        public Value decode(Tag tag, Level level) {
            BlockState bs = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), (CompoundTag) tag);
            CompoundTag compoundTag2 = null;
            if (((CompoundTag) tag).contains("TileEntityData", 10)) {
				compoundTag2 = ((CompoundTag) tag).getCompound("TileEntityData");
            }
            return new BlockValue(bs, null, null, compoundTag2);
        }
    }
    public static class ItemParam extends Param
    {
        
        protected ItemParam(String id) {
            super(id);
        }
        @Override
        public Value validate(Map<String, Value> options, MinecraftServer server, Value value)
        {
            ItemStack item=ValueConversions.getItemStackFromValue(value, true, server.registryAccess());
            return new NBTSerializableValue(item.save(new CompoundTag()));
        }
        @Override
        public Tag toTag(Value value) {
            return ((NBTSerializableValue)value).getTag();
        }
        @Override
        public Value decode(Tag tag, Level level) {
            return new NBTSerializableValue(tag);
        }
    }
    public static class TextParam extends StringParam
    {
        protected TextParam(String id)
        {
            super(id);
        }
        @Override
        public Value validate(Map<String, Value> options, MinecraftServer server, Value value)
        {
            return value;
        }
    }
    public static class FormattedTextParam extends StringParam
    {

        protected FormattedTextParam(String id)
        {
            super(id);
        }
        @Override
        public Value validate(Map<String, Value> options, MinecraftServer server, Value value)
        {
            if (!(value instanceof FormattedTextValue))
                value = new FormattedTextValue(Component.literal(value.getString()));
            return value;
        }

        @Override
        public Tag toTag(Value value)
        {
            if (!(value instanceof FormattedTextValue))
                value = new FormattedTextValue(Component.literal(value.getString()));
            return StringTag.valueOf(((FormattedTextValue)value).serialize());
        }

        @Override
        public Value decode(Tag tag, Level level)
        {
            return FormattedTextValue.deserialize(tag.getAsString());
        }
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
        public Value validate(Map<String, Value> options, MinecraftServer server, Value value)
        {
            if (this.options.contains(value.getString())) return value;
            return null;
        }
    }

    public static class DimensionParam extends StringParam
    {
        protected DimensionParam() { super("dim"); }

        @Override
        public Value validate(Map<String, Value> options, MinecraftServer server, Value value) { return value; }
    }
    public static class ShapeParam extends StringParam
    {
        protected ShapeParam() { super("shape"); }

        @Override
        public Value validate(Map<String, Value> options, MinecraftServer server, Value value)
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
        public Value validate(Map<String, Value> options, MinecraftServer server, Value value)
        {
            if (!(value instanceof NumericValue))
                throw new InternalExpressionException("'" + id + "' needs to be a number");
            return value;
        }
    }
    public static class BoolParam extends NumericParam
    {
        protected BoolParam(String id)
        {
            super(id);
        }

        @Override
        public Tag toTag(Value value)
        {
            return ByteTag.valueOf(value.getBoolean());
        }

        @Override
        public Value decode(Tag tag, Level level)
        {
            return BooleanValue.of(((ByteTag) tag).getAsByte() > 0);
        }
    }
    public static class FloatParam extends NumericParam
    {
        protected FloatParam(String id) { super(id); }
        @Override
        public Value decode(Tag tag, Level level) { return new NumericValue(((FloatTag)tag).getAsFloat()); }
        @Override
        public Tag toTag(Value value) { return FloatTag.valueOf(NumericValue.asNumber(value, id).getFloat()); }
    }

    public static abstract class PositiveParam extends NumericParam
    {
        protected PositiveParam(String id) { super(id); }
        @Override public Value validate(Map<String, Value> options, MinecraftServer server, Value value)
        {
            Value ret = super.validate(options, server, value);
            if (((NumericValue)ret).getDouble()<=0) throw new InternalExpressionException("'"+id+"' should be positive");
            return ret;
        }
    }
    public static class PositiveFloatParam extends PositiveParam
    {
        protected PositiveFloatParam(String id) { super(id); }
        @Override
        public Value decode(Tag tag, Level level) { return new NumericValue(((FloatTag)tag).getAsFloat()); }
        @Override
        public Tag toTag(Value value) { return FloatTag.valueOf(NumericValue.asNumber(value, id).getFloat()); }

    }
    public static class PositiveIntParam extends PositiveParam
    {
        protected PositiveIntParam(String id) { super(id); }
        @Override
        public Value decode(Tag tag, Level level) { return new NumericValue(((IntTag)tag).getAsInt()); }
        @Override
        public Tag toTag(Value value) { return IntTag.valueOf(NumericValue.asNumber(value, id).getInt()); }

    }
    public static class NonNegativeIntParam extends NumericParam
    {
        protected NonNegativeIntParam(String id) { super(id); }
        @Override
        public Value decode(Tag tag, Level level) { return new NumericValue(((IntTag)tag).getAsInt()); }
        @Override
        public Tag toTag(Value value) { return IntTag.valueOf(NumericValue.asNumber(value, id).getInt()); }
        @Override public Value validate(Map<String, Value> options, MinecraftServer server, Value value)
        {
            Value ret = super.validate(options, server, value);
            if (((NumericValue)ret).getDouble()<0) throw new InternalExpressionException("'"+id+"' should be non-negative");
            return ret;
        }
    }
    public static class NonNegativeFloatParam extends NumericParam
    {
        protected NonNegativeFloatParam(String id) { super(id); }
        @Override
        public Value decode(Tag tag, Level level) { return new NumericValue(((FloatTag)tag).getAsFloat()); }
        @Override
        public Tag toTag(Value value) { return FloatTag.valueOf(NumericValue.asNumber(value, id).getFloat()); }
        @Override public Value validate(Map<String, Value> options, MinecraftServer server, Value value)
        {
            Value ret = super.validate(options, server, value);
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
        public Value validate(Map<String, Value> options, MinecraftServer server, Value value)
        {
            return validate(this, options, value, roundsUpForBlocks);
        }
        public static Value validate(Param p, Map<String, Value> options, Value value, boolean roundsUp)
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

        @Override
        public Value decode(Tag tag, Level level)
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
            tag.add(DoubleTag.valueOf(NumericValue.asNumber(lv.get(0), "x").getDouble()));
            tag.add(DoubleTag.valueOf(NumericValue.asNumber(lv.get(1), "y").getDouble()));
            tag.add(DoubleTag.valueOf(NumericValue.asNumber(lv.get(2), "z").getDouble()));
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
        public Value validate(Map<String, Value> options, MinecraftServer server, Value value)
        {
            if (!(value instanceof ListValue))
                throw new InternalExpressionException(id+ " parameter should be a list");
            List<Value> points = new ArrayList<>();
            for (Value point: ((ListValue) value).getItems())
                points.add(Vec3Param.validate(this, options, point, false));
            return ListValue.wrap(points);
        }

        @Override
        public Value decode(Tag tag, Level level)
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
                tag.add(DoubleTag.valueOf(NumericValue.asNumber(coords.get(0), "x").getDouble()));
                tag.add(DoubleTag.valueOf(NumericValue.asNumber(coords.get(1), "y").getDouble()));
                tag.add(DoubleTag.valueOf(NumericValue.asNumber(coords.get(2), "z").getDouble()));
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

        @Override
        public Value decode(Tag tag, Level level) { return new NumericValue(((IntTag)tag).getAsInt()); }
        @Override
        public Tag toTag(Value value) { return IntTag.valueOf(NumericValue.asNumber(value, id).getInt()); }
    }

    public static class EntityParam extends Param
    {

        protected EntityParam(String id) { super(id); }

        @Override
        public Tag toTag(Value value)
        {
            return IntTag.valueOf(NumericValue.asNumber(value, id).getInt());
        }

        @Override
        public Value validate(Map<String, Value> options, MinecraftServer server, Value value)
        {
            if (value instanceof EntityValue) return new NumericValue(((EntityValue) value).getEntity().getId());
            ServerPlayer player = EntityValue.getPlayerByValue(server, value);
            if (player == null)
                throw new InternalExpressionException(id+" parameter needs to represent an entity or player");
            return new NumericValue(player.getId());
        }

        @Override
        public Value decode(Tag tag, Level level) { return new NumericValue(((IntTag)tag).getAsInt()); }
    }

    private static boolean isStraight(Vec3 from, Vec3 to, double density)
    {
        if ( (from.x == to.x && from.y == to.y) || (from.x == to.x && from.z == to.z) || (from.y == to.y && from.z == to.z))
            return from.distanceTo(to) / density > 20;
        return false;
    }

    private static int drawOptimizedParticleLine(List<ServerPlayer> playerList, ParticleOptions particle, Vec3 from, Vec3 to, double density)
    {
        double distance = from.distanceTo(to);
        int particles = (int)(distance/density);
        Vec3 towards = to.subtract(from);
        int parts = 0;
        for (ServerPlayer player : playerList)
        {
            ServerLevel world = player.getLevel();
            world.sendParticles(player, particle, true,
                    (towards.x)/2+from.x, (towards.y)/2+from.y, (towards.z)/2+from.z, particles/3,
                    towards.x/6, towards.y/6, towards.z/6, 0.0);
            world.sendParticles(player, particle, true,
                    from.x, from.y, from.z,1,0.0,0.0,0.0,0.0);
            world.sendParticles(player, particle, true,
                    to.x, to.y, to.z,1,0.0,0.0,0.0,0.0);
            parts += particles/3+2;
        }
        int divider = 6;
        while (particles/divider > 1)
        {
            int center = (divider*2)/3;
            int dev = 2*divider;
            for (ServerPlayer player : playerList)
            {
                ServerLevel world = player.getLevel();
                world.sendParticles(player, particle, true,
                        (towards.x)/center+from.x, (towards.y)/center+from.y, (towards.z)/center+from.z, particles/divider,
                        towards.x/dev, towards.y/dev, towards.z/dev, 0.0);
                world.sendParticles(player, particle, true,
                        (towards.x)*(1.0-1.0/center)+from.x, (towards.y)*(1.0-1.0/center)+from.y, (towards.z)*(1.0-1.0/center)+from.z, particles/divider,
                        towards.x/dev, towards.y/dev, towards.z/dev, 0.0);
            }
            parts += 2*particles/divider;
            divider = 2*divider;
        }
        return parts;
    }

    public static int drawParticleLine(List<ServerPlayer> players, ParticleOptions particle, Vec3 from, Vec3 to, double density)
    {
        double distance = from.distanceToSqr(to);
        if (distance == 0) return 0;
        int pcount = 0;
        if (distance < 100)
        {
            RandomSource rand = players.get(0).level.random;
            int particles = (int)(distance/density)+1;
            Vec3 towards = to.subtract(from);
            for (int i = 0; i < particles; i++)
            {
                Vec3 at = from.add(towards.scale( rand.nextDouble()));
                for (ServerPlayer player : players)
                {
                    player.getLevel().sendParticles(player, particle, true,
                            at.x, at.y, at.z, 1,
                            0.0, 0.0, 0.0, 0.0);
                    pcount ++;
                }
            }
            return pcount;
        }

        if (isStraight(from, to, density)) return drawOptimizedParticleLine(players, particle, from, to, density);
        Vec3 incvec = to.subtract(from).scale(2*density/Math.sqrt(distance));

        for (Vec3 delta = new Vec3(0.0,0.0,0.0);
             delta.lengthSqr()<distance;
             delta = delta.add(incvec.scale(Sys.randomizer.nextFloat())))
        {
            for (ServerPlayer player : players)
            {
                player.getLevel().sendParticles(player, particle, true,
                        delta.x+from.x, delta.y+from.y, delta.z+from.z, 1,
                        0.0, 0.0, 0.0, 0.0);
                pcount ++;
            }
        }
        return pcount;
    }
}
