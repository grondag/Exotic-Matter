package grondag.exotic_matter.varia.intstream;

import java.util.concurrent.ArrayBlockingQueue;

import grondag.exotic_matter.ExoticMatter;
import net.minecraft.util.math.MathHelper;


public abstract class IntStreams
{
//    static final int SMALL_BLOCK_SIZE = 256;
//    static final int BLOCK_SIZE_FACTOR = 4;
    
    static final int LARGE_BLOCK_SIZE = 1024;
    static final int LARGE_BLOCK_MASK = LARGE_BLOCK_SIZE - 1;
    static final int LARGE_BLOCK_SHIFT = Integer.bitCount(LARGE_BLOCK_MASK);
    
    
    private static final ArrayBlockingQueue<SimpleStream> simpleStreams = new ArrayBlockingQueue<>(256);
    
    private static final ArrayBlockingQueue<int[]> bigBlocks = new ArrayBlockingQueue<>(256);
    
    private static final int[] EMPTY = new int[LARGE_BLOCK_SIZE];
    
    private static int[] claimBig()
    {
        int[] result = bigBlocks.poll();
        if(result == null)
            return new int[LARGE_BLOCK_SIZE];
        else
        {
            System.arraycopy(EMPTY, 0, result, 0, LARGE_BLOCK_SIZE);
            return result;
        }
    }
    
    private static void releaseBig(int[] block)
    {
        //TODO: remove message
        if(!bigBlocks.offer(block))
            ExoticMatter.INSTANCE.info("Big block buffer was full on block release");
    }
    
    public static IIntStream claim(int sizeHint)
    {
        SimpleStream result = simpleStreams.poll();
        if(result == null)
            result = new SimpleStream();
        result.prepare(sizeHint);
        return result;
    }
    
    public static IIntStream claim()
    {
        return claim(LARGE_BLOCK_SIZE);
    }
    
    private static void release(SimpleStream freeStream)
    {
        freeStream.releaseBlocks();
        simpleStreams.offer(freeStream);
    }
    
    /**
     * Uses large blocks only - may be space-inefficient.
     */
    private static class SimpleStream  implements IIntStream
    {
        int[][] blocks = new int[64][];
        
        int capacity = 0;
        
        private void checkAddress(int address)
        {
            if(address >= capacity)
            {
                int current = capacity >> LARGE_BLOCK_SHIFT;
                int needed =  (address >> LARGE_BLOCK_SHIFT) + 1;
                
                if(needed > blocks.length)
                {
                    int newMax = MathHelper.smallestEncompassingPowerOfTwo(needed);
                    int[][] newBlocks = new int[newMax][];
                    System.arraycopy(blocks, 0, newBlocks, 0, blocks.length);
                    blocks = newBlocks;
                }
                
                for(int i = current; i < needed; i++)
                    blocks[i] = claimBig();
                
                capacity = needed << LARGE_BLOCK_SHIFT;
            }
        }
        
        @Override
        public int get(int address)
        {
            return address < capacity
                    ? blocks[address >> LARGE_BLOCK_SHIFT][address & LARGE_BLOCK_MASK]
                    : 0;
        }

        public void prepare(int sizeHint)
        {
            checkAddress(sizeHint - 1);
        }

        public void releaseBlocks()
        {
            int current = capacity >> LARGE_BLOCK_SHIFT;
            if(current > 0)
                for(int i = 0; i < current; i++)
                {
                    releaseBig(blocks[i]);
                    blocks[i] = null;
                }
            capacity = 0;
        }

        @Override
        public void set(int address, int value)
        {
            checkAddress(address);
            blocks[address >> LARGE_BLOCK_SHIFT][address & LARGE_BLOCK_MASK] = value;
        }

        @Override
        public void clear()
        {
            int current = capacity >> LARGE_BLOCK_SHIFT;
            if(current > 0)
                for(int i = 0; i < current; i++)
                    System.arraycopy(EMPTY, 0, blocks[i], 0, LARGE_BLOCK_SIZE);
            
        }

        @Override
        public void release()
        {
            IntStreams.release(this);
        }

        @Override
        public void copyFrom(int targetAddress, IIntStream source, int sourceAddress, int length)
        {
            // PERF: special case handling using ArrayCopy for faster transfer
            IIntStream.super.copyFrom(targetAddress, source, sourceAddress, length);
        }
        
        
    }
}
