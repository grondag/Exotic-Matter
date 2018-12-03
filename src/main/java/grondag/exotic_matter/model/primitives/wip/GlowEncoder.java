package grondag.exotic_matter.model.primitives.wip;

import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.*;

public abstract class GlowEncoder
{
    private static final GlowEncoder NO_GLOW = new GlowEncoder()
    {
        @Override
        public int getGlow(IIntStream stream, int glowAddress, int vertexIndex)
        {
            return 0;
        }

        @Override
        public void setGlow(IIntStream stream, int glowAddress, int vertexIndex, int glow)
        {
            throw new UnsupportedOperationException();
        }
    };
            
    private static final GlowEncoder SAME_GLOW = new GlowEncoder()
    {
        @Override
        public int getGlow(IIntStream stream, int glowAddress, int vertexIndex)
        {
            return stream.get(glowAddress);
        }

        @Override
        public void setGlow(IIntStream stream, int glowAddress, int vertexIndex, int glow)
        {
            stream.set(glowAddress, glow);
        }
    };
    
    private static final GlowEncoder VERTEX_GLOW = new GlowEncoder()
    {
        @Override
        public int getGlow(IIntStream stream, int glowAddress, int vertexIndex)
        {
            final int streamIndex = glowAddress + (vertexIndex >> 2);
            final int byteIndex = vertexIndex & 3;
            final int shift = 8 * byteIndex;
            return (stream.get(streamIndex) >> shift ) & 0xFF;
        }

        @Override
        public void setGlow(IIntStream stream, int glowAddress, int vertexIndex, int glow)
        {
            final int streamIndex = glowAddress + (vertexIndex >> 2);
            final int byteIndex = vertexIndex & 3;
            final int shift = 8 * byteIndex;
            final int mask = 0xFF << shift;
            stream.set(streamIndex, (stream.get(streamIndex) & ~mask) | ((glow & 0xFF ) << shift));
        }
    };
    
    /**
     * All mutable formats will have per-vertex glow.
     */
    public static GlowEncoder get(int format)
    {
        final int glowFormat = getVertexGlowFormat(format);
        return glowFormat == VERTEX_GLOW_PER_VERTEX || isMutable(format)
                ? VERTEX_GLOW
                : glowFormat == VERTEX_GLOW_SAME
                    ? SAME_GLOW
                    : NO_GLOW;
    }

    public abstract int getGlow(IIntStream stream, int glowAddress, int vertexIndex);

    public abstract void setGlow(IIntStream stream, int glowAddress, int vertexIndex, int glow);
}
