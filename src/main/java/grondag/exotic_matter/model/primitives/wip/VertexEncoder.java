package grondag.exotic_matter.model.primitives.wip;

import static grondag.exotic_matter.model.primitives.wip.EncoderFunctions.BAD_ADDRESS;
import static grondag.exotic_matter.model.primitives.wip.EncoderFunctions.GET_FLOAT;
import static grondag.exotic_matter.model.primitives.wip.EncoderFunctions.GET_FLOAT_FAIL;
import static grondag.exotic_matter.model.primitives.wip.EncoderFunctions.GET_INT;
import static grondag.exotic_matter.model.primitives.wip.EncoderFunctions.GET_INT_FAIL;
import static grondag.exotic_matter.model.primitives.wip.EncoderFunctions.GET_NORMAL_X_QUANTIZED;
import static grondag.exotic_matter.model.primitives.wip.EncoderFunctions.GET_NORMAL_Y_QUANTIZED;
import static grondag.exotic_matter.model.primitives.wip.EncoderFunctions.GET_NORMAL_Z_QUANTIZED;
import static grondag.exotic_matter.model.primitives.wip.EncoderFunctions.SET_FLOAT;
import static grondag.exotic_matter.model.primitives.wip.EncoderFunctions.SET_FLOAT2;
import static grondag.exotic_matter.model.primitives.wip.EncoderFunctions.SET_FLOAT2_FAIL;
import static grondag.exotic_matter.model.primitives.wip.EncoderFunctions.SET_FLOAT3;
import static grondag.exotic_matter.model.primitives.wip.EncoderFunctions.SET_FLOAT3_FAIL;
import static grondag.exotic_matter.model.primitives.wip.EncoderFunctions.SET_FLOAT_FAIL;
import static grondag.exotic_matter.model.primitives.wip.EncoderFunctions.SET_INT;
import static grondag.exotic_matter.model.primitives.wip.EncoderFunctions.SET_INT_FAIL;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.VERTEX_COLOR_PER_VERTEX_LAYER;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.VERTEX_FORMAT_COUNT;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.VERTEX_FORMAT_SHIFT;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.VERTEX_NORMAL_FACE;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.VERTEX_NORMAL_QUANTIZED;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.VERTEX_NORMAL_REGULAR;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.VERTEX_UV_BY_LAYER;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.getLayerCount;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.getVertexColorFormat;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.getVertexNormalFormat;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.isMutable;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.setLayerCount;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.setQuantizedPos;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.setVertexColorFormat;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.setVertexNormalFormat;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.setVertexUVFormat;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.vertexFormatKey;

import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.model.primitives.wip.EncoderFunctions.FloatGetter;
import grondag.exotic_matter.model.primitives.wip.EncoderFunctions.FloatSetter;
import grondag.exotic_matter.model.primitives.wip.EncoderFunctions.FloatSetter2;
import grondag.exotic_matter.model.primitives.wip.EncoderFunctions.FloatSetter3;
import grondag.exotic_matter.model.primitives.wip.EncoderFunctions.IntGetter;
import grondag.exotic_matter.model.primitives.wip.EncoderFunctions.IntSetter;
import grondag.exotic_matter.varia.intstream.IIntStream;

public class VertexEncoder
{
    private static final VertexEncoder[] ENCODERS = new VertexEncoder[VERTEX_FORMAT_COUNT];
    
    private static final VertexEncoder MUTABLE;
    
    static
    {
        for(int i = 0; i < VERTEX_FORMAT_COUNT; i++)
            ENCODERS[i] = new VertexEncoder(i << VERTEX_FORMAT_SHIFT);
        
        int mutableFormat = 0;
        mutableFormat = setLayerCount(mutableFormat, 3);
        mutableFormat = setVertexColorFormat(mutableFormat, VERTEX_COLOR_PER_VERTEX_LAYER);
        mutableFormat = setQuantizedPos(mutableFormat, false);
        mutableFormat = setVertexNormalFormat(mutableFormat, VERTEX_NORMAL_REGULAR);
        mutableFormat = setVertexUVFormat(mutableFormat, VERTEX_UV_BY_LAYER);
        
        assert getLayerCount(mutableFormat) == 3;
        assert getVertexColorFormat(mutableFormat) == VERTEX_COLOR_PER_VERTEX_LAYER;
        
        MUTABLE = ENCODERS[vertexFormatKey(mutableFormat)];
        
        assert MUTABLE.hasColor();
        assert MUTABLE.hasNormals();
    }
    
