package grondag.exotic_matter.model.primitives;

import javax.annotation.Nullable;

public interface IVertexFactory
{
    public Vertex newVertex(float x, float y, float z, float u, float v, int color, @Nullable Vec3f normal, int glow);
    
    public default Vertex newVertex(float x, float y, float z, float u, float v, int color)
    {
        return newVertex(x, y, z, u, v, color, null);
    }

    public default Vertex newVertex(float x, float y, float z, float u, float v, int color, int glow)
    {
        return newVertex(x, y, z, u, v, color, null, glow);
    }
    
    public default Vertex newVertex(float x, float y, float z, float u, float v, int color, @Nullable Vec3f normal)
    {
        return newVertex(x, y, z, u, v, color, normal, 0);
    }
    
    public default Vertex newVertex(float x, float y, float z, float u, float v, int color, float normalX, float normalY, float normalZ)
    {
        return newVertex(x, y, z, u, v, color, Vec3f.create(normalX, normalY, normalZ));
    }

    public Vertex interpolate(float newX, float newY, float newZ, @Nullable Vec3f newNormal, Vertex from, Vertex to, float toWeight);

    public Vertex withColorGlow(Vertex vertex, int colorIn, int glowIn);

    public Vertex withUV(Vertex vertex, float uNew, float vNew);

    public Vertex withGeometry(Vertex vertex, float x, float y, float z, @Nullable Vec3f normal);

}
