package grondag.exotic_matter.model.primitives.better;

import net.minecraft.util.EnumFacing;

public interface IMutablePoly<T extends IMutableGeometricVertex> extends IPoly<T>
{
    public void setNominalFace(EnumFacing face);
}
