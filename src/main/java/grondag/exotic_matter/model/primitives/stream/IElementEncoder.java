package grondag.exotic_matter.model.primitives.stream;

import grondag.exotic_matter.model.primitives.polygon.IMutablePolygon;
import grondag.exotic_matter.model.primitives.polygon.IPolygon;

public interface IElementEncoder
{
    /**
     * Must be different for implementers that are meant to encode different things
     * and same for implementer that encode same thing. 
     * Each stream format will have exactly zero or one encoder of each type.
     */
    int encoderType();
    
    /**
     * If non-zero, {@link #bitLength()} must be zero.
     */
    int intLenght();
    
    /**
     * If non-zero, {@link #intLength()} must be zero.
     */
    int bitLength();
    
    IEncoderFunction encoder();
    
    IDecoderFunction decoder();
}
