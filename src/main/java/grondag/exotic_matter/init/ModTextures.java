package grondag.exotic_matter.init;

import static grondag.exotic_matter.model.TextureRotationType.CONSISTENT;
import static grondag.exotic_matter.model.TextureRotationType.FIXED;
import static grondag.exotic_matter.model.TextureRotationType.RANDOM;
import static grondag.exotic_matter.world.Rotation.ROTATE_180;
import static grondag.exotic_matter.world.Rotation.ROTATE_270;
import static grondag.exotic_matter.world.Rotation.ROTATE_90;
import static grondag.exotic_matter.world.Rotation.ROTATE_NONE;

import grondag.exotic_matter.ConfigXM;
import grondag.exotic_matter.model.ITexturePalette;
import grondag.exotic_matter.model.TextureGroup;
import grondag.exotic_matter.model.TextureLayout;
import grondag.exotic_matter.model.TexturePaletteRegistry;
import grondag.exotic_matter.model.TexturePaletteSpec;
import grondag.exotic_matter.model.TextureRenderIntent;
import grondag.exotic_matter.model.TextureScale;

public class ModTextures
{
    //======================================================================
    //  TEST/DEBUG TEXTURES - NOT LOADED UNLESS NEEDED
    //======================================================================
    
    // but still load placeholders so we don't lose test texture attributes on
    // blocks if test textures are temporarily disabled
    
    public static final ITexturePalette BIGTEX_TEST_SINGLE = TexturePaletteRegistry.addTexturePallette("bigtex_test_single", ConfigXM.BLOCKS.showHiddenTextures ? "bigtex_single" : "noise_moderate_0_0",
            new TexturePaletteSpec().withVersionCount(1).withScale(TextureScale.SMALL).withLayout(TextureLayout.BIGTEX)
            .withRotation(CONSISTENT.with(ROTATE_NONE)).withRenderIntent(TextureRenderIntent.BASE_ONLY).withGroups(TextureGroup.HIDDEN_TILES));
    
    public static final ITexturePalette BIGTEX_TEST1  = TexturePaletteRegistry.addTexturePallette("big_tex_test1", ConfigXM.BLOCKS.showHiddenTextures ? "bigtex" : "noise_moderate",
            new TexturePaletteSpec().withVersionCount(4).withScale(TextureScale.TINY).withLayout(TextureLayout.SPLIT_X_8)
            .withRotation(RANDOM.with(ROTATE_NONE)).withRenderIntent(TextureRenderIntent.BASE_ONLY).withGroups(TextureGroup.HIDDEN_TILES));
    
    public static final ITexturePalette BIGTEX_TEST2 = TexturePaletteRegistry.addTexturePallette("big_tex_test2", ConfigXM.BLOCKS.showHiddenTextures ? "bigtex" : "noise_moderate",
            new TexturePaletteSpec(BIGTEX_TEST1).withScale(TextureScale.SMALL));
    public static final ITexturePalette BIGTEX_TEST3 = TexturePaletteRegistry.addTexturePallette("big_tex_test3", ConfigXM.BLOCKS.showHiddenTextures ? "bigtex" : "noise_moderate",
            new TexturePaletteSpec(BIGTEX_TEST1).withScale(TextureScale.MEDIUM));
    public static final ITexturePalette BIGTEX_TEST4 = TexturePaletteRegistry.addTexturePallette("big_tex_test4", ConfigXM.BLOCKS.showHiddenTextures ? "bigtex" : "noise_moderate",
            new TexturePaletteSpec(BIGTEX_TEST1).withScale(TextureScale.LARGE));
    public static final ITexturePalette BIGTEX_TEST5 = TexturePaletteRegistry.addTexturePallette("big_tex_test5", ConfigXM.BLOCKS.showHiddenTextures ? "bigtex" : "noise_moderate",
            new TexturePaletteSpec(BIGTEX_TEST1).withScale(TextureScale.GIANT));

    public static final ITexturePalette TEST = TexturePaletteRegistry.addTexturePallette("test", ConfigXM.BLOCKS.showHiddenTextures ? "test" : "noise_moderate_0_0", 
            new TexturePaletteSpec().withVersionCount(1).withScale(TextureScale.SINGLE).withLayout(TextureLayout.BIGTEX)
            .withRotation(FIXED.with(ROTATE_NONE)).withRenderIntent(TextureRenderIntent.BASE_ONLY).withGroups(TextureGroup.HIDDEN_TILES));
    public static final ITexturePalette TEST_ZOOM = TexturePaletteRegistry.addZoomedPallete(TEST);
    
    public static final ITexturePalette TEST_90 = TexturePaletteRegistry.addTexturePallette("test_90", ConfigXM.BLOCKS.showHiddenTextures ? "test" : "noise_moderate_0_0", 
            new TexturePaletteSpec(TEST).withRotation(FIXED.with(ROTATE_90)));
    public static final ITexturePalette TEST_90_ZOOM = TexturePaletteRegistry.addZoomedPallete(TEST_90);
    
    public static final ITexturePalette TEST_180 = TexturePaletteRegistry.addTexturePallette("test_180", ConfigXM.BLOCKS.showHiddenTextures ? "test" : "noise_moderate_0_0", 
            new TexturePaletteSpec(TEST).withRotation(FIXED.with(ROTATE_180)));
    public static final ITexturePalette TEST_180_ZOOM = TexturePaletteRegistry.addZoomedPallete(TEST_180);
    
    public static final ITexturePalette TEST_270 = TexturePaletteRegistry.addTexturePallette("test_270", ConfigXM.BLOCKS.showHiddenTextures ? "test" : "noise_moderate_0_0", 
            new TexturePaletteSpec(TEST).withRotation(FIXED.with(ROTATE_270)));
    public static final ITexturePalette TEST_270_ZOOM = TexturePaletteRegistry.addZoomedPallete(TEST_270);
    
    public static final ITexturePalette TEST_4X4 = TexturePaletteRegistry.addTexturePallette("test4x4", ConfigXM.BLOCKS.showHiddenTextures ? "test4x4" : "noise_moderate_0_0", 
            new TexturePaletteSpec().withVersionCount(1).withScale(TextureScale.SMALL).withLayout(TextureLayout.BIGTEX)
            .withRotation(FIXED.with(ROTATE_NONE)).withRenderIntent(TextureRenderIntent.BASE_ONLY).withGroups(TextureGroup.HIDDEN_TILES));
    
    public static final ITexturePalette TEST_4x4_90 = TexturePaletteRegistry.addTexturePallette("test4x4_90", ConfigXM.BLOCKS.showHiddenTextures ? "test4x4" : "noise_moderate_0_0", 
            new TexturePaletteSpec(TEST_4X4).withRotation(FIXED.with(ROTATE_90)));
    public static final ITexturePalette TEST_4x4_180 = TexturePaletteRegistry.addTexturePallette("test4x4_180", ConfigXM.BLOCKS.showHiddenTextures ? "test4x4" : "noise_moderate_0_0", 
            new TexturePaletteSpec(TEST_4X4).withRotation(FIXED.with(ROTATE_180)));
    public static final ITexturePalette TEST_4x4_270 = TexturePaletteRegistry.addTexturePallette("test4x4_270", ConfigXM.BLOCKS.showHiddenTextures ? "test4x4" : "noise_moderate_0_0", 
            new TexturePaletteSpec(TEST_4X4).withRotation(FIXED.with(ROTATE_270)));
}