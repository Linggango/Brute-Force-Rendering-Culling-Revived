package misanthropy.brute_force_culling_revived.instanced;

import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;

public class EntityCullingInstanceRenderer extends InstanceVertexRenderer {
    private static final float[] QUAD_VERTICES = {
            -1f, -1f,
            1f, -1f,
            1f,  1f,
            -1f,  1f
    };

    public EntityCullingInstanceRenderer() {
        super(VertexFormat.Mode.QUADS, new PixelVertexBuffer(0), EntityCullingInstanceRenderer::init, new EntityUpdateVertex(1));
    }

    private static void init(@NotNull FloatBuffer buffer) {
        buffer.put(QUAD_VERTICES);
        buffer.flip();
        GL15.glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
    }
}