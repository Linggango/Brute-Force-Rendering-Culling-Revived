package misanthropy.brute_force_culling_revived.mixin.sodium;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.VisibleChunkCollector;
import misanthropy.brute_force_culling_revived.api.impl.ICollectorAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VisibleChunkCollector.class)
public abstract class MixinVisibleChunkCollector implements ICollectorAccessor {
    @Shadow(remap = false) @Final private ObjectArrayList<ChunkRenderList> sortedRenderLists;

    @Shadow(remap = false) protected abstract void addToRebuildLists(RenderSection section);

    @Override
    public void bruteForceRenderingRevived$addAsyncToRebuildLists(RenderSection renderSection) {
        this.addToRebuildLists(renderSection);
    }

    @Override
    public void bruteForceRenderingRevived$addRenderList(ChunkRenderList renderList) {
        this.sortedRenderLists.add(renderList);
    }
}
