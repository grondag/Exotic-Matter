package grondag.exotic_matter;

import java.io.FileInputStream;
import java.nio.ByteBuffer;

import org.junit.Test;

import grondag.exotic_matter.model.ModShapes;
import grondag.exotic_matter.model.ModelState;
import grondag.exotic_matter.model.TerrainMeshFactory;
import grondag.exotic_matter.render.CSGNode;
import grondag.exotic_matter.render.CSGPlane;

public class TerrainPerf
{

    @Test
    public void test()
    {
        ModelState[] modelStates = new ModelState[120000];
        int[] offenders = new int[modelStates.length];
        ConfigXM.BLOCKS.simplifyTerrainBlockGeometry = true;
        
        try
          {
            FileInputStream fis = new FileInputStream("terrainState.data");
            ByteBuffer bytes = ByteBuffer.allocate(modelStates.length * 4 * Long.BYTES);
            fis.getChannel().read(bytes);
            fis.close();
            bytes.flip();
            for(int i = 0; i < modelStates.length; i++)
            {
                ModelState newState = new ModelState(bytes.getLong(), bytes.getLong(), bytes.getLong(), bytes.getLong());
                assert newState.getShape() == ModShapes.TERRAIN_FILLER || newState.getShape() == ModShapes.TERRAIN_HEIGHT;
                assert newState.getTerrainState() != null;
                modelStates[i] = newState;
            }
          }
          catch (Exception e)
          {
              e.printStackTrace();
              return;
          }
        
        TerrainMeshFactory mesher = new TerrainMeshFactory();
         
        for(int i = 0; i < 10; i++)
        {
            long elapsed = 0;
            long min = Long.MAX_VALUE;
            long max = 0;
            int longCount = 0;
            int errorCount = 0;
            
//            int minOffset = Integer.MAX_VALUE;
//            int maxOffset = Integer.MIN_VALUE;
            
            for(int j = 0; j < modelStates.length; j++)
            {
                ModelState modelState = modelStates[j];
                
//                int y = modelState.getTerrainState().getYOffset();
//                if(y < minOffset) minOffset = y;
//                if(y > maxOffset) maxOffset = y;
                
                final long start = System.nanoTime();
                try
                {
                    mesher.getShapeQuads(modelState);
                }
                catch(Exception e)
                {
                    errorCount++;
//                    e.printStackTrace();
                }
                long t = System.nanoTime() - start;
                min = Math.min(min, t);
                max = Math.max(max, t);
                if(t > 60000)
                {
                    offenders[j]++;
                    longCount++;
                }
                elapsed += t;
            }
            
            System.out.println("getShapeQuads mean time = " + elapsed / modelStates.length  + "ns");
            System.out.println("getShapeQuads min time  = " + min  + "ns");
            System.out.println("getShapeQuads max time  = " + max  + "ns");
            System.out.println("Runs exceeding 60,000ns: " + longCount);
//            CSGNode.Root.recombinedRenderableQuadsCounter.reportAndClear();
//            CSGPlane.splitTimer.reportAndClear();
//            CSGPlane.splitSpanningTimer.reportAndClear();
            System.out.println("Error count = " + errorCount);
//            System.out.println("minOffset = " + minOffset);
//            System.out.println("maxOffset = " + maxOffset);
            System.out.println(" ");
        }
        
        int offenderCount = 0;
        int goodConcavity = 0;
        int offenderConcavity = 0;
        System.out.println("Repeat offenders");
        for(int j = 0; j < modelStates.length; j++)
        {
            final int concavity = modelStates[j].getTerrainState().divergence();
            if(offenders[j] > 4)
            {
                offenderConcavity += concavity;
                offenderCount++;
                System.out.println(offenders[j] + "x " + modelStates[j].getTerrainState().toString());
            }
            else
            {
                goodConcavity += concavity;
            }
        }
        System.out.println("Repeat offender count: " + offenderCount);
        System.out.println("Average concavity non-offenders: " + (float) goodConcavity / (modelStates.length - offenderCount));
        System.out.println("Average concavity offenders: " + (float) offenderConcavity / offenderCount);
    }

    
}