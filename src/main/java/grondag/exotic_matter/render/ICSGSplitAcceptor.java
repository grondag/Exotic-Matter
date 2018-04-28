package grondag.exotic_matter.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public abstract class ICSGSplitAcceptor
{
    protected int size;
    protected int index;
    protected Object[] items = new Object[24];

    public abstract void acceptFront(CSGPolygon poly);
    public abstract void acceptBack(CSGPolygon poly);
    public abstract void acceptCoplanarFront(CSGPolygon poly);
    public abstract void acceptCoplanarBack(CSGPolygon poly);
    
    public final void splitPolyStartingWith(CSGPolygon toClip, CSGNode firstNode)
    {
        this.items[0] = toClip;
        this.items[1] = firstNode;
        this.size = 1;
        this.index = 0;
        
        do
        {
            currentNode().plane.splitQuad(currentPoly(), this);
        } 
        while(++index < size);
    }
    
    protected final CSGPolygon currentPoly()
    {
        return (CSGPolygon) this.items[index * 2];
    }
    
    protected final CSGNode currentNode()
    {
        return (CSGNode) this.items[index * 2 + 1];
    }
    
    protected final void expandIfNecessary()
    {
        if(size * 2 == this.items.length)
        {
            this.items = Arrays.copyOf(this.items, this.size * 4);
        }
    }
    
    public final static class ClipAcceptor extends ICSGSplitAcceptor
    {
        private final List<CSGPolygon> output = new ArrayList<>();

        public final List<CSGPolygon> output()
        {
            return this.output;
        }
   
        
        @Override
        public final void acceptFront(CSGPolygon poly)
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
        public final void acceptBack(CSGPolygon poly)
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
        public final void acceptCoplanarFront(CSGPolygon poly)
        {
            this.acceptFront(poly);
            
        }

        @Override
        public final void acceptCoplanarBack(CSGPolygon poly)
        {
            this.acceptBack(poly);
        }
    }
    
    
    /**
     * Assigns co-planar polys to the current node and front back 
     * to stack.  
     */
    public final static class CoFrontBack extends  ICSGSplitAcceptor
    {
        @Override
        public final void acceptFront(CSGPolygon poly)
        {
            CSGNode n = this.currentNode();
            if(n.front == null)
            {
                n.front = new CSGNode(poly);
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
        public final void acceptBack(CSGPolygon poly)
        {
            CSGNode n = this.currentNode();
            if(n.back == null)
            {
                n.back = new CSGNode(poly);
            }
            else
            {
                this.expandIfNecessary();
                final int i = (size++) * 2;
                this.items[i] = poly;
                this.items[i + 1] = n.back;
            }
        }
        
        @Override
        public final void acceptCoplanarFront(CSGPolygon poly)
        {
            this.currentNode().add(poly);
        }

        @Override
        public final void acceptCoplanarBack(CSGPolygon poly)
        {
            this.currentNode().add(poly);
        }
    }
}
