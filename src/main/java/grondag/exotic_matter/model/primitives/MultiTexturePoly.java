package grondag.exotic_matter.model.primitives;

public abstract class MultiTexturePoly
{
    public static class Double extends PolyImpl
    {
        protected final DoubleDelegate doubleDelegate  = new DoubleDelegate();

        public Double(IPolygon fromTemplate)
        {
            this(fromTemplate.vertexCount());
            this.copyProperties(fromTemplate);
            this.copyVertices(fromTemplate);
        }
        
        public Double(int  vertexCount)
        {
            super(vertexCount);
        }
        
        protected IMutablePolygon getParentInner()
        {
            return this;
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
            else
            {
                this.doubleDelegate.copyPropertiesFrom(fromObject);
            }
        }

        @Override
        protected void copyVertices(IPolygon template)
        {
            if(template.getClass() == this.getClass())
                super.copyVertices(template);
            else
            {
                final int c = this.vertexCount();
                for(int i = 0; i < c; i++)
                {
                    Vertex v = template.getVertex(i);
                    this.setVertex(i, vertexFactory().newVertex(v.pos.x(), v.pos.y(), v.pos.z(), v.u, v.v, v.color, v.normal, v.glow));
                }
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
                return getParentInner();
            }

            @Override
            protected int layerIndex()
            {
                return 1;
            }

            @Override
            public boolean isEmissive()
            {
                return EMISSIVE_BIT_2.getValue(Double.this);
            }

            @Override
            public void setEmissive(boolean isEmissive)
            {
                EMISSIVE_BIT_2.setValue(isEmissive, Double.this);
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
        
        public Triple(IPolygon fromTemplate)
        {
            this(fromTemplate.vertexCount());
            this.copyProperties(fromTemplate);
            this.copyVertices(fromTemplate);
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
            else
            {
                this.tripleDelegate.copyPropertiesFrom(fromObject);
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
                return getParentInner();
            }

            @Override
            protected int layerIndex()
            {
                return 2;
            }

            @Override
            public boolean isEmissive()
            {
                return EMISSIVE_BIT_3.getValue(Triple.this);
            }

            @Override
            public void setEmissive(boolean isEmissive)
            {
                EMISSIVE_BIT_3.setValue(isEmissive, Triple.this);
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
