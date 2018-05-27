package grondag.exotic_matter.render;

/**
 * Used by painters to know how to UV map a surface for painting.
 */
public enum SurfaceTopology
{
    /** 
     * Surface represents six faces of a single block. This is the
     * topology to use for any model that fits within a single world
     * block position and has some approximate orientation within the world.<p>
     * 
     * Face is identified by {@link IPolygon#getNominalFace()} and does
     * not have to actually be on the face of a cube. (But it will be
     * textured as if it were.)<p>
     * 
     * Models generating quads for this topology should emit
     * quads with min/max UVs with the full span of 0-16. A UV
     * span of 16 represents one in-world block.  The model mesh
     * do not have to span a full block - vertex UV coordinates
     * will be in the range 0-1 and map the mesh geometry to the quad UV span.<p>
     * 
     * During BigTex painting, quad UV min/max will be set to some 
     * sub-span of 0-16 based on location within the world and texture scale.
     * For single-block tiled texture painting, the span is not altered but UV coordinates
     * may be flipped, rotated or the underlying texture randomized if multiple versions exist.<p>
     * 
     * Surface UV scale should always be 1.0 but is generally ignored for this surface type.
     */
    CUBIC,
    
    /** 
     * Quad is part of a continuous surface.  Textures tile at the edges of the UV span 0-16.
     * The min/max UV for any quad within the surface will be in the range 0-16 but generally
     * will be some subspan unless quad fully covers a surface about one-block in size.<p>
     * 
     * In some cases (cylinders and toroids - more below) it may be useful to assign texture 
     * coordinates in one dimension (u or v) values below 0 or above 16.  
     * In these cases, the value modulo 16 is used. The painter will use the dividend for randomization.
     * For example, 17 would be handled as 1 and -1 would be handled as 15, but the painter
     * would try to randomize textures to be different from textures in the span 0-16.<p>
     * 
     * Surface UV scale should be set to the approximate in-world distance of the 0-16 UV span. 
     * The minimum scale is 1.0, which means the entire texture must always be scaled to fit the 0-16 span
     * because no textures are designed (at least right now) to cover less than one MC block face.
     * At larger UV scales, the texture will be tiled and, if possible, randomized within the 0-16 span.
     * For small models with big textures, the texture will simply be scaled down to fit. 
     * This is done to ensure correct wrapping at the 0-16 boundary. <p>
     * 
     * There is no explicit limit on the UV scale that can be assigned, except that it must be at least
     * 1 and must be a power of 2. Values under 1 are set to one and values that are not a power of 2
     * are rounded to the closest power of 2.<p>
     * 
     * Mesh generators can emit quads with UV coordinates that span a 0-16 boundary in either dimension.
     * For example, a quad can start at 15.5 and end at 16.5. However, this will force the painter
     * to split the quad along the texture boundary(s) into two (or four) quads. This complicates
     * the painter logic but greatly simplifies mesh generation.<p>
     * 
     * It is generally preferable to use the largest UV scale possible. For surfaces that do not need
     * to wrap this will be the largest in-world dimension of the model surface (rounded to the closest power of 2). 
     * This gives the painter more information to drive randomization of textures within the surface
     * to prevent visible repetition. It also means the model doesn't have to consider texture tiling
     * for textures that are smaller than the whole surface - the painter will handle it.<p>
     * 
     * For surfaces that wrap on one or both dimensions (spheres, toroids, cylinders...) the model must
     * ensure that emitted quads meet at 0-16 boundary (or at some multiple of 16). The model
     * should generally use the span of 0-16 for the smallest wrapping dimension and set the texture
     * scale based on the in-world size of the surface in that dimension.<p>
     * 
     * Unlike a cubic surface, in-world position can't be used for face randomization within the
     * model. As mentioned before, for UV coordinates outside the 0-16 range, the painter can
     * used the dividend from modulo 16 division for randomization. The painter will also use texture salt 
     * (specified in the surface instance) and nominal face (if provided) to drive texture
     * randomization.  These last two are useful for smaller models that have to wrap the same texture within
     * a small visible region. (The painter <em>may</em> use in-world origin position or other
     * in-world information to randomize the whole texturing process, but this does not provide
     * variation of faces within the model.)<p>
     * 
     * Example 1: A cubic sphere of one block diameter has a UV scale of 1 with nominal faces assigned to quads.
     * All quads will have UV min/max of 0-16. The quad painter will scale the selected texture to fit
     * within the 0-16 span. Big textures will appear small but will wrap correctly.  Nominal face
     * will be used to prevent visual repetition.<p>
     * 
     * Example 2: A single-texture cylinder of radius 1 and length 10 will have a UV scale of 8 because the texture
     * must wrap on the circular surface and the approximate diameter will be ~6.2, giving 8 as the closest power of 2.
     * The cylinder is longer than 8 in the lengthwise dimension, so UV coordinate outside of 0-16 will be used
     * in that dimension, giving the painter information it can use to avoid repetition along the length. 
     * Quads on the cylinder ends will use the same UV scale so that textures appear similar, but do not
     * need to wrap or join with the lengthwise surface, so whatever UV coordinates are convenient (and consistent
     * with the scale) can be used.  If one, two or four-block texture is selected, it will be tiled within
     * the surface because the texture scale is 8. And eight-block texture will wrap around the cylinder exactly
     * once at something close to the intended visual scale and larger textures will be scaled down to wrap exactly once.
     */
    TILED
}
