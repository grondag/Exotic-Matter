package grondag.exotic_matter.render;

import grondag.exotic_matter.world.Rotation;

public interface IMutablePolygon extends IPolygon
{

    /**
     * Changes all vertices and quad color to new color and returns itself
     */
    
    // used in mesh construction
    
    public IMutablePolygon replaceColor(int color);
    
    public void setMinU(float minU);

    public void setMaxU(float maxU);

    public void setMinV(float minV);

    public void setMaxV(float maxV);

    
    ///// used in painters
    
    public void setTextureName(String textureName);

    public void setRenderPass(RenderPass renderPass);

    public void setFullBrightness(boolean isFullBrightnessIntended);

    /**
     * Assigns UV coordinates to each vertex by projecting vertex
     * onto plane of the quad's face. If the quad is not rotated,
     * then semantics of vertex coordinates matches those of setupFaceQuad.
     * For example, on NSEW faces, "up" (+y) corresponds to the top of the texture.
     * 
     * Assigned values are in the range 0-16, as is conventional for MC.
     */
    public void assignLockedUVCoordinates();

    /** 
     * Unique scale transformation of all vertex coordinates 
     * using block center (0.5, 0.5, 0.5) as origin.
     */
    public void scaleFromBlockCenter(float scale);

    
    public void setVertexColor(int index, int vColor);

    /**
     * Multiplies this quads color and all vertex color by given value
     */
    public void multiplyColor(int color);

    public void setRotation(Rotation rotation);

    /**
     * Multiplies uvMin/Max by the given factors.
     */
    public void scaleQuadUV(float uScale, float vScale);
}
