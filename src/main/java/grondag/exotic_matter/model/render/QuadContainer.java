package grondag.exotic_matter.model.render;

import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.model.primitives.IGeometricVertexConsumer;
import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import grondag.exotic_matter.varia.SimpleUnorderedArrayList;
import grondag.exotic_matter.varia.Useful;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;

public class QuadContainer
{
    private static IPolygon[] EMPTY_LIST = {};
    private static int[] EMPTY_COUNTS = {0, 0, 0, 0, 0, 0};
    public static final QuadContainer EMPTY_CONTAINER = new QuadContainer(EMPTY_LIST, EMPTY_COUNTS) ;

    // Heavy usage, many instances, so using sublists of a single immutable list to improve LOR
    // I didn't profile this to make sure it's worthwhile - don't tell Knuth.
    // Only populated if baked quads are requested
    protected @Nullable ImmutableList<BakedQuad>[] faceLists = null;
    
    private @Nullable int[] occlusionHash = null;
    
    private int[] paintedFaceIndex = new int[EnumFacing.VALUES.length];
    
    private final IPolygon[] paintedQuads;
    
    protected QuadContainer(IPolygon[] paintedQuads, int[] paintedFaceIndex)
    {
        this.paintedQuads = paintedQuads;
        this.paintedFaceIndex = paintedFaceIndex;
    }
    
    @SuppressWarnings({ "unchecked", "null" })
    public List<BakedQuad> getBakedQuads(@Nullable EnumFacing face)
    {
        //  build locally and don't set until end in case another thread is racing with us
        ImmutableList<BakedQuad>[] faceLists = this.faceLists;
        
        if(faceLists == null)
        {
            faceLists = new ImmutableList[7];
            
            {
                final ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
                this.forEachPaintedQuad(null, q -> q.addBakedQuadsToBuilder(builder, false));
                faceLists[6] = builder.build();
            }
            
            for(EnumFacing f : EnumFacing.VALUES)
            {
                final ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
                this.forEachPaintedQuad(f, q -> q.addBakedQuadsToBuilder(builder, false));
                faceLists[f.ordinal()] = builder.build();
            }
            
            this.faceLists = faceLists;
        }
 
        return face == null ? faceLists[6] : faceLists[face.ordinal()];
    }
    
    public void forEachPaintedQuad(Consumer<IPolygon> consumer)
    {
        for(IPolygon q : this.paintedQuads)
            consumer.accept(q);
    }
    
    public void forEachPaintedQuad(@Nullable EnumFacing face, Consumer<IPolygon> consumer)
    {
        int start, end;
        if(face == null)
        {
            start = 0;
            end = this.paintedFaceIndex[0];
        }
        else
        {
            final int n = face.ordinal();
            start = this.paintedFaceIndex[n];
            end = n == 5 ? this.paintedQuads.length : this.paintedFaceIndex[n + 1];
        }
        while(start < end)
        {
            consumer.accept(this.paintedQuads[start++]);
        }
    }
    
    public int getOcclusionHash(@Nullable EnumFacing face)
    {
        if(face == null) return 0;

        int[] occlusionHash = this.occlusionHash;
        
        if(occlusionHash == null)
        {
            occlusionHash = new int[6];
            for(int i = 0; i < 6; i++)
            {
                final EnumFacing f = EnumFacing.VALUES[i];
                occlusionHash[f.ordinal()] = computeOcclusionHash(f);
                this.occlusionHash = occlusionHash;
            }
        }

        return occlusionHash[face.ordinal()];
    }

    private int computeOcclusionHash(EnumFacing face)
    {
        QuadListKeyBuilder keyBuilder = new QuadListKeyBuilder(face);
        this.forEachPaintedQuad(face, q -> q.produceGeometricVertices(keyBuilder));
        return keyBuilder.getQuadListKey();
    }
    
    public static class Builder implements Consumer<IPolygon>
    {
        int size = 0;
        
        @SuppressWarnings("unchecked")
        final SimpleUnorderedArrayList<IPolygon>[] buckets = new SimpleUnorderedArrayList[7];
        
        @Override
        public void accept(@SuppressWarnings("null") IPolygon quad)
        {
            final @Nullable EnumFacing facing = quad.getActualFace();
            final int index = facing == null ? 6 : facing.ordinal();
            
            SimpleUnorderedArrayList<IPolygon> bucket = buckets[index];
            if(bucket  == null)
            {
                bucket = new SimpleUnorderedArrayList<IPolygon>();
                buckets[index] = bucket;
            }
            bucket.add(quad);
            size++;
        }
            
        public QuadContainer build()
        {
            if(this.size == 0)
                return EMPTY_CONTAINER;
            
            IPolygon[] quads = new IPolygon[this.size];
            int[] indexes = new int[6];
            
            int i = addAndGetSize(quads, 0, buckets[6]);
            
            for(int j = 0; j < 6; j++)
            {
                indexes[j] = i;
                i += addAndGetSize(quads, i, buckets[j]);
            }
            
            return new QuadContainer(quads, indexes);
        }

        private final int addAndGetSize(IPolygon[] targetArray, int firstOpenIndex, @Nullable SimpleUnorderedArrayList<IPolygon> sourceList)
        {
            if(sourceList == null) return 0;
            sourceList.copyToArray(targetArray, firstOpenIndex);
            return sourceList.size();
        }
    }


    private static class QuadListKeyBuilder implements IGeometricVertexConsumer
    {
        private final int axis0;
        private final int axis1;

        private TreeSet<Long> vertexKeys = new TreeSet<Long>();

        private QuadListKeyBuilder(EnumFacing face)
        {
            switch(face.getAxis())
            {
            case X:
                axis0 = 1;
                axis1 = 2;
                break;
            case Y:
                axis0 = 0;
                axis1 = 2;
                break;
            case Z:
            default:
                axis0 = 0;
                axis1 = 1;
                break;
            }
        }

        /** call after piping vertices into this instance */
        private int getQuadListKey()
        {
            long key = 0L;
            for(Long vk : vertexKeys)
            {
                key += Useful.longHash(vk); 
            }
            return (int)(key & 0xFFFFFFFF);     
        }

        @Override
        public void acceptVertex(float x, float y, float z)
        {
            float v0 = 0, v1 = 0;
            switch(axis0)
            {
                case 0:
                    v0 = x;
                    break;
                case 1:
                    v0 = y;
                    break;
                case 2:
                    v0 = z;
                    break;
            }
            
            switch(axis1)
            {
                case 0:
                    v1 = x;
                    break;
                case 1:
                    v1 = y;
                    break;
                case 2:
                    v1 = z;
                    break;
            }
            //don't need to check which element - position is the only one included
            vertexKeys.add(((long)(Float.floatToRawIntBits(v0)) | ((long)(Float.floatToRawIntBits(v1)) << 32)));
        }
    }
}
