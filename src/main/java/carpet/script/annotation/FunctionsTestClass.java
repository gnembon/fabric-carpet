package carpet.script.annotation;

import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.network.packet.s2c.play.TitleS2CPacket.Action;

import java.util.Locale;

public class FunctionsTestClass {
	
	@LazyFunction
	public LazyValue make_noise(Value string, Value string2) {
		Value retVal = StringValue.of(string.getString()+ " "+string2.getString());
		return (c, t) -> retVal;
	}
	
	@LazyFunction(maxParams = 8)
	public LazyValue multiparams(Value... values) {
		String str = "";
		for(Value val : values)
			str += val.getString();
		Value retval = StringValue.of(str);
		return (c,t) -> retval;
	}
	
	@LazyFunction(maxParams = 6)
	public LazyValue semi_multiparams(Value fixed, Value... values) {
		String str = fixed.getString();
		for (Value val : values)
			str += val.getString();
		Value retval = StringValue.of(str);
		return (c,t) -> retval;
	}
	
	//@LazyFunction(maxParams = 6) TODO This is a concept
	public Value display_title(ServerPlayerEntity target, String actionString, Text content, Integer... times) {
	    TitleS2CPacket.Action action;
	    switch (actionString.toLowerCase(Locale.ROOT))
	    {
	        case "title":
	            action = Action.TITLE;
	            break;
	        case "subtitle":
	            action = Action.SUBTITLE;
	            break;
	        case "actionbar":
	            action = Action.ACTIONBAR;
	            break;
	        default:
	            throw new InternalExpressionException("'display_title' requires 'title', 'subtitle' or 'actionbar' as second argument");
	    }
	    if (times.length == 3)
	    {
	        target.networkHandler.sendPacket(new TitleS2CPacket(Action.TIMES, null, times[0], times[1], times[2]));
	    }
	    target.networkHandler.sendPacket(new TitleS2CPacket(action, content));
	    return Value.TRUE;
	}
}
