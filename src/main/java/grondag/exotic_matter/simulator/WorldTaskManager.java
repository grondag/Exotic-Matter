package grondag.exotic_matter.simulator;

import java.util.concurrent.ConcurrentLinkedQueue;

import grondag.exotic_matter.ConfigXM;
import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.network.PacketHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 *  Maintains a queue of tasks that require world access and executes them in FIFO order.  
 *   
 *  Ensures that all world access is synchronized, in-order, and does not exceed
 *  the configured threshold for points per tick.
 *  
 *  Tasks are not serialized or persisted - queue must be rebuilt by
 *  task providers on world reload.
 *
 */
public class WorldTaskManager
{
    /**
     * Metered tasks - run based on budget consumption until drained.
     */
    private static ConcurrentLinkedQueue<IWorldTask> tasks = new ConcurrentLinkedQueue<IWorldTask>();
    
    /**
     * Immediate tasks - queue is fully drained each tick.
     */
    private static ConcurrentLinkedQueue<Runnable> immediateTasks = new ConcurrentLinkedQueue<Runnable>();
    
    public static void clear()
    {
        tasks.clear();
    }
    
    public static void doServerTick()
    {
        while(!immediateTasks.isEmpty())
        {
            Runnable r = immediateTasks.poll();
            r.run();
        }
        
        if(tasks.isEmpty()) return;
        
        int operations = ConfigXM.EXECUTION.maxQueuedWorldOperationsPerTick;
        
        IWorldTask task = tasks.peek();
        while(operations > 0 && task != null)
        {
            // check for canceled tasks and move to next if checked
            if(task.isDone())
            {
                tasks.poll();
                task = tasks.peek();
                continue;
            }
            
            operations -= task.runInServerTick(operations);
            
            if(task.isDone())
            {
                tasks.poll();
                task = tasks.peek();
            }
        }
    }
    
    public static void enqueue(IWorldTask task)
    {
        tasks.offer(task);
    }
    
    /**
     * Use for short-running operations that should run on next tick.
     */
    public static void enqueueImmediate(Runnable task)
    {
        immediateTasks.offer(task);
    }
    
    /**
     * Runs the task if called from the server thread. Otherwise enques for immediate execution on next tick.
     */
    public static void runOrEnqueueImmediate(Runnable task)
    {
        if(FMLCommonHandler.instance().getMinecraftServerInstance().isCallingFromMinecraftThread())
            task.run();
        else
            immediateTasks.offer(task);
    }
    
    /**
     * Use to send packets during next world tick if not running on server thread. 
     * Don't think network wrapper supports concurrent access.
     * 
     * @param forceToQueue  If false, will queue packet even if running on server thread.  Use this
     * when need to preserve sequence of packets that may be generated with correct order but
     * from different threads.
     */
    public static void sendPacketFromServerThread(IMessage message, EntityPlayerMP player, boolean sendNowIfPossible)
    {
        if(sendNowIfPossible && FMLCommonHandler.instance().getMinecraftServerInstance().isCallingFromMinecraftThread())
        {
            ExoticMatter.INSTANCE.info("sending direct packet");
            PacketHandler.CHANNEL.sendTo(message, player);
        }
        else
        {
            ExoticMatter.INSTANCE.info("sending queued packet");
            enqueueImmediate(new Runnable()
            {
                @Override
                public void run()
                {
                    PacketHandler.CHANNEL.sendTo(message, player);
                }
            });
        }
    }
}
