package grondag.exotic_matter.model.primitives;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector4f;

import grondag.exotic_matter.model.CSG.CSGNode.Root;
import grondag.exotic_matter.varia.ColorHelper;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3i;

public class PolyImpl extends AbstractPolygon implements IMutablePolygon
{
    private final Vertex[] vertices;
    private final int vertexCount;

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
    }

    public PolyImpl(IPolygon template, int vertexCount)
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
        
        for(int i = 0; i < c; i++)
        {
            this.setVertex(i, template.getVertex(i));
        }
    }

    @Override
    public int vertexCount()
    {
        return this.vertexCount;
    }

    @Override
    protected void copyProperties(IPolygon fromObject)
    {
        super.copyProperties(fromObject);
//        this.tag = fromObject.getTag();
    }

    @Override
    public void toQuads(Consumer<IPolygon> target, boolean ensureConvex)
    {
        if(this.vertexCount <= 4 && (!ensureConvex || this.isConvex()))
        {
            target.accept(this);
            return;
        }
        

        int head = vertexCount - 1;
        int tail = 2;
        PolyImpl work = new PolyImpl(this, 4);
        work.setVertex(0, this.getVertex(head));
        work.setVertex(1, this.getVertex(0));
        work.setVertex(2, this.getVertex(1));
        work.setVertex(3, this.getVertex(tail));
        target.accept(work);

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
            if(ensureConvex && !work.isConvex())
            {
                work.toTris(target);
            }
            else target.accept(work);
        }
    }
    

    @Override
    public IMutablePolygon setupFaceQuad(FaceVertex vertexIn0, FaceVertex vertexIn1, FaceVertex vertexIn2, FaceVertex vertexIn3, @Nullable EnumFacing topFace)
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
            setVertex(0, new Vertex(rv0.x, 1-rv0.depth, 1-rv0.y, rv0.u(), rv0.v(), rv0.color(this.getColor()), rv0.glow()));
            setVertex(1, new Vertex(rv1.x, 1-rv1.depth, 1-rv1.y, rv1.u(), rv1.v(), rv1.color(this.getColor()), rv1.glow()));
            setVertex(2, new Vertex(rv2.x, 1-rv2.depth, 1-rv2.y, rv2.u(), rv2.v(), rv2.color(this.getColor()), rv2.glow()));
            if(this.vertexCount == 4) setVertex(3, new Vertex(rv3.x, 1-rv3.depth, 1-rv3.y, rv3.u(), rv3.v(), rv3.color(this.getColor()), rv3.glow()));
            break;

        case DOWN:     
            setVertex(0, new Vertex(rv0.x, rv0.depth, rv0.y, 1-rv0.u(), 1-rv0.v(), rv0.color(this.getColor()), rv0.glow()));
            setVertex(1, new Vertex(rv1.x, rv1.depth, rv1.y, 1-rv1.u(), 1-rv1.v(), rv1.color(this.getColor()), rv1.glow()));
            setVertex(2, new Vertex(rv2.x, rv2.depth, rv2.y, 1-rv2.u(), 1-rv2.v(), rv2.color(this.getColor()), rv2.glow()));
            if(this.vertexCount == 4) setVertex(3, new Vertex(rv3.x, rv3.depth, rv3.y, 1-rv3.u(), 1-rv3.v(), rv3.color(this.getColor()), rv3.glow()));
            break;

        case EAST:
            setVertex(0, new Vertex(1-rv0.depth, rv0.y, 1-rv0.x, rv0.u(), rv0.v(), rv0.color(this.getColor()), rv0.glow()));
            setVertex(1, new Vertex(1-rv1.depth, rv1.y, 1-rv1.x, rv1.u(), rv1.v(), rv1.color(this.getColor()), rv1.glow()));
            setVertex(2, new Vertex(1-rv2.depth, rv2.y, 1-rv2.x, rv2.u(), rv2.v(), rv2.color(this.getColor()), rv2.glow()));
            if(this.vertexCount == 4) setVertex(3, new Vertex(1-rv3.depth, rv3.y, 1-rv3.x, rv3.u(), rv3.v(), rv3.color(this.getColor()), rv3.glow()));
            break;

        case WEST:
            setVertex(0, new Vertex(rv0.depth, rv0.y, rv0.x, rv0.u(), rv0.v(), rv0.color(this.getColor()), rv0.glow()));
            setVertex(1, new Vertex(rv1.depth, rv1.y, rv1.x, rv1.u(), rv1.v(), rv1.color(this.getColor()), rv1.glow()));
            setVertex(2, new Vertex(rv2.depth, rv2.y, rv2.x, rv2.u(), rv2.v(), rv2.color(this.getColor()), rv2.glow()));
            if(this.vertexCount == 4) setVertex(3, new Vertex(rv3.depth, rv3.y, rv3.x, rv3.u(), rv3.v(), rv3.color(this.getColor()), rv3.glow()));
            break;

        case NORTH:
            setVertex(0, new Vertex(1-rv0.x, rv0.y, rv0.depth, rv0.u(), rv0.v(), rv0.color(this.getColor()), rv0.glow()));
            setVertex(1, new Vertex(1-rv1.x, rv1.y, rv1.depth, rv1.u(), rv1.v(), rv1.color(this.getColor()), rv1.glow()));
            setVertex(2, new Vertex(1-rv2.x, rv2.y, rv2.depth, rv2.u(), rv2.v(), rv2.color(this.getColor()), rv2.glow()));
            if(this.vertexCount == 4) setVertex(3, new Vertex(1-rv3.x, rv3.y, rv3.depth, rv3.u(), rv3.v(), rv3.color(this.getColor()), rv3.glow()));
            break;

        case SOUTH:
            setVertex(0, new Vertex(rv0.x, rv0.y, 1-rv0.depth, rv0.u(), rv0.v(), rv0.color(this.getColor()), rv0.glow()));
            setVertex(1, new Vertex(rv1.x, rv1.y, 1-rv1.depth, rv1.u(), rv1.v(), rv1.color(this.getColor()), rv1.glow()));
            setVertex(2, new Vertex(rv2.x, rv2.y, 1-rv2.depth, rv2.u(), rv2.v(), rv2.color(this.getColor()), rv2.glow()));
            if(this.vertexCount == 4) setVertex(3, new Vertex(rv3.x, rv3.y, 1-rv3.depth, rv3.u(), rv3.v(), rv3.color(this.getColor()), rv3.glow()));
            break;
        }

        return this;
    }


    @Override
    public IMutablePolygon setupFaceQuad(EnumFacing side, FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, FaceVertex tv3, @Nullable EnumFacing topFace)
    {
        assert(this.vertexCount() == 4);
        this.setNominalFace(side);
        return this.setupFaceQuad(tv0, tv1, tv2, tv3, topFace);
    }

    @Override
    public IMutablePolygon setupFaceQuad(float x0, float y0, float x1, float y1, float depth, @Nullable EnumFacing topFace)
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
    public IMutablePolygon setupFaceQuad(EnumFacing face, float x0, float y0, float x1, float y1, float depth, @Nullable EnumFacing topFace)
    {
        assert(this.vertexCount() == 4);
        this.setNominalFace(face);
        return this.setupFaceQuad(x0, y0, x1, y1, depth, topFace);
    }

    @Override
    public IMutablePolygon setupFaceQuad(EnumFacing side, FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, @Nullable EnumFacing topFace)
    {
        assert(this.vertexCount() == 3);
        this.setNominalFace(side);
        return this.setupFaceQuad(tv0, tv1, tv2, tv2, topFace);
    }


    @Override
    public IMutablePolygon setupFaceQuad(FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, @Nullable EnumFacing topFace)
    {
        assert(this.vertexCount() == 3);
        return this.setupFaceQuad(tv0, tv1, tv2, tv2, topFace);
    }

    @Override
    public void setVertexNormal(int index, float x, float y, float z)
    {
        if(index < this.vertexCount)
        {
            this.setVertex(index, ((Vertex)this.getVertex(index)).withNormal(x, y, z));
        }
    }
    
    @Override
    public void setVertexNormal(int index, Vec3f normal)
    {
        this.setVertexNormal(index, normal.x, normal.y, normal.z);
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
            setVertex(i, ((Vertex)getVertex(i)).withColor(color));
        }
        return this;
    }
    
    @Override
    public void multiplyColor(int color)
    {
        super.multiplyColor(color);
        for(int i = 0; i < this.vertexCount(); i++)
        {
            Vertex v = (Vertex)this.getVertex(i);
            int vColor = ColorHelper.multiplyColor(color, v.color);
            this.setVertex(i, v.withColor(vColor));
        }
    }

    /** 
     * Using this instead of referencing vertex array directly.
     */
    @Override
    public void setVertex(int index, Vertex vertexIn)
    {
        this.vertices[index] =  vertexIn;
    }

    @Override
    public void mapEachVertex(Function<Vertex, Vertex> mapper)
    {
        for(int i = 0; i < this.vertexCount; i++)
        {
            final Vertex input = this.vertices[i];
            final Vertex result = mapper.apply(input);
            if(result != input) this.vertices[i] = result;
        }
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
    public void addVertex(int index, float x, float y, float z, float u, float v, int color)
    {
        this.addVertex(index, new Vertex(x, y, z, u, v, color));
    }
    
    @Override
    public void addVertex(int index, float x, float y, float z, float u, float v, int color, Vec3f normal)
    {
        this.addVertex(index, new Vertex(x, y, z, u, v, color, normal));
    }
    
    @Override
    public void addVertex(int index, float x, float y, float z, float u, float v, int color, float normalX, float normalY, float normalZ)
    {
        this.addVertex(index, new Vertex(x, y, z, u, v, color, normalX, normalY, normalZ));
    }
    
    @Override
    public Vertex getVertex(int index)
    {
        if(this.vertexCount() == 3 && index == 3) return this.vertices[2];
        return this.vertices[index];
    }

    @Override
    public void assignLockedUVCoordinates()
    {
        EnumFacing face = getNominalFace();

        for(int i = 0; i < this.vertexCount(); i++)
        {
            this.setVertex(i, this.getVertex(i).uvLocked(face));
        }
    }
    
    @Override
    public void scaleFromBlockCenter(float scale)
    {
        float c = 0.5f * (1-scale);
        
        for(int i = 0; i < this.vertexCount(); i++)
        {
            Vertex v = (Vertex)getVertex(i);
            this.setVertex(i, v.withXYZ(v.x * scale + c, v.y * scale + c, v.z * scale + c));
        }
    }


    @Override
    public String toString()
    {
        String result = "face: " + this.getNominalFace();
        for(int i = 0; i < vertexCount(); i++)
        {
            result += " v" + i + ": " + this.getVertex(i).toString();
        }
        return result;
    }

    @Override
    public void transform(Matrix4f matrix)
    {
        // transform vertices
        for(int i = 0; i < this.vertexCount; i++)
        {
            Vertex vertex = (Vertex)this.getVertex(i);
            Vector4f temp = new Vector4f(vertex.x, vertex.y, vertex.z, 1);
            matrix.transform(temp);
            this.setVertex(i, vertex.withXYZ((float)temp.x, (float)temp.y, (float)temp.z));
        }
        
        // transform nominal face
        // our matrix transform has block center as its origin,
        // so need to translate face vectors to/from block center 
        // origin before/applying matrix.
        final EnumFacing nomFace = this.getNominalFace();
        Vec3i curNorm = nomFace.getDirectionVec();
        Vector4f newFaceVec = new Vector4f(curNorm.getX() + 0.5f, curNorm.getY() + 0.5f, curNorm.getZ() + 0.5f, 1);
        matrix.transform(newFaceVec);
        newFaceVec.x -= 0.5;
        newFaceVec.y -= 0.5;
        newFaceVec.z -= 0.5;
        this.setNominalFace(QuadHelper.computeFaceForNormal(newFaceVec));
    }

    @Override
    public void setVertexColor(int index, int vColor)
    {
        this.setVertex(index, ((Vertex)this.getVertex(index)).withColor(vColor));        
    }

    @Override
    public Vertex[] vertexArray()
    {
        return this.vertices;
    }

    @Override
    public void addTrisToCSGRoot(Root root)
    {
        if(this.vertexCount == 3)
        {
            root.addPolygon(this);
        }
        else
        {
            int head = this.vertexCount - 1;
            int tail = 1;

            PolyImpl work = new PolyImpl(this, 3);
            work.setVertex(0, this.getVertex(head));
            work.setVertex(1, this.getVertex(0));
            work.setVertex(2, this.getVertex(tail));
            root.addPolygon(work);

            while(head - tail > 1)
            {
                work = new PolyImpl(this, 3);
                work.setVertex(0, this.getVertex(head));
                work.setVertex(1, this.getVertex(tail));
                work.setVertex(2, this.getVertex(++tail));
                root.addPolygon(work);

                if(head - tail > 1)
                {
                    work = new PolyImpl(this, 3);
                    work.setVertex(0, this.getVertex(head));
                    work.setVertex(1, this.getVertex(tail));
                    work.setVertex(2, this.getVertex(--head));
                    root.addPolygon(work);
                }
            }
        }        
    }

    @Override
    public void toTris(Consumer<IPolygon> target)
    {
        // UGLY: egregious hack is egregious
        // is copy pasta of CSG version - couldn't be buggered at the time
        // probably the right way is to accept a collection interface
        // and implement that in CSG root
        if(this.vertexCount == 3)
        {
            target.accept(this);
        }
        else
        {
            int head = this.vertexCount - 1;
            int tail = 1;

            PolyImpl work = new PolyImpl(this, 3);
            work.setVertex(0, this.getVertex(head));
            work.setVertex(1, this.getVertex(0));
            work.setVertex(2, this.getVertex(tail));
            target.accept(work);

            while(head - tail > 1)
            {
                work = new PolyImpl(this, 3);
                work.setVertex(0, this.getVertex(head));
                work.setVertex(1, this.getVertex(tail));
                work.setVertex(2, this.getVertex(++tail));
                target.accept(work);

                if(head - tail > 1)
                {
                    work = new PolyImpl(this, 3);
                    work.setVertex(0, this.getVertex(head));
                    work.setVertex(1, this.getVertex(tail));
                    work.setVertex(2, this.getVertex(--head));
                    target.accept(work);
                }
            }
        }
    }

    @Override
    public final void offsetVertexUV(float uShift, float vShift)
    {
        for(int i = 0; i < this.vertexCount; i++)
        {
            Vertex v = this.vertices[i];
            v = v.withUV(v.u + uShift, v.v + vShift);
            
            assert v.u > -QuadHelper.EPSILON : "vertex uv offset out of bounds"; 
            assert v.u < 1 + QuadHelper.EPSILON : "vertex uv offset out of bounds"; 
            assert v.v > -QuadHelper.EPSILON : "vertex uv offset out of bounds"; 
            assert v.v < 1 + QuadHelper.EPSILON : "vertex uv offset out of bounds";

            this.vertices[i] = v;
        }
    }
    
    @Override
    public void forEachVertex(Consumer<Vertex> consumer)
    {
        for(int i = 0; i < this.vertexCount; i++)
        {
            consumer.accept(this.vertices[i]);
        }
    }

    @Override
    public IMutablePolygon getParent()
    {
        return this;
    }

    @Override
    public IPaintableVertex getPaintableVertex(int i)
    {
        return this.getVertex(i);
    }

    @Override
    public int layerCount()
    {
        return 1;
    }

    @Override
    public IPaintableQuad paintableCopy()
    {
        return Poly.mutableCopyOf(this);
    }

    @Override
    public IPaintableQuad paintableCopy(int vertexCount)
    {
        return Poly.mutable(this, vertexCount);
    }

    @Override
    public void setVertex(int i, IPaintableVertex v)
    {
        this.setVertex(i, (Vertex)v);
    }

    @Override
    public void toPaintableQuads(Consumer<IPaintableQuad> consumer, boolean ensureConvex)
    {
        this.toQuads(q -> consumer.accept((IPaintableQuad) q), ensureConvex);
    }
    
//    private String tag;
//
//    @Override
//    public String getTag()
//    {
//        return tag;
//    }
//
//    @Override
//    public PolyImpl setTag(String tag)
//    {
//        this.tag = tag;
//        return this;
//    }
}