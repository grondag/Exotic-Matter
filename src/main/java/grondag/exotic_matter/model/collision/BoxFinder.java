package grondag.exotic_matter.model.collision;

import java.util.BitSet;

import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.model.collision.BoxHelper.Slice;
import grondag.exotic_matter.model.collision.CollisionBoxList.Builder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import static grondag.exotic_matter.model.collision.BoxHelper.*;

/**
 * Quickly identifies largest filled AABBs within an 8x8x8 voxel volume
 * and outputs those boxes plus any unclaimed voxels. <p>
 * 
 * Exploits representation of each 8x8x1 layer as a single long value to
 * enable fast bit-wise comparisons. <p>
 */
public class BoxFinder
{
    private long[] voxels = new long[8];
    
    private long[] combined = new long[Slice.values().length];
    
    private int searchIndex = 0;
    
    private final IntArrayList searchList = new IntArrayList();
    
    final IntArrayList maximalVolumes = new IntArrayList();
    
    final long[] intersects = new long[64];
    
    public void clear()
    {
        System.arraycopy(EMPTY, 0, voxels, 0, 8);
        searchList.clear();
        searchIndex = 0;
    }
    
    /**
     * Coordinates must be 0 - 8
     */
    public void setFilled(int x, int y, int z)
    {
        voxels[z] |= (1L << BoxHelper.bitIndex(x, y));
    }
    
