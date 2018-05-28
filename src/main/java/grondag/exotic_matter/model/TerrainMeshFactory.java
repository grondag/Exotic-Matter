package grondag.exotic_matter.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.ConfigXM;
import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.cache.LongSimpleCacheLoader;
import grondag.exotic_matter.cache.LongSimpleLoadingCache;
import grondag.exotic_matter.render.CSGMesh;
import grondag.exotic_matter.render.CSGNode;
import grondag.exotic_matter.render.FaceVertex;
import grondag.exotic_matter.render.IMutablePolygon;
import grondag.exotic_matter.render.IPolygon;
import grondag.exotic_matter.render.Poly;
import grondag.exotic_matter.render.SideShape;
import grondag.exotic_matter.render.Surface;
import grondag.exotic_matter.render.Surface.SurfaceInstance;
import grondag.exotic_matter.render.SurfaceTopology;
import grondag.exotic_matter.render.SurfaceType;
import grondag.exotic_matter.render.Vec3f;
import grondag.exotic_matter.varia.Color;
import grondag.exotic_matter.world.HorizontalCorner;
import grondag.exotic_matter.world.HorizontalFace;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class TerrainMeshFactory extends ShapeMeshGenerator implements ICollisionHandler
{
    private static final SurfaceInstance SURFACE_TOP = new Surface(SurfaceType.MAIN, SurfaceTopology.CUBIC).unitInstance.withAllowBorders(false).withIgnoreDepthForRandomization(true);
    private static final SurfaceInstance SURFACE_SIDE = new Surface(SurfaceType.MAIN, SurfaceTopology.CUBIC).unitInstance.withAllowBorders(false);

    private static final AxisAlignedBB[] COLLISION_BOUNDS =
        {
                new AxisAlignedBB(0, 0, 0, 1, 1, 1),
                new AxisAlignedBB(0, 0, 0, 1, 11F/12F, 1),
                new AxisAlignedBB(0, 0, 0, 1, 10F/12F, 1),
                new AxisAlignedBB(0, 0, 0, 1, 9F/12F, 1),

                new AxisAlignedBB(0, 0, 0, 1, 8F/12F, 1),
                new AxisAlignedBB(0, 0, 0, 1, 7F/12F, 1),
                new AxisAlignedBB(0, 0, 0, 1, 6F/12F, 1),
                new AxisAlignedBB(0, 0, 0, 1, 5F/12F, 1),

                new AxisAlignedBB(0, 0, 0, 1, 4F/12F, 1),
                new AxisAlignedBB(0, 0, 0, 1, 3F/12F, 1),
                new AxisAlignedBB(0, 0, 0, 1, 2F/12F, 1),
                new AxisAlignedBB(0, 0, 0, 1, 1F/12F, 1),

                // These aren't actually valid meta values, but prevent NPE if we get one somehow
                new AxisAlignedBB(0, 0, 0, 1, 1, 1),
                new AxisAlignedBB(0, 0, 0, 1, 1, 1),
                new AxisAlignedBB(0, 0, 0, 1, 1, 1),
                new AxisAlignedBB(0, 0, 0, 1, 1, 1)
        };

    private final IPolygon template;
    private final IPolygon[] bottoms = new IPolygon[5];

    private final CSGNode.Root[] terrainNodesSimple = new CSGNode.Root[5];
    private final CSGNode.Root[] terrainNodesHybrid = new CSGNode.Root[5];
    //    private final CSGNode.Root[] terrainNodesComplex = new CSGNode.Root[5];

    private final CSGNode.Root cubeNodeSimple;
    private final CSGNode.Root cubeNodeHybrid;
    //    private final CSGNode.Root cubeNodeComplex;

    private final LongSimpleLoadingCache<Collection<IPolygon>> modelCache = new LongSimpleLoadingCache<>(new TerrainCacheLoader(),  0xFFFF);

//    private final AtomicInteger cacheAttempts = new AtomicInteger(0);
//    private final AtomicInteger cacheMisses = new AtomicInteger(0);
//    
//    public void reportCacheHits()
//    {
//        final int attempts = cacheAttempts.get();
//        final int hits = attempts - cacheMisses.get();
//        System.out.println(String.format("Terrain cache hits: %d / %d (%f percent)", hits, attempts, 100f * hits / attempts ));
//        System.out.println(String.format("Cache capacity = %d, maxfill = %d", modelCache.capacity, modelCache.maxFill));
//        cacheMisses.set(0);
//        cacheAttempts.set(0);
//    }
    
    private class TerrainCacheLoader implements LongSimpleCacheLoader<Collection<IPolygon>>
    {
        @Override
        public Collection<IPolygon> load(long key)
        {
//            cacheMisses.incrementAndGet();
            return createShapeQuads(new TerrainState(key));
        }
    }
    
    private int getIndexForState(TerrainState state)
    {
        return state.getYOffset() + 2;
    }

    /** 
     * Used for Y coord of bottom face and as lower Y coord of side faces
     * Expects values of -2 through +2 from {@link TerrainState#getYOffset()}
     */
    private int getBottomY(int yOffset)
    {
        return -2 - yOffset;
    }

    public TerrainMeshFactory()
    {
        super(  StateFormat.FLOW, 
                ModelStateData.STATE_FLAG_NEEDS_POS, 
                SURFACE_TOP.surface(), SURFACE_SIDE.surface());

        IMutablePolygon templateBuilder = Poly.mutable(4);

        templateBuilder.setColor(Color.WHITE);
        templateBuilder.setLockUV(true);
        templateBuilder.setSurfaceInstance(SURFACE_TOP);
        // default - need to change for sides and bottom
        templateBuilder.setNominalFace(EnumFacing.UP);
//        templateBuilder.setTag("template");
        template = templateBuilder;

        for(int i = 0; i < 5; i++)
        {
            // Bottom faces are pre-built
            IMutablePolygon qBottom = Poly.mutable(template);
            qBottom.setSurfaceInstance(SURFACE_SIDE);
            qBottom.setNominalFace(EnumFacing.DOWN);        
            qBottom.setupFaceQuad(0, 0, 1, 1, getBottomY(i-2), EnumFacing.NORTH);
//            qBottom.setTag("bottom-" + i);
            bottoms[i] = qBottom;
            
//            qBottom = Poly.mutableCopyOf(qBottom).setTag("bottom-simple-" + i);

            terrainNodesSimple[i] = CSGNode.create(ImmutableList.of(qBottom));

//            qBottom = Poly.mutableCopyOf(qBottom).setTag("bottom-hybrid-" + i);
            
            terrainNodesHybrid[i] = CSGNode.create(ImmutableList.of(qBottom), false);

            //            terrainNodesComplex[i] = CSGNode.create(ImmutableList.of(qBottom), true);

        }
        List<IPolygon> cubeQuads = cubeQuads();
        
//        cubeQuads.forEach(q -> q.setTag("cube-simple-" + q.getNominalFace().toString()));
        this.cubeNodeSimple = CSGNode.create(cubeQuads);
        
        cubeQuads = cubeQuads();
//        cubeQuads.forEach(q -> q.setTag("cube-hybrid" + q.getNominalFace().toString()));
        this.cubeNodeHybrid  = CSGNode.create(cubeQuads, false);
        
        //        this.cubeNodeComplex  = CSGNode.create(cubeQuads, true);
    }

    @Override
    public @Nonnull ICollisionHandler collisionHandler()
    {
        return this;
    }

    @Override
    public int geometricSkyOcclusion(ISuperModelState modelState)
    {
        return modelState.getTerrainState().verticalOcclusion();
    }

    /**
     * Flowing terrain tends to appear washed out due to simplistic lighting model.
     * Not hacking the lighter, but can scale down the vertical component of vertex normals
     * to make the shadows a little deeper.
     */
    private Vec3f shadowEnhance(Vec3f vec)
    {
        Vec3f temp = vec.normalize();
        return new Vec3f(temp.x, temp.y * temp.y, temp.z);
    }
   
    @Override
    public @Nonnull Collection<IPolygon> getShapeQuads(ISuperModelState modelState)
    {
        TerrainState flowState = modelState.getTerrainState();
        
        Collection<IPolygon> mesh;
        
        if(flowState.isFullCube())
        {
            mesh = cubeQuads();
        }
        else
        {
//            cacheAttempts.incrementAndGet();
            mesh = this.modelCache.get(flowState.getStateKey());
        }
        
        ImmutableList.Builder<IPolygon> builder = ImmutableList.builder();
        
        // scale all quads UVs according to position to match what surface painter expects
        // Any quads with a null face are assumed to be part of the top face

        // We want top face textures to always join irrespective of Y.
        // Other face can vary based on orthogonal dimension to break up appearance of layers.
        for(IPolygon quad : mesh)
        {
            EnumFacing face = quad.getNominalFace();
            if(face == null)
            {
                assert false : "Terrain Mesh Generator is outputting quad with null nominal face.";
                face = EnumFacing.UP;
            }

            IMutablePolygon mutableQuad = Poly.mutableCopyOf(quad);

            switch(face)
            {
            case NORTH:
            {
                int zHash = MathHelper.hash(modelState.getPosZ());
                mutableQuad.setMinU(255 - ((modelState.getPosX() + (zHash >> 16)) & 0xFF));
                mutableQuad.setMaxU(quad.getMinU() +  1);
                mutableQuad.setMinV(255 - ((modelState.getPosY() + zHash) & 0xFF));
                mutableQuad.setMaxV(quad.getMinV() + 1);
                break;
            }
            case SOUTH:
            {
                int zHash = MathHelper.hash(modelState.getPosZ());
                mutableQuad.setMinU((modelState.getPosX() + (zHash >> 16)) & 0xFF);
                mutableQuad.setMaxU(quad.getMinU() +  1);
                mutableQuad.setMinV(255 - ((modelState.getPosY() + zHash) & 0xFF));
                mutableQuad.setMaxV(quad.getMinV() + 1);
                break;
            }
            case EAST:
            {
                int xHash = MathHelper.hash(modelState.getPosX());
                mutableQuad.setMinU(255 - ((modelState.getPosZ() + (xHash >> 16)) & 0xFF));
                mutableQuad.setMaxU(quad.getMinU() +  1);
                mutableQuad.setMinV(255 - ((modelState.getPosY() + xHash) & 0xFF));
                mutableQuad.setMaxV(quad.getMinV() + 1);
                break;
            }
            case WEST:
            {
                int xHash = MathHelper.hash(modelState.getPosX());
                mutableQuad.setMinU((modelState.getPosZ() + (xHash >> 16)) & 0xFF);
                mutableQuad.setMaxU(quad.getMinU() +  1);
                mutableQuad.setMinV(255 - ((modelState.getPosY() + xHash) & 0xFF));
                mutableQuad.setMaxV(quad.getMinV() + 1);
                break;
            } 
            case DOWN:
            {
                int yHash = MathHelper.hash(modelState.getPosY());
                mutableQuad.setMinU(255 - ((modelState.getPosX() + (yHash >> 16)) & 0xFF));
                mutableQuad.setMaxU(quad.getMinU() +  1);
                mutableQuad.setMinV((modelState.getPosZ() + (yHash >> 16)) & 0xFF);
                mutableQuad.setMaxV(quad.getMinV() + 1);
                break;
            }
            case UP:
            default:
            {
                mutableQuad.setMinU(modelState.getPosX());
                mutableQuad.setMaxU(quad.getMinU() +  1);
                mutableQuad.setMinV(modelState.getPosZ());
                mutableQuad.setMaxV(quad.getMinV() + 1);
                break;
            }
            }
            
            // when above was written, quad UVs were normally in 0-16 range, 
            // now expected to be 0-1, so scale to that standard
            mutableQuad.scaleQuadUV(1f/16f, 1f/16f);
            
            builder.add(mutableQuad);
        }

        Collection<IPolygon> result = builder.build();
        
        if(ConfigXM.BLOCKS.enableTerrainQuadDebugRender) IPolygon.recolor(result);

        return result;
    }
    
    //    private static ISuperModelState[] modelStates = new ISuperModelState[120000];
    //    private static int index = 0;
    private @Nonnull Collection<IPolygon> createShapeQuads(TerrainState flowState)
    {
        //        shapeTimer.start();
        //        Collection<IPolygon> result = innerShapeQuads(modelState);
        //        shapeTimer.stop();
        //        synchronized(modelStates)
        //        {
        //            modelStates[index++] = modelState;
        //            if(index == modelStates.length)
        //            {
        //                try
        //                {
        //                    ByteBuffer bytes = ByteBuffer.allocate(modelStates.length * 4 * Long.BYTES);
        //                    for(ISuperModelState mstate : modelStates)
        //                    {
        //                        assert mstate.getShape() == ModShapes.TERRAIN_FILLER || mstate.getShape() == ModShapes.TERRAIN_HEIGHT;
        //                        assert mstate.getTerrainState() != null;
        //                        bytes.putLong(mstate.getBits0());
        //                        bytes.putLong(mstate.getBits1());
        //                        bytes.putLong(mstate.getBits2());
        //                        bytes.putLong(mstate.getBits3());
        //                    }
        //                    bytes.flip();
        //                    FileOutputStream fos = new FileOutputStream("terrainState.data");
        //                    fos.getChannel().write(bytes);
        //                    fos.close();
        //                    
        //                    
        //                    FileInputStream fis = new FileInputStream("terrainState.data");
        //                    ByteBuffer testBytes = ByteBuffer.allocate(modelStates.length * 4 * Long.BYTES);
        //                    fis.getChannel().read(testBytes);
        //                    fis.close();
        //                    testBytes.flip();
        //                    for(int i = 0; i < modelStates.length; i++)
        //                    {
        //                        ModelState testModelState = new ModelState(testBytes.getLong(), testBytes.getLong(), testBytes.getLong(), testBytes.getLong());
        //                        ISuperModelState originalModelState = modelStates[i];
        //                        assert testModelState.equalsIncludeStatic(originalModelState);
        //                        assert testModelState.getShape() == ModShapes.TERRAIN_FILLER || testModelState.getShape() == ModShapes.TERRAIN_HEIGHT;
        //                        assert testModelState.getTerrainState() != null;
        //                    }
        //                }
        //                catch (Exception e)
        //                {
        //                    e.printStackTrace();
        //                }
        //                index = 0;
        //            }
        //        }
        //        if(shapeTimer.stop())
        //        {

        //        }
        //        return result;
        //    }
        //    private static MicroTimer shapeTimer = new MicroTimer("terrainGetShapeQuads", 400000);
        //   
        //   
        //    private @Nonnull Collection<IPolygon> innerShapeQuads(ISuperModelState modelState)
        //    {

        //        synchronized(stateList)
        //        {
        //            stateList.add(modelState.getShape() ==ModShapes.TERRAIN_FILLER ?  -flowState.getStateKey() : flowState.getStateKey());
        //        }


        CSGNode.Root terrainNode;
        CSGNode.Root cubeNode;

        if(flowState.isTopSimple())
        {
            terrainNode = terrainNodesSimple[getIndexForState(flowState)].clone();
            cubeNode = this.cubeNodeSimple.clone();
        }
        else //if(flowState.areMostSidesSimple())
        {
            terrainNode = terrainNodesHybrid[getIndexForState(flowState)].clone();
            cubeNode = this.cubeNodeHybrid.clone();
        }
        //            else
        //            {
        //                terrainNode = terrainNodesComplex[getIndexForState(flowState)].clone();
        //                cubeNode = this.cubeNodeComplex.clone();
        //            }
        
        addTerrainQuads(flowState, terrainNode);
        
       return CSGMesh.intersect(
                cubeNode,
                terrainNode
                );
    }

//    private static LongOpenHashSet hitMap = new LongOpenHashSet();
//    private static AtomicInteger tryCount = new AtomicInteger();
//    private static AtomicInteger hitCount = new AtomicInteger();
//    
//    public static void reportAndClearHitCount()
//    {
//        synchronized(hitMap)
//        {
//            ExoticMatter.INSTANCE.info("Terrain geometry potential cache hit rate = %d percent", (hitCount.get() *  100) / tryCount.get());
//            ExoticMatter.INSTANCE.info("Terrain geometry max cached states = %d", hitMap.size());
//            tryCount.set(0);
//            hitCount.set(0);
//            hitMap.clear();
//        }
//    }
    
    private void addTerrainQuads(TerrainState flowState, CSGNode.Root terrainQuads)
    {
//        tryCount.incrementAndGet();
//        synchronized(hitMap)
//        {
//            if(!hitMap.add(flowState.getStateKey()))
//            {
//                hitCount.incrementAndGet();
//            }
//        }
        
        // center vertex setup
        FaceVertex fvCenter = new FaceVertex(0.5f, 0.5f, 1.0f - flowState.getCenterVertexHeight() + flowState.getYOffset());

        /**
         * Quads on left (west) side of the top face.<br>
         * Needed for model and to computer center normal.
         */
        IMutablePolygon quadInputsCenterLeft[] = new IMutablePolygon[4];
        /**
         * Quads on right (east) side of the top face.<br>
         * Needed for model and to compute center normal.
         */
        IMutablePolygon quadInputsCenterRight[] = new IMutablePolygon[4];


        /**
         * Quads adjacent to each side midpoint vertex.  Needed to compute normals.
         * Will always contains quads for this block but only contains quads
         * for adjacent space if it has a terrain height.
         */
        ArrayList<ArrayList<IMutablePolygon>> quadInputsSide = new ArrayList<>(4);

        /**
         * Quads adjacent to each corner vertex.  Needed to compute normals.
         * Will always contains quads for this block but only contains quads
         * for adjacent spaces if the space has a terrain height.
         */
        ArrayList<ArrayList<IMutablePolygon>> quadInputsCorner = new ArrayList<>(4);


        ///////////////////////////////////////////////
        // set up corner heights and face vertices
        ///////////////////////////////////////////////


        // Coordinates assume quad will be set up with North=top orientation
        // Depth will be set separately.

        /**
         * Top face vertex positions for corners of this block.  Could be above
         * or below the box bounding box.  CSG operations will trim shape to block box later.
         * Initialized to a height of one and changed based on world state.
         */
        FaceVertex fvMidCorner[] = new FaceVertex[HorizontalFace.values().length];
        fvMidCorner[HorizontalCorner.NORTH_EAST.ordinal()] = new FaceVertex(1, 1, 1);
        fvMidCorner[HorizontalCorner.NORTH_WEST.ordinal()] = new FaceVertex(0, 1, 1);
        fvMidCorner[HorizontalCorner.SOUTH_EAST.ordinal()] = new FaceVertex(1, 0, 1);
        fvMidCorner[HorizontalCorner.SOUTH_WEST.ordinal()] = new FaceVertex(0, 0, 1);

        /**
         * Top face vertex positions for centers of the block at the four corners.  
         * Initialized to a height of one and changed based on world state.
         * Used to generate tris needed to compute vertex normals
         */
        FaceVertex fvFarCorner[] = new FaceVertex[HorizontalFace.values().length];
        fvFarCorner[HorizontalCorner.NORTH_EAST.ordinal()] = new FaceVertex(1.5f, 1.5f, 1);
        fvFarCorner[HorizontalCorner.NORTH_WEST.ordinal()] = new FaceVertex(-0.5f, 1.5f, 1);
        fvFarCorner[HorizontalCorner.SOUTH_EAST.ordinal()] = new FaceVertex(1.5f, -0.5f, 1);
        fvFarCorner[HorizontalCorner.SOUTH_WEST.ordinal()] = new FaceVertex(-0.5f, -0.5f, 1);



        for(HorizontalCorner corner : HorizontalCorner.values())
        {

            fvMidCorner[corner.ordinal()] = fvMidCorner[corner.ordinal()].withDepth(1 - flowState.getMidCornerVertexHeight(corner) + flowState.getYOffset());
            fvFarCorner[corner.ordinal()] = fvFarCorner[corner.ordinal()].withDepth(1 - flowState.getFarCornerVertexHeight(corner) + flowState.getYOffset());

            quadInputsCorner.add(new ArrayList<IMutablePolygon>(8));            
        }

        // Coordinates assume quad will be set up with North=top orientation
        // Depth will be set separately.
        FaceVertex fvMidSide[] = new FaceVertex[HorizontalFace.values().length];
        fvMidSide[HorizontalFace.NORTH.ordinal()] = new FaceVertex(0.5f, 1f, 1.0f);
        fvMidSide[HorizontalFace.SOUTH.ordinal()] = new FaceVertex(0.5f, 0f, 1.0f);
        fvMidSide[HorizontalFace.EAST.ordinal()] = new FaceVertex(1.0f, 0.5f, 1.0f);
        fvMidSide[HorizontalFace.WEST.ordinal()] = new FaceVertex(0f, 0.5f, 1.0f);

        FaceVertex fvFarSide[] = new FaceVertex[HorizontalFace.values().length];
        fvFarSide[HorizontalFace.NORTH.ordinal()] = new FaceVertex(0.5f, 1.5f, 1.0f);
        fvFarSide[HorizontalFace.SOUTH.ordinal()] = new FaceVertex(0.5f, -0.5f, 1.0f);
        fvFarSide[HorizontalFace.EAST.ordinal()] = new FaceVertex(1.5f, 0.5f, 1.0f);
        fvFarSide[HorizontalFace.WEST.ordinal()] = new FaceVertex(-0.5f, 0.5f, 1.0f);

        for(HorizontalFace side : HorizontalFace.values())
        {
            fvMidSide[side.ordinal()] = fvMidSide[side.ordinal()].withDepth(1 - flowState.getMidSideVertexHeight(side) + flowState.getYOffset());
            fvFarSide[side.ordinal()] = fvFarSide[side.ordinal()].withDepth(1 - flowState.getFarSideVertexHeight(side) + flowState.getYOffset());

            quadInputsSide.add(new ArrayList<IMutablePolygon>(8));   


            // build quads on the top of this block that that border this side (left and right)
            // these are always included in the vertex normal calculations for the side midpoint and corner vertices

            IMutablePolygon qiWork = Poly.mutable(template, 3);
            qiWork.setupFaceQuad(
                    fvMidSide[side.ordinal()],
                    fvMidCorner[HorizontalCorner.find(side, side.getLeft()).ordinal()],
                    fvCenter,
                    EnumFacing.NORTH);           
            quadInputsCenterLeft[side.ordinal()] = qiWork;
            quadInputsSide.get(side.ordinal()).add(qiWork);
            quadInputsCorner.get(HorizontalCorner.find(side, side.getLeft()).ordinal()).add(qiWork);

            qiWork = Poly.mutable(template, 3);
            qiWork.setupFaceQuad(
                    fvMidCorner[HorizontalCorner.find(side, side.getRight()).ordinal()],
                    fvMidSide[side.ordinal()],
                    fvCenter,
                    EnumFacing.NORTH);
            quadInputsCenterRight[side.ordinal()] = qiWork;
            quadInputsSide.get(side.ordinal()).add(qiWork);
            quadInputsCorner.get(HorizontalCorner.find(side, side.getRight()).ordinal()).add(qiWork);

            final boolean isSidePresent = flowState.getSideHeight(side) != TerrainState.NO_BLOCK;

            // add side block tri that borders this block if it is there
            if(isSidePresent)
            {
                qiWork = Poly.mutable(template, 3);
                qiWork.setupFaceQuad(
                        fvFarSide[side.ordinal()],
                        fvMidCorner[HorizontalCorner.find(side, side.getLeft()).ordinal()],
                        fvMidSide[side.ordinal()],
                        EnumFacing.NORTH);           
                quadInputsSide.get(side.ordinal()).add(qiWork);
                quadInputsCorner.get(HorizontalCorner.find(side, side.getLeft()).ordinal()).add(qiWork);

                qiWork = Poly.mutable(template, 3);
                qiWork.setupFaceQuad(
                        fvMidCorner[HorizontalCorner.find(side, side.getRight()).ordinal()],
                        fvFarSide[side.ordinal()],
                        fvMidSide[side.ordinal()],
                        EnumFacing.NORTH);           
                quadInputsSide.get(side.ordinal()).add(qiWork);
                quadInputsCorner.get(HorizontalCorner.find(side, side.getRight()).ordinal()).add(qiWork);
            }


            // add side block tris that connect to corner but do not border side
            // if both the side and corner block are present, this will be a tri that
            // spans both the side and corner block (and will affect normals proportional to area)

            // if only the side is present, will be the half on the side block and if
            // the side block is missing but the corner block is present will be the part
            // that is on the corner block.

            // in the cases where either the side or corner block is missing, terrain state
            // will compute the height of midpoint between them to be the 1/2 block less than
            // the height of center of the block that is present (see TerrainState.calcMidSideVertexHeight)

            final HorizontalCorner leftCorner = HorizontalCorner.find(side, side.getLeft());
            final boolean isLeftCornerPresent = flowState.getCornerHeight(leftCorner) != TerrainState.NO_BLOCK;

            final HorizontalCorner rightCorner = HorizontalCorner.find(side, side.getRight());
            final boolean isRightCornerPresent = flowState.getCornerHeight(rightCorner) != TerrainState.NO_BLOCK;

            if(isSidePresent)
            {
                qiWork = Poly.mutable(template, 3);
                
                final FaceVertex leftFarCorner = isLeftCornerPresent

                        // have both the corner and side so do one big tri for both
                        ? fvFarCorner[leftCorner.ordinal()]

                                // only have tri on side block, vertex for side of missing corner will
                                // be half a block lower than the side's center height
                                : midPoint(fvFarSide[side.ordinal()], fvFarCorner[leftCorner.ordinal()])
                                .withDepth(fvFarSide[side.ordinal()].depth + 0.5f);

                        qiWork.setupFaceQuad(
                                fvMidCorner[leftCorner.ordinal()],
                                fvFarSide[side.ordinal()],
                                leftFarCorner,
                                EnumFacing.NORTH);           
                        quadInputsCorner.get(leftCorner.ordinal()).add(qiWork);


                qiWork = Poly.mutable(template, 3);

                final FaceVertex rightFarCorner = isRightCornerPresent

                        // have both the corner and side so do one big tri for both
                        ? fvFarCorner[rightCorner.ordinal()]

                                // only have tri on side block, vertex for side of missing corner will
                                // be half a block lower than the side's center height
                                : midPoint(fvFarSide[side.ordinal()], fvFarCorner[rightCorner.ordinal()])
                                .withDepth(fvFarSide[side.ordinal()].depth + 0.5f);

                        qiWork.setupFaceQuad(
                                fvMidCorner[rightCorner.ordinal()],
                                rightFarCorner,
                                fvFarSide[side.ordinal()],
                                EnumFacing.NORTH);           
                        quadInputsCorner.get(rightCorner.ordinal()).add(qiWork);
    }
            else
            {
                if(isLeftCornerPresent)
                {
                    // only have the corner
                    qiWork = Poly.mutable(template, 3);
                    qiWork.setupFaceQuad(
                            fvMidCorner[leftCorner.ordinal()],
                            midPoint(fvFarSide[side.ordinal()], fvFarCorner[leftCorner.ordinal()])
                            .withDepth(fvFarCorner[leftCorner.ordinal()].depth + 0.5f),
                            fvFarCorner[leftCorner.ordinal()],
                            EnumFacing.NORTH);           
                    quadInputsCorner.get(leftCorner.ordinal()).add(qiWork);
                }

                if(isRightCornerPresent)
                {
                    // only have the corner
                    qiWork = Poly.mutable(template, 3);
                    qiWork.setupFaceQuad(
                            fvMidCorner[rightCorner.ordinal()],
                            fvFarCorner[rightCorner.ordinal()],
                            midPoint(fvFarSide[side.ordinal()], fvFarCorner[rightCorner.ordinal()])
                            .withDepth(fvFarCorner[rightCorner.ordinal()].depth + 0.5f),
                            EnumFacing.NORTH);           
                    quadInputsCorner.get(rightCorner.ordinal()).add(qiWork);
                }
            }
        }

        int bottom = getBottomY(flowState.getYOffset());

        Vec3f normCenter = quadInputsCenterLeft[0].getFaceNormal().scale(quadInputsCenterLeft[0].getArea());
        normCenter = normCenter.add(quadInputsCenterLeft[1].getFaceNormal().scale(quadInputsCenterLeft[1].getArea()));
        normCenter = normCenter.add(quadInputsCenterLeft[2].getFaceNormal().scale(quadInputsCenterLeft[2].getArea()));
        normCenter = normCenter.add(quadInputsCenterLeft[3].getFaceNormal().scale(quadInputsCenterLeft[3].getArea()));
        normCenter = normCenter.add(quadInputsCenterRight[0].getFaceNormal().scale(quadInputsCenterRight[0].getArea()));
        normCenter = normCenter.add(quadInputsCenterRight[1].getFaceNormal().scale(quadInputsCenterRight[1].getArea()));
        normCenter = normCenter.add(quadInputsCenterRight[2].getFaceNormal().scale(quadInputsCenterRight[2].getArea()));
        normCenter = normCenter.add(quadInputsCenterRight[3].getFaceNormal().scale(quadInputsCenterRight[3].getArea()));
        normCenter = shadowEnhance(normCenter).normalize();

        Vec3f normSide[] = new Vec3f[4];
        for(HorizontalFace side : HorizontalFace.values())
        {
            Vec3f normTemp = null;
            for(IMutablePolygon qi : quadInputsSide.get(side.ordinal()))
            {
                if(normTemp == null) 
                {
                    normTemp = qi.getFaceNormal().scale(qi.getArea());
                }
                else
                {
                    normTemp = normTemp.add(qi.getFaceNormal().scale(qi.getArea()));
                }
            }
            normSide[side.ordinal()] = shadowEnhance(normTemp).normalize();
        }

        Vec3f normCorner[] = new Vec3f[4];
        for(HorizontalCorner corner : HorizontalCorner.values())
        {
            Vec3f normTemp = null;
            for(IMutablePolygon qi : quadInputsCorner.get(corner.ordinal()))
            {
                if(normTemp == null) 
                {
                    normTemp = qi.getFaceNormal().scale(qi.getArea());
                }
                else
                {
                    normTemp = normTemp.add(qi.getFaceNormal().scale(qi.getArea()));
                }
            }
            normCorner[corner.ordinal()] = shadowEnhance(normTemp).normalize();
        }

        final boolean isTopSimple = ConfigXM.BLOCKS.simplifyTerrainBlockGeometry && flowState.isTopSimple();

        // note that outputting sides first seems to work best for CSG intersect performance
        // with convex polyhedra tend to get an unbalanced BSP tree - not much can do about it without creating unwanted splits
        for(HorizontalFace side: HorizontalFace.values())
        {
            // don't use middle vertex if it is close to being in line with corners
            if(ConfigXM.BLOCKS.simplifyTerrainBlockGeometry && flowState.isSideSimple(side))
            {

                // side
                IMutablePolygon qSide = Poly.mutable(template);
//                qSide.setTag("side-simple-" + side.toString());
                
                qSide.setSurfaceInstance(SURFACE_SIDE);
                qSide.setNominalFace(side.face);
                setupUVForSide(qSide, side.face);

                qSide.setupFaceQuad(
                        new FaceVertex(0, bottom, 0),
                        new FaceVertex(1, bottom, 0),
                        new FaceVertex(1, flowState.getMidCornerVertexHeight(HorizontalCorner.find(side, side.getLeft())) - flowState.getYOffset(), 0),
                        new FaceVertex(0, flowState.getMidCornerVertexHeight(HorizontalCorner.find(side, side.getRight())) - flowState.getYOffset(), 0),
                        EnumFacing.UP);
                terrainQuads.addPolygon(qSide);
                
                // if side is simple top *may* be not necessarily so - build top if not simple
                if(!isTopSimple)
                {
                    IMutablePolygon qi = Poly.mutable(template, 3);
                    
//                    qi.setTag("top-simpleside-" + side.toString());
                    
                    qi.setupFaceQuad(
                            fvMidCorner[HorizontalCorner.find(side, side.getLeft()).ordinal()],
                            fvCenter,
                            fvMidCorner[HorizontalCorner.find(side, side.getRight()).ordinal()],
                            EnumFacing.NORTH);   
                    qi.setVertexNormal(0, normCorner[HorizontalCorner.find(side, side.getLeft()).ordinal()]);
                    qi.setVertexNormal(1, normCenter);
                    qi.setVertexNormal(2, normCorner[HorizontalCorner.find(side, side.getRight()).ordinal()]);                    
                    terrainQuads.addPolygon(qi);    
                }

            }
            else
            {

                //Sides
                IMutablePolygon qSide = Poly.mutable(template);
//                qSide.setTag("side-complex-1-" + side.toString());
                qSide.setSurfaceInstance(SURFACE_SIDE);
                qSide.setNominalFace(side.face);
                setupUVForSide(qSide, side.face);

                qSide.setupFaceQuad(
                        new FaceVertex(0, bottom, 0),
                        new FaceVertex(0.5f, bottom, 0),
                        new FaceVertex(0.5f, flowState.getMidSideVertexHeight(side) - flowState.getYOffset(), 0),
                        new FaceVertex(0, flowState.getMidCornerVertexHeight(HorizontalCorner.find(side, side.getRight())) - flowState.getYOffset(), 0),
                        EnumFacing.UP);
                terrainQuads.addPolygon(qSide);

                qSide = Poly.mutable(qSide);
//                qSide.setTag("side-complex-2-" + side.toString());
                qSide.setSurfaceInstance(SURFACE_SIDE);
                qSide.setNominalFace(side.face);
                qSide.setupFaceQuad(
                        new FaceVertex(0.5f, bottom, 0),
                        new FaceVertex(1, bottom, 0),
                        new FaceVertex(1, flowState.getMidCornerVertexHeight(HorizontalCorner.find(side, side.getLeft())) - flowState.getYOffset(), 0),
                        new FaceVertex(0.5f, flowState.getMidSideVertexHeight(side) - flowState.getYOffset(), 0),
                        EnumFacing.UP);
                terrainQuads.addPolygon(qSide);
                
                //side is not simple so have to output tops
                IMutablePolygon qi = quadInputsCenterLeft[side.ordinal()];
//                qi.setTag("side-complex-1-top-" + side.toString());
                
                qi.setVertexNormal(0, normSide[side.ordinal()]);
                qi.setVertexNormal(1, normCorner[HorizontalCorner.find(side, side.getLeft()).ordinal()]);
                qi.setVertexNormal(2, normCenter);
                terrainQuads.addPolygon(qi);

                qi = quadInputsCenterRight[side.ordinal()];
//                qi.setTag("side-complex-2-top-" + side.toString());
                qi.setVertexNormal(0, normCorner[HorizontalCorner.find(side, side.getRight()).ordinal()]);
                qi.setVertexNormal(1, normSide[side.ordinal()]);
                qi.setVertexNormal(2, normCenter);     
                terrainQuads.addPolygon(qi);
            }
        }   
        
        //simple top face if it is relatively flat and all sides can be drawn without a mid vertex
        if(isTopSimple)
        {
            IMutablePolygon qi = Poly.mutable(template, 4);
            
//            qi.setTag("top-simple");
            
            qi.setupFaceQuad(
                    fvMidCorner[HorizontalCorner.SOUTH_WEST.ordinal()],
                    fvMidCorner[HorizontalCorner.SOUTH_EAST.ordinal()],
                    fvMidCorner[HorizontalCorner.NORTH_EAST.ordinal()],
                    fvMidCorner[HorizontalCorner.NORTH_WEST.ordinal()],
                    EnumFacing.NORTH);   
            qi.setVertexNormal(0, normCorner[HorizontalCorner.SOUTH_WEST.ordinal()]);
            qi.setVertexNormal(1, normCorner[HorizontalCorner.SOUTH_EAST.ordinal()]);
            qi.setVertexNormal(2, normCorner[HorizontalCorner.NORTH_EAST.ordinal()]);
            qi.setVertexNormal(3, normCorner[HorizontalCorner.NORTH_WEST.ordinal()]);

            //break into tris unless it is truly coplanar
            if(qi.isOnSinglePlane())
            {
                terrainQuads.addPolygon(qi);   
            }
            else
            {
                qi.addTrisToCSGRoot(terrainQuads);
            }
        }
        

        // Bottom face is pre-added to CSGNode templates
        //        IMutablePolygon qBottom = Poly.mutable(template);
        //        qBottom.setSurfaceInstance(SURFACE_SIDE);
        //        qBottom.setNominalFace(EnumFacing.DOWN);        
        //        qBottom.setupFaceQuad(0, 0, 1, 1, bottom, EnumFacing.NORTH);
        //        terrainQuads.add(qBottom);
        //        terrainQuads.add(getBottomForState(flowState));
    }

    private List<IPolygon> cubeQuads()
    {
        ArrayList<IPolygon> cubeQuads = new ArrayList<>();
        
        //note the order here is significant - testing shows this order gives fewest splits in CSG intersect
        //most important thing seems to be that sides come first
        cubeQuads.add(setupUVForSide(Poly.mutableCopyOf(template), EnumFacing.NORTH).setSurfaceInstance(SURFACE_SIDE).setupFaceQuad(EnumFacing.NORTH, 0, 0, 1, 1, 0, EnumFacing.UP));
        cubeQuads.add(setupUVForSide(Poly.mutableCopyOf(template), EnumFacing.EAST).setSurfaceInstance(SURFACE_SIDE).setupFaceQuad(EnumFacing.EAST, 0, 0, 1, 1, 0, EnumFacing.UP));
        cubeQuads.add(setupUVForSide(Poly.mutableCopyOf(template), EnumFacing.SOUTH).setSurfaceInstance(SURFACE_SIDE).setupFaceQuad(EnumFacing.SOUTH, 0, 0, 1, 1, 0, EnumFacing.UP));
        cubeQuads.add(setupUVForSide(Poly.mutableCopyOf(template), EnumFacing.WEST).setSurfaceInstance(SURFACE_SIDE).setupFaceQuad(EnumFacing.WEST, 0, 0, 1, 1, 0, EnumFacing.UP));
        cubeQuads.add(Poly.mutableCopyOf(template).setSurfaceInstance(SURFACE_SIDE).setupFaceQuad(EnumFacing.UP, 0, 0, 1, 1, 0, EnumFacing.NORTH));
        cubeQuads.add(Poly.mutableCopyOf(template).setSurfaceInstance(SURFACE_SIDE).setupFaceQuad(EnumFacing.DOWN, 0, 0, 1, 1, 0, EnumFacing.NORTH));
        return cubeQuads;
    }

    /**
     * Returns a face vertex at the average of the coordinates of the inputs.
     * Does not use any other properties of the inputs.
     */
    private FaceVertex midPoint(FaceVertex first, FaceVertex second)
    {
        return new FaceVertex((first.x + second.x) / 2, (first.y + second.y) / 2, (first.depth + second.depth) / 2);
    }

    private IMutablePolygon setupUVForSide(IMutablePolygon quad, EnumFacing face)
    {

        //        quad.minU = (face.getAxis() == Axis.X ? flowTex.getZ() : flowTex.getX()) * 2;
        // need to flip U on these side faces so that textures align properly
        if(face == EnumFacing.EAST || face == EnumFacing.NORTH) 
        {
            quad.setMinU(16);
            quad.setMaxU(0);
        }
        else
        {
            quad.setMinU(0);
            quad.setMaxU(16);
        }
        return quad;
        //         quad.maxU = quad.minU + 2;
        //        quad.minV = 14 - flowTex.getY() * 2;
        //        quad.maxV = quad.minV + 2;
    }

    @Override
    public boolean isCube(ISuperModelState modelState)
    {
        return false; 
        // doing it this way caused lighting problems: massive dark areas
        //        return modelState.getTerrainState().isFullCube();
    }

    @Override
    public boolean rotateBlock(IBlockState blockState, World world, BlockPos pos, EnumFacing axis, ISuperBlock block, ISuperModelState modelState)
    {
        return false;
    }

    @Override
    public SideShape sideShape(ISuperModelState modelState, EnumFacing side)
    {
        return modelState.getTerrainState().isFullCube() ? SideShape.SOLID : SideShape.MISSING;
    }

    @Override
    public List<AxisAlignedBB> getCollisionBoxes(ISuperModelState modelState)
    {
        return CollisionBoxDispatcher.INSTANCE.getCollisionBoxes(modelState);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(ISuperModelState modelState)
    {
        try
        {
            return COLLISION_BOUNDS[modelState.getTerrainState().getCenterHeight() - 1];
        }
        catch (Exception ex)
        {
            ExoticMatter.INSTANCE.error("TerrainMeshFactory received Collision Bounding Box check for a foreign block.", ex);
            return Block.FULL_BLOCK_AABB;
        }
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox(ISuperModelState modelState)
    {
        return getCollisionBoundingBox(modelState);
    }

    @Override
    public int getMetaData(ISuperModelState modelState)
    {
        return modelState.getTerrainState().getCenterHeight();
    }
}
