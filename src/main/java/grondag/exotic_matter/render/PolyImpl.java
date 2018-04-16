package grondag.exotic_matter.render;

import java.util.ArrayList;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector4f;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.render.Surface.SurfaceInstance;
import grondag.exotic_matter.varia.Color;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

@Deprecated
class PolyImpl implements IMutableCSGPolygon
{
    private Vertex[] vertices;
    private @Nullable Vec3d faceNormal;
    private final int vertexCount;

    private @Nullable EnumFacing nominalFace;
    private Rotation rotation = Rotation.ROTATE_NONE;
    private boolean isFullBrightness = false;
    private boolean isLockUV = false;
    private boolean shouldContractUVs = true;
    private RenderPass renderPass = RenderPass.SOLID_SHADED;
    

    private @Nullable String textureName;
    
    
    private int color = Color.WHITE;
    private SurfaceInstance surfaceInstance = IPolygon.NO_SURFACE;

    private float minU = 0;
    private float maxU = 16;
    private float minV = 0;
    private float maxV = 16;

    private static AtomicInteger nextQuadID = new AtomicInteger(1);
    private boolean isInverted = false;
    
    private final int quadID = nextQuadID.incrementAndGet();
    private int ancestorQuadID = ICSGPolygon.NO_ID;
    private int[] lineID;

    PolyImpl()
    {
        this(4);
    }

    PolyImpl(IPolygon template)
    {
        this(template, template.vertexCount());
    }

    PolyImpl(int vertexCount)
    {
        assert vertexCount > 2 : "Bad polygon structure.";
        this.vertexCount = vertexCount;
        this.vertices = new Vertex[vertexCount];
        this.lineID = new int[vertexCount];
    }

    PolyImpl(IPolygon template, int vertexCount)
    {
        this(vertexCount);
        this.copyProperties(template);
    }

