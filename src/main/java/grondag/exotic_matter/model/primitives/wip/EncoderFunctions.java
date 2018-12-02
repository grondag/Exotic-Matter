package grondag.exotic_matter.model.primitives.wip;

import grondag.exotic_matter.varia.NormalQuantizer;

public abstract class EncoderFunctions
{
    public static final int BAD_ADDRESS = Integer.MIN_VALUE;
    
    @FunctionalInterface
    public static interface IntGetterBase
    {
        int get(IIntStream stream, int baseAddress);
    }
    
    @FunctionalInterface
    public static interface IntSetterBase
    {
        void set(IIntStream stream, int baseAddress, int value);
    }
    
    @FunctionalInterface
    public static interface FloatGetterBase
    {
        float get(IIntStream stream, int baseAddress);
    }
    
    @FunctionalInterface
    public static interface FloatSetterBase
    {
        void set(IIntStream stream, int baseAddress, float value);
    }
    
    @FunctionalInterface
    public static interface FloatSetterBase3
    {
        void set(IIntStream stream, int baseAddress, float x, float y, float z);
    }
    
    public static final IntGetterBase GET_INT_FAIL = (stream, address) -> {throw new UnsupportedOperationException();};
    public static final IntGetterBase GET_INT = (stream, address) -> stream.get(address);

    public static final IntGetterBase GET_INT_WHITE = (stream, address) -> 0xFFFFFFFF;
    
    public static final IntGetterBase GET_HALF_INT_LOW = (stream, address) -> stream.get(address) & 0xFFFF;
    public static final IntGetterBase GET_HALF_INT_HIGH = (stream, address) -> (stream.get(address) >>> 16) & 0xFFFF;
    
    public static final IntSetterBase SET_INT_FAIL = (stream, address, value) -> {throw new UnsupportedOperationException();};
    public static final IntSetterBase SET_INT = (stream, address, value) -> stream.set(address, value);
    
    public static final IntSetterBase SET_HALF_INT_LOW = (stream, address, value) 
            -> stream.set(address, (stream.get(address) & 0xFFFF0000) | (value & 0xFFFF));
    public static final IntSetterBase SET_HALF_INT_HIGH = (stream, address, value) 
            -> stream.set(address, (stream.get(address) & 0x0000FFFF) | (value << 16));
    
    public static final FloatGetterBase GET_FLOAT_FAIL = (stream, address) -> {throw new UnsupportedOperationException();};
    public static final FloatGetterBase GET_FLOAT = (stream, address) -> Float.intBitsToFloat(stream.get(address));
    
    public static final FloatSetterBase SET_FLOAT_FAIL = (stream, address, value) -> {throw new UnsupportedOperationException();};
    public static final FloatSetterBase SET_FLOAT = (stream, address, value) -> stream.set(address, Float.floatToRawIntBits(value));
    
    public static final FloatSetterBase3 SET_FLOAT3_FAIL = (stream, address, x, y, z) -> {throw new UnsupportedOperationException();};
    public static final FloatSetterBase3 SET_FLOAT3 = (stream, address, x, y, z) -> 
    {
        stream.set(address, Float.floatToRawIntBits(x));
        stream.set(address, Float.floatToRawIntBits(x + 1));
        stream.set(address, Float.floatToRawIntBits(x + 2));
    };
    
    public static final FloatGetterBase GET_NORMAL_X_QUANTIZED = (stream, address) -> NormalQuantizer.unpackX(stream.get(address));
    public static final FloatGetterBase GET_NORMAL_Y_QUANTIZED = (stream, address) -> NormalQuantizer.unpackY(stream.get(address));
    public static final FloatGetterBase GET_NORMAL_Z_QUANTIZED = (stream, address) -> NormalQuantizer.unpackZ(stream.get(address));
    public static final FloatSetterBase3 SET_NORMAL_XYZ_QUANTIZED = (stream, address, x, y, z) 
            -> stream.set(address, NormalQuantizer.pack(x, y, z));

}
