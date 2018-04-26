package grondag.exotic_matter;

import java.util.Random;

import org.junit.Test;

import grondag.exotic_matter.model.ModShapes;
import grondag.exotic_matter.model.ModelState;
import grondag.exotic_matter.model.TerrainMeshFactory;
import grondag.exotic_matter.model.TerrainState;

public class TerrainPerf
{

    @Test
    public void test()
    {
        Random r = new Random(1);
        TerrainMeshFactory mesher = new TerrainMeshFactory();
        final int range = TerrainState.MAX_HEIGHT - TerrainState.MIN_HEIGHT + 1;
         
        ExoticMatter.INSTANCE.info("Warm up / single thread runs...");
        for(int i = 0; i < 400000; i++)
        {
            boolean  isFiller = r.nextBoolean();
            int level = r.nextInt(TerrainState.BLOCK_LEVELS_INT);
            ModelState modelState = new ModelState();
            modelState.setShape(isFiller ? ModShapes.TERRAIN_FILLER : ModShapes.TERRAIN_HEIGHT);
            int[] sides = {TerrainState.MIN_HEIGHT + r.nextInt(range), TerrainState.MIN_HEIGHT + r.nextInt(range), TerrainState.MIN_HEIGHT + r.nextInt(range), TerrainState.MIN_HEIGHT + r.nextInt(range)};
            int[] corners = {TerrainState.MIN_HEIGHT + r.nextInt(range), TerrainState.MIN_HEIGHT + r.nextInt(range), TerrainState.MIN_HEIGHT + r.nextInt(range), TerrainState.MIN_HEIGHT + r.nextInt(range)};
            TerrainState flowState = new TerrainState(level, sides, corners, isFiller ? r.nextInt(2) + 1: 0);
            modelState.setTerrainState(flowState);
            mesher.getShapeQuads(modelState);
        }
        
        ExoticMatter.INSTANCE.info(" ");
        ExoticMatter.INSTANCE.info("Multi-thread runs...");
    }

}