package grondag.exotic_matter.model.collision.octree;

import grondag.exotic_matter.model.collision.BoxFinderUtils;

/**
 * Voxels of a unit cube navigable as an OctTree.  
 * Meant for high-performance voxelization of block models.<p>
 * 
 * Strictly speaking, this is <em>navigable</em> as an Octree which is
 * useful to shortcut tests and other operations, but it is 
 * implemented as a straightforward array of bits (as longs).
 * This is simple and performant for our particular use case.
 */
public abstract class AbstractVoxelOctree
{
    protected final long[] voxelBits;
    protected final long[] fillBits;
    protected final int maxDivisionLevel;
    
    protected  AbstractVoxelOctree(int maxDivisionlevel)
    {
        int wordCount = 8 << ((maxDivisionlevel - 3) * 3);
        this.maxDivisionLevel = maxDivisionlevel;
        voxelBits = new long[wordCount];
        fillBits = new long[wordCount];
    }
    
    abstract public void clear(int index, int divisionLevel);
    
    public final void clear()
    {
        clear(0, 0);
    }
    
    public abstract boolean isEmpty(int index, int divisionLevel);

    public final boolean isEmpty()
    {
        return isEmpty(0, 0);
    }
    
    public abstract boolean isFull(int index, int divisionLevel);
    
    public final boolean isFull()
    {
        return isFull(0, 0);
    }
    
    public abstract void setFull(int index, int divisionLevel);
    
    public final void setFull()
    {
        setFull(0, 0);
    }
    
    public final void visit(IOctreeVisitor visitor)
    {
        if(visitor.visit(0, 0, false))
            visitNodes(0, 1, visitor);
    }
    
    protected final void visitNodes(int startingIndex, int divisionLevel, IOctreeVisitor visitor)
    {
        visitInner(startingIndex++, divisionLevel, visitor);
        visitInner(startingIndex++, divisionLevel, visitor);
        visitInner(startingIndex++, divisionLevel, visitor);
        visitInner(startingIndex++, divisionLevel, visitor);
        visitInner(startingIndex++, divisionLevel, visitor);
        visitInner(startingIndex++, divisionLevel, visitor);
        visitInner(startingIndex++, divisionLevel, visitor);
        visitInner(startingIndex, divisionLevel, visitor);
    }
    
    /**
     * Never called for leaf nodes.
     */
    private final void visitInner(int index, int divisionLevel, IOctreeVisitor visitor)
    {
        final boolean leaf = divisionLevel == maxDivisionLevel;
        if(visitor.visit(index, divisionLevel, leaf) & !leaf)
            visitNodes(index << 3, divisionLevel + 1, visitor);
    }
    
    public abstract void fillInterior();
    
    /**
     * Makes leaf nodes that are mostly full completely full, or otherwise makes them empty.
     */
    public void simplify()
    {
        final long[] bits = this.voxelBits;
        
        for(int i = 0; i < bits.length; i++)
        {
            long b = bits[i];
            if(b == 0 || b == -1L) continue;
            b = simplifyInner(b, 0, 0xFFL);
            b = simplifyInner(b, 1 * 8, 0xFF00L);
            b = simplifyInner(b, 2 * 8, 0xFF0000L);
            b = simplifyInner(b, 3 * 8, 0xFF000000L);
            b = simplifyInner(b, 4 * 8, 0xFF00000000L);
            b = simplifyInner(b, 5 * 8, 0xFF0000000000L);
            b = simplifyInner(b, 6 * 8, 0xFF000000000000L);
            b = simplifyInner(b, 7 * 8, 0xFF00000000000000L);
            bits[i] = b;
        }
    }
    
    private long simplifyInner(long bits, int shift, long mask)
    {
        int b = (int)(bits >> shift) & 0xFF;
        return (b == 0 | b == 0xFF)
                ? bits
                : BoxFinderUtils.bitCount8(b) >= 4
                    ? (bits | mask)
                    : (bits &= ~mask);
    }
}