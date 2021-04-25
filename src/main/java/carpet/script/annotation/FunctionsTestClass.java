package carpet.script.annotation;

import carpet.logging.HUDController;
import carpet.script.LazyValue;
import carpet.script.annotation.Param.AllowSingleton;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.BaseText;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static net.minecraft.network.packet.s2c.play.TitleS2CPacket.Action;

public class FunctionsTestClass {
	@LazyFunction
	public String make_noise(String string, String string2) {
		return string + " " + string2;
	}
	
	@LazyFunction(maxParams = 8)
	public LazyValue multiparams(String... values) {
		String str = "";
		for (String val : values)
			str += val;
		Value retval = StringValue.of(str);
		return (c, t) -> retval;
	}
	
	@LazyFunction(maxParams = 6)
	public LazyValue semi_multiparams(Value fixed, Value... values) {
		String str = fixed.getString();
		for (Value val : values)
			str += val.getString();
		Value retval = StringValue.of(str);
		return (c, t) -> retval;
	}
	
	@LazyFunction(maxParams = 6)
	public Integer display_title2(@AllowSingleton List<ServerPlayerEntity> targets, String actionString, Optional<Text> content, Integer... times) {
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
	        case "clear":
	            action = Action.CLEAR;
	            break;
	        case "player_list_header":
	        case "player_list_footer":
	            action = null;
	            break;
	        default:
	            throw new InternalExpressionException("'display_title' requires 'title', 'subtitle', 'actionbar', 'player_list_header', 'player_list_footer' or 'clear' as second argument");
	    }
	    if (action == null) // Player list
	    {
	        Map<ServerPlayerEntity, BaseText> map = actionString.equals("player_list_header") ? HUDController.scarpet_headers : HUDController.scarpet_footers;
	        if (!content.isPresent()) // null
	            for (ServerPlayerEntity p : targets)
	                map.remove(p);
	        else
	            for (ServerPlayerEntity p : targets)
	                map.put(p, (BaseText) content.get());
	        return targets.size();
	    }
	    targets.forEach(p -> {
	        if (times.length == 3) p.networkHandler.sendPacket(new TitleS2CPacket(Action.TIMES, null, times[0], times[1], times[2]));
	        p.networkHandler.sendPacket(new TitleS2CPacket(action, content.orElse(null)));
	    });
	    return targets.size();
	}
}
