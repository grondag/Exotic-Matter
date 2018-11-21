package grondag.exotic_matter.model.primitives.polygon;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import grondag.exotic_matter.model.CSG.CSGNode;
import grondag.exotic_matter.model.primitives.PointInPolygonTest;
import grondag.exotic_matter.model.primitives.QuadHelper;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.model.primitives.vertex.Vertex;
import grondag.exotic_matter.model.render.QuadBakery;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

/**
 * Immutable base interface for classes used to create and transform meshes before baking into MC quads.<p>
 */

public interface IPolygon extends IPaintablePolygon
{
    /**
     * If this polygon is mutable, returns mutable reference to self.
     * Thows an exception if not mutable.
     */
    public default IMutablePolygon mutableReference()
    {
        throw new UnsupportedOperationException();
    }
    
    public Vertex getVertex(int index);
    
    public void forEachVertex(Consumer<Vertex> consumer);
    
    /**
     * Like {@link #toQuads(boolean)} but doesn't instantiate a new list.
     */
    public default void addQuadsToList(List<IPolygon> list, boolean ensureConvex)
    {
        this.toQuads(p -> list.add(p), ensureConvex);
    }
    
    /**
     * Like {@link #toQuads(boolean)} but doesn't instantiate a new list.
     */
    public void toQuads(Consumer<IPolygon> target, boolean ensureConvex);
    
    /**
     * Like {@link #toTris()} but doesn't instantiate a new list.
     */
    public void toTris(Consumer<IPolygon> target);
    
    /**
     * Like {@link #toTris()} but doesn't instantiate a new list.
     */
    public default void addTrisToList(List<IPolygon> list)
    {
        this.toTris(p -> list.add(p));
    }
    
    public void addTrisToCSGRoot(CSGNode.Root root);
    
      /**
      * Provide direct access to vertex array for CSG only.
      * Performance optimization.
      */
     public Vertex[] vertexArray();
    
    public default AxisAlignedBB getAABB()
    {
        double minX = Math.min(Math.min(getVertex(0).x(), getVertex(1).x()), Math.min(getVertex(2).x(), getVertex(3).x()));
        double minY = Math.min(Math.min(getVertex(0).y(), getVertex(1).y()), Math.min(getVertex(2).y(), getVertex(3).y()));
        double minZ = Math.min(Math.min(getVertex(0).z(), getVertex(1).z()), Math.min(getVertex(2).z(), getVertex(3).z()));

        double maxX = Math.max(Math.max(getVertex(0).x(), getVertex(1).x()), Math.max(getVertex(2).x(), getVertex(3).x()));
        double maxY = Math.max(Math.max(getVertex(0).y(), getVertex(1).y()), Math.max(getVertex(2).y(), getVertex(3).y()));
        double maxZ = Math.max(Math.max(getVertex(0).z(), getVertex(1).z()), Math.max(getVertex(2).z(), getVertex(3).z()));

        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public default boolean isConvex()
    {
        return QuadHelper.isConvex(this.vertexArray(), this.vertexCount());
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

        Vec3f fn = this.getFaceNormal();

        float faceX = fn.x();
        float faceY = fn.y();
        float faceZ = fn.z();

        Vertex first = this.getVertex(0);
        
        for(int i = 3; i < this.vertexCount(); i++)
        {
            Vertex v = this.getVertex(i);
            
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
            if(!getVertex(i).isOnFacePlane(face, tolerance))
                return false;
        }
        return true;
    }
    
    public default Vec3f computeFaceNormal()
    {
        try
        {
            final Vec3f v0 = getVertex(0).pos();
            final Vec3f v1 = getVertex(1).pos();
            final Vec3f v2 = getVertex(2).pos();
            final Vec3f v3 = getVertex(3).pos();
            
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
        Vertex[] V = Arrays.copyOf(this.vertexArray(), n + 1);
        V[n] = V[0];
        
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
                area += (V[i].y() * (V[j].z() - V[k].z()));
            break;
          case 2:
            for (i=1, j=2, k=0; i<n; i++, j++, k++)
                area += (V[i].z() * (V[j].x() - V[k].x()));
            break;
          case 3:
            for (i=1, j=2, k=0; i<n; i++, j++, k++)
                area += (V[i].x() * (V[j].y() - V[k].y()));
            break;
        }
        
        switch (coord)
        {    // wrap-around term
          case 1:
            area += (V[n].y() * (V[1].z() - V[n-1].z()));
            break;
          case 2:
            area += (V[n].z() * (V[1].x() - V[n-1].x()));
            break;
          case 3:
            area += (V[n].x() * (V[1].y() - V[n-1].y()));
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

    public default void addBakedQuadsToBuilder(Builder<BakedQuad> builder, boolean isItem)
    {
        builder.add(QuadBakery.createBakedQuad(this, isItem));
    }

    IMutablePolygon mutableCopyWithVertices();

    IMutablePolygon mutableCopy();

    IMutablePolygon mutableCopy(int vertexCount);
    
//    /**
//     * For testing
//     */
//    public String getTag();
//    
//    public IMutablePolygon setTag(String tag);
}
