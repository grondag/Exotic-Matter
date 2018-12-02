package grondag.exotic_matter.model.primitives.wip;

import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.FACE_NORMAL_FORMAT_FULL_MISSING;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.FACE_NORMAL_FORMAT_FULL_PRESENT;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.FACE_NORMAL_FORMAT_NOMINAL;
import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.FACE_NORMAL_FORMAT_QUANTIZED;

import static grondag.exotic_matter.model.primitives.wip.EncoderFunctions.*;

import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.varia.structures.ObjectHandle;

public class PolyEncoder
{
    private static final PolyEncoder[] ENCODERS = new PolyEncoder[PolyStreamFormat.POLY_FORMAT_COUNT];
    
    private static final ObjectHandle<String> textureHandler = new ObjectHandle<String>(String.class);
    
    static
    {
        for(int i = 0; i < PolyStreamFormat.POLY_FORMAT_COUNT; i++)
            ENCODERS[i] = new PolyEncoder(i);
    }
    
    public static PolyEncoder get(int format)
    {
        return ENCODERS[PolyStreamFormat.polyFormatKey(format)];
    }
    
   
            
    //////////////////////////////////////////////////////////////
    /// END STATIC MEMBERS
    //////////////////////////////////////////////////////////////
    
    private final FloatGetterBase getNormalX;
    private final int getNormalXOffset;
    private final FloatGetterBase getNormalY;
    private final int getNormalYOffset;
    private final FloatGetterBase getNormalZ;
    private final int getNormalZOffset;
    
    private final FloatSetterBase3 setNormalXYZ;
    
    private final FloatGetterBase getU0;
    private final FloatGetterBase getV0;
    private final FloatSetterBase setU0;
    private final FloatSetterBase setV0;
    private final int minUOffset0;
    private final int maxUOffset0;
    private final int minVOffset0;
    private final int maxVOffset0;
    
    private final FloatGetterBase getU1;
    private final FloatGetterBase getV1;
    private final FloatSetterBase setU1;
    private final FloatSetterBase setV1;
    private final int minUOffset1;
    private final int maxUOffset1;
    private final int minVOffset1;
    private final int maxVOffset1;
    
    private final FloatGetterBase getU2;
    private final FloatGetterBase getV2;
    private final FloatSetterBase setU2;
    private final FloatSetterBase setV2;
    private final int minUOffset2;
    private final int maxUOffset2;
    private final int minVOffset2;
    private final int maxVOffset2;
    
    private final int linkOffset;
    private final IntGetterBase getLink;
    private final IntSetterBase setLink;
    
    private final int tagOffset;
    private final IntGetterBase getTag;
    private final IntSetterBase setTag;
    
    private final IntGetterBase getTexture0;
    private final IntSetterBase setTexture0;
    private final IntGetterBase getTexture1;
    private final IntSetterBase setTexture1;
    private final IntGetterBase getTexture2;
    private final IntSetterBase setTexture2;
    /** holds first two textures */
    private final int textureOffset01;
    private final int textureOffset2;
    
    private final IntGetterBase getColor0;
    private final IntSetterBase setColor0;
    private final IntGetterBase getColor1;
    private final IntSetterBase setColor1;
    private final IntGetterBase getColor2;
    private final IntSetterBase setColor2;
    private final int colorOffset0;
    private final int colorOffset1;
    private final int colorOffset2;
    
    private final int stride;
    
