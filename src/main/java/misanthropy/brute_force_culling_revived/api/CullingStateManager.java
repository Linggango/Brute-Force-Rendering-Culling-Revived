package misanthropy.brute_force_culling_revived.api;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import misanthropy.brute_force_culling_revived.api.data.ChunkCullingMap;
import misanthropy.brute_force_culling_revived.api.data.EntityCullingMap;
import misanthropy.brute_force_culling_revived.api.impl.IEntitiesForRender;
import misanthropy.brute_force_culling_revived.api.impl.IRenderChunkInfo;
import misanthropy.brute_force_culling_revived.api.impl.IRenderSectionVisibility;
import misanthropy.brute_force_culling_revived.mixin.AccessorLevelRender;
import misanthropy.brute_force_culling_revived.mixin.AccessorMinecraft;
import misanthropy.brute_force_culling_revived.util.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Checks;
import org.slf4j.Logger;

import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.GL_TEXTURE;
import static org.lwjgl.opengl.GL30.*;

public class CullingStateManager {
    public static final String MOD_ID = "brute_force_culling_revived";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static volatile @Nullable EntityCullingMap ENTITY_CULLING_MAP = null;
    public static volatile @Nullable ChunkCullingMap CHUNK_CULLING_MAP = null;
    public static @NotNull Matrix4f VIEW_MATRIX = new Matrix4f();
    public static @NotNull Matrix4f PROJECTION_MATRIX = new Matrix4f();

    static {
        PROJECTION_MATRIX.identity();
    }

    public static final int DEPTH_SIZE = 5;
    public static int DEPTH_INDEX;
    public static int MAIN_DEPTH_TEXTURE = 0;
    public static RenderTarget @NotNull [] DEPTH_BUFFER_TARGET = new RenderTarget[DEPTH_SIZE];
    public static RenderTarget CHUNK_CULLING_MAP_TARGET;
    public static RenderTarget ENTITY_CULLING_MAP_TARGET;
    public static ShaderInstance CHUNK_CULLING_SHADER;
    public static ShaderInstance COPY_DEPTH_SHADER;
    public static ShaderInstance REMOVE_COLOR_SHADER;
    public static ShaderInstance INSTANCED_ENTITY_CULLING_SHADER;
    public static Frustum FRUSTUM;
    public static boolean updatingDepth;
    public static boolean applyFrustum;
    public static int DEBUG = 0;
    public static int @NotNull [] DEPTH_TEXTURE = new int[DEPTH_SIZE];
    public static @Nullable ShaderLoader SHADER_LOADER = null;

    public static final LifeTimer<Entity> visibleEntity = new LifeTimer<>();
    public static final LifeTimer<BlockPos> visibleBlock = new LifeTimer<>();
    private static boolean isNewTickFrame = false;
    private static boolean isNextLoopFrame = false;

    public static int fps = 0;
    private static int tick = 0;
    public static int clientTickCount = 0;
    public static int entityCulling = 0;
    public static int entityCount = 0;
    public static int blockCulling = 0;
    public static int blockCount = 0;
    public static long entityCullingTime = 0;
    public static long blockCullingTime = 0;
    public static long chunkCullingTime = 0;
    private static long preEntityCullingTime = 0;
    private static long preBlockCullingTime = 0;
    private static long preChunkCullingTime = 0;
    public static long preApplyFrustumTime = 0;
    public static long applyFrustumTime = 0;
    public static long chunkCullingInitTime = 0;
    public static long preChunkCullingInitTime = 0;
    public static long entityCullingInitTime = 0;
    public static long preEntityCullingInitTime = 0;
    public static int cullingInitCount = 0;
    public static int preCullingInitCount = 0;
    public static boolean checkCulling = false;
    public static boolean checkTexture = false;
    private static boolean usingShader = false;
    protected static int fullChunkUpdateCooldown = 0;
    public static int LEVEL_SECTION_RANGE;
    public static int LEVEL_POS_RANGE;
    public static int LEVEL_MIN_SECTION_ABS;
    public static int LEVEL_MIN_POS;
    private static double invLevelPosRange = 0;
    public static Camera CAMERA;

    private static final Int2IntOpenHashMap SHADER_DEPTH_BUFFER_ID = new Int2IntOpenHashMap();
    private static int frame;
    private static int lastVisibleUpdatedFrame;
    public static volatile boolean useOcclusionCulling = true;
    private static int continueUpdateCount;
    private static boolean lastUpdate;

