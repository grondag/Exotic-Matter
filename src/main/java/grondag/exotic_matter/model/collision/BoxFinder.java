package grondag.exotic_matter.model.collision;

import static grondag.exotic_matter.model.collision.BoxFinderUtils.EMPTY;

import java.util.function.IntConsumer;

import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.model.collision.BoxFinderUtils.Slice;
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
    
    void clear()
    {
        System.arraycopy(EMPTY, 0, voxels, 0, 8);
    }
    
    /**
     * Saves voxel data to given array.   Array must be >= 8 in length.
     * Load with {@link #restoreFrom(long[])}.
     */
    public void saveTo(long[] data)
    {
        System.arraycopy(voxels, 0, data, 0, 8);
    }
    
    /**
     * Loads data from an earlier call to {@link #saveTo(long[])}
     */
    public void restoreFrom(long[] data)
    {
        System.arraycopy(data, 0, voxels, 0, 8);
    }
    
    
    /**
     * Coordinates must be 0 - 8
     */
    public void setFilled(int x, int y, int z)
    {
        voxels[z] |= (1L << BoxFinderUtils.areaBitIndex(x, y));
    }
    
    /**
     * Coordinates must be 0 - 8
     */
    public boolean isFilled(int x, int y, int z)
    {
        return (voxels[z] & (1L << BoxFinderUtils.areaBitIndex(x, y))) != 0;
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
        voxels[z] &= ~(1L << BoxFinderUtils.areaBitIndex(x, y));
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
    

    /**
     * Finds the maximal volume that encloses the largest number of maximal volumes
     * for the smallest number of voxel inaccuracies and then fills that volume.
     */
    boolean simplify()
    {
        calcCombined();
        populateMaximalVolumes();
        
        int bestVolume = -1;
        int bestScore = 0;
        
        for(int v : BoxFinderUtils.VOLUME_KEYS)
        {
            int n = simplifcationScore(v);
            
            // for our purpose, split boxes contained in volume don't count
            if(n > 0) 
                n--;
            
            if(n > bestScore)
            {
                bestScore = n;
                bestVolume = v;
            }
        }
        
        if(bestVolume == -1)
            return false;
        
        this.fillVolume(bestVolume);
        return true;
    }
    
    int simplifcationScore(int volumeKey)
    {
        
        final int[] volumes = this.maximalVolumes;
        final int limit = this.volumeCount;
        
        // To score higher than zero, must fully enclose at least two maximal volumes.
        // If this volume would cause other maximal volume to split into two or more volumes
        // that are not fully enclosed in this volume, then those count against the score.
        int boxScore = -1;
        
        for(int i = 0; i < limit; i++)
        {
            int v = volumes[i];
            if(BoxFinderUtils.isVolumeIncluded(volumeKey, v))
                boxScore++;
            
            boxScore -= BoxFinderUtils.splitScore(volumeKey, v);
        }
        
        if(boxScore == 0)
            return 0;
        
        final int filled = countFilledVoxels(volumeKey);
        final int vol = BoxFinderUtils.volumeFromKey(volumeKey);
        
        // must fill in some voxels or isn't a simplification
        if(filled == vol)
            return 0;
        
        // Preference is always given to lower error rates.
        // Because we have chosen high scores to mean "better" we
        // represent this inversely, as total possible voxels
        // less the number of error voxels. 
        int voxelScore = 512 - vol + filled;
        
        return (voxelScore << 16) | boxScore;
    }
    
    public void outputBoxes(ICollisionBoxListBuilder builder)
    {
        while(outputBest(builder)) {};
        outputRemainers(builder);
    }
    
    void outputRemainers(ICollisionBoxListBuilder builder)
    {
        outputRemainerInner(0, builder);
        outputRemainerInner(1, builder);
        outputRemainerInner(2, builder);
        outputRemainerInner(3, builder);
        outputRemainerInner(4, builder);
        outputRemainerInner(5, builder);
        outputRemainerInner(6, builder);
        outputRemainerInner(7, builder);
    }
    
    void outputRemainerInner(int z, ICollisionBoxListBuilder builder)
    {
        final long bits = voxels[z];
        
        if(bits == 0L)
            return;
        
        BoxFinderUtils.forEachBit(bits, i -> 
        {
            final int x = BoxFinderUtils.xFromAreaBitIndex(i);
            final int y = BoxFinderUtils.yFromAreaBitIndex(i);
            builder.addSorted(x, y, z, x + 1, y + 1, z + 1);
        });
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
    
    
    private void outputDisjointSet(long disjointSet, ICollisionBoxListBuilder builder)
    {
        BoxFinderUtils.forEachBit(disjointSet, i -> 
        {
            int k = maximalVolumes[i];
            addBox(k, builder);
        });
    }
    
    void explainDisjointSets()
    {
        for(Long l : disjointSets)
        {
            explainDisjointSet(l);
        }   
    }
    
    void explainDisjointSet(long disjointSet)
    {
        ExoticMatter.INSTANCE.info("Disjoint Set Info: Box Count = %d, Score = %d", Long.bitCount(disjointSet), scoreOfDisjointSet(disjointSet));
        BoxFinderUtils.forEachBit(disjointSet, i -> 
        {
            explainVolume(i);
        });
        ExoticMatter.INSTANCE.info("");
    }
    
    void explainMaximalVolumes()
    {
        ExoticMatter.INSTANCE.info("Maximal Volumes");
        for(int i = 0;  i < volumeCount; i++)
        {
            explainVolume(i);
        }
        ExoticMatter.INSTANCE.info("");
    }
    
    void explainVolume(int volIndex)
    {
        final int volKey = maximalVolumes[volIndex];
        StringBuilder b = new StringBuilder();
        BoxFinderUtils.forEachBit(intersects[volIndex], i ->
        {
            if(b.length() != 0)
                b.append(", ");
            b.append(i);
        });
        final Slice slice = BoxFinderUtils.sliceFromKey(volKey);
        BoxFinderUtils.testAreaBounds(BoxFinderUtils.patternFromKey(volKey), (minX, minY, maxX, maxY) ->
        {
            ExoticMatter.INSTANCE.info("Box w/ %d volume @ %d, %d,%d to %d, %d, %d  score = %d  intersects = %s  volKey = %d", BoxFinderUtils.volumeFromKey(volKey), 
                    minX, minY, slice.min, maxX, maxY, slice.max, volumeScores[volIndex], b.toString(), volKey);
            return 0;
        });
    }
    
    private boolean outputBest(ICollisionBoxListBuilder builder)
    {
        calcCombined();
        populateMaximalVolumes();
        return outputBestInner(builder);
    }
    
    private boolean outputBestInner(ICollisionBoxListBuilder builder)
    {
        if(this.volumeCount <= 1)
        {
            if(this.volumeCount == 1)
                addBox(maximalVolumes[0], builder);
            return false;
        }
        
        populateIntersects();
        
        findDisjointSets();
        
       
        
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
        BoxFinderUtils.forEachBit(set, setScoreCounter);
        return counter.total;
    }

    void addBox(int volumeKey, ICollisionBoxListBuilder builder)
    {
        Slice slice = BoxFinderUtils.sliceFromKey(volumeKey);
        BoxFinderUtils.testAreaBounds(BoxFinderUtils.patternFromKey(volumeKey), (minX, minY, maxX, maxY) ->
        {
            setEmpty(minX, minY, slice.min, maxX, maxY, slice.max);
            builder.add(minX, minY, slice.min, maxX + 1, maxY + 1, slice.max + 1);
            return 0;
        });
    }
    
    void loadVoxels(VoxelOctree voxels)
    {
        clear();
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
    
    /**
     * True if volume cannot be expanded along any axis.
     */
    boolean isVolumeMaximal(int volKey)
    {
        final Slice slice = BoxFinderUtils.sliceFromKey(volKey);
        final long pattern = BoxFinderUtils.patternFromKey(volKey);
        
        return BoxFinderUtils.testAreaBounds(pattern, (minX, minY, maxX, maxY) ->
        {
            if(slice.min > 0 && isVolumePresent(pattern, BoxFinderUtils.sliceByMinMax(slice.min - 1, slice.max)))
                return 1;
            
            if(slice.max < 7 && isVolumePresent(pattern, BoxFinderUtils.sliceByMinMax(slice.min, slice.max + 1)))
                return 1;
            
            if(minX > 0 && isVolumePresent((pattern >>> 1) | pattern, slice))
                return 1;
            
            if(maxX < 7 && isVolumePresent((pattern << 1) | pattern, slice))
                return 1;
            
            if(minY > 0 && isVolumePresent((pattern >>> 8) | pattern, slice))
                return 1;
            
            if(maxY < 7 && isVolumePresent((pattern << 8) | pattern, slice))
                return 1;
            
            return 0;
        }) == 0;
    }
    
    @FunctionalInterface
    interface IPresenceTest
    {
        boolean apply(int volumeKey);
    }
    
    void populateMaximalVolumes()
    {
        this.volumeCount = 0;
        final int[] volumes = this.maximalVolumes;
        
        // disabled for release, not strictly needed but prevents confusion during debug
//        Arrays.fill(volumes, 0, 64, 0);
        
        // PERF: use an exclusion test voxel hash to reduce search universe 
        for(int v : BoxFinderUtils.VOLUME_KEYS)
        {
            if(isVolumePresent(v) && isVolumeMaximal(v))
            {
                final int c = this.volumeCount++;
                volumes[c] = v;
                if(c == 63)
                    break;
            }
        }
        
//        for(int i = 0; i < volumeCount; i++)
//        {
//            for(int j = 0; j < volumeCount; j++)
//            {
//                if(i != j && BoxFinderUtils.isVolumeIncluded(volumes[i], volumes[j]))
//                {
//                    ExoticMatter.INSTANCE.info("REDUNDANT MAXIMAL VOLUMES");
//                    explainVolume(i);
//                    explainVolume(j);
//                }
//                    
//            }
//        }
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
                if(BoxFinderUtils.doVolumesIntersect(a, volumes[j]))
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
        return isVolumePresent(BoxFinderUtils.patternFromKey(volKey), BoxFinderUtils.sliceFromKey(volKey));
    }
    
    boolean isVolumePresent(long pattern, Slice slice)
    {
        return (pattern & combined[slice.ordinal()]) == pattern;
    }
    
    void fillVolume(int volKey)
    {
        final long pattern = BoxFinderUtils.patternFromKey(volKey);
        final Slice slice = BoxFinderUtils.sliceFromKey(volKey);
        
        for(int i = slice.min; i <= slice.max; i++)
        {
            voxels[i] |= pattern;
        }
    }
    
    int countFilledVoxels(int volKey)
    {
        final long pattern = BoxFinderUtils.patternFromKey(volKey);
        final Slice slice = BoxFinderUtils.sliceFromKey(volKey);
        
        int result = 0;
        
        for(int i = slice.min; i <= slice.max; i++)
        {
            result += Long.bitCount(voxels[i] & pattern);
        }
        
        return result;
    }
    
    class VolumeScoreAccumulator implements IntConsumer
    {
        int actorVolume;
        int total;

        @Override
        public void accept(int value)
        { 
            total += BoxFinderUtils.splitScore(actorVolume, maximalVolumes[value]);
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
            BoxFinderUtils.forEachBit(intersects[i], counter);
            scores[i] = counter.total;
        }
        
    }
}
