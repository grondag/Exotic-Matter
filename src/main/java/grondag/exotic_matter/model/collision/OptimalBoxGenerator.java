package grondag.exotic_matter.model.collision;

import java.util.function.Consumer;

import grondag.exotic_matter.ConfigXM;
import grondag.exotic_matter.model.collision.octree.VoxelOctree16;
import grondag.exotic_matter.model.primitives.IPolygon;

public class OptimalBoxGenerator extends AbstractVoxelBuilder<VoxelOctree16> implements Consumer<IPolygon>
{
    protected OptimalBoxGenerator()
    {
        super(new SimpleBoxListBuilder(), new VoxelOctree16());
    }

    final BoxFinder bf = new BoxFinder();
    
    final long[] snapshot = new long[8];
    
    @Override
    protected void generateBoxes(ICollisionBoxListBuilder builder)
    {
        if(voxels.isEmpty())
            return;
        
        if(voxels.isFull())
            builder.add(0, 0, 0, 8, 8, 8);
        
        else
            generateBoxesInner(builder);
    }
    
    protected void generateBoxesInner(ICollisionBoxListBuilder builder)
    {
        bf.loadVoxels(voxels);
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
    }

    @Override
    public void accept(@SuppressWarnings("null") IPolygon poly)
    {
        super.accept(poly, 4);
    }
}
