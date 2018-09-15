package grondag.exotic_matter.model.collision;

import grondag.exotic_matter.ConfigXM;
import grondag.exotic_matter.ExoticMatter;

public class OptimalBoxGenerator extends AbstractVoxelBuilder
{
    final BoxFinder bf = new BoxFinder();
    
    final long[] snapshot = new long[8];
    
    @Override
    protected void generateBoxes(CollisionBoxListBuilder builder)
    {
        if(voxels.isEmpty())
            return;
        
        if(voxels.isFull())
            builder.add(voxels.xMin8(), voxels.yMin8(), voxels.zMin8(), voxels.xMax8(), voxels.yMax8(), voxels.zMax8());
        else
            generateBoxesInner(builder);
    }
    
    protected void generateBoxesInner(CollisionBoxListBuilder builder)
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
}
