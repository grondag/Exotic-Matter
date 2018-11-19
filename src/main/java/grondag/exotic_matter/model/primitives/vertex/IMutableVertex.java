package grondag.exotic_matter.model.primitives.vertex;

import javax.vecmath.Matrix4f;

public interface IMutableVertex extends IVertex
{
    IPaintableVertex forTextureLayer(int layer);
    
    public IMutableVertex flipped();
    
    public IMutableVertex withNormal(Vec3f normal);
    
    public IMutableVertex withXYZ(float xNew, float yNew, float zNew);
    
    public IMutableVertex transform(Matrix4f matrix, boolean rescaleToUnitCube);
    
    public IMutableVertex interpolate(IVertex otherVertex, final float otherWeight);
}
