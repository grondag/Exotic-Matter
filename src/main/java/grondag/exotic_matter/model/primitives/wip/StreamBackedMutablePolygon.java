package grondag.exotic_matter.model.primitives.wip;

import javax.annotation.Nullable;

import grondag.acuity.api.IRenderPipeline;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.polygon.IMutablePolygon;
import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

public class StreamBackedMutablePolygon extends StreamBackedPolygon implements IMutablePolygon
{
    @Override
    public final IMutablePolygon setVertexLayer(int layerIndex, int vertexIndex, float u, float v, int color, int glow)
    {
        setVertexColor(layerIndex, vertexIndex, color);
        setVertexUV(layerIndex, vertexIndex, u, v);
        setVertexGlow(vertexIndex, glow);
        return this;
    }

    @Override
    public final IMutablePolygon setMaxU(int layerIndex, float maxU)
    {
        polyEncoder.setMaxU(stream, baseAddress, layerIndex, maxU);
        return this;
    }

    @Override
    public final IMutablePolygon setMaxV(int layerIndex, float maxV)
    {
        polyEncoder.setMaxV(stream, baseAddress, layerIndex, maxV);
        return this;
    }

    @Override
    public final IMutablePolygon setMinU(int layerIndex, float minU)
    {
        polyEncoder.setMinU(stream, baseAddress, layerIndex, minU);
        return this;
    }

    @Override
    public final IMutablePolygon setMinV(int layerIndex, float minV)
    {
        polyEncoder.setMinV(stream, baseAddress, layerIndex, minV);
        return this;
    }

    @Override
    public final IMutablePolygon setTextureSalt(int salt)
    {
        StaticEncoder.setTextureSalt(stream, baseAddress, salt);
        return this;
    }

    @Override
    public final IMutablePolygon setLockUV(int layerIndex, boolean lockUV)
    {
        StaticEncoder.setLockUV(stream, baseAddress, layerIndex, lockUV);
        return this;
    }

    @Override
    public final IMutablePolygon setTextureName(int layerIndex, String textureName)
    {
        polyEncoder.setTextureName(stream, baseAddress, layerIndex, textureName);
        return this;
    }

    @Override
    public final IMutablePolygon setRotation(int layerIndex, Rotation rotation)
    {
        StaticEncoder.setRotation(stream, baseAddress, layerIndex, rotation);
        return this;
    }

    @Override
    public final IMutablePolygon setShouldContractUVs(int layerIndex, boolean contractUVs)
    {
        StaticEncoder.setContractUVs(stream, baseAddress, layerIndex, contractUVs);
        return this;
    }

    @Override
    public final IMutablePolygon setRenderLayer(int layerIndex, BlockRenderLayer layer)
    {
        StaticEncoder.setRenderLayer(stream, baseAddress, layerIndex, layer);
        return this;
    }

    //FIXME: should throw exception if not a fixed format
    @Override
    public final IMutablePolygon setLayerCount(int layerCount)
    {
        saveAndLoadFormat(PolyStreamFormat.setLayerCount(format(), layerCount));
        return this;
    }

    @Override
    public final IMutablePolygon setEmissive(int layerIndex, boolean emissive)
    {
        StaticEncoder.setEmissive(stream, baseAddress, layerIndex, emissive);
        return this;
    }

    @Override
    public final IMutablePolygon setVertex(int vertexIndex, float x, float y, float z, float u, float v, int color, int glow)
    {
        setVertexPos(vertexIndex, x, y, z);
        setVertexUV(0, vertexIndex, u, v);
        setVertexColor(0, vertexIndex, color);
        setVertexGlow(vertexIndex, glow);
        return this;
    }

    @Override
    public final IMutablePolygon setVertexPos(int vertexIndex, float x, float y, float z)
    {
        vertexEncoder.setVertexPos(stream, vertexAddress, vertexIndex, x, y, z);
        return this;
    }

    @Override
    public final IMutablePolygon setVertexPos(int vertexIndex, Vec3f pos)
    {
        vertexEncoder.setVertexPos(stream, vertexAddress, vertexIndex, pos.x(), pos.y(), pos.z());
        return this;
    }

    @Override
    public final IMutablePolygon setVertexColor(int layerIndex, int vertexIndex, int color)
    {
        if(vertexEncoder.hasColor())
            vertexEncoder.setVertexColor(stream, vertexAddress, layerIndex, vertexIndex, color);
        else
            polyEncoder.setVertexColor(stream, baseAddress, layerIndex, color);
        return this;
    }
    
