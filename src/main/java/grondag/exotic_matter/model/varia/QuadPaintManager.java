package grondag.exotic_matter.model.varia;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.function.Consumer;

import grondag.exotic_matter.model.painting.PaintLayer;
import grondag.exotic_matter.model.painting.QuadPainterFactory;
import grondag.exotic_matter.model.painting.QuadrantSplitter;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.IMutablePolygon;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.primitives.Poly;
import grondag.exotic_matter.model.state.ISuperModelState;

/**
 * Low-garbage consumer for quads from mesh generators that
 * manages instantiation and processing of painters and then
 * passes through painted quad to another consumer.
 *
 */
public class QuadPaintManager
{
    private static ThreadLocal<Manager> managers = ThreadLocal.withInitial(() -> new Manager());

    public static final Consumer<IPolygon> provideReadyConsumer(final ISuperModelState modelState, final boolean isItem, final Consumer<IPolygon> target)
    {
        Manager result = managers.get();
        result.clear();
        result.modelState = modelState;
        result.isItem = isItem;
        result.target = target;
        return result;
    }
    
    private static class Manager implements Consumer<IPolygon>
    {
        @SuppressWarnings("null")
        private ISuperModelState modelState;
        private boolean isItem; 
        @SuppressWarnings("null")
        private Consumer<IPolygon> target;
        
        private ArrayDeque<PainterList> emptyLists
             = new ArrayDeque<PainterList>(8);
        
        
        private final IdentityHashMap<Surface, PainterList> surfaces
            = new IdentityHashMap<>();
        
        private void clear()
        {
            for(PainterList list : surfaces.values())
            {
                list.clear();
                emptyLists.add(list);
            }
            surfaces.clear();
        }
        
        private PainterList paintersForSurface(Surface surface)
        {
            PainterList result = surfaces.get(surface);
            if(result == null)
            {
                result =  emptyLists.isEmpty() 
                        ? new PainterList()
                        : emptyLists.pop();
                        
                for(PaintLayer l : PaintLayer.VALUES)
                {
                    if(modelState.isLayerEnabled(l) && !surface.isLayerDisabled(l))
                    {
                        result.add(QuadPainterFactory.getPainterForSurface(modelState, surface, l));
                    }
                }
                        
                surfaces.put(surface, result);
            }
            return result;
        }
        
        @Override
        public void accept(@SuppressWarnings("null") IPolygon poly)
        {
            PainterList painters = paintersForSurface(poly.getSurfaceInstance());
            if(painters.isEmpty()) return;
            
            // if lockUV is on, derive UV coords by projection
            // of vertex coordinates on the plane of the quad's face
            
            // do this here to avoid doing it for all painters
            // and because the quadrant split test requires it.
            if(poly.isLockUV())
            {
                IMutablePolygon q = Poly.mutableCopyOf(poly);
                q.assignLockedUVCoordinates();
                poly = q;
            }
            
            if(painters.needsQuadrants() && QuadrantSplitter.uvQuadrant(poly) == null)
            {
                QuadrantSplitter.splitAndPaint(poly, 
                        q -> painters.producePaintedQuads(q, target, isItem));
            }
            else
            {
                painters.producePaintedQuads(poly, target, isItem);
            }
        }
      
    }
}
