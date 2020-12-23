package codechicken.enderstorage.storage.liquid;

public interface ITileEnderTanks {
    public int getRotation();
    public TankStates.PressureState getPressureState();
    public TankStates.EnderTankState getLiquidState();
    public int getFreq();
    public String getOwner();

}
