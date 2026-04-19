package misanthropy.brute_force_culling_revived.instanced;

import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;

public class EntityCullingInstanceRenderer extends InstanceVertexRenderer {

    public EntityCullingInstanceRenderer() {
        super(VertexFormat.Mode.QUADS, new PixelVertexBuffer(0), EntityCullingInstanceRenderer::init, new EntityUpdateVertex(1));
    }

    private static void init(@NotNull FloatBuffer buffer) {
        buffer.position(0);
        buffer.put(-1f);
        buffer.put(-1f);
        buffer.put(1f);
        buffer.put(-1f);
        buffer.put(1f);
        buffer.put(1f);
        buffer.put(-1f);
        buffer.put(1f);
        buffer.flip();
        GL15.glBufferData(34962, buffer, 35044);
    }
}
