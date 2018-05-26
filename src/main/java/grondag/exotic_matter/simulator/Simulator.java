package grondag.exotic_matter.simulator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.lang.Thread.UncaughtExceptionHandler;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.concurrency.PerformanceCounter;
import grondag.exotic_matter.concurrency.SimpleConcurrentList;
import grondag.exotic_matter.concurrency.ScatterGatherThreadPool;
import grondag.exotic_matter.serialization.NBTDictionary;
import grondag.exotic_matter.simulator.persistence.AssignedNumbersAuthority;
import grondag.exotic_matter.simulator.persistence.IPersistenceNode;
import grondag.exotic_matter.simulator.persistence.ISimulationTopNode;
import grondag.exotic_matter.simulator.persistence.PersistenceManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;


/**
 * Events are processed from a queue in the order they arrive.
 * 
 * World events are always added to the queue as soon as they arrive.
 * 
 * Simulation ticks are generated as the world clock advances
 * at the rate of one simulation tick per world tick.
 * 
 * Simulation ticks are added to the queue by a privileged task 
 * that is added at the end of simulation tick.  No new 
 * simulation ticks are added until all tasks in the last tick are complete.
 * 
 * No simulation ticks are ever skipped. This means that if players
 * sleep and the work clock advances, the simulation will continue
 * running as quickly as possible until caught up.  
 * 
 * However, world events will continue to be processed as soon as they
 * arrive.  This means that a player waking up and interacting with
 * machines immediately may not see that all processing is complete but
 * will observe that the machines are running very quickly.
 * 
 */
public class Simulator  implements IPersistenceNode, ForgeChunkManager.OrderedLoadingCallback
{
    
    ////////////////////////////////////////////////////////////
    // STATIC MEMBERS
    ////////////////////////////////////////////////////////////

    private static final String NBT_TAG_SIMULATOR = NBTDictionary.claim("emSimulator");
    private static final String NBT_TAG_LAST_TICK = NBTDictionary.claim("simLastTick");
    private static final String NBT_TAG_WORLD_TICK_OFFSET = NBTDictionary.claim("simTickOffset");
    
    /**
     * Only use if need a reference before it starts.
     */
    public static final Simulator RAW_INSTANCE_DO_NOT_USE = new Simulator();
    
    /**
     * Needed to prevent overhead of retrieving instance each time this is needed.  Needed all over.
     */
    private static int currentTick;
    
    /**
     * Needed to prevent overhead of retrieving instance each time this is needed.  Needed all over.
     */
    public static final int currentTick()
    {
        return currentTick;
    }
    
    private static final HashSet<Class<? extends ISimulationTopNode>> nodeTypes = new HashSet<>();
    
    public static void register(Class<? extends ISimulationTopNode> nodeType)
    {
        nodeTypes.add(nodeType);
    }
    
    
    public static final ScatterGatherThreadPool SCATTER_GATHER_POOL = new ScatterGatherThreadPool();
    
    /**
     * General-purpose thread pool. Use for any simulation-related activity
     * so long as it doesn't have specific timing or sequencing requirements.
     */
    @SuppressWarnings("null")
    @Deprecated
    public static final ForkJoinPool SIMULATION_POOL = new ForkJoinPool(
            Runtime.getRuntime().availableProcessors(),
            new ForkJoinWorkerThreadFactory()
            {
                private AtomicInteger count = new AtomicInteger(1);

                @Override
                public ForkJoinWorkerThread newThread(ForkJoinPool pool)
                {
                    ForkJoinWorkerThread result = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                    result.setName("Exotic Matter Simulation Thread -" + count.getAndIncrement());
                    return result;
                }
            },
            new UncaughtExceptionHandler()
            {
                @Override
                public void uncaughtException(Thread t, Throwable e)
                {
                    ExoticMatter.INSTANCE.getLog().error("Simulator thread terminated due to uncaught exception.  Badness may ensue.", e);
                }}, 
            true);

