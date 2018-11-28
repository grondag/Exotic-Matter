package grondag.exotic_matter.model.CSG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public abstract class CSGSplitAcceptor
{
    protected int size;
    protected int index;
    protected CSGPolygon[] polys = new CSGPolygon[12];
    protected CSGNode[] nodes = new CSGNode[12];

    public abstract void acceptFront(CSGPolygon poly);
    public abstract void acceptBack(CSGPolygon poly);
    public abstract void acceptCoplanarFront(CSGPolygon poly);
    public abstract void acceptCoplanarBack(CSGPolygon poly);
    
    public final void splitPolyStartingWith(CSGPolygon toClip, CSGNode firstNode)
    {
        this.polys[0] = toClip;
        this.nodes[0] = firstNode;
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
        return this.polys[index];
    }
    
    protected final CSGNode currentNode()
    {
        return this.nodes[index];
    }
    
    protected final void expandIfNecessary()
    {
        if(size  == this.nodes.length)
        {
            this.polys = Arrays.copyOf(this.polys, this.size * 2);
            this.nodes = Arrays.copyOf(this.nodes, this.size * 2);
        }
    }
    
    public final static class ClipAcceptor extends CSGSplitAcceptor
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
                this.polys[size] = poly;
                this.nodes[size++] = n.front;
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
                this.polys[size] = poly;
                this.nodes[size++] = n.back;
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
    public final static class CoFrontBack extends  CSGSplitAcceptor
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
                this.polys[size] = poly;
                this.nodes[size++] = n.front;
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
                this.polys[size] = poly;
                this.nodes[size++] = n.back;
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
