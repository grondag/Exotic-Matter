package grondag.exotic_matter.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.ExoticMatter;
import net.minecraft.util.text.translation.I18n;

public class ModelShape<T extends ShapeMeshGenerator>
{
    public static final int MAX_SHAPES = 128;
    
    private static final HashMap<String, ModelShape<?>> allByName = new HashMap<>();
    private static final ArrayList<ModelShape<?>> allByOrdinal = new ArrayList<>();
    
    private static int nextOrdinal = 0;

    private final Class<T> meshFactoryClass;
    private final boolean isAvailableInGui;
    private final MetaUsage metaUsage;
    private final String systemName;
    private final int ordinal;
    
    public static <V extends ShapeMeshGenerator> ModelShape<V> create(String systemName, Class<V> meshFactoryClass, MetaUsage metaUsage, boolean isAvailableInGui)
    {
        return new ModelShape<V>(systemName, meshFactoryClass, metaUsage, isAvailableInGui);
    }
    
    public static <V extends ShapeMeshGenerator> ModelShape<V> create(String systemName, Class<V> meshFactoryClass, MetaUsage metaUsage)
    {
        return new ModelShape<V>(systemName, meshFactoryClass, metaUsage);
    }
    
    private ModelShape(String systemName, Class<T> meshFactoryClass, MetaUsage metaUsage, boolean isAvailableInGui)
    {
        this.meshFactoryClass = meshFactoryClass;
        this.ordinal = nextOrdinal++;
        this.systemName = systemName;
        this.metaUsage = metaUsage;
        this.isAvailableInGui = isAvailableInGui;
        allByName.put(systemName, this);
        if(this.ordinal < MAX_SHAPES)
            allByOrdinal.add(this);
        else
            ExoticMatter.INSTANCE.warn("Model shape limit of %d exceeded.  Shape %s added but will not be rendered.", MAX_SHAPES, systemName);
        
    }
    
    private ModelShape(String systemName, Class<T> meshFactoryClass, MetaUsage metaUsage)
    {
        this(systemName, meshFactoryClass, metaUsage, true);
    }
    
    
    @Nullable
    public static ModelShape<?> get(String systemName)
    {
        return allByName.get(systemName);
    }
    
    public static ModelShape<?> get(int ordinal)
    {
        return allByOrdinal.get(ordinal);
    }
    
    private static List<ModelShape<?>> guiAvailableShapes = ImmutableList.of();
    
    public static List<ModelShape<?>> guiAvailableShapes()
    {
        if(guiAvailableShapes.isEmpty())
        {
            ImmutableList.Builder<ModelShape<?>> builder = ImmutableList.builder();
            for(ModelShape<?> shape : allByOrdinal)
            {
                if(shape.isAvailableInGui) builder.add(shape);
            }
            guiAvailableShapes = builder.build();
        }
        return guiAvailableShapes;
    }
    
    private boolean factoryNeedLoad = true;
    private @Nullable T factory = null;
    
    @SuppressWarnings("null")
    public T meshFactory()
    {
        if(this.factoryNeedLoad)
        {
            try
            {
                this.factory = this.meshFactoryClass.newInstance();
            }
            catch (Exception e)
            {
                ExoticMatter.INSTANCE.error("Unable to load model factory for shape " + this.systemName + " due to error.", e);
            }
            factoryNeedLoad = false;
        }
        return this.factory;
    }
    
    @SuppressWarnings("deprecation")
    public String localizedName()
    {
        return I18n.translateToLocal("shape." + this.systemName.toLowerCase());
    }

    public MetaUsage metaUsage()
    {
        return this.metaUsage;
    }

    public boolean isAvailableInGui()
    {
        return this.isAvailableInGui;
    }
    
    public int ordinal()
    {
        return this.ordinal;
    }

    public String systemName()
    {
        return this.systemName;
    }
}