    /**
     * For simulation step control - do not use for actual work.
     */
    @SuppressWarnings("null")
    private static final ExecutorService CONTROL_THREAD = Executors.newSingleThreadExecutor(
            new ThreadFactory()
            {
                private AtomicInteger count = new AtomicInteger(1);
                @Override
                public Thread newThread(@Nullable Runnable r)
                {
                    Thread thread = new Thread(r, "Hard Science Simulation Control Thread -" + count.getAndIncrement());
                    thread.setDaemon(true);
                    return thread;
                }
            });
    
    ////////////////////////////////////////////////////////////
    // INSTANCE MEMBERS
    ////////////////////////////////////////////////////////////
    
    
    private AssignedNumbersAuthority assignedNumbersAuthority = new AssignedNumbersAuthority();

    public AssignedNumbersAuthority assignedNumbersAuthority() { return this.assignedNumbersAuthority; }
    
    private final IdentityHashMap<Class<? extends ISimulationTopNode>, ISimulationTopNode> nodes = new IdentityHashMap<>();

    private List<ISimulationTickable> tickables = new ArrayList<ISimulationTickable>();

    private @Nullable Future<?> lastTickFuture = null;

    /** used for world time */
    private @Nullable World world;

    private boolean isDirty;

    private volatile boolean isRunning = false;
    public boolean isRunning() { return isRunning; }

    /** true if we've warned once about clock going backwards - prevents log spam */
    private boolean isClockSetbackNotificationNeeded = true;

    //   private AtomicInteger nextNodeID = new AtomicInteger(NodeRoots.FIRST_NORMAL_NODE_ID);
    //    private static final String TAG_NEXT_NODE_ID = "nxid";


    /** 
     * Set to worldTickOffset + lastWorldTick at end of server tick.
     * If equal to currentSimTick, means simulation is caught up with world ticks.
     */
    private volatile int lastSimTick = 0;

    /** worldTickOffset + lastWorldTick = max value of current simulation tick.
     * Updated on server post tick, *after* all world tick events should be submitted.
     */
    private volatile long worldTickOffset = 0; 
    
    @SuppressWarnings("unchecked")
    @Nullable
    public <V extends ISimulationTopNode> V getNode(Class<V> nodeType)
    {
        return (V) this.nodes.get(nodeType);
    }
    
    private void start()  
    {
        synchronized(this)
        {
            ExoticMatter.INSTANCE.info("Simulator initialization started.");
    
            ChunkLoader.clear();
            
            this.assignedNumbersAuthority.clear();
            this.assignedNumbersAuthority.setDirtKeeper(this);
            
            // we're going to assume for now that all the dimensions we care about are using the overworld clock
            this.world = FMLCommonHandler.instance().getMinecraftServerInstance().worlds[0];
            MapStorage mapStore = this.world.getMapStorage();
        
            this.tickables.clear();
            
            this.nodes.clear();
            nodeTypes.forEach(t -> {
                try
                {
                    ISimulationTopNode node = t.newInstance();
                    this.nodes.put(t, node);
                    node.afterCreated(this);
                }
                catch (Exception e)
                {
                    ExoticMatter.INSTANCE.error("Unable to create simulation node " + t.getName(),  e);
                }
            });
            
            
            
            if(PersistenceManager.loadNode(mapStore, this))
            {
                for(ISimulationTopNode node : this.nodes.values())
                {
                    if(!PersistenceManager.loadNode(mapStore, node))
                    {
                        ExoticMatter.INSTANCE.warn("Persisted date for %s not found.  Some world state may be lost.", node.getClass().getName());
                        node.loadNew();
                        PersistenceManager.registerNode(mapStore, node);  
                    }
                }
            }
            else
            {
                ExoticMatter.INSTANCE.info("Creating new simulation.");
                
                // Assume new game and new simulation
                this.lastSimTick = 0; 
                this.worldTickOffset = -this.world.getWorldTime();

                this.setSaveDirty(true);
                PersistenceManager.registerNode(mapStore, this);

                for(ISimulationTopNode node : this.nodes.values())
                {
                    node.loadNew();
                    PersistenceManager.registerNode(mapStore, node);  
                }
                
            }
            
            this.nodes.values().forEach(n -> n.afterDeserialization());

            this.nodes.values().forEach(n -> {if(n instanceof ISimulationTickable) this.tickables.add((ISimulationTickable) n);});
 
            
            ExoticMatter.INSTANCE.info("Simulator initialization complete. Simulator running.");
        }
    }

