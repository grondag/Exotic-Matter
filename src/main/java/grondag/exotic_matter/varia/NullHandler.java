package grondag.exotic_matter.varia;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NullHandler
{
    @Nonnull
    public static <T> T defaultIfNull(@Nullable final T checkedValue, @Nonnull final T defaultValue)
    {
        return checkedValue == null ? defaultValue : checkedValue;
    }
}
