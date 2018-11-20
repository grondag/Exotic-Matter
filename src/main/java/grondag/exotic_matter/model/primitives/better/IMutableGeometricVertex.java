package grondag.exotic_matter.model.primitives.better;

import javax.vecmath.Matrix4f;

import grondag.exotic_matter.model.primitives.vertex.Vec3f;

public interface IMutableGeometricVertex extends  IGeometricVertex
{
    public IMutableGeometricVertex flip();
    
    /**
     * Will not retain a reference to normal if it is mutable.
     */
    public IMutableGeometricVertex setNormal(Vec3f normal);
    
    public IMutableGeometricVertex setNormal(float x, float y, float z);
    
    /**
     * Will not retain a reference to pos if it is mutable.
     */
    public IMutableGeometricVertex setPos(Vec3f pos);
    
    public IMutableGeometricVertex setPos(float x, float y, float z);
    
    public IMutableGeometricVertex transform(Matrix4f matrix, boolean rescaleToUnitCube);
    
}
