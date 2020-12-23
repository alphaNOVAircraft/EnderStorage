package codechicken.enderstorage.storage.liquid;

import codechicken.enderstorage.common.TileFrequencyOwner;
import codechicken.enderstorage.internal.EnderStorageSPH;
import codechicken.lib.math.MathHelper;
import codechicken.lib.packet.PacketCustom;

public class TankStates extends TileFrequencyOwner {

    public class EnderTankState extends TankSynchroniser.TankState
    {
        @Override
        public void sendSyncPacket() {
            PacketCustom packet = new PacketCustom(EnderStorageSPH.channel, 5);
            packet.writeCoord(xCoord, yCoord, zCoord);
            packet.writeFluidStack(s_liquid);
            packet.sendToChunk(worldObj, xCoord >> 4, zCoord >> 4);
        }

        @Override
        public void onLiquidChanged() {
            worldObj.func_147451_t(xCoord, yCoord, zCoord);
        }
    }

    public class PressureState
    {
        public boolean invert_redstone;
        public boolean a_pressure;
        public boolean b_pressure;

        public double a_rotate;
        public double b_rotate;

        public void update(boolean client) {
            if (client) {
                b_rotate = a_rotate;
                a_rotate = MathHelper.approachExp(a_rotate, approachRotate(), 0.5, 20);
            } else {
                b_pressure = a_pressure;
                a_pressure = worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord) != invert_redstone;
                if (a_pressure != b_pressure)
                    sendSyncPacket();
            }
        }

        public double approachRotate() {
            return a_pressure ? -90 : 90;
        }

        private void sendSyncPacket() {
            PacketCustom packet = new PacketCustom(EnderStorageSPH.channel, 6);
            packet.writeCoord(xCoord, yCoord, zCoord);
            packet.writeBoolean(a_pressure);
            packet.sendToChunk(worldObj, xCoord >> 4, zCoord >> 4);
        }

        public void invert() {
            invert_redstone = !invert_redstone;
            worldObj.getChunkFromBlockCoords(xCoord, zCoord).setChunkModified();
        }
    }

    @Override
    public void reloadStorage(){
    };

    @Override
    public EnderLiquidStorage getStorage() {
        return null;
    }

}
