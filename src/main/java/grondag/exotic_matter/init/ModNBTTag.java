package grondag.exotic_matter.init;

/**
 * Provides concise and consistent (albeit human-unfriendly) tags for serialization. 
 * DO NOT MODIFY ORDER OR REMOVE VALUES UNLESS PUBLISHING A WORLD-BREAKING RELEASE.
 * 
 * @author grondag
 *
 */
public class ModNBTTag
{
    private static int nextID = 0;

    public final static String LOCATION_DIMENSION = "xm" + Integer.toUnsignedString(++nextID, Character.MAX_RADIX);
    public final static String LOCATION_POSITION = "xm" + Integer.toUnsignedString(++nextID, Character.MAX_RADIX);
    public final static String BLOCK_SUBSTANCE = "xm" + Integer.toUnsignedString(++ModNBTTag.nextID, Character.MAX_RADIX);


}
