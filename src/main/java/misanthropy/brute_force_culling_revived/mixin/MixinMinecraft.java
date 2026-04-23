package misanthropy.brute_force_culling_revived.mixin;

import misanthropy.brute_force_culling_revived.api.CullingStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V", shift = At.Shift.AFTER))
    public void afterRunTick(boolean p_91384_, CallbackInfo ci) {
        CullingStateManager.onProfilerPopPush("afterRunTick");
    }

    @Inject(method = "runTick", at = @At(value = "HEAD"))
    public void beforeRunTick(boolean p_91384_, CallbackInfo ci) {
        CullingStateManager.onProfilerPopPush("beforeRunTick");
    }

    @Inject(method = "setLevel", at = @At(value = "HEAD"))
    public void onJoinWorld(ClientLevel world, CallbackInfo ci) {
        CullingStateManager.onWorldUnload(world);
    }
}