package grondag.exotic_matter.model.render;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.varia.SimpleUnorderedArrayList;
import net.minecraft.client.renderer.OpenGlHelper;

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
        private int progID = 0;
        public final @Nullable String fragmentFileName;
        public final @Nullable String vertexFileName;
        private final SimpleUnorderedArrayList<Uniform> uniforms = new SimpleUnorderedArrayList<>();
        protected boolean isDirty = false;
        
        public abstract class Uniform
        {
            private final String name;
            protected int unifID = -1;
            protected final @Nullable Consumer<Uniform> initializer;
            
            protected Uniform(String name, @Nullable Consumer<Uniform> initializer)
            {
                this.name = name;
                this.initializer = initializer;
            }
            
            private void load(int programID)
            {
                this.unifID = OpenGlHelper.glGetUniformLocation(programID, name);
                if(this.unifID == -1)
                    ExoticMatter.INSTANCE.warn("Unable to find uniform %s in shader(s) %s, %s", this.name, Shader.this.vertexFileName, Shader.this.fragmentFileName);
                else if(this.initializer != null)
                    this.initializer.accept(this);
            }
            
            protected abstract void upload();
        }
        
        protected abstract class UniformFloat extends Uniform
        {
            protected final FloatBuffer uniformFloatBuffer;
            
            protected UniformFloat(String name, @Nullable Consumer<Uniform> initializer, int size)
            {
                super(name, initializer);
                this.uniformFloatBuffer = BufferUtils.createFloatBuffer(size);
            }
        }
        
        public class Uniform1f extends UniformFloat
        {
            protected Uniform1f(String name, @Nullable Consumer<Uniform> initializer)
            {
                super(name, initializer, 1);
            }

            public void set(float value)
            {
                if(this.unifID == -1) return;
                this.uniformFloatBuffer.position(0);
                this.uniformFloatBuffer.put(0, value);
                isDirty = true;
            }
            
            @Override
            protected void upload()
            {
                OpenGlHelper.glUniform1(this.unifID, this.uniformFloatBuffer);
            }
        }
        
        public class Uniform2f extends UniformFloat
        {
            protected Uniform2f(String name, @Nullable Consumer<Uniform> initializer)
            {
                super(name, initializer, 2);
            }

            public void set(float v0, float v1)
            {
                if(this.unifID == -1) return;
                this.uniformFloatBuffer.position(0);
                this.uniformFloatBuffer.put(0, v0);
                this.uniformFloatBuffer.put(1, v1);
                isDirty = true;
            }
            
            @Override
            protected void upload()
            {
                OpenGlHelper.glUniform2(this.unifID, this.uniformFloatBuffer);
            }
        }
        
        public class Uniform3f extends UniformFloat
        {
            protected Uniform3f(String name, @Nullable Consumer<Uniform> initializer)
            {
                super(name, initializer, 3);
            }

            public void set(float v0, float v1, float v2)
            {
                if(this.unifID == -1) return;
                this.uniformFloatBuffer.position(0);
                this.uniformFloatBuffer.put(0, v0);
                this.uniformFloatBuffer.put(1, v1);
                this.uniformFloatBuffer.put(2, v2);
                isDirty = true;
            }
            
            @Override
            protected void upload()
            {
                OpenGlHelper.glUniform3(this.unifID, this.uniformFloatBuffer);
            }
        }
        
        public class Uniform4f extends UniformFloat
        {
            protected Uniform4f(String name, @Nullable Consumer<Uniform> initializer)
            {
                super(name, initializer, 4);
            }

            public void set(float v0, float v1, float v2, float v3)
            {
                if(this.unifID == -1) return;
                this.uniformFloatBuffer.position(0);
                this.uniformFloatBuffer.put(v0);
                this.uniformFloatBuffer.put(v1);
                this.uniformFloatBuffer.put(v2);
                this.uniformFloatBuffer.put(v3);
                this.uniformFloatBuffer.flip();
                isDirty = true;                
            }
            
            @Override
            protected void upload()
            {
                OpenGlHelper.glUniform4(this.unifID, this.uniformFloatBuffer);
            }
        }
        
        public Uniform1f uniform1f(String name, @Nullable Consumer<Uniform> initializer)
        {
            Uniform1f result = new Uniform1f(name, initializer);
            this.uniforms.add(result);
            return result;
        }
        
        public Uniform2f uniform2f(String name, @Nullable Consumer<Uniform> initializer)
        {
            Uniform2f result = new Uniform2f(name, initializer);
            this.uniforms.add(result);
            return result;
        }
        
        public Uniform3f uniform3f(String name, @Nullable Consumer<Uniform> initializer)
        {
            Uniform3f result = new Uniform3f(name, initializer);
            this.uniforms.add(result);
            return result;
        }
        
        public Uniform4f uniform4f(String name, @Nullable Consumer<Uniform> initializer)
        {
            Uniform4f result = new Uniform4f(name, initializer);
            this.uniforms.add(result);
            return result;
        }
        
        protected abstract class UniformInt extends Uniform
        {
            protected final IntBuffer uniformIntBuffer;
            
            protected UniformInt(String name, @Nullable Consumer<Uniform> initializer, int size)
            {
                super(name, initializer);
                this.uniformIntBuffer = BufferUtils.createIntBuffer(size);
            }
        }
        
        public class Uniform1i extends UniformInt
        {
            protected Uniform1i(String name, @Nullable Consumer<Uniform> initializer)
            {
                super(name, initializer, 1);
            }

            public void set(int value)
            {
                if(this.unifID == -1) return;
                this.uniformIntBuffer.position(0);
                this.uniformIntBuffer.put(0, value);
                isDirty = true;
            }
            
            @Override
            protected void upload()
            {
                OpenGlHelper.glUniform1(this.unifID, this.uniformIntBuffer);
            }
        }
        
        public class Uniform2i extends UniformInt
        {
            protected Uniform2i(String name, @Nullable Consumer<Uniform> initializer)
            {
                super(name, initializer, 2);
            }

            public void set(int v0, int v1)
            {
                if(this.unifID == -1) return;
                this.uniformIntBuffer.position(0);
                this.uniformIntBuffer.put(0, v0);
                this.uniformIntBuffer.put(1, v1);
                isDirty = true;
            }
            
            @Override
            protected void upload()
            {
                OpenGlHelper.glUniform2(this.unifID, this.uniformIntBuffer);
            }
        }
        
        public class Uniform3i extends UniformInt
        {
            protected Uniform3i(String name, @Nullable Consumer<Uniform> initializer)
            {
                super(name, initializer, 3);
            }

            public void set(int v0, int v1, int v2)
            {
                if(this.unifID == -1) return;
                this.uniformIntBuffer.position(0);
                this.uniformIntBuffer.put(0, v0);
                this.uniformIntBuffer.put(1, v1);
                this.uniformIntBuffer.put(2, v2);
                isDirty = true;
            }
            
            @Override
            protected void upload()
            {
                OpenGlHelper.glUniform3(this.unifID, this.uniformIntBuffer);
            }
        }
        
        public class Uniform4i extends UniformInt
        {
            protected Uniform4i(String name, @Nullable Consumer<Uniform> initializer)
            {
                super(name, initializer, 4);
            }

            public void set(int v0, int v1, int v2, int v3)
            {
                if(this.unifID == -1) return;
                this.uniformIntBuffer.position(0);
                this.uniformIntBuffer.put(v0);
                this.uniformIntBuffer.put(v1);
                this.uniformIntBuffer.put(v2);
                this.uniformIntBuffer.put(v3);
                this.uniformIntBuffer.flip();
                isDirty = true;                
            }
            
            @Override
            protected void upload()
            {
                OpenGlHelper.glUniform4(this.unifID, this.uniformIntBuffer);
            }
        }
        
        public Uniform1i uniform1i(String name, @Nullable Consumer<Uniform> initializer)
        {
            Uniform1i result = new Uniform1i(name, initializer);
            this.uniforms.add(result);
            return result;
        }
        
        public Uniform2i uniform2i(String name, @Nullable Consumer<Uniform> initializer)
        {
            Uniform2i result = new Uniform2i(name, initializer);
            this.uniforms.add(result);
            return result;
        }
        
        public Uniform3i uniform3i(String name, @Nullable Consumer<Uniform> initializer)
        {
            Uniform3i result = new Uniform3i(name, initializer);
            this.uniforms.add(result);
            return result;
        }
        
        public Uniform4i uniform4i(String name, @Nullable Consumer<Uniform> initializer)
        {
            Uniform4i result = new Uniform4i(name, initializer);
            this.uniforms.add(result);
            return result;
        }
        private Shader(@Nullable String vertexFileName, @Nullable String fragmentFileName)
        {
            this.fragmentFileName = fragmentFileName;
            this.vertexFileName = vertexFileName;
        }
        
        private void load()
        {
            if(progID != 0)
                this.release();
            
            this.progID = createProgram(this.vertexFileName, this.fragmentFileName);
            if(progID != 0)
                this.uniforms.forEach(u -> u.load(progID));
            
            isDirty = true;
        }
        
        private void release()
        {
            if(progID == 0) return;
            OpenGlHelper.glDeleteProgram(progID);
            progID = 0;
        }
        
        public void activate()
        {
            int magic = 0;
            if(tick != magic)
            {
                tick = magic;
                this.load();
            }
            
            if(progID != 0)
                OpenGlHelper.glUseProgram(progID);
            
            if(this.isDirty)
            {
                this.uniforms.forEach(u -> {if(u.unifID != -1) u.upload();});
                this.isDirty = false;
            }
        }

        public void deactivate()
        {
            OpenGlHelper.glUseProgram(0);
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
            vertId = createShader(vert, OpenGlHelper.GL_VERTEX_SHADER);
        if(frag != null)
            fragId = createShader(frag, OpenGlHelper.GL_FRAGMENT_SHADER);

        programID = OpenGlHelper.glCreateProgram();
        if(programID == 0)
            return 0;

        if(vert != null)
            OpenGlHelper.glAttachShader(programID, vertId);
        if(frag != null)
            OpenGlHelper.glAttachShader(programID, fragId);

        OpenGlHelper.glLinkProgram(programID);
        if(OpenGlHelper.glGetProgrami(programID, OpenGlHelper.GL_LINK_STATUS) == GL11.GL_FALSE)
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
            shader = OpenGlHelper.glCreateShader(shaderType);

            if(shader == 0)
                return 0;

            @Nullable ByteBuffer source = readFileAsString(filename);
            
            if(source == null)
                return 0;
            
            OpenGlHelper.glShaderSource(shader, source);
            OpenGlHelper.glCompileShader(shader);

            if (OpenGlHelper.glGetProgrami(shader, OpenGlHelper.GL_COMPILE_STATUS) == GL11.GL_FALSE)
                throw new RuntimeException("Error creating shader: " + getLogInfo(shader));

            return shader;
        }
        catch(Exception e)
        {
            OpenGlHelper.glDeleteShader(shader);
            ExoticMatter.INSTANCE.error("Unable to create shader", e);
            return -1;
        }
    }

    private static String getLogInfo(int obj)
    {
        return OpenGlHelper.glGetProgramInfoLog(obj, OpenGlHelper.glGetProgrami(obj, GL20.GL_INFO_LOG_LENGTH));
    }

    private static @Nullable ByteBuffer readFileAsString(String filename) throws Exception
    {
        InputStream in = Shaders.class.getResourceAsStream(filename);

        if(in == null)
            return null;

        byte[] abyte = IOUtils.toByteArray(new BufferedInputStream(in));
        ByteBuffer bytebuffer = BufferUtils.createByteBuffer(abyte.length);
        bytebuffer.put(abyte);
        bytebuffer.position(0);
        return bytebuffer;
    }
}
