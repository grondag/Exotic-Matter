//package grondag.exotic_matter.model.primitives.vertex;
//
//import javax.annotation.Nullable;
//
//import grondag.exotic_matter.varia.ColorHelper;
//
//public class MultiTextureVertex
//{
//    public static class Double extends Vertex
//    {
//        public static final IVertexFactory FACTORY = new IVertexFactory()
//        {
//            @Override
//            public Vertex newVertex(float x, float y, float z, float u, float v, int color, @Nullable Vec3f normal, int glow)
//            {
//                return new Double(x, y, z, u, v, color, normal, glow);
//            }
//
//            @Override
//            public Vertex interpolate(float newX, float newY, float newZ, @Nullable Vec3f newNormal, IVertex from, IVertex to, float toWeight)
//            {
//                final int newGlow = (int) (from.glow() + (to.glow() - from.glow()) * toWeight);
//                final int newColor = ColorHelper.interpolate(from.color(), to.color(), toWeight);
//                final float newU = from.u() + (to.u() - from.u()) * toWeight;
//                final float newV = from.v() + (to.v() - from.v()) * toWeight;
//                
//                Second from2 = ((Double)from).second;
//                Second to2 = ((Double)to).second;
//                Second new2 = (Second) from2.interpolate(to2, toWeight);
//                
//                return new Double(newX, newY, newZ, newU, newV, newColor, newNormal, newGlow, new2);
//            }
//
//            @Override
//            public Vertex withColorGlow(Vertex vertex, int colorIn, int glowIn)
//            {
//                return new Double(vertex.pos.x(), vertex.pos.y(), vertex.pos.z(), vertex.u, vertex.v, colorIn, vertex.normal, glowIn, ((Double)vertex).second.copy());
//            }
//
//            @Override
//            public Vertex withUV(Vertex vertex, float uNew, float vNew)
//            {
//                return new Double(vertex.pos.x(), vertex.pos.y(), vertex.pos.z(), uNew, vNew, vertex.color, vertex.normal, vertex.glow, ((Double)vertex).second.copy());
//            }
//
//            @Override
//            public Vertex withGeometry(Vertex vertex, float x, float y, float z, @Nullable Vec3f normal)
//            {
//                return new Double(x, y, z, vertex.u, vertex.v, vertex.color, normal, vertex.glow, ((Double)vertex).second.copy());
//            }
//    
//        };
//                
//        protected final Second second;
//        
//        protected Double(float x, float y, float z, float u, float v, int color, @Nullable Vec3f normal, int glow, Second second)
//        {
//            super(x, y, z, u, v, color, normal, glow);
//            this.second = second;
//            second.setParent(this);
//        }
//        
//        protected Double(float x, float y, float z, float u, float v, int color, @Nullable Vec3f normal, int glow)
//        {
//            this(x, y, z, u, v, color, normal, glow, new Second(u, v, color, glow));
//        }
//        
//        protected Double withNewSecond(float u, float v, int color, int glow)
//        {
//            return new Double(this.pos.x(), this.pos.y(), this.pos.z(), this.u, this.v, this.color, this.normal, this.glow, new Second(u, v, color, glow));
//        }
//        
//        @Override
//        protected IVertexFactory factory()
//        {
//            return Double.FACTORY;
//        }
//
//        @Override
//        public IPaintableVertex forTextureLayer(int layer)
//        {
//            switch(layer)
//            {
//            case 0:
//                return this;
//            case 1:
//                return second;
//            default:
//                throw new IndexOutOfBoundsException();
//            }
//        }
//        
//        protected static class Second extends PaintableSubVertex<Double>
//        {
//            protected Second(float u, float v, int color, int glow)
//            {
//                super(u, v, color, glow);
//            }
//            
//            @Override
//            protected Second factory(float u, float v, int color, int glow)
//            {
//                return new Second(u, v, color, glow);
//            }
//
//            @Override
//            protected Second copy()
//            {
//                return (Second) super.copy();
//            }
//            
//            @Override
//            public IPaintableVertex withUV(float u, float v)
//            {
//                return parent.withNewSecond(u, v, this.color, this.glow);
//            }
//
//            @Override
//            public IPaintableVertex withColorGlow(int colorIn, int glowIn)
//            {
//                return parent.withNewSecond(this.u, this.v, colorIn, glowIn);
//            }
//
//            @Override
//            public int layerIndex()
//            {
//                return 1;
//            }
//        }
//    }
//    
//    
//    public static class Triple extends Double
//    {
//        public static final IVertexFactory FACTORY = new IVertexFactory()
//        {
//            @Override
//            public Vertex newVertex(float x, float y, float z, float u, float v, int color, @Nullable Vec3f normal, int glow)
//            {
//                return new Triple(x, y, z, u, v, color, normal, glow);
//            }
//
//            @Override
//            public Vertex interpolate(float newX, float newY, float newZ, @Nullable Vec3f newNormal, IVertex from, IVertex to, float toWeight)
//            {
//                final int newGlow = (int) (from.glow() + (to.glow() - from.glow()) * toWeight);
//                final int newColor = ColorHelper.interpolate(from.color(), to.color(), toWeight);
//                final float newU = from.u() + (to.u() - from.u()) * toWeight;
//                final float newV = from.v() + (to.v() - from.v()) * toWeight;
//                
//                Second from2 = ((Double)from).second;
//                Second to2 = ((Double)to).second;
//                Second new2 = (Second) from2.interpolate(to2, toWeight);
//                
//                Third from3 = ((Triple)from).third;
//                Third to3 = ((Triple)to).third;
//                Third new3 = (Third) from3.interpolate(to3, toWeight);
//                
//                return new Triple(newX, newY, newZ, newU, newV, newColor, newNormal, newGlow, new2, new3);
//            }
//
//            @Override
//            public Vertex withColorGlow(Vertex vertex, int colorIn, int glowIn)
//            {
//                return new Triple(vertex.pos.x(), vertex.pos.y(), vertex.pos.z(), vertex.u, vertex.v, colorIn, vertex.normal, glowIn, 
//                        ((Double)vertex).second.copy(), ((Triple)vertex).third.copy());
//            }
//
//            @Override
//            public Vertex withUV(Vertex vertex, float uNew, float vNew)
//            {
//                return new Triple(vertex.pos.x(), vertex.pos.y(), vertex.pos.z(), uNew, vNew, vertex.color, vertex.normal, vertex.glow, 
//                        ((Double)vertex).second.copy(), ((Triple)vertex).third.copy());
//            }
//
//            @Override
//            public Vertex withGeometry(Vertex vertex, float x, float y, float z, @Nullable Vec3f normal)
//            {
//                return new Triple(x, y, z, vertex.u, vertex.v, vertex.color, normal, vertex.glow, 
//                        ((Double)vertex).second.copy(), ((Triple)vertex).third.copy());
//            }
//        };
//                
//        protected final Third third;
//        
//        protected Triple(float x, float y, float z, float u, float v, int color, @Nullable Vec3f normal, int glow, Second second, Third third)
//        {
//            super(x, y, z, u, v, color, normal, glow, second);
//            this.third = third;
//            third.setParent(this);
//        }
//        
//        protected Triple(float x, float y, float z, float u, float v, int color, @Nullable Vec3f normal, int glow)
//        {
//            this(x, y, z, u, v, color, normal, glow, new Second(u, v, color, glow), new Third(u, v, color, glow));
//        }
//        
//        @Override
//        protected Double withNewSecond(float u, float v, int color, int glow)
//        {
//            return new Triple(this.pos.x(), this.pos.y(), this.pos.z(), this.u, this.v, this.color, this.normal, this.glow, new Second(u, v, color, glow), this.third.copy());
//        }
//        
//        protected Triple withNewThird(float u, float v, int color, int glow)
//        {
//            return new Triple(this.pos.x(), this.pos.y(), this.pos.z(), this.u, this.v, this.color, this.normal, this.glow, this.second.copy(), new Third(u, v, color, glow));
//        }
//        
//        @Override
//        protected IVertexFactory factory()
//        {
//            return Triple.FACTORY;
//        }
//
//        @Override
//        public IPaintableVertex forTextureLayer(int layer)
//        {
//            switch(layer)
//            {
//            case 0:
//                return this;
//            case 1:
//                return second;
//            case 2:
//                return third;
//            default:
//                throw new IndexOutOfBoundsException();
//            }
//        }
//        
//        protected static class Third extends PaintableSubVertex<Triple>
//        {
//            protected Third(float u, float v, int color, int glow)
//            {
//                super(u, v, color, glow);
//            }
//            
//            @Override
//            protected Third factory(float u, float v, int color, int glow)
//            {
//                return new Third(u, v, color, glow);
//            }
//
//            @Override
//            protected Third copy()
//            {
//                return (Third) super.copy();
//            }
//            
//            @Override
//            public IPaintableVertex withUV(float u, float v)
//            {
//                return parent.withNewThird(u, v, this.color, this.glow);
//            }
//
//            @Override
//            public IPaintableVertex withColorGlow(int colorIn, int glowIn)
//            {
//                return parent.withNewThird(this.u, this.v, colorIn, glowIn);
//            }
//
//            @Override
//            public int layerIndex()
//            {
//                return 2;
//            }
//        }
//    }
//}
