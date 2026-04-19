package misanthropy.brute_force_culling_revived.mixin;

import misanthropy.brute_force_culling_revived.api.impl.IRenderChunkInfo;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LevelRenderer.RenderChunkInfo.class)
public class MixinRenderChunkInfo implements IRenderChunkInfo {

    @Final
    @Shadow
    ChunkRenderDispatcher.RenderChunk chunk;

    @Override
    public ChunkRenderDispatcher.RenderChunk bruteForceRenderingRevived$getRenderChunk() {
        return chunk;
    }

    @Shadow
    @Final
    int step;

    @Override
    public int bruteForceRenderingRevived$getStep() {
        return this.step;
    }
}
