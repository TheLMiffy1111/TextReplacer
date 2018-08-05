package thelm.textreplacer.matchers;

import java.util.function.Predicate;

import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWallSign;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class SignMatcher implements Predicate<TileEntitySign> {

	private BlockPos pos = null;
	private boolean wall = true;
	private boolean standing = true;
	private EnumFacing wallFacing = null;
	private int standingRotation = -1;

	public void setPos(BlockPos pos) {
		this.pos = pos;
	}

	public void setWall(boolean wall) {
		this.wall = wall;
	}

	public void setStanding(boolean standing) {
		this.standing = standing;
	}

	public void setWallFacing(EnumFacing wallFacing) {
		this.wallFacing = wallFacing;
	}

	public void setStandingRotation(int standingRotation) {
		this.standingRotation = standingRotation;
	}

	@Override
	public boolean test(TileEntitySign sign) {
		IBlockState state = sign.getWorld().getBlockState(sign.getPos());
		return (this.pos == null || sign.getPos().equals(this.pos)) &&
				(state.getBlock() instanceof BlockWallSign && this.wall &&
						(wallFacing == null || state.getValue(BlockWallSign.FACING) == this.wallFacing) ||
						state.getBlock() instanceof BlockStandingSign && this.standing &&
						(standingRotation == -1 || state.getValue(BlockStandingSign.ROTATION) == this.standingRotation));
	}
}
