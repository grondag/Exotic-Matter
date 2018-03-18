package grondag.exotic_matter.model;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import grondag.exotic_matter.Log;
import grondag.exotic_matter.render.CSGShape;
import grondag.exotic_matter.render.FaceVertex;
import grondag.exotic_matter.render.RawQuad;
import grondag.exotic_matter.render.SideShape;
import grondag.exotic_matter.render.Surface;
import grondag.exotic_matter.render.SurfaceTopology;
import grondag.exotic_matter.render.SurfaceType;
import grondag.exotic_matter.render.Surface.SurfaceInstance;
import grondag.exotic_matter.varia.Color;
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
     * Not hacking the lighter, but can scale horizontal component of vertex normals
     * to make the shadows a little deeper.
     */
    private Vec3d shadowEnhance(Vec3d vec)
    {
        return new Vec3d(vec.x * 4, vec.y, vec.z * 2);
    }

    @Override
    public @Nonnull List<RawQuad> getShapeQuads(ISuperModelState modelState)
    {
        CSGShape rawQuads = new CSGShape();
        RawQuad template = new RawQuad();

        template.color = Color.WHITE;
        template.lockUV = true;
        template.surfaceInstance = SURFACE_TOP;
        // default - need to change for sides and bottom
        template.setFace(EnumFacing.UP);


        TerrainState flowState = modelState.getTerrainState();

        // center vertex setup
        FaceVertex fvCenter = new FaceVertex(0.5, 0.5, 1.0 - flowState.getCenterVertexHeight() + flowState.getYOffset());

        RawQuad quadInputsCenterLeft[] = new RawQuad[4];
        RawQuad quadInputsCenterRight[] = new RawQuad[4];
        ArrayList<ArrayList<RawQuad>> quadInputsSide = new ArrayList<ArrayList<RawQuad>>(4);
        ArrayList<ArrayList<RawQuad>> quadInputsCorner = new ArrayList<ArrayList<RawQuad>>(4);


        ///////////////////////////////////////////////
        // set up corner heights and face vertices
        ///////////////////////////////////////////////


        // Coordinates assume quad will be set up with North=top orientation
        // Depth will be set separately.
        FaceVertex fvMidCorner[] = new FaceVertex[HorizontalFace.values().length];
        FaceVertex fvFarCorner[] = new FaceVertex[HorizontalFace.values().length];

        fvMidCorner[HorizontalCorner.NORTH_EAST.ordinal()] = new FaceVertex(1, 1, 1.0);
        fvMidCorner[HorizontalCorner.NORTH_WEST.ordinal()] = new FaceVertex(0, 1, 1.0);
        fvMidCorner[HorizontalCorner.SOUTH_EAST.ordinal()] = new FaceVertex(1, 0, 1.0);
        fvMidCorner[HorizontalCorner.SOUTH_WEST.ordinal()] = new FaceVertex(0, 0, 1.0);

        fvFarCorner[HorizontalCorner.NORTH_EAST.ordinal()] = new FaceVertex(1.5, 1.5, 1.0);
        fvFarCorner[HorizontalCorner.NORTH_WEST.ordinal()] = new FaceVertex(-0.5, 1.5, 1.0);
        fvFarCorner[HorizontalCorner.SOUTH_EAST.ordinal()] = new FaceVertex(1.5, -0.5, 1.0);
        fvFarCorner[HorizontalCorner.SOUTH_WEST.ordinal()] = new FaceVertex(-0.5, -0.5, 1.0);

        for(HorizontalCorner corner : HorizontalCorner.values())
        {

            fvMidCorner[corner.ordinal()] = fvMidCorner[corner.ordinal()].withDepth(1.0 - flowState.getMidCornerVertexHeight(corner) + flowState.getYOffset());
            fvFarCorner[corner.ordinal()] = fvFarCorner[corner.ordinal()].withDepth(1.0 - flowState.getFarCornerVertexHeight(corner) + flowState.getYOffset());

            quadInputsCorner.add(new ArrayList<RawQuad>(8));            
        }

        // Coordinates assume quad will be set up with North=top orientation
        // Depth will be set separately.
        FaceVertex fvMidSide[] = new FaceVertex[HorizontalFace.values().length];
        fvMidSide[HorizontalFace.NORTH.ordinal()] = new FaceVertex(0.5, 1, 1.0);
        fvMidSide[HorizontalFace.SOUTH.ordinal()] = new FaceVertex(0.5, 0, 1.0);
        fvMidSide[HorizontalFace.EAST.ordinal()] = new FaceVertex(1.0, 0.5, 1.0);
        fvMidSide[HorizontalFace.WEST.ordinal()] = new FaceVertex(0, 0.5, 1.0);

        FaceVertex fvFarSide[] = new FaceVertex[HorizontalFace.values().length];
        fvFarSide[HorizontalFace.NORTH.ordinal()] = new FaceVertex(0.5, 1.5, 1.0);
        fvFarSide[HorizontalFace.SOUTH.ordinal()] = new FaceVertex(0.5, -0.5, 1.0);
        fvFarSide[HorizontalFace.EAST.ordinal()] = new FaceVertex(1.5, 0.5, 1.0);
        fvFarSide[HorizontalFace.WEST.ordinal()] = new FaceVertex(-0.5, 0.5, 1.0);

        for(HorizontalFace side : HorizontalFace.values())
        {
            fvMidSide[side.ordinal()] = fvMidSide[side.ordinal()].withDepth(1.0 - flowState.getMidSideVertexHeight(side) + flowState.getYOffset());
            fvFarSide[side.ordinal()] = fvFarSide[side.ordinal()].withDepth(1.0 - flowState.getFarSideVertexHeight(side) + flowState.getYOffset());

            quadInputsSide.add(new ArrayList<RawQuad>(8));   


            // build left and right quads on the block that edge this side

            RawQuad qiWork = new RawQuad(template, 3);
            qiWork.setupFaceQuad(
                    fvMidSide[side.ordinal()],
                    fvMidCorner[HorizontalCorner.find(side, side.getLeft()).ordinal()],
                    fvCenter,
                    EnumFacing.NORTH);           
            quadInputsCenterLeft[side.ordinal()] = qiWork;
            quadInputsSide.get(side.ordinal()).add(qiWork);
            quadInputsCorner.get(HorizontalCorner.find(side, side.getLeft()).ordinal()).add(qiWork);

            qiWork = new RawQuad(template, 3);
            qiWork.setupFaceQuad(
                    fvMidCorner[HorizontalCorner.find(side, side.getRight()).ordinal()],
                    fvMidSide[side.ordinal()],
                    fvCenter,
                    EnumFacing.NORTH);
            quadInputsCenterRight[side.ordinal()] = qiWork;
            quadInputsSide.get(side.ordinal()).add(qiWork);
            quadInputsCorner.get(HorizontalCorner.find(side, side.getRight()).ordinal()).add(qiWork);

            // side block tri that borders this block
            qiWork = new RawQuad(template, 3);
            qiWork.setupFaceQuad(
                    fvFarSide[side.ordinal()],
                    fvMidCorner[HorizontalCorner.find(side, side.getLeft()).ordinal()],
                    fvMidSide[side.ordinal()],
                    EnumFacing.NORTH);           
            quadInputsSide.get(side.ordinal()).add(qiWork);
            quadInputsCorner.get(HorizontalCorner.find(side, side.getLeft()).ordinal()).add(qiWork);

            qiWork = new RawQuad(template, 3);
            qiWork.setupFaceQuad(
                    fvMidCorner[HorizontalCorner.find(side, side.getRight()).ordinal()],
                    fvFarSide[side.ordinal()],
                    fvMidSide[side.ordinal()],
                    EnumFacing.NORTH);           
            quadInputsSide.get(side.ordinal()).add(qiWork);
            quadInputsCorner.get(HorizontalCorner.find(side, side.getRight()).ordinal()).add(qiWork);

            // side block tri that connects to corner but does not border side
            qiWork = new RawQuad(template, 3);
            qiWork.setupFaceQuad(
                    fvMidCorner[HorizontalCorner.find(side, side.getLeft()).ordinal()],
                    fvFarSide[side.ordinal()],
                    fvFarCorner[HorizontalCorner.find(side, side.getLeft()).ordinal()],
                    EnumFacing.NORTH);           
            quadInputsCorner.get(HorizontalCorner.find(side, side.getLeft()).ordinal()).add(qiWork);

            qiWork = new RawQuad(template, 3);
            qiWork.setupFaceQuad(
                    fvMidCorner[HorizontalCorner.find(side, side.getRight()).ordinal()],
                    fvFarCorner[HorizontalCorner.find(side, side.getRight()).ordinal()],
                    fvFarSide[side.ordinal()],
                    EnumFacing.NORTH);           
            quadInputsCorner.get(HorizontalCorner.find(side, side.getRight()).ordinal()).add(qiWork);

        }

        /** Used for Y coord of bottom face and as lower Y coord of side faces*/
        double bottom = -2 - flowState.getYOffset();// - QuadFactory.EPSILON;

        Vec3d normCenter = quadInputsCenterLeft[0].getFaceNormal();
        normCenter = normCenter.add(quadInputsCenterLeft[1].getFaceNormal());
        normCenter = normCenter.add(quadInputsCenterLeft[2].getFaceNormal());
        normCenter = normCenter.add(quadInputsCenterLeft[3].getFaceNormal());
        normCenter = normCenter.add(quadInputsCenterRight[0].getFaceNormal());
        normCenter = normCenter.add(quadInputsCenterRight[1].getFaceNormal());
        normCenter = normCenter.add(quadInputsCenterRight[2].getFaceNormal());
        normCenter = normCenter.add(quadInputsCenterRight[3].getFaceNormal());
        normCenter = shadowEnhance(normCenter).normalize();

        Vec3d normSide[] = new Vec3d[4];
        for(HorizontalFace side : HorizontalFace.values())
        {
            Vec3d normTemp = null;
            for(RawQuad qi : quadInputsSide.get(side.ordinal()))
            {
                if(normTemp == null) 
                {
                    normTemp = qi.getFaceNormal();
                }
                else
                {
                    normTemp = normTemp.add(qi.getFaceNormal());
                }
            }
            normSide[side.ordinal()] = shadowEnhance(normTemp).normalize();
        }

        Vec3d normCorner[] = new Vec3d[4];
        for(HorizontalCorner corner : HorizontalCorner.values())
        {
            Vec3d normTemp = null;
            for(RawQuad qi : quadInputsCorner.get(corner.ordinal()))
            {
                if(normTemp == null) 
                {
                    normTemp = qi.getFaceNormal();
                }
                else
                {
                    normTemp = normTemp.add(qi.getFaceNormal());
                }
            }
            normCorner[corner.ordinal()] = shadowEnhance(normTemp).normalize();
        }

        //single top face if it is relatively flat and all sides can be drawn without a mid vertex
        if(flowState.isTopSimple())
        {
            RawQuad qi = new RawQuad(template, 4);
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
            if(flowState.isSideSimple(side))
            {
                // top
                if(!flowState.isTopSimple())
                {
                    RawQuad qi = new RawQuad(template, 3);
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
                RawQuad qSide = new RawQuad(template);
                qSide.surfaceInstance = SURFACE_SIDE;
                qSide.setFace(side.face);
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
                //tops
                RawQuad qi = quadInputsCenterLeft[side.ordinal()];
                qi.setVertexNormal(0, normSide[side.ordinal()]);
                //            qi.setVertexNormal(1, normCorner[HorizontalCorner.find(HorizontalFace.values()[side.ordinal()], HorizontalFace.values()[side.ordinal()].getLeft()).ordinal()]);
                qi.setVertexNormal(1, normCorner[HorizontalCorner.find(side, side.getLeft()).ordinal()]);
                qi.setVertexNormal(2, normCenter);

                rawQuads.add(qi);

                qi = quadInputsCenterRight[side.ordinal()];
                //            qi.setVertexNormal(0, normCorner[HorizontalCorner.find(HorizontalFace.values()[side.ordinal()], HorizontalFace.values()[side.ordinal()].getRight()).ordinal()]);
                qi.setVertexNormal(0, normCorner[HorizontalCorner.find(side, side.getRight()).ordinal()]);
                qi.setVertexNormal(1, normSide[side.ordinal()]);
                qi.setVertexNormal(2, normCenter);     

                rawQuads.add(qi);

                //Sides
                RawQuad qSide = new RawQuad(template);
                qSide.surfaceInstance = SURFACE_SIDE;
                qSide.setFace(side.face);
                setupUVForSide(qSide, side.face);

                qSide.setupFaceQuad(
                        new FaceVertex(0, bottom, 0),
                        new FaceVertex(0.5, bottom, 0),
                        new FaceVertex(0.5, flowState.getMidSideVertexHeight(side) - flowState.getYOffset(), 0),
                        new FaceVertex(0, flowState.getMidCornerVertexHeight(HorizontalCorner.find(side, side.getRight())) - flowState.getYOffset(), 0),
                        EnumFacing.UP);
                rawQuads.add(qSide);

                qSide = new RawQuad(qSide);
                qSide.surfaceInstance = SURFACE_SIDE;
                qSide.setFace(side.face);
                qSide.setupFaceQuad(
                        new FaceVertex(0.5, bottom, 0),
                        new FaceVertex(1, bottom, 0),
                        new FaceVertex(1, flowState.getMidCornerVertexHeight(HorizontalCorner.find(side, side.getLeft())) - flowState.getYOffset(), 0),
                        new FaceVertex(0.5, flowState.getMidSideVertexHeight(side) - flowState.getYOffset(), 0),
                        EnumFacing.UP);
                rawQuads.add(qSide);
            }
        }     

        // Bottom face
        RawQuad qBottom = new RawQuad(template);
        //flip X-orthogonalAxis texture on bottom face
//        qBottom.minU = 14 - qBottom.minU;
//        qBottom.maxU = qBottom.minU + 2;
        qBottom.surfaceInstance = SURFACE_SIDE;
        qBottom.setFace(EnumFacing.DOWN);        
        qBottom.setupFaceQuad(0, 0, 1, 1, bottom, EnumFacing.NORTH);
        rawQuads.add(qBottom);



        CSGShape cubeQuads = new CSGShape();
        cubeQuads.add(template.clone().setSurfaceInstance(SURFACE_SIDE).setupFaceQuad(EnumFacing.UP, 0, 0, 1, 1, 0, EnumFacing.NORTH));
        RawQuad faceQuad = template.clone();
        
        //flip X-orthogonalAxis texture on bottom face
//        faceQuad.minU = 14 - faceQuad.minU;
//        faceQuad.maxU = faceQuad.minU + 2;
        
        cubeQuads.add(faceQuad.clone().setSurfaceInstance(SURFACE_SIDE).setupFaceQuad(EnumFacing.DOWN, 0, 0, 1, 1, 0, EnumFacing.NORTH));

        
        cubeQuads.add(setupUVForSide(faceQuad.clone(), EnumFacing.NORTH).setSurfaceInstance(SURFACE_SIDE).setupFaceQuad(EnumFacing.NORTH, 0, 0, 1, 1, 0, EnumFacing.UP));
        cubeQuads.add(setupUVForSide(faceQuad.clone(), EnumFacing.SOUTH).setSurfaceInstance(SURFACE_SIDE).setupFaceQuad(EnumFacing.SOUTH, 0, 0, 1, 1, 0, EnumFacing.UP));
        cubeQuads.add(setupUVForSide(faceQuad.clone(), EnumFacing.EAST).setSurfaceInstance(SURFACE_SIDE).setupFaceQuad(EnumFacing.EAST, 0, 0, 1, 1, 0, EnumFacing.UP));
        cubeQuads.add(setupUVForSide(faceQuad.clone(), EnumFacing.WEST).setSurfaceInstance(SURFACE_SIDE).setupFaceQuad(EnumFacing.WEST, 0, 0, 1, 1, 0, EnumFacing.UP));

        rawQuads = rawQuads.intersect(cubeQuads);

        // scale all quads UVs according to position to match what surface painter expects
        // Any quads with a null face are assumed to be part of the top face
        
        // We want top face textures to always join irrespective of Y.
        // Other face can vary based on orthogonal dimension to break up appearance of layers.
        for(RawQuad quad : rawQuads)
        {
            EnumFacing face = quad.getNominalFace();
            if(face == null)
            {
                assert false : "Terrain Mesh Generator is outputting quad with null nominal face.";
                face = EnumFacing.UP;
            }
            
            switch(face)
            {
                case NORTH:
                {
                    int zHash = MathHelper.hash(modelState.getPosZ());
                    quad.minU = 255 - ((modelState.getPosX() + (zHash >> 16)) & 0xFF);
                    quad.maxU = quad.minU +  1;
                    quad.minV = 255 - ((modelState.getPosY() + zHash) & 0xFF);
                    quad.maxV = quad.minV + 1;
                    break;
                }
                case SOUTH:
                {
                    int zHash = MathHelper.hash(modelState.getPosZ());
                    quad.minU = (modelState.getPosX() + (zHash >> 16)) & 0xFF;
                    quad.maxU = quad.minU +  1;
                    quad.minV = 255 - ((modelState.getPosY() + zHash) & 0xFF);
                    quad.maxV = quad.minV + 1;
                    break;
                }
                case EAST:
                {
                    int xHash = MathHelper.hash(modelState.getPosX());
                    quad.minU = 255 - ((modelState.getPosZ() + (xHash >> 16)) & 0xFF);
                    quad.maxU = quad.minU +  1;
                    quad.minV = 255 - ((modelState.getPosY() + xHash) & 0xFF);
                    quad.maxV = quad.minV + 1;
                    break;
                }
                case WEST:
                {
                    int xHash = MathHelper.hash(modelState.getPosX());
                    quad.minU = (modelState.getPosZ() + (xHash >> 16)) & 0xFF;
                    quad.maxU = quad.minU +  1;
                    quad.minV = 255 - ((modelState.getPosY() + xHash) & 0xFF);
                    quad.maxV = quad.minV + 1;
                    break;
                } 
                case DOWN:
                {
                    int yHash = MathHelper.hash(modelState.getPosY());
                    quad.minU = 255 - ((modelState.getPosX() + (yHash >> 16)) & 0xFF);
                    quad.maxU = quad.minU +  1;
                    quad.minV = (modelState.getPosZ() + (yHash >> 16)) & 0xFF;
                    quad.maxV = quad.minV + 1;
                    break;
                }
                case UP:
                default:
                {
                    quad.minU = modelState.getPosX();
                    quad.maxU = quad.minU +  1;
                    quad.minV = modelState.getPosZ();
                    quad.maxV = quad.minV + 1;
                    break;
                }
            }
        }
        return rawQuads;
    }

   
    private RawQuad setupUVForSide(RawQuad quad, EnumFacing face)
    {
        
//        quad.minU = (face.getAxis() == Axis.X ? flowTex.getZ() : flowTex.getX()) * 2;
        // need to flip U on these side faces so that textures align properly
        if(face == EnumFacing.EAST || face == EnumFacing.NORTH) 
        {
            quad.minU = 16;
            quad.maxU = 0;
        }
        else
        {
            quad.minU = 0;
            quad.maxU = 16;
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
            Log.error("TerrainMeshFactory received Collision Bounding Box check for a foreign block.", ex);
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
