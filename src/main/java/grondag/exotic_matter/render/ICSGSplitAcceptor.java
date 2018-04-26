package grondag.exotic_matter.render;

import java.util.Iterator;

import javax.annotation.Nullable;

import grondag.exotic_matter.varia.SimpleUnorderedArrayList;



public interface ICSGSplitAcceptor
{
    public void acceptFront(CSGPolygon poly);
    public void acceptBack(CSGPolygon poly);
    public void acceptCoplanarFront(CSGPolygon poly);
    public void acceptCoplanarBack(CSGPolygon poly);
    public boolean hasBack();
    public Iterator<CSGPolygon> allBack();
    public boolean hasFront();
    public Iterator<CSGPolygon> allFront();
    
    public static class FrontBack implements ICSGSplitAcceptor
    {

        protected final SimpleUnorderedArrayList<CSGPolygon> front = new SimpleUnorderedArrayList<>();
        protected final SimpleUnorderedArrayList<CSGPolygon> back = new SimpleUnorderedArrayList<>();
        
        public void clear()
        {
            this.front.clear();;
            this.back.clear();;
        }
        
        @Override
        public void acceptFront(CSGPolygon poly)
        {
            this.front.add(poly);
        }

        @Override
        public void acceptBack(CSGPolygon poly)
        {
            this.back.add(poly);
        }

        @Override
        public void acceptCoplanarFront(CSGPolygon poly)
        {
            this.acceptFront(poly);
        }

        @Override
        public void acceptCoplanarBack(CSGPolygon poly)
        {
            this.acceptBack(poly);
        }

        @Override
        public boolean hasBack()
        {
            return !this.back.isEmpty();
        }

        @Override
        public Iterator<CSGPolygon> allBack()
        {
            return this.back.iterator();
        }

        @Override
        public boolean hasFront()
        {
            return !this.front.isEmpty();
        }

        @Override
        public Iterator<CSGPolygon> allFront()
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
        public void acceptCoplanarFront(CSGPolygon poly)
        {
            this.coplanarNode.add(poly);
        }

        @Override
        public void acceptCoplanarBack(CSGPolygon poly)
        {
            this.coplanarNode.add(poly);
        }
    }
}
