package grondag.exotic_matter.model.primitives;

/**
 * 
 * Ported to Java from Tomas Akenine-Möller
 * http://fileadmin.cs.lth.se/cs/Personal/Tomas_Akenine-Moller/code/tribox3.txt
 */

public class TriangleBoxTest
{

    public final static int X = 0;
    public final static int Y = 1;
    public final static int Z = 2;

    private static void CROSS(float[] dest, float[] v1, float[] v2) 
    {
          dest[0]=v1[1]*v2[2]-v1[2]*v2[1];
          dest[1]=v1[2]*v2[0]-v1[0]*v2[2];
          dest[2]=v1[0]*v2[1]-v1[1]*v2[0]; 
    }

    private static float DOT(float[] v0, float[] v1) 
    {
        return (v0[0]*v1[0]+v0[1]*v1[1]+v0[2]*v1[2]);
    }


    private static void SUB(float[] dest, float[] v0, float[] v1)
    {
          dest[0]=v0[0]-v1[0];
          dest[1]=v0[1]-v1[1];
          dest[2]=v0[2]-v1[2]; 
    }

    public static boolean planeBoxOverlapSlow(float[] normal, float[] vert, float[] maxbox)
    {
        int q;
        float[] vmin = new float[3];
        float[] vmax = new float[3];
        float v;

        for(q = X; q <= Z; q++)
        {
            v=vert[q];
            if(normal[q]>0.0f)
            {
                vmin[q]=-maxbox[q] - v;
                vmax[q]= maxbox[q] - v;
            }
            else
            {
                vmin[q]= maxbox[q] - v;
                vmax[q]=-maxbox[q] - v;
            }
        }

        if(DOT(normal,vmin)>0.0f) return false;
        if(DOT(normal,vmax)>=0.0f) return true;
        return false;
    }
    
    public static boolean planeBoxOverlap(float[] normal, float[] vert, float boxHalfSize)
    {
        int q;
        float[] vmin = new float[3];
        float[] vmax = new float[3];
        float v;

        for(q = X; q <= Z; q++)
        {
            v=vert[q];
            if(normal[q]>0.0f)
            {
                vmin[q]= -boxHalfSize - v;
                vmax[q]= boxHalfSize - v;
            }
            else
            {
                vmin[q]= boxHalfSize - v;
                vmax[q]=  -boxHalfSize - v;
            }
        }

        if(DOT(normal,vmin)>0.0f) return false;
        if(DOT(normal,vmax)>=0.0f) return true;
        return false;
    }
    
  

    public static boolean triBoxOverlapSlow(float centerX, float centerY, float centerZ, float halfDist, IPolygon poly)
    {
        float[] boxcenter = new float[3];
        float[] boxhalfsize = new float[3];
        
        boxcenter[0] = centerX;
        boxcenter[1] = centerY;
        boxcenter[2] = centerZ;
        boxhalfsize[0] = halfDist;
        boxhalfsize[1] = halfDist;
        boxhalfsize[2] = halfDist;
        
        Vertex[] v = poly.vertexArray();
        
        if(poly.vertexCount() == 3)
        {
            return triBoxOverlapSlow(boxcenter, boxhalfsize, v[0], v[1], v[2]);
        }
        else
        {
            return triBoxOverlapSlow(boxcenter, boxhalfsize, v[0], v[1], v[2])
                || triBoxOverlapSlow(boxcenter, boxhalfsize, v[0], v[2], v[3]);
        }
    }
    
    public static boolean triBoxOverlapSlow(float[] boxcenter, float[] boxhalfsize, Vertex v0, Vertex v1, Vertex v2)
    {
        float[][] triverts = new float[3][3];
        triverts[0][0] = v0.x;
        triverts[0][1] = v0.y;
        triverts[0][2] = v0.z;
        triverts[1][0] = v1.x;
        triverts[1][1] = v1.y;
        triverts[1][2] = v1.z;
        triverts[2][0] = v2.x;
        triverts[2][1] = v2.y;
        triverts[2][2] = v2.z;
        return triBoxOverlapSlow(boxcenter, boxhalfsize, triverts);
    }
    
