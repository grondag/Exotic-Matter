package grondag.exotic_matter.model;

import static grondag.exotic_matter.model.TextureLayout.BORDER_13;
import static grondag.exotic_matter.model.TextureLayout.MASONRY_5;
import static grondag.exotic_matter.model.TextureRotationType.FIXED;
import static grondag.exotic_matter.model.TextureRotationType.RANDOM;
import static grondag.exotic_matter.world.Rotation.ROTATE_NONE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.ConfigXM;
import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.IGrondagMod;
import grondag.exotic_matter.init.SubstanceConfig;
import grondag.exotic_matter.model.TextureRotationType.TextureRotationSetting;
import grondag.exotic_matter.render.EnhancedSprite;
import grondag.exotic_matter.varia.NullHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SuppressWarnings("deprecation")
public class TexturePaletteRegistry implements Iterable<ITexturePalette>
{
    /**
     * Max number of texture palettes that can be registered, loaded and represented in model state.
     */
    public static final int MAX_PALETTES = 4096;
    
    private static int nextOrdinal = 0;
    private static final HashMap<String, ITexturePalette> allByName = new HashMap<>();
    private static final ArrayList<ITexturePalette> allByOrdinal = new ArrayList<ITexturePalette>();
    private static final List<ITexturePalette> allReadOnly = Collections.unmodifiableList(allByOrdinal);
    
    /**
     * Important that we have at least one texture and should have ordinal zero so that it is the default value returned by modelState.
     * Is not meant for user selection. For CUT paint layer means should use same texture as base layer.
     * For DETAIL and OVERLAY layers, indicates those layers are disabled. 
     */
     public static final ITexturePalette NONE = addTexturePallette("none", "noise_moderate", 
            new TexturePaletteSpec(ExoticMatter.INSTANCE).withVersionCount(4).withScale(TextureScale.SINGLE).withLayout(TextureLayout.SPLIT_X_8)
            .withRotation(RANDOM.with(ROTATE_NONE)).withRenderIntent(TextureRenderIntent.BASE_ONLY).withGroups(TextureGroup.ALWAYS_HIDDEN));
    
    
    public static List<ITexturePalette> all()
    {
        return allReadOnly;
    }

    public static ITexturePalette get(String systemName)
    {
        return NullHandler.defaultIfNull(allByName.get(systemName), NONE);
    }
    
    public static ITexturePalette get(int ordinal)
    {
        return ordinal < 0 || ordinal >= allByOrdinal.size() ? NONE : NullHandler.defaultIfNull(allByOrdinal.get(ordinal), NONE);
    }
    
    public static ITexturePalette addTexturePallette(String systemName, String textureBaseName, TexturePaletteSpec info)
    {
        ITexturePalette result = new TexturePallette(systemName, textureBaseName, info);
        addToCollections(result);
        return result;
    }
    
    public static ITexturePalette addZoomedPallete(ITexturePalette source)
    {
        ITexturePalette result = new TexturePallette(
                source.systemName() + ".zoom", 
                source.textureBaseName(),
                new TexturePaletteSpec(source)
                    .withZoomLevel(source.zoomLevel() + 1)
                    .withScale(source.textureScale().zoom()));
        addToCollections(result);
        return result;
    }

    private static void addToCollections(ITexturePalette palette)
    {
        allByOrdinal.add(palette);
        allByName.put(palette.systemName(), palette);
    }
    public int size() { return allByOrdinal.size(); }

    public boolean isEmpty() { return allByOrdinal.isEmpty(); }

    public boolean contains(Object o) { return allByOrdinal.contains(o); }
   
    @Override
    public Iterator<ITexturePalette> iterator() { return allByOrdinal.iterator(); }
   
