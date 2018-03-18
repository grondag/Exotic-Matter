package grondag.exotic_matter.model;

import static grondag.exotic_matter.model.ModelStateData.*;

public enum TextureScale
{
    /** 1x1 */
    SINGLE(0, STATE_FLAG_NONE),
    
    /** 2x2 */
    TINY(1, STATE_FLAG_NEEDS_POS),
    
    /** 4x4 */
    SMALL(2, STATE_FLAG_NEEDS_POS),
    
    /** 8x8 */
    MEDIUM(3, STATE_FLAG_NEEDS_POS),
    
    /** 16x16 */
    LARGE(4, STATE_FLAG_NEEDS_POS),
    
    /** 32x32 */
    GIANT(5, STATE_FLAG_NEEDS_POS);
    
    /** UV length for each subdivision of the texture */
    public final float sliceIncrement;
    
    /** number of texture subdivisions */
    public final int sliceCount;
    
    /** mask to derive a value within the number of slice counts (sliceCount - 1) */
    public final int sliceCountMask;
    
    /** number of texture subdivisions as an exponent of 2 */
    public final int power;
    
    /** identifies the world state needed to drive texture random rotation/selection */
    public final int modelStateFlag;
    
    public TextureScale zoom()
    {
        if(this == GIANT)
        {
            return GIANT;
        }
        else
        {
            return values()[this.ordinal() + 1];
        }
    }
    
    private TextureScale(int power, int modelStateFlag)
    {
        this.power = power;
        this.sliceCount = 1 << power;
        this.sliceCountMask = sliceCount - 1;
        this.sliceIncrement = 16f / sliceCount;
        this.modelStateFlag = modelStateFlag;
    }

}