    /**
     * All mutable formats have same full-feature binary format for data-compatibility with format changes
     */
    public static VertexEncoder get(int format)
    {
        return isMutable(format) ? MUTABLE : ENCODERS[vertexFormatKey(format)];
    }

    private final int vertexStride;
    private final boolean hasNormals;
    private final boolean hasColor;
    
    private final FloatGetter getPosX;
    private final FloatGetter getPosY;
    private final FloatGetter getPosZ;
    private final FloatSetter3 setPosXYZ;
    private final int offsetPosX;
    private final int offsetPosY;
    private final int offsetPosZ;
    
    private final FloatGetter getNormalX;
    private final FloatGetter getNormalY;
    private final FloatGetter getNormalZ;
    private final FloatSetter3 setNormalXYZ;
    private final int offsetNormalX;
    private final int offsetNormalY;
    private final int offsetNormalZ;
    
    private final IntGetter getColor0;
    private final IntGetter getColor1;
    private final IntGetter getColor2;
    private final IntSetter setColor0;
    private final IntSetter setColor1;
    private final IntSetter setColor2;
    private final int offsetColor0;
    private final int offsetColor1;
    private final int offsetColor2;
    
    private final FloatGetter getU0;
    private final FloatGetter getV0;
    private final FloatSetter setU0;
    private final FloatSetter setV0;
    private final FloatSetter2 setUV0;
    private final int offsetU0;
    private final int offsetV0;
    
    private final FloatGetter getU1;
    private final FloatGetter getV1;
    private final FloatSetter setU1;
    private final FloatSetter setV1;
    private final FloatSetter2 setUV1;
    private final int offsetU1;
    private final int offsetV1;
    
    private final FloatGetter getU2;
    private final FloatGetter getV2;
    private final FloatSetter setU2;
    private final FloatSetter setV2;
    private final FloatSetter2 setUV2;
    private final int offsetU2;
    private final int offsetV2;
    
    private VertexEncoder(int format)
    {
        int offset = 0;
        
        // PERF: quantize position
        offsetPosX = offset++;
        offsetPosY = offset++;
        offsetPosZ = offset++;
        getPosX = GET_FLOAT;
        getPosY = GET_FLOAT;
        getPosZ = GET_FLOAT;
        setPosXYZ = SET_FLOAT3;
        
        switch(getVertexNormalFormat(format))
        {
            case VERTEX_NORMAL_QUANTIZED:
                hasNormals = true;
                getNormalX = GET_NORMAL_X_QUANTIZED;
                getNormalY = GET_NORMAL_Y_QUANTIZED;
                getNormalZ = GET_NORMAL_Z_QUANTIZED;
                setNormalXYZ = SET_FLOAT3_FAIL;
                offsetNormalX = offset++;
                offsetNormalY = offsetNormalX;
                offsetNormalZ = offsetNormalX;
                break;
                
            case VERTEX_NORMAL_REGULAR:
                hasNormals = true;
                getNormalX = GET_FLOAT;
                getNormalY = GET_FLOAT;
                getNormalZ = GET_FLOAT;
                setNormalXYZ = SET_FLOAT3;
                offsetNormalX = offset++;
                offsetNormalY = offset++;
                offsetNormalZ = offset++;
                break;
                
            default:
            case VERTEX_NORMAL_FACE:
                hasNormals = false;
                getNormalX = GET_FLOAT_FAIL;
                getNormalY = GET_FLOAT_FAIL;
                getNormalZ = GET_FLOAT_FAIL;
                setNormalXYZ = SET_FLOAT3_FAIL;
                offsetNormalX = BAD_ADDRESS;
                offsetNormalY = BAD_ADDRESS;
                offsetNormalZ = BAD_ADDRESS;
                break;
        }
        

        
        final int layerCount = getLayerCount(format);
        
        // PERF: quantize UV
        getU0 = GET_FLOAT;
        getV0 = GET_FLOAT;
        setU0 = SET_FLOAT;
        setV0 = SET_FLOAT;
        setUV0 = SET_FLOAT2;
        offsetU0 = offset++;
        offsetV0 = offset++;
        
        getU1 = layerCount > 1 ? GET_FLOAT : GET_FLOAT_FAIL;
        getV1 = layerCount > 1 ? GET_FLOAT : GET_FLOAT_FAIL;
        setU1 = layerCount > 1 ? SET_FLOAT : SET_FLOAT_FAIL;
        setV1 = layerCount > 1 ? SET_FLOAT : SET_FLOAT_FAIL;
        setUV1 = layerCount > 1 ? SET_FLOAT2 : SET_FLOAT2_FAIL;
        offsetU1 = layerCount > 1 ? offset++ : BAD_ADDRESS;
        offsetV1 = layerCount > 1 ? offset++ : BAD_ADDRESS;
        
        getU2 = layerCount  == 3 ? GET_FLOAT : GET_FLOAT_FAIL;
        getV2 = layerCount  == 3 ? GET_FLOAT : GET_FLOAT_FAIL;
        setU2 = layerCount  == 3 ? SET_FLOAT : SET_FLOAT_FAIL;
        setV2 = layerCount  == 3 ? SET_FLOAT : SET_FLOAT_FAIL;
        setUV2 = layerCount  == 3 ? SET_FLOAT2 : SET_FLOAT2_FAIL;
        offsetU2 = layerCount  == 3 ? offset++ : BAD_ADDRESS;
        offsetV2 = layerCount  == 3 ? offset++ : BAD_ADDRESS;
        
        hasColor = getVertexColorFormat(format) == VERTEX_COLOR_PER_VERTEX_LAYER;
        if(hasColor)
        {
            getColor0 = GET_INT;
            getColor1 = layerCount > 1 ? GET_INT : GET_INT_FAIL;
            getColor2 = layerCount == 3 ? GET_INT : GET_INT_FAIL;
            setColor0 = SET_INT;
            setColor1 = layerCount > 1 ? SET_INT : SET_INT_FAIL;
            setColor2 = layerCount == 3 ? SET_INT : SET_INT_FAIL;
            offsetColor0 = offset++;
            offsetColor1 = layerCount > 1 ? offset++ : BAD_ADDRESS;
            offsetColor2 = layerCount == 3 ? offset++ : BAD_ADDRESS; 
        }
        else
        {
            getColor0 = GET_INT_FAIL;
            getColor1 = GET_INT_FAIL;
            getColor2 = GET_INT_FAIL;
            setColor0 = SET_INT_FAIL;
            setColor1 = SET_INT_FAIL;
            setColor2 = SET_INT_FAIL;
            offsetColor0 = BAD_ADDRESS;
            offsetColor1 = BAD_ADDRESS;
            offsetColor2 = BAD_ADDRESS;    
        }
        
        vertexStride = offset;
    }
    