    private PolyEncoder(int format)
    {
        final int baseOffset = 1 + StaticEncoder.INTEGER_WIDTH;
        int offset = baseOffset;
        
        switch(PolyStreamFormat.getFaceNormalFormat(format))
        {
            case  FACE_NORMAL_FORMAT_FULL_MISSING:
            case  FACE_NORMAL_FORMAT_FULL_PRESENT:
                getNormalXOffset = offset++; 
                getNormalX = GET_FLOAT;
                getNormalYOffset = offset++; 
                getNormalY = GET_FLOAT;
                getNormalZOffset = offset++; 
                getNormalZ = GET_FLOAT;
                setNormalXYZ = SET_FLOAT3;
                break;
                
            default:
            case  FACE_NORMAL_FORMAT_NOMINAL:
                getNormalXOffset = -1;
                getNormalX = GET_FLOAT_FAIL;
                getNormalYOffset = -1;
                getNormalY = GET_FLOAT_FAIL;
                getNormalZOffset = -1;
                getNormalZ = GET_FLOAT_FAIL;
                setNormalXYZ = SET_FLOAT3_FAIL;
                break;
                
            case  FACE_NORMAL_FORMAT_QUANTIZED:
                getNormalXOffset = offset++; 
                getNormalYOffset = getNormalXOffset; 
                getNormalZOffset = getNormalXOffset; 
                getNormalX = GET_NORMAL_X_QUANTIZED;
                getNormalY = GET_NORMAL_Y_QUANTIZED;
                getNormalZ = GET_NORMAL_Z_QUANTIZED;
                setNormalXYZ = SET_NORMAL_XYZ_QUANTIZED;
                break;
        }
        
        final float layerCount = PolyStreamFormat.getLayerCount(format);
        
        // PERF: implement packed UV encoding
        getU0 = GET_FLOAT;
        getV0 = GET_FLOAT;
        setU0 = SET_FLOAT;
        setV0 = SET_FLOAT;
        minUOffset0 = offset++;
        maxUOffset0 = offset++;
        minVOffset0 = offset++;
        maxVOffset0 = offset++;
        
        textureOffset01 = offset++;
        getTexture0 = GET_HALF_INT_LOW;
        setTexture0 = SET_HALF_INT_LOW;
        
        if(layerCount > 1)
        {
            getU1 = GET_FLOAT;
            getV1 = GET_FLOAT;
            setU1 = SET_FLOAT;
            setV1 = SET_FLOAT;
            minUOffset1 = offset++;
            maxUOffset1 = offset++;
            minVOffset1 = offset++;
            maxVOffset1 = offset++;
            
            getTexture1 = GET_HALF_INT_HIGH;
            setTexture1 = SET_HALF_INT_HIGH;
        }
        else
        {
            getU1 = GET_FLOAT_FAIL;
            getV1 = GET_FLOAT_FAIL;
            setU1 = SET_FLOAT_FAIL;
            setV1 = SET_FLOAT_FAIL;
            minUOffset1 = BAD_ADDRESS;
            maxUOffset1 = BAD_ADDRESS;
            minVOffset1 = BAD_ADDRESS;
            maxVOffset1 = BAD_ADDRESS;
            
            getTexture1 = GET_INT_FAIL;
            setTexture1 = SET_INT_FAIL;
        }
        
        if(layerCount == 3)
        {
            getU2 = GET_FLOAT;
            getV2 = GET_FLOAT;
            setU2 = SET_FLOAT;
            setV2 = SET_FLOAT;
            minUOffset2 = offset++;
            maxUOffset2 = offset++;
            minVOffset2 = offset++;
            maxVOffset2 = offset++;
            
            textureOffset2 = offset++;
            getTexture2 = GET_INT;
            setTexture2 = SET_INT;
        }
        else
        {
            getU2 = GET_FLOAT_FAIL;
            getV2 = GET_FLOAT_FAIL;
            setU2 = SET_FLOAT_FAIL;
            setV2 = SET_FLOAT_FAIL;
            minUOffset2 = BAD_ADDRESS;
            maxUOffset2 = BAD_ADDRESS;
            minVOffset2 = BAD_ADDRESS;
            maxVOffset2 = BAD_ADDRESS;
            
            textureOffset2 = BAD_ADDRESS;
            getTexture2 = GET_INT_FAIL;
            setTexture2 = SET_INT_FAIL;
        }
        
        if(PolyStreamFormat.isLinked(format))
        {
            linkOffset = offset++;
            getLink = GET_INT;
            setLink = SET_INT;
        }
        else
        {
            linkOffset = BAD_ADDRESS;
            getLink = GET_INT_FAIL;
            setLink = SET_INT_FAIL;
        }
        
        if(PolyStreamFormat.isTagged(format))
        {
            tagOffset = offset++;
            getTag = GET_INT;
            setTag = SET_INT;
        }
        else
        {
            tagOffset = BAD_ADDRESS;
            getTag = GET_INT_FAIL;
            setTag = SET_INT_FAIL;
        }
        
        switch(PolyStreamFormat.getVertexColorFormat(format))
        {
            case PolyStreamFormat.VERTEX_COLOR_WHITE:
                getColor0 = GET_INT_WHITE;
                getColor1 = layerCount > 1 ? GET_INT_WHITE : GET_INT_FAIL;
                getColor2 = layerCount > 1 ? GET_INT_WHITE : GET_INT_FAIL;
                setColor0 = SET_INT_FAIL;
                setColor1 = SET_INT_FAIL;
                setColor2 = SET_INT_FAIL;
                colorOffset0 = BAD_ADDRESS;
                colorOffset1 = BAD_ADDRESS;
                colorOffset2 = BAD_ADDRESS;
                break;
                
            case PolyStreamFormat.VERTEX_COLOR_SAME:
                getColor0 = GET_INT;
                getColor1 = GET_INT;
                getColor2 = GET_INT;
                setColor0 = SET_INT;
                setColor1 = SET_INT;
                setColor2 = SET_INT;
                colorOffset0 = offset++;
                colorOffset1 = colorOffset0;
                colorOffset2 = colorOffset0;
                break;
                
            case PolyStreamFormat.VERTEX_COLOR_SAME_BY_LAYER:
                getColor0 = GET_INT;
                getColor1 = GET_INT;
                getColor2 = GET_INT;
                setColor0 = SET_INT;
                setColor1 = SET_INT;
                setColor2 = SET_INT;
                colorOffset0 = offset++;
                colorOffset1 = offset++;
                colorOffset2 = offset++;
                break;
                
            default:
            case PolyStreamFormat.VERTEX_COLOR_PER_VERTEX_LAYER:
                getColor0 = GET_INT_FAIL;
                getColor1 = GET_INT_FAIL;
                getColor2 = GET_INT_FAIL;
                setColor0 = SET_INT_FAIL;
                setColor1 = SET_INT_FAIL;
                setColor2 = SET_INT_FAIL;
                colorOffset0 = BAD_ADDRESS;
                colorOffset1 = BAD_ADDRESS;
                colorOffset2 = BAD_ADDRESS;
                break;
        }
        
        stride = offset - baseOffset;
    }
    
