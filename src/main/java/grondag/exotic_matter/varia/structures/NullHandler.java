package grondag.exotic_matter.varia.structures;

import javax.annotation.Nullable;

public class NullHandler
{
    public static <T> T defaultIfNull(@Nullable final T checkedValue, final T defaultValue)
    {
        return checkedValue == null ? defaultValue : checkedValue;
    }
}