    @Override
    public final IMutablePolygon setVertexU(int layerIndex, int vertexIndex, float u)
    {
        vertexEncoder.setVertexU(stream, vertexAddress, layerIndex, vertexIndex, u);
        return this;
    }
    
    @Override
    public final IMutablePolygon setVertexV(int layerIndex, int vertexIndex, float v)
    {
        vertexEncoder.setVertexV(stream, vertexAddress, layerIndex, vertexIndex, v);
        return this;
    }
    
    @Override
    public final IMutablePolygon setVertexUV(int layerIndex, int vertexIndex, float u, float v)
    {
        vertexEncoder.setVertexUV(stream, vertexAddress, layerIndex, vertexIndex, u, v);
        return this;
    }

    @Override
    public final IMutablePolygon setVertexGlow(int vertexIndex, int glow)
    {
        glowEncoder.setGlow(stream, glowAddress, vertexIndex, glow);
        return this;
    }

    @Override
    public final IMutablePolygon setVertexNormal(int vertexIndex, @Nullable Vec3f normal)
    {
        if(vertexEncoder.hasNormals())
        {
            if(normal == null)
                vertexEncoder.setVertexNormal(stream, vertexAddress, vertexIndex, 0, 0, 0);
            else
                vertexEncoder.setVertexNormal(stream, vertexAddress, vertexIndex, normal.x(), normal.y(), normal.z());
        }
        return this;
    }

    @Override
    public final IMutablePolygon setVertexNormal(int vertexIndex, float x, float y, float z)
    {
        if(vertexEncoder.hasNormals())
            vertexEncoder.setVertexNormal(stream, vertexAddress, vertexIndex, x, y, z);
        return this;
    }

    @Override
    public final IMutablePolygon setPipeline(IRenderPipeline pipeline)
    {
        StaticEncoder.setPipeline(stream, baseAddress, pipeline);
        return this;
    }

    @Override
    public final IMutablePolygon clearFaceNormal()
    {
        int normalFormat = PolyStreamFormat.getFaceNormalFormat(format());
        
        assert normalFormat == PolyStreamFormat.FACE_NORMAL_FORMAT_FULL_MISSING || 
                normalFormat == PolyStreamFormat.FACE_NORMAL_FORMAT_FULL_PRESENT
                : "Face normal clear should only happen for full-precision polys";
        
        if(normalFormat == PolyStreamFormat.FACE_NORMAL_FORMAT_FULL_PRESENT)
            saveAndLoadFormat(PolyStreamFormat.setFaceNormalFormat(format(), PolyStreamFormat.FACE_NORMAL_FORMAT_FULL_MISSING));
        return this;
    }

    @Override
    public final IMutablePolygon setNominalFace(EnumFacing face)
    {
        saveAndLoadFormat(PolyStreamFormat.setNominalFace(format(), face));
        return this;
    }

    @Override
    public final IMutablePolygon setSurface(Surface surface)
    {
        StaticEncoder.setSurface(stream, baseAddress, surface);
        return this;
    }

    @Override
    public final IMutablePolygon copyVertexFrom(int targetIndex, IPolygon source, int sourceIndex)
    {
        if(source.hasVertexNormal(sourceIndex))
        {
            assert vertexEncoder.hasNormals();
            setVertexNormal(targetIndex, source.getVertexNormalX(sourceIndex), source.getVertexNormalY(sourceIndex), source.getVertexNormalZ(sourceIndex));
        }
        else if(vertexEncoder.hasNormals())
            vertexEncoder.setVertexNormal(stream, vertexAddress, targetIndex, 0, 0, 0);

        setVertexPos(targetIndex, source.getVertexX(sourceIndex), source.getVertexY(sourceIndex), source.getVertexZ(sourceIndex));

        setVertexGlow(targetIndex, source.getVertexGlow(sourceIndex));
        
        final int layerCount = source.layerCount();
        assert layerCount <= layerCount();
        
        setVertexColor(0, targetIndex, source.getVertexColor(0, sourceIndex));
        setVertexUV(0, targetIndex, source.getVertexU(0, sourceIndex), source.getVertexV(0, sourceIndex));
        
        if(layerCount > 1)
        {
            setVertexColor(1, targetIndex, source.getVertexColor(1, sourceIndex));
            setVertexUV(1, targetIndex, source.getVertexU(1, sourceIndex), source.getVertexV(1, sourceIndex));
            
            if(layerCount == 3)
            {
                setVertexColor(2, targetIndex, source.getVertexColor(2, sourceIndex));
                setVertexUV(2, targetIndex, source.getVertexU(2, sourceIndex), source.getVertexV(2, sourceIndex));
            }
        }
        return this;
    }
}
