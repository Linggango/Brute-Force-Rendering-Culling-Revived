package misanthropy.brute_force_culling_revived.util;

import misanthropy.brute_force_culling_revived.api.impl.IRenderSectionVisibility;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class DummySection implements IRenderSectionVisibility {
    private final int x;
    private final int y;
    private final int z;

    public DummySection(@NotNull AABB aabb) {
        Vec3 center = aabb.getCenter();
        x = (int) center.x;
        y = (int) center.y;
        z = (int) center.z;
    }

    @Override
    public boolean bruteForceRenderingRevived$shouldCheckVisibility(int clientTick) {
        return false;
    }

    @Override
    public void bruteForceRenderingRevived$updateVisibleTick(int clientTick) {
    }

    @Override
    public int bruteForceRenderingRevived$getPositionX() {
        return x;
    }

    @Override
    public int bruteForceRenderingRevived$getPositionY() {
        return y;
    }

    @Override
    public int bruteForceRenderingRevived$getPositionZ() {
        return z;
    }
}
