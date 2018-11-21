package grondag.exotic_matter.model.mesh;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.painting.SurfaceTopology;
import grondag.exotic_matter.model.primitives.better.IPaintablePoly;
import grondag.exotic_matter.model.primitives.better.IPaintedPoly;
import grondag.exotic_matter.model.primitives.better.IPaintedVertex;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.varia.SimpleUnorderedArrayList;
import grondag.exotic_matter.varia.Useful;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public class MeshHelper
{
    // NOTE: this is a prototype implementation
    // It's fine for smaller objects, but would likely generate excess polys for big shapes after CSG operations.
    // Also needs better/different texture handling for top and bottom when face diameter is > 1.
    // Will probably need separate version for creating orthogonalAxis-aligned cylinders and cones.  
    // Also needs a parameter for minimum slices to reduce poly count on small model parts when appropriate.
    // Right now minimum is fixed at 12.
    public static List<IPaintedPoly<IPaintedVertex>> makeCylinder(Vec3d start, Vec3d end, double startRadius, double endRadius, IPaintablePoly template)
    {
        double circumference = Math.PI * Math.max(startRadius, endRadius) * 2;
        int textureSlices = (int) Math.max(1, Math.round(circumference));
        int polysPerTextureSlice = 1;
        while(textureSlices * polysPerTextureSlice < 12) polysPerTextureSlice++;
        int polySlices = textureSlices * polysPerTextureSlice;

        double length = start.distanceTo(end);
        int raySlices = (int) Math.ceil(length);
        
        final Vec3d axisZ = end.subtract(start).normalize();
        boolean isY = (Math.abs(axisZ.y) > 0.5);
        final Vec3d axisX = new Vec3d(isY ? 1 : 0, !isY ? 1 : 0, 0)
                .crossProduct(axisZ).normalize();
        final Vec3d axisY = axisX.crossProduct(axisZ).normalize();
        IPaintablePoly top = template.claimCopy(polySlices);
        IPaintablePoly bottom = template.claimCopy(polySlices);
        IPaintablePoly side = template.claimCopy(4);
        
        List<IPaintedPoly<IPaintedVertex>> results = new ArrayList<>(48);

        for (int i = 0; i < polySlices; i++) {
            double t0 = i / (double) polySlices, t1 = (i + 1) / (double) polySlices;

            for(int j = 0; j < raySlices; j++ )
            {
                double rayLength = Math.min(1,  length - j);
                Vec3d centerStart = start.add(axisZ.scale(j));
                Vec3d centerEnd = start.add(axisZ.scale(j + rayLength));
                
                double quadStartRadius = Useful.linearInterpolate(startRadius, endRadius, (double) j / raySlices );
                double quadEndRadius = Useful.linearInterpolate(startRadius, endRadius, Math.min(1, (double) (j + 1) / raySlices ));

                double uStart = ((double) (i % polysPerTextureSlice) / polysPerTextureSlice);
                double u0 = uStart;
                double u1 = uStart + 1.0 / polysPerTextureSlice;
                double v0 = 0;
                double v1 = rayLength;
                
                Vec3d n0 = cylNormal(axisX, axisY, t1);
                Vec3d n1= cylNormal(axisX, axisY, t0);
                
                
                side.setVertex(0, centerStart.add(n0.scale(quadStartRadius)), u0, v0, 0xFFFFFFFF, n0);
                side.setVertex(1, centerStart.add(n1.scale(quadStartRadius)), u1, v0, 0xFFFFFFFF, n1);
                side.setVertex(2, centerEnd.add(n1.scale(quadEndRadius)), u1, v1, 0xFFFFFFFF, n1);
                side.setVertex(3, centerEnd.add(n0.scale(quadEndRadius)), u0, v1, 0xFFFFFFFF, n0);
                side.addPaintedQuadsToList(0, results);
                
                if(j == 0 || j == raySlices - 1)
                {
                    double angle = t0 * Math.PI * 2;
                    double u = 8.0 + Math.cos(angle) * 8.0;
                    double v = 8.0 + Math.sin(angle) * 8.0;

                    if(j == 0)
                    {    
                        bottom.setVertex(i, centerStart.add(n0.scale(quadStartRadius)), u, v, 0xFFFFFFFF, null);                
                    }
                    if(j == raySlices - 1)
                    {
                        top.setVertex(polySlices - i - 1, centerEnd.add(n0.scale(quadEndRadius)), u, v, 0xFFFFFFFF, null);
                    }
                }
            }
        
        }

        top.addPaintedQuadsToList(0, results);
        bottom.addPaintedQuadsToList(0, results);
        
        top.release();
        bottom.release();
        side.release();
        return results;
    }

    private static Vec3d cylNormal(Vec3d axisX, Vec3d axisY, double slice) {
            double angle = slice * Math.PI * 2;
            return axisX.scale(Math.cos(angle)).add(axisY.scale(Math.sin(angle)));
    }
    
    /**
     * Makes a regular icosahedron, which is a very close approximation to a sphere for most purposes.
     * Loosely based on http://blog.andreaskahler.com/2009/06/creating-icosphere-mesh-in-code.html
     */
    public static List<IPaintedPoly<IPaintedVertex>> makeIcosahedron(Vec3d center, double radius, IPaintablePoly template, boolean smoothNormals) 
    {
        /** vertex scale */
        final double s = radius  / (2 * Math.sin(2 * Math.PI / 5));
        
        Vec3d[] vertexes = new Vec3d[12];
        
        // create 12 vertices of a icosahedron
        final double t = s * (1.0 + Math.sqrt(5.0)) / 2.0;
        int vi = 0;
        
        vertexes[vi++] = new Vec3d(-s,  t,  0).add(center);
        vertexes[vi++] = new Vec3d( s,  t,  0).add(center);
        vertexes[vi++] = new Vec3d(-s, -t,  0).add(center);
        vertexes[vi++] = new Vec3d( s, -t,  0).add(center);
        
        vertexes[vi++] = new Vec3d( 0, -s,  t).add(center);
        vertexes[vi++] = new Vec3d( 0,  s,  t).add(center);
        vertexes[vi++] = new Vec3d( 0, -s, -t).add(center);
        vertexes[vi++] = new Vec3d( 0,  s, -t).add(center);
        
        vertexes[vi++] = new Vec3d( t,  0, -s).add(center);
        vertexes[vi++] = new Vec3d( t,  0,  s).add(center);
        vertexes[vi++] = new Vec3d(-t,  0, -s).add(center);
        vertexes[vi++] = new Vec3d(-t,  0,  s).add(center);

        Vec3d[] normals = null;
        if(smoothNormals)
        {
            normals = new Vec3d[12];
            for(int i = 0; i < 12; i++)
            {
                normals[i] = vertexes[i].subtract(center).normalize();
            }
        }
        
        // create 20 triangles of the icosahedron
        List<IPaintedPoly<IPaintedVertex>> results = new ArrayList<>(20);
        
        IPaintablePoly poly = template.claimCopy(3);
       
        Surface.Builder surfBuilder = Surface.builder(poly.getSurfaceInstance());
        if(surfBuilder.topology() == SurfaceTopology.TILED)
        {
            final float uvMax = (float) (2 * s);
            poly.setMaxU(0, uvMax);
            poly.setMaxV(0, uvMax);
            surfBuilder.withWrapDistance(uvMax);
        }
        poly.setSurfaceInstance(surfBuilder.build());
        
        //enable texture randomization
        int salt = 0;
        poly.setTextureSalt(salt++);
        makeIcosahedronFace(true, 0, 11, 5, vertexes, normals, poly).addPaintedQuadsToList(0, results);
        makeIcosahedronFace(false, 4, 5, 11, vertexes, normals, poly).addPaintedQuadsToList(0, results);
        
        poly.setTextureSalt(salt++);
        makeIcosahedronFace(true, 0, 5, 1, vertexes, normals, poly).addPaintedQuadsToList(0, results);
        makeIcosahedronFace(false, 9, 1, 5, vertexes, normals, poly).addPaintedQuadsToList(0, results);
        
        poly.setTextureSalt(salt++);
        makeIcosahedronFace(true,  0, 1, 7, vertexes, normals, poly).addPaintedQuadsToList(0, results);
        makeIcosahedronFace(false, 8, 7, 1, vertexes, normals, poly).addPaintedQuadsToList(0, results);
        
        poly.setTextureSalt(salt++);
        makeIcosahedronFace(true, 0, 7, 10, vertexes, normals, poly).addPaintedQuadsToList(0, results);
        makeIcosahedronFace(false, 6, 10, 7, vertexes, normals, poly).addPaintedQuadsToList(0, results);
        
        poly.setTextureSalt(salt++);
        makeIcosahedronFace(true, 0, 10, 11, vertexes, normals, poly).addPaintedQuadsToList(0, results);
        makeIcosahedronFace(false, 2, 11, 10, vertexes, normals, poly).addPaintedQuadsToList(0, results);

        poly.setTextureSalt(salt++);
        makeIcosahedronFace(true, 5, 4, 9, vertexes, normals, poly).addPaintedQuadsToList(0, results);
        makeIcosahedronFace(false, 3, 9, 4, vertexes, normals, poly).addPaintedQuadsToList(0, results);

        poly.setTextureSalt(salt++);
        makeIcosahedronFace(true, 11, 2, 4, vertexes, normals, poly).addPaintedQuadsToList(0, results);
        makeIcosahedronFace(false, 3, 4, 2, vertexes, normals, poly).addPaintedQuadsToList(0, results);
        
        poly.setTextureSalt(salt++);
        makeIcosahedronFace(true, 10, 6, 2, vertexes, normals, poly).addPaintedQuadsToList(0, results);
        makeIcosahedronFace(false, 3, 2, 6, vertexes, normals, poly).addPaintedQuadsToList(0, results);
        
        poly.setTextureSalt(salt++);
        makeIcosahedronFace(true, 7, 8, 6, vertexes, normals, poly).addPaintedQuadsToList(0, results);
        makeIcosahedronFace(false, 3, 6, 8, vertexes, normals, poly).addPaintedQuadsToList(0, results);

        poly.setTextureSalt(salt++);
        makeIcosahedronFace(true, 1, 9, 8, vertexes, normals, poly).addPaintedQuadsToList(0, results);
        makeIcosahedronFace(false, 3, 8, 9, vertexes, normals, poly).addPaintedQuadsToList(0, results);
  
        poly.release();
        
        return results;
    }
    
    private static IPaintablePoly makeIcosahedronFace(boolean topHalf, int p1, int p2, int p3, Vec3d[] points, @Nullable Vec3d[] normals, IPaintablePoly template)
    {
        if(normals == null)
        {
            if(topHalf)
            {
                template.setVertex(0, points[p1], 1, 1, 0xFFFFFFFF, null);
                template.setVertex(1, points[p2], 0, 1, 0xFFFFFFFF, null);
                template.setVertex(2, points[p3], 1, 0, 0xFFFFFFFF, null);
            }
            else
            {
                template.setVertex(0, points[p1], 0, 0, 0xFFFFFFFF, null);
                template.setVertex(1, points[p2], 1, 0, 0xFFFFFFFF, null);
                template.setVertex(2, points[p3], 0, 1, 0xFFFFFFFF, null);
            }
        }
        else
        {
            if(topHalf)
            {
                template.setVertex(0, points[p1], 1, 1, 0xFFFFFFFF, normals[p1]);
                template.setVertex(1, points[p2], 0, 1, 0xFFFFFFFF, normals[p2]);
                template.setVertex(2, points[p3], 1, 0, 0xFFFFFFFF, normals[p3]);
            }
            else
            {
                template.setVertex(0, points[p1], 0, 0, 0xFFFFFFFF, normals[p1]);
                template.setVertex(1, points[p2], 1, 0, 0xFFFFFFFF, normals[p2]);
                template.setVertex(2, points[p3], 0, 1, 0xFFFFFFFF, normals[p3]);
            }
        }
        // clear face normal if has been set somehow
        template.clearFaceNormal();
        return template;
    }

    /**
     * Collection version of {@link #makeBox(AxisAlignedBB, IPolygon, Consumer)}
     */
    @Deprecated // use the consumer version
    public static Collection<IPaintedPoly<IPaintedVertex>> makeBox(AxisAlignedBB box, IPaintablePoly template)
    {
        SimpleUnorderedArrayList<IPaintedPoly<IPaintedVertex>> result = new SimpleUnorderedArrayList<>(6);
        makeBox(box, template, result);
        return result;
    }
    
    /**
     * This method is intended for boxes that fit within a single world block.
     * Typically used with locked UV coordinates.
     */
    @SuppressWarnings("deprecation")
    public static void makeBox(AxisAlignedBB box, IPaintablePoly template, Consumer<IPaintedPoly<IPaintedVertex>> target)
    {
        IPaintablePoly quad = template.claimCopy(4);
        quad.setupFaceQuad(EnumFacing.UP, 1 - box.maxX, box.minZ, 1 - box.minX, box.maxZ, 1 - box.maxY, EnumFacing.SOUTH);
        quad.produceQuads(target);
    
        quad.setupFaceQuad(EnumFacing.DOWN, box.minX, box.minZ, box.maxX, box.maxZ, box.minY, EnumFacing.SOUTH);
        quad.produceQuads(target);
    
        //-X
        quad.setupFaceQuad(EnumFacing.WEST, box.minZ, box.minY, box.maxZ, box.maxY, box.minX, EnumFacing.UP);
        quad.produceQuads(target);
        
        //+X
        quad.setupFaceQuad(EnumFacing.EAST, 1 - box.maxZ, box.minY, 1 - box.minZ, box.maxY, 1 - box.maxX, EnumFacing.UP);
        quad.produceQuads(target);
        
        //-Z
        quad.setupFaceQuad(EnumFacing.NORTH, 1 - box.maxX, box.minY, 1 - box.minX, box.maxY, box.minZ, EnumFacing.UP);
        quad.produceQuads(target);
        
        //+Z
        quad.setupFaceQuad(EnumFacing.SOUTH, box.minX, box.minY, box.maxX, box.maxY, 1 - box.maxZ, EnumFacing.UP);
        quad.produceQuads(target);
        
        quad.release();
    }
    
    /**
     * This method is intended for boxes that span multiple world blocks.
     * Typically used with unlocked UV coordinates and tiled surface painter.
     * Will emit quads with uv min/max outside the 0-1 range.
     * Textures will render 1:1, no wrapping.
     * 
     * TODO: incomplete
     */
    public static List<IPaintedPoly<IPaintedVertex>> makeBigBox(Vec3f origin, final float xSize, final float ySize, final float zSize, IPaintablePoly template)
    {
        ImmutableList.Builder<IPaintedPoly<IPaintedVertex>> builder = ImmutableList.builder();
        
        final float xEnd = origin.x() + xSize;
        final float yEnd = origin.y() + ySize;
        final float zEnd = origin.z() + zSize;
        
        IPaintablePoly quad = template.claimCopy(4);
        quad.setLockUV(0, false);
        quad.setMinU(0, 0);
        quad.setMaxU(0, xSize);
        quad.setMinV(0, 0);
        quad.setMaxV(0, zSize);
        quad.setNominalFace(EnumFacing.UP);
        quad.setVertex(0, xEnd, yEnd, origin.z(), 0, 0, 0xFFFFFFFF, 0, 1, 0);
        quad.setVertex(1, origin.x(), yEnd, origin.z(), 0, 0, 0xFFFFFFFF, 0, 1, 0);
        quad.setVertex(2, origin.x(), yEnd, zEnd, 0, 0, 0xFFFFFFFF, 0, 1, 0);
        quad.setVertex(3, xEnd, yEnd, zEnd, 0, 0, 0xFFFFFFFF, 0, 1, 0);
//        quad.setupFaceQuad(EnumFacing.UP, 1 - box.maxX, box.minZ, 1 - box.minX, box.maxZ, 1 - box.maxY, EnumFacing.SOUTH);
        builder.add(quad);
    
//        quad = Poly.mutable(template);
//        quad.setupFaceQuad(EnumFacing.DOWN, box.minX, box.minZ, box.maxX, box.maxZ, box.minY, EnumFacing.SOUTH);
//        builder.add(quad);
//    
//        //-X
//        quad = Poly.mutable(template);
//        quad.setupFaceQuad(EnumFacing.WEST, box.minZ, box.minY, box.maxZ, box.maxY, box.minX, EnumFacing.UP);
//        builder.add(quad);
//        
//        //+X
//        quad = Poly.mutable(template);
//        quad.setupFaceQuad(EnumFacing.EAST, 1 - box.maxZ, box.minY, 1 - box.minZ, box.maxY, 1 - box.maxX, EnumFacing.UP);
//        builder.add(quad);
//        
//        //-Z
//        quad = Poly.mutable(template);
//        quad.setupFaceQuad(EnumFacing.NORTH, 1 - box.maxX, box.minY, 1 - box.minX, box.maxY, box.minZ, EnumFacing.UP);
//        builder.add(quad);
//        
//        //+Z
//        quad = Poly.mutable(template);
//        quad.setupFaceQuad(EnumFacing.SOUTH, box.minX, box.minY, box.maxX, box.maxY, 1 - box.maxZ, EnumFacing.UP);
//        builder.add(quad);
        
        return builder.build();
    }
}