    /**
     * use separating axis theorem to test overlap between triangle and box
     * 
     *    need to test for overlap in these directions:
     *    1) the {x,y,z}-directions (actually, since we use the AABB of the triangle
     *       we do not even need to test these)
     *    2) normal of the triangle
     *    3) crossproduct(edge from tri, {x,y,z}-directin)
     *       this gives 3x3=9 more tests
     * Dimensions are 3, 3, 3-3
     * 
     * TODO: use threadlocal combined float array to hold all intermediate values for optimal LOR
     * 
     * This version closely follows the original and is useful for regression testing.
     */
    public static boolean triBoxOverlapSlow(float[] boxcenter,float[] boxhalfsize, float[][] triverts)
    {
        float[] v0 = new float[3];
        float[] v1 = new float[3];
        float[] v2 = new float[3];
        float min,max,p0,p1,p2,rad,fex,fey,fez;

        float[] normal = new float[3];
        float[] e0 = new float[3];
        float[] e1 = new float[3];
        float[] e2 = new float[3];

        /* move everything so that the boxcenter is in (0,0,0) */
        SUB(v0, triverts[0], boxcenter);
        SUB(v1, triverts[1], boxcenter);
        SUB(v2, triverts[2], boxcenter);

        /* compute triangle edges */
        SUB(e0, v1, v0);      /* tri edge 0 */
        SUB(e1, v2, v1);      /* tri edge 1 */
        SUB(e2, v0, v2);      /* tri edge 2 */

        /*  test the 9 tests first (this was faster) */
        fex = Math.abs(e0[X]);
        fey = Math.abs(e0[Y]);
        fez = Math.abs(e0[Z]);

       //AXISTEST_X01(e0[Z], e0[Y], fez, fey);
       p0 = e0[Z] * v0[Y] - e0[Y] * v0[Z];
       p2 = e0[Z] * v2[Y] - e0[Y] * v2[Z];
       if(p0<p2) {min=p0; max=p2;} else {min=p2; max=p0;}
       rad = fez * boxhalfsize[Y] + fey * boxhalfsize[Z];
       if(min>rad || max<-rad) return false;
       
       //AXISTEST_Y02(e0[Z], e0[X], fez, fex);
       p0 = -e0[Z] * v0[X] + e0[X] * v0[Z];
       p2 = -e0[Z] * v2[X] + e0[X] * v2[Z];
       if(p0<p2) {min=p0; max=p2;} else {min=p2; max=p0;}
       rad = fez * boxhalfsize[X] + fex * boxhalfsize[Z];
       if(min>rad || max<-rad) return false;
       
       //AXISTEST_Z12(e0[Y], e0[X], fey, fex);
       p1 = e0[Y] * v1[X] - e0[X] * v1[Y];
       p2 = e0[Y] * v2[X] - e0[X] * v2[Y];
       if(p2<p1) {min=p2; max=p1;} else {min=p1; max=p2;}
       rad = fey * boxhalfsize[X] + fex * boxhalfsize[Y];
       if(min>rad || max<-rad) return false;

       fex = Math.abs(e1[X]);
       fey = Math.abs(e1[Y]);
       fez = Math.abs(e1[Z]);

       //AXISTEST_X01(e1[Z], e1[Y], fez, fey);
       p0 = e1[Z] * v0[Y] - e1[Y] * v0[Z];
       p2 = e1[Z] * v2[Y] - e1[Y] * v2[Z];
       if(p0<p2) {min=p0; max=p2;} else {min=p2; max=p0;}
       rad = fez * boxhalfsize[Y] + fey * boxhalfsize[Z];
       if(min>rad || max<-rad) return false;
       
       //AXISTEST_Y02(e1[Z], e1[X], fez, fex);
       p0 = -e1[Z] * v0[X] + e1[X] * v0[Z];
       p2 = -e1[Z] * v2[X] + e1[X] * v2[Z];
       if(p0<p2) {min=p0; max=p2;} else {min=p2; max=p0;}
       rad = fez * boxhalfsize[X] + fex * boxhalfsize[Z];
       if(min>rad || max<-rad) return false;

       //AXISTEST_Z0(e1[Y], e1[X], fey, fex);
       p0 = e1[Y] * v0[X] - e1[X] * v0[Y];
       p1 = e1[Y] * v1[X] - e1[X] * v1[Y];
       if(p0<p1) {min=p0; max=p1;} else {min=p1; max=p0;}
       rad = fey * boxhalfsize[X] + fex * boxhalfsize[Y];
       if(min>rad || max<-rad) return false;

       fex = Math.abs(e2[X]);
       fey = Math.abs(e2[Y]);
       fez = Math.abs(e2[Z]);

       //AXISTEST_X2(e2[Z], e2[Y], fez, fey);
       p0 = e2[Z] * v0[Y] - e2[Y] * v0[Z];
       p1 = e2[Z] * v1[Y] - e2[Y] * v1[Z];
       if(p0<p1) {min=p0; max=p1;} else {min=p1; max=p0;}
       rad = fez * boxhalfsize[Y] + fey * boxhalfsize[Z];
       if(min>rad || max<-rad) return false;

       //AXISTEST_Y1(e2[Z], e2[X], fez, fex);
       p0 = -e2[Z] * v0[X] + e2[X] * v0[Z];
       p1 = -e2[Z] * v1[X] + e2[X] * v1[Z];
       if(p0<p1) {min=p0; max=p1;} else {min=p1; max=p0;}
       rad = fez * boxhalfsize[X] + fex * boxhalfsize[Z];
       if(min>rad || max<-rad) return false;

       //AXISTEST_Z12(e2[Y], e2[X], fey, fex);
       p1 = e2[Y] * v1[X] - e2[X] * v1[Y];
       p2 = e2[Y] * v2[X] - e2[X] * v2[Y];
       if(p2<p1) {min=p2; max=p1;} else {min=p1; max=p2;}
       rad = fey * boxhalfsize[X] + fex * boxhalfsize[Y];
       if(min>rad || max<-rad) return false;


       /*  first test overlap in the {x,y,z}-directions */
       /*  find min, max of the triangle each direction, and test for overlap in */
       /*  that direction -- this is equivalent to testing a minimal AABB around */
       /*  the triangle against the AABB */

       // TODO: could be more efficient - some inequality test outcomes imply other tests aren't necessary
       // and should cache quad AABB in the quad - will be used multiple time
       /* test in X-direction */
//       FINDMINMAX(v0[X],v1[X],v2[X],min,max);
       min = max = v0[X];
       if(v1[X] < min) min=v1[X];
       if(v1[X] > max) max=v1[X];
       if(v2[X] < min) min=v2[X];
       if(v2[X] > max) max=v2[X];
       if(min>boxhalfsize[X] || max<-boxhalfsize[X]) return false;


       /* test in Y-direction */
//       FINDMINMAX(v0[Y],v1[Y],v2[Y],min,max);
       min = max = v0[Y];
       if(v1[Y] < min) min=v1[Y];
       if(v1[Y] > max) max=v1[Y];
       if(v2[Y] < min) min=v2[Y];
       if(v2[Y] > max) max=v2[Y];
       if(min>boxhalfsize[Y] || max<-boxhalfsize[Y]) return false;

       /* test in Z-direction */
//       FINDMINMAX(v0[Z],v1[Z],v2[Z],min,max);
       //#define FINDMINMAX(x0,x1,x2,min,max) \
       min = max = v0[Z];
       if(v1[Z] < min) min=v1[Z];
       if(v1[Z] > max) max=v1[Z];
       if(v2[Z] < min) min=v2[Z];
       if(v2[Z] > max) max=v2[Z];
       if(min>boxhalfsize[Z] || max<-boxhalfsize[Z]) return false;

       /*  test if the box intersects the plane of the triangle */
       /*  compute plane equation of triangle: normal*x+d=0 */
       //TODO = use the normal from the quad
       CROSS(normal,e0,e1);

       if(!planeBoxOverlapSlow(normal, v0, boxhalfsize)) return false;

       return true;   /* box and triangle overlaps */
    }
    
