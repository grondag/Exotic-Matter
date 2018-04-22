package grondag.exotic_matter.render;

import java.util.Iterator;

import javax.annotation.Nullable;

import grondag.exotic_matter.varia.SimpleUnorderedArrayList;



public interface ICSGSplitAcceptor
{
    public void acceptFront(ICSGPolygon poly);
    public void acceptBack(ICSGPolygon poly);
    public void acceptCoplanarFront(ICSGPolygon poly);
    public void acceptCoplanarBack(ICSGPolygon poly);
    public boolean hasBack();
    public Iterator<ICSGPolygon> allBack();
    public boolean hasFront();
    public Iterator<ICSGPolygon> allFront();
    
    public static class FrontBack implements ICSGSplitAcceptor
    {

        protected final SimpleUnorderedArrayList<ICSGPolygon> front = new SimpleUnorderedArrayList<>();
        protected final SimpleUnorderedArrayList<ICSGPolygon> back = new SimpleUnorderedArrayList<>();
        
        public void clear()
        {
            this.front.clear();;
            this.back.clear();;
        }
        
        @Override
        public void acceptFront(ICSGPolygon poly)
        {
            this.front.add(poly);
        }

        @Override
        public void acceptBack(ICSGPolygon poly)
        {
            this.back.add(poly);
        }

        @Override
        public void acceptCoplanarFront(ICSGPolygon poly)
        {
            this.acceptFront(poly);
        }

        @Override
        public void acceptCoplanarBack(ICSGPolygon poly)
        {
            this.acceptBack(poly);
        }

        @Override
        public boolean hasBack()
        {
            return !this.back.isEmpty();
        }

        @Override
        public Iterator<ICSGPolygon> allBack()
        {
            return this.back.iterator();
        }

        @Override
        public boolean hasFront()
        {
            return !this.front.isEmpty();
        }

        @Override
        public Iterator<ICSGPolygon> allFront()
        {
            return this.front.iterator();
        }
        
    }
    
    /**
     * Assigns co-planar polys to a CSG node and front back 
     * to separate lists.  allFront and allBack do NOT include coplanars.
     * Will throw NPE if coplanar node not set!
     */
    public static class CoFrontBack extends  FrontBack
    {
        private @Nullable CSGNode coplanarNode;
        
        @Override
        public void clear()
        {
            super.clear();
            this.coplanarNode = null;
        }
        
        public void setCoplanarNode(CSGNode coplanarNode)
        {
            this.coplanarNode = coplanarNode;
        }
        
        @Override
        public void acceptCoplanarFront(ICSGPolygon poly)
        {
            this.coplanarNode.add(poly);
        }

        @Override
        public void acceptCoplanarBack(ICSGPolygon poly)
        {
            this.coplanarNode.add(poly);
        }
    }
}
