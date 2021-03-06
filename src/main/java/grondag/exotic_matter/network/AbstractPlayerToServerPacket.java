package grondag.exotic_matter.network;

import javax.annotation.Nullable;

import grondag.exotic_matter.serialization.IMessagePlus;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public abstract class AbstractPlayerToServerPacket<T extends IMessagePlus> implements IMessageHandler<T, IMessage>, IMessagePlus
{

    @Override
    public IMessage onMessage(final @Nullable T message, @Nullable MessageContext context) {
        final EntityPlayerMP player = context.getServerHandler().player;

        player.getServerWorld().addScheduledTask(() -> handle(message, player));

        return null;
    }

    protected abstract void handle(T message, EntityPlayerMP player);

}