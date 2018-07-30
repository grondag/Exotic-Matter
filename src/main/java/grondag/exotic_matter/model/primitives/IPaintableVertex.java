package grondag.exotic_matter.model.primitives;

public interface IPaintableVertex
{

    short glow();

    int color();

    float u();

    float v();

    IPaintableVertex withUV(float uThis, float v);

    IPaintableVertex interpolate(IPaintableVertex nextVertex, float dist);

    IPaintableVertex withColorGlow(int colorIn, int glowIn);

    IPaintableVertex withGlow(int glowIn);

    IPaintableVertex withColor(int colorIn);

}
