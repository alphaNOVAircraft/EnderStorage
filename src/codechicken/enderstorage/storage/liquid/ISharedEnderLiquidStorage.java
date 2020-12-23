package codechicken.enderstorage.storage.liquid;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

public interface ISharedEnderLiquidStorage extends IFluidHandler {

    public FluidStack getFluid();
    public int getFreq();
    public String getOwner();

}
