package grondag.exotic_matter.model.collision.octree;

@FunctionalInterface
public interface IOctreeVisitor
{
    /**
     * Returns true if sub-nodes should be visited.
     * True result will be ignored if this is a leaf node.
     */
    boolean visit(int index, int divisionLevel, boolean isLeaf);
}
