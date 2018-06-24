package grondag.exotic_matter.model.render;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

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
        // TODO: remove
        private int tick = 0;
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
            GL20.glDeleteProgram(glID);
            glID = 0;
        }
        
        public void activate()
        {
            int magic = 0;
            if(tick != magic)
            {
                tick = magic;
                this.load();
            }
            
            if(glID != 0)
                GL20.glUseProgram(glID);
        }

        public void deactivate()
        {
            GL20.glUseProgram(0);
        }
        
        // TODO: if going to call these frequently may need to retain location reference
        public void setUniform(String name, int value)
        {
            if(glID == 0) return;
            final int uid = GL20.glGetUniformLocation(glID, name);
            if(uid != -1)
                GL20.glUniform1i(uid, value);
        }
      
        
     // TODO: if going to call these frequently may need to retain location reference
        public void setUniform(String name, float value)
        {
            if(glID == 0) return;
            final int uid =  GL20.glGetUniformLocation(glID, name);
            if(uid != -1)
                GL20.glUniform1f(uid, value);
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
            vertId = createShader(vert, GL20.GL_VERTEX_SHADER);
        if(frag != null)
            fragId = createShader(frag, GL20.GL_FRAGMENT_SHADER);

        programID = GL20.glCreateProgram();
        if(programID == 0)
            return 0;

        if(vert != null)
            GL20.glAttachShader(programID, vertId);
        if(frag != null)
            GL20.glAttachShader(programID, fragId);

        GL20.glLinkProgram(programID);
        if(GL20.glGetProgrami(programID, GL20.GL_LINK_STATUS) == GL11.GL_FALSE)
        {
            ExoticMatter.INSTANCE.error(getLogInfo(programID));
            return 0;
        }

        GL20.glValidateProgram(programID);
        if (GL20.glGetProgrami(programID, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE)
        {
            ExoticMatter.INSTANCE.error(getLogInfo(programID));
            return 0;
        }

        return programID;
    }

    // TODO: use OpenGL helper to use appropriate methods
    private static int createShader(String filename, int shaderType)
    {
        int shader = 0;
        try
        {
            shader = GL20.glCreateShader(shaderType);

            if(shader == 0)
                return 0;

            GL20.glShaderSource(shader, readFileAsString(filename));
            GL20.glCompileShader(shader);

            if (GL20.glGetProgrami(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
                throw new RuntimeException("Error creating shader: " + getLogInfo(shader));

            return shader;
        }
        catch(Exception e)
        {
            GL20.glDeleteShader(shader);
            ExoticMatter.INSTANCE.error("Unable to create shader", e);
            return -1;
        }
    }

    private static String getLogInfo(int obj)
    {
        return GL20.glGetProgramInfoLog(obj, GL20.glGetProgrami(obj, GL20.GL_INFO_LOG_LENGTH));
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