    public void setFilled(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
    {
        for(int x = minX; x <= maxX; x++)
        {
            for(int y = minY; y <= maxY; y++)
            {
                for(int z = minZ; z <= maxZ; z++)
                {
                    setFilled(x, y, z);
                }
            }            
        }
    }
    
    public void setEmpty(int x, int y, int z)
    {
        voxels[z] &= ~(1L << BoxHelper.bitIndex(x, y));
    }
    
    public void setEmpty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
    {
        for(int x = minX; x <= maxX; x++)
        {
            for(int y = minY; y <= maxY; y++)
            {
                for(int z = minZ; z <= maxZ; z++)
                {
                    setEmpty(x, y, z);
                }
            }            
        }
    }
    
    public void outputBoxes(VoxelOctTree voxels, CollisionBoxList.Builder builder)
    {
        loadVoxels(voxels);
        
//        ExoticMatter.INSTANCE.info("STARTING BOX FINDINATOR");
        
        while(outputBest(voxels, builder)) {};
        
        outputRemnants(builder);
        
    }
    
    private boolean outputLargest(VoxelOctTree voxels, Builder builder)
    {
        calcCombined();
        
        Slice bestSlice = null;
        int bestVolume = 0;
        int bestIndex = -1;
        
        for(Slice slice : Slice.values())
        {
            for(int i = 0; i < BoxHelper.AREAS.length; i++)
            {
                long pattern = BoxHelper.AREAS[i];
                
                if((pattern & combined[slice.ordinal()]) == pattern)
                {
                    int v = slice.depth * Long.bitCount(pattern);
                    if(v > bestVolume)
                    {
                        bestVolume = v;
                        bestSlice = slice;
                        bestIndex = i;
                    }
                }
            }
        }
        
        if(bestSlice == null) 
            return false;
        
        setEmpty(BoxHelper.MIN_X[bestIndex], BoxHelper.MIN_Y[bestIndex], bestSlice.min, BoxHelper.MAX_X[bestIndex], BoxHelper.MAX_Y[bestIndex], bestSlice.max);
        builder.add(BoxHelper.MIN_X[bestIndex], BoxHelper.MIN_Y[bestIndex], bestSlice.min, BoxHelper.MAX_X[bestIndex] + 1, BoxHelper.MAX_Y[bestIndex] + 1, bestSlice.max + 1);
        
        return true;
    }

    final LongArrayList disjointSets = new LongArrayList();
    
    final IntArrayList stack = new IntArrayList();
    
    void findDisjointSets()
    {
        disjointSets.clear();
        
        final int limit = searchList.size() - 1;
        
        for(int i = 0; i < limit; i++)
        {
            tryDisjoint(i);
        }
    }
    
    /**
     * If stack is empty adds to stack and defines a new set.
     * If stack is not empty and is disjoint with all in stack
     * then then defines a new set with everything already
     * in the stack.
     * Lastly, pushes on to stack and attempts to create new sets 
     * recursively, popping index off stack before returning.
     * 
     * Returns true if was successful in adding boxes.  
     * If all subattempts are false this is a leaf node.
     */
    private boolean tryDisjoint(int index)
    {
        if(isDisjointWithStack(index))
        {
            stack.push(index);
            final int limit = searchList.size();
            boolean isLeaf = true;
            if(index < limit - 2)
            {
                
                for(int i = index + 1; i < limit; i++)
                {
                    if(tryDisjoint(i))
                        isLeaf = false;
                }
            }
            if(isLeaf)
                addSetFromStack();
            stack.popInt();
            return true;
        }
        else return false;
    }
    
    private boolean isDisjointWithStack(int index)
    {
        if(stack.isEmpty())
            return true;
        
        final IntArrayList stack = this.stack;
        final IntArrayList searchList = this.searchList;
        final int limit = stack.size();
        final int targetKey = searchList.getInt(index);
        
        for(int i = 0; i < limit; i++)
        {
            if(BoxHelper.doVolumesIntersect(targetKey, searchList.getInt(stack.getInt(i))))
                return false;
        }
        return true;
    }
    
    private void addSetFromStack()
    {
        final IntArrayList stack = this.stack;
        final int limit = stack.size();
        assert limit > 0;
        
        long set = 1L << stack.getInt(0);
        
        if(limit > 1)
        {
            for(int i = 0; i < limit; i++)
            {
                set |= (1L << stack.getInt(i));
            }
        }
        disjointSets.add(set);
    }
    
    /**
     * Iterates in reverse order to allow remove of search results without
     * affecting remaining indexes;  Integer value is index in searchList.
     */
    private void forEachBoxInDisjointSet(long disjointSet, BoxHelper.IBoxConsumer consumer)
    {
        long l = Long.highestOneBit(disjointSet);
        
        do
        {
            if((l & disjointSet) != 0L)
                consumer.accept(63 - Long.numberOfLeadingZeros(l));
            
            l = l >> 1;
        } while( l != 0);
    }
    
    private class VolumeAccumulator implements BoxHelper.IBoxConsumer
    {
        int total;
        int count;

        @Override
        public void accept(int value)
        {
            int score = BoxHelper.volumeFromKey(searchList.getInt(value));
            total += score;
            count++;
        }
        
        int score()
        {
            return total; //Math.round((float)total / (count * count));
        }
    }
    
    private int scoreOfDisjointSet(long disjointSet)
    {
        VolumeAccumulator result = new VolumeAccumulator();
        
        forEachBoxInDisjointSet(disjointSet, result);
        
        return result.score();
    }
    
    private final IntArrayList removalList = new IntArrayList();
    
    private void outputDisjointSet(long disjointSet, Builder builder)
    {
        final IntArrayList removalList = this.removalList;
        
        forEachBoxInDisjointSet(disjointSet, i -> 
        {
            int k = searchList.removeInt(i);
            addBox(k, builder);
            removalList.add(k);
        });
        
        IntIterator ita = searchList.iterator();
        
        while(ita.hasNext())
        {
            final int a = ita.nextInt();
            
            IntIterator itb = removalList.iterator();
            while(itb.hasNext())
            {
                if(BoxHelper.doVolumesIntersect(a, itb.nextInt()))
                {
                    ita.remove();
                    break;
                }
            }
        }
        
        removalList.clear();
    }
    
    void explainDisjointSet(long disjointSet)
    {
        ExoticMatter.INSTANCE.info("Disjoint Set Info: Box Count = %d", Long.bitCount(disjointSet));
        forEachBoxInDisjointSet(disjointSet, i -> 
        {
            int k = searchList.getInt(i);
            Slice slice = BoxHelper.sliceFromKey(k);
            int areaIndex = BoxHelper.patternIndexFromKey(k);
            ExoticMatter.INSTANCE.info("Box w/ %d volume @ %d, %d,%d to %d, %d, %d", BoxHelper.volumeFromKey(k), BoxHelper.MIN_X[areaIndex], BoxHelper.MIN_Y[areaIndex], slice.min, BoxHelper.MAX_X[areaIndex], BoxHelper.MAX_Y[areaIndex], slice.max);
        });
        ExoticMatter.INSTANCE.info("");
    }
    
    private boolean outputBest(VoxelOctTree voxels, Builder builder)
    {
        calcCombined();
        
        populateSearchSet();
        
        final IntArrayList searchList = this.searchList;
        
        if(searchList.size() < 4)
        {
            while(!searchList.isEmpty())
            {
                addBox(searchList.getInt(0), builder);
                removeFromSearch(0);
            }
            return false;
        }
        
        findDisjointSets();
        
        int bestScore = 0;
        int bestIndex = -1;
        
//        ExoticMatter.INSTANCE.info("AVAILABLE DISJOINT SETS");
        
        final LongArrayList disjointSets = this.disjointSets;
        final int limit = disjointSets.size();
        for(int i = 0; i < limit; i++)
        {
//            explainDisjointSet(disjointSets.getLong(i));
            final int v = scoreOfDisjointSet(disjointSets.getLong(i));
            if(v > bestScore)
            {
                bestIndex = i;
                bestScore = v;
            }
        }
        
//        ExoticMatter.INSTANCE.info("CHOSEN DISJOINT SET");
//        explainDisjointSet(disjointSets.getLong(bestIndex));
        
        outputDisjointSet(disjointSets.getLong(bestIndex), builder);
        
        return true;
    }
    
    private void addBox(int volumeKey, Builder builder)
    {
        Slice slice = BoxHelper.sliceFromKey(volumeKey);
        int areaIndex = BoxHelper.patternIndexFromKey(volumeKey);
        
        setEmpty(BoxHelper.MIN_X[areaIndex], BoxHelper.MIN_Y[areaIndex], slice.min, BoxHelper.MAX_X[areaIndex], BoxHelper.MAX_Y[areaIndex], slice.max);
        builder.add(BoxHelper.MIN_X[areaIndex], BoxHelper.MIN_Y[areaIndex], slice.min, BoxHelper.MAX_X[areaIndex] + 1, BoxHelper.MAX_Y[areaIndex] + 1, slice.max + 1);
    }

    
    private void removeFromSearch(int index)
    {
        int k = searchList.removeInt(index);
        IntIterator it = searchList.iterator();
        while(it.hasNext())
        {
            if(BoxHelper.doVolumesIntersect(it.nextInt(), k))
                it.remove();
        }
    }
    
    private int bestTwoVolume(int withIndex)
    {
        final int withKey = searchList.getInt(withIndex);
        final int limit = searchList.size();
        int best = 0;
        
        for(int i = 0; i < limit; i++)
        {
            if(i == withIndex) continue;
            final int k = searchList.getInt(i);
            if(BoxHelper.areVolumesDisjoint(withKey, k))
                best = Math.max(best, BoxHelper.volumeFromKey(k));
        }
        return best + BoxHelper.volumeFromKey(withKey);
    }
    
    private void loadVoxels(VoxelOctTree voxels)
    {
        voxels.forEachBottom(v -> 
        {
            if(v.isFull())
                setFilled(v.xMin8(), v.yMin8(), v.zMin8());
        });
    }
    
    void calcCombined()
    {
        combined[Slice.D1_0.ordinal()] = voxels[0];
        combined[Slice.D1_1.ordinal()] = voxels[1];
        combined[Slice.D1_2.ordinal()] = voxels[2];
        combined[Slice.D1_3.ordinal()] = voxels[3];
        combined[Slice.D1_4.ordinal()] = voxels[4];
        combined[Slice.D1_5.ordinal()] = voxels[5];
        combined[Slice.D1_6.ordinal()] = voxels[6];
        combined[Slice.D1_7.ordinal()] = voxels[7];
        
        combined[Slice.D2_0.ordinal()] = voxels[0] & voxels[1];
        combined[Slice.D2_1.ordinal()] = voxels[1] & voxels[2];
        combined[Slice.D2_2.ordinal()] = voxels[2] & voxels[3];
        combined[Slice.D2_3.ordinal()] = voxels[3] & voxels[4];
        combined[Slice.D2_4.ordinal()] = voxels[4] & voxels[5];
        combined[Slice.D2_5.ordinal()] = voxels[5] & voxels[6];
        combined[Slice.D2_6.ordinal()] = voxels[6] & voxels[7];
        
        combined[Slice.D3_0.ordinal()] = combined[Slice.D2_0.ordinal()] & voxels[2];
        combined[Slice.D3_1.ordinal()] = combined[Slice.D2_1.ordinal()] & voxels[3];
        combined[Slice.D3_2.ordinal()] = combined[Slice.D2_2.ordinal()] & voxels[4];
        combined[Slice.D3_3.ordinal()] = combined[Slice.D2_3.ordinal()] & voxels[5];
        combined[Slice.D3_4.ordinal()] = combined[Slice.D2_4.ordinal()] & voxels[6];
        combined[Slice.D3_5.ordinal()] = combined[Slice.D2_5.ordinal()] & voxels[7];
        
        combined[Slice.D4_0.ordinal()] = combined[Slice.D3_0.ordinal()] & voxels[3];
        combined[Slice.D4_1.ordinal()] = combined[Slice.D3_1.ordinal()] & voxels[4];
        combined[Slice.D4_2.ordinal()] = combined[Slice.D3_2.ordinal()] & voxels[5];
        combined[Slice.D4_3.ordinal()] = combined[Slice.D3_3.ordinal()] & voxels[6];
        combined[Slice.D4_4.ordinal()] = combined[Slice.D3_4.ordinal()] & voxels[7];
        
        combined[Slice.D5_0.ordinal()] = combined[Slice.D4_0.ordinal()] & voxels[4];
        combined[Slice.D5_1.ordinal()] = combined[Slice.D4_1.ordinal()] & voxels[5];
        combined[Slice.D5_2.ordinal()] = combined[Slice.D4_2.ordinal()] & voxels[6];
        combined[Slice.D5_3.ordinal()] = combined[Slice.D4_3.ordinal()] & voxels[7];

        combined[Slice.D6_0.ordinal()] = combined[Slice.D5_0.ordinal()] & voxels[5];
        combined[Slice.D6_1.ordinal()] = combined[Slice.D5_1.ordinal()] & voxels[6];
        combined[Slice.D6_2.ordinal()] = combined[Slice.D5_2.ordinal()] & voxels[7];

        combined[Slice.D7_0.ordinal()] = combined[Slice.D6_0.ordinal()] & voxels[6];
        combined[Slice.D7_1.ordinal()] = combined[Slice.D6_1.ordinal()] & voxels[7];

        combined[Slice.D8_0.ordinal()] = combined[Slice.D7_0.ordinal()] & voxels[7];
    }
    
    boolean isVolumeMaximal(int volKey)
    {
        IntArrayList list = this.maximalVolumes;
        final int limit = list.size();
        if(limit == 0)
            return true;
        
        if(limit == 1)
            return !BoxHelper.isVolumeIncluded(list.getInt(0), volKey);
        
        for(int i = 0; i < limit; i++)
        {
            if(BoxHelper.isVolumeIncluded(list.getInt(i), volKey))
                return false;
        }
        
        return true;
    }
    
    void populateMaximalVolumes()
    {
        final IntArrayList list = this.maximalVolumes;
        list.clear();
        
        // TODO: use an exclusion test voxel hash to reduce search universe 
        for(int v : BoxHelper.VOLUME_KEYS)
        {
            if(isVolumePresent(v) && isVolumeMaximal(v))
            {
                list.add(v);
                if(list.size() >= 64)
                    return;
            }
        }
    }
    
    void populateIntersects()
    {
        final long[] intersects = this.intersects;
        final IntArrayList list = this.maximalVolumes;
        final int limit = list.size();

        System.arraycopy(EMPTY, 0, intersects, 0, 64);
        
        if(limit < 2)
            return;
        
        for(int i = 1; i < limit; i++)
        {
            final int a = list.getInt(i);
            
            for(int j = 0; j < i; j++)
            {
                if(BoxHelper.doVolumesIntersect(a, list.getInt(j)))
                {
                    // could store only half of values, but 
                    // makes lookups and some tests easier later on
                    // to simply update both halves while we're here.
                    intersects[i] |= (1L << j);
                    intersects[j] |= (1L << i);
                }
            }
        }
    }
    
    boolean isVolumePresent(int volKey)
    {
        long pattern = BoxHelper.patternFromKey(volKey);
        Slice slice = BoxHelper.sliceFromKey(volKey);
        
        return (pattern & combined[slice.ordinal()]) == pattern;
    }
    
    void populateSearchSet()
    {
        IntArrayList list = this.searchList;
        int index = this.searchIndex;
        while(list.size() < 16 && index < BoxHelper.VOLUME_KEYS.length)
        {
            final int volKey = BoxHelper.VOLUME_KEYS[index++];
            if(isVolumePresent(volKey))
                list.add(volKey);
        }
        this.searchIndex = index;
    }
    
    private void outputRemnants(CollisionBoxList.Builder builder)
    {
        for(int z = 0; z < 8; z++ )
        {
            final long bits = voxels[z];
            if(bits == 0) continue;
            
            for(int i = 0; i < 64; i++)
            {
                if((bits & (1L << i)) != 0L)
                {
                    int x = i & 7;
                    int y = (i >> 3) & 7;
                    builder.addSorted(x, y, z, x + 1, y + 1, z + 1);
                }   
            }
        }
    }
}
