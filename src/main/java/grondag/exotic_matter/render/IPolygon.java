package grondag.exotic_matter.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.render.Surface.SurfaceInstance;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;

public interface IPolygon
{
    SurfaceInstance NO_SURFACE = new Surface(SurfaceType.MAIN, SurfaceTopology.CUBIC).unitInstance;

    public @Nullable String getTextureName();

    /**
     * Gets the face to be used for setupFace semantics.  
     * Is a general facing but does NOT mean poly is actually on that face.
     */
    public @Nullable EnumFacing getNominalFace();

    /** 
     * Causes texture to appear rotated within the frame
     * of this texture. Relies on UV coordinates
     * being in the range 0-16. <br><br>
     * 
     * Rotation happens during quad bake.
     * If lockUV is true, rotation happens after UV
     * coordinates are derived.
     */
    public Rotation getRotation();

    public int getColor();

    public boolean isFullBrightness();

    /** 
     * If true then quad painters will ignore UV coordinates and instead set
     * based on projection of vertices onto the given nominal face.
     * Note that FaceVertex does this by default even if lockUV is not specified.
     * To get unlockedUV coordiates, specificy a face using FaceVertex.UV or FaceVertex.UVColored.
     */
    public boolean isLockUV();
    
    /**
     * True if this instance implements ICSGPolygon. Generally faster than instanceof tests.
     */
//    public default boolean isCSG()
//    {
//        return false;
//    }
    
    /**
     * If this polygon has CSG metadata and supports the CSG interface, 
     * returns mutable reference to self as such.
     * Thows an exception otherwise.
     */
//    public default ICSGPolygon csgReference()
//    {
//        throw new UnsupportedOperationException();
//    }

    /**
     * If this polygon is mutable, returns mutable reference to self.
     * Thows an exception if not mutable.
     */
    public default IMutablePolygon mutableReference()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * True if face normal has been established, either implicitly via access
     * or by directly setting a value. 
     */
    public boolean hasFaceNormal();
    
    public Vec3f getFaceNormal();
    
    public default  float[] getFaceNormalArray()
    {
        Vec3f normal = getFaceNormal();

        float[] retval = new float[3];

        retval[0] = normal.x;
        retval[1] = normal.y;
        retval[2] = normal.z;
        return retval;
    }

    public boolean shouldContractUVs();

    public float getMinU();

    public float getMaxU();

    public float getMinV();

    public float getMaxV();

    public RenderPass getRenderPass();

    public SurfaceInstance getSurfaceInstance();

    public int vertexCount();
    
    public Vertex getVertex(int index);
    
    /**
     * Splits into quads if higher vertex count than four, otherwise returns self.
     * If ensureConvex is true, will also split quads that are concave into tris.
     */
    public default List<IPolygon> toQuads(boolean ensureConvex)
    {
        final int vertexCount = this.vertexCount();
        
        if(vertexCount == 3)  
            return ImmutableList.of(this);
        
        if(vertexCount == 4 && (!ensureConvex || this.isConvex())) 
            return ImmutableList.of(this);
        
        ArrayList<IPolygon> result = new ArrayList<>();
        this.addQuadsToList(result, ensureConvex);
        return result;
    }
    
    /**
     * Like {@link #toQuads(boolean)} but doesn't instantiate a new list.
     */
    public void addQuadsToList(List<IPolygon> list, boolean ensureConvex);
    
    /** 
     * If this is a quad or higher order polygon, returns new tris.
     * If is already a tri returns self.<p>
     */
    public default List<IPolygon> toTris()
    {
        if(this.vertexCount() == 3) return ImmutableList.of(this);
        ArrayList<IPolygon> result = new ArrayList<>();
        this.addTrisToList(result);
        return result;
    }
    
    /**
     * Like {@link #toTris()} but doesn't instantiate a new list.
     */
    public void addTrisToList(List<IPolygon> list);
    
