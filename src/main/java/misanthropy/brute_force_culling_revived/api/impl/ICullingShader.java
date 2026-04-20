package misanthropy.brute_force_culling_revived.api.impl;

import com.mojang.blaze3d.shaders.Uniform;
import org.jetbrains.annotations.Nullable;

public interface ICullingShader {
    @Nullable @org.jspecify.annotations.Nullable Uniform bruteForceRenderingRevived$getRenderDistance();
    @Nullable @org.jspecify.annotations.Nullable Uniform bruteForceRenderingRevived$getCullingCameraPos();
    @Nullable @org.jspecify.annotations.Nullable Uniform bruteForceRenderingRevived$getCullingCameraDir();
    @Nullable @org.jspecify.annotations.Nullable Uniform bruteForceRenderingRevived$getBoxScale();
    @Nullable @org.jspecify.annotations.Nullable Uniform bruteForceRenderingRevived$getDepthSize();
    @Nullable @org.jspecify.annotations.Nullable Uniform bruteForceRenderingRevived$getCullingSize();
    @Nullable @org.jspecify.annotations.Nullable Uniform bruteForceRenderingRevived$getLevelHeightOffset();
    @Nullable @org.jspecify.annotations.Nullable Uniform bruteForceRenderingRevived$getLevelMinSection();
    @Nullable @org.jspecify.annotations.Nullable Uniform bruteForceRenderingRevived$getEntityCullingSize();
    @Nullable @org.jspecify.annotations.Nullable Uniform bruteForceRenderingRevived$getCullingFrustum();
    @Nullable @org.jspecify.annotations.Nullable Uniform bruteForceRenderingRevived$getFrustumPos();
    @Nullable @org.jspecify.annotations.Nullable Uniform bruteForceRenderingRevived$getCullingViewMat();
    @Nullable @org.jspecify.annotations.Nullable Uniform bruteForceRenderingRevived$getCullingProjMat();
    @Nullable @org.jspecify.annotations.Nullable Uniform bruteForceRenderingRevived$getTestPos();
}