package misanthropy.brute_force_culling_revived.api.impl;

public interface IRenderSectionVisibility {
    boolean bruteForceRenderingRevived$shouldCheckVisibility(int clientTick);

    void bruteForceRenderingRevived$updateVisibleTick(int clientTick);

    int bruteForceRenderingRevived$getPositionX();

    int bruteForceRenderingRevived$getPositionY();

    int bruteForceRenderingRevived$getPositionZ();
}
