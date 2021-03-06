package grondag.exotic_matter.model.primitives.polygon;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList.Builder;

import grondag.acuity.api.IPipelinedQuad;
import grondag.acuity.api.IPipelinedVertexConsumer;
import grondag.acuity.api.IRenderPipeline;
import grondag.exotic_matter.ClientProxy;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.PolyFactory;
import grondag.exotic_matter.model.primitives.QuadHelper;
import grondag.exotic_matter.model.primitives.vertex.IVec3f;
import grondag.exotic_matter.model.primitives.vertex.IVertexCollection;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.model.render.QuadBakery;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public interface IPolygon extends IVertexCollection, IPipelinedQuad, IStreamPolygon
{
    public Vec3f getFaceNormal();
    
    public default float getFaceNormalX()
    {
        return getFaceNormal().x();
    }

    public default float getFaceNormalY()
    {
        return getFaceNormal().y();
    }
    
    public default float getFaceNormalZ()
    {
        return getFaceNormal().z();
    }
    
    public EnumFacing getNominalFace();

    public default AxisAlignedBB getAABB()
    {
        IVec3f p0 = getPos(0);
        IVec3f p1 = getPos(1);
        IVec3f p2 = getPos(2);
        IVec3f p3 = getPos(3);
        
        double minX = Math.min(Math.min(p0.x(), p1.x()), Math.min(p2.x(), p3.x()));
        double minY = Math.min(Math.min(p0.y(), p1.y()), Math.min(p2.y(), p3.y()));
        double minZ = Math.min(Math.min(p0.z(), p1.z()), Math.min(p2.z(), p3.z()));

        double maxX = Math.max(Math.max(p0.x(), p1.x()), Math.max(p2.x(), p3.x()));
        double maxY = Math.max(Math.max(p0.y(), p1.y()), Math.max(p2.y(), p3.y()));
        double maxZ = Math.max(Math.max(p0.z(), p1.z()), Math.max(p2.z(), p3.z()));

        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static final int VERTEX_NOT_FOUND = -1;
    
    /**
     * Will return {@link #VERTEX_NOT_FOUND} (-1) if vertex is not found in this polygon.
     */
    public default int indexForVertex(Vec3f v)
    {
        final int limit = this.vertexCount();
        for(int i = 0; i < limit; i++)
        {
            if(v.equals(this.getPos(i)))
                return i;
        }
        return VERTEX_NOT_FOUND;
    }
    
    public default boolean isConvex()
    {
        return QuadHelper.isConvex(this);
    }

    public default boolean isOrthogonalTo(EnumFacing face)
    {
        Vec3i dv = face.getDirectionVec();
        float dot = this.getFaceNormal().dotProduct(dv.getX(), dv.getY(), dv.getZ());
        return Math.abs(dot) <= QuadHelper.EPSILON;
    }

    public default boolean isOnSinglePlane()
    {
        if(this.vertexCount() == 3) return true;

        IVec3f fn = this.getFaceNormal();

        float faceX = fn.x();
        float faceY = fn.y();
        float faceZ = fn.z();

        IVec3f first = this.getPos(0);
        
        for(int i = 3; i < this.vertexCount(); i++)
        {
            IVec3f v = this.getPos(i);
            
            float dx = v.x() - first.x();
            float dy = v.y() - first.y();
            float dz = v.z() - first.z();

            if(Math.abs(faceX * dx + faceY * dy + faceZ * dz) > QuadHelper.EPSILON) return false;
        }

        return true;
    }

    public default boolean isOnFace(@Nullable EnumFacing face, float tolerance)
    {
        if(face == null) return false;
        for(int i = 0; i < this.vertexCount(); i++)
        {
            if(!getPos(i).isOnFacePlane(face, tolerance))
                return false;
        }
        return true;
    }
    
    public default Vec3f computeFaceNormal()
    {
        try
        {
            final IVec3f v0 = getPos(0);
            final IVec3f v1 = getPos(1);
            final IVec3f v2 = getPos(2);
            final IVec3f v3 = getPos(3);
            
            final float x0 = v2.x() - v0.x();
            final float y0 = v2.y() - v0.y();
            final float z0 = v2.z() - v0.z();
            
            final float x1 = v3.x() - v1.x();
            final float y1 = v3.y() - v1.y();
            final float z1 = v3.z() - v1.z();
            
            final float x =  y0 * z1 - z0 * y1;
            final float y = z0 * x1 - x0 * z1;
            final float z = x0 * y1 - y0 * x1;
            
            float mag = MathHelper.sqrt(x * x + y * y + z * z);
            if(mag < 1.0E-4F)
                mag = 1f;
            
            return Vec3f.create(x / mag, y / mag, z / mag);
        }
        catch(Exception e)
        {
            assert false : "Bad polygon structure during face normal request.";
            return Vec3f.ZERO;
        }
    }

     // adapted from http://geomalgorithms.com/a01-_area.html
     // Copyright 2000 softSurfer, 2012 Dan Sunday
     // This code may be freely used and modified for any purpose
     // providing that this copyright notice is included with it.
     // iSurfer.org makes no warranty for this code, and cannot be held
     // liable for any real or imagined damage resulting from its use.
     // Users of this code must verify correctness for their application.
    public default float getArea()
    {
        float area = 0;
        float an, ax, ay, az; // abs value of normal and its coords
        int  coord;           // coord to ignore: 1=x, 2=y, 3=z
        int  i, j, k;         // loop indices
        final int n = this.vertexCount();
        Vec3f N = this.getFaceNormal();
        
        if (n < 3) return 0;  // a degenerate polygon

        // select largest abs coordinate to ignore for projection
        ax = (N.x()>0 ? N.x() : -N.x());    // abs x-coord
        ay = (N.y()>0 ? N.y() : -N.y());    // abs y-coord
        az = (N.z()>0 ? N.z() : -N.z());    // abs z-coord

        coord = 3;                    // ignore z-coord
        if (ax > ay) 
        {
            if (ax > az) coord = 1;   // ignore x-coord
        }
        else if (ay > az) coord = 2;  // ignore y-coord

        // compute area of the 2D projection
        switch (coord)
        {
          case 1:
            for (i=1, j=2, k=0; i<n; i++, j++, k++)
                area += (getPosModulo(i)).y() * (getPosModulo(j).z() - getPosModulo(k).z());
            break;
          case 2:
            for (i=1, j=2, k=0; i<n; i++, j++, k++)
                area += (getPosModulo(i).z() * (getPosModulo(j).x() - getPosModulo(k).x()));
            break;
          case 3:
            for (i=1, j=2, k=0; i<n; i++, j++, k++)
                area += (getPosModulo(i).x() * (getPosModulo(j).y() - getPosModulo(k).y()));
            break;
        }
        
        switch (coord)
        {    // wrap-around term
          case 1:
            area += (getPosModulo(n).y() * (getPosModulo(1).z() - getPosModulo(n-1).z()));
            break;
          case 2:
            area += (getPosModulo(n).z() * (getPosModulo(1).x() - getPosModulo(n-1).x()));
            break;
          case 3:
            area += (getPosModulo(n).x() * (getPosModulo(1).y() - getPosModulo(n-1).y()));
            break;
        }

        // scale to get area before projection
        an = MathHelper.sqrt( ax*ax + ay*ay + az*az); // length of normal vector
        switch (coord)
        {
          case 1:
            area *= (an / (2 * N.x()));
            break;
          case 2:
            area *= (an / (2 * N.y()));
            break;
          case 3:
            area *= (an / (2 * N.z()));
        }
        return area;
    }
    
    public Surface getSurface();
    
    /**
     * Returns computed face normal if no explicit normal assigned.
     */
    public Vec3f getVertexNormal(int vertexIndex);
    
    public boolean hasVertexNormal(int vertexIndex);
    
    public float getVertexNormalX(int vertexIndex);
    
    public float getVertexNormalY(int vertexIndex);
    
    public float getVertexNormalZ(int vertexIndex);
    
    /** 
     * Face to use for shading testing.
     * Based on which way face points. 
     * Never null
     */
    public default EnumFacing getNormalFace()
    {
        return QuadHelper.computeFaceForNormal(this.getFaceNormal());
    }
    
    /** 
     * Face to use for occlusion testing.
     * Null if not fully on one of the faces.
     * Fudges a bit because painted quads can be slightly offset from the plane.
     */
    public default @Nullable EnumFacing getActualFace()
    {
        EnumFacing nominalFace = this.getNominalFace();
        
        // semantic face will be right most of the time
        if(this.isOnFace(nominalFace, QuadHelper.EPSILON)) return nominalFace;

        for(int i = 0; i < 6; i++)
        {
            final EnumFacing f = EnumFacing.VALUES[i];
            if(f != nominalFace && this.isOnFace(f, QuadHelper.EPSILON)) return f;
        }
        return null;
    }
    
    float getMaxU(int layerIndex);

    float getMaxV(int layerIndex);

    float getMinU(int layerIndex);

    float getMinV(int layerIndex);

    int layerCount();
    
    String getTextureName(int layerIndex);

    boolean shouldContractUVs(int layerIndex);

    Rotation getRotation(int layerIndex);
    
    float getVertexX(int vertexIndex);
    
    float getVertexY(int vertexIndex);
    
    float getVertexZ(int vertexIndex);
    
    /** 
     * Will return quad color if vertex color not set.
     */
    int getVertexColor(int layerIndex, int vertexIndex);
    
    /** 
     * Will return zero if vertex color not set.
     */
    int getVertexGlow(int vertexIndex);
    
    float getVertexU(int layerIndex, int vertexIndex);
    
    float getVertexV(int layerIndex, int vertexIndex);
    
    int getTextureSalt();

    boolean isLockUV(int layerIndex);

    public default boolean hasRenderLayer(BlockRenderLayer layer)
    {
        if(getRenderLayer(0) == layer)
            return true;
        
        final int count = this.layerCount();
        return (count > 1 && getRenderLayer(1) == layer)
               || (count == 3 && getRenderLayer(2) == layer);
    }
    
    /**
     * This is Acuity-only.  Acuity assumes quad has only a single render layer.
     */
    @Override
    public default BlockRenderLayer getRenderLayer()
    {
        return getRenderLayer(0);
    }
    
    BlockRenderLayer getRenderLayer(int layerIndex);
    
    /**
     * Adds all quads that belong in the given layer.
     * If layer is null, outputs all quads.
     */
    public default void addBakedQuadsToBuilder(@Nullable BlockRenderLayer layer, Builder<BakedQuad> builder, boolean isItem)
    {
        final int limit = this.layerCount();
        if(limit == 1)
        {
            if(layer == null || this.getRenderLayer(0) == layer)
                addBakedQuadsToBuilder(0, builder, isItem);
        }
        else
        {
            for(int i = 0; i < limit; i++)
            {
                if(layer == null || this.getRenderLayer(i) == layer)
                    addBakedQuadsToBuilder(i, builder, isItem);
            }
        }
    }
    
    public default void addBakedQuadsToBuilder(int layerIndex, Builder<BakedQuad> builder, boolean isItem) {
        assert vertexCount() <= 4;
        builder.add(QuadBakery.createBakedQuad(this, isItem));
    }
    
    boolean isEmissive(int layerIndex);
    
    int getPipelineIndex();
    
    @Override
    default IRenderPipeline getPipeline()
    {
        return ClientProxy.acuityPipeline(getPipelineIndex());
    }
    
    //TODO: retire in favor of streams
    /**
     * Same vertex count. Includes vertex data.
     */
    @Deprecated
    default IMutablePolygon claimCopy()
    {
        return factory().claimCopy(this);
    }
    
    //TODO: retire in favor of streams
    /**
     * Copies non-vertex attributes.  Will include vertex data only if vertex counts match.
     */
    @Deprecated
    public default IMutablePolygon claimCopy(int vertexCount)
    {
        return factory().claimCopy(this, vertexCount);
    }
    
    @Override
    @SideOnly(value = Side.CLIENT)
    public default void produceVertices(@SuppressWarnings("null") IPipelinedVertexConsumer vertexLighter)
    {
        float[][][] uvData = AcuityHelper.getUVData(this);
        int lastGlow = 0;
        final int layerCount = layerCount();
        
        vertexLighter.setEmissive(0, isEmissive(0));
        if(layerCount > 1)
        {
            vertexLighter.setEmissive(1, isEmissive(1));
            if(layerCount == 3)
                vertexLighter.setEmissive(2, isEmissive(2));
        }
        
        for(int i = 0; i < 4; i++)
        {
            // passing layer 0 glow as an extra data point (for lava)
            int currentGlow = this.getVertexGlow(i);
            if(currentGlow != lastGlow)
            {
                final int g = currentGlow * 17;
                
                vertexLighter.setBlockLightMap(g, g, g, 255);
                lastGlow = currentGlow;
            }
            
            switch(layerCount)
            {
            case 1:
                vertexLighter.acceptVertex(
                        getVertexX(i), getVertexY(i), getVertexZ(i), 
                        getVertexNormalX(i), getVertexNormalY(i), getVertexNormalZ(i),
                        getVertexColor(0, i), uvData[0][i][0], uvData[0][i][1]);
                break;
                
            case 2:
                vertexLighter.acceptVertex(
                        getVertexX(i), getVertexY(i), getVertexZ(i), 
                        getVertexNormalX(i), getVertexNormalY(i), getVertexNormalZ(i),
                        getVertexColor(0, i), uvData[0][i][0], uvData[0][i][1],
                        getVertexColor(1, i), uvData[1][i][0], uvData[1][i][1]);
                break;
            
            case 3:
                vertexLighter.acceptVertex(
                        getVertexX(i), getVertexY(i), getVertexZ(i), 
                        getVertexNormalX(i), getVertexNormalY(i), getVertexNormalZ(i),
                        getVertexColor(0, i), uvData[0][i][0], uvData[0][i][1],
                        getVertexColor(1, i), uvData[1][i][0], uvData[1][i][1],
                        getVertexColor(2, i), uvData[2][i][0], uvData[2][i][1]);
                break;
            
            default:
                throw new ArrayIndexOutOfBoundsException();
            }
        }
    }
    
    class AcuityHelper
    {
        /**
         * INTERNAL USE ONLY
         */
        private static final ThreadLocal<float[][][]> uvArray = new ThreadLocal<float[][][]>()
        {
            @Override
            protected float[][][] initialValue()
            {
                return new float[3][4][2];
            }
        };
        
        /**
         * WARNING: returned result is thread-local, do not let it escape.
         */
        static float[][][] getUVData(IPolygon poly)
        {
            final int layerCount = poly.layerCount();
            
            final float[][][] uvData = uvArray.get();
            
            for(int l = 0; l < layerCount; l++)
            {
                final TextureAtlasSprite textureSprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(poly.getTextureName(l));
                
                final float minU = poly.getMinU(l);
                final float minV = poly.getMinV(l);
                
                final float spanU = poly.getMaxU(l) - minU;
                final float spanV = poly.getMaxV(l) - minV;
                
                for(int v = 0; v < 4; v++)
                {
                    uvData[l][v][0] = poly.getVertexU(l, v);
                    uvData[l][v][1] = poly.getVertexV(l, v);
                }
                
                // apply texture rotation
                QuadBakery.applyTextureRotation(poly.getRotation(l), uvData[l]);
                
                // scale UV coordinates to size of texture sub-region
                for(int v = 0; v < 4; v++)
                {
                    uvData[l][v][0] = minU + spanU * uvData[l][v][0];
                    uvData[l][v][1] = minV + spanV * uvData[l][v][1];
                }
        
                if(poly.shouldContractUVs(l))
                {
                    QuadBakery.contractUVs(textureSprite, uvData[l]);
                }
                
                final float spriteMinU = textureSprite.getMinU();
                final float spriteSpanU = textureSprite.getMaxU() - spriteMinU;
                final float spriteMinV = textureSprite.getMinV();
                final float spriteSpanV = textureSprite.getMaxV() - spriteMinV;
                
                for(int v = 0; v < 4; v++)
                {
                    // doing interpolation here vs using sprite methods to avoid wasteful multiply and divide by 16
                    // PERF: can this be combined with loop above?
                    uvData[l][v][0] = spriteMinU + uvData[l][v][0] * spriteSpanU;
                    uvData[l][v][1] = spriteMinV + uvData[l][v][1] * spriteSpanV;
                }
            }
            return uvData;
        }
    }
    
    //TODO: retire in favor of streams
    /**
     * Allocation manager for this instance. May or may not be
     * a pooled allocation manager. If it is pooled, then can 
     * be used to allocate instances in the same pool
     * and (if supported) discover and inspect allocated objects
     * in the same pool.  Not generally intended to be used directly.
     */
    @Deprecated
    default IPrimitiveFactory factory()
    {
        return PolyFactory.COMMON_POOL;
    }
    
    //TODO: retire in favor of streams
    /**
     * Signals to allocation manager this instance is being referenced
     * by something other than the original requester and will prevent
     * the object from being recycled if the original allocator releases it.<p>
     * 
     * Note that retain count is always 1 when an object is first created,
     * so if the object is held by the originator there is no need to call this.
     */
    @Deprecated
    default void retain()
    {
        
    }
    
    /**
     * Should be called by when the original reference or another reference
     * created via {@link #retain()} is no longer held.  <p>
     * 
     * When retain count is 0 the object will be returned to its allocation
     * pool if it has one.
     */
    default void release()
    {
        
    }

    /**
     * Should be called instead of {@link #release()} when this is the last
     * held reference allocated by this objects factory. Will raise an assertion
     * error (if enabled) if this is not the last retained instance in the factory pool.<p>
     * 
     * For use as a debugging aid - has no functional necessity otherwise.
     * Also has no effect/meaning for unpooled instances.
     */
    default void releaseLast()
    {
        release();
    }
}
