package misanthropy.brute_force_culling_revived.util;

import misanthropy.brute_force_culling_revived.api.Config;
import misanthropy.brute_force_culling_revived.api.CullingStateManager;
import misanthropy.brute_force_culling_revived.api.ModLoader;
import misanthropy.brute_force_culling_revived.api.data.ChunkCullingMap;
import net.minecraft.client.Minecraft;

import java.util.concurrent.Semaphore;

public class OcclusionCullerThread extends Thread {
    public static OcclusionCullerThread INSTANCE;
    private volatile boolean finished = false;
    private static final Semaphore TICK_SEMAPHORE = new Semaphore(0);

    public OcclusionCullerThread() {
        super("BFR-OcclusionCuller");
        this.setDaemon(true);
        if (INSTANCE != null) {
            INSTANCE.finished = true;
            TICK_SEMAPHORE.release();
        }
        INSTANCE = this;
    }

    public static void shouldUpdate() {
        if (Config.getAsyncChunkRebuild() && ModLoader.hasSodium()) {
            if (TICK_SEMAPHORE.availablePermits() < 1) {
                TICK_SEMAPHORE.release();
            }
            SodiumSectionAsyncUtil.shouldUpdate();
        }
    }

    @Override
    public void run() {
        while (!finished) {
            try {
                TICK_SEMAPHORE.acquire();

                if (finished || Minecraft.getInstance().level == null) {
                    finished = true;
                    break;
                }

                ChunkCullingMap chunkCullingMap = CullingStateManager.CHUNK_CULLING_MAP;
                if (chunkCullingMap != null && chunkCullingMap.isDone()) {
                    if (Config.getAsyncChunkRebuild() && ModLoader.hasSodium()) {
                        SodiumSectionAsyncUtil.asyncSearchRebuildSection();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                CullingStateManager.LOGGER.error("Error in culling thread", e);
            }
        }
    }
}