package misanthropy.brute_force_culling_revived.api;

import com.google.common.collect.ImmutableList;
import misanthropy.brute_force_culling_revived.api.data.ChunkCullingMap;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class Config {

    public static ForgeConfigSpec CLIENT_CONFIG;

    private static final ForgeConfigSpec.DoubleValue SAMPLING;
    private static final ForgeConfigSpec.BooleanValue CULL_ENTITY;
    private static final ForgeConfigSpec.BooleanValue CULL_BLOCK_ENTITY;
    private static final ForgeConfigSpec.BooleanValue CULL_CHUNK;
    private static final ForgeConfigSpec.BooleanValue ASYNC;
    private static final ForgeConfigSpec.BooleanValue AUTO_DISABLE_ASYNC;
    private static final ForgeConfigSpec.IntValue UPDATE_DELAY;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_SKIP;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCK_ENTITY_SKIP;

    private static final double DEFAULT_SAMPLING = 0.5;
    private static final double MIN_SAMPLING = 0.05;

    private static volatile boolean loaded = false;

    public static void setLoaded() {
        loaded = true;
    }

    private static boolean unload() {
        return !loaded;
    }

    public static double getSampling() {
        if (unload())
            return DEFAULT_SAMPLING;
        return Math.max(SAMPLING.get(), MIN_SAMPLING);
    }

    public static void setSampling(double value) {
        SAMPLING.set(Math.max(MIN_SAMPLING, value));
        SAMPLING.save();
    }

    public static boolean doEntityCulling() {

        if (unload() || !CullingStateManager.gl33())
            return false;
        return CULL_ENTITY.get() || CULL_BLOCK_ENTITY.get();
    }

    public static boolean getCullEntity() {
        if (unload() || !CullingStateManager.gl33())
            return false;
        return CULL_ENTITY.get();
    }

    public static void setCullEntity(boolean value) {
        CULL_ENTITY.set(value);
        CULL_ENTITY.save();
    }

    public static boolean getCullBlockEntity() {
        if (unload() || !CullingStateManager.gl33())
            return false;
        return CULL_BLOCK_ENTITY.get();
    }

    public static void setCullBlockEntity(boolean value) {
        CULL_BLOCK_ENTITY.set(value);
        CULL_BLOCK_ENTITY.save();
    }

    public static boolean getCullChunk() {
        if (unload())
            return false;
        return CULL_CHUNK.get();
    }

    public static boolean shouldCullChunk() {
        if (unload())
            return false;
        ChunkCullingMap chunkCullingMap = CullingStateManager.CHUNK_CULLING_MAP;
        if (chunkCullingMap == null || !chunkCullingMap.isDone())
            return false;
        return CULL_CHUNK.get();
    }

    public static void setCullChunk(boolean value) {
        CULL_CHUNK.set(value);
        CULL_CHUNK.save();
    }

    public static boolean getAsyncChunkRebuild() {
        if (unload())
            return false;
        if (!shouldCullChunk())
            return false;
        if (CullingStateManager.needPauseRebuild())
            return false;
        if (!ModLoader.hasSodium())
            return false;
        if (ModLoader.hasNvidium())
            return false;
        if (getAutoDisableAsync() && CullingStateManager.enabledShader())
            return false;
        return ASYNC.get();
    }

    public static void setAsyncChunkRebuild(boolean value) {
        if (!shouldCullChunk())
            return;
        if (!ModLoader.hasSodium())
            return;
        if (CullingStateManager.needPauseRebuild())
            return;
        if (ModLoader.hasNvidium())
            return;
        ASYNC.set(value);
        ASYNC.save();
    }

    public static boolean getAutoDisableAsync() {
        if (unload())
            return false;
        return AUTO_DISABLE_ASYNC.get();
    }

    public static void setAutoDisableAsync(boolean value) {
        AUTO_DISABLE_ASYNC.set(value);
        AUTO_DISABLE_ASYNC.save();
    }

    public static int getShaderDynamicDelay() {
        return CullingStateManager.enabledShader() ? 1 : 0;
    }

    public static int getDepthUpdateDelay() {
        if (unload())
            return 1;

        int delay = UPDATE_DELAY.get();
        return delay <= 9 ? delay + getShaderDynamicDelay() : delay;
    }

    public static void setDepthUpdateDelay(int value) {
        UPDATE_DELAY.set(value);
        UPDATE_DELAY.save();
    }

    public static List<? extends String> getEntitiesSkip() {
        if (unload())
            return ImmutableList.of();
        return ENTITY_SKIP.get();
    }

    public static List<? extends String> getBlockEntitiesSkip() {
        if (unload())
            return ImmutableList.of();
        return BLOCK_ENTITY_SKIP.get();
    }

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("Sampling multiple");
        SAMPLING = builder.defineInRange("multiple", DEFAULT_SAMPLING, 0.0, 1.0);
        builder.pop();

        builder.push("Culling Map update delay");
        UPDATE_DELAY = builder.defineInRange("delay frame", 1, 0, 10);
        builder.pop();

        builder.push("Cull entity");
        CULL_ENTITY = builder.define("enabled", true);
        builder.pop();

        builder.push("Cull block entity");
        CULL_BLOCK_ENTITY = builder.define("enabled", true);
        builder.pop();

        builder.push("Cull chunk");
        CULL_CHUNK = builder.define("enabled", true);
        builder.pop();

        builder.push("Async chunk rebuild");
        ASYNC = builder.define("enabled", true);
        builder.pop();

        builder.push("Auto disable async rebuild");
        AUTO_DISABLE_ASYNC = builder.define("enabled", true);
        builder.pop();

        builder.comment("Entity skip CULLING").push("Entity ResourceLocation");
        ENTITY_SKIP = builder
                .comment("Entities that skip culling, example: [\"minecraft:creeper\", \"minecraft:zombie\"]")
                .defineList("list", List.of("create:stationary_contraption"), o -> o instanceof String);
        builder.pop();

        builder.comment("Block Entity skip CULLING").push("Block Entity ResourceLocation");
        BLOCK_ENTITY_SKIP = builder
                .comment("Block entities that skip culling, example: [\"minecraft:chest\", \"minecraft:mob_spawner\"]")
                .defineList("list", List.of("minecraft:beacon"), o -> o instanceof String);
        builder.pop();

        CLIENT_CONFIG = builder.build();
    }
}