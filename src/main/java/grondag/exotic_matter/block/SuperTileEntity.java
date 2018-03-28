package grondag.exotic_matter.block;


import javax.annotation.Nonnull;

import grondag.exotic_matter.model.ISuperBlock;
import grondag.exotic_matter.model.ISuperModelState;
import grondag.exotic_matter.model.ModelState;
import grondag.exotic_matter.serialization.NBTDictionary;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class SuperTileEntity extends TileEntity
{
    ////////////////////////////////////////////////////////////////////////
    //  STATIC MEMBERS
    ////////////////////////////////////////////////////////////////////////
    
    /**
     * Anything stored in this tag will not be sent to clients.
     */
    public static final String NBT_SERVER_SIDE_TAG = NBTDictionary.claim("serverOnly");
    
    /** Returns server-side tag if one is present, creating it if not. */
    public static @Nonnull NBTTagCompound getServerTag(@Nonnull NBTTagCompound fromTag)
    {
        NBTBase result = fromTag.getTag(NBT_SERVER_SIDE_TAG);
        if(result == null || result.getId() != 10)
        {
            result = new NBTTagCompound();
            fromTag.setTag(NBT_SERVER_SIDE_TAG, result);
        }
        return (NBTTagCompound) result;
    }
    
    /** Returns tag stripped of server-side tag if it is present. 
     * If the tag must be stripped, returns a modified copy. Otherwise returns input tag.
     * Will return null if a null tag is passed in.
     */
    public static NBTTagCompound withoutServerTag(NBTTagCompound inputTag)
    {
        if(inputTag != null && inputTag.hasKey(NBT_SERVER_SIDE_TAG))
        {
            inputTag = inputTag.copy();
            inputTag.removeTag(NBT_SERVER_SIDE_TAG);
        }
        return inputTag;
    }
    
    /**
     * Will be updated to actual game render distance on client side.
     */
    private static int maxSuperBlockRenderDistanceSq = 4096;
    
    ////////////////////////////////////////////////////////////////////////
    //  INSTANCE MEMBERS
    ////////////////////////////////////////////////////////////////////////
    
    protected ISuperModelState modelState = new ModelState();
  
    //  public IExtendedBlockState exBlockState;
    private boolean isModelStateCacheDirty = true;

    /**
     * Called client side at start up and when setting is changed.
     */
    public static void updateRenderDistance()
    {
        int configuredDist = Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16;
        maxSuperBlockRenderDistanceSq = configuredDist * configuredDist;
    }
    
    @Override
    public double getMaxRenderDistanceSquared()
    {
        return maxSuperBlockRenderDistanceSq;
    }
    
    @Override
    public boolean shouldRefresh(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState oldState, @Nonnull IBlockState newSate) 
    {
        if(oldState.getBlock() == newSate.getBlock())
        {
            return false;
        }
        else
        {
            if(world.isRemote) updateClientRenderState();
            return true;
        }
    }
 
    public void updateClientRenderState()
    {
        this.isModelStateCacheDirty = true;

        invalidateClientCache(pos.up());
        invalidateClientCache(pos.down());
        invalidateClientCache(pos.east());
        invalidateClientCache(pos.west());
        invalidateClientCache(pos.north());
        invalidateClientCache(pos.south());

        invalidateClientCache(pos.up().east());
        invalidateClientCache(pos.up().west());
        invalidateClientCache(pos.up().north());
        invalidateClientCache(pos.up().south());

        invalidateClientCache(pos.down().east());
        invalidateClientCache(pos.down().west());
        invalidateClientCache(pos.down().north());
        invalidateClientCache(pos.down().south());

        invalidateClientCache(pos.north().east());
        invalidateClientCache(pos.north().west());
        invalidateClientCache(pos.south().east());
        invalidateClientCache(pos.south().west());

        invalidateClientCache(pos.up().north().east());
        invalidateClientCache(pos.up().south().east());
        invalidateClientCache(pos.up().north().west());
        invalidateClientCache(pos.up().south().west());

        invalidateClientCache(pos.down().north().east());
        invalidateClientCache(pos.down().south().east());
        invalidateClientCache(pos.down().north().west());
        invalidateClientCache(pos.down().south().west());

        this.world.markBlockRangeForRenderUpdate(pos.up().north().east(), pos.down().south().west());

    }

    private void invalidateClientCache(BlockPos updatePos)
    {
        TileEntity target = this.world.getTileEntity(updatePos);
        if(target != null && target instanceof SuperTileEntity)
        {
            ((SuperTileEntity)target).isModelStateCacheDirty = true;
        }
    }

    /**
     * Generate tag sent to client when block/chunk start loads.
     * MUST include x, y, z tags so client knows which TE belong to.
     * Super calls writeInternal() instead of {@link #writeToNBT(NBTTagCompound)}
     * We call writeToNBT so that we include all info, but filter out
     * server-side only tag to prevent wastefully large packets.
     */
    @Override
    public NBTTagCompound getUpdateTag()
    {
        return withoutServerTag(writeToNBT(super.getUpdateTag()));
    }

    /**
     * Generate packet sent to client for TE synch after block/chunk is loaded.
     * Is inefficient that the information is serialized twice: start to NBT
     * then to ByteBuffer but that is how MC does it and the packet only accepts NBT.
     */
    @Override
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        return new SPacketUpdateTileEntity(this.pos, getBlockMetadata(), this.getUpdateTag());
    }

    /**
     * Process packet sent to client for TE synch after block/chunk is loaded.
     */
    @Override
    public void onDataPacket(@Nonnull NetworkManager net, @Nonnull SPacketUpdateTileEntity pkt) 
    {
        handleUpdateTag(pkt.getNbtCompound());
    }

    @Override
    public final void readFromNBT(@Nonnull NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.readModNBT(compound);
    }
    
    /**
     * Restores all state previously serialized by {@link #writeModNBT(NBTTagCompound)}<p>
     * 
     * Note: important that tags used here match those used PlacementItem helper methods.
     */
    public void readModNBT(NBTTagCompound compound)
    {
        this.modelState.deserializeNBT(compound);
        this.onModelStateChange(true);
    }
    
    /**
     * Stores all state for this mod to the given tag.
     * Used internally for serialization but can also be used to restore state from ItemStack<p>
     * 
     * Note: important that tags used here match those used PlacementItem helper methods
     */
    public void writeModNBT(NBTTagCompound compound)
    {
        this.modelState.serializeNBT(compound);
    }
    

    @Override
    public final NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound)
    {
        this.writeModNBT(compound);
        return super.writeToNBT(compound);
    }

    public ISuperModelState getModelState(IBlockState state, IBlockAccess world, BlockPos pos, boolean refreshFromWorldIfNeeded)
    { 
        if(this.modelState == null)
        {
            this.modelState = ((ISuperBlock)state.getBlock()).getDefaultModelState();
            this.isModelStateCacheDirty = true;
            
            // necessary for species
            refreshFromWorldIfNeeded = true;
        }
        
        if(this.isModelStateCacheDirty && refreshFromWorldIfNeeded)
        {
            this.modelState.refreshFromWorld(state, world, pos);
            this.isModelStateCacheDirty = false;
        }
        
        
        return this.modelState; 
    }
    
    /**
     * Use this version when you don't have world state handy
     */
    public ISuperModelState getModelState()
    {
        if(!(this.modelState == null || this.isModelStateCacheDirty)) 
        {
            return this.modelState;
        }
        else
        {
            return this.getModelState(this.world.getBlockState(this.pos), world, pos, true); 
        }
    }
    
    /** intended for use in TESR - don't refresh unless missing because should be up to date from getExtendedState called before this */
    public ISuperModelState getCachedModelState()
    {
        return this.modelState == null ? this.getModelState(this.world.getBlockState(this.pos), world, pos, true) : this.modelState;
    }
    
    public void setModelState(ISuperModelState modelState) 
    { 
        // if making existing appearance static, don't need to refresh on client side
        boolean needsClientRefresh = this.world != null
                && this.world.isRemote
                && !(
                        this.modelState != null  
                        && this.modelState.equals(modelState)
                        && modelState.isStatic()
                        && this.modelState.isStatic() != modelState.isStatic());
            {
                this.modelState = modelState;
                this.onModelStateChange(!modelState.isStatic());
            }
      
        this.modelState = modelState;
        this.onModelStateChange(needsClientRefresh);
    }
    
    /** call whenever modelState changes (or at least probably did).
     * Parameter should always be true except in case of changing
     * dynamic blocks to static without altering appearance. 
     */
    protected void onModelStateChange(boolean refreshClientRenderState)
    {
        /**
         * This can be called by onBlockPlaced after we've already been established.
         * If that happens, need to treat it like an update, markDirty(), refresh client state, etc.
         */
        this.isModelStateCacheDirty = true;
        if(this.world !=null)
        {
            if(this.world.isRemote)
            {
                if(refreshClientRenderState) this.updateClientRenderState();
            }
            else
            {
                this.markDirty();
            }
        }
    }
    
    /**
     * Only true for virtual blocks.  Prevents "instanceof" checking.
     */
    public boolean isVirtual() { return false; }
    
    @SideOnly(Side.CLIENT)
    private net.minecraft.util.math.AxisAlignedBB renderBB;
    
    /**
     * Cache result. Gets called alot for TESR. <br><br>
     * 
     * {@inheritDoc}
     */
    @SideOnly(Side.CLIENT)
    @Override
    public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox()
    {
        net.minecraft.util.math.AxisAlignedBB result = this.renderBB;
        if(result == null)
        {
            result = new net.minecraft.util.math.AxisAlignedBB(getPos().add(-1, 0, -1), getPos().add(1, 1, 1));
            this.renderBB = result;
        }
        return result; 
    }
    
}