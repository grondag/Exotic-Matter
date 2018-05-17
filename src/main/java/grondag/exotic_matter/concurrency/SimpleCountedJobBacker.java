package grondag.exotic_matter.concurrency;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Re-generates array needed by counted job
 * if it marked dirty.  Requires that you call
 * {@link #setDirty()} any time you modify the collection
 *
 */
@Deprecated
public abstract class SimpleCountedJobBacker implements ICountedJobBacker
{
    private @Nullable volatile Object[] operands = null;
    
    public void setDirty()
    {
        synchronized(this)
        {
            this.operands = null;
        }
    }

    @Nonnull
    protected abstract Object[] generateOperands();
    
    @Override
    public Object[] getOperands()
    {
        Object[] result = this.operands;
        if(result == null)
        {
            synchronized(this)
            {
                result = this.operands;
                if(result == null)
                {
                    result = this.generateOperands();
                    this.operands = result;
                }
            }
        }
        return result;
    }

    @Override
    public int size()
    {
        return this.getOperands().length;
    }
}
