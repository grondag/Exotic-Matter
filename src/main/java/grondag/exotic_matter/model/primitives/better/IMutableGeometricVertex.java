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

    /**
     * The two input vertices must be of the same concrete type and both will be unmodified.<br>
     * Returned instance will be a new instance.<br>
     * Does not retain a reference to the output or either input.<br>
     */
    public IMutableGeometricVertex interpolate(IMutableGeometricVertex jVertex, float t);
    
}