    public final int stride()
    {
        return stride;
    }

    public final void setFaceNormal(IIntStream stream, int baseAddress, Vec3f normal)
    {
        setFaceNormal(stream, baseAddress, normal.x(), normal.y(), normal.z());
        
    }
    
    public final void setFaceNormal(IIntStream stream, int baseAddress, float x, float y, float z)
    {
        setNormalXYZ.set(stream, baseAddress + getNormalXOffset, x, y, z);
    }
    
    public final Vec3f getFaceNormal(IIntStream stream, int baseAddress)
    {
        return Vec3f.create(
                getNormalX.get(stream, baseAddress + getNormalXOffset), 
                getNormalY.get(stream, baseAddress + getNormalYOffset), 
                getNormalZ.get(stream, baseAddress + getNormalZOffset));
    }
    
    public final float getFaceNormalX(IIntStream stream, int baseAddress)
    {
        return getNormalX.get(stream, baseAddress + getNormalXOffset);
    }

    public final float getFaceNormalY(IIntStream stream, int baseAddress)
    {
        return getNormalY.get(stream, baseAddress + getNormalYOffset);
    }
    
    public final float getFaceNormalZ(IIntStream stream, int baseAddress)
    {
        return getNormalZ.get(stream, baseAddress + getNormalZOffset);
    }

    public final int getTag(IIntStream stream, int baseAddress)
    {
        return getTag.get(stream, baseAddress + tagOffset);
    }

    public final void setTag(IIntStream stream, int baseAddress, int tag)
    {
        setTag.set(stream, baseAddress + tagOffset, tag);
    }

    public final int getLink(IIntStream stream, int baseAddress)
    {
        return getLink.get(stream, baseAddress + linkOffset);
    }

    public final void setLink(IIntStream stream, int baseAddress, int link)
    {
        setLink.set(stream, baseAddress + linkOffset, link);
    }
    
    public final float getMaxU(IIntStream stream, int baseAddress, int layerIndex)
    {
        return layerIndex == 0 
                ? getU0.get(stream, baseAddress + maxUOffset0)
                : layerIndex == 1
                    ? getU1.get(stream, baseAddress + maxUOffset1)
                    : getU2.get(stream, baseAddress + maxUOffset2);
    }
    