    public static List<ITexturePalette> getTexturesForSubstanceAndPaintLayer(SubstanceConfig substance, PaintLayer layer)
    {
        int searchFlags = 0;
        switch(layer)
        {
        case BASE:
        case CUT:
        case LAMP:
            searchFlags = TextureGroup.STATIC_TILES.bitFlag | TextureGroup.DYNAMIC_TILES.bitFlag;
            if(ConfigXM.BLOCKS.showHiddenTextures) searchFlags |= TextureGroup.HIDDEN_TILES.bitFlag;
            break;
    
        case MIDDLE:
        case OUTER:
            searchFlags = TextureGroup.STATIC_DETAILS.bitFlag | TextureGroup.DYNAMIC_DETAILS.bitFlag
                         | TextureGroup.STATIC_BORDERS.bitFlag | TextureGroup.DYNAMIC_BORDERS.bitFlag;
            
            if(ConfigXM.BLOCKS.showHiddenTextures) 
                    searchFlags |= (TextureGroup.HIDDEN_DETAILS.bitFlag | TextureGroup.HIDDEN_BORDERS.bitFlag);
            
            break;
            
        default:
            break;
        
        }
        
        ImmutableList.Builder<ITexturePalette> builder = ImmutableList.builder();
        for(ITexturePalette t : allByOrdinal)
        {
            if((t.textureGroupFlags() & searchFlags) != 0)
            {
                builder.add(t);
            }
        }
        
        return builder.build();
    }

    public static ITexturePalette addBorderRandom(IGrondagMod mod, String textureName, boolean allowTile, boolean renderNoBorderAsTile)
    {
        return addTexturePallette(textureName, textureName, 
                new TexturePaletteSpec(mod).withVersionCount(4).withScale(TextureScale.SINGLE).withLayout(TextureLayout.BORDER_13)
                .withRotation(FIXED.with(ROTATE_NONE))
                .withRenderIntent(allowTile ? TextureRenderIntent.BASE_OR_OVERLAY_NO_CUTOUT : TextureRenderIntent.OVERLAY_ONLY)
                .withGroups( allowTile ? TextureGroup.STATIC_BORDERS : TextureGroup.STATIC_TILES, TextureGroup.STATIC_BORDERS)
                .withRenderNoBorderAsTile(renderNoBorderAsTile));
    }

    public static ITexturePalette addBorderSingle(IGrondagMod mod, String textureName)
    {
        return addTexturePallette(textureName, textureName, 
                new TexturePaletteSpec(mod).withVersionCount(1).withScale(TextureScale.SINGLE).withLayout(TextureLayout.BORDER_13)
                .withRotation(FIXED.with(ROTATE_NONE)).withRenderIntent(TextureRenderIntent.OVERLAY_ONLY).withGroups(TextureGroup.STATIC_BORDERS));
    }

    private static class TexturePallette implements ITexturePalette
    {
        private final IGrondagMod mod;
        
        private final String systemName;
        
        private final String textureBaseName;
        
        /** number of texture versions must be a power of 2 */
        final int textureVersionCount;
        
        final TextureScale textureScale;
        private final TextureLayout textureLayout;
        
        /** 
         * Used to display appropriate label for texture.
         * 0 = no zoom, 1 = 2x zoom, 2 = 4x zoom
         */
        final int zoomLevel;
        
        /**
         * Masks the version number provided by consumers - alternators that
         * drive number generation may support larger number of values. 
         * Implies number of texture versions must be a power of 2 
         */
        private final int textureVersionMask;
        
        /** Governs default rendering rotation for texture and what rotations are allowed. */
        final TextureRotationSetting rotation;
        
        /** 
         * Determines layer that should be used for rendering this texture.
         */
        final TextureRenderIntent renderIntent;
        
        /**
         * Globally unique id
         */
        private final int ordinal;
        
        /**
         * Used by modelstate to know which world state must be retrieved to drive this texture
         * (rotation and block version)
         */
        private final int stateFlags;
        
        final int textureGroupFlags;
        
        /**
         * Number of ticks each frame should be rendered on the screen
         * before progressing to the next frame.
         */
        final int ticksPerFrame;

