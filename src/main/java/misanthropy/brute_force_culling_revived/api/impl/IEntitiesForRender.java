package misanthropy.brute_force_culling_revived.api.impl;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

public interface IEntitiesForRender {
    ObjectArrayList<?> bruteForceRenderingRevived$renderChunksInFrustum();
    ChunkRenderDispatcher.@Nullable RenderChunk bruteForceRenderingRevived$invokeGetRelativeFrom(BlockPos pos, ChunkRenderDispatcher.RenderChunk chunk, Direction dir);
    ChunkRenderDispatcher.RenderChunk bruteForceRenderingRevived$invokeGetRenderChunkAt(BlockPos pos);

}