    public int vertexStride()
    {
        return vertexStride;
    }

    public boolean hasNormals()
    {
        return hasNormals;
    }

    public Vec3f getVertexNormal(IIntStream stream, int vertexAddress, int vertexIndex)
    {
        if(!hasNormals)
            return null;
        
        final int base = vertexAddress + vertexIndex * vertexStride;
        Vec3f result = Vec3f.create(getNormalX.get(stream, base + offsetNormalX),
                getNormalY.get(stream, base + offsetNormalY),
                getNormalZ.get(stream, base + offsetNormalZ));
        
        return result == Vec3f.ZERO ? null : result;
    }

    // PERF: really used?
    public boolean hasVertexNormal(IIntStream stream, int vertexAddress, int vertexIndex)
    {
        final int base = vertexAddress + vertexIndex * vertexStride;
        return hasNormals && 
                (getNormalX.get(stream, base + offsetNormalX) != 0
                || getNormalY.get(stream, base + offsetNormalY) != 0
                || getNormalZ.get(stream, base + offsetNormalZ) != 0);
    }

    public float getVertexNormalX(IIntStream stream, int vertexAddress, int vertexIndex)
    {
        return getNormalX.get(stream, vertexAddress + vertexIndex * vertexStride + offsetNormalX);
    }

    public float getVertexNormalY(IIntStream stream, int vertexAddress, int vertexIndex)
    {
        return getNormalY.get(stream, vertexAddress + vertexIndex * vertexStride + offsetNormalY);
    }
    
    public float getVertexNormalZ(IIntStream stream, int vertexAddress, int vertexIndex)
    {
        return getNormalZ.get(stream, vertexAddress + vertexIndex * vertexStride + offsetNormalZ);
    }

    public void setVertexNormal(IIntStream stream, int vertexAddress, int vertexIndex, float normalX, float normalY, float normalZ)
    {
        setNormalXYZ.set(stream, vertexAddress + vertexIndex * vertexStride + offsetNormalX, normalX, normalY, normalZ);
    }
    
