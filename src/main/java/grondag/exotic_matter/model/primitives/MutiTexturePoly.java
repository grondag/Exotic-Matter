package grondag.exotic_matter.model.primitives;

public abstract class MutiTexturePoly
{
    public static class Double extends PolyImpl
    {
        protected final DoubleDelegate doubleDelegate  = new DoubleDelegate();

        public Double(int vertexCount)
        {
            super(vertexCount);
        }
        
        @Override
        public Double newInstance(int vertexCount)
        {
            return new Double(vertexCount);
        }
        
        @Override
        protected void copyProperties(IPolygon fromObject)
        {
            super.copyProperties(fromObject);
            if(fromObject instanceof Double)
            {
                this.doubleDelegate.copyPropertiesFrom(((Double)fromObject).doubleDelegate);
            }
        }

        @Override
        protected IVertexFactory vertexFactory()
        {
            return MultiTextureVertex.Double.FACTORY;
        }

        @Override
        public int layerCount()
        {
            return 2;
        }
        
        private class DoubleDelegate extends PaintableSubQuad
        {
            @Override
            public IMutablePolygon getParent()
            {
                return Double.this;
            }

            @Override
            protected int layerIndex()
            {
                return 1;
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

        public Triple(int vertexCount)
        {
            super(vertexCount);
        }
        
        @Override
        public Triple newInstance(int vertexCount)
        {
            return new Triple(vertexCount);
        }
        
        @Override
        protected void copyProperties(IPolygon fromObject)
        {
            super.copyProperties(fromObject);
            if(fromObject instanceof Triple)
            {
                this.tripleDelegate.copyPropertiesFrom(((Triple)fromObject).tripleDelegate);
            }
        }

        @Override
        protected IVertexFactory vertexFactory()
        {
            return MultiTextureVertex.Triple.FACTORY;
        }

        @Override
        public int layerCount()
        {
            return 3;
        }
        
        private class TripleDelegate extends PaintableSubQuad
        {
            
            @Override
            public IMutablePolygon getParent()
            {
                return Triple.this;
            }

            @Override
            protected int layerIndex()
            {
                return 2;
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
            case 2:
                return this.tripleDelegate;
            default:
                throw new IndexOutOfBoundsException();
            }
        }
    }
}
