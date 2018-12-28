package grondag.exotic_matter.varia.intstream;

import org.junit.jupiter.api.Test;

class Float3Int1MapTest
{

    @Test
    void test()
    {
        Float3Int1Map map = Float3Int1Map.claim();
        
        assert map.isEmpty();
        assert map.size() == 0;
        
        Float3Int1MapCursor writer = map.writer();
        
        writer.x = 1f;
        writer.y = 2.415f;
        writer.z = -5.123f;
        writer.i = 4577;
        
        assert map.find() == false;
        
        map.put();
        
        assert map.find() == true;
        
        Float3Int1MapCursor reader = map.reader();
        
        assert reader.x == 1f;
        assert reader.y == 2.415f;
        assert reader.z == -5.123f;
        assert reader.i == 4577;
        
        writer.i = 89501;
        
        assert map.put() == false;
        
        writer.x = 2f;
        
        assert map.put();
        assert map.size() == 2;
        
        assert reader.origin();
        float lastX = reader.x;
        assert reader.i == 89501;
        assert reader.next();
        assert reader.i == 89501;
        assert reader.x != lastX;
        
        assert ! reader.next();
        
        map.release();
    }

}
