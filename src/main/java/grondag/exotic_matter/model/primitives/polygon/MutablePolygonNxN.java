package grondag.exotic_matter.model.primitives.polygon;

import javax.annotation.Nullable;

import grondag.acuity.api.IRenderPipeline;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.FaceVertex;
import grondag.exotic_matter.model.primitives.polygon.PolygonAccessor.Layer;
import grondag.exotic_matter.model.primitives.vertex.IMutableVertex;
import grondag.exotic_matter.model.primitives.vertex.UnpackedVertex3;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

public class MutablePolygonNxN extends AbstractLargePolygon<MutablePolygonNxN> implements IMutablePolygon
{
    @SuppressWarnings("unchecked")
    private static final Layer<MutablePolygonNxN>[] LAYERS = new Layer[3];
    
    static
    {
        LAYERS[0] = new Layer<MutablePolygonNxN>()
        {
            {
                this.textureGetter = p -> p.texture0;
                this.textureSetter = (p, v) -> p.texture0 = v;
                
                this.uMaxGetter = p -> p.uMax0;
                this.uMaxSetter = (p, v) -> p.uMax0 = v;
                
                this.uMinGetter = p -> p.uMin0;
                this.uMinSetter = (p, v) -> p.uMin0 = v;
                
                this.vMaxGetter = p -> p.vMax0;
                this.vMaxSetter = (p, v) -> p.vMax0 = v;
                
                this.vMinGetter = p -> p.vMin0;
                this.vMinSetter = (p, v) -> p.vMin0 = v;
            }
        };
        
        LAYERS[1] = new Layer<MutablePolygonNxN>()
        {
            {
                this.textureGetter = p -> p.texture1;
                this.textureSetter = (p, v) -> p.texture1 = v;
                
                this.uMaxGetter = p -> p.uMax1;
                this.uMaxSetter = (p, v) -> p.uMax1 = v;
                
                this.uMinGetter = p -> p.uMin1;
                this.uMinSetter = (p, v) -> p.uMin1 = v;
                
                this.vMaxGetter = p -> p.vMax1;
                this.vMaxSetter = (p, v) -> p.vMax1 = v;
                
                this.vMinGetter = p -> p.vMin1;
                this.vMinSetter = (p, v) -> p.vMin1 = v;
            }
        };
        
        LAYERS[2] = new Layer<MutablePolygonNxN>()
        {
            {
                this.textureGetter = p -> p.texture2;
                this.textureSetter = (p, v) -> p.texture2 = v;
                
                this.uMaxGetter = p -> p.uMax2;
                this.uMaxSetter = (p, v) -> p.uMax2 = v;
                
                this.uMinGetter = p -> p.uMin2;
                this.uMinSetter = (p, v) -> p.uMin2 = v;
                
                this.vMaxGetter = p -> p.vMax2;
                this.vMaxSetter = (p, v) -> p.vMax2 = v;
                
                this.vMinGetter = p -> p.vMin2;
                this.vMinSetter = (p, v) -> p.vMin2 = v;
            }
        };
    }
    
    private String texture0;
    private float uMin0;
    private float uMax0;
    private float vMin0;
    private float vMax0;
    
    private String texture1;
    private float uMin1;
    private float uMax1;
    private float vMin1;
    private float vMax1;
    
    private String texture2;
    private float uMin2;
    private float uMax2;
    private float vMin2;
    private float vMax2;
    
    private int vertexCount;
    private int layerCount;
    
    private IMutableVertex[] vertices;
    
    public MutablePolygonNxN(int exponentOfTwo)
    {
        super();
        assert exponentOfTwo >= 3;
        vertices = new IMutableVertex[1 << exponentOfTwo];
        for(int i = 0; i < vertices.length; i++)
            vertices[i] = new UnpackedVertex3();
    }
    
