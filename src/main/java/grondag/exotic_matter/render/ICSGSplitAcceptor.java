package grondag.exotic_matter.render;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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
    
    public static class ClipAcceptor implements ICSGSplitAcceptor
    {
        int size = 1;
        int index = 0;
        
        Object[] items = new Object[24];
        final List<CSGPolygon> output;
        
        public ClipAcceptor(CSGPolygon toClip, CSGNode firstNode, List<CSGPolygon> output)
        {
            this.output = output;
            this.items[0] = toClip;
            this.items[1] = firstNode;
        }

        /**
         * Advances to the next poly/node to be split. Returns false if at end.
         */
        public boolean advance()
        {
            return ++index < size;
        }
        
        public CSGPolygon currentPoly()
        {
            return (CSGPolygon) this.items[index * 2];
        }
        
        public CSGNode currentNode()
        {
            return (CSGNode) this.items[index * 2 + 1];
        }
        
        private void expandIfNecessary()
        {
            if(size * 2 == this.items.length)
            {
                this.items = Arrays.copyOf(this.items, this.size * 4);
            }
        }
        
        @Override
        public void acceptFront(CSGPolygon poly)
        {
            CSGNode n = this.currentNode();
            if(n.front == null)
            {
                this.output.add(poly);
            }
            else
            {
                this.expandIfNecessary();
                final int i = (size++) * 2;
                this.items[i] = poly;
                this.items[i + 1] = n.front;
            }
        }

        @Override
        public void acceptBack(CSGPolygon poly)
        {
              // not adding back plane polys to the output when
              // we get to leaf nodes is what does the clipping 
            CSGNode n = this.currentNode();
            if(n.back != null)
            {
                this.expandIfNecessary();
                final int i = (size++) * 2;
                this.items[i] = poly;
                this.items[i + 1] = n.back;
            }
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
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<CSGPolygon> allBack()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasFront()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<CSGPolygon> allFront()
        {
            throw new UnsupportedOperationException();
        }
        
    }
    
    public static class FrontBack implements ICSGSplitAcceptor
    {

        protected final SimpleUnorderedArrayList<CSGPolygon> front = new SimpleUnorderedArrayList<>();
        protected final SimpleUnorderedArrayList<CSGPolygon> back = new SimpleUnorderedArrayList<>();
        
        public void clear()
        {
            this.front.clear();
            this.back.clear();
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
