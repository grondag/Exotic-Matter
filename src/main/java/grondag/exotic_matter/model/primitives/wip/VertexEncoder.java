package grondag.exotic_matter.model.primitives.wip;

import grondag.exotic_matter.model.primitives.vertex.Vec3f;

public class VertexEncoder
{

    public static VertexEncoder get(int format)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public int stride()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean hasNormals()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public Vec3f getVertexNormal(IIntStream stream, int vertexAddress, int vertexIndex)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean hasVertexNormal(IIntStream stream, int vertexAddress, int vertexIndex)
    {
        // TODO Auto-generated method stub
        return false;
    }

    public float getVertexNormalX(IIntStream stream, int vertexAddress, int vertexIndex)
    {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public void setVertexNormalX(IIntStream stream, int vertexAddress, int vertexIndex, float normalX)
    {
        // TODO Auto-generated method stub
    }

    public float getVertexNormalY(IIntStream stream, int vertexAddress, int vertexIndex)
    {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public void setVertexNormalY(IIntStream stream, int vertexAddress, int vertexIndex, float normalY)
    {
        // TODO Auto-generated method stub
    }
    
    public float getVertexNormalZ(IIntStream stream, int vertexAddress, int vertexIndex)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setVertexNormalZ(IIntStream stream, int vertexAddress, int vertexIndex, float normalZ)
    {
        // TODO Auto-generated method stub
    }
    
    public void setVertexNormal(IIntStream stream, int vertexAddress, int vertexIndex, float normalX, float normalY, float normalZ)
    {
        // TODO Auto-generated method stub
    }
    
    public float getVertexX(IIntStream stream, int vertexAddress, int vertexIndex)
    {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public void setVertexX(IIntStream stream, int vertexAddress, int vertexIndex, float x)
    {
        // TODO Auto-generated method stub
    }
    
    public float getVertexY(IIntStream stream, int vertexAddress, int vertexIndex)
    {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public void setVertexY(IIntStream stream, int vertexAddress, int vertexIndex, float y)
    {
        // TODO Auto-generated method stub
    }
    
    public float getVertexZ(IIntStream stream, int vertexAddress, int vertexIndex)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setVertexZ(IIntStream stream, int vertexAddress, int vertexIndex, float z)
    {
        // TODO Auto-generated method stub
    }
    
    public void setVertexPos(IIntStream stream, int vertexAddress, int vertexIndex, float x, float y, float z)
    {
        // TODO Auto-generated method stub
    }
    
    public boolean hasColor()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public int getVertexColor(IIntStream stream, int vertexAddress, int layerIndex, int vertexIndex)
    {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public void setVertexColor(IIntStream stream, int vertexAddress, int layerIndex, int vertexIndex, int color)
    {
        // TODO Auto-generated method stub
    }

    public float getVertexU(IIntStream stream, int vertexAddress, int layerIndex, int vertexIndex)
    {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public void setVertexU(IIntStream stream, int vertexAddress, int layerIndex, int vertexIndex, float u)
    {
        // TODO Auto-generated method stub
    }
    
    public float getVertexV(IIntStream stream, int vertexAddress, int layerIndex, int vertexIndex)
    {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public void setVertexV(IIntStream stream, int vertexAddress, int layerIndex, int vertexIndex, float v)
    {
        // TODO Auto-generated method stub
    }
    
    public void setVertexUV(IIntStream stream, int vertexAddress, int layerIndex, int vertexIndex, float u, float v)
    {
        // TODO Auto-generated method stub
    }
}
