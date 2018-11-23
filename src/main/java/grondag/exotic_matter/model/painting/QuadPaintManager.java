package grondag.exotic_matter.model.painting;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.function.Consumer;

import grondag.exotic_matter.model.primitives.better.IMutablePolygon;
import grondag.exotic_matter.model.primitives.better.IPolygon;
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
        private ISuperModelState modelState;
        private boolean isItem; 
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
            
            IMutablePolygon quad = poly.claimCopy();
            
            // do this here to avoid doing it for all painters
            // and because the quadrant split test requires it.
            quad.assignAllLockedUVCoordinates();
            
            painters.producePaintedQuads(quad, target, isItem);
          
        }
      
    }
}