    public void addTrisToCSGRoot(CSGNode.Root root);
      /**
      * Provide direct access to vertex array for CSG only.
      * Performance optimization.
      */
     public Vertex[] vertexArray();
    
    
    /** 
     * Returns intersection point of given ray with the plane of this quad.
     * Return null if parallel or facing away.<p/>
     * 
     * Direction provided MUST BE NORMALIZED.
     * 
     */
    public default Vec3f intersectionOfRayWithPlane(float originX, float originY, float originZ, float directionX, float directionY, float directionZ)
    {
        Vec3f normal = this.getFaceNormal();
        
        // TODO: remove when normal switched to float
        float normX = (float) normal.x;
        float normY = (float) normal.y;
        float normZ = (float) normal.z;

        float directionDotNormal = directionX * normX + directionY * normY + directionZ * normZ;
        if (Math.abs(directionDotNormal) < QuadHelper.EPSILON) 
        { 
            // parallel
            return null;
        }

        Vertex firstPoint = this.getVertex(0);
        
        float dx = originX - firstPoint.x;
        float dy = originY - firstPoint.y;
        float dz = originZ - firstPoint.z;
        
        float distanceToPlane = -(dx * normX + dy * normY + dz * normZ) / directionDotNormal;
        //double distanceToPlane = -normal.dotProduct((origin.subtract(firstPoint))) / directionDotNormal;
        // facing away from plane
        if(distanceToPlane < -QuadHelper.EPSILON) return null;

        return new Vec3f(originX + directionZ * distanceToPlane, originY + directionY * distanceToPlane, originZ + directionZ * distanceToPlane);
//        return origin.add(direction.scale(distanceToPlane));
    }
    
    
    /**
     * Keeping for convenience in case discover any problems with the fast version.
     * Unit tests indicate identical results.<p>
     * 
     * Provided direction MUST BE NORMALIZED for correct results
     */
    public default boolean intersectsWithRaySlow(float originX, float originY, float originZ, float directionX, float directionY, float directionZ)
    {
        Vec3f intersection = this.intersectionOfRayWithPlane(originX, originY, originZ, directionX, directionY, directionZ);
        
        // now we just need to test if point is inside this polygon
        return intersection == null ? false : containsPointSlow(intersection);
        
    }

    /**
     * Provided direction MUST BE NORMALIZED for correct results.
     */
    public default boolean intersectsWithRay(float originX, float originY, float originZ, float directionX, float directionY, float directionZ)
    {
        Vec3f intersection = this.intersectionOfRayWithPlane(originX, originY, originZ, directionX, directionY, directionZ);

        // now we just need to test if point is inside this polygon
        return intersection == null ? false : containsPoint(intersection);
    }

    /**
     * Assumes the given point is on the plane of the polygon.
     * 
     * For each side, find a vector in the plane of the 
     * polygon orthogonal to the line formed by the two vertices of the edge.
     * Then take the dot product with vector formed by the start vertex and the point.
     * If the point is inside the polygon, the sign should be the same for all
     * edges, or the dot product should be very small, meaning the point is on the edge.
     */
    public default boolean containsPoint(Vec3f point)
    {
        return PointInPolygonTest.isPointInPolygon(point, this);
    }
    

    /**
     * Keeping for convenience in case discover any problems with the fast version.
     * Unit tests indicate identical results.
     */
    public default boolean containsPointSlow(Vec3f point)
    {
        float lastSignum = 0;
        Vec3f faceNormal = this.getFaceNormal();

        for(int i = 0; i < this.vertexCount(); i++)
        {
            int nextVertex = i + 1;
            if(nextVertex == this.vertexCount()) nextVertex = 0;

            Vec3f currentVertex = getVertex(i);
            
            Vec3f line = getVertex(nextVertex).subtract(currentVertex);
            Vec3f normalInPlane = faceNormal.crossProduct(line);

            float sign = normalInPlane.dotProduct(point.subtract(currentVertex));

            if(lastSignum == 0)
            {
                lastSignum = Math.signum(sign);
            }
            else if(Math.signum(sign) != lastSignum)
            {
                return false;
            }
        }
        return true;
    }


