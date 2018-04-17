package grondag.exotic_matter.model;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import grondag.exotic_matter.ConfigXM;
import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.render.CSGMesh;
import grondag.exotic_matter.render.FaceVertex;
import grondag.exotic_matter.render.IFancyMutablePolygon;
import grondag.exotic_matter.render.IMutablePolygon;
import grondag.exotic_matter.render.IPolygon;
import grondag.exotic_matter.render.Poly;
import grondag.exotic_matter.render.SideShape;
import grondag.exotic_matter.render.Surface;
import grondag.exotic_matter.render.Surface.SurfaceInstance;
import grondag.exotic_matter.render.SurfaceTopology;
import grondag.exotic_matter.render.SurfaceType;
import grondag.exotic_matter.varia.Color;
import grondag.exotic_matter.varia.MicroTimer;
import grondag.exotic_matter.world.HorizontalCorner;
import grondag.exotic_matter.world.HorizontalFace;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
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
    
    public TerrainMeshFactory()
    {
        super(  StateFormat.FLOW, 
                ModelStateData.STATE_FLAG_NEEDS_POS, 
                SURFACE_TOP.surface(), SURFACE_SIDE.surface());
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
    private Vec3d shadowEnhance(Vec3d vec)
    {
      Vec3d temp = vec.normalize();
      return new Vec3d(temp.x, temp.y * temp.y, temp.z);
    }

//    private static LongOpenHashSet hitMap = new LongOpenHashSet();
//    private static AtomicInteger tryCount = new AtomicInteger();
//    private static AtomicInteger hitCount = new AtomicInteger();
    
    @Override
    public @Nonnull List<IPolygon> getShapeQuads(ISuperModelState modelState)
    {
        shapeTimer.start();
        List<IPolygon> result = innerShapeQuads(modelState);
        shapeTimer.stop();
//        if(shapeTimer.stop())
//        {
//            ExoticMatter.INSTANCE.info("Terrain geometry potential cache hit rate = %d percent", (hitCount.get() *  100) / tryCount.get());
//            ExoticMatter.INSTANCE.info("Terrain geometry max cached states = %d", hitMap.size());
//        }
        return result;
    }
    private static MicroTimer shapeTimer = new MicroTimer("terrainGetShapeQuads", 10000);
    private @Nonnull List<IPolygon> innerShapeQuads(ISuperModelState modelState)
    {
        CSGMesh rawQuads = new CSGMesh();
        IMutablePolygon template = Poly.mutable(4);

        template.setColor(Color.WHITE);
        template.setLockUV(true);
        template.setSurfaceInstance(SURFACE_TOP);
        // default - need to change for sides and bottom
        template.setNominalFace(EnumFacing.UP);


        TerrainState flowState = modelState.getTerrainState();
        
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
        IFancyMutablePolygon quadInputsCenterLeft[] = new IFancyMutablePolygon[4];
        /**
         * Quads on right (east) side of the top face.<br>
         * Needed for model and to compute center normal.
         */
        IFancyMutablePolygon quadInputsCenterRight[] = new IFancyMutablePolygon[4];
        
        
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

            IFancyMutablePolygon qiWork = Poly.fancyMutable(template, 3);
            qiWork.setupFaceQuad(
                    fvMidSide[side.ordinal()],
                    fvMidCorner[HorizontalCorner.find(side, side.getLeft()).ordinal()],
                    fvCenter,
                    EnumFacing.NORTH);           
            quadInputsCenterLeft[side.ordinal()] = qiWork;
            quadInputsSide.get(side.ordinal()).add(qiWork);
            quadInputsCorner.get(HorizontalCorner.find(side, side.getLeft()).ordinal()).add(qiWork);

            qiWork = Poly.fancyMutable(template, 3);
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
                qiWork = Poly.fancyMutable(template, 3);
                qiWork.setupFaceQuad(
                        fvFarSide[side.ordinal()],
                        fvMidCorner[HorizontalCorner.find(side, side.getLeft()).ordinal()],
                        fvMidSide[side.ordinal()],
                        EnumFacing.NORTH);           
                quadInputsSide.get(side.ordinal()).add(qiWork);
                quadInputsCorner.get(HorizontalCorner.find(side, side.getLeft()).ordinal()).add(qiWork);
    
                qiWork = Poly.fancyMutable(template, 3);
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
                qiWork = Poly.fancyMutable(template, 3);
                
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
                
                
                qiWork = Poly.fancyMutable(template, 3);
                
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
                    qiWork = Poly.fancyMutable(template, 3);
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
                    qiWork = Poly.fancyMutable(template, 3);
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

        /** Used for Y coord of bottom face and as lower Y coord of side faces*/
        float bottom = -2 - flowState.getYOffset();// - QuadFactory.EPSILON;

        Vec3d normCenter = quadInputsCenterLeft[0].getFaceNormal().scale(quadInputsCenterLeft[0].getArea());
        normCenter = normCenter.add(quadInputsCenterLeft[1].getFaceNormal().scale(quadInputsCenterLeft[1].getArea()));
        normCenter = normCenter.add(quadInputsCenterLeft[2].getFaceNormal().scale(quadInputsCenterLeft[2].getArea()));
        normCenter = normCenter.add(quadInputsCenterLeft[3].getFaceNormal().scale(quadInputsCenterLeft[3].getArea()));
        normCenter = normCenter.add(quadInputsCenterRight[0].getFaceNormal().scale(quadInputsCenterRight[0].getArea()));
        normCenter = normCenter.add(quadInputsCenterRight[1].getFaceNormal().scale(quadInputsCenterRight[1].getArea()));
        normCenter = normCenter.add(quadInputsCenterRight[2].getFaceNormal().scale(quadInputsCenterRight[2].getArea()));
        normCenter = normCenter.add(quadInputsCenterRight[3].getFaceNormal().scale(quadInputsCenterRight[3].getArea()));
        normCenter = shadowEnhance(normCenter).normalize();

        Vec3d normSide[] = new Vec3d[4];
        for(HorizontalFace side : HorizontalFace.values())
        {
            Vec3d normTemp = null;
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

        Vec3d normCorner[] = new Vec3d[4];
        for(HorizontalCorner corner : HorizontalCorner.values())
        {
            Vec3d normTemp = null;
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
        
        //single top face if it is relatively flat and all sides can be drawn without a mid vertex
        if(isTopSimple)
        {
            IFancyMutablePolygon qi = Poly.fancyMutable(template, 4);
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

            rawQuads.add(qi);    
        }

        for(HorizontalFace side: HorizontalFace.values())
        {
            // don't use middle vertex if it is close to being in line with corners
            if(ConfigXM.BLOCKS.simplifyTerrainBlockGeometry && flowState.isSideSimple(side))
            {
                // if side is simple top *may* be not necessarily so - build top if not simple
                if(!isTopSimple)
                {
                    IFancyMutablePolygon qi = Poly.fancyMutable(template, 3);
                    qi.setupFaceQuad(
                            fvMidCorner[HorizontalCorner.find(side, side.getLeft()).ordinal()],
                            fvCenter,
                            fvMidCorner[HorizontalCorner.find(side, side.getRight()).ordinal()],
                            EnumFacing.NORTH);   
                    qi.setVertexNormal(0, normCorner[HorizontalCorner.find(side, side.getLeft()).ordinal()]);
                    qi.setVertexNormal(1, normCenter);
                    qi.setVertexNormal(2, normCorner[HorizontalCorner.find(side, side.getRight()).ordinal()]);                    
                    rawQuads.add(qi);    
                }

                // side
                IMutablePolygon qSide = Poly.mutable(template);
                qSide.setSurfaceInstance(SURFACE_SIDE);
                qSide.setNominalFace(side.face);
                setupUVForSide(qSide, side.face);

                qSide.setupFaceQuad(
                        new FaceVertex(0, bottom, 0),
                        new FaceVertex(1, bottom, 0),
                        new FaceVertex(1, flowState.getMidCornerVertexHeight(HorizontalCorner.find(side, side.getLeft())) - flowState.getYOffset(), 0),
                        new FaceVertex(0, flowState.getMidCornerVertexHeight(HorizontalCorner.find(side, side.getRight())) - flowState.getYOffset(), 0),
                        EnumFacing.UP);
                rawQuads.add(qSide);

            }
            else
            {
                //side is not simple so have to output tops
                IFancyMutablePolygon qi = quadInputsCenterLeft[side.ordinal()];
                qi.setVertexNormal(0, normSide[side.ordinal()]);
                qi.setVertexNormal(1, normCorner[HorizontalCorner.find(side, side.getLeft()).ordinal()]);
                qi.setVertexNormal(2, normCenter);
                rawQuads.add(qi);

                qi = quadInputsCenterRight[side.ordinal()];
                qi.setVertexNormal(0, normCorner[HorizontalCorner.find(side, side.getRight()).ordinal()]);
                qi.setVertexNormal(1, normSide[side.ordinal()]);
                qi.setVertexNormal(2, normCenter);     
                rawQuads.add(qi);

                //Sides
                IMutablePolygon qSide = Poly.mutable(template);
                qSide.setSurfaceInstance(SURFACE_SIDE);
                qSide.setNominalFace(side.face);
                setupUVForSide(qSide, side.face);

                qSide.setupFaceQuad(
                        new FaceVertex(0, bottom, 0),
                        new FaceVertex(0.5f, bottom, 0),
                        new FaceVertex(0.5f, flowState.getMidSideVertexHeight(side) - flowState.getYOffset(), 0),
                        new FaceVertex(0, flowState.getMidCornerVertexHeight(HorizontalCorner.find(side, side.getRight())) - flowState.getYOffset(), 0),
                        EnumFacing.UP);
                rawQuads.add(qSide);

                qSide = Poly.mutable(qSide);
                qSide.setSurfaceInstance(SURFACE_SIDE);
                qSide.setNominalFace(side.face);
                qSide.setupFaceQuad(
                        new FaceVertex(0.5f, bottom, 0),
                        new FaceVertex(1, bottom, 0),
                        new FaceVertex(1, flowState.getMidCornerVertexHeight(HorizontalCorner.find(side, side.getLeft())) - flowState.getYOffset(), 0),
                        new FaceVertex(0.5f, flowState.getMidSideVertexHeight(side) - flowState.getYOffset(), 0),
                        EnumFacing.UP);
                rawQuads.add(qSide);
            }
        }     

        // Bottom face
        IMutablePolygon qBottom = Poly.mutable(template);
        //flip X-orthogonalAxis texture on bottom face
//        qBottom.minU = 14 - qBottom.minU;
//        qBottom.maxU = qBottom.minU + 2;
        qBottom.setSurfaceInstance(SURFACE_SIDE);
        qBottom.setNominalFace(EnumFacing.DOWN);        
        qBottom.setupFaceQuad(0, 0, 1, 1, bottom, EnumFacing.NORTH);
        rawQuads.add(qBottom);

        CSGMesh cubeQuads = new CSGMesh();
        cubeQuads.add(Poly.mutableCopyOf(template).setSurfaceInstance(SURFACE_SIDE).setupFaceQuad(EnumFacing.UP, 0, 0, 1, 1, 0, EnumFacing.NORTH));
        IMutablePolygon faceQuad = Poly.mutableCopyOf(template);
        
        //flip X-orthogonalAxis texture on bottom face
//        faceQuad.minU = 14 - faceQuad.minU;
//        faceQuad.maxU = faceQuad.minU + 2;
        
        cubeQuads.add(Poly.mutableCopyOf(faceQuad).setSurfaceInstance(SURFACE_SIDE).setupFaceQuad(EnumFacing.DOWN, 0, 0, 1, 1, 0, EnumFacing.NORTH));

        
        cubeQuads.add(setupUVForSide(Poly.mutableCopyOf(faceQuad), EnumFacing.NORTH).setSurfaceInstance(SURFACE_SIDE).setupFaceQuad(EnumFacing.NORTH, 0, 0, 1, 1, 0, EnumFacing.UP));
        cubeQuads.add(setupUVForSide(Poly.mutableCopyOf(faceQuad), EnumFacing.SOUTH).setSurfaceInstance(SURFACE_SIDE).setupFaceQuad(EnumFacing.SOUTH, 0, 0, 1, 1, 0, EnumFacing.UP));
        cubeQuads.add(setupUVForSide(Poly.mutableCopyOf(faceQuad), EnumFacing.EAST).setSurfaceInstance(SURFACE_SIDE).setupFaceQuad(EnumFacing.EAST, 0, 0, 1, 1, 0, EnumFacing.UP));
        cubeQuads.add(setupUVForSide(Poly.mutableCopyOf(faceQuad), EnumFacing.WEST).setSurfaceInstance(SURFACE_SIDE).setupFaceQuad(EnumFacing.WEST, 0, 0, 1, 1, 0, EnumFacing.UP));

        rawQuads = rawQuads.intersect(cubeQuads);

        // scale all quads UVs according to position to match what surface painter expects
        // Any quads with a null face are assumed to be part of the top face
        
        // We want top face textures to always join irrespective of Y.
        // Other face can vary based on orthogonal dimension to break up appearance of layers.
        for(IPolygon quad : rawQuads)
        {
            EnumFacing face = quad.getNominalFace();
            if(face == null)
            {
                assert false : "Terrain Mesh Generator is outputting quad with null nominal face.";
                face = EnumFacing.UP;
            }
            
            IMutablePolygon mutableQuad = quad.mutableReference();
            
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
        }
        
        if(ConfigXM.BLOCKS.enableTerrainQuadDebugRender) rawQuads.recolor();
        
        return rawQuads;
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
