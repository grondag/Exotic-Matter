package grondag.exotic_matter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Random;

import org.junit.Test;

import grondag.exotic_matter.model.ModShapes;
import grondag.exotic_matter.model.ModelState;
import grondag.exotic_matter.model.TerrainMeshFactory;
import grondag.exotic_matter.model.TerrainState;
import it.unimi.dsi.fastutil.longs.LongArrayList;

public class TerrainPerf
{

    @Test
    public void test()
    {
        ModelState[] modelStates = new ModelState[120000];
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
         
        for(int i = 0; i < 100; i++)
        {
            long elapsed = 0;
            long min = Long.MAX_VALUE;
            long max = 0;
            int errorCount = 0;
            
            for(ModelState modelState : modelStates)
            {
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
                elapsed += t;
            }
            
            System.out.println("getShapeQuads mean time = " + elapsed / modelStates.length  + "ns");
            System.out.println("getShapeQuads min time  = " + min  + "ns");
            System.out.println("getShapeQuads max time  = " + max  + "ns");
            System.out.println("Error count = " + errorCount);
            System.out.println(" ");
        }
        
    }

    
}