    private static double cachedRenderDistanceSq = 0;

    static {
        RenderSystem.recordRenderCall(() -> {
            Minecraft mc = Minecraft.getInstance();
            int w = mc.getWindow().getWidth();
            int h = mc.getWindow().getHeight();
            for (int i = 0; i < DEPTH_BUFFER_TARGET.length; ++i) {
                DEPTH_BUFFER_TARGET[i] = new TextureTarget(w, h, false, Minecraft.ON_OSX);
                DEPTH_BUFFER_TARGET[i].setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            }
            CHUNK_CULLING_MAP_TARGET = new TextureTarget(w, h, false, Minecraft.ON_OSX);
            CHUNK_CULLING_MAP_TARGET.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            ENTITY_CULLING_MAP_TARGET = new TextureTarget(w, h, false, Minecraft.ON_OSX);
            ENTITY_CULLING_MAP_TARGET.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        });
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(CullingStateManager.class);

        if (ModLoader.hasIris()) {
            try {
                SHADER_LOADER = (ShaderLoader) Class.forName("misanthropy.brute_force_culling_revived.util.IrisLoaderImpl")
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (Exception e) {
                LOGGER.error("Failed to load IrisLoaderImpl", e);
            }
        }
    }

    public static void onWorldUnload(Level world) {
        if (world != Minecraft.getInstance().level) {
            cleanup();
        }
    }

    public static void cleanup() {
        tick = 0;
        clientTickCount = 0;
        visibleEntity.clear();
        visibleBlock.clear();

        ChunkCullingMap chunkMap = CHUNK_CULLING_MAP;
        if (chunkMap != null) {
            chunkMap.cleanup();
            CHUNK_CULLING_MAP = null;
        }

        EntityCullingMap entityMap = ENTITY_CULLING_MAP;
        if (entityMap != null) {
            entityMap.cleanup();
            ENTITY_CULLING_MAP = null;
        }

        SHADER_DEPTH_BUFFER_ID.clear();
        ModLoader.pauseAsync();
        if (ModLoader.hasSodium()) {
            SodiumSectionAsyncUtil.pauseAsync();
        }
    }

    public static int mapChunkY(double posY) {
        if (LEVEL_POS_RANGE == 0) return 0;
        double offset = posY - LEVEL_MIN_POS;
        return (int) Math.floor(offset * invLevelPosRange * LEVEL_SECTION_RANGE);
    }

    public static boolean shouldRenderChunk(@Nullable IRenderSectionVisibility section, boolean checkForChunk) {
        if (section == null) return false;

        final ChunkCullingMap map = CHUNK_CULLING_MAP;
        if (map == null) return true;

        if (DEBUG < 2) {
            if (!useOcclusionCulling) return true;
            if (section.bruteForceRenderingRevived$shouldCheckVisibilityInverted(lastVisibleUpdatedFrame)) return true;
            if (map.isChunkOffsetCameraVisible(
                    section.bruteForceRenderingRevived$getPositionX(),
                    section.bruteForceRenderingRevived$getPositionY(),
                    section.bruteForceRenderingRevived$getPositionZ(),
                    checkForChunk)) {
                section.bruteForceRenderingRevived$updateVisibleTick(lastVisibleUpdatedFrame);
                return true;
            }
            return false;
        }

        if (Config.getAsyncChunkRebuild() && !useOcclusionCulling) return true;

        boolean actualRender = section.bruteForceRenderingRevived$shouldCheckVisibilityInverted(lastVisibleUpdatedFrame)
                || map.isChunkOffsetCameraVisible(
                section.bruteForceRenderingRevived$getPositionX(),
                section.bruteForceRenderingRevived$getPositionY(),
                section.bruteForceRenderingRevived$getPositionZ(),
                checkForChunk);

        if (actualRender) {
            section.bruteForceRenderingRevived$updateVisibleTick(lastVisibleUpdatedFrame);
        }

        return actualRender;
    }

    public static boolean shouldSkipBlockEntity(@NotNull BlockEntity blockEntity, @SuppressWarnings("unused") AABB aabb, @NotNull BlockPos pos) {
        blockCount++;

        final Vec3 camPos = CAMERA.getPosition();
        final double dx = pos.getX() - camPos.x;
        final double dy = pos.getY() - camPos.y;
        final double dz = pos.getZ() - camPos.z;
        final double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq > cachedRenderDistanceSq) return false;

        final EntityCullingMap entityMap = ENTITY_CULLING_MAP;
        if (entityMap == null || !Config.getCullBlockEntity()) return false;

        ResourceLocation key = BlockEntityType.getKey(blockEntity.getType());
        if (key == null || Config.getBlockEntitiesSkip().contains(key.toString())) return false;

        boolean visible = false;
        boolean actualVisible;

        if (DEBUG < 2) {
            if (entityMap.isObjectVisible(blockEntity)) {
                visibleBlock.updateUsageTick(pos, clientTickCount);
                visible = true;
            } else if (visibleBlock.contains(pos)) {
                visible = true;
            }
            return !visible;
        }

        long time = System.nanoTime();
        actualVisible = entityMap.isObjectVisible(blockEntity);

        if (actualVisible) {
            visible = true;
        } else if (visibleBlock.contains(pos)) {
            visible = true;
        }

        preBlockCullingTime += System.nanoTime() - time;

        if (checkCulling) visible = !visible;

        if (!visible) {
            blockCulling++;
        } else if (actualVisible) {
            visibleBlock.updateUsageTick(pos, clientTickCount);
        }

        return !visible;
    }

