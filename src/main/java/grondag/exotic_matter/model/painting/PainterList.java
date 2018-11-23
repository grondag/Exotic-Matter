package grondag.exotic_matter.model.painting;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import grondag.acuity.api.TextureFormat;
import grondag.exotic_matter.ClientProxy;
import grondag.exotic_matter.model.primitives.better.IPaintablePoly;
import grondag.exotic_matter.model.primitives.better.IPaintedPoly;
import grondag.exotic_matter.varia.SimpleUnorderedArrayList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PainterList extends SimpleUnorderedArrayList<QuadPainter>
{
    public PainterList()
    {
        //  don't need much space
        super(4);
    }

    @Override
    public boolean add(@Nullable QuadPainter p)
    {
        if(p == null)
            return false;
        
        return super.add(p);
    }

    @Override
    public void clear()
    {   
        super.clear();
    }

    private static class Consumers
    {
        protected Consumer<IPaintedPoly> wrappedTarget;
        protected Consumer<IPaintablePoly> firstInnerTarget;
        protected Consumer<IPaintablePoly> secondInnerTarget;
        protected Consumer<IPaintablePoly> thirdInnerTarget;
        protected QuadPainter firstPainter;
        protected QuadPainter secondPainter;
        protected QuadPainter thirdPainter;
        protected boolean isItem;
        
        /**
         * Converts mutable output to immutable and then releases.
         */
        protected final Consumer<IPaintablePoly> outputTarget = new Consumer<IPaintablePoly>()
        {
            @Override
            public void accept(@SuppressWarnings("null") IPaintablePoly p)
            {
                wrappedTarget.accept(p.toPainted());   
                p.release();
            }
        };
        
        protected final Consumer<IPaintablePoly> firstTarget = new Consumer<IPaintablePoly>()
        {
            @Override
            public void accept(@SuppressWarnings("null") IPaintablePoly p)
            {
                if(firstPainter.requiresQuadrants() && QuadrantSplitter.uvQuadrant(p, 0) == null)
                    QuadrantSplitter.splitAndPaint(p, q -> firstPainter.producePaintedQuad(q, firstInnerTarget, isItem), 0);
                else
                    firstPainter.producePaintedQuad(p, firstInnerTarget, isItem);
            }
        };
        
        protected final Consumer<IPaintablePoly> secondTarget = new Consumer<IPaintablePoly>()
        {
            @Override
            public void accept(@SuppressWarnings("null") IPaintablePoly p)
            {
                if(secondPainter.requiresQuadrants() && QuadrantSplitter.uvQuadrant(p, 1) == null)
                    QuadrantSplitter.splitAndPaint(p, q -> secondPainter.producePaintedQuad(q, secondInnerTarget, isItem), 1);
                else
                    secondPainter.producePaintedQuad(p, secondInnerTarget, isItem);
            }
        };
        
        protected final Consumer<IPaintablePoly> thirdTarget = new Consumer<IPaintablePoly>()
        {
            @Override
            public void accept(@SuppressWarnings("null") IPaintablePoly p)
            {
                if(thirdPainter.requiresQuadrants() && QuadrantSplitter.uvQuadrant(p, 2) == null)
                    QuadrantSplitter.splitAndPaint(p, q -> thirdPainter.producePaintedQuad(q, thirdInnerTarget, isItem), 2);
                else
                    thirdPainter.producePaintedQuad(p, thirdInnerTarget, isItem);
            }
        };
        
        protected Consumer<IPaintablePoly> prepareSingle(Consumer<IPaintedPoly> target, boolean isItem, PainterList painters)
        {
            this.isItem = isItem;
            this.wrappedTarget = target;
            this.firstPainter = painters.get(0);
            this.firstInnerTarget = outputTarget;
            return firstTarget;
        }
        
        protected Consumer<IPaintablePoly> prepareDouble(Consumer<IPaintedPoly> target, boolean isItem, PainterList painters)
        {
            this.isItem = isItem;
            this.wrappedTarget = target;
            this.firstPainter = painters.get(0);
            this.secondPainter = painters.get(1);
            this.firstInnerTarget = secondTarget;
            this.secondInnerTarget = outputTarget;
            return firstTarget;
        }
        
        protected Consumer<IPaintablePoly> prepareTriple(Consumer<IPaintedPoly> target, boolean isItem, PainterList painters)
        {
            this.isItem = isItem;
            this.wrappedTarget = target;
            this.firstPainter = painters.get(0);
            this.secondPainter = painters.get(1);
            this.thirdPainter = painters.get(2);
            this.firstInnerTarget = secondTarget;
            this.secondInnerTarget = thirdTarget;
            this.thirdInnerTarget = outputTarget;
            return firstTarget;
        }
    };
    
    private static ThreadLocal<Consumers> consumers = new ThreadLocal<Consumers>()
    {
        @Override
        protected Consumers initialValue() 
        {
            return new Consumers();
        }
    };
    
    /**
     * Expects that quadrant split has already happened if required.
     * May emit more quads than are input due to surface subdivision.
     */
    @SuppressWarnings("null")
    public void producePaintedQuads(IPaintablePoly q, Consumer<IPaintedPoly> target, boolean isItem)
    {
        switch(this.size)
        {
            case 0:
                break;
                
            case 1:
                this.get(0).producePaintedQuad(q, consumers.get().prepareSingle(target, isItem, this), isItem);
                break;
                
            case 2:
                // make sure has an appropriate pipeline, some models may set up before we get here
                if(q.getPipeline().textureFormat().layerCount() != 2)
                    q.setPipeline(ClientProxy.acuityDefaultPipeline(TextureFormat.DOUBLE));

                this.get(0).producePaintedQuad(q, consumers.get().prepareDouble(target, isItem, this), isItem);
                break;
                
            case 3:
                // make sure has an appropriate pipeline, some models may set up before we get here
                if(q.getPipeline().textureFormat().layerCount() != 3)
                    q.setPipeline(ClientProxy.acuityDefaultPipeline(TextureFormat.TRIPLE));

                this.get(0).producePaintedQuad(q, consumers.get().prepareTriple(target, isItem, this), isItem);
                break;
                
            default:
                throw new ArrayIndexOutOfBoundsException();
        }
    }
}