package misanthropy.brute_force_culling_revived.api.impl;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;

public interface ICollectorAccessor {
    void bruteForceRenderingRevived$addAsyncToRebuildLists(RenderSection section);

    void bruteForceRenderingRevived$addRenderList(ChunkRenderList renderList);
}