        /** for border-layout textures, controls if "no border" texture is rendered */
        private final boolean renderNoBorderAsTile;

        protected TexturePallette(String systemName, String textureBaseName, TexturePaletteSpec info)
        {
            this.mod = info.mod;
            this.ordinal = nextOrdinal++;
            this.systemName = systemName;
            this.textureBaseName = textureBaseName;
            this.textureVersionCount = info.textureVersionCount;
            this.textureVersionMask = Math.max(0, info.textureVersionCount - 1);
            this.textureScale = info.textureScale;
            this.textureLayout = info.layout;
            this.rotation = info.rotation;
            this.renderIntent = info.renderIntent;
            this.textureGroupFlags = info.textureGroupFlags;
            this.zoomLevel = info.zoomLevel;
            this.ticksPerFrame = info.ticksPerFrame;
            this.renderNoBorderAsTile = info.renderNoBorderAsTile;
  
            int flags = this.textureScale.modelStateFlag | this.textureLayout.modelStateFlag;
            
            // textures with randomization options also require position information
            
            if(info.rotation.rotationType() == TextureRotationType.RANDOM)
            {
                flags |= (ModelStateData.STATE_FLAG_NEEDS_TEXTURE_ROTATION | ModelStateData.STATE_FLAG_NEEDS_POS);
            }
            
            if(info.textureVersionCount > 1)
            {
                flags |= ModelStateData.STATE_FLAG_NEEDS_POS;
            }
            this.stateFlags =  flags;
                    
        }
        
        @Override
        public List<String> getTexturesForPrestich()
        {
            
            ArrayList<String> textureList = new ArrayList<String>();
            
            switch(this.textureLayout)
            {
            case BIGTEX:
            case BIGTEX_ANIMATED:
                for (int i = 0; i < this.textureVersionCount; i++)
                {
                    textureList.add(buildTextureNameBigTex());
                }
                break;
                
            case BORDER_13:
            {
                // last texture (no border) only needed if indicated
                int texCount = this.renderNoBorderAsTile 
                        ? BORDER_13.textureCount
                        : BORDER_13.textureCount -1;
                
                for(int i = 0; i < this.textureVersionCount; i++)
                {
                    for(int j = 0; j < texCount; j++)
                    {
                        textureList.add(buildTextureName_X_8(i * BORDER_13.blockTextureCount + j));
                    }
                }
                break;
            }
            case MASONRY_5:
                for(int i = 0; i < this.textureVersionCount; i++)
                {
                    for(int j = 0; j < MASONRY_5.textureCount; j++)
                    {
                        textureList.add(buildTextureName_X_8(i * MASONRY_5.blockTextureCount + j));
                    }
                }
                
            case SPLIT_X_8:
            default:
                for (int i = 0; i < this.textureVersionCount; i++)
                {
                    textureList.add(buildTextureName_X_8(i));
                }
                break;
            }
            
            return textureList;
        }
        
        private String buildTextureName_X_8(int offset)
        {
            return this.mod.modID() + ":blocks/" + textureBaseName + "_" + (offset >> 3) + "_" + (offset & 7);
        }

        private String buildTextureNameBigTex()
        {
            return this.mod.modID() + ":blocks/" + textureBaseName;
        }
        
        @Override
        public String getSampleTextureName() 
        { 
            switch(textureLayout)
            {
            case BIGTEX:
            case BIGTEX_ANIMATED:
                return buildTextureNameBigTex();
            case SPLIT_X_8:
            case MASONRY_5:    
            default:
                return buildTextureName_X_8(0);
                
            case BORDER_13:
                return buildTextureName_X_8(4);
            }
        }
        
        /**
         * See {@link #getSampleSprite()}
         */
        @SideOnly(Side.CLIENT)
        @Nullable
        private EnhancedSprite sampleSprite;
        
