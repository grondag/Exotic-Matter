package grondag.exotic_matter.model.primitives.wip;

import grondag.exotic_matter.varia.BitPacker32;
import net.minecraft.util.EnumFacing;

public class PolyStreamFormat
{
    @SuppressWarnings("null")
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
    
    private static final BitPacker32<PolyStreamFormat>.IntElement VERTEX_COUNT = BITPACKER.createIntElement(128);
    
    public static int getVertexCount(int rawBits)
    {
        return 3 + VERTEX_COUNT.getValue(rawBits);
    }
    
    public static int setVertexCount(int rawBits, int vertexCount)
    {
        assert vertexCount > 2;
        assert vertexCount <= 3 + 127;
        return VERTEX_COUNT.setValue(vertexCount, rawBits);
    }
    
    private static final BitPacker32<PolyStreamFormat>.EnumElement<EnumFacing> NOMINAL_FACE = BITPACKER.createEnumElement(EnumFacing.class);
    
    public static EnumFacing getNominalFace(int rawBits)
    {
        return NOMINAL_FACE.getValue(rawBits);
    }
    
    public static int setNominalFace(int rawBits, EnumFacing face)
    {
        return NOMINAL_FACE.setValue(face, rawBits);
    }
    
    public static final int FACE_NORMAL_FORMAT_VIRTUAL = 0;
    public static final int FACE_NORMAL_FORMAT_CACHED = 1;
    public static final int FACE_NORMAL_FORMAT_QUANTIZED = 2;
    public static final int FACE_NORMAL_FORMAT_NOMINAL = 3;
    
    private static final BitPacker32<PolyStreamFormat>.IntElement FACE_NORMAL_FORMAT = BITPACKER.createIntElement(4);
    
    public static int getFaceNormalFormat(int rawBits)
    {
        return FACE_NORMAL_FORMAT.getValue(rawBits);
    }
    
    public static int setFaceNormalFormat(int rawBits, int format)
    {
        return FACE_NORMAL_FORMAT.setValue(format, rawBits);
    }
    
    private static final BitPacker32<PolyStreamFormat>.BooleanElement HALF_PRECISION_POLY_UV = BITPACKER.createBooleanElement();
    
    public static boolean isHalfPrecisionPolyUV(int rawBits)
    {
        return HALF_PRECISION_POLY_UV.getValue(rawBits);
    }
    
    public static int setHalfPrecisionPolyUV(int rawBits, boolean isHalf)
    {
        return HALF_PRECISION_POLY_UV.setValue(isHalf, rawBits);
    }
    
    private static final BitPacker32<PolyStreamFormat>.IntElement LAYER_COUNT = BITPACKER.createIntElement(3);
    
    public static int getLayerCount(int rawBits)
    {
        return LAYER_COUNT.getValue(rawBits);
    }
    
    public static int setLayerCount(int rawBits, int layerCount)
    {
        return LAYER_COUNT.setValue(layerCount, rawBits);
    }
    
    /** all vertices are white */
    public static final int VERTEX_COLOR_WHITE = 0;
    /** all vertices have same color, irrespective of layer */
    public static final int VERTEX_COLOR_SAME = 1;
    /** all vertices in a layer share same color, layers are different*/
    public static final int VERTEX_COLOR_SAME_BY_LAYER = 2;
    /** assign vertex color to each layer/vertex */
    public static final int VERTEX_COLOR_PER_VERTEX_LAYER = 3;
    
    private static final BitPacker32<PolyStreamFormat>.IntElement VERTEX_COLOR_FORMAT = BITPACKER.createIntElement(4);
    
    public static int getVertexColorFormat(int rawBits)
    {
        return VERTEX_COLOR_FORMAT.getValue(rawBits);
    }
    
    public static int setVertexColorFormat(int rawBits, int format)
    {
        return VERTEX_COLOR_FORMAT.setValue(format, rawBits);
    }
    
    private static final BitPacker32<PolyStreamFormat>.BooleanElement QUANTIZED_POS = BITPACKER.createBooleanElement();
    
    public static boolean isQuantizedPos(int rawBits)
    {
        return QUANTIZED_POS.getValue(rawBits);
    }
    
    public static int setQuantizedPos(int rawBits, boolean isQuantized)
    {
        return QUANTIZED_POS.setValue(isQuantized, rawBits);
    }
    
    /** use face normal as vertex normals */
    public static final int VERTEX_NORMAL_FACE = 0;
    /** quantized normals */
    public static final int VERTEX_NORMAL_QUANTIZED = 1;
    /** full precision normals */
    public static final int VERTEX_NORMAL_REGULAR = 2;
    
    private static final BitPacker32<PolyStreamFormat>.IntElement VERTEX_NORMAL_FORMAT = BITPACKER.createIntElement(3);
    
    public static int getVertexNormalFormat(int rawBits)
    {
        return VERTEX_NORMAL_FORMAT.getValue(rawBits);
    }
    
    public static int setVertexNormalFormat(int rawBits, int format)
    {
        return VERTEX_NORMAL_FORMAT.setValue(format, rawBits);
    }
    
