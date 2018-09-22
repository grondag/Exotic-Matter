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
    
    public final long[] rawBits()
    {
        return voxelBits;
    }
    
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
    
    private static final ThreadLocal<int[]> stackHolder = new ThreadLocal<int[]>()
    {
        @Override
        protected int[] initialValue()
        {
            return new int[512];
        }
    };
    
    public final void visit(IOctreeVisitor visitor)
    {
        if(!visitor.visit(0, 0, false))
            return;
        
        final int[] stack = stackHolder.get();
        int stackPointer = 7;
        addSubNodesToStack(1 << 16, 0, stack);
        
        while(stackPointer >= 0)
        {
            final int address = stack[stackPointer--];
            final int divisionLevel = address >> 16;
            final int index = address & 0xFFFF;
            final boolean leaf = divisionLevel == maxDivisionLevel;
            if(visitor.visit(index, divisionLevel, leaf) & !leaf)
            {
                addSubNodesToStack(((divisionLevel + 1) << 16) | (index << 3), stackPointer + 1, stack);
                stackPointer += 8;
            }
        }
    }
    
    protected final void addSubNodesToStack(final int startingNodeIndex, final int firstStackIndex, int[] stack)
    {
        stack[firstStackIndex] = startingNodeIndex;
        stack[firstStackIndex + 1] = startingNodeIndex + 1;
        stack[firstStackIndex + 2] = startingNodeIndex + 2;
        stack[firstStackIndex + 3] = startingNodeIndex + 3;
        stack[firstStackIndex + 4] = startingNodeIndex + 4;
        stack[firstStackIndex + 5] = startingNodeIndex + 5;
        stack[firstStackIndex + 6] = startingNodeIndex + 6;
        stack[firstStackIndex + 7] = startingNodeIndex + 7;
    }
    
//    protected final void visitNodes(final int startingIndex, final int divisionLevel, IOctreeVisitor visitor)
//    {
//        visitInner(startingIndex, divisionLevel, visitor);
//        visitInner(startingIndex + 1, divisionLevel, visitor);
//        visitInner(startingIndex + 2, divisionLevel, visitor);
//        visitInner(startingIndex + 3, divisionLevel, visitor);
//        visitInner(startingIndex + 4, divisionLevel, visitor);
//        visitInner(startingIndex + 5, divisionLevel, visitor);
//        visitInner(startingIndex + 6, divisionLevel, visitor);
//        visitInner(startingIndex + 7, divisionLevel, visitor);
//    }
//    
//    /**
//     * Never called for leaf nodes.
//     */
//    private final void visitInner(int index, int divisionLevel, IOctreeVisitor visitor)
//    {
//        final boolean leaf = divisionLevel == maxDivisionLevel;
//        if(visitor.visit(index, divisionLevel, leaf) & !leaf)
//            visitNodes(index << 3, divisionLevel + 1, visitor);
//    }
    
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
