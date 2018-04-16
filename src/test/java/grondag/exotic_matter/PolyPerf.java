package grondag.exotic_matter;

import java.util.Random;

import javax.vecmath.Vector3f;

import org.junit.Test;

import grondag.exotic_matter.render.IMutablePolygon;
import grondag.exotic_matter.render.Poly;
import jline.internal.Log;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

public class PolyPerf
{

    @Test
    public void test()
    {
        Vec3d point;
        Vec3d direction;
        IMutablePolygon quad;
        
        Random r = new Random(9);
        
        final int RUNS = 100000;
        
        boolean results[] = new boolean[RUNS];
        
        long start = System.nanoTime();
        
        for(int i = 0; i < RUNS; i++)
        {
            EnumFacing face = EnumFacing.HORIZONTALS[r.nextInt(4)];
            float x = r.nextFloat();
            float y = r.nextFloat();
            quad = Poly.mutable(4).setupFaceQuad(face, x, y, x + r.nextFloat(), y + r.nextFloat(), r.nextFloat(), EnumFacing.UP);
            float px = r.nextFloat();
            float py = r.nextFloat();
            float pz = r.nextFloat();
            Vector3f ray = new Vector3f(r.nextFloat(), r.nextFloat(), r.nextFloat());
            ray.normalize();
            results[i] = quad.intersectsWithRaySlow(px, py, pz, ray.x, ray.y, ray.z); 
        }
        
        Log.info("Slow way ns each: " + (System.nanoTime() - start) / RUNS);
        
        r = new Random(9);
        start = System.nanoTime();
        int diffCount = 0;
        
        for(int i = 0; i < RUNS; i++)
        {
            EnumFacing face = EnumFacing.HORIZONTALS[r.nextInt(4)];
            float x = r.nextFloat();
            float y = r.nextFloat();
            quad = Poly.mutable(4).setupFaceQuad(face, x, y, x + r.nextFloat(), y + r.nextFloat(), r.nextFloat(), EnumFacing.UP);
            float px = r.nextFloat();
            float py = r.nextFloat();
            float pz = r.nextFloat();
            Vector3f ray = new Vector3f(r.nextFloat(), r.nextFloat(), r.nextFloat());
            ray.normalize();
            if(results[i] != quad.intersectsWithRay(px, py, pz, ray.x, ray.y, ray.z))
            {
                diffCount++;
                Log.info("===========================================================");
                Log.info("Quad " + quad.toString());
                Log.info("point %f, %f, %f", px, py, pz);
                Log.info("intersection " + quad.intersectionOfRayWithPlane(px, py, pz, ray.x, ray.y, ray.z).toString());
                Log.info("direction %f, %f, %f", ray.x, ray.y, ray.z);
                Log.info("slow " + quad.intersectsWithRay(px, py, pz, ray.x, ray.y, ray.z));
                Log.info("fast " + quad.intersectsWithRaySlow(px, py, pz, ray.x, ray.y, ray.z));
            }
//            assert(results[i] == quad.intersectsWithRayFast(point, direction)); 
        }
        
        Log.info("Result Diff % " + diffCount * 100f / RUNS);
        Log.info("Fast way ns each: " + (System.nanoTime() - start) / RUNS);
    }

}