    /** all layers have same UV */
    public static final int VERTEX_UV_SAME = 0;
    /** different UV in each layer */
    public static final int VERTEX_UV_BY_LAYER = 1;
    /** all layers have same UV, half precision */
    public static final int VERTEX_UV_SAME_HALF = 2;
    /** different UV in each layer, half precision */
    public static final int VERTEX_UV_BY_LAYER_HALF = 3;
    
    private static final BitPacker32<PolyStreamFormat>.IntElement VERTEX_UV_FORMAT = BITPACKER.createIntElement(4);
    
    public static int getVertexUVFormat(int rawBits)
    {
        return VERTEX_UV_FORMAT.getValue(rawBits);
    }
    
    public static int setVertexUVFormat(int rawBits, int format)
    {
        return VERTEX_UV_FORMAT.setValue(format, rawBits);
    }
    
    /** values are zero for every vertex */
    public static final int VERTEX_GLOW_NONE = 0;
    /** all vertices have same non-zero value */
    public static final int VERTEX_GLOW_SAME = 1;
    /** each vertex has a glow value */
    public static final int VERTEX_GLOW_PER_VERTEX = 2;
    
    private static final BitPacker32<PolyStreamFormat>.IntElement VERTEX_GLOW_FORMAT = BITPACKER.createIntElement(3);
    
    public static int getVertexGlowFormat(int rawBits)
    {
        return VERTEX_GLOW_FORMAT.getValue(rawBits);
    }
    
    public static int setVertexGlowFormat(int rawBits, int format)
    {
        return VERTEX_GLOW_FORMAT.setValue(format, rawBits);
    }
    
    private static final BitPacker32<PolyStreamFormat>.BooleanElement IS_MARKED = BITPACKER.createBooleanElement();
    
    public static boolean isMarked(int rawBits)
    {
        return IS_MARKED.getValue(rawBits);
    }
    
    public static int setMarked(int rawBits, boolean isMarked)
    {
        return IS_MARKED.setValue(isMarked, rawBits);
    }
    
    private static final BitPacker32<PolyStreamFormat>.BooleanElement IS_DELETED = BITPACKER.createBooleanElement();
    
    public static boolean isDeleted(int rawBits)
    {
        return IS_DELETED.getValue(rawBits);
    }
    
    public static int setDeleted(int rawBits, boolean isDeleted)
    {
        return IS_DELETED.setValue(isDeleted, rawBits);
    }
    
//    isMutable   2   1       poly    yes yes
//    vertexCount 128 7       vertex  no  no
//    nominalFace 6   3       poly    no  no
    
//    faceNormal  4   2   Dynamic/Cached/Quantized/Nominal    poly    yes no
//    uvFormat    2   1   full/half   layer   yes no
    
//    layerCount  3   2   if mutable, how many layers are used    layer   yes yes
//    vertexColor 4   2   white/same/same by layer/each   vertex layer    yes yes
//    vertexPos   2   1   Regular/Quantized   vertex  no  yes
//    vertexNormals   3   2   Face/Regular/Quantized  vertex  no  yes
//    vertexUV    4   2   Same/Per Layer/Same Half/Per Layer Half vertex layer    no  yes
    
//    vertexGlow  3   2   None/Same/Per Vertex    vertex layer    no  no
//    isMarked    2   1   stream metadata poly    no  no
//    isDeleted   2   1   stream metadata poly    no  no
    
    private final static int POLY_FORMAT_MASK;
    private final static int POLY_FORMAT_SHIFT;
    public final static int POLY_FORMAT_COUNT;
    
    private final static int VERTEX_FORMAT_MASK;
    private final static int VERTEX_FORMAT_SHIFT;
    public final static int VERTEX_FORMAT_COUNT;
    
    static
    {
        final int polyMask = FACE_NORMAL_FORMAT.comparisonMask()
                | HALF_PRECISION_POLY_UV.comparisonMask()
                | LAYER_COUNT.comparisonMask()
                | VERTEX_COLOR_FORMAT.comparisonMask();
        
        POLY_FORMAT_SHIFT = Integer.numberOfTrailingZeros(polyMask);
        POLY_FORMAT_MASK = polyMask >> POLY_FORMAT_SHIFT;
        POLY_FORMAT_COUNT = POLY_FORMAT_MASK + 1;
        
        final int vertexMask = LAYER_COUNT.comparisonMask()
                | VERTEX_COLOR_FORMAT.comparisonMask()
                | QUANTIZED_POS.comparisonMask()
                | VERTEX_NORMAL_FORMAT.comparisonMask()
                | VERTEX_UV_FORMAT.comparisonMask();
        
        VERTEX_FORMAT_SHIFT = Integer.numberOfTrailingZeros(vertexMask);
        VERTEX_FORMAT_MASK = vertexMask >> POLY_FORMAT_SHIFT;
        VERTEX_FORMAT_COUNT = VERTEX_FORMAT_MASK + 1;
    }
    
    public static int polyFormatKey(int rawBits)
    {
        return (rawBits >> POLY_FORMAT_SHIFT) & POLY_FORMAT_MASK;
    }
    
    public static int vertexFormatKey(int rawBits)
    {
        return (rawBits >> VERTEX_FORMAT_SHIFT) & VERTEX_FORMAT_MASK;
    }
}