    public static boolean shouldSkipEntity(Entity entity) {
        entityCount++;
        if (entity instanceof Player || entity.isCurrentlyGlowing()) return false;
        if (entity.distanceToSqr(CAMERA.getPosition()) < 4.0) return false;

        ResourceLocation entityKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityKey != null && Config.getEntitiesSkip().contains(entityKey.toString())) return false;

        final EntityCullingMap entityMap = ENTITY_CULLING_MAP;
        if (entityMap == null || !Config.getCullEntity()) return false;

        boolean visible = false;
        boolean actualVisible;

        if (DEBUG < 2) {
            if (entityMap.isObjectVisible(entity)) {
                visibleEntity.updateUsageTick(entity, clientTickCount);
                visible = true;
            } else if (visibleEntity.contains(entity)) {
                visible = true;
            }
            return !visible;
        }

        long time = System.nanoTime();
        actualVisible = entityMap.isObjectVisible(entity);

        if (actualVisible) {
            visible = true;
        } else if (visibleEntity.contains(entity)) {
            visible = true;
        }

        preEntityCullingTime += System.nanoTime() - time;

        if (checkCulling) visible = !visible;

        if (!visible) {
            entityCulling++;
        } else if (actualVisible) {
            visibleEntity.updateUsageTick(entity, clientTickCount);
        }

