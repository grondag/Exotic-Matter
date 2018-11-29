package grondag.exotic_matter.model.primitives.stream;

import grondag.exotic_matter.model.primitives.polygon.IMutablePolygon;
import grondag.exotic_matter.model.primitives.polygon.IPolygon;

public enum VertexNormalEncoding
{
    /**
     * Vertices don't have normals - use face normal instead.
     */
    NONE(0, 
            (stream, address, poly) -> 
            {
                
            },
            (stream, address, mPoly) -> 
            {
                
            }), 
    
    /**
     * Each vertex has 3 float normal values.
     */
    FLOATS(3, 
            (stream, address, poly) ->
            {
                
            },
            (stream, address, mPoly) -> 
            {
                
            }),
    
    /**
     * Each vertex has normal encoded to a single int value.
     */
    QUANTIZED(1, 
            (stream, address, poly) -> 
            {
                
            },
            (stream, address, mPoly) -> 
            {
                
            });
    
    public final ElementEncoder encoder;
    
    private VertexNormalEncoding(int length, IEncoderFunction encoder, IDecoderFunction decoder)
    {
        this.encoder = new ElementEncoder(EncoderTypes.VERTEX_NORMAL, length, false, encoder, decoder);
    }

}
