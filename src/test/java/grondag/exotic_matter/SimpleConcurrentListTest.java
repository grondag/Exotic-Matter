package grondag.exotic_matter;


import org.junit.Test;

import grondag.exotic_matter.concurrency.SimpleConcurrentList;
import grondag.exotic_matter.varia.MicroTimer;
import grondag.exotic_matter.varia.SimpleUnorderedArrayList;

public class SimpleConcurrentListTest
{

    //b4 changes
//    [15:39:59] [main/INFO] [grondag.exotic_matter.ExoticMatter]: Avg SimpleConcurrentList duration = 519412600 ns, total duration = 519, total runs = 1
//    [15:40:00] [main/INFO] [grondag.exotic_matter.ExoticMatter]: Avg SimpleConcurrentList duration = 522582345 ns, total duration = 522, total runs = 1
//    [15:40:00] [main/INFO] [grondag.exotic_matter.ExoticMatter]: Avg SimpleConcurrentList duration = 508847926 ns, total duration = 508, total  runs = 1
   
    @Test
    public void test()
    {
        MicroTimer timer = new MicroTimer("SimpleConcurrentList", 1);
        
        SimpleUnorderedArrayList<Integer> inputs = new SimpleUnorderedArrayList<Integer>();
        
        for(int i = 0; i < 10000000; i++)
        {
            inputs.add(i);
        }

        System.out.println("Add without preallocation.");
        for(int i = 0; i < 13; i++)
        {
            timer.start();
            doTestAdd(inputs, 16);
            timer.stop();
            System.gc();
        }
        System.out.println(" ");
        
        System.out.println("Add single thread.");
        for(int i = 0; i < 13; i++)
        {
            timer.start();
            doTestAddSingle(inputs, 16);
            timer.stop();
            System.gc();
        }
        System.out.println(" ");
        
        System.out.println("Add single thread to non-concurrent simple list (for comparison)");
        for(int i = 0; i < 13; i++)
        {
            timer.start();
            doTestAddSingleNonConcurrent(inputs);
            timer.stop();
            System.gc();
        }System.out.println(" ");
        
        System.out.println("Add with preallocation.");
        for(int i = 0; i < 13; i++)
        {
            timer.start();
            doTestAdd(inputs, 10000000);
            timer.stop();
            System.gc();
        }
        System.out.println(" ");
        
        System.out.println("Add via array copy, no preallocation");
        for(int i = 0; i < 13; i++)
        {
            timer.start();
            doTestAddAll(inputs, 10000000);
            timer.stop();
            System.gc();
        }
    }

    
    private void doTestAdd(SimpleUnorderedArrayList<Integer> inputs, int startingCapacity)
    {
        SimpleConcurrentList<Integer> list = new SimpleConcurrentList<>(Integer.class, startingCapacity);
        
        inputs.parallelStream().forEach(i -> list.add(i));
        
        assert(list.size() == inputs.size());
    }
    
    private void doTestAddSingle(SimpleUnorderedArrayList<Integer> inputs, int startingCapacity)
    {
        SimpleConcurrentList<Integer> list = new SimpleConcurrentList<>(Integer.class, startingCapacity);
        
        inputs.stream().forEach(i -> list.add(i));
        
        assert(list.size() == inputs.size());
    }
    
    private void doTestAddSingleNonConcurrent(SimpleUnorderedArrayList<Integer> inputs)
    {
        SimpleUnorderedArrayList<Integer> list = new SimpleUnorderedArrayList<Integer>();
        
        inputs.stream().forEach(i -> list.add(i));
        
        assert(list.size() == inputs.size());
    }
    
    private void doTestAddAll(SimpleUnorderedArrayList<Integer> inputs, int startingCapacity)
    {
        SimpleConcurrentList<Integer> list = new SimpleConcurrentList<>(Integer.class, startingCapacity);
        
        list.addAll(inputs);
        
        assert(list.size() == inputs.size());
    }
   

}