package codechicken.enderstorage.storage.liquid;

import codechicken.core.fluid.FluidUtils;
import codechicken.enderstorage.api.EnderStorageManager;
import codechicken.enderstorage.common.TileFrequencyOwner;
import codechicken.enderstorage.internal.EnderStorageSPH;
import codechicken.enderstorage.storage.liquid.TankSynchroniser.TankState;
import codechicken.lib.math.MathHelper;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import java.util.List;

import static codechicken.lib.vec.Vector3.center;

public class TileEnderTankHighCapacity extends TileFrequencyOwner implements IFluidHandler, ITileEnderTanks
{
    private static Cuboid6[] selectionBoxes = new Cuboid6[4];
    public static Transformation[] buttonT = new Transformation[3];

    static {
        for (int i = 0; i < 3; i++) {
            buttonT[i] = new Scale(0.6).with(new Translation(0.35 + i * 0.15, 0.91, 0.5));
            selectionBoxes[i] = selection_button.copy().apply(buttonT[i]);
        }
        selectionBoxes[3] = new Cuboid6(0.358, 0.268, 0.05, 0.662, 0.565, 0.15);
    }

    public int rotation;
    public TankStates states = new TankStates();
    public TankStates.EnderTankState liquid_state = states.new EnderTankState();
    public TankStates.PressureState pressure_state = states.new PressureState();

    private EnderLiquidStorageHighCapacity storage;
    private boolean described;

    @Override
    public void updateEntity() {
        super.updateEntity();

        pressure_state.update(worldObj.isRemote);
        if (pressure_state.a_pressure)
            ejectLiquid();

        liquid_state.update(worldObj.isRemote);
    }

    private void ejectLiquid() {
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            TileEntity t = worldObj.getTileEntity(xCoord + side.offsetX, yCoord + side.offsetY, zCoord + side.offsetZ);
            if (!(t instanceof IFluidHandler))
                continue;

            IFluidHandler c = (IFluidHandler) t;
            FluidStack liquid = drain(null, 100, false);
            if (liquid == null)
                continue;
            int qty = c.fill(side.getOpposite(), liquid, true);
            if (qty > 0)
                drain(null, qty, true);
        }
    }

    public void reloadStorage() {
        storage = (EnderLiquidStorageHighCapacity) EnderStorageManager.instance(worldObj.isRemote).getStorage(owner, freq, "liquid");
        if (!worldObj.isRemote)
            liquid_state.reloadStorage(storage);
    }

    @Override
    public EnderLiquidStorageHighCapacity getStorage() {
        return storage;
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        return storage.fill(from, resource, doFill);
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return storage.drain(from, maxDrain, doDrain);
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        return storage.drain(from, resource, doDrain);
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return storage.canDrain(from, fluid);
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return storage.canFill(from, fluid);
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        if(worldObj.isRemote)
            return new FluidTankInfo[]{new FluidTankInfo(liquid_state.s_liquid, EnderLiquidStorage.CAPACITY)};

        return storage.getTankInfo(from);
    }

    @Override
    public void onPlaced(EntityLivingBase entity) {
        rotation = (int) Math.floor(entity.rotationYaw * 4 / 360 + 2.5D) & 3;
        pressure_state.b_rotate = pressure_state.a_rotate = pressure_state.approachRotate();
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setByte("rot", (byte) rotation);
        tag.setBoolean("ir", pressure_state.invert_redstone);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        rotation = tag.getByte("rot");
        pressure_state.invert_redstone = tag.getBoolean("ir");
    }

    @Override
    public void writeToPacket(PacketCustom packet) {
        packet.writeByte(rotation);
        packet.writeFluidStack(liquid_state.s_liquid);
        packet.writeBoolean(pressure_state.a_pressure);
    }

    @Override
    public void handleDescriptionPacket(PacketCustom desc) {
        super.handleDescriptionPacket(desc);
        rotation = desc.readUByte();
        liquid_state.s_liquid = desc.readFluidStack();
        pressure_state.a_pressure = desc.readBoolean();
        if (!described) {
            liquid_state.c_liquid = liquid_state.s_liquid;
            pressure_state.b_rotate = pressure_state.a_rotate = pressure_state.approachRotate();
        }
        described = true;
    }

    @Override
    public boolean activate(EntityPlayer player, int subHit) {
        if (subHit == 4) {
            pressure_state.invert();
            return true;
        }
        return FluidUtils.fillTankWithContainer(this, player) ||
                FluidUtils.emptyTankIntoContainer(this, player, storage.getFluid());
    }

    @Override
    public void addTraceableCuboids(List<IndexedCuboid6> cuboids) {
        Vector3 pos = new Vector3(xCoord, yCoord, zCoord);
        cuboids.add(new IndexedCuboid6(0, new Cuboid6(0.15, 0, 0.15, 0.85, 0.916, 0.85).add(pos)));

        for (int i = 0; i < 4; i++)
            cuboids.add(new IndexedCuboid6(i + 1, selectionBoxes[i].copy()
                    .apply(Rotation.quarterRotations[rotation ^ 2].at(center)).add(pos)));
    }

    @Override
    public int getLightValue() {
        if (liquid_state.s_liquid.amount > 0)
            return FluidUtils.getLuminosity(liquid_state.c_liquid, liquid_state.s_liquid.amount / 16D);

        return 0;
    }

    @Override
    public boolean redstoneInteraction() {
        return true;
    }

    public void sync(PacketCustom packet) {
        if (packet.getType() == 5)
            liquid_state.sync(packet.readFluidStack());
        else if (packet.getType() == 6)
            pressure_state.a_pressure = packet.readBoolean();
    }

    @Override
    public boolean rotate() {
        if (!worldObj.isRemote) {
            rotation = (rotation + 1) % 4;
            PacketCustom.sendToChunk(getDescriptionPacket(), worldObj, xCoord >> 4, zCoord >> 4);
        }

        return true;
    }

    @Override
    public int comparatorInput() {
        FluidTankInfo tank = storage.getTankInfo(null)[0];
        return tank.fluid.amount * 14 / tank.capacity + (tank.fluid.amount > 0 ? 1 : 0);
    }

    @Override
    public int getRotation(){
        return rotation;
    }

    @Override
    public TankStates.PressureState getPressureState(){
        return pressure_state;
    }

    @Override
    public int getFreq(){
        return freq;
    }

    @Override
    public String getOwner(){
        return owner;
    }

    @Override
    public TankStates.EnderTankState getLiquidState(){
        return liquid_state;
    }


}
