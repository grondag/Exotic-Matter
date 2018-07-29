package grondag.exotic_matter.model.varia;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import grondag.exotic_matter.ClientProxy;
import grondag.exotic_matter.model.painting.QuadPainter;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.varia.SimpleUnorderedArrayList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PainterList extends SimpleUnorderedArrayList<QuadPainter>
{
    private boolean needsQuadrants = false;
    
    public PainterList()
    {
        //  don't need much space
        super(4);
    }
    
    /** 
     * If true means a layer of this surface is rendering in solid  block layer.
     * When Acuity API active, signals we can emit multi-layered quads.
     */
    private boolean hasSolidBase = false;

    @Override
    public boolean add(@Nullable QuadPainter p)
    {
        if(p == null)
            return false;
        
        if(p.requiresQuadrants())
            this.needsQuadrants = true;
        
        if(p.isSolid())
            this.hasSolidBase = true;
            
        return super.add(p);
    }

    @Override
    public void clear()
    {   
        this.needsQuadrants = false;
        this.hasSolidBase = false;
        super.clear();
    }

    /**
     * Expects that quadrant split has already happened if required.
     * May emit more quads than are input due to surface subdivision.
     */
    public void producePaintedQuads(IPolygon q, Consumer<IPolygon> target, boolean isItem)
    {
        if(this.hasSolidBase && this.size > 1 && ClientProxy.isAcuityEnabled())
        {
            // do multi-layered
            System.out.println("whoops!");
        }
        else
        {
            // emit single-layer quads
            this.forEach(painter -> painter.producePaintedQuad(q, p -> target.accept(p), isItem));
        }
    }

    boolean needsQuadrants()
    {
        return needsQuadrants;
    }
}