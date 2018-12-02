package grondag.exotic_matter.varia.structures;

import java.util.function.Consumer;

import javax.annotation.Nullable;

/**
 * Lightweight, non-concurrent collection-like class for managing small unordered lists.
 * Uses = for comparison.
 * @author grondag
 *
 */
public class SimpleUnorderedArrayList<T> extends AbstractUnorderedArrayList<T> implements Consumer<T>
{
    public SimpleUnorderedArrayList()
    {
        super();
    }
    
    public SimpleUnorderedArrayList(int startingCapacity)
    {
        super(startingCapacity);
    }
    
    /**
     * Returns true if was added (not already present)
     */
    public boolean addIfNotPresent(T newItem)
    {
        for(int i = this.size - 1; i >= 0; i--)
        {
            if(items[i] == newItem) return false;
        }
        this.add(newItem);
        return true;
    }
    
    public void removeIfPresent(T target)
    {
        super.remove(target);
    }

    @Override
    public void accept(@Nullable T t)
    {
        this.add(t);
    }

}
