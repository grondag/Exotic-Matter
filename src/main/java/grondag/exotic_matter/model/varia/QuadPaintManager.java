package grondag.exotic_matter.model.varia;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.function.Consumer;

import grondag.exotic_matter.model.painting.PaintLayer;
import grondag.exotic_matter.model.painting.QuadPainter;
import grondag.exotic_matter.model.painting.QuadPainterFactory;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.varia.SimpleUnorderedArrayList;

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
        
        private ArrayDeque<SimpleUnorderedArrayList<QuadPainter>> emptyLists
             = new ArrayDeque<SimpleUnorderedArrayList<QuadPainter>>(8);
        
        
        private final IdentityHashMap<Surface, SimpleUnorderedArrayList<QuadPainter>> surfaces
            = new IdentityHashMap<>();
        
        private void clear()
        {
            for(SimpleUnorderedArrayList<QuadPainter> list : surfaces.values())
            {
                list.clear();
                emptyLists.add(list);
            }
            surfaces.clear();
        }
        
        private SimpleUnorderedArrayList<QuadPainter> paintersForSurface(Surface surface)
        {
            SimpleUnorderedArrayList<QuadPainter> result = surfaces.get(surface);
            if(result == null)
            {
                result =  emptyLists.isEmpty() 
                        ? new SimpleUnorderedArrayList<QuadPainter>()
                        : emptyLists.pop();
                        
                for(PaintLayer l : PaintLayer.VALUES)
                {
                    if(modelState.isLayerEnabled(l) && !surface.isLayerDisabled(l))
                        result.add(QuadPainterFactory.getPainterForSurface(modelState, surface, l));
                }
                        
                surfaces.put(surface, result);
            }
            return result;
        }
        
        @Override
        public void accept(@SuppressWarnings("null") IPolygon t)
        {
            SimpleUnorderedArrayList<QuadPainter> painters = paintersForSurface(t.getSurfaceInstance());
            if(!painters.isEmpty())
                painters.forEach(p -> p.producePaintedQuad(t, target, isItem));
        }
      
    }
}
