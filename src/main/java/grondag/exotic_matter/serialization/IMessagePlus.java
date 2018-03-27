package grondag.exotic_matter.serialization;

import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Just like IMessage but avoids recasting to PacketBuffer on nested calls.
 */
public interface IMessagePlus extends IMessage
{
    @Override
    public default void fromBytes(@Nullable ByteBuf buf)
    {
        this.fromBytes(new PacketBuffer(buf));
    }
    
    public void fromBytes(PacketBuffer pBuff);

    @Override
    public default void toBytes(@Nullable ByteBuf buf)
    {
        this.toBytes(new PacketBuffer(buf));
        
    }
    
    public void toBytes(PacketBuffer pBuff);

}
