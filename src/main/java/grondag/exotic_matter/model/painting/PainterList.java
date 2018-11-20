package grondag.exotic_matter.model.painting;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import grondag.acuity.api.TextureFormat;
import grondag.exotic_matter.ClientProxy;
import grondag.exotic_matter.ExoticMatter;
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
    @SuppressWarnings("null")
    public void producePaintedQuads(IMutablePolygon q, Consumer<IPolygon> target, boolean isItem)
    {
        // Item render currently doesn't support multi-textured quads.
        // Could handle in IPolygon.addBakedQuadsToBuilder but this is easier until item support happens.
        if(!isItem && this.hasSolidBase && ExoticMatter.proxy.isAcuityEnabled())
        {
            switch(this.size)
            {
            case 0:
                break;
                
            case 1:
                this.get(0).producePaintedQuad(q, p -> target.accept(p.getParent()), isItem);
                break;
                
            case 2:
                q = new MultiTexturePoly.Double(q);
                
                // make sure has an appropriate pipeline, some models may set up before we get here
                if(q.textureFormat() != TextureFormat.DOUBLE)
                    q.setPipeline(ClientProxy.acuityDefaultPipeline(TextureFormat.DOUBLE));
                
                this.get(0).producePaintedQuad(q, p0 -> 
                {
                    this.get(1).producePaintedQuad(p0.getSubPoly(1), p1 -> target.accept(p1.getParent()), isItem);
                }, isItem);
                break;
                
            case 3:
                q = new MultiTexturePoly.Triple(q);
                
                // make sure has an appropriate pipeline, some models may set up before we get here
                if(q.textureFormat() != TextureFormat.TRIPLE)
                    q.setPipeline(ClientProxy.acuityDefaultPipeline(TextureFormat.TRIPLE));
                this.get(0).producePaintedQuad(q, p0 -> 
                {
                    this.get(1).producePaintedQuad(p0.getSubPoly(1), p1 -> 
                    {
                        this.get(2).producePaintedQuad(p1.getSubPoly(2), p2 -> target.accept(p2.getParent()), isItem);
                    }, isItem);
                }, isItem);
                break;
                
            default:
                throw new ArrayIndexOutOfBoundsException();
            }
        }
        else
        {
            // emit single-layer quads
            // avoid making an extra copy of input if we don't have to
            final int end = this.size - 1;
            if(end > 0)
            {
                for(int i = 0; i < end ; i++)
                {
                    this.get(i).producePaintedQuad(q.paintableCopyWithVertices(), p -> target.accept(p.getParent()), isItem);
                }
            }
            this.get(end).producePaintedQuad(q, p -> target.accept(p.getParent()), isItem);
        }
    }

    boolean needsQuadrants()
    {
        return needsQuadrants;
    }
}