        @Override
        @SideOnly(Side.CLIENT)
        public EnhancedSprite getSampleSprite()
        {
            EnhancedSprite result = sampleSprite;
            if(result == null)
            {
                result = (EnhancedSprite)Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(this.getSampleTextureName());
                sampleSprite = result;
            }
            return result;
        }
        
        @Override
        public String getTextureName(int version)
        {
            return buildTextureName(version & this.textureVersionMask);
        }
        
        private String buildTextureName(int version)
        {
            return (this.textureLayout == TextureLayout.BIGTEX || this.textureLayout == TextureLayout.BIGTEX_ANIMATED)
                    ? buildTextureNameBigTex()
                    : buildTextureName_X_8(version);
        }
        
        /**
         * @see grondag.hard_science.superblock.texture.ITexturePallette#getTextureName(int, int)
         */
        @Override
        public String getTextureName(int version, int index)
        {
            return buildTextureName(version & this.textureVersionMask, index);
        }
        
        private String buildTextureName(int version, int index)
        {
            switch(textureLayout)
            {
            case MASONRY_5:
                return buildTextureName_X_8(version * MASONRY_5.blockTextureCount + index);
                
            case BORDER_13:
                return buildTextureName_X_8(version * BORDER_13.blockTextureCount + index);
                
            default:
                return buildTextureName_X_8(index);
            }
        }
        
        @Override
        public String displayName()
        {
            // trim off the .zoom suffixes to get the localization string
            final String token = "texture." + this.systemName.replaceAll(".zoom", "");
            final String texName = I18n.translateToLocal(token.toLowerCase());
                    
            switch(this.zoomLevel)
            {
                case 1:
                    return I18n.translateToLocalFormatted("texture.zoom2x_format", texName);
                case 2:
                    return I18n.translateToLocalFormatted("texture.zoom4x_format", texName);
                default:
                    return texName;
            }
        }
        
        @Override
        public String textureBaseName() { return this.textureBaseName; }
        
        /** number of texture versions must be a power of 2 */
        @Override
        public int textureVersionCount() { return this.textureVersionCount; }
        
        @Override
        public TextureScale textureScale() { return this.textureScale; }
        @Override
        public TextureLayout textureLayout() { return this.textureLayout; }
        
        /** 
         * Used to display appropriate label for texture.
         * 0 = no zoom, 1 = 2x zoom, 2 = 4x zoom
         */
        @Override
        public int zoomLevel() { return this.zoomLevel; }
        
        /**
         * Masks the version number provided by consumers - alternators that
         * drive number generation may support larger number of values. 
         * Implies number of texture versions must be a power of 2 
         */
        @Override
        public int textureVersionMask() { return this.textureVersionMask; }
        
        /** Governs default rendering rotation for texture and what rotations are allowed. */
        @Override
        public TextureRotationSetting rotation() { return this.rotation; }
        
        /** 
         * Determines layer that should be used for rendering this texture.
         */
        @Override
        public TextureRenderIntent renderIntent() { return this.renderIntent; }
        
        /**
         * Globally unique id
         */
        @Override
        public int ordinal() { return this.ordinal; }
        
        /**
         * Used by modelstate to know which world state must be retrieved to drive this texture
         * (rotation and block version)
         */
        @Override
        public int stateFlags() { return this.stateFlags; }
        
        @Override
        public int textureGroupFlags() { return this.textureGroupFlags; }
        
        /**
         * Number of ticks each frame should be rendered on the screen
         * before progressing to the next frame.
         */
        @Override
        public int ticksPerFrame() { return this.ticksPerFrame; }

        /** for border-layout textures, controls if "no border" texture is rendered */
        @Override
        public boolean renderNoBorderAsTile() { return this.renderNoBorderAsTile; }

        @Override
        public String systemName()
        {
            return this.systemName;
        }

        @Override
        public IGrondagMod mod()
        {
            return this.mod;
        }
        
        
    }
}
