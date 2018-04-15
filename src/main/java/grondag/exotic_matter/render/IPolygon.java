package grondag.exotic_matter.render;

import java.util.List;

import javax.annotation.Nullable;

import grondag.exotic_matter.render.Surface.SurfaceInstance;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public interface IPolygon
{
    SurfaceInstance NO_SURFACE = new Surface(SurfaceType.MAIN, SurfaceTopology.CUBIC).unitInstance;

    public String getTextureName();

    public @Nullable EnumFacing getNominalFace();

    public Rotation getRotation();

    public int getColor();

    public boolean isFullBrightness();

    public boolean isLockUV();

    //FIXME: why is this necessary in an immutable interface?
    public IPolygon clone();
    
    /**
     * If this polygon is mutable, returns mutable reference to self.
     * Otherwise returns a mutable copy.
     */
    public IMutablePolygon mutableReference();
    
    /**
     * If this polygon is mutable, returns reference to self.
     * Otherwise returns a mutable copy.
     */
    public IMutablePolygon mutableCopy();
    
    
    /**
     * True if face normal has been established, either implicitly via access
     * or by directly setting a value. 
     */
    public boolean hasFaceNormal();
    
    public Vec3d getFaceNormal();
    
    public default  float[] getFaceNormalArray()
    {
        Vec3d normal = getFaceNormal();

        float[] retval = new float[3];

        retval[0] = (float)(normal.x);
        retval[1] = (float)(normal.y);
        retval[2] = (float)(normal.z);
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
    
    public List<IPolygon> toQuads();
    
    public List<IPolygon> toTris();
    
    /**
     * Returns polys that have been tagged with CSG metadata
     */
    public List<ICSGPolygon> toQuadsCSG();
    
    /**
     * Returns polys that have been tagged with CSG metadata
     */
    public List<ICSGPolygon> toTrisCSG();
    
    /**
     * Returns copy of self tagged with CSG metadata
     */
    public ICSGPolygon toCSG();
    
    /** 
     * Returns intersection point of given ray with the plane of this quad.
     * Return null if parallel or facing away.
     */
    public default Vec3d intersectionOfRayWithPlane(Vec3d origin, Vec3d direction)
    {
        Vec3d normal = this.getFaceNormal();

        double directionDotNormal = normal.dotProduct(direction);
        if (Math.abs(directionDotNormal) < QuadHelper.EPSILON) 
        { 
            // parallel
            return null;
        }

        Vec3d firstPoint = this.getVertex(0).toVec3d();
        
        double distanceToPlane = -normal.dotProduct((origin.subtract(firstPoint))) / directionDotNormal;
        // facing away from plane
        if(distanceToPlane < -QuadHelper.EPSILON) return null;

        return origin.add(direction.scale(distanceToPlane));
    }
    
    
    /**
     * Keeping for convenience in case discover any problems with the fast version.
     * Unit tests indicate identical results.
     */
    public default boolean intersectsWithRaySlow(Vec3d origin, Vec3d direction)
    {
        Vec3d intersection = this.intersectionOfRayWithPlane(origin, direction);
        
        // now we just need to test if point is inside this polygon
        return intersection == null ? false : containsPointSlow(intersection);
        
    }

    public default boolean intersectsWithRay(Vec3d origin, Vec3d direction)
    {
        Vec3d intersection = this.intersectionOfRayWithPlane(origin, direction);

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
    public default boolean containsPoint(Vec3d point)
    {
        return PointInPolygonTest.isPointInPolygon(point, this);
    }
    

    /**
     * Keeping for convenience in case discover any problems with the fast version.
     * Unit tests indicate identical results.
     */
    public default boolean containsPointSlow(Vec3d point)
    {
        double lastSignum = 0;
        Vec3d faceNormal = this.getFaceNormal();

        for(int i = 0; i < this.vertexCount(); i++)
        {
            int nextVertex = i + 1;
            if(nextVertex == this.vertexCount()) nextVertex = 0;

            Vec3d currentVertex = getVertex(i).toVec3d();
            
            Vec3d line = getVertex(nextVertex).toVec3d().subtract(currentVertex);
            Vec3d normalInPlane = faceNormal.crossProduct(line);

            double sign = normalInPlane.dotProduct(point.subtract(currentVertex));

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
        return Math.abs(this.getFaceNormal().dotProduct(new Vec3d(face.getDirectionVec()))) <= QuadHelper.EPSILON;
    }

    public default boolean isOnSinglePlane()
    {
        if(this.vertexCount() == 3) return true;

        Vec3d fn = this.getFaceNormal();
        if(fn == null) return false;

        float faceX = (float) fn.x;
        float faceY = (float) fn.y;
        float faceZ = (float) fn.z;

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
    
    public default Vec3d computeFaceNormal()
    {
        try
        {
            return getVertex(2).toVec3d().subtract(getVertex(0).toVec3d()).crossProduct(getVertex(3).toVec3d().subtract(getVertex(1).toVec3d())).normalize();
        }
        catch(Exception e)
        {
            assert false : "Bad polygon structure during face normal request.";
            return Vec3d.ZERO;
        }
    }

    public default double getArea()
    {
        if(this.vertexCount() == 3)
        {
            return Math.abs(getVertex(1).toVec3d().subtract(getVertex(0).toVec3d()).crossProduct(getVertex(2).toVec3d().subtract(getVertex(0).toVec3d())).lengthVector()) / 2.0;

        }
        else if(this.vertexCount() == 4) //quad
        {
            return Math.abs(getVertex(2).toVec3d().subtract(getVertex(0).toVec3d()).crossProduct(getVertex(3).toVec3d().subtract(getVertex(1).toVec3d())).lengthVector()) / 2.0;
        }
        else
        {
            double area = 0;
            for(IPolygon q : this.toQuads())
            {
                area += q.getArea();
            }
            return area;
        }
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
}
