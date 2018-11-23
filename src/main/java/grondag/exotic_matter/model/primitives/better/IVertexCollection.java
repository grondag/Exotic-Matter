package grondag.exotic_matter.model.primitives.better;

import grondag.exotic_matter.model.primitives.vertex.IVec3f;

public interface IVertexCollection
{
    public int vertexCount();
    
    public IVec3f getPos(int index);
    
    /**
     * Wraps around if index out of range.
     */
    public default IVec3f getPosModulo(int index)
    {
        return getPos(index % vertexCount());
    }
}
