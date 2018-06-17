package grondag.exotic_matter.varia;

public class ColorHelper
{
    public static float red(int colorARGB)
    {
        return (float) ((colorARGB >> 16) & 0xFF) / 255;
    }
    
    public static float green(int colorARGB)
    {
        return (float) ((colorARGB >> 8) & 0xFF) / 255;
    }
    
    public static float blue(int colorARGB)
    {
        return (float) (colorARGB & 0xFF) / 255;
    }
    
    public static class CMYK
    {
        public final float cyan;
        public final float magenta;
        public final float yellow;
        public final float keyBlack;
        
        private CMYK(float cyan, float magenta, float yellow, float keyBlack)
        {
            this.cyan = cyan;
            this.magenta = magenta;
            this.yellow = yellow;
            this.keyBlack = keyBlack;
        }
    }
    
    /**
     * Alpha components ignored but does no harm if you happen to have it.
     */
    public static CMYK cmyk(int colorARGB)
    {
        float r = red(colorARGB);
        float g = green(colorARGB);
        float b = blue(colorARGB);
        
        float k = 1 - Math.max(r, Math.max(g, b));
        float c = (1 - r - k) / (1 - k);
        float m = (1 - g - k) / (1 - k);
        float y = (1 - b - k) / (1 - k);
        return new CMYK(c, m, y, k);
    }
    
    public static class CMY
    {
        public final float cyan;
        public final float magenta;
        public final float yellow;
        
        private CMY(float cyan, float magenta, float yellow)
        {
            this.cyan = cyan;
            this.magenta = magenta;
            this.yellow = yellow;
        }
    }
    
    /**
     * Alpha components ignored but does no harm if you happen to have it.
     * Return value is on 0-255 scale.     */
    public static CMY cmy(int colorARGB)
    {
        float r = red(colorARGB);
        float g = green(colorARGB);
        float b = blue(colorARGB);
        
        float c = (1 - r);
        float m = (1 - g);
        float y = (1 - b);
        return new CMY(c, m, y);
    }
    
    public static int interpolate(int from, int to, float toWeight)
    {
        final int a1 = (from >> 24) & 0xFF;
        final int r1 = (from >> 16) & 0xFF;
        final int g1 = (from >> 8) & 0xFF;
        final int b1 = from & 0xFF;

        final int a2 = (to >> 24) & 0xFF;
        final int r2 = (to >> 16) & 0xFF;
        final int g2 = (to >> 8) & 0xFF;
        final int b2 = to & 0xFF;
        
        final int a = (int)(a1 + 0.49f + (a2 - a1) * toWeight) & 0xFF;
        final int r = (int)(r1 + 0.49f + (r2 - r1) * toWeight) & 0xFF;
        final int g = (int)(g1 + 0.49f + (g2 - g1) * toWeight) & 0xFF;
        final int b = (int)(b1 + 0.49f + (b2 - b1) * toWeight) & 0xFF;
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** arguments are assumed to be ARGB */
    public static int multiplyColor(int color1, int color2)
    {
        int red = ((color1 >> 16) & 0xFF) * ((color2 >> 16) & 0xFF) / 0xFF;
        int green = ((color1 >> 8) & 0xFF) * ((color2 >> 8) & 0xFF) / 0xFF;
        int blue = (color1 & 0xFF) * (color2 & 0xFF) / 0xFF;
        int alpha = ((color1 >> 24) & 0xFF) * ((color2 >> 24) & 0xFF) / 0xFF;
    
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static int lampColor(int baseColor)
    {
        final int alpha = baseColor & 0xFF000000;
        return Color.fromRGB(baseColor).lumify().RGB_int | alpha;
    }
}
    
