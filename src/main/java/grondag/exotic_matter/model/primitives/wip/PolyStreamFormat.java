package grondag.exotic_matter.model.primitives.wip;

import grondag.exotic_matter.varia.BitPacker32;

public class PolyStreamFormat
{
    private static final BitPacker32<PolyStreamFormat> BITPACKER = new BitPacker32<PolyStreamFormat>(null, null);

    private static final BitPacker32<PolyStreamFormat>.BooleanElement IS_MUTABLE = BITPACKER.createBooleanElement();
    
    public static boolean isMutable(int rawBits)
    {
        return IS_MUTABLE.getValue(rawBits);
    }
    
    public static int setMutable(int rawBits, boolean isMarked)
    {
        return IS_MUTABLE.setValue(isMarked, rawBits);
    }
    
}
