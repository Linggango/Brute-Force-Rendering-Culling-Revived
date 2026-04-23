package misanthropy.brute_force_culling_revived.mixin.sodium;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import misanthropy.brute_force_culling_revived.api.Config;
import misanthropy.brute_force_culling_revived.api.CullingStateManager;
import misanthropy.brute_force_culling_revived.api.impl.IRenderSectionVisibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OcclusionCuller.class)
public abstract class MixinOcclusionCuller {

    @Inject(method = "isSectionVisible", at = @At(value = "RETURN"), remap = false, cancellable = true)
    private static void onIsSectionVisible(RenderSection section, Viewport viewport, float maxDistance, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (CullingStateManager.checkCulling) {
            cir.setReturnValue(true);
            return;
        }
        if (Config.shouldCullChunk() && !CullingStateManager.shouldRenderChunk((IRenderSectionVisibility) section, true)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "findVisible", at = @At(value = "HEAD"), remap = false, cancellable = true)
    private void onFindVisible(OcclusionCuller.Visitor visitor, Viewport viewport, float searchDistance, boolean useOcclusionCulling, int frame, CallbackInfo ci) {
        if (Config.getAsyncChunkRebuild() && RenderSystem.isOnRenderThread()) {
            ci.cancel();
        }
    }
}