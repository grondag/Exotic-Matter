package grondag.exotic_matter;

import java.util.ArrayList;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;

public interface IGrondagMod
{
    public String modID();
    
    /**
     * Puts mod ID and . in front of whatever is passed in
     */
    public default String prefixName(String name)
    {
        return String.format("%s.%s", this.modID(), name.toLowerCase());
    }
    
    public default String prefixResource(String name)
    {
        return String.format("%s:%s", this.modID(), name.toLowerCase());
    }
    
    public default ResourceLocation resource(String name)
    {
        return new ResourceLocation(prefixResource(name));
    }
    
    public default void addRecipe(String itemName, int index, String recipe, String... inputs)
    {
        String[] lines = new String[3];
        lines[0] = recipe.substring(0, 3);
        lines[1] = recipe.substring(3, 6);
        lines[2] = recipe.substring(6, 9);
        
        final char[] symbols = "ABCDEFGHI".toCharArray();
        int i = 0;
        ArrayList<Object> params = new ArrayList<Object>();
        
        params.add(lines);
        
        for(String s : inputs)
        {
            params.add((Character)symbols[i]);
            params.add(ForgeRegistries.ITEMS.getValue(resource(s)));
            i++;
        }
        
        GameRegistry.addShapedRecipe(
                resource(itemName + index), 
                resource(this.modID()),
                ForgeRegistries.ITEMS.getValue(resource(itemName)).getDefaultInstance(),
                params.toArray());
    }
}