    /** 
     * Called from ServerStopping event.
     * Should be no more ticks after that.
     */
    public synchronized void stop()
    {
        ExoticMatter.INSTANCE.info("stopping server");
        this.isRunning = false;

        // wait for simulation to catch up
        if(this.lastTickFuture != null && !this.lastTickFuture.isDone())
        {
            ExoticMatter.INSTANCE.info("waiting for last frame task completion");
            try
            {
                this.lastTickFuture.get(5, TimeUnit.SECONDS);
            }
            catch (Exception e)
            {
                ExoticMatter.INSTANCE.warn("Timeout waiting for simulation shutdown");
                e.printStackTrace();
            }
        }

        nodes.values().forEach(n -> n.unload());
        nodes.clear();
        
        this.world = null;
        this.lastTickFuture = null;
    }

    public void onServerTick(ServerTickEvent event) 
    {
        if(this.isRunning)
        {
           
            if(lastTickFuture == null || lastTickFuture.isDone())
            {

                int newLastSimTick = (int) (world.getWorldTime() + this.worldTickOffset);

                // Simulation clock can't move backwards.
                // NB: don't need CAS because only ever changed by game thread in this method
                if(newLastSimTick > lastSimTick)
                {
                    // if((newLastSimTick & 31) == 31) HardScience.log.info("changing lastSimTick, old=" + lastSimTick + ", new=" + newLastSimTick);
                    this.isDirty = true;
                    this.lastSimTick = newLastSimTick;          
                }
                else
                {
                    // world clock has gone backwards or paused, so readjust offset
                    this.lastSimTick++;
                    this.worldTickOffset = this.lastSimTick - world.getWorldTime();
                    this.setSaveDirty(true);
                    if(isClockSetbackNotificationNeeded)
                    {
                        ExoticMatter.INSTANCE.warn("World clock appears to have run backwards.  Simulation clock offset was adjusted to compensate.");
                        ExoticMatter.INSTANCE.warn("Next tick according to world was " + newLastSimTick + ", using " + this.lastSimTick + " instead.");
                        ExoticMatter.INSTANCE.warn("If this recurs, simulation clock will be similarly adjusted without notification.");
                        isClockSetbackNotificationNeeded = false;
                    }
                }

                currentTick = lastSimTick;
                
                if(!Simulator.this.tickables.isEmpty())
                {
                    for(ISimulationTickable tickable : Simulator.this.tickables)
                    {
                        tickable.doOnTick();
                    }
                }

                lastTickFuture = CONTROL_THREAD.submit(this.offTickFrame);
            }
        }
    }

    @Override
    public void deserializeNBT(@Nullable NBTTagCompound nbt)
    {
        ExoticMatter.INSTANCE.info("Simulator read from NBT");
        this.assignedNumbersAuthority.deserializeNBT(nbt);
        this.lastSimTick = nbt.getInteger(NBT_TAG_LAST_TICK);
        this.worldTickOffset = nbt.getLong(NBT_TAG_WORLD_TICK_OFFSET);
    }

    @Override
    public void serializeNBT(NBTTagCompound nbt)
    {
        ExoticMatter.INSTANCE.info("saving simulation state");
        this.assignedNumbersAuthority.serializeNBT(nbt);
        nbt.setInteger(NBT_TAG_LAST_TICK, lastSimTick);
        nbt.setLong(NBT_TAG_WORLD_TICK_OFFSET, worldTickOffset);
    }

