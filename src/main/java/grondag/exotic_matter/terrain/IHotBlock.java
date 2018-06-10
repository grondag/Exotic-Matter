package grondag.exotic_matter.terrain;

public interface IHotBlock
{
    public default int heatLevel() { return 0; }
    
    public default boolean isHot() { return this.heatLevel() != 0; }
}
