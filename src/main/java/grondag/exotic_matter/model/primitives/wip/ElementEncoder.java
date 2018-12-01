package grondag.exotic_matter.model.primitives.wip;

public class ElementEncoder
{
    /**
     * Must be different for implementers that are meant to encode different things
     * and same for implementer that encode same thing. 
     * Each stream format will have exactly zero or one encoder of each type.
     */
    public final int encoderType;
    
    /**
     * If non-zero, {@link #bitLength()} must be zero.
     */
    public final int intLength;
    
    /**
     * If non-zero, {@link #intLength()} must be zero.
     */
    public final int bitLength;
    
    public final IEncoderFunction encoder;
    
    public final IDecoderFunction decoder;
    
    public ElementEncoder(EncoderTypes encoderType, int length, boolean isBits, IEncoderFunction encoder, IDecoderFunction decoder)
    {
        this.encoderType = encoderType.ordinal();
        this.intLength = isBits ? 0 : length;
        this.bitLength = isBits ? length : 0;
        this.encoder = encoder;
        this.decoder = decoder;
    }
}
