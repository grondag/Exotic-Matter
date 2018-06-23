package grondag.exotic_matter.model.render;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;

import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.varia.SimpleUnorderedArrayList;

/**
 * This code is loosely based on code from Botania, by Vaskii
 * http://botaniamod.net/
 */
public final class Shaders
{
    public static final class Shader
    {
        private int glID = 0;
        public final @Nullable String fragmentFileName;
        public final @Nullable String vertexFileName;
        
        private Shader(@Nullable String vertexFileName, @Nullable String fragmentFileName)
        {
            this.fragmentFileName = fragmentFileName;
            this.vertexFileName = vertexFileName;
        }
        
        private void load()
        {
            if(glID != 0)
                this.release();
            
            this.glID = createProgram(this.vertexFileName, this.fragmentFileName);
        }
        private void release()
        {
            if(glID == 0) return;
            ARBShaderObjects.glDeleteObjectARB(glID);
            glID = 0;
        }
        
        public void activate()
        {
            if(glID != 0)
                ARBShaderObjects.glUseProgramObjectARB(glID);
        }

        public void deactivate()
        {
            ARBShaderObjects.glUseProgramObjectARB(0);
        }
        
        // TODO: if going to call these frequently may need to retain location reference
        public void setUniform(String name, int value)
        {
            if(glID == 0) return;
            int uid = ARBShaderObjects.glGetUniformLocationARB(glID, name);
            ARBShaderObjects.glUniform1iARB(uid, value);
        }
        
     // TODO: if going to call these frequently may need to retain location reference
        public void setUniform(String name, float value)
        {
            if(glID == 0) return;
            int uid = ARBShaderObjects.glGetUniformLocationARB(glID, name);
            ARBShaderObjects.glUniform1fARB(uid, value);
        }
    }
    
    private static final SimpleUnorderedArrayList<Shader> shaders = new SimpleUnorderedArrayList<Shader>();
            
    public static Shader register(String modID, @Nullable String vertexFileName, @Nullable String fragmentFileName)
    {
        String prefix = "/assets/" + modID + "/shader/";
        Shader result = new Shader(prefix + vertexFileName, prefix +fragmentFileName);
        shaders.add(result);
        return result;
    }

    public static void loadShaders()
    {
        shaders.forEach(s -> {s.load();});
    }

    // Most of the code taken from the LWJGL wiki
    // http://lwjgl.org/wiki/index.php?title=GLSL_Shaders_with_LWJGL
    private static int createProgram(@Nullable String vert, @Nullable String frag)
    {
        int vertId = 0, fragId = 0, programID;
        if(vert != null)
            vertId = createShader(vert, ARBVertexShader.GL_VERTEX_SHADER_ARB);
        if(frag != null)
            fragId = createShader(frag, ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);

        programID = ARBShaderObjects.glCreateProgramObjectARB();
        if(programID == 0)
            return 0;

        if(vert != null)
            ARBShaderObjects.glAttachObjectARB(programID, vertId);
        if(frag != null)
            ARBShaderObjects.glAttachObjectARB(programID, fragId);

        ARBShaderObjects.glLinkProgramARB(programID);
        if(ARBShaderObjects.glGetObjectParameteriARB(programID, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE)
        {
            ExoticMatter.INSTANCE.error(getLogInfo(programID));
            return 0;
        }

        ARBShaderObjects.glValidateProgramARB(programID);
        if (ARBShaderObjects.glGetObjectParameteriARB(programID, ARBShaderObjects.GL_OBJECT_VALIDATE_STATUS_ARB) == GL11.GL_FALSE)
        {
            ExoticMatter.INSTANCE.error(getLogInfo(programID));
            return 0;
        }

        return programID;
    }

    private static int createShader(String filename, int shaderType)
    {
        int shader = 0;
        try
        {
            shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType);

            if(shader == 0)
                return 0;

            ARBShaderObjects.glShaderSourceARB(shader, readFileAsString(filename));
            ARBShaderObjects.glCompileShaderARB(shader);

            if (ARBShaderObjects.glGetObjectParameteriARB(shader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE)
                throw new RuntimeException("Error creating shader: " + getLogInfo(shader));

            return shader;
        }
        catch(Exception e)
        {
            ARBShaderObjects.glDeleteObjectARB(shader);
            ExoticMatter.INSTANCE.error("Unable to create shader", e);
            return -1;
        }
    }

    private static String getLogInfo(int obj)
    {
        return ARBShaderObjects.glGetInfoLogARB(obj, ARBShaderObjects.glGetObjectParameteriARB(obj, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB));
    }

    private static String readFileAsString(String filename) throws Exception
    {
        InputStream in = Shaders.class.getResourceAsStream(filename);

        if(in == null)
            return "";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8")))
        {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
