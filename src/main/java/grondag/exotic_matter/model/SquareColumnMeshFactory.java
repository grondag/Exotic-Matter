package grondag.exotic_matter.model;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.render.FaceVertex;
import grondag.exotic_matter.render.QuadHelper;
import grondag.exotic_matter.render.Poly;
import grondag.exotic_matter.render.SideShape;
import grondag.exotic_matter.render.SimpleQuadBounds;
import grondag.exotic_matter.render.Surface;
import grondag.exotic_matter.render.SurfaceTopology;
import grondag.exotic_matter.render.SurfaceType;
import grondag.exotic_matter.render.Surface.SurfaceInstance;
import grondag.exotic_matter.varia.BitPacker;
import grondag.exotic_matter.varia.Color;
import grondag.exotic_matter.varia.BitPacker.BitElement.BooleanElement;
import grondag.exotic_matter.varia.BitPacker.BitElement.IntElement;
import grondag.exotic_matter.world.CornerJoinBlockState;
import grondag.exotic_matter.world.CornerJoinFaceState;
import grondag.exotic_matter.world.FaceSide;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SquareColumnMeshFactory extends ShapeMeshGenerator
{
    public static final int MIN_CUTS = 1;
    public static final int MAX_CUTS = 3;
    
    private static final Surface SURFACE_MAIN = new Surface(SurfaceType.MAIN, SurfaceTopology.CUBIC);
    private static final Surface SURFACE_LAMP = new Surface(SurfaceType.LAMP, SurfaceTopology.CUBIC);
    private static final Surface SURFACE_CUT = new Surface(SurfaceType.CUT, SurfaceTopology.CUBIC);
    
    private static final BitPacker STATE_PACKER = new BitPacker();
    private static final BooleanElement STATE_ARE_CUTS_ON_EDGE = STATE_PACKER.createBooleanElement();
    private static final IntElement STATE_CUT_COUNT = STATE_PACKER.createIntElement(MIN_CUTS, MAX_CUTS);

    private static class FaceSpec
    {
        private final int cutCount;
//        private final boolean areCutsOnEdge;
        private final double cutWidth;
        private final double baseMarginWidth;
        private final double marginOffset;
        private final double cutDepth;
        
        private FaceSpec(int cutCount, boolean areCutsOnEdge)
        {
            this.cutCount = cutCount;
//            this.areCutsOnEdge = areCutsOnEdge;
            
            if(areCutsOnEdge)
              {
                  cutWidth = 0.5 / (cutCount + 1.0);
                  baseMarginWidth = 1.5 * cutWidth;
                  marginOffset = -0.5;
                  cutDepth = cutWidth * 0.8;
              }
              else
              {
                  cutWidth = 0.5 / (cutCount + 2.0);
                  baseMarginWidth = 2.5 * cutWidth;
                  marginOffset = 0.5;
                  cutDepth = cutWidth / 2.0;
              }
        }
    }
    
    public SquareColumnMeshFactory()
    {
        super(
                StateFormat.BLOCK, 
                ModelStateData.STATE_FLAG_NEEDS_CORNER_JOIN | ModelStateData.STATE_FLAG_HAS_AXIS, 
                STATE_CUT_COUNT.setValue(3, STATE_ARE_CUTS_ON_EDGE.setValue(true, 0)), 
                SURFACE_MAIN, SURFACE_LAMP, SURFACE_CUT
        );
    }

    @Override
    public @Nonnull List<Poly> getShapeQuads(ISuperModelState modelState)
    {
        FaceSpec spec = new FaceSpec(getCutCount(modelState), areCutsOnEdge(modelState));
        
        ImmutableList.Builder<Poly> general = new ImmutableList.Builder<Poly>();
        for(EnumFacing face : EnumFacing.VALUES)
        {
            general.addAll(this.makeFaceQuads(modelState, face, spec));
        }        
        return general.build();
    }

    @Override
    public @Nonnull ICollisionHandler collisionHandler()
    {
        return CubeCollisionHandler.INSTANCE;
    }

    @Override
    public boolean isCube(ISuperModelState modelState)
    {
        return false;
    }

    @Override
    public boolean rotateBlock(IBlockState blockState, World world, BlockPos pos, EnumFacing axis, ISuperBlock block, ISuperModelState modelState)
    {
        return false;
    }

    @Override
    public int geometricSkyOcclusion(ISuperModelState modelState)
    {
        return 255;
    }

    @Override
    public BlockOrientationType orientationType(ISuperModelState modelState)
    {
        return BlockOrientationType.AXIS;
    } 
    
    @Override
    public SideShape sideShape(ISuperModelState modelState, EnumFacing side)
    {
        return SideShape.PARTIAL;
    }

    private List<Poly> makeFaceQuads(ISuperModelState state, EnumFacing face, FaceSpec spec)  
    {
        if (face == null) return Collections.emptyList();

        CornerJoinBlockState bjs = state.getCornerJoin();
        EnumFacing.Axis axis = state.getAxis();
        SurfaceInstance cutSurface = SURFACE_CUT.unitInstance.withLampGradient(state.isFullBrightness(PaintLayer.LAMP)).withAllowBorders(false);

        List<Poly> retVal = null;

        
        Poly quadInputs = new Poly();
        quadInputs.setColor(Color.WHITE);
        quadInputs.setLockUV(true);


        if(face.getAxis() == axis)
        {
            retVal = makeCapFace(face, quadInputs, bjs.getFaceJoinState(face), spec, axis, cutSurface);
        }
        else
        {
            retVal = makeSideFace(face, quadInputs, bjs.getFaceJoinState(face), spec, axis, cutSurface);
        }

        return retVal;
    }

    private List<Poly> makeSideFace(EnumFacing face, Poly template, CornerJoinFaceState fjs, FaceSpec spec, EnumFacing.Axis axis, SurfaceInstance cutSurface)
    {
        if(fjs == CornerJoinFaceState.NO_FACE) return Collections.emptyList();

        ImmutableList.Builder<Poly> builder = new ImmutableList.Builder<Poly>();
        
        EnumFacing topFace = QuadHelper.getAxisTop(axis);
        EnumFacing bottomFace = topFace.getOpposite();
        EnumFacing leftFace = QuadHelper.leftOf(face, topFace);
        EnumFacing rightFace = QuadHelper.rightOf(face, topFace);

        int actualCutCount = spec.cutCount;

        /** used to randomize cut textures */
        int nextTextureSalt = 1;
        
        boolean hasLeftJoin = fjs.isJoined(leftFace, face);
        boolean hasRightJoin = fjs.isJoined(rightFace, face);
        boolean hasTopJoin = fjs.isJoined(topFace, face);
        boolean hasBottomJoin = fjs.isJoined(bottomFace, face);

        if (hasLeftJoin) actualCutCount++;
        if (hasRightJoin) actualCutCount++;

        double leftMarginWidth = hasLeftJoin ? spec.marginOffset * spec.cutWidth : spec.baseMarginWidth;
        double rightMarginWidth = hasRightJoin ? spec.marginOffset * spec.cutWidth : spec.baseMarginWidth;
        double topCapHeight = hasTopJoin ? 0.0 : spec.baseMarginWidth;
        double bottomCapHeight = hasBottomJoin ? 0.0 : spec.baseMarginWidth;

        

        //bottom
        if(!hasBottomJoin)
        {
            Poly quad = template.clone();
            quad.setSurfaceInstance(SURFACE_MAIN.unitInstance);
            quad.setupFaceQuad(face, 0.0, 0.0, 1.0, bottomCapHeight, 0.0, topFace);
            builder.add(quad);
        }              

        //top
        if(!hasTopJoin)
        {
            Poly quad = template.clone();
            quad.setSurfaceInstance(SURFACE_MAIN.unitInstance);
            quad.setupFaceQuad(face, 0.0, 1.0 - topCapHeight, 1.0, 1.0, 0.0, topFace);
            builder.add(quad);
        }

        //left margin
        if(leftMarginWidth > 0)
        {
            Poly quad = template.clone();
            quad.setSurfaceInstance(SURFACE_MAIN.unitInstance);
            quad.setupFaceQuad(face, 0.0, bottomCapHeight, leftMarginWidth, 1.0 - topCapHeight, 0.0, topFace);
            builder.add(quad);
        }

        // right margin
        if(rightMarginWidth > 0)
        {
            Poly quad = template.clone();
            quad.setSurfaceInstance(SURFACE_MAIN.unitInstance);
            quad.setupFaceQuad(face, 1.0 - rightMarginWidth, bottomCapHeight, 1.0, 1.0 - topCapHeight, 0.0, topFace);
            builder.add(quad);
        }

        //splines
        for(int i = 0; i < actualCutCount - 1; i++)
        {
            Poly quad = template.clone();
            quad.setSurfaceInstance(SURFACE_MAIN.unitInstance);
            quad.setupFaceQuad(face, leftMarginWidth + spec.cutWidth * 2.0 * (double)i + spec.cutWidth, bottomCapHeight,
                    leftMarginWidth + spec.cutWidth * 2.0 * ((double)i + 1.0), 1.0 - topCapHeight, 0.0, topFace);
            builder.add(quad);
        }

        // top left corner
        if(fjs.needsCorner(topFace, leftFace, face))
        {
            Poly quad = template.clone();
            quad.setSurfaceInstance(SURFACE_MAIN.unitInstance);
            quad.setupFaceQuad(face, Math.max(leftMarginWidth, 0.0), 1.0 - spec.baseMarginWidth, leftMarginWidth + spec.cutWidth, 1.0, 0.0, topFace);
            builder.add(quad);
        }

        // bottom left corner
        if(fjs.needsCorner(bottomFace, leftFace, face))
        {
            Poly quad = template.clone();
            quad.setSurfaceInstance(SURFACE_MAIN.unitInstance);
            quad.setupFaceQuad(face, Math.max(leftMarginWidth, 0.0), 0.0, leftMarginWidth + spec.cutWidth, spec.baseMarginWidth, 0.0, topFace);
            builder.add(quad);
        }

        // top right corner
        if(fjs.needsCorner(topFace, rightFace, face))
        {
            Poly quad = template.clone();
            quad.setSurfaceInstance(SURFACE_MAIN.unitInstance);
            quad.setupFaceQuad(face, 1.0 - rightMarginWidth - spec.cutWidth, 1.0 - spec.baseMarginWidth, Math.min(1.0 - rightMarginWidth, 1.0), 1.0, 0.0, topFace);
            builder.add(quad);
        }

        // bottom right corner
        if(fjs.needsCorner(bottomFace, rightFace, face))
        {
            Poly quad = template.clone();
            quad.setSurfaceInstance(SURFACE_MAIN.unitInstance);
            quad.setupFaceQuad(face, 1.0 - rightMarginWidth - spec.cutWidth, 0.0, Math.min(1.0 - rightMarginWidth, 1.0), spec.baseMarginWidth, 0.0, topFace);
            builder.add(quad);
        }

        for(int i = 0; i < actualCutCount; i++)
        {
            double sx0 = Math.max(0.0, leftMarginWidth + spec.cutWidth * 2.0 * (double)i);
            double sx1 = Math.min(1.0, leftMarginWidth + spec.cutWidth * 2.0 * (double)i + spec.cutWidth);

            // left face
            if(sx0 > 0.0001)
            {
                Poly quad = template.clone();
                quad.setSurfaceInstance(cutSurface.withTextureSalt(nextTextureSalt++));
                setupCutSideQuad(quad, new SimpleQuadBounds(rightFace, bottomCapHeight, 1.0-spec.cutDepth, 1.0-topCapHeight, 1.0, 1.0 - sx0, face));
                builder.add(quad);
            }

            // right face
            if(sx1 < 0.9999)
            {
                Poly quad = template.clone();
                quad.setSurfaceInstance(cutSurface.withTextureSalt(nextTextureSalt++));
                setupCutSideQuad(quad, new SimpleQuadBounds(leftFace, topCapHeight, 1.0-spec.cutDepth, 1.0-bottomCapHeight, 1.0, sx1, face));
                builder.add(quad);
            }

            // top face
            if(topCapHeight > 0)
            {
                Poly quad = template.clone();
                quad.setSurfaceInstance(cutSurface.withTextureSalt(nextTextureSalt++));
                setupCutSideQuad(quad, new SimpleQuadBounds(bottomFace, sx0, 1.0-spec.cutDepth, sx1, 1.0, 1.0 -topCapHeight, face));
                builder.add(quad);
            }

            // bottom face
            if(bottomCapHeight > 0)
            {
                Poly quad = template.clone();
                quad.setSurfaceInstance(cutSurface.withTextureSalt(nextTextureSalt++));
                setupCutSideQuad(quad, new SimpleQuadBounds(topFace, 1.0 -sx1, 1.0-spec.cutDepth, 1.0 -sx0, 1.0, 1.0 -bottomCapHeight, face));
                builder.add(quad);
            }

            // top left corner
            if(fjs.needsCorner(topFace, leftFace, face))
            {
                Poly quad = template.clone();
                quad.setSurfaceInstance(cutSurface.withTextureSalt(nextTextureSalt++));
                setupCutSideQuad(quad, new SimpleQuadBounds(bottomFace, Math.max(leftMarginWidth, 0.0), 1.0-spec.cutDepth, leftMarginWidth + spec.cutWidth, 1.0, 1.0 -spec.baseMarginWidth, face));
                builder.add(quad);
            }

            // bottom left corner
            if(fjs.needsCorner(bottomFace, leftFace, face))
            {
                Poly quad = template.clone();
                quad.setSurfaceInstance(cutSurface.withTextureSalt(nextTextureSalt++));
                setupCutSideQuad(quad, new SimpleQuadBounds(topFace, 1.0 - leftMarginWidth - spec.cutWidth, 1.0-spec.cutDepth, Math.min(1.0 - leftMarginWidth, 1.0), 1.0, 1.0 -spec.baseMarginWidth, face));
                builder.add(quad);

            }

            // top right corner
            if(fjs.needsCorner(topFace, rightFace, face))
            {
                Poly quad = template.clone();
                quad.setSurfaceInstance(cutSurface.withTextureSalt(nextTextureSalt++));
                setupCutSideQuad(quad, new SimpleQuadBounds(bottomFace, 1.0 - rightMarginWidth - spec.cutWidth, 1.0-spec.cutDepth, Math.min(1.0 - rightMarginWidth, 1.0), 1.0, 1.0 -spec.baseMarginWidth, face));
                builder.add(quad);
            }

            // bottom right corner
            if(fjs.needsCorner(bottomFace, rightFace, face))
            {
                Poly quad = template.clone();
                quad.setSurfaceInstance(cutSurface.withTextureSalt(nextTextureSalt++));
                setupCutSideQuad(quad, new SimpleQuadBounds(topFace, Math.max(rightMarginWidth, 0.0), 1.0-spec.cutDepth, rightMarginWidth + spec.cutWidth, 1.0, 1.0 -spec.baseMarginWidth, face));
                builder.add(quad);
            }
        }

        // inner lamp surface can be a single poly
        {
            Poly quad = template.clone();
            quad.setSurfaceInstance(SURFACE_LAMP.unitInstance);
            quad.setupFaceQuad(face, Math.max(0.0, leftMarginWidth), bottomCapHeight, Math.min(1.0, 1.0 - rightMarginWidth), 1.0 - topCapHeight, spec.cutDepth, topFace);
            builder.add(quad);
        }
        
        return builder.build();
    }

    private List<Poly> makeCapFace(EnumFacing face, Poly template, CornerJoinFaceState fjs, FaceSpec spec, EnumFacing.Axis axis, SurfaceInstance cutSurface)
    {
        if(fjs == CornerJoinFaceState.NO_FACE) return Collections.emptyList();

        ImmutableList.Builder<Poly> builder = new ImmutableList.Builder<Poly>();

        /** used to randomize cut textures */
        int nextTextureSalt = 1;
        
        // lamp bottom can be a single poly
        {
            Poly quad = template.clone();
            quad.setSurfaceInstance(SURFACE_LAMP.unitInstance);
            quad.setupFaceQuad(face, 
                    fjs.isJoined(FaceSide.LEFT) ? 0.0 : spec.baseMarginWidth, 
                            fjs.isJoined(FaceSide.BOTTOM) ? 0.0 : spec.baseMarginWidth, 
                                    fjs.isJoined(FaceSide.RIGHT) ? 1.0 : 1.0 -spec.baseMarginWidth, 
                                            fjs.isJoined(FaceSide.TOP) ? 1.0 : 1.0 -spec.baseMarginWidth, 
                                                    spec.cutDepth, 
                                                    FaceSide.TOP.getRelativeFace(face));
            builder.add(quad);
        }


        // build quarter slice of cap for each side separately
        // specifications below are oriented with the side at top of cap face

        for(FaceSide joinSide : FaceSide.values())
        {

            EnumFacing side = joinSide.getRelativeFace(face);

            if(fjs.isJoined(joinSide))
            {
                //This side is joined, so connect cuts to other block on this side.

                
                // margin corner faces
                {
                    Poly tri = new Poly(template, 3);
                    tri.setSurfaceInstance(SURFACE_MAIN.unitInstance);
                    tri.setupFaceQuad(face, 
                            new FaceVertex(spec.baseMarginWidth, 1.0 -spec.baseMarginWidth, 0),
                            new FaceVertex(spec.baseMarginWidth, 1.0, 0),
                            new FaceVertex(0.0, 1.0, 0), 
                            side);
                    builder.add(tri);
                }
                {
                    Poly tri = new Poly(template, 3);
                    tri.setSurfaceInstance(SURFACE_MAIN.unitInstance);
                    tri.setupFaceQuad(face, 
                            new FaceVertex(1.0 - spec.baseMarginWidth, 1.0, 0),  
                            new FaceVertex(1.0 - spec.baseMarginWidth, 1.0 -spec.baseMarginWidth, 0), 
                            new FaceVertex(1.0, 1.0, 0), 
                            side);
                    builder.add(tri);                
                }
                

                // margin corner sides
                {
                    Poly quad = template.clone();
                    quad.setSurfaceInstance(cutSurface.withTextureSalt(nextTextureSalt++));
                    setupCutSideQuad(quad, new SimpleQuadBounds(QuadHelper.rightOf(face, side), 1.0 -spec.baseMarginWidth, 1.0 -spec.cutDepth, 1.0, 1, 1.0 -spec.baseMarginWidth, face));
                    builder.add(quad);
                }
                {
                    Poly quad = template.clone();
                    quad.setSurfaceInstance(cutSurface.withTextureSalt(nextTextureSalt++));
                    setupCutSideQuad(quad, new SimpleQuadBounds(QuadHelper.leftOf(face, side), 0.0, 1.0 -spec.cutDepth, spec.baseMarginWidth, 1, 1.0 -spec.baseMarginWidth, face));
                    builder.add(quad);
                }

                //splines
                for (int i = 0; i < spec.cutCount / 2; i++)
                {
                    double xLeft = spec.baseMarginWidth + ((double)i * 2.0 + 1.0) * spec.cutWidth;
                    double xRight = Math.min(xLeft + spec.cutWidth, 0.5);

                    {
                        Poly quad = template.clone();
                        quad.setSurfaceInstance(SURFACE_MAIN.unitInstance);
                        quad.setupFaceQuad(face, 
                                new FaceVertex(xLeft, 1.0 -xLeft, 0),  
                                new FaceVertex(xRight, 1.0 -xRight, 0),
                                new FaceVertex(xRight, 1.0, 0), 
                                new FaceVertex(xLeft, 1.0, 0), 
                                side);
                        builder.add(quad);
                    }
                    {
                        // mirror on right side, reverse winding order
                        Poly quad = template.clone();
                        quad.setSurfaceInstance(SURFACE_MAIN.unitInstance);                          
                        quad.setupFaceQuad(face, 
                                new FaceVertex(1.0 - xRight, 1.0 - xRight , 0),  
                                new FaceVertex(1.0 - xLeft, 1.0 -xLeft, 0),
                                new FaceVertex(1.0 - xLeft, 1.0, 0), 
                                new FaceVertex(1.0 - xRight, 1.0, 0), 
                                side);
                        builder.add(quad);
                    }

                    // cut sides
                    // with odd number of cuts, these overlap in middle, avoid with this check

                    if(xLeft < 0.4999)
                    {
                        {
                            Poly quad = template.clone();
                            quad.setSurfaceInstance(cutSurface.withTextureSalt(nextTextureSalt++));
                            setupCutSideQuad(quad, new SimpleQuadBounds(QuadHelper.leftOf(face, side), 0.0, 1.0 -spec.cutDepth, xLeft, 1.0, xLeft, face));
                            builder.add(quad);
                        }
                        {
                            Poly quad = template.clone();
                            quad.setSurfaceInstance(cutSurface.withTextureSalt(nextTextureSalt++));
                            setupCutSideQuad(quad, new SimpleQuadBounds(QuadHelper.rightOf(face, side), 1.0 -xLeft, 1.0 -spec.cutDepth, 1.0, 1.0, xLeft, face));
                            builder.add(quad);
                        }
                    }
                    if(xRight < 0.4999)
                    {
                        {
                            Poly quad = template.clone();
                            quad.setSurfaceInstance(cutSurface.withTextureSalt(nextTextureSalt++));
                            setupCutSideQuad(quad, new SimpleQuadBounds(QuadHelper.rightOf(face, side), 1.0 -xRight, 1.0 -spec.cutDepth, 1.0, 1.0, 1.0 -xRight, face));
                            builder.add(quad);
                        }
                        {
                            Poly quad = template.clone();
                            quad.setSurfaceInstance(cutSurface.withTextureSalt(nextTextureSalt++));
                            setupCutSideQuad(quad, new SimpleQuadBounds(QuadHelper.leftOf(face, side), 0.0, 1.0 -spec.cutDepth, xRight, 1.0, 1.0 -xRight, face));
                            builder.add(quad);
                        }
                    }
                }
            }
            else  
            {    
                // This side isn't joined, so don't connect cuts to other block on this side.

                {
                    // outer face
                    Poly quad = template.clone();
                    quad.setSurfaceInstance(SURFACE_MAIN.unitInstance);
                    quad.setupFaceQuad(face, 
                            new FaceVertex(spec.baseMarginWidth, 1.0 -spec.baseMarginWidth, 0),  
                            new FaceVertex(1.0 -spec.baseMarginWidth, 1.0 -spec.baseMarginWidth, 0),
                            new FaceVertex(1.0, 1.0, 0), 
                            new FaceVertex(0, 1.0, 0), 
                            side);
                    builder.add(quad);   
                }

                {
                    // outer cut sides
                    for(int i = 0; i < (spec.cutCount + 1.0) / 2; i++)
                    {
                        double offset = spec.baseMarginWidth + (spec.cutWidth * 2.0 * i);

                        Poly quad = template.clone();
                        quad.setSurfaceInstance(cutSurface.withTextureSalt(nextTextureSalt++));
                        setupCutSideQuad(quad, new SimpleQuadBounds(side.getOpposite(), offset, 1.0 -spec.cutDepth, 1.0 -offset, 1.0, 1.0 -offset, face));
                        builder.add(quad);

                    }
                }

                for(int i = 0; i < spec.cutCount / 2; i++)
                {
                    double offset = spec.baseMarginWidth + spec.cutWidth * (2.0 * (double)i + 1.0);

                    {
                        // inner cut sides
                        Poly quad = template.clone();
                        quad.setSurfaceInstance(cutSurface.withTextureSalt(nextTextureSalt++));
                        setupCutSideQuad(quad, new SimpleQuadBounds(side, offset, 1.0 -spec.cutDepth, 1.0 -offset, 1.0, offset, face));
                        builder.add(quad);
                    }

                    {
                        // spline / center
                        Poly quad = template.clone();
                        quad.setSurfaceInstance(SURFACE_MAIN.unitInstance);
                        quad.setupFaceQuad(face, 
                                new FaceVertex(Math.min(0.5, offset + spec.cutWidth), 1.0 - offset - spec.cutWidth, 0),  
                                new FaceVertex(Math.max(0.5, 1.0 - offset - spec.cutWidth), 1.0 - offset - spec.cutWidth, 0),
                                new FaceVertex(1.0 - offset, 1.0 - offset, 0), 
                                new FaceVertex(offset, 1.0 - offset, 0), 
                                side);
                        builder.add(quad);  
                    }

                }
            }
        }

        return builder.build();
    }

    private void setupCutSideQuad(Poly qi, SimpleQuadBounds qb)
    {
        int deepColor = qi.getSurfaceInstance().isLampGradient ? Color.BLACK : Color.WHITE;
        
        qi.setupFaceQuad(qb.face,
                new FaceVertex.Colored(qb.x0, qb.y0, qb.depth, Color.WHITE),
                new FaceVertex.Colored(qb.x1, qb.y0, qb.depth, Color.WHITE),
                new FaceVertex.Colored(qb.x1, qb.y1, qb.depth, deepColor),
                new FaceVertex.Colored(qb.x0, qb.y1, qb.depth, deepColor), 
                qb.topFace);
    }
    
    /** 
     * If true, cuts in shape are on the block boundary.
     * Reads value from static shape bits in model state 
     */
    public static boolean areCutsOnEdge(ISuperModelState modelState)
    {
        return STATE_ARE_CUTS_ON_EDGE.getValue(modelState.getStaticShapeBits());
    }

    /** 
     * If true, cuts in shape are on the block boundary.
     * Saves value in static shape bits in model state 
     */
    public static void setCutsOnEdge(boolean areCutsOnEdge, ISuperModelState modelState)
    {
        modelState.setStaticShapeBits(STATE_ARE_CUTS_ON_EDGE.setValue(areCutsOnEdge, modelState.getStaticShapeBits()));
    }
    
    /** 
     * Number of cuts that appear on each face of model.
     * Reads value from static shape bits in model state 
     */
    public static int getCutCount(ISuperModelState modelState)
    {
        return STATE_CUT_COUNT.getValue(modelState.getStaticShapeBits());
    }

    /** 
     * Number of cuts that appear on each face of model.
     * Saves value in static shape bits in model state 
     */
    public static void setCutCount(int cutCount, ISuperModelState modelState)
    {
        modelState.setStaticShapeBits(STATE_CUT_COUNT.setValue(cutCount, modelState.getStaticShapeBits()));
    }
 
}