    public static final int POLY_MIN_X = 0;
    public static final int POLY_MAX_X = 1;
    public static final int POLY_MIN_Y = 2;
    public static final int POLY_MAX_Y = 3;
    public static final int POLY_MIN_Z = 4;
    public static final int POLY_MAX_Z = 5;
    public static final int POLY_V0_X = 6;
    public static final int POLY_V0_Y = 7;
    public static final int POLY_V0_Z = 8;
    public static final int POLY_V1_X = 9;
    public static final int POLY_V1_Y = 10;
    public static final int POLY_V1_Z = 11;
    public static final int POLY_V2_X = 12;
    public static final int POLY_V2_Y = 13;
    public static final int POLY_V2_Z = 14;
    public static final int POLY_NORM_X = 15;
    public static final int POLY_NORM_Y = 16;
    public static final int POLY_NORM_Z = 17;
    public static final int EDGE_0_X = 18;
    public static final int EDGE_0_Y = 19;
    public static final int EDGE_0_Z = 20;
    public static final int EDGE_1_X = 21;
    public static final int EDGE_1_Y = 22;
    public static final int EDGE_1_Z = 23;
    public static final int EDGE_2_X = 24;
    public static final int EDGE_2_Y = 25;
    public static final int EDGE_2_Z = 26;
    
