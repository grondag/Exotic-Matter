package grondag.exotic_matter.model.mesh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.model.state.MetaUsage;

public class ModelShapes
{

    public static final int MAX_SHAPES = 128;
    static final HashMap<String, ModelShape<?>> allByName = new HashMap<>();
    static final ArrayList<ModelShape<?>> allByOrdinal = new ArrayList<>();
    static List<ModelShape<?>> guiAvailableShapes = ImmutableList.of();
    
    public static <V extends ShapeMeshGenerator> ModelShape<V> create(String systemName, Class<V> meshFactoryClass, MetaUsage metaUsage, boolean isAvailableInGui)
    {
        return new ModelShape<V>(systemName, meshFactoryClass, metaUsage, isAvailableInGui);
    }
    
    public static <V extends ShapeMeshGenerator> ModelShape<V> create(String systemName, Class<V> meshFactoryClass, MetaUsage metaUsage)
    {
        return new ModelShape<V>(systemName, meshFactoryClass, metaUsage);
    }
    
    @Nullable
    public static ModelShape<?> get(String systemName)
    {
        return ModelShapes.allByName.get(systemName);
    }
    
    public static ModelShape<?> get(int ordinal)
    {
        return ModelShapes.allByOrdinal.get(ordinal);
    }
    
    public static List<ModelShape<?>> guiAvailableShapes()
    {
        if(guiAvailableShapes.isEmpty())
        {
            ImmutableList.Builder<ModelShape<?>> builder = ImmutableList.builder();
            for(ModelShape<?> shape : ModelShapes.allByOrdinal)
            {
                if(shape.isAvailableInGui()) builder.add(shape);
            }
            guiAvailableShapes = builder.build();
        }
        return guiAvailableShapes;
    }

}
