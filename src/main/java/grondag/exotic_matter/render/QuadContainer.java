package grondag.exotic_matter.render;

import static net.minecraft.util.EnumFacing.DOWN;
import static net.minecraft.util.EnumFacing.EAST;
import static net.minecraft.util.EnumFacing.NORTH;
import static net.minecraft.util.EnumFacing.SOUTH;
import static net.minecraft.util.EnumFacing.UP;
import static net.minecraft.util.EnumFacing.WEST;

import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.varia.SimpleUnorderedArrayList;
import grondag.exotic_matter.varia.Useful;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import net.minecraftforge.client.model.pipeline.LightUtil;

public class QuadContainer
{
    public static final QuadContainer EMPTY_CONTAINER = new QuadContainer(Collections.emptyList());

    // Heavy usage, many instances, so using sublists of a single immutable list to improve LOR
    // and using instance variables to avoid memory overhead of another array. 
    // I didn't profile this to make sure it's worthwhile - don't tell Knuth.
     
    protected final List<BakedQuad> general;
    
    private @Nullable int[] occlusionHash;
    
    protected QuadContainer(List<BakedQuad> general)
    {
        this.general = general;
    }
    
    public List<BakedQuad> getQuads(@Nullable EnumFacing face)
    {
        return face == null ? this.general: QuadHelper.EMPTY_QUAD_LIST;
    }
    
    public int getOcclusionHash(@Nullable EnumFacing face)
    {
        if(face == null) return 0;

        int[] occlusionHash = this.occlusionHash;
        
        if(occlusionHash == null)
        {
            occlusionHash = new int[EnumFacing.values().length];
            for(EnumFacing f : EnumFacing.values())
            {
                occlusionHash[f.ordinal()] = computeOcclusionHash(f);
                this.occlusionHash = occlusionHash;
            }
        }

        return occlusionHash[face.ordinal()];
    }

    private int computeOcclusionHash(EnumFacing face)
    {
        List<BakedQuad> quads = getQuads(face);
        QuadListKeyBuilder keyBuilder = new QuadListKeyBuilder(face);
        for(BakedQuad q : quads)
        {
            LightUtil.putBakedQuad(keyBuilder, q);
        }
        return keyBuilder.getQuadListKey();
    }
    
    protected static class Extended extends QuadContainer
    {
        private final List<BakedQuad> up;
        private final List<BakedQuad> down;
        private final List<BakedQuad> east;
        private final List<BakedQuad> west;
        private final List<BakedQuad> north;
        private final List<BakedQuad> south;
        
        protected Extended(
                List<BakedQuad> general,
                List<BakedQuad> up,
                List<BakedQuad> down,
                List<BakedQuad> east,
                List<BakedQuad> west,
                List<BakedQuad> north,
                List<BakedQuad> south)
        {
            super(general);
            this.up = up;
            this.down = down;
            this.east = east;
            this.west = west;
            this.north = north;
            this.south = south;
        }
        
        @Override
        public final List<BakedQuad> getQuads(@Nullable EnumFacing face)
        {
            if(face ==null) return this.general;

            switch(face)
            {
            case DOWN:
                return this.down;
            case EAST:
                return this.east;
            case NORTH:
                return this.north;
            case SOUTH:
                return this.south;
            case UP:
                return this.up;
            case WEST:
                return this.west;
            default:
                return QuadHelper.EMPTY_QUAD_LIST;

            }
        }
    }
    
    public static class Builder implements Consumer<IPolygon>
    {

        @SuppressWarnings("unchecked")
        final SimpleUnorderedArrayList<BakedQuad>[] buckets = new SimpleUnorderedArrayList[7];
        
        @Override
        public void accept(@SuppressWarnings("null") IPolygon poly)
        {
           
            EnumFacing facing = poly.getActualFace();
            final int index = facing == null ? 6 : facing.ordinal();
            
            SimpleUnorderedArrayList<BakedQuad> bucket = buckets[index];
            if(bucket  == null)
            {
                bucket = new SimpleUnorderedArrayList<BakedQuad>();
                buckets[index] = bucket;
            }
            bucket.add(QuadBakery.createBakedQuad(poly, false));
        }
            
        public QuadContainer build()
        {
            ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
    
            final int upCount = addAndGetSize(builder, buckets[UP.ordinal()]);
            final int downCount = addAndGetSize(builder, buckets[DOWN.ordinal()]);
            final int eastCount = addAndGetSize(builder, buckets[EAST.ordinal()]);
            final int westCount = addAndGetSize(builder, buckets[WEST.ordinal()]);
            final int northCount = addAndGetSize(builder, buckets[NORTH.ordinal()]);
            final int southCount = addAndGetSize(builder, buckets[SOUTH.ordinal()]);
            final int genCount = addAndGetSize(builder, buckets[6]);
            
            ImmutableList<BakedQuad> quads = builder.build();
    
            if(upCount == 0 && downCount == 0 && eastCount == 0 && westCount == 0 && northCount == 0 && southCount == 0)
            {
                return genCount  == 0 
                        ? EMPTY_CONTAINER
                        : new QuadContainer(quads);
            }
            
            int first = 0;
            
            final List<BakedQuad> up = upCount == 0 ? ImmutableList.of() : quads.subList(first, first + upCount);
            first +=  upCount;
    
            final List<BakedQuad> down = downCount == 0 ? ImmutableList.of() : quads.subList(first, first + downCount);
            first +=  downCount;
            
            final List<BakedQuad> east = eastCount == 0 ? ImmutableList.of() : quads.subList(first, first + eastCount);
            first +=  eastCount;
            
            final List<BakedQuad> west = westCount == 0 ? ImmutableList.of() : quads.subList(first, first + westCount);
            first +=  westCount;
            
            final List<BakedQuad> north = northCount == 0 ? ImmutableList.of() : quads.subList(first, first + northCount);
            first +=  northCount;
            
            final List<BakedQuad> south = southCount == 0 ? ImmutableList.of() : quads.subList(first, first + southCount);
            first +=  southCount;
            
            final List<BakedQuad> gen = genCount == 0 ? ImmutableList.of() : quads.subList(first, first + genCount);
            
            return new QuadContainer.Extended(gen, up, down, east, west, north, south);
        
        }

        private final int addAndGetSize(ImmutableList.Builder<BakedQuad> builder, @Nullable SimpleUnorderedArrayList<BakedQuad> list)
        {
            if(list == null) return 0;
            builder.addAll(list);
            return list.size();
        }
    }


    private static class QuadListKeyBuilder implements IVertexConsumer
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
        public @Nonnull VertexFormat getVertexFormat()
        {
            return DefaultVertexFormats.POSITION;
        }

        @Override
        public void setQuadTint(int tint)
        {
            //NOOP - not used
        }

        @Override
        public void setQuadOrientation(@Nonnull EnumFacing orientation)
        {
            //NOOP - not used
        }

        @Override
        public void setApplyDiffuseLighting(boolean diffuse)
        {
            //NOOP - not used
        }

        @Override
        public void put(int element, @Nonnull float... data)
        {
            //don't need to check which element - position is the only one included
            vertexKeys.add(((long)(Float.floatToRawIntBits(data[axis0])) | ((long)(Float.floatToRawIntBits(data[axis1])) << 32)));
        }

        @Override
        public void setTexture(@Nonnull TextureAtlasSprite texture)
        {
            //NOOP - not used
        }
    }


}