    @Override
    public PolyImpl clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }
    
    @Deprecated
    protected PolyImpl mutableCopy()
    {
        PolyImpl result = new PolyImpl(this);
        result.copyVertices(this);
        return result;
    }

    protected void copyVertices(IPolygon template)
    {
        final int c = this.vertexCount();
        
        if(template.isCSG())
        {
            ICSGPolygon fromCSG = template.csgReference();
            for(int i = 0; i < c; i++)
            {
                this.setVertex(i, fromCSG.getVertex(i));
                this.lineID[i] = fromCSG.getLineID(i);
            }
        }
        else
        {
            for(int i = 0; i < c; i++)
            {
                this.setVertex(i, template.getVertex(i));
            }
        }
    }

    @Override
    public int vertexCount()
    {
        return this.vertexCount;
    }

    private void copyProperties(IPolygon fromObject)
    {
        this.setNominalFace(fromObject.getNominalFace());
        this.setTextureName(fromObject.getTextureName());
        this.setRotation(fromObject.getRotation());
        this.setColor(fromObject.getColor());
        this.setFullBrightness(fromObject.isFullBrightness());
        this.setLockUV(fromObject.isLockUV());
        if(fromObject.hasFaceNormal()) this.faceNormal = fromObject.getFaceNormal();
        this.setShouldContractUVs(fromObject.shouldContractUVs());
        this.setMinU(fromObject.getMinU());
        this.setMaxU(fromObject.getMaxU());
        this.setMinV(fromObject.getMinV());
        this.setMaxV(fromObject.getMaxV());
        this.setRenderPass(fromObject.getRenderPass());
        this.setSurfaceInstance(fromObject.getSurfaceInstance());
        
        if(fromObject.isCSG())
        {
            ICSGPolygon fromCSG = fromObject.csgReference();
            this.setAncestorQuadID(fromCSG.getAncestorQuadID());
            this.setInverted(fromCSG.isInverted());
        }
    }

    @Override
    public List<IPolygon> toQuads()
    {
        return this.toQuadsInner();
    }
    
    @Override
    public List<ICSGPolygon> toQuadsCSG()
    {
        return this.toQuadsInner();
    }

    
    @SuppressWarnings("unchecked")
    private <T extends IPolygon> List<T> toQuadsInner()
    {
        ArrayList<T> retVal = new ArrayList<T>();

        if(this.vertexCount <= 4)
        {
            retVal.add((T) this);
        }
        else
        {
            int head = vertexCount - 1;
            int tail = 2;
            PolyImpl work = new PolyImpl(this, 4);
            work.setVertex(0, this.getVertex(head));
            work.setVertex(1, this.getVertex(0));
            work.setVertex(2, this.getVertex(1));
            work.setVertex(3, this.getVertex(tail));
            retVal.add((T) work);

            while(head - tail > 1)
            {
                work = new PolyImpl(this, head - tail == 2 ? 3 : 4);
                work.setVertex(0, this.getVertex(head));
                work.setVertex(1, this.getVertex(tail));
                work.setVertex(2, this.getVertex(++tail));
                if(head - tail > 1)
                {
                    work.setVertex(3, this.getVertex(--head));
                }
                retVal.add((T) work);
            }
        }
        return retVal;
    }
    
    @Override
    public List<IPolygon> toTris()
    {
        return this.toTrisInner();
    }

    @Override
    public List<ICSGPolygon> toTrisCSG()
    {
        return this.toTrisInner();
    }
    
    /** 
     * If this is a quad, returns two new tris.
     * If is already a tri, returns copy of self.<p>
     * 
     * Does not mutate this object and does not 
     * retain any reference to polygons in this object.
     */
    @SuppressWarnings("unchecked")
    private <T extends IPolygon> List<T> toTrisInner()
    {
        if(this.vertexCount() == 3)
        {
            return ImmutableList.of((T)this.mutableCopy());
        }
        else
        {
            ArrayList<T>  retVal= new ArrayList<>(this.vertexCount()-2);
            int splitLineID = CSGPlane.nextInsideLineID.getAndIncrement();
            int head = vertexCount - 1;
            int tail = 1;

            PolyImpl work = new PolyImpl(this, 3);
            work.setVertex(0, this.getVertex(head));
            work.setLineID(0, this.getLineID(head));
            work.setVertex(1, this.getVertex(0));
            work.setLineID(1, this.getLineID(0));
            work.setVertex(2, this.getVertex(tail));
            work.setLineID(2, splitLineID);
            work.setAncestorQuadID(this.getAncestorQuadIDForDescendant());
            retVal.add((T) work);

            while(head - tail > 1)
            {
                work = new PolyImpl(this, 3);
                work.setVertex(0, this.getVertex(head));
                work.setLineID(0, splitLineID);
                work.setVertex(1, this.getVertex(tail));
                work.setLineID(1, this.getLineID(tail));
                splitLineID = CSGPlane.nextInsideLineID.getAndIncrement();
                work.setVertex(2, this.getVertex(++tail));
                work.setLineID(2, head - tail == 1 ? this.getLineID(tail): splitLineID);
                work.setAncestorQuadID(this.getAncestorQuadIDForDescendant());
                retVal.add((T) work);

                if(head - tail > 1)
                {
                    work = new PolyImpl(this, 3);
                    work.setVertex(0, this.getVertex(head));
                    work.setLineID(0, splitLineID);
                    splitLineID = CSGPlane.nextInsideLineID.getAndIncrement();
                    work.setVertex(1, this.getVertex(tail));
                    work.setLineID(1, head - tail == 1 ? this.getLineID(tail): splitLineID);
                    work.setVertex(2, this.getVertex(--head).clone());
                    work.setLineID(2, this.getLineID(head));
                    work.setAncestorQuadID(this.getAncestorQuadIDForDescendant());
                    retVal.add((T) work);
                }
            }
            return retVal;
        }

    }

    /**
     * Reverses winding order of this quad and returns itself
     */
    @Override
    public ICSGPolygon invert()
    {

        for(int i = 0; i < vertices.length; i++)
        {
            Vertex v = getVertex(i);
            if(v != null)
            {
                this.setVertex(i, v.withNormal(-v.normalX, -v.normalY, -v.normalZ));
            }
        }            

        //reverse order of vertices
        for (int i = 0, mid = vertices.length / 2, j = vertices.length - 1; i < mid; i++, j--)
        {
            Vertex swapVertex = vertices[i];
            vertices[i] = vertices[j];
            vertices[j] = swapVertex;
        }

        // last edge is still always the last, and isn't sorted  (draw it to see)
        for (int i = 0, mid = (vertices.length - 1) / 2, j = vertices.length - 2; i < mid; i++, j--)
        {
            int swapLineID = lineID[i];
            lineID[i] = lineID[j];
            lineID[j] = swapLineID;
        }

        if(this.faceNormal != null) this.faceNormal = faceNormal.scale(-1);

        this.setInverted(!this.isInverted());

        return this;
    }

    @Override
    public IMutablePolygon setupFaceQuad(FaceVertex vertexIn0, FaceVertex vertexIn1, FaceVertex vertexIn2, FaceVertex vertexIn3, EnumFacing topFace)
    {
        
        EnumFacing defaultTop = QuadHelper.defaultTopOf(this.getNominalFace());
        if(topFace == null) topFace = defaultTop;
        
        FaceVertex rv0;
        FaceVertex rv1;
        FaceVertex rv2;
        FaceVertex rv3;

        if(topFace == defaultTop)
        {
            rv0 = vertexIn0;
            rv1 = vertexIn1;
            rv2 = vertexIn2;
            rv3 = vertexIn3;
        }
        else if(topFace == QuadHelper.rightOf(this.getNominalFace(), defaultTop))
        {
            rv0 = vertexIn0.withXY(vertexIn0.y, 1 - vertexIn0.x);
            rv1 = vertexIn1.withXY(vertexIn1.y, 1 - vertexIn1.x);
            rv2 = vertexIn2.withXY(vertexIn2.y, 1 - vertexIn2.x);
            rv3 = vertexIn3.withXY(vertexIn3.y, 1 - vertexIn3.x);
        }
        else if(topFace == QuadHelper.bottomOf(this.getNominalFace(), defaultTop))
        {
            rv0 = vertexIn0.withXY(1 - vertexIn0.x, 1 - vertexIn0.y);
            rv1 = vertexIn1.withXY(1 - vertexIn1.x, 1 - vertexIn1.y);
            rv2 = vertexIn2.withXY(1 - vertexIn2.x, 1 - vertexIn2.y);
            rv3 = vertexIn3.withXY(1 - vertexIn3.x, 1 - vertexIn3.y);
        }
        else // left of
        {
            rv0 = vertexIn0.withXY(1 - vertexIn0.y, vertexIn0.x);
            rv1 = vertexIn1.withXY(1 - vertexIn1.y, vertexIn1.x);
            rv2 = vertexIn2.withXY(1 - vertexIn2.y, vertexIn2.x);
            rv3 = vertexIn3.withXY(1 - vertexIn3.y, vertexIn3.x);
        }

        
        switch(this.getNominalFace())
        {
        case UP:
            setVertex(0, new Vertex(rv0.x, 1-rv0.depth, 1-rv0.y, rv0.u() * 16, rv0.v() * 16, rv0.color(this.getColor())));
            setVertex(1, new Vertex(rv1.x, 1-rv1.depth, 1-rv1.y, rv1.u() * 16, rv1.v() * 16, rv1.color(this.getColor())));
            setVertex(2, new Vertex(rv2.x, 1-rv2.depth, 1-rv2.y, rv2.u() * 16, rv2.v() * 16, rv2.color(this.getColor())));
            if(this.vertexCount == 4) setVertex(3, new Vertex(rv3.x, 1-rv3.depth, 1-rv3.y, rv3.u() * 16, rv3.v() * 16, rv3.color(this.getColor())));
            break;

        case DOWN:     
            setVertex(0, new Vertex(rv0.x, rv0.depth, rv0.y, 16-rv0.u() * 16, 16-rv0.v() * 16, rv0.color(this.getColor())));
            setVertex(1, new Vertex(rv1.x, rv1.depth, rv1.y, 16-rv1.u() * 16, 16-rv1.v() * 16, rv1.color(this.getColor())));
            setVertex(2, new Vertex(rv2.x, rv2.depth, rv2.y, 16-rv2.u() * 16, 16-rv2.v() * 16, rv2.color(this.getColor())));
            if(this.vertexCount == 4) setVertex(3, new Vertex(rv3.x, rv3.depth, rv3.y, 16-rv3.u() * 16, 16-rv3.v() * 16, rv3.color(this.getColor())));
            break;

        case EAST:
            setVertex(0, new Vertex(1-rv0.depth, rv0.y, 1-rv0.x, rv0.u() * 16, rv0.v() * 16, rv0.color(this.getColor())));
            setVertex(1, new Vertex(1-rv1.depth, rv1.y, 1-rv1.x, rv1.u() * 16, rv1.v() * 16, rv1.color(this.getColor())));
            setVertex(2, new Vertex(1-rv2.depth, rv2.y, 1-rv2.x, rv2.u() * 16, rv2.v() * 16, rv2.color(this.getColor())));
            if(this.vertexCount == 4) setVertex(3, new Vertex(1-rv3.depth, rv3.y, 1-rv3.x, rv3.u() * 16, rv3.v() * 16, rv3.color(this.getColor())));
            break;

        case WEST:
            setVertex(0, new Vertex(rv0.depth, rv0.y, rv0.x, rv0.u() * 16, rv0.v() * 16, rv0.color(this.getColor())));
            setVertex(1, new Vertex(rv1.depth, rv1.y, rv1.x, rv1.u() * 16, rv1.v() * 16, rv1.color(this.getColor())));
            setVertex(2, new Vertex(rv2.depth, rv2.y, rv2.x, rv2.u() * 16, rv2.v() * 16, rv2.color(this.getColor())));
            if(this.vertexCount == 4) setVertex(3, new Vertex(rv3.depth, rv3.y, rv3.x, rv3.u() * 16, rv3.v() * 16, rv3.color(this.getColor())));
            break;

        case NORTH:
            setVertex(0, new Vertex(1-rv0.x, rv0.y, rv0.depth, rv0.u() * 16, rv0.v() * 16, rv0.color(this.getColor())));
            setVertex(1, new Vertex(1-rv1.x, rv1.y, rv1.depth, rv1.u() * 16, rv1.v() * 16, rv1.color(this.getColor())));
            setVertex(2, new Vertex(1-rv2.x, rv2.y, rv2.depth, rv2.u() * 16, rv2.v() * 16, rv2.color(this.getColor())));
            if(this.vertexCount == 4) setVertex(3, new Vertex(1-rv3.x, rv3.y, rv3.depth, rv3.u() * 16, rv3.v() * 16, rv3.color(this.getColor())));
            break;

        case SOUTH:
            setVertex(0, new Vertex(rv0.x, rv0.y, 1-rv0.depth, rv0.u() * 16, rv0.v() * 16, rv0.color(this.getColor())));
            setVertex(1, new Vertex(rv1.x, rv1.y, 1-rv1.depth, rv1.u() * 16, rv1.v() * 16, rv1.color(this.getColor())));
            setVertex(2, new Vertex(rv2.x, rv2.y, 1-rv2.depth, rv2.u() * 16, rv2.v() * 16, rv2.color(this.getColor())));
            if(this.vertexCount == 4) setVertex(3, new Vertex(rv3.x, rv3.y, 1-rv3.depth, rv3.u() * 16, rv3.v() * 16, rv3.color(this.getColor())));
            break;
        }

        return this;
    }


    @Override
    public IMutablePolygon setupFaceQuad(EnumFacing side, FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, FaceVertex tv3, EnumFacing topFace)
    {
        assert(this.vertexCount() == 4);
        this.setNominalFace(side);
        return this.setupFaceQuad(tv0, tv1, tv2, tv3, topFace);
    }

    @Override
    public IMutablePolygon setupFaceQuad(float x0, float y0, float x1, float y1, float depth, EnumFacing topFace)
    {
        assert(this.vertexCount() == 4);
        this.setupFaceQuad(
                new FaceVertex(x0, y0, depth),
                new FaceVertex(x1, y0, depth),
                new FaceVertex(x1, y1, depth),
                new FaceVertex(x0, y1, depth), 
                topFace);
        return this;
    }


    @Override
    public IMutablePolygon setupFaceQuad(EnumFacing face, float x0, float y0, float x1, float y1, float depth, EnumFacing topFace)
    {
        assert(this.vertexCount() == 4);
        this.setNominalFace(face);
        return this.setupFaceQuad(x0, y0, x1, y1, depth, topFace);
    }

    @Override
    public IMutablePolygon setupFaceQuad(EnumFacing side, FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, EnumFacing topFace)
    {
        assert(this.vertexCount() == 3);
        this.setNominalFace(side);
        return this.setupFaceQuad(tv0, tv1, tv2, tv2, topFace);
    }


    @Override
    public IMutablePolygon setupFaceQuad(FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, EnumFacing topFace)
    {
        assert(this.vertexCount() == 3);
        return this.setupFaceQuad(tv0, tv1, tv2, tv2, topFace);
    }

    @Override
    public void setVertexNormal(int index, float x, float y, float z)
    {
        if(index < this.vertexCount)
        {
            this.setVertex(index, this.getVertex(index).withNormal(x, y, z));
        }
    }
    
    @Override
    public void setVertexNormal(int index, Vec3d normal)
    {
        this.setVertexNormal(index, (float)normal.x, (float)normal.y, (float)normal.z);
    }

    /**
     * Changes all vertices and quad color to new color and returns itself
     */
    @Override
    public IMutablePolygon replaceColor(int color)
    {
        this.setColor(color);
        for(int i = 0; i < this.vertexCount(); i++)
        {
            if(getVertex(i) != null) setVertex(i, getVertex(i).withColor(color));
        }
        return this;
    }
    
    @Override
    public void multiplyColor(int color)
    {
        this.setColor(QuadHelper.multiplyColor(this.getColor(), color));
        for(int i = 0; i < this.vertexCount(); i++)
        {
            Vertex v = this.getVertex(i);
            if(v != null)
            {
                int vColor = QuadHelper.multiplyColor(color, v.color);
                this.setVertex(i, v.withColor(vColor));
            }
        }
    }

    /** 
     * Using this instead of referencing vertex array directly.
     */
    private void setVertex(int index, Vertex vertexIn)
    {
        this.vertices[index] = vertexIn;
    }

    /**
     * Enforces immutability of vertex geometry once a vertex is added
     * by rejecting any attempt to set a vertex that already exists.
     * Rejection is via an assertion, so no overhead in normal use.
     */
    @Override
    public void addVertex(int index, Vertex vertexIn)
    {
        assert this.vertices[index] == null : "attempt to change existing vertex";
        this.vertices[index] = vertexIn;
    }
    
    @Override
    public Vertex getVertex(int index)
    {
        if(this.vertexCount() == 3 && index == 3) return this.vertices[2];
        return this.vertices[index];
    }

    @Override
    public void setLineID(int index, int lineID)
    {
        if(index < this.vertexCount)
        {
            this.lineID[index] = lineID;
        }
    }

    @Override
    public int getLineID(int index)
    {
        if(this.vertexCount() == 3 && index == 3) return this.lineID[2];
        return this.lineID[index];
    }

    @Override
    public int getAncestorQuadID()
    {
        return this.ancestorQuadID;
    }

    @Override
    public int getAncestorQuadIDForDescendant()
    {
        return this.getAncestorQuadID() == ICSGPolygon.IS_AN_ANCESTOR ? this.quadID() : this.getAncestorQuadID();
    }

    /**
     * Initializes metadata values that can be used for CSG operations.
     * Has no effect on polygon geometry or appearance.<p>
     * 
     * So while it technically does mutate this object, generally
     * not necessary to clone this object before using it.<p>
     * 
     * Returns self as convenience.
     */
    private void setupCsgMetadata()
    {
        this.setAncestorQuadID(ICSGPolygon.IS_AN_ANCESTOR);
        for(int i = 0; i < this.vertexCount(); i++)
        {
            this.lineID[i] = CSGPlane.nextOutsideLineID.getAndDecrement();
        }
    }
    
    @Override
    public ICSGPolygon toCSG()
    {
        PolyImpl result = this.mutableCopy();
        result.setupCsgMetadata();
        return result;
    }

    @Override
    public void assignLockedUVCoordinates()
    {
        EnumFacing face = getNominalFace();
        if(face == null)
        {
            assert false : "RawQuad.assignLockedUVCoordinates encountered null nominal face.  Should not occur.  Using normal face instead.";
            face = getNormalFace();
        }

        for(int i = 0; i < this.vertexCount(); i++)
        {
            Vertex v = getVertex(i);
            
            switch(face)
            {
            case EAST:
                this.setVertex(i, v.withUV((1 - v.z) * 16, (1 - v.y) * 16));
                break;
                
            case WEST:
                this.setVertex(i, v.withUV(v.z * 16, (1 - v.y) * 16));
                break;
                
            case NORTH:
                this.setVertex(i, v.withUV((1 - v.x) * 16, (1 - v.y) * 16));
                break;
                
            case SOUTH:
                this.setVertex(i, v.withUV(v.x * 16, (1 - v.y) * 16));
                break;
                
            case DOWN:
                this.setVertex(i, v.withUV(v.x * 16, (1 - v.z) * 16));
                break;
                
            case UP:
                // our default semantic for UP is different than MC
                // "top" is north instead of south
                this.setVertex(i, v.withUV(v.x * 16, v.z * 16));
                break;
            
            }
        }
        
    }

    @Override
    public void scaleQuadUV(float uScale, float vScale)
    {
        this.minU *= uScale;
        this.maxU *= uScale;
        this.minV *= vScale;
        this.maxV *= vScale;
    }
    

    @Override
    public void scaleFromBlockCenter(float scale)
    {
        float c = 0.5f * (1-scale);
        
        for(int i = 0; i < this.vertexCount(); i++)
        {
            Vertex v = getVertex(i);
            this.setVertex(i, v.withXYZ(v.x * scale + c, v.y * scale + c, v.z * scale + c));
        }
    }

    @Override
    public boolean hasFaceNormal()
    {
        return  this.faceNormal != null;
    }
    
    @Override
    public Vec3d getFaceNormal()
    {
        if(this.faceNormal == null) this.faceNormal = computeFaceNormal();
        return this.faceNormal;
    }
    


    @Override
    public String toString()
    {
        String result = "id: " + this.quadID() + " face: " + this.getNominalFace();
        for(int i = 0; i < vertexCount(); i++)
        {
            result += " v" + i + ": " + this.getVertex(i).toString();
            result += " l" + i + ": " + this.getLineID(i);
        }
        return result;
    }


    @Override
    public @Nullable EnumFacing getNominalFace()
    {
        return nominalFace;
    }

    @Override
    public EnumFacing setNominalFace(EnumFacing face)
    {
        this.nominalFace = face;
        return face;
    }
    
    @Override
    public IMutablePolygon setSurfaceInstance(SurfaceInstance surfaceInstance)
    {
        this.surfaceInstance = surfaceInstance;
        return this;
    }
    
    @Override
    public void transform(Matrix4f matrix)
    {
        // transform vertices
        for(int i = 0; i < this.vertexCount; i++)
        {
            Vertex vertex = this.getVertex(i);
            Vector4f temp = new Vector4f(vertex.x, vertex.y, vertex.z, 1);
            matrix.transform(temp);
            this.setVertex(i, vertex.withXYZ((float)temp.x, (float)temp.y, (float)temp.z));
        }
        
        // transform nominal face
        // our matrix transform has block center as its origin,
        // so need to translate face vectors to/from block center 
        // origin before/applying matrix.
        if(this.nominalFace != null)
        {
            Vec3i curNorm = this.nominalFace.getDirectionVec();
            Vector4f newFaceVec = new Vector4f(curNorm.getX() + 0.5f, curNorm.getY() + 0.5f, curNorm.getZ() + 0.5f, 1);
            matrix.transform(newFaceVec);
            newFaceVec.x -= 0.5;
            newFaceVec.y -= 0.5;
            newFaceVec.z -= 0.5;
            this.setNominalFace(QuadHelper.computeFaceForNormal(newFaceVec));
        }
    }

    @Override
    public void setVertexColor(int index, int vColor)
    {
        this.setVertex(index, this.getVertex(index).withColor(vColor));        
    }

    @Override
    public @Nullable String getTextureName()
    {
        return textureName;
    }

    @Override
    public void setTextureName(@Nullable String textureName)
    {
        this.textureName = textureName;
    }

    @Override
    public Rotation getRotation()
    {
        return rotation;
    }

    @Override
    public void setRotation(Rotation rotation)
    {
        this.rotation = rotation;
    }

    @Override
    public int getColor()
    {
        return color;
    }

    @Override
    public void setColor(int color)
    {
        this.color = color;
    }

    @Override
    public boolean isFullBrightness()
    {
        return isFullBrightness;
    }

    @Override
    public void setFullBrightness(boolean isFullBrightness)
    {
        this.isFullBrightness = isFullBrightness;
    }

    @Override
    public boolean isLockUV()
    {
        return isLockUV;
    }

    @Override
    public void setLockUV(boolean isLockUV)
    {
        this.isLockUV = isLockUV;
    }

    @Override
    public boolean shouldContractUVs()
    {
        return shouldContractUVs;
    }

    @Override
    public void setShouldContractUVs(boolean shouldContractUVs)
    {
        this.shouldContractUVs = shouldContractUVs;
    }

    @Override
    public RenderPass getRenderPass()
    {
        return renderPass;
    }

    @Override
    public void setRenderPass(RenderPass renderPass)
    {
        this.renderPass = renderPass;
    }

    @Override
    public SurfaceInstance getSurfaceInstance()
    {
        return surfaceInstance;
    }

    @Override
    public float getMinU()
    {
        return minU;
    }

    @Override
    public void setMinU(float minU)
    {
        this.minU = minU;
    }

    @Override
    public float getMaxU()
    {
        return maxU;
    }

    @Override
    public void setMaxU(float maxU)
    {
        this.maxU = maxU;
    }

    @Override
    public float getMinV()
    {
        return minV;
    }

    @Override
    public void setMinV(float minV)
    {
        this.minV = minV;
    }

    @Override
    public float getMaxV()
    {
        return maxV;
    }

    @Override
    public void setMaxV(float maxV)
    {
        this.maxV = maxV;
    }

    @Override
    public boolean isInverted()
    {
        return isInverted;
    }

    protected void setInverted(boolean isInverted)
    {
        this.isInverted = isInverted;
    }

    @Override
    public int quadID()
    {
        return quadID;
    }

    @Override
    public void setAncestorQuadID(int ancestorQuadID)
    {
        this.ancestorQuadID = ancestorQuadID;
    }
}