        return !visible;
    }

    public static void onProfilerPopPush(@NotNull String s) {
        Minecraft mc = Minecraft.getInstance();
        switch (s) {
            case "beforeRunTick" -> {
                if (((AccessorLevelRender) mc.levelRenderer).getNeedsFullRenderChunkUpdate() && mc.level != null) {
                    if (ModLoader.hasMod("embeddium")) ModLoader.pauseAsync();
                    Level level = mc.level;
                    LEVEL_SECTION_RANGE = level.getMaxSection() - level.getMinSection();
                    LEVEL_MIN_SECTION_ABS = Math.abs(level.getMinSection());
                    LEVEL_MIN_POS = level.getMinBuildHeight();
                    LEVEL_POS_RANGE = level.getMaxBuildHeight() - level.getMinBuildHeight();
                    invLevelPosRange = (LEVEL_POS_RANGE != 0) ? (1.0 / LEVEL_POS_RANGE) : 0;
                }
            }
            case "afterRunTick" -> {
                ++frame;
                updateMapData();
                OcclusionCullerThread.shouldUpdate();
            }
            case "captureFrustum" -> {
                AccessorLevelRender levelFrustum = (AccessorLevelRender) mc.levelRenderer;
                Frustum frustum = levelFrustum.getCapturedFrustum() != null
                        ? levelFrustum.getCapturedFrustum()
                        : levelFrustum.getCullingFrustum();
                CullingStateManager.FRUSTUM = new Frustum(frustum).offsetToFullyIncludeCameraCube(32);

                ChunkCullingMap chunkMap = CullingStateManager.CHUNK_CULLING_MAP;
                if (chunkMap != null) chunkMap.updateCamera();
                checkShader();
            }
            case "destroyProgress" -> {
                updatingDepth = true;
                updateDepthMap();
                readMapData();
                CullingRenderEvent.updateCullingMap();
                updatingDepth = false;
            }
        }
    }

    public static void onProfilerPush(@NotNull String s) {
        if (s.equals("onKeyboardInput")) {
            ModLoader.onKeyPress();
        } else if (s.equals("center")) {
            Minecraft mc = Minecraft.getInstance();
            CAMERA = mc.gameRenderer.getMainCamera();
            int thisTick = clientTickCount % 20;

            isNewTickFrame = (tick != thisTick);
            isNextLoopFrame = (isNewTickFrame && thisTick == 0);

            if (isNewTickFrame) {
                tick = thisTick;
            }

            entityCulling = 0;
            entityCount = 0;
            blockCulling = 0;
            blockCount = 0;

            double renderDist = mc.options.getEffectiveRenderDistance() * 16.0;
            cachedRenderDistanceSq = renderDist * renderDist * 2.0;

            if (isNewTickFrame) {
                if (fullChunkUpdateCooldown > 0) fullChunkUpdateCooldown--;
                if (continueUpdateCount > 0) continueUpdateCount--;
            }

            if (isNextLoopFrame) {
                visibleBlock.tick(clientTickCount, 3);
                visibleEntity.tick(clientTickCount, 3);

                final EntityCullingMap entityMap = ENTITY_CULLING_MAP;
                if (entityMap != null) entityMap.getEntityTable().tickTemp(clientTickCount);

                applyFrustumTime = preApplyFrustumTime; preApplyFrustumTime = 0;
                entityCullingTime = preEntityCullingTime; preEntityCullingTime = 0;
                blockCullingTime = preBlockCullingTime; preBlockCullingTime = 0;
                chunkCullingInitTime = preChunkCullingInitTime; preChunkCullingInitTime = 0;
                cullingInitCount = preCullingInitCount; preCullingInitCount = 0;
                entityCullingInitTime = preEntityCullingInitTime; preEntityCullingInitTime = 0;

                final ChunkCullingMap chunkMap = CHUNK_CULLING_MAP;
                if (chunkMap != null) {
                    chunkMap.lastQueueUpdateCount = chunkMap.queueUpdateCount;
                    chunkMap.queueUpdateCount = 0;
                }

                if (preChunkCullingTime != 0) {
                    chunkCullingTime = preChunkCullingTime;
                    preChunkCullingTime = 0;
                }
            }
        }
    }

    public static void readMapData() {
        if (checkCulling) return;

        final ChunkCullingMap chunkMap = CHUNK_CULLING_MAP;
        if (Config.getCullChunk() && chunkMap != null && chunkMap.isTransferred()) {
            long time = System.nanoTime();
            chunkMap.readData();
            lastVisibleUpdatedFrame = frame;
            preChunkCullingInitTime += System.nanoTime() - time;
        }

        final EntityCullingMap entityMap = ENTITY_CULLING_MAP;
        if (Config.doEntityCulling() && entityMap != null && entityMap.isTransferred()) {
            long time = System.nanoTime();
            entityMap.readData();
            lastVisibleUpdatedFrame = frame;
            preEntityCullingInitTime += System.nanoTime() - time;
        }
    }

    public static void checkShader() {
        if (SHADER_LOADER != null) {
            boolean currentEnabled = SHADER_LOADER.enabledShader();
            if (currentEnabled != usingShader) {
                usingShader = currentEnabled;
                cleanup();
            }
        }
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.@NotNull ComputeCameraAngles event) {
        PoseStack pose = new PoseStack();
        Vec3 pos = event.getCamera().getPosition();
        pose.mulPose(Axis.ZP.rotationDegrees(event.getRoll()));
        pose.mulPose(Axis.XP.rotationDegrees(event.getPitch()));
        pose.mulPose(Axis.YP.rotationDegrees(event.getYaw() + 180.0F));
        pose.translate((float) -pos.x, (float) -pos.y, (float) -pos.z);
        CullingStateManager.VIEW_MATRIX.set(pose.last().pose());
    }

    public static void updateDepthMap() {
        CullingStateManager.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        if (anyCulling() && !checkCulling && anyNeedTransfer() && continueUpdateDepth()) {
            Minecraft mc = Minecraft.getInstance();
            float sampling = (float) Config.getSampling();
            int width = mc.getWindow().getWidth();
            int height = mc.getWindow().getHeight();

            runOnDepthFrame((ctx) -> {
                int sw = Math.max(1, (int) (width * sampling * ctx.scale()));
                int sh = Math.max(1, (int) (height * sampling * ctx.scale()));
                if (ctx.frame().width != sw || ctx.frame().height != sh) ctx.frame().resize(sw, sh, Minecraft.ON_OSX);
            });

            int depthTexture = mc.getMainRenderTarget().getDepthTextureId();
            ShaderLoader loader = SHADER_LOADER;
            if (loader != null && loader.enabledShader()) {
                int fboId = loader.getFrameBufferID();
                if (!SHADER_DEPTH_BUFFER_ID.containsKey(fboId)) {
                    RenderSystem.assertOnRenderThreadOrInit();
                    GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, fboId);
                    int[] attachmentObjectType = new int[1];
                    glGetFramebufferAttachmentParameteriv(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE, attachmentObjectType);

                    if (attachmentObjectType[0] == GL_TEXTURE) {
                        int[] depthTextureID = new int[1];
                        glGetFramebufferAttachmentParameteriv(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME, depthTextureID);
                        depthTexture = depthTextureID[0];
                        SHADER_DEPTH_BUFFER_ID.put(fboId, depthTexture);
                    }
                } else {
                    depthTexture = SHADER_DEPTH_BUFFER_ID.get(fboId);
                }
            }

            MAIN_DEPTH_TEXTURE = depthTexture;
            runOnDepthFrame((ctx) -> {
                useShader(CullingStateManager.COPY_DEPTH_SHADER);
                ctx.frame().clear(Minecraft.ON_OSX);
                ctx.frame().bindWrite(false);
                Tesselator tess = Tesselator.getInstance();
                BufferBuilder bb = tess.getBuilder();
                bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
                bb.vertex(-1, -1, 0).endVertex(); bb.vertex(1, -1, 0).endVertex();
                bb.vertex(1, 1, 0).endVertex(); bb.vertex(-1, 1, 0).endVertex();
                RenderSystem.setShaderTexture(0, ctx.lastTexture());
                tess.end();
                DEPTH_TEXTURE[ctx.index()] = ctx.frame().getColorTextureId();
            });
            bindMainFrameTarget();
        }
    }

    public static void updateMapData() {
        if (!anyCulling()) {
            cleanup();
            return;
        }

        if (anyNeedTransfer()) preCullingInitCount++;
        Minecraft mc = Minecraft.getInstance();

        if (Config.getCullChunk()) updateChunkCullingMap(mc);
        if (Config.doEntityCulling()) updateEntityCullingMap(mc);

        fps = ((AccessorMinecraft) mc).getFps();
    }

    private static void updateChunkCullingMap(Minecraft mc) {
        int dist = mc.options.getEffectiveRenderDistance();
        int renderingDiameter = dist * 2 + 1;
        int maxSize = renderingDiameter * LEVEL_SECTION_RANGE * renderingDiameter;
        int cSize = (int) Math.sqrt(maxSize) + 1;

        if (CHUNK_CULLING_MAP_TARGET.width != cSize) {
            CHUNK_CULLING_MAP_TARGET.resize(cSize, cSize, Minecraft.ON_OSX);

            ChunkCullingMap oldMap = CHUNK_CULLING_MAP;
            if (oldMap != null) oldMap.cleanup();

            ChunkCullingMap newMap = new ChunkCullingMap(cSize, cSize);
            CHUNK_CULLING_MAP = newMap;
            newMap.generateIndex(dist);
        }

        long time = System.nanoTime();
        ChunkCullingMap chunkMap = CHUNK_CULLING_MAP;
        if (chunkMap != null) chunkMap.transferData();
        preChunkCullingInitTime += System.nanoTime() - time;
    }

    private static void updateEntityCullingMap(Minecraft mc) {

        EntityCullingMap entityMap = ENTITY_CULLING_MAP;
        if (entityMap == null) {
            entityMap = new EntityCullingMap(8, 64);
            ENTITY_CULLING_MAP = entityMap;
        }

        int neededH = (entityMap.getEntityTable().size() / 64 * 64 + 64) / 8 + 1;
        if (ENTITY_CULLING_MAP_TARGET.height != neededH) {
            ENTITY_CULLING_MAP_TARGET.resize(8, neededH, Minecraft.ON_OSX);
            EntityCullingMap newMap = new EntityCullingMap(8, neededH);
            newMap.getEntityTable().copyTemp(entityMap.getEntityTable(), clientTickCount);
            entityMap.cleanup();
            entityMap = newMap;
            ENTITY_CULLING_MAP = entityMap;
        }

        long time = System.nanoTime();
        entityMap.transferData();
        preEntityCullingInitTime += System.nanoTime() - time;

        if (mc.level != null) {
            entityMap.getEntityTable().clearIndexMap();

            for (Entity e : mc.level.entitiesForRendering()) {
                entityMap.getEntityTable().addObject(e);
            }

            IEntitiesForRender levelRenderer = (IEntitiesForRender) mc.levelRenderer;
            for (Object info : levelRenderer.bruteForceRenderingRevived$renderChunksInFrustum()) {
                for (BlockEntity be : ((IRenderChunkInfo) info).bruteForceRenderingRevived$getRenderChunk().getCompiledChunk().getRenderableBlockEntities()) {
                    entityMap.getEntityTable().addObject(be);
                }
            }

            entityMap.getEntityTable().addAllTemp();
        }
    }

    public static void useShader(ShaderInstance instance) { RenderSystem.setShader(() -> instance); }

    public static void bindMainFrameTarget() {
        if (renderingShader()) {
            assert SHADER_LOADER != null;
            SHADER_LOADER.bindDefaultFrameBuffer();
        } else {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        }
    }

    public static void runOnDepthFrame(@NotNull Consumer<DepthContext> consumer) {
        float f = 1.0f;
        for (DEPTH_INDEX = 0; DEPTH_INDEX < DEPTH_BUFFER_TARGET.length; ++DEPTH_INDEX) {
            int lastTex = DEPTH_INDEX == 0 ? MAIN_DEPTH_TEXTURE : DEPTH_BUFFER_TARGET[DEPTH_INDEX - 1].getColorTextureId();
            consumer.accept(new DepthContext(DEPTH_BUFFER_TARGET[DEPTH_INDEX], DEPTH_INDEX, f, lastTex));
            f *= 0.35f;
        }
    }

    public static void callDepthTexture() { runOnDepthFrame(ctx -> RenderSystem.setShaderTexture(ctx.index(), DEPTH_TEXTURE[ctx.index()])); }
    public static boolean renderingIris() { return renderingShader(); }
    public static boolean renderingShader() { return SHADER_LOADER != null && SHADER_LOADER.renderingShaderPass(); }
    public static boolean enabledShader() { return SHADER_LOADER != null && SHADER_LOADER.enabledShader(); }

    public static boolean anyNextTick() { return isNewTickFrame; }
    public static boolean isNextLoop() { return isNextLoopFrame; }

    public static boolean anyCulling() { return Config.getCullChunk() || Config.doEntityCulling(); }
    public static boolean anyNeedTransfer() {
        ChunkCullingMap chunkMap = CHUNK_CULLING_MAP;
        EntityCullingMap entityCullingMap = ENTITY_CULLING_MAP;
        return (entityCullingMap != null && entityCullingMap.needTransferData()) || (chunkMap != null && chunkMap.needTransferData());
    }

    private static int gl33 = -1;
    public static boolean gl33() {
        if (RenderSystem.isOnRenderThread() && gl33 < 0)
            gl33 = (GL.getCapabilities().OpenGL33 || Checks.checkFunctions(GL.getCapabilities().glVertexAttribDivisor)) ? 1 : 0;
        return gl33 == 1;
    }

    public static boolean needPauseRebuild() { return fullChunkUpdateCooldown > 0; }
    public static void updating() { continueUpdateCount = 10; lastUpdate = true; }
    public static boolean continueUpdateChunk() { if (continueUpdateCount > 0) return true; if (lastUpdate) { lastUpdate = false; return true; } return false; }
    public static boolean continueUpdateDepth() { return continueUpdateCount > 0; }

    public static long getPreChunkCullingTime() { return preChunkCullingTime; }
    public static void setPreChunkCullingTime(long preChunkCullingTime) { CullingStateManager.preChunkCullingTime = preChunkCullingTime; }
}