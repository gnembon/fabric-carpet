package carpet.helpers;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.settings.ParsedRule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

public class ClientNetworkHandler
{
    public static void handleData(Identifier channel, PacketByteBuf data)
    {
        if (CarpetServer.CARPET_CHANNEL.equals(channel) && data != null)
        {
            int id = data.readVarInt();
            
            if (id == 1)
                setOnJoinInfo(data);
            else if(id == 2)
                updateRule(data);
        }
    }
    
    private static void setOnJoinInfo(PacketByteBuf data)
    {
        CompoundTag compound = data.readCompoundTag();
        if (compound == null) return;
        
        TickSpeed.tickrate(compound.getFloat("Tickrate"));
        
        ListTag rulesList = compound.getList("rules", 10);
        for (Tag tag : rulesList)
        {
            CompoundTag ruleNBT = (CompoundTag) tag;
            String rule = ruleNBT.getString("rule");
            String value = ruleNBT.getString("value");
            CarpetServer.settingsManager.getRule(rule).set(null, value);
        }
    }
    
    private static void updateRule(PacketByteBuf data)
    {
        String rule = data.readString();
        String newValue = data.readString();
        CarpetServer.settingsManager.getRule(rule).set(null, newValue);
    }
}
