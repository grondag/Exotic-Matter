package grondag.exotic_matter.render;

import java.util.List;

import javax.annotation.Nullable;

import grondag.exotic_matter.render.Surface.SurfaceInstance;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

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
    public default boolean isCSG()
    {
        return false;
    }
    
    /**
     * If this polygon has CSG metadata and supports the CSG interface, 
     * returns mutable reference to self as such.
     * Thows an exception otherwise.
     */
    public default ICSGPolygon csgReference()
    {
        throw new UnsupportedOperationException();
    }

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
     */
    public List<IPolygon> toQuads();
    
    /** 
     * If this is a quad or higher order polygon, returns new tris.
     * If is already a tri returns self.<p>
     */
    public List<IPolygon> toTris();
    
    
    /**
     * Returns copy of self tagged with CSG metadata
     * 
     * TODO: semantics of this are inconsistent with factory methods for new CSG 
     */
    public ICSGPolygon toCSG();
    
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

    public default float getArea()
    {
        if(this.vertexCount() == 3)
        {
            return Math.abs(getVertex(1).subtract(getVertex(0)).crossProduct(getVertex(2).subtract(getVertex(0))).lengthVector()) / 2.0f;

        }
        else if(this.vertexCount() == 4) //quad
        {
            return Math.abs(getVertex(2).subtract(getVertex(0)).crossProduct(getVertex(3).subtract(getVertex(1))).lengthVector()) / 2.0f;
        }
        else
        {
            float area = 0;
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
