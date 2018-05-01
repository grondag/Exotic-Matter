package grondag.exotic_matter.varia;

import java.util.Random;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArraySet;

/**
 * Generates tileable 2d blue noise with integer coordinates using Bridson algorithm for Poisson disk sampling.
 * Representation is sparse so can hold a fairly large tile without too much memory 
 * usage but generation time will be correspondingly longer.  (Relative to size ^ 2)
 */
public class BlueNoise
{
    private final IntArraySet points = new IntArraySet();
    public final int size;
    public final int minSpacing;
    
    public static BlueNoise create(int size, int minSpacing, int seed)
    {
        return new BlueNoise(size, minSpacing, seed);
    }
    
    private BlueNoise(int size, int minSpacing, int seed)
    {
        this.size = size;
        this.minSpacing = minSpacing;
        this.generate(seed);
    }

    private class Point
    {
        private final int x;
        private final int y;
        
        private Point(int x, int y)
        {
            this.x = x;
            this.y = y;
        }
        
        private Point(int index)
        {
            this.x = xFromIndex(index);
            this.y = yFromIndex(index);
        }
        
        private int index()
        {
            return indexOf(x, y);
        }
    }
    
    
    private void generate(int seed)
    {
        final Random r = new Random(seed);
        
        IntArrayList active = new IntArrayList();
        
        {
            final int first = indexOf(r.nextInt(), r.nextInt());
            active.add(first);
            points.add(first);
        }
        
        while(! active.isEmpty())
        {
            final Point subject = new Point(active.removeInt(r.nextInt(active.size())));
            for(int i = 0; i < 60; i++)
            {
                Point trial = generatePointAround(subject, r);
                if(pointIsValid(trial))
                {
                    active.add(subject.index());
                    final int trialIndex = trial.index();
                    active.add(trialIndex);
                    points.add(trialIndex);
                    break;
                }
            }
        }
    }
    
    private boolean pointIsValid(Point p)
    {
        for(int j = -minSpacing; j <= minSpacing; j++)
        {
            for(int k = -minSpacing; k <= minSpacing; k++)
            {
                if(isSet(p.x + j, p.y + k) && Math.sqrt(j * j + k * k) <= minSpacing)
                {
                    return false;
                }
            }
        }
        return true;
    }
    
    private Point generatePointAround(Point p, Random r)
    { 
        int radius = minSpacing +  r.nextInt(minSpacing + 1);
        double angle = 2 * Math.PI * r.nextDouble();
        int newX = (int) Math.round(p.x + radius * Math.cos(angle));
        int newY = (int) Math.round(p.y + radius * Math.sin(angle));
        return new Point(newX, newY);
    }
    
    /** 
     * Coordinates are wrapped to size of noise.
     */
    public boolean isSet(int x, int y)
    {
        return this.points.contains(indexOf(x, y));
    }
    
    public void set(int x, int y)
    {
        this.points.add(indexOf(x, y));
    }
    
    public void clear(int x, int y)
    {
        this.points.rem(indexOf(x, y));
    }
    
    private int xFromIndex(int index)
    {
        return index & 0xFFFF;
    }
    
    private int yFromIndex(int index)
    {
        return index >> 16;
    }
    
    private int indexOf(int x, int y)
    {
        x = (x % size + size) % size;
        y = (y % size + size) % size;
        return (y << 16) | x;
    }
}
