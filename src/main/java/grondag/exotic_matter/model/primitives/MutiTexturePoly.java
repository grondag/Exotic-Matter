package grondag.exotic_matter.model.primitives;

import java.util.function.Consumer;

import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

public abstract class MutiTexturePoly extends PolyImpl implements IMultiTexturedPolygon
{
    public static class Double extends MutiTexturePoly
    {
        protected final DoubleDelegate doubleDelegate  = new DoubleDelegate();
        
        @Override
        public int layerCount()
        {
            return 2;
        }
        
        private class DoubleDelegate implements IPaintableQuad
        {

            @Override
            public void setMinU(float f)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public float getMinU()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public void setMaxU(float f)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void setMinV(float f)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public float getMinV()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public void setMaxV(float f)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public Surface getSurfaceInstance()
            {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int textureSalt()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public boolean isLockUV()
            {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public EnumFacing getNominalFace()
            {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void setRotation(Rotation object)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public Rotation getRotation()
            {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void setTextureName(String textureName)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public IMutablePolygon getParent()
            {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int vertexCount()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public IPaintableVertex getPaintableVertex(int i)
            {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void scaleFromBlockCenter(float f)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public int layerCount()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public void setRenderPass(BlockRenderLayer renderPass)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void offsetVertexUV(float f, float g)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public float getMaxU()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public float getMaxV()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public IPaintableQuad paintableCopy()
            {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public IPaintableQuad paintableCopy(int vertexCount)
            {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void setVertex(int i, IPaintableVertex thisVertex)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public boolean isConvex()
            {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void toPaintableQuads(Consumer<IPaintableQuad> consumer, boolean b)
            {
                // TODO Auto-generated method stub
                
            }

            
        }
        
        @Override
        public IPaintableQuad getSubQuad(int layerIndex)
        {
            switch(layerIndex)
            {
            case 0:
                return this;
            case 1:
                return this.doubleDelegate;
            default:
                throw new IndexOutOfBoundsException();
            }
        }
        
        
    }
    
    public static class Triple extends Double
    {
        protected final TripleDelegate tripleDelegate  = new TripleDelegate();
        
        private class TripleDelegate implements IPaintableQuad
        {

            @Override
            public void setMinU(float f)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public float getMinU()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public void setMaxU(float f)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void setMinV(float f)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public float getMinV()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public void setMaxV(float f)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public Surface getSurfaceInstance()
            {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int textureSalt()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public boolean isLockUV()
            {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public EnumFacing getNominalFace()
            {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void setRotation(Rotation object)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public Rotation getRotation()
            {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void setTextureName(String textureName)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public IMutablePolygon getParent()
            {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int vertexCount()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public IPaintableVertex getPaintableVertex(int i)
            {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void scaleFromBlockCenter(float f)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public int layerCount()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public void setRenderPass(BlockRenderLayer renderPass)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void offsetVertexUV(float f, float g)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public float getMaxU()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public float getMaxV()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public IPaintableQuad paintableCopy()
            {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public IPaintableQuad paintableCopy(int vertexCount)
            {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void setVertex(int i, IPaintableVertex thisVertex)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public boolean isConvex()
            {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void toPaintableQuads(Consumer<IPaintableQuad> consumer, boolean b)
            {
                // TODO Auto-generated method stub
                
            }

            
        }
        
        @Override
        public int layerCount()
        {
            return 3;
        }
        
        @Override
        public IPaintableQuad getSubQuad(int layerIndex)
        {
            switch(layerIndex)
            {
            case 0:
                return this;
            case 1:
                return this.doubleDelegate;
            case 2:
                return this.tripleDelegate;
            default:
                throw new IndexOutOfBoundsException();
            }
        }
    }
}
