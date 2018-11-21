package grondag.exotic_matter.model.primitives.better;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.QuadHelper;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

public interface IPoly extends IVertexCollection
{
    public Vec3f getFaceNormal();
    
    public EnumFacing getNominalFace();
    
    //TODO: put these in helpers or something, don't belong here
//    public default void addQuadsToList(List<IPoly<T>> list, boolean ensureConvex)
//    {
//        this.toQuads(p -> list.add(p), ensureConvex);
//    }
//    
//    public void toQuads(Consumer<IPoly<T>> target, boolean ensureConvex);
//    
//    public void toTris(Consumer<IPoly<T>> target);
//    
//    public default void addTrisToList(List<IPoly<T>> list)
//    {
//        this.toTris(p -> list.add(p));
//    }
//    
//    public void addTrisToCSGRoot(CSGNode.Root root);

    public default AxisAlignedBB getAABB()
    {
        Vec3f p0 = getPos(0);
        Vec3f p1 = getPos(1);
        Vec3f p2 = getPos(2);
        Vec3f p3 = getPos(3);
        
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

        Vec3f fn = this.getFaceNormal();

        float faceX = fn.x();
        float faceY = fn.y();
        float faceZ = fn.z();

        Vec3f first = this.getPos(0);
        
        for(int i = 3; i < this.vertexCount(); i++)
        {
            Vec3f v = this.getPos(i);
            
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
            final Vec3f v0 = getPos(0);
            final Vec3f v1 = getPos(1);
            final Vec3f v2 = getPos(2);
            final Vec3f v3 = getPos(3);
            
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
    
    public Surface getSurfaceInstance();
    
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
     * If this instance is mutable, it MAY be added to the consumer and a reference retained.
     * Do NOT retain references to mutable instances after using this method.
     */
    public <P extends IPoly> void produceQuads(Consumer<P> target);
    
    /**
     * Splits and creates new instances as needed.
     * If this instance is mutable, it MAY be added to the consumer and a reference retained.
     * Do NOT retain references to mutable instances after using this method.
     */
    @SuppressWarnings("unchecked")
    public default <P extends IPoly> void addQuadsToList(List<P> list)
    {
        produceQuads(q -> list.add((P) q));
    }
}
