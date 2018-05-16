package grondag.exotic_matter;


import org.junit.Test;

import grondag.exotic_matter.concurrency.SimpleConcurrentList;
import grondag.exotic_matter.varia.SimpleUnorderedArrayList;

public class SimpleConcurrentListTest
{

    @Test
    public void test()
    {
        SimpleConcurrentList<Integer> list = SimpleConcurrentList.create(Integer.class, false, "", null);
        SimpleUnorderedArrayList<Integer> inputs = new SimpleUnorderedArrayList<Integer>();
        
        for(int i = 0; i < 10000000; i++)
        {
            inputs.add(i);
        }
        
        inputs.parallelStream().forEach(i -> list.add(i));
        
        assert(list.size() == inputs.size());
        
        list.addAll(inputs);
        
        assert(list.size() == inputs.size() * 2);
    }

   

}