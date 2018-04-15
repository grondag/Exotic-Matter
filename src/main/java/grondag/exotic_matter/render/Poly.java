package grondag.exotic_matter.render;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4d;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector4f;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.render.Surface.SurfaceInstance;
import grondag.exotic_matter.varia.Color;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class Poly implements IPolygon
{
    private Vertex[] vertices;
    private @Nullable Vec3d faceNormal;
    private final int vertexCount;

    private @Nullable EnumFacing nominalFace;
    private @Nullable String textureName;
    
    /** 
     * Causes texture to appear rotated within the frame
     * of this texture. Relies on UV coordinates
     * being in the range 0-16. <br><br>
     * 
     * Rotation happens during quad bake.
     * If lockUV is true, rotation happens after UV
     * coordinates are derived.
     */
    private Rotation rotation = Rotation.ROTATE_NONE;
    
    private int color = Color.WHITE;
    
    private boolean isFullBrightness = false;
    
    /** 
     * If true then quad painters will ignore UV coordinates and instead set
     * based on projection of vertices onto the given nominal face.
     * Note that FaceVertex does this by default even if lockUV is not specified.
     * To get unlockedUV coordiates, specificy a face using FaceVertex.UV or FaceVertex.UVColored.
     */
    private boolean isLockUV = false;
    

    private boolean shouldContractUVs = true;
    
    private RenderPass renderPass = RenderPass.SOLID_SHADED;
    private SurfaceInstance surfaceInstance = IPolygon.NO_SURFACE;

    private float minU = 0;
    private float maxU = 16;
    private float minV = 0;
    private float maxV = 16;

    private static AtomicInteger nextQuadID = new AtomicInteger(1);
    private boolean isInverted = false;
    private final int quadID = nextQuadID.incrementAndGet();
    private int ancestorQuadID = IPolygon.NO_ID;
    private int[] lineID;

    public Poly()
    {
        this(4);
    }

    public Poly(Poly template)
    {
        this(template, template.vertexCount());
    }

    public Poly(int vertexCount)
    {
        assert vertexCount > 2 : "Bad polygon structure.";
        this.vertexCount = vertexCount;
        this.vertices = new Vertex[vertexCount];
        this.lineID = new int[vertexCount];
    }

    public Poly(Poly template, int vertexCount)
    {
        this(vertexCount);
        this.copyProperties(template);
    }

    @Override
    public Poly clone()
    {
        Poly retval = new Poly(this);
        retval.copyVertices(this);
        return retval;
    }

    protected void copyVertices(Poly template)
    {
        final int c = this.vertexCount();
        for(int i = 0; i < c; i++)
        {
            this.setVertex(i, template.getVertex(i));
            this.lineID[i] = template.lineID[i];
        }
    }

    @Override
    public int vertexCount()
    {
        return this.vertexCount;
    }

    private void copyProperties(Poly fromObject)
    {
        this.setNominalFace(fromObject.getNominalFace());
        this.setTextureName(fromObject.getTextureName());
        this.setRotation(fromObject.getRotation());
        this.setColor(fromObject.getColor());
        this.setFullBrightness(fromObject.isFullBrightness());
        this.setLockUV(fromObject.isLockUV());
        this.setAncestorQuadID(fromObject.getAncestorQuadID());
        this.setInverted(fromObject.isInverted());
        this.faceNormal = fromObject.faceNormal;
        this.setShouldContractUVs(fromObject.shouldContractUVs());
        this.setMinU(fromObject.getMinU());
        this.setMaxU(fromObject.getMaxU());
        this.setMinV(fromObject.getMinV());
        this.setMaxV(fromObject.getMaxV());
        this.setRenderPass(fromObject.getRenderPass());
        this.setSurfaceInstance(fromObject.getSurfaceInstance());
    }

    public List<Poly> toQuads()
    {
        ArrayList<Poly> retVal = new ArrayList<Poly>();

        if(this.vertexCount <= 4)
        {
            retVal.add(this);
        }
        else
        {
            int head = vertexCount - 1;
            int tail = 2;
            Poly work = new Poly(this, 4);
            work.setVertex(0, this.getVertex(head));
            work.setVertex(1, this.getVertex(0));
            work.setVertex(2, this.getVertex(1));
            work.setVertex(3, this.getVertex(tail));
            retVal.add(work);

            while(head - tail > 1)
            {
                work = new Poly(this, head - tail == 2 ? 3 : 4);
                work.setVertex(0, this.getVertex(head));
                work.setVertex(1, this.getVertex(tail));
                work.setVertex(2, this.getVertex(++tail));
                if(head - tail > 1)
                {
                    work.setVertex(3, this.getVertex(--head));
                }
                retVal.add(work);
            }
        }
        return retVal;
    }

    /** 
     * If this is a quad, returns two new tris.
     * If is already a tri, returns copy of self.<p>
     * 
     * Does not mutate this object and does not 
     * retain any reference to polygons in this object.
     */
    public List<Poly> toTris()
    {

        if(this.vertexCount() == 3)
        {
            return ImmutableList.of(this.clone());
        }
        else
        {
            ArrayList<Poly>  retVal= new ArrayList<Poly>(this.vertexCount()-2);
            int splitLineID = CSGPlane.nextInsideLineID.getAndIncrement();
            int head = vertexCount - 1;
            int tail = 1;

            Poly work = new Poly(this, 3);
            work.setVertex(0, this.getVertex(head));
            work.setLineID(0, this.getLineID(head));
            work.setVertex(1, this.getVertex(0));
            work.setLineID(1, this.getLineID(0));
            work.setVertex(2, this.getVertex(tail));
            work.setLineID(2, splitLineID);
            work.setAncestorQuadID(this.getAncestorQuadIDForDescendant());
            retVal.add(work);

            while(head - tail > 1)
            {
                work = new Poly(this, 3);
                work.setVertex(0, this.getVertex(head));
                work.setLineID(0, splitLineID);
                work.setVertex(1, this.getVertex(tail));
                work.setLineID(1, this.getLineID(tail));
                splitLineID = CSGPlane.nextInsideLineID.getAndIncrement();
                work.setVertex(2, this.getVertex(++tail));
                work.setLineID(2, head - tail == 1 ? this.getLineID(tail): splitLineID);
                work.setAncestorQuadID(this.getAncestorQuadIDForDescendant());
                retVal.add(work);

                if(head - tail > 1)
                {
                    work = new Poly(this, 3);
                    work.setVertex(0, this.getVertex(head));
                    work.setLineID(0, splitLineID);
                    splitLineID = CSGPlane.nextInsideLineID.getAndIncrement();
                    work.setVertex(1, this.getVertex(tail));
                    work.setLineID(1, head - tail == 1 ? this.getLineID(tail): splitLineID);
                    work.setVertex(2, this.getVertex(--head).clone());
                    work.setLineID(2, this.getLineID(head));
                    work.setAncestorQuadID(this.getAncestorQuadIDForDescendant());
                    retVal.add(work);
                }
            }
            return retVal;
        }

    }

    /**
     * Reverses winding order of this quad and returns itself
     */
    public Poly invert()
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

    /** 
     * Sets up a quad with human-friendly semantics. <br><br>
     * 
     * topFace establishes a reference for "up" in these semantics. If null, will use default.
     * Depth represents how far recessed into the surface of the face the quad should be. <br><br>
     * 
     * Vertices should be given counter-clockwise.
     * Ordering of vertices is maintained for future references.
     * (First vertex passed in will be vertex 0, for example.) <br><br>
     * 
     * UV coordinates will be based on where rotated vertices project onto the nominal 
     * face for this quad (effectively lockedUV) unless face vertexes have UV coordinates.
     */
    public Poly setupFaceQuad(FaceVertex vertexIn0, FaceVertex vertexIn1, FaceVertex vertexIn2, FaceVertex vertexIn3, EnumFacing topFace)
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

    /**
     * Same as {@link #setupFaceQuad(FaceVertex, FaceVertex, FaceVertex, FaceVertex, EnumFacing)}
     * except also sets nominal face to the given face in the start parameter. 
     * Returns self for convenience.
     */
    public Poly setupFaceQuad(EnumFacing side, FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, FaceVertex tv3, EnumFacing topFace)
    {
        assert(this.vertexCount() == 4);
        this.setNominalFace(side);
        return this.setupFaceQuad(tv0, tv1, tv2, tv3, topFace);
    }

    /** 
     * Sets up a quad with standard semantics.  
     * x0,y0 are at lower left and x1, y1 are top right.
     * topFace establishes a reference for "up" in these semantics.
     * Depth represents how far recessed into the surface of the face the quad should be.<br><br>
     * 
     * Returns self for convenience.<br><br>
     * 
     * @see #setupFaceQuad(FaceVertex, FaceVertex, FaceVertex, FaceVertex, EnumFacing)
     */
    public Poly setupFaceQuad(float x0, float y0, float x1, float y1, float depth, EnumFacing topFace)
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


    /**
     * Same as {@link #setupFaceQuad(double, double, double, double, double, EnumFacing)}
     * but also sets nominal face with given face in start parameter.  
     * Returns self as convenience.
     */
    public Poly setupFaceQuad(EnumFacing face, float x0, float y0, float x1, float y1, float depth, EnumFacing topFace)
    {
        assert(this.vertexCount() == 4);
        this.setNominalFace(face);
        return this.setupFaceQuad(x0, y0, x1, y1, depth, topFace);
    }

    public Poly setupFaceQuad(EnumFacing face, double x0, double y0, double x1, double y1, double depth, EnumFacing topFace)
    {
        return this.setupFaceQuad(face, (float)x0, (float)y0, (float)x1, (float)y1, (float)depth, topFace);
    }
    
    /**
     * Triangular version of {@link #setupFaceQuad(EnumFacing, FaceVertex, FaceVertex, FaceVertex, EnumFacing)}
     */
    public Poly setupFaceQuad(EnumFacing side, FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, EnumFacing topFace)
    {
        assert(this.vertexCount() == 3);
        this.setNominalFace(side);
        return this.setupFaceQuad(tv0, tv1, tv2, tv2, topFace);
    }

    /**
     * Triangular version of {@link #setupFaceQuad(FaceVertex, FaceVertex, FaceVertex, FaceVertex, EnumFacing)}
     */
    public Poly setupFaceQuad(FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, EnumFacing topFace)
    {
        assert(this.vertexCount() == 3);
        return this.setupFaceQuad(tv0, tv1, tv2, tv2, topFace);
    }

    /** Using this instead of method on vertex 
     * ensures normals are set correctly for tris.
     */
    public void setVertexNormal(int index, float x, float y, float z)
    {
        if(index < this.vertexCount)
        {
            this.setVertex(index, this.getVertex(index).withNormal(x, y, z));
        }
    }
    
    public void setVertexNormal(int index, Vec3d normal)
    {
        this.setVertexNormal(index, (float)normal.x, (float)normal.y, (float)normal.z);
    }

    /**
     * Changes all vertices and quad color to new color and returns itself
     */
    public Poly replaceColor(int color)
    {
        this.setColor(color);
        for(int i = 0; i < this.vertexCount(); i++)
        {
            if(getVertex(i) != null) setVertex(i, getVertex(i).withColor(color));
        }
        return this;
    }
    

    /**
     * Multiplies all vertex color by given color and returns itself
     */
    public Poly multiplyColor(int color)
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
        return this;
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

    public long getQuadID()
    {
        return this.quadID();
    }
    
    public static int LINE_NOT_FOUND = -1;
    /**
     * Returns the index of the edge with the given line ID.
     * Returns LINE_NOT_FOUND if, uh, you know, it wasn't checked.
     */
    public int findLineIndex(long lineID)
    {
        for(int i = 0; i < this.vertexCount(); i++)
        {
            if(this.getLineID(i) == lineID)
            {
                return i;
            }
        }
        return LINE_NOT_FOUND;
    }

    @Override
    public int getAncestorQuadID()
    {
        return this.ancestorQuadID;
    }

    public int getAncestorQuadIDForDescendant()
    {
        return this.getAncestorQuadID() == IPolygon.IS_AN_ANCESTOR ? this.quadID() : this.getAncestorQuadID();
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
    public Poly setupCsgMetadata()
    {
        this.setAncestorQuadID(IPolygon.IS_AN_ANCESTOR);
        for(int i = 0; i < this.vertexCount(); i++)
        {
            this.lineID[i] = CSGPlane.nextOutsideLineID.getAndDecrement();
        }
        return this;
    }

    //        public List<RawQuad> clipToFace(EnumFacing face, RawQuad patchTemplate)
    //        {
    //            LinkedList<RawQuad> retVal = new LinkedList<RawQuad>();
    //            for(RawTri tri : this.split())
    //            {
    //                retVal.addAll(tri.splitOnFace(face, patchTemplate));
    //            }
    //            return retVal;
    //        }

    /**
     * Returns true if this polygon is convex.
     * All Tris must be.  
     * For quads, confirms that each turn around the quad 
     * goes same way by comparing cross products of edges.
     */
    public boolean isConvex()
    {
        if(this.vertexCount() == 3) return true;

        float testX = 0;
        float testY = 0;
        float testZ = 0;
        boolean needTest = true;
                
        for(int thisIndex = 0; thisIndex < this.vertexCount(); thisIndex++)
        {
            int nextIndex = thisIndex + 1;
            if(nextIndex == this.vertexCount()) nextIndex = 0;

            int priorIndex = thisIndex - 1;
            if(priorIndex == -1) priorIndex = this.vertexCount() - 1;

            final Vertex thisVertex =  getVertex(thisIndex);
            final Vertex nextVertex = getVertex(nextIndex);
            final Vertex priorVertex = getVertex(priorIndex);
            
            final float ax = thisVertex.x - priorVertex.x;
            final float ay = thisVertex.y - priorVertex.y;
            final float az = thisVertex.z - priorVertex.z;
            
            final float bx = nextVertex.x - thisVertex.x;
            final float by = nextVertex.y - thisVertex.y;
            final float bz = nextVertex.z - thisVertex.z;

//            Vec3d lineA = getVertex(thisIndex).subtract(getVertex(priorIndex));
//            Vec3d lineB = getVertex(nextIndex).subtract(getVertex(thisIndex));
            
            final float crossX = ay * bz - az * by;
            final float crossY = az * bx - ax * bz;
            final float crossZ = ax * by - ay * bx;
            
            if(needTest)
            {
                needTest = false;
                testX = crossX;
                testY = crossY;
                testZ = crossZ;
            }
            else if(testX * crossX  + testY * crossY + testZ * crossZ < 0) 
            {
                return false;
            }
        }
        return true;
    }

    protected boolean isOrthogonalTo(EnumFacing face)
    {
        return Math.abs(this.getFaceNormal().dotProduct(new Vec3d(face.getDirectionVec()))) <= QuadHelper.EPSILON;
    }

    public boolean isOnSinglePlane()
    {
        if(this.vertexCount() == 3) return true;

        Vec3d fn = this.getFaceNormal();
        if(fn == null) return false;

        float faceX = (float) fn.x;
        float faceY = (float) fn.y;
        float faceZ = (float) fn.z;

        Vertex first = this.getVertex(0);
        
        for(int i = 3; i < this.vertexCount(); i++)
        {
            Vertex v = this.getVertex(i);
            if(v == null) return false;
            
            float dx = v.x - first.x;
            float dy = v.y - first.y;
            float dz = v.z - first.z;

            if(Math.abs(faceX * dx + faceY * dy + faceZ * dz) > QuadHelper.EPSILON) return false;
        }

        return true;
    }

    public boolean isOnFace(EnumFacing face, float tolerance)
    {
        if(face == null) return false;
        boolean retVal = true;
        for(int i = 0; i < this.vertexCount; i++)
        {
            retVal = retVal && getVertex(i).isOnFacePlane(face, tolerance);
        }
        return retVal;
    }

    /** 
     * Returns intersection point of given ray with the plane of this quad.
     * Return null if parallel or facing away.
     */
    public Vec3d intersectionOfRayWithPlane(Vec3d origin, Vec3d direction)
    {
        Vec3d normal = this.getFaceNormal();

        double directionDotNormal = normal.dotProduct(direction);
        if (Math.abs(directionDotNormal) < QuadHelper.EPSILON) 
        { 
            // parallel
            return null;
        }

        Vec3d firstPoint = this.getVertex(0).toVec3d();
        
        double distanceToPlane = -normal.dotProduct((origin.subtract(firstPoint))) / directionDotNormal;
        // facing away from plane
        if(distanceToPlane < -QuadHelper.EPSILON) return null;

        return origin.add(direction.scale(distanceToPlane));
    }
    
    /**
     * Keeping for convenience in case discover any problems with the fast version.
     * Unit tests indicate identical results.
     */
    public boolean intersectsWithRaySlow(Vec3d origin, Vec3d direction)
    {
        Vec3d intersection = this.intersectionOfRayWithPlane(origin, direction);
        
        // now we just need to test if point is inside this polygon
        return intersection == null ? false : containsPointSlow(intersection);
        
    }

    public boolean intersectsWithRay(Vec3d origin, Vec3d direction)
    {
        Vec3d intersection = this.intersectionOfRayWithPlane(origin, direction);

        // now we just need to test if point is inside this polygon
        return intersection == null ? false : containsPoint(intersection);
    }

    /**
     * Assumes the given point is on the plane of the polygon.
     * 
     * For each side, find a vector in the plane of the 
     * polygon orthogonal to the line formed by the two vertices of the edge.
     * Then take the dot product with vector formed by the start vertex and the point.
     * If the point is inside the polygon, the sign should be the same for all
     * edges, or the dot product should be very small, meaning the point is on the edge.
     */
    public boolean containsPoint(Vec3d point)
    {
        return PointInPolygonTest.isPointInRawQuad(point, this);
    }
    

    /**
     * Keeping for convenience in case discover any problems with the fast version.
     * Unit tests indicate identical results.
     */
    public boolean containsPointSlow(Vec3d point)
    {
        double lastSignum = 0;
        Vec3d faceNormal = this.getFaceNormal();

        for(int i = 0; i < this.vertexCount(); i++)
        {
            int nextVertex = i + 1;
            if(nextVertex == this.vertexCount()) nextVertex = 0;

            Vec3d currentVertex = getVertex(i).toVec3d();
            
            Vec3d line = getVertex(nextVertex).toVec3d().subtract(currentVertex);
            Vec3d normalInPlane = faceNormal.crossProduct(line);

            double sign = normalInPlane.dotProduct(point.subtract(currentVertex));

            if(lastSignum == 0)
            {
                lastSignum = Math.signum(sign);
            }
            else if(Math.signum(sign) != lastSignum)
            {
                return false;
            }
        }
        return true;
    }


    public AxisAlignedBB getAABB()
    {
        double minX = Math.min(Math.min(getVertex(0).x, getVertex(1).x), Math.min(getVertex(2).x, getVertex(3).x));
        double minY = Math.min(Math.min(getVertex(0).y, getVertex(1).y), Math.min(getVertex(2).y, getVertex(3).y));
        double minZ = Math.min(Math.min(getVertex(0).z, getVertex(1).z), Math.min(getVertex(2).z, getVertex(3).z));

        double maxX = Math.max(Math.max(getVertex(0).x, getVertex(1).x), Math.max(getVertex(2).x, getVertex(3).x));
        double maxY = Math.max(Math.max(getVertex(0).y, getVertex(1).y), Math.max(getVertex(2).y, getVertex(3).y));
        double maxZ = Math.max(Math.max(getVertex(0).z, getVertex(1).z), Math.max(getVertex(2).z, getVertex(3).z));

        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Assigns UV coordinates to each vertex by projecting vertex
     * onto plane of the quad's face. If the quad is not rotated,
     * then semantics of vertex coordinates matches those of setupFaceQuad.
     * For example, on NSEW faces, "up" (+y) corresponds to the top of the texture.
     * 
     * Assigned values are in the range 0-16, as is conventional for MC.
     */
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
     /**
      * Multiplies uvMin/Max by the given factors.
      */
    public void scaleQuadUV(double uScale, double vScale)
    {
        this.minU *= uScale;
        this.maxU *= uScale;
        this.minV *= vScale;
        this.maxV *= vScale;
    }
    
    /** 
     * Unique scale transformation of all vertex coordinates 
     * using block center (0.5, 0.5, 0.5) as origin.
     */
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
    public Vec3d getFaceNormal()
    {
        if(this.faceNormal == null) this.faceNormal = computeFaceNormal();
        return this.faceNormal;
    }
    
    private Vec3d computeFaceNormal()
    {
        try
        {
            return getVertex(2).toVec3d().subtract(getVertex(0).toVec3d()).crossProduct(getVertex(3).toVec3d().subtract(getVertex(1).toVec3d())).normalize();
        }
        catch(Exception e)
        {
            assert false : "Bad polygon structure during face normal request.";
            return Vec3d.ZERO;
        }
    }

    public float[] getFaceNormalArray()
    {
        Vec3d normal = getFaceNormal();

        float[] retval = new float[3];

        retval[0] = (float)(normal.x);
        retval[1] = (float)(normal.y);
        retval[2] = (float)(normal.z);
        return retval;
    }

    public double getArea()
    {
        if(this.vertexCount() == 3)
        {
            return Math.abs(getVertex(1).toVec3d().subtract(getVertex(0).toVec3d()).crossProduct(getVertex(2).toVec3d().subtract(getVertex(0).toVec3d())).lengthVector()) / 2.0;

        }
        else if(this.vertexCount() == 4) //quad
        {
            return Math.abs(getVertex(2).toVec3d().subtract(getVertex(0).toVec3d()).crossProduct(getVertex(3).toVec3d().subtract(getVertex(1).toVec3d())).lengthVector()) / 2.0;
        }
        else
        {
            double area = 0;
            for(Poly q : this.toQuads())
            {
                area += q.getArea();
            }
            return area;
        }
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

    /**
     * Gets the face to be used for setupFace semantics.  
     * Is a general facing but does NOT mean poly is actually on that face.
     */
    @Override
    public EnumFacing getNominalFace()
    {
        return nominalFace;
    }

    /** 
     * Face to use for shading testing.
     * Based on which way face points. 
     * Never null
     */
    public EnumFacing getNormalFace()
    {
        return QuadHelper.computeFaceForNormal(this.getFaceNormal());
    }
    
    /** 
     * Face to use for occlusion testing.
     * Null if not fully on one of the faces.
     * Fudges a bit because painted quads can be slightly offset from the plane.
     */
    public EnumFacing getActualFace()
    {
        // semantic face will be right most of the time
        if(this.isOnFace(this.nominalFace, QuadHelper.EPSILON)) return nominalFace;

        for(EnumFacing f : EnumFacing.values())
        {
            if(f != nominalFace && this.isOnFace(f, QuadHelper.EPSILON)) return f;
        }
        return null;
    }

    /**
     * Sets the face to be used for setupFace semantics
     */
    public EnumFacing setNominalFace(EnumFacing face)
    {
        this.nominalFace = face;
        return face;
    }
    
    /** convenience method - sets surface value and returns self */
    public Poly setSurfaceInstance(SurfaceInstance surfaceInstance)
    {
        this.surfaceInstance = surfaceInstance;
        return this;
    }
    
    /** returns a copy of this quad with the given transformation applied */
    public Poly transform(Matrix4d m)
    {
        return this.transform(new Matrix4f(m));
    }
    
    /** returns a copy of this quad with the given transformation applied */
    public Poly transform(Matrix4f matrix)
    {
        Poly result = this.clone();
        
        // transform vertices
        for(int i = 0; i < result.vertexCount; i++)
        {
            Vertex vertex = result.getVertex(i);
            Vector4f temp = new Vector4f(vertex.x, vertex.y, vertex.z, 1);
            matrix.transform(temp);
            if(Math.abs(temp.w - 1) > 1e-5) temp.scale(1 / temp.w);
            result.setVertex(i, vertex.withXYZ((float)temp.x, (float)temp.y, (float)temp.z));
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
            result.setNominalFace(QuadHelper.computeFaceForNormal(newFaceVec));
        }
        
        return result;
    }

    public void setVertexColor(int index, int vColor)
    {
        this.setVertex(index, this.getVertex(index).withColor(vColor));        
    }

    @Override
    public String getTextureName()
    {
        return textureName;
    }

    public void setTextureName(String textureName)
    {
        this.textureName = textureName;
    }

    @Override
    public Rotation getRotation()
    {
        return rotation;
    }

    public void setRotation(Rotation rotation)
    {
        this.rotation = rotation;
    }

    @Override
    public int getColor()
    {
        return color;
    }

    public void setColor(int color)
    {
        this.color = color;
    }

    @Override
    public boolean isFullBrightness()
    {
        return isFullBrightness;
    }

    public void setFullBrightness(boolean isFullBrightness)
    {
        this.isFullBrightness = isFullBrightness;
    }

    @Override
    public boolean isLockUV()
    {
        return isLockUV;
    }

    public void setLockUV(boolean isLockUV)
    {
        this.isLockUV = isLockUV;
    }

    @Override
    public boolean shouldContractUVs()
    {
        return shouldContractUVs;
    }

    public void setShouldContractUVs(boolean shouldContractUVs)
    {
        this.shouldContractUVs = shouldContractUVs;
    }

    @Override
    public RenderPass getRenderPass()
    {
        return renderPass;
    }

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

    public void setMinU(float minU)
    {
        this.minU = minU;
    }

    @Override
    public float getMaxU()
    {
        return maxU;
    }

    public void setMaxU(float maxU)
    {
        this.maxU = maxU;
    }

    @Override
    public float getMinV()
    {
        return minV;
    }

    public void setMinV(float minV)
    {
        this.minV = minV;
    }

    @Override
    public float getMaxV()
    {
        return maxV;
    }

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

    protected void setAncestorQuadID(int ancestorQuadID)
    {
        this.ancestorQuadID = ancestorQuadID;
    }
}