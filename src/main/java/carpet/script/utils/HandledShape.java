package carpet.script.utils;

import java.util.List;
import carpet.script.CarpetContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public abstract class HandledShape {
	protected final List<ServerPlayerEntity> playersList;
	
	public HandledShape(List<ServerPlayerEntity> playersList) {
		this.playersList = playersList;
	}
	
	/**
	 * Called every tick for the handled shape.
	 */
	public void tick() {}
	
	/**
	 * @param p The player who left. It will be removed from the handler
	 * @return Whether the shape was removed because its player list ended up empty
	 */
	public boolean isRemovedOnPlayerLeft(ServerPlayerEntity p) {
		playersList.remove(p);
		if (playersList.isEmpty())
		{
			remove();
			return true;
		}
		return false;
	}
	
	/**
	 * Removes this shape
	 */
	public abstract void remove();
	
	public static class HandledLabel extends HandledShape {
		private final CarpetArmorStandLabel entity;

		public HandledLabel(List<ServerPlayerEntity> playersList, Text text, CarpetContext cc)
		{
			super(playersList);
			this.entity = new CarpetArmorStandLabel(cc.s.getWorld(), this.playersList);
			entity.setCustomName(text);
			cc.s.getWorld().spawnEntity(entity);
		}

		@Override
		public void remove()
		{
			entity.remove();
		}
		
	}
}