    public default AxisAlignedBB getAABB()
    {
        double minX = Math.min(Math.min(getVertex(0).x, getVertex(1).x), Math.min(getVertex(2).x, getVertex(3).x));
        double minY = Math.min(Math.min(getVertex(0).y, getVertex(1).y), Math.min(getVertex(2).y, getVertex(3).y));
        double minZ = Math.min(Math.min(getVertex(0).z, getVertex(1).z), Math.min(getVertex(2).z, getVertex(3).z));

        double maxX = Math.max(Math.max(getVertex(0).x, getVertex(1).x), Math.max(getVertex(2).x, getVertex(3).x));
        double maxY = Math.max(Math.max(getVertex(0).y, getVertex(1).y), Math.max(getVertex(2).y, getVertex(3).y));
        double maxZ = Math.max(Math.max(getVertex(0).z, getVertex(1).z), Math.max(getVertex(2).z, getVertex(3).z));

        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    /**
     * Returns true if this polygon is convex.
     * All Tris must be.  
     * For quads, confirms that each turn around the quad 
     * goes same way by comparing cross products of edges.
     */
    public default boolean isConvex()
    {
        if(this.vertexCount() == 3) return true;

        float testX = 0;
        float testY = 0;
        float testZ = 0;
        boolean needTest = true;
                
        for(int thisIndex = 0; thisIndex < this.vertexCount(); thisIndex++)
        {
            int nextIndex = thisIndex + 1;
            if(nextIndex == this.vertexCount()) nextIndex = 0;

            int priorIndex = thisIndex - 1;
            if(priorIndex == -1) priorIndex = this.vertexCount() - 1;

            final Vertex thisVertex =  getVertex(thisIndex);
            final Vertex nextVertex = getVertex(nextIndex);
            final Vertex priorVertex = getVertex(priorIndex);
            
            final float ax = thisVertex.x - priorVertex.x;
            final float ay = thisVertex.y - priorVertex.y;
            final float az = thisVertex.z - priorVertex.z;
            
            final float bx = nextVertex.x - thisVertex.x;
            final float by = nextVertex.y - thisVertex.y;
            final float bz = nextVertex.z - thisVertex.z;

//            Vec3d lineA = getVertex(thisIndex).subtract(getVertex(priorIndex));
//            Vec3d lineB = getVertex(nextIndex).subtract(getVertex(thisIndex));
            
            final float crossX = ay * bz - az * by;
            final float crossY = az * bx - ax * bz;
            final float crossZ = ax * by - ay * bx;
            
            if(needTest)
            {
                needTest = false;
                testX = crossX;
                testY = crossY;
                testZ = crossZ;
            }
            else if(testX * crossX  + testY * crossY + testZ * crossZ < 0) 
            {
                return false;
            }
        }
        return true;
    }

    public default boolean isOrthogonalTo(EnumFacing face)
    {
        return Math.abs(this.getFaceNormal().dotProduct(new Vec3f(face.getDirectionVec()))) <= QuadHelper.EPSILON;
    }

    public default boolean isOnSinglePlane()
    {
        if(this.vertexCount() == 3) return true;

        Vec3f fn = this.getFaceNormal();
        if(fn == null) return false;

        float faceX = fn.x;
        float faceY = fn.y;
        float faceZ = fn.z;

        Vertex first = this.getVertex(0);
        
        for(int i = 3; i < this.vertexCount(); i++)
        {
            Vertex v = this.getVertex(i);
            if(v == null) return false;
            
            float dx = v.x - first.x;
            float dy = v.y - first.y;
            float dz = v.z - first.z;

            if(Math.abs(faceX * dx + faceY * dy + faceZ * dz) > QuadHelper.EPSILON) return false;
        }

        return true;
    }

    public default boolean isOnFace(EnumFacing face, float tolerance)
    {
        if(face == null) return false;
        boolean retVal = true;
        for(int i = 0; i < this.vertexCount(); i++)
        {
            retVal = retVal && getVertex(i).isOnFacePlane(face, tolerance);
        }
        return retVal;
    }
    
    public default Vec3f computeFaceNormal()
    {
        try
        {
            return getVertex(2).subtract(getVertex(0)).crossProduct(getVertex(3).subtract(getVertex(1))).normalize();
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
        ax = (N.x>0 ? N.x : -N.x);    // abs x-coord
        ay = (N.y>0 ? N.y : -N.y);    // abs y-coord
        az = (N.z>0 ? N.z : -N.z);    // abs z-coord

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
                area += (V[i].y * (V[j].z - V[k].z));
            break;
          case 2:
            for (i=1, j=2, k=0; i<n; i++, j++, k++)
                area += (V[i].z * (V[j].x - V[k].x));
            break;
          case 3:
            for (i=1, j=2, k=0; i<n; i++, j++, k++)
                area += (V[i].x * (V[j].y - V[k].y));
            break;
        }
        
        switch (coord)
        {    // wrap-around term
          case 1:
            area += (V[n].y * (V[1].z - V[n-1].z));
            break;
          case 2:
            area += (V[n].z * (V[1].x - V[n-1].x));
            break;
          case 3:
            area += (V[n].x * (V[1].y - V[n-1].y));
            break;
        }

        // scale to get area before projection
        an = MathHelper.sqrt( ax*ax + ay*ay + az*az); // length of normal vector
        switch (coord)
        {
          case 1:
            area *= (an / (2 * N.x));
            break;
          case 2:
            area *= (an / (2 * N.y));
            break;
          case 3:
            area *= (an / (2 * N.z));
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
    public default EnumFacing getActualFace()
    {
        EnumFacing nominalFace = this.getNominalFace();
        
        // semantic face will be right most of the time
        if(this.isOnFace(nominalFace, QuadHelper.EPSILON)) return nominalFace;

        for(EnumFacing f : EnumFacing.values())
        {
            if(f != nominalFace && this.isOnFace(f, QuadHelper.EPSILON)) return f;
        }
        return null;
    }

    /**
     * Randomly recolors all the polygons as an aid to debugging.
     * Polygons must be mutable and are mutated by this operation.
     */
    static void recolor(Collection<IPolygon> target)
    {
        Stream<IPolygon> quadStream;
    
        if (target.size() > 200) {
            quadStream = target.parallelStream();
        } else {
            quadStream = target.stream();
        }
    
        quadStream.forEach((IPolygon quad) -> quad.mutableReference().replaceColor((ThreadLocalRandom.current().nextInt(0x1000000) & 0xFFFFFF) | 0xFF000000));
    }
}
