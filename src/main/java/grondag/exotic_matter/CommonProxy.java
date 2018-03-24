package grondag.exotic_matter;

import grondag.exotic_matter.player.ModifierKeys;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;

public class CommonProxy 
{
    public void preInit(FMLPreInitializationEvent event) 
    {
        Log.setLog(event.getModLog());

        CapabilityManager.INSTANCE.register(ModifierKeys.class,new Capability.IStorage<ModifierKeys>()
        {

            @Override
            public NBTBase writeNBT(Capability<ModifierKeys> capability, ModifierKeys instance, EnumFacing side) 
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void readNBT(Capability<ModifierKeys> capability, ModifierKeys instance, EnumFacing side, NBTBase nbt)
            {
                throw new UnsupportedOperationException();
            }

        }, () -> {
            throw new UnsupportedOperationException();
        });

    }

    public void init(FMLInitializationEvent event) 
    {

    }

    public void postInit(FMLPostInitializationEvent event) 
    {
    }

    public void serverStarting(FMLServerStartingEvent event)
    {
    }

    public void serverStopping(FMLServerStoppingEvent event)
    {
    }

    public void serverAboutToStart(FMLServerAboutToStartEvent event)
    {

    }
}