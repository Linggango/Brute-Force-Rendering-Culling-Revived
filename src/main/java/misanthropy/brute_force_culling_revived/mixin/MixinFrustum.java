package misanthropy.brute_force_culling_revived.mixin;

import misanthropy.brute_force_culling_revived.api.Config;
import misanthropy.brute_force_culling_revived.api.CullingStateManager;
import misanthropy.brute_force_culling_revived.util.DummySection;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
public abstract class MixinFrustum {

    @Inject(method = "isVisible", at = @At(value = "RETURN"), cancellable = true)
    public void afterVisible(@NotNull AABB aabb, @NotNull CallbackInfoReturnable<Boolean> cir) {
        if (CullingStateManager.applyFrustum && Config.shouldCullChunk() && cir.getReturnValue() && !CullingStateManager.shouldRenderChunk(new DummySection(aabb), true))
            cir.setReturnValue(false);
    }
}
