package carpet.script.utils;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SavedBlockChange {
  public final World world;
  public final BlockPos pos;
  public final BlockState state;
  public final CompoundTag entity;

  public SavedBlockChange(World world, BlockPos pos, BlockState state, CompoundTag entity) {
    this.world = world;
    this.pos = pos;
    this.state = state;
    this.entity = entity;
  }
}