    public float getVertexX(IIntStream stream, int vertexAddress, int vertexIndex)
    {
        return getPosX.get(stream, vertexAddress + vertexIndex * vertexStride + offsetPosX);
    }
    
    public float getVertexY(IIntStream stream, int vertexAddress, int vertexIndex)
    {
        return getPosY.get(stream, vertexAddress + vertexIndex * vertexStride + offsetPosY);
    }
    
    public float getVertexZ(IIntStream stream, int vertexAddress, int vertexIndex)
    {
        return getPosZ.get(stream, vertexAddress + vertexIndex * vertexStride + offsetPosZ);
    }
    
    public void setVertexPos(IIntStream stream, int vertexAddress, int vertexIndex, float x, float y, float z)
    {
        setPosXYZ.set(stream, vertexAddress + vertexIndex * vertexStride + offsetPosX, x, y, z);
    }
    
    public boolean hasColor()
    {
        return hasColor;
    }

    public int getVertexColor(IIntStream stream, int vertexAddress, int layerIndex, int vertexIndex)
    {
        return layerIndex == 0 
                ? getColor0.get(stream, vertexAddress + vertexIndex * vertexStride + offsetColor0)
                : layerIndex == 1
                    ? getColor1.get(stream, vertexAddress + vertexIndex * vertexStride + offsetColor1)
                    : getColor2.get(stream, vertexAddress + vertexIndex * vertexStride + offsetColor2);
    }
    
    public void setVertexColor(IIntStream stream, int vertexAddress, int layerIndex, int vertexIndex, int color)
    {
        if(layerIndex == 0)
            setColor0.set(stream, vertexAddress + vertexIndex * vertexStride + offsetColor0, color);
        else if(layerIndex == 1)
            setColor1.set(stream, vertexAddress + vertexIndex * vertexStride + offsetColor1, color);
        else
            setColor2.set(stream, vertexAddress + vertexIndex * vertexStride + offsetColor2, color);
    }

    public float getVertexU(IIntStream stream, int vertexAddress, int layerIndex, int vertexIndex)
    {
        return layerIndex == 0 
                ? getU0.get(stream, vertexAddress + vertexIndex * vertexStride + offsetU0)
                : layerIndex == 1
                    ? getU1.get(stream, vertexAddress + vertexIndex * vertexStride + offsetU1)
                    : getU2.get(stream, vertexAddress + vertexIndex * vertexStride + offsetU2);
    }
    
    public void setVertexU(IIntStream stream, int vertexAddress, int layerIndex, int vertexIndex, float u)
    {
        if(layerIndex == 0)
            setU0.set(stream, vertexAddress + vertexIndex * vertexStride + offsetU0, u);
        else if(layerIndex == 1)
            setU1.set(stream, vertexAddress + vertexIndex * vertexStride + offsetU1, u);
        else
            setU2.set(stream, vertexAddress + vertexIndex * vertexStride + offsetU2, u);
    }
    
    public float getVertexV(IIntStream stream, int vertexAddress, int layerIndex, int vertexIndex)
    {
        return layerIndex == 0 
                ? getV0.get(stream, vertexAddress + vertexIndex * vertexStride + offsetV0)
                : layerIndex == 1
                    ? getV1.get(stream, vertexAddress + vertexIndex * vertexStride + offsetV1)
                    : getV2.get(stream, vertexAddress + vertexIndex * vertexStride + offsetV2);
    }
    
    public void setVertexV(IIntStream stream, int vertexAddress, int layerIndex, int vertexIndex, float v)
    {
        if(layerIndex == 0)
            setV0.set(stream, vertexAddress + vertexIndex * vertexStride + offsetV0, v);
        else if(layerIndex == 1)
            setV1.set(stream, vertexAddress + vertexIndex * vertexStride + offsetV1, v);
        else
            setV2.set(stream, vertexAddress + vertexIndex * vertexStride + offsetV2, v);
    }
    
    public void setVertexUV(IIntStream stream, int vertexAddress, int layerIndex, int vertexIndex, float u, float v)
    {
        if(layerIndex == 0)
            setUV0.set(stream, vertexAddress + vertexIndex * vertexStride + offsetU0, u, v);
        else if(layerIndex == 1)
            setUV1.set(stream, vertexAddress + vertexIndex * vertexStride + offsetU1, u, v);
        else
            setUV2.set(stream, vertexAddress + vertexIndex * vertexStride + offsetU2, u, v);
    }
}