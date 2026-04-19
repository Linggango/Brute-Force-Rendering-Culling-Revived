package misanthropy.brute_force_culling_revived.util;

import com.mojang.blaze3d.pipeline.RenderTarget;

public record DepthContext(RenderTarget frame, int index, float scale, int lastTexture) {}