    @Override
    protected void copyPolyAttributesFrom(IPolygon template)
    {
        this.setLayerCount(template.layerCount());
        super.copyPolyAttributesFrom(template);
    }
    
    @Override
    public final IMutableVertex[] vertices()
    {
        return vertices;
    }
    
    @Override
    protected final int computeArrayIndex(int vertexIndex)
    {
        return vertexIndex;
    }
    
    public MutablePolygonNxN prepare(int layerCount, int vertexCount)
    {
        assert layerCount >= 1;
        assert layerCount <= 3;
        assert vertexCount > 4;
        assert vertexCount <= vertices.length;
        this.layerCount = layerCount;
        this.vertexCount = vertexCount;
        return this;
    }
    
    @Override
    public final int layerCount()
    {
        return layerCount;
    }

    @Override
    public final int vertexCount()
    {
        return vertexCount;
    }
    
    @Override
    protected final Layer<MutablePolygonNxN>[] layerAccess()
    {
        return LAYERS;
    }

    @Override
    public IMutablePolygon setMaxU(int layerIndex, float maxU)
    {
        super.setMaxUImpl(layerIndex, maxU);
        return this;
    }

    @Override
    public IMutablePolygon setMaxV(int layerIndex, float maxV)
    {
        super.setMaxVImpl(layerIndex, maxV);
        return this;
    }

    @Override
    public IMutablePolygon setMinU(int layerIndex, float minU)
    {
        super.setMinUImpl(layerIndex, minU);
        return this;
    }

    @Override
    public IMutablePolygon setMinV(int layerIndex, float minV)
    {
        super.setMinVImpl(layerIndex, minV);
        return this;
    }

    @Override
    public IMutablePolygon setTextureSalt(int salt)
    {
        super.setTextureSaltImpl(salt);
        return this;
    }

    @Override
    public IMutablePolygon setLockUV(int layerIndex, boolean lockUV)
    {
        super.setLockUVImpl(layerIndex, lockUV);
        return this;
    }

    @Override
    public IMutablePolygon setTextureName(int layerIndex, String textureName)
    {
        super.setTextureNameImpl(layerIndex, textureName);
        return this;
    }

    @Override
    public IMutablePolygon setRotation(int layerIndex, Rotation rotation)
    {
        super.setRotationImpl(layerIndex, rotation);
        return this;
    }

    @Override
    public IMutablePolygon setShouldContractUVs(int layerIndex, boolean contractUVs)
    {
        super.setShouldContractUVsImpl(layerIndex, contractUVs);
        return this;
    }

    @Override
    public IMutablePolygon setRenderLayer(int layerIndex, BlockRenderLayer layer)
    {
        super.setRenderLayerImpl(layerIndex, layer);
        return this;
    }

    @Override
    public IMutablePolygon setEmissive(int layerIndex, boolean emissive)
    {
        super.setEmissiveImpl(layerIndex, emissive);
        return this;
    }

    @Override
    public IMutablePolygon setVertexColorGlow(int layerIndex, int vertexIndex, int color, int glow)
    {
        super.setVertexColorGlowImpl(layerIndex, vertexIndex, color, glow);
        return this;
    }

    @Override
    public IMutablePolygon setVertexColor(int layerIndex, int vertexIndex, int color)
    {
        super.setVertexColorImpl(layerIndex, vertexIndex, color);
        return this;
    }

    @Override
    public IMutablePolygon setVertexUV(int layerIndex, int vertexIndex, float u, float v)
    {
        super.setVertexUVImpl(layerIndex, vertexIndex, u, v);
        return this;
    }

    @Override
    public IMutablePolygon setVertexGlow(int layerIndex, int vertexIndex, int glow)
    {
        super.setVertexGlowImpl(layerIndex, vertexIndex, glow);
        return this;
    }

    @Override
    public IMutablePolygon setVertexNormal(int vertexIndex, @Nullable Vec3f normal)
    {
        super.setVertexNormalImpl(vertexIndex, normal);
        return this;
    }