    public static void packPolyData(Vertex v0, Vertex v1, Vertex v2, float[] polyData)
    {
        final float x0 = v0.x;
        final float y0 = v0.y;
        final float z0 = v0.z;
        
        final float x1 = v1.x;
        final float y1 = v1.y;
        final float z1 = v1.z;
        
        final float x2 = v2.x;
        final float y2 = v2.y;
        final float z2 = v2.z;

        polyData[POLY_V0_X] = x0;
        polyData[POLY_V0_Y] = y0;
        polyData[POLY_V0_Z] = z0;
        polyData[POLY_V1_X] = x1;
        polyData[POLY_V1_Y] = y1;
        polyData[POLY_V1_Z] = z1;
        polyData[POLY_V2_X] = x2;
        polyData[POLY_V2_Y] = y2;
        polyData[POLY_V2_Z] = z2;
        
        // find min/max of three components with only 2 or 3 comparisons
        if(x0 > x1)
        {
            if(x0 > x2)
            {
                polyData[POLY_MAX_X] = x0;
                polyData[POLY_MIN_X] = x1 < x2 ? x1 : x2;
            }
            else // x1 < x0 <= x2
            {
                polyData[POLY_MAX_X] = x2;
                polyData[POLY_MIN_X] = x1;
            }
        }
        else // x0 <= x1
        {
            if(x1 > x2)
            {
                polyData[POLY_MAX_X] = x1;
                polyData[POLY_MIN_X] = x0 < x2 ? x0 : x2;
            }
            else // x0 <= x1 && x1 <= x2
            {
                polyData[POLY_MAX_X] = x2;
                polyData[POLY_MIN_X] = x0;
            }
        }

        if(y0 > y1)
        {
            if(y0 > y2)
            {
                polyData[POLY_MAX_Y] = y0;
                polyData[POLY_MIN_Y] = y1 < y2 ? y1 : y2;
            }
            else // y1 < y0 <= y2
            {
                polyData[POLY_MAX_Y] = y2;
                polyData[POLY_MIN_Y] = y1;
            }
        }
        else // y0 <= y1
        {
            if(y1 > y2)
            {
                polyData[POLY_MAX_Y] = y1;
                polyData[POLY_MIN_Y] = y0 < y2 ? y0 : y2;
            }
            else // y0 <= y1 && y1 <= y2
            {
                polyData[POLY_MAX_Y] = y2;
                polyData[POLY_MIN_Y] = y0;
            }
        }
        
        if(z0 > z1)
        {
            if(z0 > z2)
            {
                polyData[POLY_MAX_Z] = z0;
                polyData[POLY_MIN_Z] = z1 < z2 ? z1 : z2;
            }
            else // z1 < z0 <= z2
            {
                polyData[POLY_MAX_Z] = z2;
                polyData[POLY_MIN_Z] = z1;
            }
        }
        else // z0 <= z1
        {
            if(z1 > z2)
            {
                polyData[POLY_MAX_Z] = z1;
                polyData[POLY_MIN_Z] = z0 < z2 ? z0 : z2;
            }
            else // z0 <= z1 && z1 <= z2
            {
                polyData[POLY_MAX_Z] = z2;
                polyData[POLY_MIN_Z] = z0;
            }
        }
        
        /* pre-compute triangle edges */
        
        // local cuz needed for normal calc
        final float e0x = x1 - x0;
        final float e0y = y1 - y0;
        final float e0z = z1 - z0;
        final float e1x = x2 - x1;
        final float e1y = y2 - y1;
        final float e1z = z2 - z1;
        polyData[EDGE_0_X] = e0x;
        polyData[EDGE_0_Y] = e0y;
        polyData[EDGE_0_Z] = e0z;
        polyData[EDGE_1_X] = e1x;
        polyData[EDGE_1_Y] = e1y;
        polyData[EDGE_1_Z] = e1z;

        // can go direct to array, not needed for normal
        polyData[EDGE_2_X] = x0 - x2;
        polyData[EDGE_2_Y] = y0 - y2;
        polyData[EDGE_2_Z] = z0 - z2;
        
        /* pre-compute normal */
        polyData[POLY_NORM_X] = e0y * e1z - e0z * e1y;
        polyData[POLY_NORM_Y] = e0z * e1x - e0x * e1z;
        polyData[POLY_NORM_Z] = e0x * e1y - e0y * e1x; 
    }
    
