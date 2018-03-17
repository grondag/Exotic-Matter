package grondag.exotic_matter.render;


import javax.annotation.Nonnull;

import grondag.exotic_matter.varia.SimpleUnorderedArraySet;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.obj.OBJLoader;

public class HSObjModelLoader implements ICustomModelLoader
{

    public static final HSObjModelLoader INSTANCE = new HSObjModelLoader();

    private final SimpleUnorderedArraySet<String> includes = new SimpleUnorderedArraySet<>();
    
    public void enable(String modID)
    {
        includes.putIfNotPresent(modID);
    }
        
    @Override
    public void onResourceManagerReload(@Nonnull IResourceManager resourceManager)
    {
       // relies on OBJLoader for everything so nothing to do
    }

    @Override
    public boolean accepts(@Nonnull ResourceLocation modelLocation)
    {
        return modelLocation.getResourcePath().endsWith(".obj")
                && includes.contains(modelLocation.getResourceDomain()); 
    }

    @Override
    public @Nonnull IModel loadModel(@Nonnull ResourceLocation modelLocation) throws Exception
    {
        return new HSOBJModelWrapper(OBJLoader.INSTANCE.loadModel(modelLocation));
    }
    
}
