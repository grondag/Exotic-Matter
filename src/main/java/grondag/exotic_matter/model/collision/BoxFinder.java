package grondag.exotic_matter.model.collision;

import static grondag.exotic_matter.model.collision.BoxHelper.EMPTY;

import java.util.function.IntConsumer;

import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.model.collision.BoxHelper.Slice;
import grondag.exotic_matter.model.collision.CollisionBoxList.Builder;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

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
    
    final int[] maximalVolumes = new int[64];
    
    int volumeCount = 0;
    
    final long[] intersects = new long[64];
    
    final LongOpenHashSet disjointSets = new LongOpenHashSet();
    
    final int[] volumeScores = new int[64];
    
    public void clear()
    {
        System.arraycopy(EMPTY, 0, voxels, 0, 8);
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
        
    }
    
    void findDisjointSets()
    {
        disjointSets.clear();
        
        final int limit = this.volumeCount;
        
        for(int i = 0; i < limit; i++)
        {
            tryDisjoint(i, 0L, 0L);
        }
    }
    
    /**
     * If maximal volume at index does not intersect with any
     * existing member, then is added to the set and recursively
     * attempt to add more members.
     * 
     * If the volume cannot be added to the set because it 
     * intersects, then this is a leaf node - adds the volume
     * as a disjoint set and returns false.
     * 
     */
    private boolean tryDisjoint(int index, long members, long combinedIntersects)
    {
        final long joins = intersects[index];
        final long mId = (1L << index);
        if((mId & (combinedIntersects | members)) == 0)
        {
            combinedIntersects |= joins;
            members |= mId;
            final int limit = this.volumeCount;
            boolean isLeaf = true;
            for(int i = 0; i  < limit; i++)
            {
                if(i != index && tryDisjoint(i, members, combinedIntersects))
                    isLeaf = false;
            }
            if(isLeaf)
                disjointSets.add(members);
            return true;
        }
        else return false;
    }
    
    
    private void outputDisjointSet(long disjointSet, Builder builder)
    {
        BoxHelper.forEachBit(disjointSet, i -> 
        {
            int k = maximalVolumes[i];
            addBox(k, builder);
        });
    }
    
    void explainDisjointSet(long disjointSet)
    {
        ExoticMatter.INSTANCE.info("Disjoint Set Info: Box Count = %d, Score = %d", Long.bitCount(disjointSet), scoreOfDisjointSet(disjointSet));
        BoxHelper.forEachBit(disjointSet, i -> 
        {
            int k = maximalVolumes[i];
            Slice slice = BoxHelper.sliceFromKey(k);
            int areaIndex = BoxHelper.patternIndexFromKey(k);
            ExoticMatter.INSTANCE.info("Box w/ %d volume @ %d, %d,%d to %d, %d, %d", BoxHelper.volumeFromKey(k), BoxHelper.MIN_X[areaIndex], BoxHelper.MIN_Y[areaIndex], slice.min, BoxHelper.MAX_X[areaIndex], BoxHelper.MAX_Y[areaIndex], slice.max);
        });
        ExoticMatter.INSTANCE.info("");
    }
    
    private boolean outputBest(VoxelOctTree voxels, Builder builder)
    {
        calcCombined();
        
        populateMaximalVolumes();
        if(this.volumeCount <= 1)
        {
            if(this.volumeCount == 1)
                addBox(maximalVolumes[0], builder);
            return false;
        }
        
        populateIntersects();
        
        findDisjointSets();
        
        
//        ExoticMatter.INSTANCE.info("AVAILABLE DISJOINT SETS");
        
        final LongIterator it = disjointSets.iterator();
        long bestSet = it.nextLong();
        
        if(disjointSets.size() == 1)
        {
            outputDisjointSet(bestSet, builder);
            return false;
        }
        
        scoreMaximalVolumes();
        int bestScore = scoreOfDisjointSet(bestSet);
        
        while(it.hasNext())
        {
            long set = it.nextLong();
            int score = scoreOfDisjointSet(set);
            if(score < bestScore)
            {
                bestScore = score;
                bestSet = set;
            }
        }
        
//        ExoticMatter.INSTANCE.info("CHOSEN DISJOINT SET");
//        explainDisjointSet(disjointSets.getLong(bestIndex));
        
        outputDisjointSet(bestSet, builder);
        
        return true;
    }
    
    
    class SetScoreAccumulator implements IntConsumer
    {
        int total;

        @Override
        public void accept(int value)
        { 
            total += volumeScores[value];
        }
    
        void prepare()
        {
            total = 0;
        }
    }
    
    private final SetScoreAccumulator setScoreCounter = new SetScoreAccumulator();
    
    private int scoreOfDisjointSet(long set)
    {
        final SetScoreAccumulator counter = this.setScoreCounter;
        counter.prepare();
        BoxHelper.forEachBit(set, setScoreCounter);
        return counter.total;
    }

    private void addBox(int volumeKey, Builder builder)
    {
        Slice slice = BoxHelper.sliceFromKey(volumeKey);
        int areaIndex = BoxHelper.patternIndexFromKey(volumeKey);
        
        setEmpty(BoxHelper.MIN_X[areaIndex], BoxHelper.MIN_Y[areaIndex], slice.min, BoxHelper.MAX_X[areaIndex], BoxHelper.MAX_Y[areaIndex], slice.max);
        builder.add(BoxHelper.MIN_X[areaIndex], BoxHelper.MIN_Y[areaIndex], slice.min, BoxHelper.MAX_X[areaIndex] + 1, BoxHelper.MAX_Y[areaIndex] + 1, slice.max + 1);
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
        final int[] volumes = this.maximalVolumes;
        final int limit = this.volumeCount;
        if(limit == 0)
            return true;
        
        if(limit == 1)
            return !BoxHelper.isVolumeIncluded(volumes[0], volKey);
        
        for(int i = 0; i < limit; i++)
        {
            if(BoxHelper.isVolumeIncluded(volumes[i], volKey))
                return false;
        }
        
        return true;
    }
    
    void populateMaximalVolumes()
    {
        this.volumeCount = 0;
        final int[] volumes = this.maximalVolumes;
        
        // TODO: use an exclusion test voxel hash to reduce search universe 
        for(int v : BoxHelper.VOLUME_KEYS)
        {
            if(isVolumePresent(v) && isVolumeMaximal(v))
            {
                final int c = this.volumeCount++;
                volumes[c] = v;
                if(c == 63)
                    break;
            }
        }
    }
    
    void populateIntersects()
    {
        final long[] intersects = this.intersects;
        final int[] volumes = this.maximalVolumes;
        final int limit = this.volumeCount;

        System.arraycopy(EMPTY, 0, intersects, 0, 64);
        
        if(limit < 2)
            return;
        
        for(int i = 1; i < limit; i++)
        {
            final int a = volumes[i];
            
            for(int j = 0; j < i; j++)
            {
                if(BoxHelper.doVolumesIntersect(a, volumes[j]))
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
    
    class VolumeScoreAccumulator implements IntConsumer
    {
        int actorVolume;
        int total;

        @Override
        public void accept(int value)
        { 
            total += BoxHelper.splitScore(actorVolume, maximalVolumes[value]);
        }
    
        void prepare(int actorVolume)
        {
            this.actorVolume = actorVolume;
            total = 0;
        }
    }
    
    private final VolumeScoreAccumulator volumeScorecounter = new VolumeScoreAccumulator();
    
    void scoreMaximalVolumes()
    {
        final int[] scores = this.volumeScores;
        final long[] intersects = this.intersects;
        final int limit = this.volumeCount;
        final VolumeScoreAccumulator counter = this.volumeScorecounter;
        
        for(int i = 0; i < limit; i++)
        {
            counter.prepare(maximalVolumes[i]);
            BoxHelper.forEachBit(intersects[i], counter);
            scores[i] = counter.total;
        }
        
    }
}
