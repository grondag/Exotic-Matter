package grondag.exotic_matter.model.collision;

import static grondag.exotic_matter.model.collision.octree.OctreeCoordinates.ALL_EMPTY;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.ConfigXM;
import grondag.exotic_matter.model.collision.octree.OctreeCoordinates;
import grondag.exotic_matter.model.collision.octree.VoxelVolume16;
import grondag.exotic_matter.model.primitives.TriangleBoxTest;
import grondag.exotic_matter.model.primitives.Vertex;
import net.minecraft.util.math.AxisAlignedBB;

public class OptimalBoxGenerator extends AbstractBoxGenerator
{
    private static void div1(final float[] polyData, final long[] voxelBits)
    {
        if(TriangleBoxTest.triBoxOverlap(CLOW1, CLOW1, CLOW1, R1, polyData))
            div2(00000, 0.0f, 0.0f, 0.0f, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(CHIGH1, CLOW1, CLOW1, R1, polyData))
            div2(01000, D1, 0.0f, 0.0f, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(CLOW1, CHIGH1, CLOW1, R1, polyData))
            div2(02000, 0.0f, D1, 0.0f, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(CHIGH1, CHIGH1, CLOW1, R1, polyData))
            div2(03000, D1, D1, 0.0f, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(CLOW1, CLOW1, CHIGH1, R1, polyData))
            div2(04000, 0.0f, 0.0f, D1, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(CHIGH1, CLOW1, CHIGH1, R1, polyData))
            div2(05000, D1, 0.0f, D1, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(CLOW1, CHIGH1, CHIGH1, R1, polyData))
            div2(06000, 0.0f, D1, D1, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(CHIGH1, CHIGH1, CHIGH1, R1, polyData))
            div2(07000, D1, D1, D1, polyData, voxelBits);
            
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
            div3(baseIndex + 0000, x0, y0, z0, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b0, c0, R2, polyData))
            div3(baseIndex + 0100, x1, y0, z0, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a0, b1, c0, R2, polyData))
            div3(baseIndex + 0200, x0, y1, z0, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b1, c0, R2, polyData))
            div3(baseIndex + 0300, x1, y1, z0, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a0, b0, c1, R2, polyData))
            div3(baseIndex + 0400, x0, y0, z1, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b0, c1, R2, polyData))
            div3(baseIndex + 0500, x1, y0, z1, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a0, b1, c1, R2, polyData))
            div3(baseIndex + 0600, x0, y1, z1, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b1, c1, R2, polyData))
            div3(baseIndex + 0700, x1, y1, z1, polyData, voxelBits);
    }
    
    private static void div3(int baseIndex, float x0, float y0, float z0, float[] polyData, long[] voxelBits)
    {
        final float a0 = x0 + CLOW3;
        final float a1 = x0 + CHIGH3;
        final float b0 = y0 + CLOW3;
        final float b1 = y0 + CHIGH3;
        final float c0 = z0 + CLOW3;
        final float c1 = z0 + CHIGH3;
        
        final float x1 = x0 + D3;
        final float y1 = y0 + D3;
        final float z1 = z0 + D3;
        
        if(TriangleBoxTest.triBoxOverlap(a0, b0, c0, R3, polyData))
            div4(baseIndex + 000, x0, y0, z0, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b0, c0, R3, polyData))
            div4(baseIndex + 010, x1, y0, z0, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a0, b1, c0, R3, polyData))
            div4(baseIndex + 020, x0, y1, z0, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b1, c0, R3, polyData))
            div4(baseIndex + 030, x1, y1, z0, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a0, b0, c1, R3, polyData))
            div4(baseIndex + 040, x0, y0, z1, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b0, c1, R3, polyData))
            div4(baseIndex + 050, x1, y0, z1, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a0, b1, c1, R3, polyData))
            div4(baseIndex + 060, x0, y1, z1, polyData, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b1, c1, R3, polyData))
            div4(baseIndex + 070, x1, y1, z1, polyData, voxelBits);
    }
    
    private static void div4(int baseIndex, float x0, float y0, float z0, float[] polyData, long[] voxelBits)
    {
        final float a0 = x0 + CLOW4;
        final float a1 = x0 + CHIGH4;
        final float b0 = y0 + CLOW4;
        final float b1 = y0 + CHIGH4;
        final float c0 = z0 + CLOW4;
        final float c1 = z0 + CHIGH4;
        
        if(TriangleBoxTest.triBoxOverlap(a0, b0, c0, R4, polyData))
            setVoxelBit(baseIndex + 00, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b0, c0, R4, polyData))
            setVoxelBit(baseIndex + 01, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a0, b1, c0, R4, polyData))
            setVoxelBit(baseIndex + 02, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b1, c0, R4, polyData))
            setVoxelBit(baseIndex + 03, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a0, b0, c1, R4, polyData))
            setVoxelBit(baseIndex + 04, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b0, c1, R4, polyData))
            setVoxelBit(baseIndex + 05, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a0, b1, c1, R4, polyData))
            setVoxelBit(baseIndex + 06, voxelBits);
        
        if(TriangleBoxTest.triBoxOverlap(a1, b1, c1, R4, polyData))
            setVoxelBit(baseIndex + 07, voxelBits);
    }
    
    static void setVoxelBit(int voxelIndex4, long[] voxelBits)
    {
        final int xyz = OctreeCoordinates.indexToXYZ4(voxelIndex4);
        voxelBits[xyz >> 6] |= (1L << (xyz & 63));
    }
    
    private final long[] voxelBits = new long[128];
    private final float[] polyData = new float[36];
    private final SimpleBoxListBuilder builder = new SimpleBoxListBuilder();
    final long[] snapshot = new long[8];
    final BoxFinder bf = new BoxFinder();
    
    
    @Override
    protected void acceptTriangle(Vertex v0, Vertex v1, Vertex v2)
    {
        final float[] data = polyData;
        TriangleBoxTest.packPolyData(v0, v1, v2, data);
        div1(polyData, voxelBits);        
    }

    public final ImmutableList<AxisAlignedBB> build()
    {
        builder.clear();
        final long[] data = this.voxelBits;
        VoxelVolume16.fillVolume(data);
        bf.clear();
        VoxelVolume16.forEachSimpleVoxel(data, (x, y, z) -> bf.setFilled(x, y, z));
        
        // prep for next use
        System.arraycopy(ALL_EMPTY, 0, data, 0, 64);
        
        bf.saveTo(snapshot);
        bf.outputBoxes(builder);

        while(builder.size() > ConfigXM.BLOCKS.collisionBoxBudget)
        {
            bf.restoreFrom(snapshot);
            
            if(!bf.simplify())
                break;
            
            bf.saveTo(snapshot);
            builder.clear();
            bf.outputBoxes(builder);
            
            // debug code to view/trace initial disjoint set selection in optimal simplification
//            if(builder.size() <= ConfigXM.BLOCKS.collisionBoxBudget)
//            {
//                ExoticMatter.INSTANCE.info("FINAL BOX STRUCTURE REPORT");
//                ExoticMatter.INSTANCE.info("=============================================");
//                bf.restoreFrom(snapshot);
//                bf.calcCombined();
//                bf.populateMaximalVolumes();
//                bf.populateIntersects();
//                bf.scoreMaximalVolumes();
//                bf.explainMaximalVolumes();
//                bf.findDisjointSets();
//                bf.explainDisjointSets();
//            }
        }

        // debug code to view/trace maximal volumes
//        bf.restoreFrom(snapshot);
//        bf.calcCombined();
//        bf.populateMaximalVolumes();
//        builder.clear();
//        int limit  = bf.volumeCount;
//        for(int i = 0; i < limit; i++)
//        {
//            bf.addBox(bf.maximalVolumes[i], builder);
//        }
        
        return builder.build();
    }
}