    public final void setMaxU(IIntStream stream, int baseAddress, int layerIndex, float maxU)
    {
        if(layerIndex == 0)
            setU0.set(stream, baseAddress + maxUOffset0, maxU);
        else if(layerIndex == 1)
            setU1.set(stream, baseAddress + maxUOffset1, maxU);
        else
            setU2.set(stream, baseAddress + maxUOffset2, maxU);
    }

    public final float getMinU(IIntStream stream, int baseAddress, int layerIndex)
    {
        return layerIndex == 0 
                ? getU0.get(stream, baseAddress + minUOffset0)
                : layerIndex == 1
                    ? getU1.get(stream, baseAddress + minUOffset1)
                    : getU2.get(stream, baseAddress + minUOffset2);
    }
    
    public final void setMinU(IIntStream stream, int baseAddress, int layerIndex, float minU)
    {
        if(layerIndex == 0)
            setU0.set(stream, baseAddress + minUOffset0, minU);
        else if(layerIndex == 1)
            setU1.set(stream, baseAddress + minUOffset1, minU);
        else
            setU2.set(stream, baseAddress + minUOffset2, minU);
    }
    
    public final float getMaxV(IIntStream stream, int baseAddress, int layerIndex)
    {
        return layerIndex == 0 
                ? getV0.get(stream, baseAddress + maxVOffset0)
                : layerIndex == 1
                    ? getV1.get(stream, baseAddress + maxVOffset1)
                    : getV2.get(stream, baseAddress + maxVOffset2);
    }
    
    public final void setMaxV(IIntStream stream, int baseAddress, int layerIndex, float maxV)
    {
        if(layerIndex == 0)
            setV0.set(stream, baseAddress + maxVOffset0, maxV);
        else if(layerIndex == 1)
            setV1.set(stream, baseAddress + maxVOffset1, maxV);
        else
            setV2.set(stream, baseAddress + maxVOffset2, maxV);
    }
    
    public final float getMinV(IIntStream stream, int baseAddress, int layerIndex)
    {
        return layerIndex == 0 
                ? getV0.get(stream, baseAddress + minVOffset0)
                : layerIndex == 1
                    ? getV1.get(stream, baseAddress + minVOffset1)
                    : getV2.get(stream, baseAddress + minVOffset2);
    }
    
    public final void setMinV(IIntStream stream, int baseAddress, int layerIndex, float minV)
    {
        if(layerIndex == 0)
            setV0.set(stream, baseAddress + minVOffset0, minV);
        else if(layerIndex == 1)
            setV1.set(stream, baseAddress + minVOffset1, minV);
        else
            setV2.set(stream, baseAddress + minVOffset2, minV);
    }

    public final String getTextureName(IIntStream stream, int baseAddress, int layerIndex)
    {
        final int handle = layerIndex == 0 
                ? getTexture0.get(stream, baseAddress + textureOffset01)
                : layerIndex == 1
                    ? getTexture1.get(stream, baseAddress + textureOffset01)
                    : getTexture2.get(stream, baseAddress + textureOffset2);
        
        return textureHandler.fromHandle(handle);
    }
    
    public final void setTextureName(IIntStream stream, int baseAddress, int layerIndex, String textureName)
    {
        final int handle = textureHandler.toHandle(textureName);
        if(layerIndex == 0)
            setTexture0.set(stream, baseAddress + textureOffset01, handle);
        else if(layerIndex == 1)
            setTexture1.set(stream, baseAddress + textureOffset01, handle);
        else
            setTexture2.set(stream, baseAddress + textureOffset2, handle);
    }

    public final int getVertexColor(IIntStream stream, int baseAddress, int layerIndex)
    {
        return layerIndex == 0 
                ? getColor0.get(stream, baseAddress + colorOffset0)
                : layerIndex == 1
                    ? getColor1.get(stream, baseAddress + colorOffset1)
                    : getColor2.get(stream, baseAddress + colorOffset2);
    }
    
    public final void setVertexColor(IIntStream stream, int baseAddress, int layerIndex, int color)
    {
        if(layerIndex == 0)
            setColor0.set(stream, baseAddress + colorOffset0, color);
        else if(layerIndex == 1)
            setColor1.set(stream, baseAddress + colorOffset1, color);
        else
            setColor2.set(stream, baseAddress + colorOffset2, color);
    }
    
}
