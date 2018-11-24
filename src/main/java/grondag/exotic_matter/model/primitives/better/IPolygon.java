package grondag.exotic_matter.model.primitives.better;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList.Builder;

import grondag.acuity.api.IPipelinedQuad;
import grondag.acuity.api.IRenderPipeline;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.QuadHelper;
import grondag.exotic_matter.model.primitives.vertex.IVec3f;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

public interface IPolygon extends IVertexCollection, IPipelinedQuad
{
    public Vec3f getFaceNormal();
    
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
     * CONVERT TO IMMUTABLE IF SAVING A REFERENCE.
     */
    public IVec3f getVertexNormal(int vertexIndex);
    
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
    
    /**
     * Splits and creates new instances as needed.
     * Does NOT mutate this instance and does not retain any reference to the instances added to the list.
     * If you know the current instance is not held elsewhere, then better to
     * check if split is necessary and if not then simply add this instance to the list.
     * If a split is necessary and this instance is no longer needed, release it after calling.
     */
    void addPaintableQuadsToList(List<IMutablePolygon> list);
    
    /**
     * Splits and creates new instances as needed.
     * Does NOT mutate this instance if this instance is mutable.
     * If this instance is immutable and does not require split, will 
     * simply add this instance to the list. 
     */
    void addPaintedQuadsToList(List<IPolygon> list);
    
    /**
     * Splits and creates new instances as needed.
     * Does NOT mutate this instance and does not retain any reference to the instances added to the list.
     * If you know the current instance is not held elsewhere, then better to
     * check if split is necessary and if not then simply add this instance to the list.
     * If a split is necessary and this instance is no longer needed, release it after calling.
     */
    void producePaintableQuads(Consumer<IMutablePolygon> consumer);
    
    /**
     * Splits and creates new instances as needed.
     * Does NOT mutate this instance if this instance is mutable.
     * If this instance is immutable and does not require split, will 
     * simply add this instance to the list. 
     */
    void producePaintedQuads(Consumer<IPolygon> consumer);
    
    /**
     * Transfers mutable copies of this poly's vertices to the provided array.
     * Array size must be >= {@link #vertexCount()}.
     * Retains no reference to the copies, which should be released when no longer used.
     * For use in CSG operations.
     */
    public void claimVertexCopiesToArray(IMutableVertex[] vertex);
    
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
     * Will return quad color if vertex color not set.
     */
    int getVertexGlow(int layerIndex, int vertexIndex);
    
    float getVertexU(int layerIndex, int vertexIndex);
    
    float getVertexV(int layerIndex, int vertexIndex);
    
    int getTextureSalt(int layerIndex);

    boolean isLockUV(int layerIndex);

    public boolean hasRenderLayer(BlockRenderLayer layer);
    
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
    
    public void addBakedQuadsToBuilder(int layerIndex, Builder<BakedQuad> builder, boolean isItem);
//    {
//        produceQuads(q -> builder.add(QuadBakery.createBakedQuad(layerIndex, (IPaintedPoly) q, isItem)));
//    }

    IPolygon recoloredCopy();
    //final Random r = ThreadLocalRandom.current();
    //(r.nextInt(0x1000000) & 0xFFFFFF) | 0xFF000000

    boolean isEmissive(int textureLayerIndex);
    
    @Override
    IRenderPipeline getPipeline();
    
    /**
     * Same vertex count. Includes vertex data.
     */
    default IMutablePolygon claimCopy()
    {
        return claimCopy(this.vertexCount());
    }
    
    /**
     * Includes vertex data.
     */
    IMutablePolygon claimCopy(int vertexCount);
    
}
