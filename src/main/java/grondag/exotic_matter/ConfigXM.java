package grondag.exotic_matter;

import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.LangKey;
import net.minecraftforge.common.config.Config.RangeDouble;
import net.minecraftforge.common.config.Config.RangeInt;
import net.minecraftforge.common.config.Config.RequiresMcRestart;
import net.minecraftforge.common.config.Config.Type;

@LangKey("config.general")
@Config(modid = ExoticMatter.MODID, type = Type.INSTANCE)
public class ConfigXM
{
    ////////////////////////////////////////////////////
    // EXECUTION
    ////////////////////////////////////////////////////
    @LangKey("config.execution")
    @Comment("General settings for game logic execution.")
    public static ExecutionSettings EXECUTION = new ExecutionSettings();
    
    public static class ExecutionSettings
    {
        
        @Comment({"Maximum number of queued 'operations' to be executed each server tick.",
        " Operations are submitted by game logic that runs outside the main server thread.",
        " The size of each operation varies, and some tasks consume more than one op. ",
        " Try smaller values if seeing tick lag on the server. Some game actions or events may take ",
        " longer to complete with small values.  Could have indirect effects on client if results in",
        " large numbers of block update, for example."})
        @RangeInt(min = 128, max = 1000000)
        public int maxQueuedWorldOperationsPerTick = 4096;
    }
    
    ////////////////////////////////////////////////////        
    // BLOCKS
    ////////////////////////////////////////////////////
    @LangKey("config.blocks")
    @Comment("Settings for blocks.")
    public static BlockSettings BLOCKS = new BlockSettings();

    public static class BlockSettings
    {
        @Comment("Allow user selection of hidden textures in SuperModel Block GUI. Generally only useful for testing.")
        public boolean showHiddenTextures = false;

        @Comment("Controls how much detail should be shown if The One Probe is enabled.")
        public ProbeInfoLevel probeInfoLevel = ProbeInfoLevel.BASIC;

        public static enum ProbeInfoLevel
        {
            BASIC,
            EXTRA,
            DEBUG
        }

        @Comment("Set true to enable tracing output for block model state.  Can spam the log quite a bit, so leave false unless having problems.")
        public boolean debugModelState = true;

        @Comment({"Maximum number of block states checked before placing virtual blocks.",
            " Try smaller values if placing large multi-block regions is causing FPS problems.",
            " With smaller values, species (connected textures) may not be selected properly ",
        " for large multi-block placements."})
        @RangeInt(min = 16, max = 4096)
        public int maxPlacementCheckCount = 512;
    }
    
    ////////////////////////////////////////////////////        
    // RENDERING
    ////////////////////////////////////////////////////
    @LangKey("config.render")
    @Comment("Settings for visual appearance.")
    
    public static Render RENDER = new Render();
    public static class Render
    {
        @Comment("Maxiumum number of quads held in cache for reuse. Higher numbers may result is less memory consuption overall, up to a point.")
        @RangeInt(min = 0xFFFF, max = 0xFFFFF)
        public int quadCacheSizeLimit = 524280;
    
        @RequiresMcRestart
        @Comment("Collect statistics on quad caching. Used for testing.")
        public boolean enableQuadCacheStatistics = false;
    
        @RequiresMcRestart
        @Comment("Enable animated textures. Set false if animation may be causing memory or performance problems.")
        public boolean enableAnimatedTextures = true;
    
        @RequiresMcRestart
        @Comment("Collect statistics on texture animation. Used for testing.")
        public boolean enableAnimationStatistics = false;
    
        @RequiresMcRestart
        @Comment({"Enable in-memroy texture compression of animated textures if your graphics card supports is.",
        "Can reduce memory usage by 1GB or more."})
        public boolean enableAnimatedTextureCompression = false;
    
        @RequiresMcRestart
        @Comment("Seconds between output of client-side performance statistics to log, if any are enabled.")
        @RangeInt(min = 10, max = 600)
        public int clientStatReportingInterval = 10;
    
        @Comment({"Shade blocks from this mod with a uniform light vector. Provides a somewhat better appearance for flowing ",
        "lava blocks (for example) but may appear odd when next to blocks from Vanilla or other mods."})
        public boolean enableCustomShading = true;
    
        @Comment({"If true, Dynamic flow block (volcanic lava and basalt) will not render faces occulded by adjacent flow blocks.",
            " True is harder on CPU and easier on your graphics card/chip.  Experiment if you have FPS problems.",
        " Probably won't matter on systems with both a fast CPU and fast graphics."})
        public boolean enableFaceCullingOnFlowBlocks = false;
    
        @Comment("Minimum lighting on any block face with custom shading. Smaller values give deeper shadows.")
        @RangeDouble(min = 0, max = 0.9)
        public float minAmbientLight =0.3F;
    
        @Comment("X component of ambient light source.")
        @RangeDouble(min = -1, max = 1)
        public float normalX = 0.0F;
    
        @Comment("Y component of ambient light source.")
        @RangeDouble(min = -1, max = 1)
        public float normalY = 1.0F;
    
        @Comment("Z component of ambient light source.")
        @RangeDouble(min = -1, max = 1)
        public float normalZ = 0.25F;
    
        @Comment("Debug Feature: draw block boundaries for non-cubic blocks.")
        public boolean debugDrawBlockBoundariesForNonCubicBlocks = false;
    
        @Comment("Rendering for blocks about to be placed.")
        public PreviewMode previewSetting = PreviewMode.OUTLINE;
    
        @Comment("Debug Feature: output generated font images that are uploaded to texture map.")
        public boolean outputFontTexturesForDebugging = false;
    
        @Comment("Debug Feature: output generated color atlas in config folder to show possible hues.")
        public boolean debugOutputColorAtlas = false;
    
        public static float normalLightFactor;
    
        public static Vec3d lightingNormal = new Vec3d(1.0, 1.0, 1.0);
    
        public static void recalcDerived()
        {
            normalLightFactor = 0.5F * (1F - RENDER.minAmbientLight);
            lightingNormal = new Vec3d(RENDER.normalX, RENDER.normalY, RENDER.normalZ).normalize();
        }
    
        public static enum PreviewMode
        {
            NONE,
            OUTLINE
        }
    }

    ////////////////////////////////////////////////////        
    // HYPERSTONE
    ////////////////////////////////////////////////////
    @LangKey("config.hypermaterial")
    @Comment("Settings for hyperdimensional building materials.")
    public static HyperStone HYPERSTONE = new HyperStone();
    
    public static class HyperStone
    {
        @Comment("If false, mobs cannot spawn on hyper-dimensional blocks in darkness; similar to slabs.")
        public boolean allowMobSpawning = false;
    
        @Comment("If false, normal fires directly above hyper-dimensional blocks are immediately extinguished.")
        public boolean allowFire = false;
    
        @Comment("If false, players cannot harvest hyper-dimensional blocks without silk touch - they can be broken but drop rubble.")
        public boolean allowHarvest = false;
    
        @Comment("If true, hyper-dimensional blocks can be harvested intact with silk touch. Only matters if allowHarvest is true.")
        public boolean allowSilkTouch = true;
    
        @Comment("If true, hyper-dimensional blocks have a chance to lose durability due to damage from entities or explosions.")
        public boolean canBeDamaged;
    }

    public static void recalcDerived()
    {
        Render.recalcDerived();
    }


    
}
