package grondag.exotic_matter.model.primitives.polygon;

public abstract class IPolygonExt
{
    public static interface IMarkedPolygon extends IPolygon
    {
        default void flipMark()
        {
            this.setMark(!this.isMarked());
        }
        
        void setMark(boolean isMarked);
    }
    
    public static interface IDeletablePolygon extends IPolygon
    {
        void setDeleted();
    }
    
    public static interface ILinkedPolygon extends IPolygon
    {
        void setLink(int link);
    }
    
    public static interface ITaggablePolygon extends IPolygon
    {
        void setTag(int tag);
    }
    
    public static interface ICSGPolygon extends IMarkedPolygon, IDeletablePolygon, ILinkedPolygon, ITaggablePolygon
    {
        
    }
    
    public static interface ICSGMutablePolygon extends IMutablePolygon, ICSGPolygon
    {
        
    }
}