package grondag.exotic_matter.model.collision;

import static grondag.exotic_matter.model.collision.octree.OctreeCoordinates.ALL_EMPTY;

import java.util.List;
import java.util.function.Consumer;

import grondag.exotic_matter.model.collision.octree.OctreeCoordinates;
import grondag.exotic_matter.model.collision.octree.VoxelVolume;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.primitives.TriangleBoxTest;
import grondag.exotic_matter.model.primitives.Vertex;
import net.minecraft.util.math.AxisAlignedBB;

public class FastBoxGenerator implements Consumer<IPolygon>
{
    // diameters
    static final float D1 = 0.5f;
    static final float D2 = D1 * 0.5f;
    static final float D3 = D2 * 0.5f;
    
    // radii
    static final float R1 = D1 * 0.5f;
    static final float R2 = D2 * 0.5f;
    static final float R3 = D3 * 0.5f;
    
    // center offsets, low and high
    static final float CLOW1 = 0.25f;
    static final float CHIGH1 = CLOW1 + D1;
    
    static final float CLOW2 = CLOW1 * 0.5f;
    static final float CHIGH2 = CLOW2 + D2;
    
    static final float CLOW3 = CLOW2 * 0.5f;
    static final float CHIGH3 = CLOW3 + D3;
    
    private static void div1(final float[] polyData, final long[] voxelBits)
    {
        if(TriangleBoxTest.triBoxOverlap(CLOW1, CLOW1, CLOW1, R1, polyData))
            div2(0000, 0.0f, 0.0f, 0.0f, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(CHIGH1, CLOW1, CLOW1, R1, polyData))
            div2(0100, D1, 0.0f, 0.0f, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(CLOW1, CHIGH1, CLOW1, R1, polyData))
            div2(0200, 0.0f, D1, 0.0f, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(CHIGH1, CHIGH1, CLOW1, R1, polyData))
            div2(0300, D1, D1, 0.0f, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(CLOW1, CLOW1, CHIGH1, R1, polyData))
            div2(0400, 0.0f, 0.0f, D1, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(CHIGH1, CLOW1, CHIGH1, R1, polyData))
            div2(0500, D1, 0.0f, D1, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(CLOW1, CHIGH1, CHIGH1, R1, polyData))
            div2(0600, 0.0f, D1, D1, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(CHIGH1, CHIGH1, CHIGH1, R1, polyData))
            div2(0700, D1, D1, D1, polyData, voxelBits);
            
    }

    private static void div2(int baseIndex, float x0, float y0, float z0, float[] polyData, long[] voxelBits)
    {
        final float a0 = x0 + CLOW2;
        final float a1 = x0 + CHIGH2;
        final float b0 = y0 + CLOW2;
        final float b1 = y0 + CHIGH2;
        final float c0 = z0 + CLOW2;
        final float c1 = z0 + CHIGH2;
        
        final float x1 = x0 + D2;
        final float y1 = y0 + D2;
        final float z1 = z0 + D2;
        
        if(TriangleBoxTest.triBoxOverlap(a0, b0, c0, R2, polyData))
            div3(baseIndex + 000, x0, y0, z0, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b0, c0, R2, polyData))
            div3(baseIndex + 010, x1, y0, z0, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a0, b1, c0, R2, polyData))
            div3(baseIndex + 020, x0, y1, z0, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b1, c0, R2, polyData))
            div3(baseIndex + 030, x1, y1, z0, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a0, b0, c1, R2, polyData))
            div3(baseIndex + 040, x0, y0, z1, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b0, c1, R2, polyData))
            div3(baseIndex + 050, x1, y0, z1, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a0, b1, c1, R2, polyData))
            div3(baseIndex + 060, x0, y1, z1, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b1, c1, R2, polyData))
            div3(baseIndex + 070, x1, y1, z1, polyData, voxelBits);
    }
    
    private static void div3(int baseIndex, float x0, float y0, float z0, float[] polyData, long[] voxelBits)
    {
        final float a0 = x0 + CLOW3;
        final float a1 = x0 + CHIGH3;
        final float b0 = y0 + CLOW3;
        final float b1 = y0 + CHIGH3;
        final float c0 = z0 + CLOW3;
        final float c1 = z0 + CHIGH3;
        
        if(TriangleBoxTest.triBoxOverlap(a0, b0, c0, R3, polyData))
            setVoxelBit(baseIndex + 00, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b0, c0, R3, polyData))
            setVoxelBit(baseIndex + 01, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a0, b1, c0, R3, polyData))
            setVoxelBit(baseIndex + 02, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b1, c0, R3, polyData))
            setVoxelBit(baseIndex + 03, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a0, b0, c1, R3, polyData))
            setVoxelBit(baseIndex + 04, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b0, c1, R3, polyData))
            setVoxelBit(baseIndex + 05, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a0, b1, c1, R3, polyData))
            setVoxelBit(baseIndex + 06, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b1, c1, R3, polyData))
            setVoxelBit(baseIndex + 07, voxelBits);
    }
    
    static void setVoxelBit(int voxelIndex3, long[] voxelBits)
    {
        final int xyz = OctreeCoordinates.indexToXYZ3(voxelIndex3);
        voxelBits[xyz >> 6] |= (1L << (xyz & 63));
    }
    
    private final long[] voxelBits = new long[16];
    private final float[] polyData = new float[36];
    private final JoiningBoxListBuilder builder = new JoiningBoxListBuilder();

    @SuppressWarnings("null")
    @Override
    public final void accept(IPolygon poly)
    {
        Vertex[] v  = poly.vertexArray();
        
        acceptTriangle(v[0], v[1], v[2]);
        
        if(poly.vertexCount() == 4)
            acceptTriangle(v[0], v[2], v[3]);
    }

    protected final void acceptTriangle(Vertex v0, Vertex v1, Vertex v2)
    {
        final float[] data = polyData;
        TriangleBoxTest.packPolyData(v0, v1, v2, data);
        div1(polyData, voxelBits);
    }

    public final List<AxisAlignedBB> build()
    {
        builder.clear();
        final long[] data = this.voxelBits;
        VoxelVolume.fillVolume8(data);
        VoxelVolume.forEachSimpleVoxelInner(data, (x, y, z) ->
        {
            builder.addSorted(x, y, z, x + 2, y + 2, z + 2);
        });
        
        // prep for next use
        System.arraycopy(ALL_EMPTY, 0, data, 0, 16);
        
        return builder.build();
    }
}
