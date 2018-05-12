package grondag.exotic_matter.concurrency;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

import com.google.common.collect.ImmutableList;

public class StupidThreadPoolExecutor
{
    public static interface IIndexedRunnable
    {
        public void run(int index);
    }
    
    private final Object runLock = new Object();
    
    private final Object startNotifier = new Object();
    
    private final Object completionNotifier = new Object();
    
    private IIndexedRunnable thingsNeedingDone;
    
    private final AtomicInteger nextIndex = new AtomicInteger();
    
    private final AtomicInteger completionCount = new AtomicInteger();
    
    private final int poolSize;
    
    private final int completedFlags;
    
    private int endIndex;
    
    private final ImmutableList<Thread> threads;
    
    public StupidThreadPoolExecutor()
    {
        this.poolSize = Runtime.getRuntime().availableProcessors();
        this.completedFlags = (1 << this.poolSize) - 1;
        
        ImmutableList.Builder<Thread> builder = ImmutableList.builder();
        
        for(int i = 0; i < this.poolSize; i++)
        {
            Thread thread = new Thread(
                    new DoerOfThings(i), 
                    "Hard Science Simulation Thread - " + i);
            thread.setDaemon(true);
            builder.add(thread);
            thread.start();
        }
        this.threads = builder.build();
    }
    
    private class DoerOfThings implements Runnable
    {
        private final int threadFlag;
        
        private DoerOfThings(int threadIndex)
        {
            this.threadFlag = 1 << threadIndex;
        }
        
        @Override
        public void run()
        {
            while(true)
            {
                int i;
                if(thingsNeedingDone == null || (i = nextIndex.getAndIncrement()) >= endIndex)
                {
                    if(completionCount.updateAndGet(new IntUnaryOperator() {
                        @Override
                        public int applyAsInt(int operand)
                        {
                            return operand | threadFlag;
                        }}) == completedFlags)
                    {
                        synchronized(completionNotifier) { completionNotifier.notify();}
                    }
                    
                    try
                    {
                        synchronized(startNotifier)
                        {
                            startNotifier.wait();
                        }
                    }
                    catch (InterruptedException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    continue;
                }
                thingsNeedingDone.run(i);
            }
        }

    }
    
    public void doThings(IIndexedRunnable thingsToDo, int startIndex, int endIndex)
    {
        synchronized(runLock)
        {
            this.completionCount.set(0);
            this.endIndex = endIndex;
            this.thingsNeedingDone = thingsToDo;
            this.nextIndex.set(startIndex);
            
            synchronized(this.startNotifier)
            {
                this.startNotifier.notifyAll();
            }
            
            while(completionCount.get() != completedFlags)
            {
                try
                {
                    synchronized(this.completionNotifier)
                    {
                        this.completionNotifier.wait();
                    }
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
            
            // don't hold references
            this.thingsNeedingDone = null;
        }
    }
}
