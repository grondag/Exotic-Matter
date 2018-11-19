package grondag.exotic_matter.model.primitives;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import grondag.exotic_matter.model.CSG.CSGNode;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.painting.SurfaceTopology;
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

public interface IPolygon extends IPaintableQuad
{
    Surface NO_SURFACE = Surface.builder(SurfaceTopology.CUBIC).build();

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
    
    public default  float[] getFaceNormalArray()
    {
        Vec3f normal = getFaceNormal();

        float[] retval = new float[3];

        retval[0] = normal.x();
        retval[1] = normal.y();
        retval[2] = normal.z();
        return retval;
    }
    
    public Vertex getVertex(int index);
    
    public void forEachVertex(Consumer<Vertex> consumer);
    
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
        
        ImmutableList.Builder<IPolygon> builder = ImmutableList.builder();
        this.toQuads(p -> builder.add(p), ensureConvex);
        return builder.build();
    }
    
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
     * If this is a quad or higher order polygon, returns new tris.
     * If is already a tri returns self.<p>
     */
    public default List<IPolygon> toTris()
    {
        if(this.vertexCount() == 3) return ImmutableList.of(this);
        ImmutableList.Builder<IPolygon> builder = ImmutableList.builder();
        this.toTris(p -> builder.add(p));
        return builder.build();
    }
    
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
    
    
    /** 
     * Returns intersection point of given ray with the plane of this quad.
     * Return null if parallel or facing away.<p/>
     * 
     * Direction provided MUST BE NORMALIZED.
     * 
     */
    public default @Nullable Vec3f intersectionOfRayWithPlane(float originX, float originY, float originZ, float directionX, float directionY, float directionZ)
    {
        Vec3f normal = this.getFaceNormal();
        
        final float normX = normal.x();
        final float normY = normal.y();
        final float normZ = normal.z();

        float directionDotNormal = directionX * normX + directionY * normY + directionZ * normZ;
        if (Math.abs(directionDotNormal) < QuadHelper.EPSILON) 
        { 
            // parallel
            return null;
        }

        Vertex firstPoint = this.getVertex(0);
        
        float dx = originX - firstPoint.x();
        float dy = originY - firstPoint.y();
        float dz = originZ - firstPoint.z();
        
        float distanceToPlane = -(dx * normX + dy * normY + dz * normZ) / directionDotNormal;
        //double distanceToPlane = -normal.dotProduct((origin.subtract(firstPoint))) / directionDotNormal;
        // facing away from plane
        if(distanceToPlane < -QuadHelper.EPSILON) return null;

        return Vec3f.create(originX + directionZ * distanceToPlane, originY + directionY * distanceToPlane, originZ + directionZ * distanceToPlane);
//        return origin.add(direction.scale(distanceToPlane));
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
        return isConvex(this.vertexArray());
    }

    public static boolean isConvex(Vertex[] vertices)
    {
        final int size = vertices.length;
        
        if(size == 3) return true;

        float testX = 0;
        float testY = 0;
        float testZ = 0;
        boolean needTest = true;
                
        for(int thisIndex = 0; thisIndex < size; thisIndex++)
        {
            int nextIndex = thisIndex + 1;
            if(nextIndex == size) nextIndex = 0;

            int priorIndex = thisIndex - 1;
            if(priorIndex == -1) priorIndex = size - 1;

            final Vertex thisVertex =  vertices[thisIndex];
            final Vertex nextVertex = vertices[nextIndex];
            final Vertex priorVertex = vertices[priorIndex];
            
            final float ax = thisVertex.x() - priorVertex.x();
            final float ay = thisVertex.y() - priorVertex.y();
            final float az = thisVertex.z() - priorVertex.z();
            
            final float bx = nextVertex.x() - thisVertex.x();
            final float by = nextVertex.y() - thisVertex.y();
            final float bz = nextVertex.z() - thisVertex.z();

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
    
    public static Consumer<IPolygon> makeRecoloring(Consumer<IPolygon> wrapped)
    {
        final Random r = ThreadLocalRandom.current();
        return p -> p.mutableReference().replaceColor((r.nextInt(0x1000000) & 0xFFFFFF) | 0xFF000000);
    }
    
    public default void addBakedQuadsToBuilder(Builder<BakedQuad> builder, boolean isItem)
    {
        builder.add(QuadBakery.createBakedQuad(this, isItem));
    }

    public default void produceGeometricVertices(IGeometricVertexConsumer consumer)
    {
        this.forEachVertex(v -> consumer.acceptVertex(v.x(), v.y(), v.z()));
    }
    
    @SuppressWarnings("null")
    public default void produceNormalVertices(INormalVertexConsumer consumer)
    {
        Vec3f faceNorm = this.getFaceNormal();
        
        this.forEachVertex(v -> 
        {
            if(v.normal() == null)
                consumer.acceptVertex(v.x(), v.y(), v.z(), faceNorm.x(), faceNorm.y(), faceNorm.z());
            else
                consumer.acceptVertex(v.x(), v.y(), v.z(), v.normal().x(), v.normal().y(), v.normal().z());
        });
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