    /**
     * Assumes boxes are cubes and polygon info is pre-packed into array for LOR and reuse during iteration of voxels.
     * 
     * TODO: if poly is coplanar with box face, then only count as overlap if box is behind the poly 
     */
    public static boolean triBoxOverlap(float boxCenterX, float boxCenterY, float boxCenterZ, float boxHalfSize, float[] polyData)
    {
        // Bounding box tests
        
        final float minX = polyData[POLY_MIN_X];
        final float maxX = polyData[POLY_MAX_X];
        final float minY = polyData[POLY_MIN_Y];
        final float maxY = polyData[POLY_MAX_Y];
        final float minZ = polyData[POLY_MIN_Z];
        final float maxZ = polyData[POLY_MAX_Z];

        // Exclude polys that merely touch an edge unless the poly is co-planar
        if(minX == maxX)
        {
            if(minX > boxCenterX + boxHalfSize) return false;
            if(maxX < boxCenterX - boxHalfSize) return false;
        }
        else
        {
            if(minX >= boxCenterX + boxHalfSize) return false;
            if(maxX <= boxCenterX - boxHalfSize) return false;
        }
        
        if(minY == maxY)
        {
            if(minY > boxCenterY + boxHalfSize) return false;
            if(maxY < boxCenterY - boxHalfSize) return false;
        }
        else
        {
            if(minY >= boxCenterY + boxHalfSize) return false;
            if(maxY <= boxCenterY - boxHalfSize) return false;
        }
        
        if(minZ == maxZ)
        {
            if(minZ > boxCenterZ + boxHalfSize) return false;
            if(maxZ < boxCenterZ - boxHalfSize) return false;
        }
        else
        {
            if(minZ >= boxCenterZ + boxHalfSize) return false;
            if(maxZ <= boxCenterZ - boxHalfSize) return false;
        }
        
        float[] v0 = new float[3];
        float[] v1 = new float[3];
        float[] v2 = new float[3];
        float min,max,p0,p1,p2,rad,fex,fey,fez;

        float[] normal = new float[3];
        float[] e0 = new float[3];
        float[] e1 = new float[3];
        float[] e2 = new float[3];

        /* move everything so that the boxcenter is in (0,0,0) */
//        final float v0x = polyData[POLY_V0_X] - boxCenterX;
//        final float v0y = polyData[POLY_V0_Y] - boxCenterY;
//        final float v0z = polyData[POLY_V0_Z] - boxCenterZ;
        v0[X] = polyData[POLY_V0_X] - boxCenterX;
        v0[Y] = polyData[POLY_V0_Y] - boxCenterY;
        v0[Z] = polyData[POLY_V0_Z] - boxCenterZ;
        
        v1[X] = polyData[POLY_V1_X] - boxCenterX;
        v1[Y] = polyData[POLY_V1_Y] - boxCenterY;
        v1[Z] = polyData[POLY_V1_Z] - boxCenterZ;
        
        v2[X] = polyData[POLY_V2_X] - boxCenterX;
        v2[Y] = polyData[POLY_V2_Y] - boxCenterY;
        v2[Z] = polyData[POLY_V2_Z] - boxCenterZ;
                
        /* copy triangle edges */
        // TODO: use directly 
        e0[X] = polyData[EDGE_0_X];
        e0[Y] = polyData[EDGE_0_Y];
        e0[Z] = polyData[EDGE_0_Z];
        
        e1[X] = polyData[EDGE_1_X];
        e1[Y] = polyData[EDGE_1_Y];
        e1[Z] = polyData[EDGE_1_Z];
        
        e2[X] = polyData[EDGE_2_X];
        e2[Y] = polyData[EDGE_2_Y];
        e2[Z] = polyData[EDGE_2_Z];
        
        /*  test the 9 tests first (this was faster) */
        
        // TODO: pre-multiply w/ boxsize?
        fex = Math.abs(e0[X]);
        fey = Math.abs(e0[Y]);
        fez = Math.abs(e0[Z]);

       //AXISTEST_X01(e0[Z], e0[Y], fez, fey);
       p0 = e0[Z] * v0[Y] - e0[Y] * v0[Z];
       p2 = e0[Z] * v2[Y] - e0[Y] * v2[Z];
       if(p0<p2) {min=p0; max=p2;} else {min=p2; max=p0;}
       rad = fez * boxHalfSize + fey * boxHalfSize;
       if(min>rad || max<-rad) return false;
       
       //AXISTEST_Y02(e0[Z], e0[X], fez, fex);
       p0 = -e0[Z] * v0[X] + e0[X] * v0[Z];
       p2 = -e0[Z] * v2[X] + e0[X] * v2[Z];
       if(p0<p2) {min=p0; max=p2;} else {min=p2; max=p0;}
       rad = fez * boxHalfSize + fex * boxHalfSize;
       if(min>rad || max<-rad) return false;
       
       //AXISTEST_Z12(e0[Y], e0[X], fey, fex);
       p1 = e0[Y] * v1[X] - e0[X] * v1[Y];
       p2 = e0[Y] * v2[X] - e0[X] * v2[Y];
       if(p2<p1) {min=p2; max=p1;} else {min=p1; max=p2;}
       rad = fey * boxHalfSize + fex * boxHalfSize;
       if(min>rad || max<-rad) return false;

       fex = Math.abs(e1[X]);
       fey = Math.abs(e1[Y]);
       fez = Math.abs(e1[Z]);

       //AXISTEST_X01(e1[Z], e1[Y], fez, fey);
       p0 = e1[Z] * v0[Y] - e1[Y] * v0[Z];
       p2 = e1[Z] * v2[Y] - e1[Y] * v2[Z];
       if(p0<p2) {min=p0; max=p2;} else {min=p2; max=p0;}
       rad = fez * boxHalfSize + fey * boxHalfSize;
       if(min>rad || max<-rad) return false;
       
       //AXISTEST_Y02(e1[Z], e1[X], fez, fex);
       p0 = -e1[Z] * v0[X] + e1[X] * v0[Z];
       p2 = -e1[Z] * v2[X] + e1[X] * v2[Z];
       if(p0<p2) {min=p0; max=p2;} else {min=p2; max=p0;}
       rad = fez * boxHalfSize + fex * boxHalfSize;
       if(min>rad || max<-rad) return false;

       //AXISTEST_Z0(e1[Y], e1[X], fey, fex);
       p0 = e1[Y] * v0[X] - e1[X] * v0[Y];
       p1 = e1[Y] * v1[X] - e1[X] * v1[Y];
       if(p0<p1) {min=p0; max=p1;} else {min=p1; max=p0;}
       rad = fey * boxHalfSize + fex * boxHalfSize;
       if(min>rad || max<-rad) return false;

       // TODO: premultiply w/ boxsize?
       fex = Math.abs(e2[X]);
       fey = Math.abs(e2[Y]);
       fez = Math.abs(e2[Z]);
       
       //AXISTEST_X2(e2[Z], e2[Y], fez, fey);
       p0 = e2[Z] * v0[Y] - e2[Y] * v0[Z];
       p1 = e2[Z] * v1[Y] - e2[Y] * v1[Z];
       if(p0<p1) {min=p0; max=p1;} else {min=p1; max=p0;}
       rad = fez * boxHalfSize + fey * boxHalfSize;
       if(min>rad || max<-rad)  return false;

       //AXISTEST_Y1(e2[Z], e2[X], fez, fex);
       p0 = -e2[Z] * v0[X] + e2[X] * v0[Z];
       p1 = -e2[Z] * v1[X] + e2[X] * v1[Z];
       if(p0<p1) {min=p0; max=p1;} else {min=p1; max=p0;}
       rad = fez * boxHalfSize + fex * boxHalfSize;
       if(min>rad || max<-rad) return false;

       //AXISTEST_Z12(e2[Y], e2[X], fey, fex);
       p1 = e2[Y] * v1[X] - e2[X] * v1[Y];
       p2 = e2[Y] * v2[X] - e2[X] * v2[Y];
       if(p2<p1) {min=p2; max=p1;} else {min=p1; max=p2;}
       rad = fey * boxHalfSize + fex * boxHalfSize;
       if(min>rad || max<-rad) return false;


       /*  first test overlap in the {x,y,z}-directions */
       /*  find min, max of the triangle each direction, and test for overlap in */
       /*  that direction -- this is equivalent to testing a minimal AABB around */
       /*  the triangle against the AABB */

       // TODO: use cached min/max
       // TODO: is this redundant of tests at start?
       /* test in X-direction */
       min = max = v0[X];
       if(v1[X] < min) min=v1[X];
       if(v1[X] > max) max=v1[X];
       if(v2[X] < min) min=v2[X];
       if(v2[X] > max) max=v2[X];
       if(min>boxHalfSize || max<-boxHalfSize) return false;


       /* test in Y-direction */
       min = max = v0[Y];
       if(v1[Y] < min) min=v1[Y];
       if(v1[Y] > max) max=v1[Y];
       if(v2[Y] < min) min=v2[Y];
       if(v2[Y] > max) max=v2[Y];
       if(min>boxHalfSize || max<-boxHalfSize) return false;

       /* test in Z-direction */
       min = max = v0[Z];
       if(v1[Z] < min) min=v1[Z];
       if(v1[Z] > max) max=v1[Z];
       if(v2[Z] < min) min=v2[Z];
       if(v2[Z] > max) max=v2[Z];
       if(min>boxHalfSize || max<-boxHalfSize) return false;

       /*  test if the box intersects the plane of the triangle */
       /*  compute plane equation of triangle: normal*x+d=0 */
       //TODO = use the normal from the quad
       CROSS(normal,e0,e1);

       if(!planeBoxOverlap(normal, v0, boxHalfSize)) return false;

       return true;   /* box and triangle overlaps */
    }
}