    @Override
    public IMutablePolygon setVertexNormal(int vertexIndex, float x, float y, float z)
    {
        super.setVertexNormalImpl(vertexIndex, x, y, z);
        return this;
    }

    @Override
    public IMutablePolygon setPipeline(@Nullable IRenderPipeline pipeline)
    {
        super.setPipelineImpl(pipeline);
        return this;
    }

    @Override
    public IMutablePolygon clearFaceNormal()
    {
        super.clearFaceNormalImpl();
        return this;
    }

    @Override
    public IMutablePolygon setupFaceQuad(EnumFacing side, FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, FaceVertex tv3, @Nullable EnumFacing topFace)
    {
        super.setupFaceQuadImpl(side, tv0, tv1, tv2, tv3, topFace);
        return this;
    }

    @Override
    public IMutablePolygon setupFaceQuad(FaceVertex vertexIn0, FaceVertex vertexIn1, FaceVertex vertexIn2, FaceVertex vertexIn3, @Nullable EnumFacing topFace)
    {
        super.setupFaceQuadImpl(vertexIn0, vertexIn1, vertexIn2, vertexIn3, topFace);
        return this;
    }

    @Override
    public IMutablePolygon setupFaceQuad(float x0, float y0, float x1, float y1, float depth, @Nullable EnumFacing topFace)
    {
        super.setupFaceQuadImpl(x0, y0, x1, y1, depth, topFace);
        return this;
    }

    @Override
    public IMutablePolygon setupFaceQuad(EnumFacing face, float x0, float y0, float x1, float y1, float depth, @Nullable EnumFacing topFace)
    {
        super.setupFaceQuadImpl(face, x0, y0, x1, y1, depth, topFace);
        return this;
    }

    @Override
    public IMutablePolygon setupFaceQuad(EnumFacing side, FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, @Nullable EnumFacing topFace)
    {
        super.setupFaceQuadImpl(side, tv0, tv1, tv2, topFace);
        return this;
    }

    @Override
    public IMutablePolygon setupFaceQuad(FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, @Nullable EnumFacing topFace)
    {
        super.setupFaceQuadImpl(tv0, tv1, tv2, topFace);
        return this;
    }

    @Override
    public IMutablePolygon setNominalFace(EnumFacing face)
    {
        super.setNominalFaceImpl(face);
        return this;
    }

    @Override
    public IMutablePolygon setSurface(Surface surface)
    {
        super.setSurfaceImpl(surface);
        return this;
    }

    @Override
    public IMutablePolygon setVertexLayer(int layerIndex, int vertexIndex, float u, float v, int color, int glow)
    {
        super.setVertexLayerImpl(layerIndex, vertexIndex, u, v, color, glow);
        return this;
    }

    @Override
    public final IMutablePolygon setVertex(int vertexIndex, float x, float y, float z, float u, float v, int color, int glow)
    {
        super.setVertexImpl(vertexIndex, x, y, z, u, v, color, glow);
        return this;
    }

    @Override
    public final IMutablePolygon setVertexPos(int vertexIndex, float x, float y, float z)
    {
        super.setVertexPosImpl(vertexIndex, x, y, z);
        return this;
    }

    @Override
    public final IMutablePolygon setVertexPos(int vertexIndex, Vec3f pos)
    {
        super.setVertexPosImpl(vertexIndex, pos);
        return this;
    }

    @Override
    public IMutablePolygon setLayerCount(int layerCount)
    {
        this.layerCount = layerCount;
        for(int i = 0; i < vertexCount; i++)
            vertices[i].setLayerCount(layerCount);
        
        return this;
    }

    @Override
    public IMutablePolygon copyVertexFrom(int targetIndex, IPolygon source, int sourceIndex)
    {
        copyVertexFromImpl(targetIndex, source, sourceIndex);
        return this;
    }
}
