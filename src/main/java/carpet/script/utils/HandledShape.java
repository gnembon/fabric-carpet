package carpet.script.utils;

import java.util.Set;

import carpet.script.CarpetContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public abstract class HandledShape {
	protected final Set<ServerPlayerEntity> playersSet;
	
	public HandledShape(Set<ServerPlayerEntity> playersSet) {
		this.playersSet = playersSet;
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
		playersSet.remove(p);
		if (playersSet.isEmpty())
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

		public HandledLabel(Set<ServerPlayerEntity> playersSet, Text text, CarpetContext cc)
		{
			super(playersSet);
			this.entity = new CarpetArmorStandLabel(cc.s.getWorld(), this.playersSet);
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
