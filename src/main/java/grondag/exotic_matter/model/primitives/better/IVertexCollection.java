package grondag.exotic_matter.model.primitives.better;

import grondag.exotic_matter.model.primitives.vertex.Vec3f;

public interface IVertexCollection
{
    public int vertexCount();
    
    public Vec3f getPos(int index);
    
    /**
     * Wraps around if index out of range.
     */
    public default Vec3f getPosModulo(int index)
    {
        return getPos(index % vertexCount());
    }
}
