package grondag.exotic_matter.varia;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Iterator;

import javax.annotation.Nullable;

/**
 * Lightweight, non-concurrent collection-like class for managing small unordered lists.
 * Uses = for comparison.
 * @author grondag
 *
 */
public class AbstractUnorderedArrayList<T> extends AbstractCollection<T>
{

    protected Object[] items = new Object[4];
    
    protected int size = 0;
    
    @Override
    public int size()
    {
        return this.size;
    }
    
    @Override
    public boolean isEmpty()
    {
        return this.size == 0;
    }
    
    @Override
    public boolean add(@Nullable T newItem)
    {
        if(this.size == this.items.length)
        {
            this.increaseCapacity();
        }
        this.items[size++] = newItem;
        return true;
    }
    
    /** returns index of item if it exists in this list. -1 if not. */
    public int findIndex(T itemToFind)
    {
        for(int i = this.size - 1; i >= 0; i--)
        {
            if(items[i] == itemToFind) return i;
        }
        
        return -1;
    }
    
    @SuppressWarnings("unchecked")
    @Nullable
    public T get(int index)
    {
        return (T) this.items[index];
    }
    
    /** Does NOT preserve order! */
    public void remove(int index)
    {
        assert this.size > 0 : "SimpleUnoderedArrayList detected attempt to remove item from empty list.";

        if(this.size > 0)
        {
            this.size--;
            if(index < size)
            {
                this.items[index] = this.items[size];
            }
            this.items[size] = null;
        }
    }
    
    @Override
    public boolean remove(@Nullable Object itemToRemove)
    {
        for(int i = this.size - 1; i >= 0; i--)
        {
            if(items[i] == itemToRemove)
            {
                this.remove(i);
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean contains(@Nullable Object itemToFind)
    {
        for(int i = this.size - 1; i >= 0; i--)
        {
            if(items[i] == itemToFind) return true;
        }
        return false;
    }
    
    @Override
    public void clear()
    {
        if(this.size == 0) return;
        
        for(int i = this.size - 1; i >= 0; i--)
        {
            items[i] = null;
        }
        this.size = 0;
    }
    
    private void increaseCapacity()
    {
        int newCapacity = this.items.length * 2;
        this.items = Arrays.copyOf(this.items, newCapacity);
    }

    /**
     * Iterator does not fail on concurrent modification but is
     * not reliable if there are deletions.  Will generally
     * be consistent if all modifications are additions. Will skip
     * items if deletions occur while iterator is active.
     */
    @Override
    public Iterator<T> iterator()
    {
        return new Iterator<T>()
        {
            private int i = 0;

            @Override
            public boolean hasNext()
            {
                return i < size;
            }

            @Override
            public T next()
            {
                return get(i++);
            }};
    }

    public void copyToArray(T[] target, int targetStart)
    {
        System.arraycopy(this.items, 0, target, targetStart, this.size);
    }
}
