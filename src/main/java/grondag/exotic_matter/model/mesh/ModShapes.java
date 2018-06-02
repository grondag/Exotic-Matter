package grondag.exotic_matter.model.mesh;

import static grondag.exotic_matter.model.state.MetaUsage.SHAPE;
import static grondag.exotic_matter.model.state.MetaUsage.SPECIES;

import grondag.exotic_matter.terrain.TerrainMeshFactory;

public class ModShapes
{

    public static final ModelShape<?> CUBE = ModelShape.create("cube", CubeMeshFactory.class, SPECIES);
    public static final ModelShape<?> COLUMN_SQUARE = ModelShape.create("column_square", SquareColumnMeshFactory.class, SPECIES);
    public static final ModelShape<?> STACKED_PLATES = ModelShape.create("stacked_plates", StackedPlatesMeshFactory.class, SHAPE);
    public static final ModelShape<?> TERRAIN_HEIGHT = ModelShape.create("terrain_height", TerrainMeshFactory.class, SHAPE, true);
    public static final ModelShape<?> TERRAIN_FILLER = ModelShape.create("terrain_filler", TerrainMeshFactory.class, SHAPE, false);
    public static final ModelShape<?> WEDGE = ModelShape.create("wedge", WedgeMeshFactory.class, SPECIES, true);
    public static final ModelShape<?> STAIR = ModelShape.create("stair", StairMeshFactory.class, SPECIES, true);
    public static final ModelShape<?> SPHERE = ModelShape.create("sphere", SphereMeshFactory.class, SPECIES, true);
    public static final ModelShape<?> CSGTEST = ModelShape.create("csgtest", CSGTestMeshFactory.class, SPECIES, true);
}