    public @Nullable World getWorld() { return this.world; }

    // Frame execution logic
    Runnable offTickFrame = new Runnable()
    {
        @Override
        public void run()
        {
            if(!Simulator.this.tickables.isEmpty())
            {
                for(ISimulationTickable tickable : Simulator.this.tickables)
                {
                    try
                    {
                        tickable.doOffTick();
                    }
                    catch(Exception e)
                    {
                        ExoticMatter.INSTANCE.error("Exception during simulator off-tick processing", e);
                    }
                }
            }
        }    
    };

    // CHUNK LOADING START UP HANDLERS

    @Override
    public void ticketsLoaded(@Nullable List<Ticket> tickets, @Nullable World world)
    {
        // For volcanos we re-force chunks when simulation loaded
        // or when activation changes. Should get no tickets.
        ;
    }

    @Override
    public List<Ticket> ticketsLoaded(@Nullable List<Ticket> tickets, @Nullable World world, int maxTicketCount)
    {
        // For volcanos we re-force chunks when simulation loaded
        // or when activation changes. Dispose of all tickets.
        List<ForgeChunkManager.Ticket> validTickets = Lists.newArrayList();
        return validTickets;
    }

    @Override
    public boolean isSaveDirty()
    {
        return this.isDirty;
    }

    @Override
    public void setSaveDirty(boolean isDirty)
    {
        this.isDirty = true;

    }

    @Override
    public String tagName()
    {
        return NBT_TAG_SIMULATOR;
    }

    public static <T> void runTaskAppropriately(SimpleConcurrentList<T> list, Consumer<T> action, int concurrencyThreshold, PerformanceCounter counter)
    {
        counter.startRun();
        if(list.size() > concurrencyThreshold)
        {
            try
            {
                SIMULATION_POOL.submit(() -> list.stream(true).forEach(action)).get(1, TimeUnit.SECONDS);
            }
            catch (InterruptedException | ExecutionException | TimeoutException e)
            {
                ExoticMatter.INSTANCE.error("Unexpected error", e);
            }
        }
        else
        {
            list.stream(false).forEach( action);
        }
        counter.endRun();
        counter.addCount(list.size());
    }
    
    public static <T> void runTaskAppropriately(T[] array, int startInclusive, int endExclusive, Consumer<T> action, int concurrencyThreshold, PerformanceCounter counter)
    {
        counter.startRun();
        final int size = endExclusive - startInclusive;
        Stream<T> s = StreamSupport.stream(Arrays.spliterator(array, startInclusive, endExclusive), size > concurrencyThreshold);
        
        if(size > concurrencyThreshold)
        {
            try
            {
                SIMULATION_POOL.submit(() -> s.forEach(action)).get(1, TimeUnit.SECONDS);
            }
            catch (InterruptedException | ExecutionException | TimeoutException e)
            {
                ExoticMatter.INSTANCE.error("Unexpected error", e);
            }
        }
        else
        {
            s.forEach( action);
        }
        counter.endRun();
        counter.addCount(size);
    }

    public static Simulator instance()
    {
        loadSimulatorIfNotLoaded();
        return RAW_INSTANCE_DO_NOT_USE;
        
    }

    /**
     * Simulator is lazily loaded because needs world to be loaded
     * but is also referenced by tile entities during chunk load.
     * No forge event that lets us load after worlds loaded but
     * before chunk loading, so using start reference as the trigger.
     */
    public static void loadSimulatorIfNotLoaded()
    {
        // If called from world thread before loaded,
        // want to block and complete loading before return.
        // However, if the load process (running on the calling
        // thread) makes a re-entrant call we want to return the 
        // instance so that loading can progress.
        
        if(RAW_INSTANCE_DO_NOT_USE.isRunning) return;
        synchronized(RAW_INSTANCE_DO_NOT_USE)
        {
            RAW_INSTANCE_DO_NOT_USE.isRunning = true;
            RAW_INSTANCE_DO_NOT_USE.start();
        }
    }
}
